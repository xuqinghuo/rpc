package org.lib.rpc.protocol.hessian;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import com.caucho.hessian.io.*;
import com.caucho.services.server.ServiceContext;
import org.apache.commons.codec.binary.Base64;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.util.CharsetUtil;
import org.lib.rpc.processor.SerivceExporter;
import org.lib.rpc.processor.ServiceMetaData;
import org.lib.rpc.conf.SerivceConfig;
import org.lib.rpc.processor.ServiceProcessor;
import org.lib.rpc.transfer.RequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by xuqinghuo on 2017/4/26.
 */
public class HessionHandler extends SimpleChannelUpstreamHandler {
    private final Logger logger = LoggerFactory.getLogger(HessionHandler.class);
    private HttpRequest request;
    private boolean readingChunks;
    private final StringBuilder buf = new StringBuilder();
    private HessianInputFactory _inputFactory = new HessianInputFactory();
    private HessianFactory _hessianFactory = new HessianFactory();

    private SerializerFactory _serializerFactory;
    private ExecutorService threadpool;
    private SerivceConfig serivceConfig;
    public HessionHandler(SerivceConfig serivceConfig,ExecutorService threadpool){
        this.threadpool=threadpool;

        this.serivceConfig = serivceConfig;
    }
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e){
        if(!(e.getCause() instanceof IOException)){
            logger.error("catch some exception not IOException",e.getCause());
        }
    }

    public void messageReceived(ChannelHandlerContext channelHandlerContext, MessageEvent event){
        if(!readingChunks) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            HttpRequest request = this.request=(HttpRequest) event.getMessage();
            HttpResponse response =new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
            String uri = request.getUri();
            if(!uri.startsWith("/"+serivceConfig.getContextRoot()+"/")){
                sendResourceNotFound(channelHandlerContext,event);
                return;
            }
            if(uri.endsWith("/")){
                uri=uri.substring(0,uri.length()-1);
            }
            String serviceName = uri.substring(uri.lastIndexOf("/")+1);
            SocketAddress remoteAddress = channelHandlerContext.getChannel().getRemoteAddress();
            String ipAddress = remoteAddress.toString().split(":")[0];
            request.addHeader("Client-IP",ipAddress.substring(0));
            handleService(serviceName,request,response,os,event);
        }
    }
    private void handleService(final String serviceName,
                               final HttpRequest request, final HttpResponse response,
                               final ByteArrayOutputStream os, final MessageEvent e){
        try {
            threadpool.execute(new Runnable() {
                public void run() {
                    try {
                        service(serviceName, request, response, os);
                    }catch (Exception e1){
                        logger.error(e1.getMessage());
                    }
                    if(HttpHeaders.is100ContinueExpected(request)){
                        send100Continue(e);
                    }
                    if(request.isChunked()){
                        readingChunks=true;
                    }else{
                        writeResponse(e,response,os);
                    }
                }
            });
        }catch(RejectedExecutionException exception){
           logger.error("server threadpool full,threadpool maxsize is "+((ThreadPoolExecutor)threadpool).getMaximumPoolSize());

        }
    }

    public void setSerializerFactory(SerializerFactory factory) {
        _serializerFactory = factory;
    }

    public SerializerFactory getSerializerFactory() {
        if (_serializerFactory == null)
            _serializerFactory = new SerializerFactory();

        return _serializerFactory;
    }

    public void service(String serviceName,HttpRequest req,HttpResponse response,ByteArrayOutputStream os){
       byte [] bytes = req.getContent().array();
        InputStream is = new ByteArrayInputStream(bytes);
        SerializerFactory serializerFactory = getSerializerFactory();
        String username=null;
        String password = null;
        String[] authLink = getUsernameAndPassword(req);
        username = authLink[0].equals("")?null:authLink[0];
        password = authLink[1].equals("")?null:authLink[1];
        String clientIP = request.getHeader("Client-IP");
        RequestWrapper rw = new RequestWrapper(username,password,clientIP,serviceName);
        invoke(rw,is,os,serializerFactory);
    }

    private String[] getUsernameAndPassword(HttpRequest req){
        String auths = request.getHeader("Authorization");
        if(auths == null){
            String str[] ={"",""};
            return str;
        }
        String auth[] = auths.split(" ");
        String bauth = auth[1];
        String dauth = new String(Base64.decodeBase64(bauth));
        String authLink[] = dauth.split(":");
        return authLink;
    }
    private void writeResponse(MessageEvent e,HttpResponse response,ByteArrayOutputStream os){
        boolean keepAlive = isKeepAlive(request);
        ChannelBuffer cb = ChannelBuffers.dynamicBuffer();
        cb.writeBytes(os.toByteArray());
        response.setContent(cb);
        if(keepAlive){
            response.setHeader(CONTENT_LENGTH,response.getContent().readableBytes());
        }
        ChannelFuture future = e.getChannel().write(response);
        if(!keepAlive){
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    protected void invoke(RequestWrapper rw, InputStream is, OutputStream os,SerializerFactory serializerFactory){
        AbstractHessianInput in = null;
        AbstractHessianOutput out = null;
        String username = rw.getUser();
        String password = rw.getPassword();
        try {
            HessianInputFactory.HeaderType header = _inputFactory.readHeader(is);
            switch (header) {
                case CALL_1_REPLY_1:
                    in = _hessianFactory.createHessianInput(is);
                    out = _hessianFactory.createHessianOutput(os);
                    break;
                case CALL_1_REPLY_2:
                    in = _hessianFactory.createHessianInput(is);
                    out = _hessianFactory.createHessian2Output(os);
                    break;
                case HESSIAN_2:
                    in = _hessianFactory.createHessian2Input(is);
                    in.readCall();
                    out = _hessianFactory.createHessian2Output(os);
                    break;
                default:
                    throw new IllegalStateException(header
                            + " is an unknown Hessian call");
            }
            if (serializerFactory != null) {
                in.setSerializerFactory(serializerFactory);
                out.setSerializerFactory(serializerFactory);
            }
            invoke(rw, in, out);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                out.writeFault("ServiceException", e.getMessage(), e);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void invoke(RequestWrapper rw,AbstractHessianInput in,AbstractHessianOutput out) throws Exception{
        ServiceContext context = ServiceContext.getContext();
        String serviceName =rw.getServiceName();
        in.skipOptionalCall();
        String header;
        while ((header = in.readHeader()) != null) {
            Object value = in.readObject();
            context.addHeader(header, value);
        }
        ServiceMetaData metaData = SerivceExporter.getServiceMetaData(serviceName);
        if (metaData == null) {
            logger.error("service " + serviceName+ " can't find.");
            out.writeFault("NoSuchService","service " + serviceName+ " can't find.", null);
            out.close();
            return;
        }
        String methodName = in.readMethod();
        int argLength = in.readMethodArgLength();
        Method method = metaData.getMethod(methodName + "__" + argLength);
        if (method == null) {
            method = metaData.getMethod(methodName);
        }
        if (method == null) {
            out.writeFault("NoSuchMethod","service["+methodName+"]'s method " + methodName+ " cannot find", null);
            out.close();
            return;
        }
        Class<?>[] argTypes = method.getParameterTypes();
        Object[] argObjs = new Object[argTypes.length];
        for (int i = 0; i < argTypes.length; i++) {
            argObjs[i] = in.readObject(argTypes[i]);
        }
        rw.setMethodName(method.getName());
        rw.setArgs(argObjs);
        rw.setArgsTypes(argTypes);
        if (argLength != argObjs.length && argLength >= 0) {
            out.writeFault("NoSuchMethod","service["+methodName+"]'s method " + methodName
                    + " argument length mismatch, received length="
                    + argLength, null);
            out.close();
            return;
        }
        Object result = null;
        try {
            result = ServiceProcessor.handleRequest(rw);
        } catch (Exception e) {
            Throwable e1 = e;
            if (e1 instanceof InvocationTargetException)
                e1 = ((InvocationTargetException) e).getTargetException();

            logger.debug(this + " " + e1.toString(), e1);
            result = e;
            out.writeFault("ServiceException", e1.getMessage(), e1);
            out.close();
            return;
        }
        in.completeCall();
        out.writeReply(result);
        out.close();
    }

    private void send100Continue(MessageEvent e){
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1,CONTINUE);
        ChannelBuffer content1 = request.getContent();
        if(content1.readable()){
            buf.append(content1.toString(CharsetUtil.UTF_8));
        }
        ChannelFuture future = e.getChannel().write(response);
        boolean keepAlive = isKeepAlive(request);
        if(!keepAlive){
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void sendResourceNotFound(ChannelHandlerContext ctx, MessageEvent e) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1,
                HttpResponseStatus.NOT_FOUND);
        response.setContent(ChannelBuffers.copiedBuffer("NOT FOUND!",
                CharsetUtil.UTF_8));
        response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

}

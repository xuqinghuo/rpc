package org.lib.rpc.server;

import static org.jboss.netty.channel.Channels.pipeline;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.lib.rpc.conf.SerivceConfig;
import org.lib.rpc.utils.NetKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by xuqinghuo on 2017/4/26.
 */
public class Server {
    private static Logger logger = LoggerFactory.getLogger(Server.class);
    private ServerBootstrap httpBootstrap = null;
    private SerivceConfig serivceConfig;
    private ChannelHandler channelHandler;
    public Server(ChannelHandler remoteJobContext,SerivceConfig serivceConfig){
        this.channelHandler = remoteJobContext;
        this.serivceConfig=serivceConfig;
    }
    public void start(){
        logger.info("server start on " + this.serivceConfig);
        initialize();
    }

    /**
     *
     */
    public void initialize(){
        ThreadFactory bossThreadFactory = new NamedThreadFactory("SERVER-BOSS-");
        ThreadFactory workerThreadFactory = new NamedThreadFactory("SERVER-WORK-");
        httpBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(bossThreadFactory),Executors.newCachedThreadPool(workerThreadFactory)));
        httpBootstrap.setOption("tcpNoDelay",serivceConfig.isTcpNoDelay());
        httpBootstrap.setOption("reuseAddress",serivceConfig.isReuseAddress());
        httpBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = pipeline();
                pipeline.addLast("decoder",new HttpRequestDecoder());
                pipeline.addLast("aggregator",new HttpChunkAggregator(1048576));
                pipeline.addLast("encoder",new HttpResponseEncoder());
                pipeline.addLast("deflater",new HttpContentCompressor());
                pipeline.addLast("handler",channelHandler);
                return pipeline;
            }
        });
        if(!NetKit.checkPortConfig(serivceConfig.getPort())){
            throw new IllegalStateException("端口:"+serivceConfig.getPort()+"被专用!");
        }
        httpBootstrap.bind(new InetSocketAddress(serivceConfig.getPort()));
    }

    public void stop(){
        logger.info("server stop");
        if(httpBootstrap != null){
            httpBootstrap.releaseExternalResources();
        }
    }
}

package org.lib.rpc.spring;

import org.lib.rpc.processor.SerivceExporter;
import org.lib.rpc.processor.Service;
import org.lib.rpc.conf.SerivceConfig;
import org.lib.rpc.protocol.hessian.HessionHandler;
import org.lib.rpc.server.NamedThreadFactory;
import org.lib.rpc.server.Server;
import org.lib.rpc.utils.ReflectKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


/**
 * Created by xuqinghuo on 2017/4/26.
 */
public class ProvidersBean implements  ApplicationContextAware,InitializingBean, DisposableBean,ApplicationListener<ContextRefreshedEvent>{

    private final Logger logger = LoggerFactory.getLogger(ProvidersBean.class);
    private boolean tcpNoDelay = true;
    private boolean reuseAddress = true;
    private int port=8081;
    private int corePoolSize=4;
    private int maxPoolSize=100;
    private int keepAliveTime=3000;
    private boolean token = false;
    private String contextRoot="rpc";
    private  static boolean isStarted=false;
    private Server server;
    private List<Bean> providerBeans = new ArrayList<Bean>();
    protected ApplicationContext applicationContext;
    protected SerivceConfig serivceConfig = SerivceConfig.insSerivceConfig();

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        System.out.println("3");
        this.applicationContext = applicationContext;

    }

    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        System.out.println("4");
       if(!isStarted){
           isStarted=true;
           startServer();
       }

    }

    public void destroy() throws Exception {
        System.out.println("5");
        if(server != null){
         server.stop();
        }
    }

    public void afterPropertiesSet() throws Exception {
        System.out.println("6");
       serivceConfig.setPort(this.getPort());
        serivceConfig.setContextRoot(this.getContextRoot());
        for(Bean bean:providerBeans){
            Service service = new Service();
            service.setId(bean.getBeanId());
            service.setName(bean.getBeanId());
            service.setOverload(bean.isOverload());
            service.setTypeClass(ReflectKit.forName(bean.getInterfaze()));
            Object object = applicationContext.getBean(bean.getRef());
            service.setProviderClass(object.getClass());
            service.setProvider(object);
            SerivceExporter.addToServiceMap(service);
        }
        initializeServer();

    }

    protected void initializeServer(){
        SerivceExporter.export();
        ThreadFactory threadFactory = new NamedThreadFactory(serivceConfig.getContextRoot()+"-PROCESS-");
        ExecutorService threadPoolExecutor = new ThreadPoolExecutor(serivceConfig.getCorePoolSize(),serivceConfig.getMaxPoolSize(),serivceConfig.getKeepAliveTime(), TimeUnit.SECONDS,new SynchronousQueue<Runnable>());
        HessionHandler handler = new HessionHandler(serivceConfig,threadPoolExecutor);
        server = new Server(handler,serivceConfig);
    }

    protected void startServer(){
        if(server != null){
            server.start();
        }
    }

    /**
     * @return the tcpNoDelay
     */
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    /**
     * @param tcpNoDelay the tcpNoDelay to set
     */
    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    /**
     * @return the reuseAddress
     */
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    /**
     * @param reuseAddress the reuseAddress to set
     */
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the corePoolSize
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * @param corePoolSize the corePoolSize to set
     */
    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    /**
     * @return the maxPoolSize
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * @param maxPoolSize the maxPoolSize to set
     */
    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * @return the keepAliveTime
     */
    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    /**
     * @param keepAliveTime the keepAliveTime to set
     */
    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    /**
     * @return the token
     */
    public boolean getToken() {
        return token;
    }

    /**
     * @param token the token to set
     */
    public void setToken(boolean token) {
        this.token = token;
    }

    /**
     * @return the contextRoot
     */
    public String getContextRoot() {
        return contextRoot;
    }

    /**
     * @param contextRoot the contextRoot to set
     */
    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    /**
     * @return the providerBeans
     */
    public List<Bean> getProviderBeans() {
        return providerBeans;
    }

    /**
     * @param providerBeans the providerBeans to set
     */
    public void setProviderBeans(List<Bean> providerBeans) {
        this.providerBeans = providerBeans;
    }


    /**
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }
}

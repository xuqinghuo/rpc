package org.lib.rpc.spring;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.remoting.caucho.HessianClientInterceptor;

/**
 * Created by xuqinghuo on 2017/4/26.
 */
public class ReferencesProxy extends HessianClientInterceptor implements FactoryBean<Object> {
    private Object serviceProxy;

    public void afterPropertiesSet(){
        super.afterPropertiesSet();
        this.serviceProxy = new ProxyFactory(getServiceInterface(),this).getProxy(getBeanClassLoader());
    }
    public Object getObject() throws Exception {
        return this.serviceProxy;
    }

    public Class<?> getObjectType() {
        return getServiceInterface();
    }

    public boolean isSingleton() {
        return true;
    }
}

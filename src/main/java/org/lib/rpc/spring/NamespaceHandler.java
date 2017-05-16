package org.lib.rpc.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Created by xuqinghuo on 2017/4/26.
 */
public class NamespaceHandler extends NamespaceHandlerSupport {


    public void init(){
        registerBeanDefinitionParser("providers", new RpcBeanDefinitionParser(ProvidersBean.class,true));


    }

}

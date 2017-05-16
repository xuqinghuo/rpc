package org.lib.rpc.spring;

import org.lib.rpc.utils.StringKit;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuqinghuo on 2017/4/26.
 */
public class RpcBeanDefinitionParser implements BeanDefinitionParser {
    private final Class<?> beanClass;
    private final boolean required;

    public RpcBeanDefinitionParser(Class<?> beanClass,boolean required){
        System.out.println("------------- 1--------");
        this.beanClass=beanClass;
        this.required=required;
    }
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        System.out.println("---------------------2-------------------");
        String id = element.getAttribute("id");
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);
        if(ProvidersBean.class.equals(beanClass)){

            List<Element> providers = DomUtils.getChildElementsByTagName(element,"provider");
            List<Bean> providerBeans = new ArrayList<Bean>();
            for(Element provider:providers){
                String providerId=provider.getAttribute("id");
                String interfaze = provider.getAttribute("interface");
                String ref = provider.getAttribute("ref");
                String asyn= provider.getAttribute("asyn");
                 Bean bean = new Bean();
                bean.setBeanId(providerId);
                bean.setInterfaze(interfaze);
                bean.setRef(ref);
               if(StringKit.isNotEmpty(asyn) && "true".equals(asyn)){
                   bean.setAsyn(true);
               }
                providerBeans.add(bean);
            }
            int port =Integer.valueOf(element.getAttribute("port"));
            String contextRoot ="rpc";// element.getAttribute("contextRoot");
            beanDefinition.getPropertyValues().addPropertyValue("port",port);
            beanDefinition.getPropertyValues().addPropertyValue("contextRoot",contextRoot);
            beanDefinition.getPropertyValues().addPropertyValue("providerBeans",providerBeans);
            parserContext.getRegistry().registerBeanDefinition(id,beanDefinition);
        }else if(ReferencesProxy.class.equals(beanClass)){
            String url = element.getAttribute("url");
            String user = element.getAttribute("user");
            String password = element.getAttribute("password");
            List<Element> references = DomUtils.getChildElementsByTagName(element,"reference");
            for(Element reference:references){
                String reference_id = reference.getAttribute("id");
                RootBeanDefinition referenced = new RootBeanDefinition();
                referenced.setBeanClass(ReferencesProxy.class);
                referenced.setLazyInit(false);
                referenced.getPropertyValues().addPropertyValue("serviceUrl",url+reference_id+"/");
                referenced.getPropertyValues().addPropertyValue("serviceInterface",reference.getAttribute("interface"));
                parserContext.getRegistry();
            }
        }
        return beanDefinition;
    }
}

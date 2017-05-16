package org;

import com.caucho.hessian.client.HessianProxyFactory;
import org.lib.rpc.service.HelloWorld;

import java.net.MalformedURLException;

/**
 * Created by xuqinghuo on 2017/4/27.
 */
public class TestHelloWorld {
    public static void main(String[] args) throws MalformedURLException {
        HessianProxyFactory proxyFactory = new HessianProxyFactory();
        HelloWorld helloWorld = (HelloWorld) proxyFactory.create(HelloWorld.class,"http://localhost:8081/rpc/helloWord");
        String re = helloWorld.hello("您好！！！！！");
        System.out.println(re);
    }
}

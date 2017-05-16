package org.lib.rpc.service.impl;

import org.lib.rpc.service.HelloWorld;

/**
 * Created by xuqinghuo on 2017/4/26.
 */
public class HelloWorldImpl implements HelloWorld {
    public String hello(String name) {
        return "这是服务端:"+name;
    }
}

package org.lib.rpc.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by xuqinghuo on 2017/4/26.
 */
public class SerivceExporter {
    public static final Map<String,Service> serviceMap = new HashMap<String,Service>();
    private static Map<String, ServiceMetaData> serviceMetaMap = new HashMap<String, ServiceMetaData>();


    public static void addToServiceMap(Service service){
        if(!isServiceExits(service.getName())){
            serviceMap.put(service.getName(), service);
        }
    }
    public static boolean isServiceExits(String serviceName){
        return serviceMap.containsKey(serviceName);
    }

    public static Map<String,Service> getServiceMap(){
        return Collections.unmodifiableMap(serviceMap);
    }
    public static Service getServiceByName(String serviceName){
        return serviceMap.get(serviceName);
    }


    public static void export() {
        Map<String, Service> serviceMap = SerivceExporter.getServiceMap();
        Set<String> serviceNames = serviceMap.keySet();
        for (String name : serviceNames) {
            Service service = serviceMap.get(name);
            Class<?> clazz = service.getTypeClass();
            ServiceMetaData smd = new ServiceMetaData(clazz,service.isOverload());
            serviceMetaMap.put(name, smd);
        }
    }

    public static ServiceMetaData getServiceMetaData(String sname) {
        return serviceMetaMap.get(sname);
    }
}

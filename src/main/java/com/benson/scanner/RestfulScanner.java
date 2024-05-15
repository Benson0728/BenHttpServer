package com.benson.scanner;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class RestfulScanner {
    public static MultiValueMap<Class<?>, Method> scan(Class<? extends Annotation> annotatedType) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        MultiValueMap<Class<?>,Method> map = new LinkedMultiValueMap<>();
        Set<Class<?>> controllerClass = ControllerScanner.scanController();
        for (Class<?> clazz : controllerClass) {
            //获取每个Controller的方法
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                //判断方法是否有@Post注解
                if (method.isAnnotationPresent(annotatedType)){
                    map.add(clazz,method);
                }
            }
        }
        return map;
    }
}

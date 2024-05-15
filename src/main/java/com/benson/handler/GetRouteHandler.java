package com.benson.handler;

import com.alibaba.fastjson2.JSON;
import com.benson.annotation.Get;
import com.benson.annotation.PathVariable;
import com.benson.annotation.RequestHeader;
import com.benson.annotation.RequestParam;
import com.benson.scanner.RestfulScanner;
import com.benson.util.PathVariableUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * 处理Get请求并路由的handler
 */
@Slf4j
public class GetRouteHandler extends SimpleChannelInboundHandler<HttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpRequest HttpRequest) throws Exception {
        //预设返回状态码为404
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpRequest.protocolVersion(), HttpResponseStatus.NOT_FOUND);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
        HttpHeaders headers = HttpRequest.headers();
        System.out.println("--------新请求---------");
        System.out.println("进入GetRouteHandler");
        if (HttpMethod.GET.equals(HttpRequest.method())) {  //如果是Get请求则执行
            System.out.println("处理");
            //获取请求路径
            String uriWithQuery = HttpRequest.uri();
            String[] parts = uriWithQuery.split("\\?");
            String uri = parts[0];
            System.out.println("请求路径:" + uri);

            //获取查询参数
            Map<String, String> queryParams = new HashMap<>();
            if (parts.length > 1) {
                String query = parts[1];  // 提取查询参数部分
                String[] params = query.split("&");  // 根据&分割参数
                for (String param : params) {
                    String[] keyValue = param.split("=");  // 根据=分割键值对
                    if (keyValue.length == 2) {
                        String key = URLDecoder.decode(keyValue[0], "UTF-8");
                        String value = URLDecoder.decode(keyValue[1], "UTF-8");
                        queryParams.put(key, value);
                    }
                }
            }

            //定义正则表达式判断注解uri是否含有路径参数
            Pattern pattern = Pattern.compile("\\{.*?\\}");

            //扫描所有包获得所有带有@Get注解的Controller方法
            MultiValueMap<Class<?>, Method> map = RestfulScanner.scan(Get.class);

            Object result;
            //遍历
            outerLoop:
            for (Map.Entry<Class<?>, List<Method>> entry : map.entrySet()) {
                List<Method> methods = entry.getValue();
                for (Method method : methods) {
                    Parameter[] parameters = method.getParameters(); //获取入参个数
                    //获取不带注解的参数数量
                    Integer paramsWithoutAnno = 0;
                    for (Parameter parameter : parameters) {
                        if(!parameter.isAnnotationPresent(PathVariable.class)&&!parameter.isAnnotationPresent(RequestHeader.class)){
                            paramsWithoutAnno++;
                        }
                    }
                    //查找路径匹配的方法
                    if (uri.equals(method.getAnnotation(Get.class).value())) {//不含PathVariable
                        if (paramsWithoutAnno != queryParams.size()) { //参数数量错误
                            System.out.println("数量不匹配");
                            response.setStatus(HttpResponseStatus.BAD_REQUEST);
                            break outerLoop;  //直接跳出最外层循环
                        }
                        //动态构造可变参数
                        Object[] args = new Object[parameters.length];
                        for (int i = 0; i < parameters.length; i++) {
                            //参数类型转换
                            if (parameters[i].isAnnotationPresent(RequestHeader.class)){
                                String value = headers.get(parameters[i].getAnnotation(RequestHeader.class).value());
                                args[i]=typeConvertor(parameters[i],value,args[i]);
                            }else if(parameters[i].isAnnotationPresent(RequestParam.class)){
                                String value = queryParams.get(parameters[i].getAnnotation(RequestParam.class).value());
                                args[i]=typeConvertor(parameters[i],value,args[i]);
                            }
                            else {
                                String value = queryParams.get(parameters[i].getName());
                                args[i]=typeConvertor(parameters[i],value,args[i]);
                            }
                        }
                        try {
                            result = method.invoke(entry.getKey().getConstructor().newInstance(), args);
                        } catch (IllegalArgumentException e) {
                            //参数类型不对 触发异常
                            e.printStackTrace();
                            response.setStatus(HttpResponseStatus.BAD_REQUEST);
                            break outerLoop;
                        }
                        doResponse(result, response);
                        break outerLoop; // 找到匹配的方法后立即退出循环
                    } else if (pattern.matcher(method.getAnnotation(Get.class).value()).find()) { //判断注解uri是否含有路径参数
                        //将注释路径和请求路径按"/"分割
                        String[] split1 = method.getAnnotation(Get.class).value().split("/");
                        String[] split2 = uri.split("/");
                        //路径层数不一样直接跳出
                        if (split1.length != split2.length) continue;
                        int pathVariableNums = PathVariableUtils.getPathVariableNums(method.getAnnotation(Get.class).value());
                        //路径参数个数不一样直接跳出
                        if (PathVariableUtils.splitCompare(split1, split2) != pathVariableNums) continue;
                        List<Integer> indexList = PathVariableUtils.getSplitDifferenceIndex(split1, split2);
                        //获取路径参数对应的值
                        Map<String, String> pathVariableMap = new HashMap<>();
                        List<String> pathVariableName = PathVariableUtils.getPathVariables(method.getAnnotation(Get.class).value());
                        for (Integer integer : indexList) {
                            int index = 0;
                            pathVariableMap.put(pathVariableName.get(index), split2[integer]);
                        }
                        //动态构造参数
                        Object[] args = new Object[parameters.length];
                        for (int i = 0; i < parameters.length; i++) {
                            if (parameters[i].isAnnotationPresent(PathVariable.class)) {
                                String value = pathVariableMap.get(parameters[i].getAnnotation(PathVariable.class).value());
                                args[i]=typeConvertor(parameters[i],value,args[i]);
                            } else if(parameters[i].isAnnotationPresent(RequestHeader.class)){
                                String value = headers.get(parameters[i].getAnnotation(RequestHeader.class).value());
                                args[i]=typeConvertor(parameters[i],value,args[i]);
                            }
                            else if(parameters[i].isAnnotationPresent(RequestParam.class)){
                                String value = queryParams.get(parameters[i].getAnnotation(RequestParam.class).value());
                                args[i]=typeConvertor(parameters[i],value,args[i]);
                            }
                            else{
                                String value = queryParams.get(parameters[i].getName());
                                args[i]=typeConvertor(parameters[i],value,args[i]);
                            }
                        }
                        try {
                            result = method.invoke(entry.getKey().getConstructor().newInstance(), args);
                        } catch (IllegalArgumentException e) {
                            //参数类型不对 触发异常
                            e.printStackTrace();
                            response.setStatus(HttpResponseStatus.BAD_REQUEST);
                            break outerLoop;
                        }
                        doResponse(result, response);
                        break outerLoop; // 找到匹配的方法后立即退出循环
                    }
                }
            }
            channelHandlerContext.writeAndFlush(response);
        }else{
            response.release();
            channelHandlerContext.fireChannelRead(HttpRequest);
        }
    }


    private static void doResponse(Object result, DefaultFullHttpResponse response) {
        if (result != null) {
            String jsonString = JSON.toJSONString(result);
            byte[] bytes = jsonString.getBytes();
            // json格式；设置字符集编码为 UTF-8
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json; charset=UTF-8");
            //状态码
            response.setStatus(HttpResponseStatus.OK);
            //设置响应长度
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
            response.content().writeBytes(bytes);
        }
    }

    private static Object typeConvertor(Parameter parameter,String value,Object arg){
        if (parameter.getType() == Integer.class){
            arg = Integer.parseInt(value);
        }else if (parameter.getType() == Long.class){
            arg = Long.parseLong(value);
        }else arg  = value;
        return arg;
    }

}

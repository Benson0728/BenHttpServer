package com.benson.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.benson.annotation.*;
import com.benson.dto.FormDataItem;
import com.benson.scanner.RestfulScanner;
import com.benson.util.FormDataToMultipartFileAdapter;
import com.benson.util.PathVariableUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;


public class PostRouteHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {
        System.out.println("进入PostRouteHandler");
        //预设返回状态码为404
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(fullHttpRequest.protocolVersion(), HttpResponseStatus.NOT_FOUND);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
        if (fullHttpRequest.method() == HttpMethod.POST) { //如果是Post请求则处理
            System.out.println("处理");
            //获取uri
            String uri = fullHttpRequest.uri();
            HttpHeaders headers = fullHttpRequest.headers();
            System.out.println("请求路径：" + uri);
            //处理请求体
            String contentType = headers.get(HttpHeaderNames.CONTENT_TYPE);
            ByteBuf content = fullHttpRequest.content();
            System.out.println(content.toString(StandardCharsets.UTF_8));

            if (contentType == null) {
                response.setStatus(HttpResponseStatus.BAD_REQUEST);
                return;
            } else if (!contentType.contains("application/json") && !contentType.contains("multipart/form-data")) {
                response.setStatus(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                return;
            }

            //定义正则表达式判断注解uri是否含有路径参数
            Pattern pattern = Pattern.compile("\\{.*?\\}");

            //扫描所有Controller包下带有@Post的方法
            MultiValueMap<Class<?>, Method> map = RestfulScanner.scan(Post.class);

            Object result;
            AtomicBoolean expectFormData = new AtomicBoolean(false);
            AtomicBoolean expectJson = new AtomicBoolean(false);

            //遍历
            outerLoop:
            for (Map.Entry<Class<?>, List<Method>> entry : map.entrySet()) {
                List<Method> methods = entry.getValue();
                for (Method method : methods) {
                    //获取参数
                    Parameter[] parameters = method.getParameters();
                    if (method.getAnnotation(Post.class).value().equals(uri)) { //不含PathVariable
                        AtomicBoolean isLegal = legitimacyChecks(parameters, contentType, response, expectJson, expectFormData);
                        if (!isLegal.get()) return;
                        if (expectFormData.get()) { //form-data格式
                            try {
                                result = handleFormData(content, entry, method,headers);
                            } catch (Exception e) {
                                e.printStackTrace();
                                response.setStatus(HttpResponseStatus.BAD_REQUEST);
                                break outerLoop;
                            }
                            doResponse(result, response);
                            break outerLoop;
                        }
                        if (expectJson.get()) { //json格式
                            try {
                                result = handleJsonBody(content, entry, method,headers);
                            } catch (Exception e) {
                                e.printStackTrace();
                                response.setStatus(HttpResponseStatus.BAD_REQUEST);
                                break outerLoop;
                            }
                            doResponse(result, response);
                            break outerLoop;
                        }
                    } else if (pattern.matcher(method.getAnnotation(Post.class).value()).find()) {
                        //将注释路径和请求路径按"/"分割
                        String[] split1 = method.getAnnotation(Post.class).value().split("/");
                        String[] split2 = uri.split("/");
                        //路径层数不一样直接跳出
                        if (split1.length != split2.length) continue;
                        int pathVariableNums = PathVariableUtils.getPathVariableNums(method.getAnnotation(Post.class).value());
                        //路径参数个数不一样直接跳出
                        if (PathVariableUtils.splitCompare(split1, split2) != pathVariableNums) continue;
                        List<Integer> indexList = PathVariableUtils.getSplitDifferenceIndex(split1, split2);
                        //获取路径参数对应的值
                        Map<String, String> pathVariableMap = new HashMap<>();
                        List<String> pathVariableName = PathVariableUtils.getPathVariables(method.getAnnotation(Post.class).value());
                        for (Integer integer : indexList) {
                            int index = 0;
                            pathVariableMap.put(pathVariableName.get(index), split2[integer]);
                        }

                        AtomicBoolean isLegal = legitimacyChecks(parameters, contentType, response, expectJson, expectFormData);
                        if (!isLegal.get()) return;

                        if (expectFormData.get()) { //form-data格式
                            try {
                                result = handleFormData(content, entry, method,pathVariableMap,headers);
                            } catch (Exception e) {
                                e.printStackTrace();
                                response.setStatus(HttpResponseStatus.BAD_REQUEST);
                                break outerLoop;
                            }
                            doResponse(result, response);
                            break outerLoop;
                        }
                        if (expectJson.get()) { //json格式
                            try {
                                result = handleJsonBody(content, entry, method,pathVariableMap,headers);
                            } catch (Exception e) {
                                e.printStackTrace();
                                response.setStatus(HttpResponseStatus.BAD_REQUEST);
                                break outerLoop;
                            }
                            doResponse(result, response);
                            break outerLoop;
                        }
                    }
                }
            }
            channelHandlerContext.writeAndFlush(response);
        }else {
            fullHttpRequest.retain();
            response.release();
            channelHandlerContext.fireChannelRead(fullHttpRequest);
        }
    }

    /**
     * 不含PathVariable
     */
    private static Object handleJsonBody(ByteBuf content, Map.Entry<Class<?>, List<Method>> entry, Method method,HttpHeaders headers) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(RequestBody.class)){
                String jsonString = content.toString(StandardCharsets.UTF_8);
                JSONObject jsonObject = JSON.parseObject(jsonString);
                args[i] = jsonObject;
            }else if (parameters[i].isAnnotationPresent(RequestHeader.class)){
                String value = headers.get(parameters[i].getAnnotation(RequestHeader.class).value());
                args[i]=typeConvertor(parameters[i],value,args[i]);
            }
        }
        return method.invoke(entry.getKey().getConstructor().newInstance(), args);
    }

    /**
     * 不含PathVariable
     */
    private static Object handleFormData(ByteBuf content,Map.Entry<Class<?>, List<Method>> entry,Method method,HttpHeaders headers) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Parameter[] parameters = method.getParameters();
        String contentString = content.toString(StandardCharsets.UTF_8);
        Map<String, FormDataItem> formData = parseFormData(contentString);

        //动态构造参数
        Object[] args = new Object[parameters.length];
        for (int i = 0;i < parameters.length;i++) {
            if (parameters[i].isAnnotationPresent(RequestParam.class)){
                FormDataItem formDataItem = formData.get(parameters[i].getAnnotation(RequestParam.class).value());
                if (parameters[i].getType() == MultipartFile.class){
                    MultipartFile multipartFile = new FormDataToMultipartFileAdapter(parameters[i].getAnnotation(RequestParam.class).value(),formDataItem);
                    args[i] = multipartFile;
                }else{
                    args[i]=typeConvertor(parameters[i],formDataItem.getValue(),args[i]);
                }
            }else if (parameters[i].isAnnotationPresent(RequestHeader.class)){
                String value = headers.get(parameters[i].getAnnotation(RequestHeader.class).value());
                args[i]=typeConvertor(parameters[i],value,args[i]);
            }else {
                String value = formData.get(parameters[i].getName()).getValue();
                args[i]=typeConvertor(parameters[i],value,args[i]);
            }
        }
        return method.invoke(entry.getKey().getConstructor().newInstance(), args);
    }

    /**
     * 含PathVariable
     */
    private static Object handleJsonBody(ByteBuf content, Map.Entry<Class<?>, List<Method>> entry, Method method,Map<String,String> pathVariables,HttpHeaders headers) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(RequestBody.class)){
                String jsonString = content.toString(StandardCharsets.UTF_8);
                JSONObject jsonObject = JSON.parseObject(jsonString);
                args[i] = jsonObject;
            }else if (parameters[i].isAnnotationPresent(PathVariable.class)){
                String value = pathVariables.get(parameters[i].getAnnotation(PathVariable.class).value());
                args[i]=typeConvertor(parameters[i],value,args[i]);
            }else if (parameters[i].isAnnotationPresent(RequestHeader.class)){
                String value = headers.get(parameters[i].getAnnotation(RequestHeader.class).value());
                args[i]=typeConvertor(parameters[i],value,args[i]);
            }
        }
        return method.invoke(entry.getKey().getConstructor().newInstance(), args);
    }

    /**
     * 含PathVariable
     */
    private static Object handleFormData(ByteBuf content,Map.Entry<Class<?>, List<Method>> entry,Method method,Map<String,String> pathVariables,HttpHeaders headers) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Parameter[] parameters = method.getParameters();
        String contentString = content.toString(StandardCharsets.UTF_8);
        Map<String, FormDataItem> formData = parseFormData(contentString);
        //动态构造参数
        Object[] args = new Object[parameters.length];
        for (int i = 0;i < parameters.length;i++) {
            if (parameters[i].isAnnotationPresent(RequestParam.class)){
                FormDataItem formDataItem = formData.get(parameters[i].getAnnotation(RequestParam.class).value());
                if (parameters[i].getType() == MultipartFile.class){
                    MultipartFile multipartFile = new FormDataToMultipartFileAdapter(parameters[i].getAnnotation(RequestParam.class).value(),formDataItem);
                    args[i] = multipartFile;
                }else{
                    args[i]=typeConvertor(parameters[i],formDataItem.getValue(),args[i]);
                }
            }else if (parameters[i].isAnnotationPresent(RequestHeader.class)){
                String value = headers.get(parameters[i].getAnnotation(RequestHeader.class).value());
                args[i]=typeConvertor(parameters[i],value,args[i]);
            }else if (parameters[i].isAnnotationPresent(PathVariable.class)){
                String value = pathVariables.get(parameters[i].getAnnotation(PathVariable.class).value());
                args[i]=typeConvertor(parameters[i],value,args[i]);
            } else {
                String value = formData.get(parameters[i].getName()).getValue();
                args[i]=typeConvertor(parameters[i],value,args[i]);
            }
        }
        return method.invoke(entry.getKey().getConstructor().newInstance(), args);
    }


    private static void doResponse(Object result, DefaultFullHttpResponse response) {
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

    /**
     * 解析form-data
     */
    public static Map<String, FormDataItem> parseFormData(String content) {
        Map<String, FormDataItem> formData = new HashMap<>();
        String[] parts = content.split("--");
        for (String part : parts) {
            if (part.trim().isEmpty()) {
                continue; // 跳过空部分
            }
            String[] lines = part.trim().split("\r\n");
            String key = null;
            String value = null;
            String filename = null;
            String contentType = null;
            for (String line : lines) {
                if (line.startsWith("Content-Disposition")) {
                    String[] headerParts = line.split("; ");
                    for (String headerPart : headerParts) {
                        if (headerPart.trim().startsWith("name=")) {
                            key = headerPart.trim().substring("name=\"".length(), headerPart.trim().length() - 1);
                        } else if (headerPart.trim().startsWith("filename=")) {
                            filename = headerPart.trim().substring("filename=\"".length(), headerPart.trim().length() - 1);
                        }
                    }
                } else if (line.startsWith("Content-Type")) {
                    contentType = line.split(":")[1].trim();
                } else if (!line.isEmpty()) {
                    value = line;
                }
            }
            if (key != null) {
                if (filename != null) {
                    // 文件类型参数
                    formData.put(key, new FormDataItem(value, contentType, filename));
                } else {
                    // 文本类型参数
                    formData.put(key, new FormDataItem(value));
                }
            }
        }
        return formData;
    }

    /**
     * 检查方法合法性
     */
    private static AtomicBoolean legitimacyChecks(Parameter[] parameters, String contentType, DefaultFullHttpResponse response, AtomicBoolean expectJson, AtomicBoolean expectFormData){
        AtomicBoolean isLegal = new AtomicBoolean(true);
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(RequestParam.class)) expectFormData.set(true);
            if (parameter.getAnnotations().length == 0) expectFormData.set(true);
            if (parameter.isAnnotationPresent(RequestBody.class)) expectJson.set(true);
        }

        if (expectJson.get() && expectFormData.get()) {
            response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            isLegal.set(false);
        } else if (expectJson.get() && contentType.contains("multipart/form-data")) {
            response.setStatus(HttpResponseStatus.BAD_REQUEST);
            isLegal.set(false);
        } else if (expectFormData.get() && contentType.contains("application/json")) {
            response.setStatus(HttpResponseStatus.BAD_REQUEST);
            isLegal.set(false);
        }
        return isLegal;
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

package com.benson.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathVariableUtils {
    /**
     * 获取注解路径中路径参数的个数
     *
     * @param uri
     * @return
     */
    public static int getPathVariableNums(String uri) {
        int count = 0;
        Pattern pattern = Pattern.compile("\\{.*?\\}"); // 匹配形如 {parameter} 的字符串
        Matcher matcher = pattern.matcher(uri);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 获取注解路径和请求路径不同部分的数量
     *
     * @param split1
     * @param split2
     * @return
     */
    public static int splitCompare(String[] split1, String[] split2) {
        int count = 0;
        for (int i = 0; i < split1.length; i++) {
            if (!split1[i].equals(split2[i])) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取路径参数在路径中的index
     *
     * @param split1
     * @param split2
     * @return
     */
    public static List<Integer> getSplitDifferenceIndex(String[] split1, String[] split2) {
        List<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < split1.length; i++) {
            if (split1[i] != split2[i]) indexList.add(i);
        }
        return indexList;
    }

    /**
     * 获取路径参数的名字
     *
     * @param uri
     * @return
     */
    public static List<String> getPathVariables(String uri) {
        Pattern pattern = Pattern.compile("\\{.*?\\}"); // 匹配形如 {parameter} 的字符串
        Matcher matcher = pattern.matcher(uri);
        String str;
        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            str = matcher.group();
            list.add(str.substring(1, str.length() - 1));
        }
        return list;
    }
}

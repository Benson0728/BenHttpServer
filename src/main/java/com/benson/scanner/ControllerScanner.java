package com.benson.scanner;

import com.benson.annotation.MyController;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * 扫描当前模块所有包 筛选出有@MyController的类
 */
public class ControllerScanner {
    public static Set<Class<?>> scanController() {
        Set<Class<?>> controllerClasses = scanForMyControllerClasses();
        return controllerClasses;
    }

    public static Set<Class<?>> scanForMyControllerClasses() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(MyController.class));

        Set<Class<?>> controllerClasses = new HashSet<>();
        for (String basePackage : getBasePackages()) {
            for (BeanDefinition beanDefinition : scanner.findCandidateComponents(basePackage)) {
                try {
                    controllerClasses.add(Class.forName(beanDefinition.getBeanClassName()));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return controllerClasses;
    }

    // 获取当前模块下的所有.java文件所在的基础包
    private static Set<String> getBasePackages() {
        Set<String> basePackages = new HashSet<>();
        String currentWorkingDir = System.getProperty("user.dir");
        String sourceDir = currentWorkingDir + "/src/main/java";
        File sourceDirFile = new File(sourceDir);
        if (sourceDirFile.exists() && sourceDirFile.isDirectory()) {
            scanDirectoryForPackages(sourceDirFile, "", basePackages);
        }
        return basePackages;
    }

    private static void scanDirectoryForPackages(File directory, String parentPackage, Set<String> basePackages) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                String packageName = parentPackage.isEmpty() ? file.getName() : parentPackage + "." + file.getName();
                scanDirectoryForPackages(file, packageName, basePackages);
            } else if (file.getName().endsWith(".java")) {
                // 获取当前文件所在的包名
                String packageName = parentPackage.isEmpty() ? "" : parentPackage.replace("/", ".").substring(0);
                basePackages.add(packageName);
            }
        }
    }
}
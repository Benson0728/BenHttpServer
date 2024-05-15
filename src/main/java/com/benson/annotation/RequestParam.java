package com.benson.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE_PARAMETER})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {
    String value();
}

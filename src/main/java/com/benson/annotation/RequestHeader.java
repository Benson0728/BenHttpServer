package com.benson.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE_PARAMETER,ElementType.PARAMETER})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestHeader {
    String value();
}

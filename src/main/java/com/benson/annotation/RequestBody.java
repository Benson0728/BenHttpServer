package com.benson.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.PARAMETER, ElementType.TYPE_PARAMETER})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestBody {
}

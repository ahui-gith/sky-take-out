package com.sky.annotation;

import com.sky.enumeration.OperationType;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于标识需要自动填充的字段
 */
@Target(ElementType.METHOD) // 表示该注解只能用于方法上
@Retention(RetentionPolicy.RUNTIME) // 表示该注解在运行时保留
public @interface AutoFill {
    // 填充的字段 OperationType枚举
    OperationType value(); // 默认值是插入
}

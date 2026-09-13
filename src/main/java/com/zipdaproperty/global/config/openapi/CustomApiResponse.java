package com.zipdaproperty.global.config.openapi;

import com.zipdaproperty.global.response.constant.CustomResponseCode;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomApiResponse {

    CustomResponseCode[] value();

    String successResponseCode() default "200";

    String successDescription() default "SUCCESS";
}

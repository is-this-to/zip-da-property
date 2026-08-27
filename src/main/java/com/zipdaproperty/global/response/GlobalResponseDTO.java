package com.zipdaproperty.global.response;

import com.zipdaproperty.global.context.TraceIdContext;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public record GlobalResponseDTO<T>(
        String code,
        String message,
        T data,
        String traceId
) {

     public static <T> GlobalResponseDTO<T> from(
             CustomResponseCode customResponseCode,
             T data
     ) {
          return new GlobalResponseDTO<>(
                  customResponseCode.getCode(),
                  customResponseCode.name(),
                  data,
                  TraceIdContext.getOrCreate()
          );
     }

     public static GlobalResponseDTO<Void> from(
             CustomResponseCode customResponseCode
     ) {
          return new GlobalResponseDTO<>(
                  customResponseCode.getCode(),
                  customResponseCode.name(),
                  null,
                  TraceIdContext.getOrCreate()
          );
     }

     public static <T> GlobalResponseDTO<T> success(T data) {
          return from(
                  CustomResponseCode.SUCCESS,
                  data
          );
     }

     public static GlobalResponseDTO<Void> success() {
          return from(CustomResponseCode.SUCCESS);
     }
}
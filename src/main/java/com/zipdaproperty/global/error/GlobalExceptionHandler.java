package com.zipdaproperty.global.error;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.FieldErrorDTO;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<GlobalResponseDTO<Void>> generateErrorResponse(
            CustomResponseCode customResponseCode
    ) {
        return ResponseEntity
                .status(customResponseCode.getHttpStatus())
                .body(GlobalResponseDTO.from(customResponseCode));
    }

    private <T> ResponseEntity<GlobalResponseDTO<T>> generateErrorResponse(
            CustomResponseCode customResponseCode,
            T data
    ) {
        return ResponseEntity
                .status(customResponseCode.getHttpStatus())
                .body(GlobalResponseDTO.from(
                        customResponseCode,
                        data
                ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleBusinessException(
            BusinessException exception
    ) {
        log.warn(
                "{}: {}",
                exception.getCustomResponseCode().name(),
                exception.getMessage()
        );

        return generateErrorResponse(
                exception.getCustomResponseCode()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException exception
    ) {
        log.debug(
                "{}: invalid parameter={}",
                CustomResponseCode.INVALID_REQUEST.name(),
                exception.getName()
        );

        return generateErrorResponse(
                CustomResponseCode.INVALID_REQUEST
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleMessageNotReadableException(
            HttpMessageNotReadableException exception
    ) {
        log.debug(
                "{}: request body is not readable",
                CustomResponseCode.INVALID_REQUEST.name()
        );

        return generateErrorResponse(
                CustomResponseCode.INVALID_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponseDTO<List<FieldErrorDTO>>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        List<FieldErrorDTO> fieldErrors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new FieldErrorDTO(
                        fieldError.getField(),
                        fieldError.getDefaultMessage() == null
                                ? "유효하지 않은 값입니다."
                                : fieldError.getDefaultMessage()
                ))
                .toList();

        log.debug(
                "{}: invalid fields={}",
                CustomResponseCode.INVALID_REQUEST.name(),
                fieldErrors
                        .stream()
                        .map(FieldErrorDTO::field)
                        .toList()
        );

        return generateErrorResponse(
                CustomResponseCode.INVALID_REQUEST,
                fieldErrors
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleMethodValidationException(
            HandlerMethodValidationException exception
    ) {
        log.debug(
                "{}: method parameter validation failed",
                CustomResponseCode.INVALID_REQUEST.name()
        );

        return generateErrorResponse(
                CustomResponseCode.INVALID_REQUEST
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleConstraintViolationException(
            ConstraintViolationException exception
    ) {
        log.debug(
                "{}: constraint validation failed",
                CustomResponseCode.INVALID_REQUEST.name()
        );

        return generateErrorResponse(
                CustomResponseCode.INVALID_REQUEST
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleNoResourceFoundException(
            NoResourceFoundException exception
    ) {
        log.debug(
                "{}: resourcePath={}",
                CustomResponseCode.NOT_FOUND_RESOURCE.name(),
                exception.getResourcePath()
        );

        return generateErrorResponse(
                CustomResponseCode.NOT_FOUND_RESOURCE
        );
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleDuplicateKeyException(
            DuplicateKeyException exception
    ) {
        log.error(
                "DB duplicate key error",
                exception
        );

        return generateErrorResponse(
                CustomResponseCode.DB_DUPLICATED_KEY_ERROR
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleDataAccessException(
            DataAccessException exception
    ) {
        log.error(
                "Database access error",
                exception
        );

        return generateErrorResponse(
                CustomResponseCode.DB_ERROR
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleException(
            Exception exception
    ) {
        log.error(
                "Unexpected system error",
                exception
        );

        return generateErrorResponse(
                CustomResponseCode.SYSTEM_ERROR
        );
    }
}
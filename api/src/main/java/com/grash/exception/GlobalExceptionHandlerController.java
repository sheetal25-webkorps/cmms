package com.grash.exception;

import lombok.extern.slf4j.Slf4j;
import com.grash.advancedsearch.InvalidSearchFieldException;
import com.grash.dto.SuccessResponse;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.validation.FieldError;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.xml.bind.ValidationException;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandlerController {

    @Bean
    public ErrorAttributes errorAttributes() {
        // Hide exception field in the return object
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
                return super.getErrorAttributes(webRequest,
                        ErrorAttributeOptions.defaults().excluding(ErrorAttributeOptions.Include.EXCEPTION));
            }
        };
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<SuccessResponse> handleCustomException(HttpServletResponse res, CustomException ex) {
        ex.printStackTrace();
        return new ResponseEntity<>(new SuccessResponse(false, ex.getMessage()), ex.getHttpStatus());
    }

    @ExceptionHandler(InvalidSearchFieldException.class)
    public ResponseEntity<SuccessResponse> handleInvalidSearchField(InvalidSearchFieldException ex) {
        return new ResponseEntity<>(new SuccessResponse(false, ex.getClientMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<SuccessResponse> handleAccessDeniedException(HttpServletResponse res) {
        return new ResponseEntity<>(new SuccessResponse(false, "Access is denied"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<SuccessResponse> handleHttpRequestMethodNotSupportedException(HttpServletResponse res,
                                                                                        Exception ex) {
        return new ResponseEntity<>(new SuccessResponse(false, ex.getMessage()), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    ResponseEntity<SuccessResponse> handleValidationException(ValidationException ex) {
        return new ResponseEntity<>(new SuccessResponse(false, ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<SuccessResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(GlobalExceptionHandlerController::describeValidationError)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        if (message.isEmpty()) {
            message = "Validation failed";
        }
        return new ResponseEntity<>(new SuccessResponse(false, message), HttpStatus.BAD_REQUEST);
    }

    /**
     * Built-in constraint messages ("must not be null") don't say which field failed, so they are
     * prefixed with the field name. Custom, already descriptive messages are kept as-is.
     */
    private static String describeValidationError(ObjectError error) {
        String message = error.getDefaultMessage();
        if (message != null && message.startsWith("must ") && error instanceof FieldError fieldError) {
            return fieldError.getField() + " " + message;
        }
        return message;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<SuccessResponse> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        return new ResponseEntity<>(
                new SuccessResponse(false, "The resource was modified by another request. Please refresh and retry."),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<SuccessResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return new ResponseEntity<>(new SuccessResponse(false, "Resource not found"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<SuccessResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining(", "));
        return new ResponseEntity<>(new SuccessResponse(false, message.isEmpty() ? "Validation failed" : message),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<SuccessResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return new ResponseEntity<>(new SuccessResponse(false,
                "This action conflicts with existing data. The record may be referenced by other records or " +
                        "contain a duplicate value."), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SuccessResponse> handleException(HttpServletResponse res, Exception ex) {
        // Full details go to the server log only; clients get a generic message (no internals/stack traces).
        log.error("Unhandled exception", ex);
        return new ResponseEntity<>(new SuccessResponse(false,
                "An unexpected error occurred. Please try again or contact your administrator."),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

}


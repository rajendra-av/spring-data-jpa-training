package com.springboot.training.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    // Runs after HandlerMapping, before the controller method
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.debug("interceptor: preHandle {} {}", request.getMethod(), request.getRequestURI());

        // For controller methods the handler is a HandlerMethod, so we know which one will run
        if (handler instanceof HandlerMethod handlerMethod) {
            log.debug("interceptor: handler is {}.{}",
                    handlerMethod.getBeanType().getSimpleName(), handlerMethod.getMethod().getName());
        }
        return true;   // false would stop the request here, and we would have to write the response ourselves
    }

    // Runs only if the controller returned normally. For a @RestController the body is
    // already written by now, so this is too late to change the response.
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        log.debug("interceptor: postHandle");
    }

    // Always runs after the request completes. ex is null when the controller succeeded,
    // and also when an @ExceptionHandler already handled the exception.
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        log.debug("interceptor: afterCompletion status={}", response.getStatus());
    }
}

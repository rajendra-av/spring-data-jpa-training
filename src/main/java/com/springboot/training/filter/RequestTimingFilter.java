package com.springboot.training.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
// Annotation of @Component and making as bean is just enough in sprig boot
// No explicit filter registration is needed
public class RequestTimingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);

    // OncePerRequestFilter calls this once per request, even for forwards or async dispatches
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // "In" half: runs before the request reaches DispatcherServlet
        long start = System.nanoTime();
        log.debug("filter: before {} {}", request.getMethod(), request.getRequestURI());

        try {
            // Hands the request to the next filter, then DispatcherServlet, interceptors,
            // controller and so on. Not calling this method would stop the request here.
            chain.doFilter(request, response);
        } finally {
            // "Out" half: runs after the response has been produced. In a finally block
            // so the timing is logged even if something further down throws.
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.debug("filter: after {} {} -> {} in {} ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
        }
    }
}
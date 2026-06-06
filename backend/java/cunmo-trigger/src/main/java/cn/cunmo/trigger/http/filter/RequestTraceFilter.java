package cn.cunmo.trigger.http.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 为每个 HTTP 请求建立 traceId，并记录请求总耗时。
 */
@Component
public class RequestTraceFilter extends OncePerRequestFilter {
    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String TRACE_MDC_KEY = "traceId";

    private static final Logger log =
            LoggerFactory.getLogger(RequestTraceFilter.class);
    private static final Pattern SAFE_TRACE_ID =
            Pattern.compile("[A-Za-z0-9_-]{8,64}");

    /**
     * 建立请求 traceId、记录请求耗时并在结束后清理 MDC。
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TRACE_HEADER));
        long startedAt = System.nanoTime();
        MDC.put(TRACE_MDC_KEY, traceId);
        response.setHeader(TRACE_HEADER, traceId);

        log.info(
                "event=http_request stage=started method={} path={}",
                request.getMethod(),
                request.getRequestURI());
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info(
                    "event=http_request stage=completed method={} path={} status={} elapsedMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - startedAt));
            MDC.remove(TRACE_MDC_KEY);
        }
    }

    /**
     * 校验调用方 traceId；无效时生成新的安全标识。
     */
    private String resolveTraceId(String candidate) {
        if (candidate != null && SAFE_TRACE_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}

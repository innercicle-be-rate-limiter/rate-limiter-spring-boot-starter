package com.innercicle.aop;

import com.innercicle.advice.exceptions.LockAcquisitionFailureException;
import com.innercicle.annotations.RateLimiting;
import com.innercicle.domain.AbstractTokenInfo;
import com.innercicle.handler.RateLimitHandler;
import com.innercicle.lock.LockManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class RateLimitAop {

    private final RateLimitingProperties rateLimitingProperties;
    private final LockManager lockManager;
    private final RateLimitHandler rateLimitHandler;

    @Around("@annotation(com.innercicle.annotations.RateLimiting)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!rateLimitingProperties.isEnabled()) {
            return joinPoint.proceed();
        }
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimiting rateLimiting = method.getAnnotation(RateLimiting.class);
        if (rateLimiting == null) {
            // 어노테이션이 없는 경우 처리하지 않음
            return joinPoint.proceed();
        }
        String lockKey =
            method.getName() + CustomSpringELParser.getDynamicValue(signature.getParameterNames(),
                                                                    joinPoint.getArgs(),
                                                                    rateLimiting.cacheKey());

        lockManager.getLock(lockKey);

        try {
            boolean lockable = lockManager.tryLock(rateLimiting);
            if (!lockable) {
                log.error("Lock 획득 실패={}", lockKey);
                throw new LockAcquisitionFailureException("Lock 획득 실패했습니다.");
            }
            log.debug("{} lock 시작", this.getClass().getName());
            String cacheKey = "cache-".concat(lockKey);

            AbstractTokenInfo tokenBucketInfo = rateLimitHandler.allowRequest(cacheKey);

            Object proceed = joinPoint.proceed();
            tokenBucketInfo.endProcess();

            setResponseHeader(tokenBucketInfo);

            return proceed;
        } catch (InterruptedException e) {
            log.error("에러 발생 : {}", e.getMessage());
            throw e;
        } finally {
            log.debug("{} lock 해제", this.getClass().getName());
            lockManager.unlock();
        }
    }

    private static void setResponseHeader(AbstractTokenInfo tokenBucketInfo) {
        HttpServletResponse response = ((ServletRequestAttributes)(RequestContextHolder.currentRequestAttributes())).getResponse();
        if (response != null) {
            response.setIntHeader("X-Ratelimit-Remaining", tokenBucketInfo.getRemaining());
            response.setIntHeader("X-Ratelimit-Limit", tokenBucketInfo.getLimit());
            response.setIntHeader("X-Ratelimit-Retry-After", tokenBucketInfo.getRetryAfter());
        }
    }

}

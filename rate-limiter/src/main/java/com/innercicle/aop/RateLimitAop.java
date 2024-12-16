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

    /**
     * <h2>RateLimiting 어노테이션을 이용한 Rate Limiting 처리</h2>
     * - RateLimiting 어노테이션이 붙은 메소드에 대한 Rate Limiting 처리 <br/>
     * - enable/disable 설정에 따라 Rate Limiting 처리 여부 결정 {@link RateLimitingProperties#isEnabled()} <br/>
     *
     * @param joinPoint : AspectJ JoinPoint
     * @return Object : 메소드 실행 결과
     */
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
        String lockKey = getLockAndLockKey(joinPoint, method, signature, rateLimiting);

        try {
            tryLock(rateLimiting, lockKey);

            String cacheKey = "cache-".concat(lockKey);

            AbstractTokenInfo tokenBucketInfo = rateLimitHandler.allowRequest(cacheKey);

            Object proceed = joinPoint.proceed();

            rateLimitHandler.endRequest();
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

    private void tryLock(RateLimiting rateLimiting, String lockKey) throws InterruptedException {
        boolean lockable = lockManager.tryLock(rateLimiting);
        if (!lockable) {
            log.error("Lock 획득 실패={}", lockKey);
            throw new LockAcquisitionFailureException("Lock 획득 실패했습니다.");
        }
        log.debug("{} lock 시작", this.getClass().getName());
    }

    /**
     * <h2>Lock 획득 및 Lock Key 생성</h2>
     * - Lock 획득 <br/>
     * - Lock Key 생성
     *
     * @return Lock Key
     */
    private String getLockAndLockKey(ProceedingJoinPoint joinPoint, Method method, MethodSignature signature, RateLimiting rateLimiting) {
        String lockKey =
            method.getName() + CustomSpringELParser.getDynamicValue(signature.getParameterNames(),
                                                                    joinPoint.getArgs(),
                                                                    rateLimiting.cacheKey());

        lockManager.getLock(lockKey);
        return lockKey;
    }

    /**
     * <h2>클라이언트에게 회신할 response 정보 세팅</h2>
     * - X-Ratelimit-Remaining : 남은 요청 횟수 <br/>
     * - X-Ratelimit-Limit : 요청 제한 횟수 <br/>
     * - X-Ratelimit-Retry-After : 다음 요청까지 대기 시간
     *
     * @param tokenBucketInfo 토큰 정보
     */
    private void setResponseHeader(AbstractTokenInfo tokenBucketInfo) {
        HttpServletResponse response = ((ServletRequestAttributes)(RequestContextHolder.currentRequestAttributes())).getResponse();
        if (response != null) {
            response.setIntHeader("X-Ratelimit-Remaining", tokenBucketInfo.getRemaining());
            response.setIntHeader("X-Ratelimit-Limit", tokenBucketInfo.getLimit());
            response.setIntHeader("X-Ratelimit-Retry-After", tokenBucketInfo.getRetryAfter());
        }
    }

}

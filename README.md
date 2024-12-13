# 처리율 제한기

## 1. 개요

`가상면접 사례로 배우는 대규모 시스템 설계 기초` 책의 챕터 3에서 소개된 처리율 제한기를 구현한 프로젝트입니다.

## 2. 구현 내용

`@RateLimiting` 어노테이션을 사용하여 메서드에 대한 처리율 제한을 설정할 수 있습니다.

`application.yml` 파일에 설정을 해서 lock 을 구현하는 방법, 어떤 알고리즘을 사용하여 처리율을 제한할지, 캐시는 어떤것을 사용할지 등등을 정할 수 있습니다.

## 3. 사용 방법

`@RateLiming`어노테이션의 `cacheKey` 를 통해 Lock 을 걸어서 멀티 쓰레드 환경에서 동시에 처리율 제한을 적용할 수 있습니다.
ex)

```java
@RateLimiting(
    name = "rate-limiting-service",
    cacheKey = "#request.carNo"
)
```

`application.yml` 에서 설정을 하실 수 있습니다.

### 처리율 제한 설정.

```yaml
rate-limiter:
  enabled: true
  lock-type: redis_redisson
  rate-type: token_bucket
  cache-type: redis

```

- `enable`: 처리율 제한기 사용 여부, false 로 지정할 경우 처리율 제한을 사용하실 수 없습니다. 기본값 (false)
- `lock-type`: 멀티 쓰레드 환경에서 동시에 처리율 제한을 적용할 때 사용할 Lock 을 지정합니다. (redis_redisson, concurrent_hash_map) 기본값 (concurrent_hash_map)
- `rate-type`: 처리율 제한 알고리즘을 지정합니다. (token_bucket, leaky_bucket, fixed_window_counter, sliding_window_logging, sliding_window_counter) 기본값 (token_bucket)
- `cache-type`: 캐시를 사용할 때 사용할 캐시를 지정합니다. (redis, concurrent_hash_map) 기본값 (concurrent_hash_map)

`rate-type`을 redis_redisson 으로 설정하거나, `cache-type` 을 redis 로 설정할 경우 redis 서버를 설치하고 세팅을 해 주셔야 합니다.
만약 세팅을 하지 않는다면 기본 설정을 따라가므로 localhost:6379 로 접속을 시도합니다.

### 처리율 알고리즘 설정

처리율 제한을 처리할 알고리즘을 선택하시려면 `application.yml` 에 다음 항목을 넣어주세요.

- `rate-type`이 token_bucket 일 경우

```yaml
token-bucket:
  capacity: 10
  rate: 1
  rate-unit: seconds
```

- capacity: 토큰에 담길 최대 용량을 설정합니다.
- rate: 토큰이 생성되는 속도를 설정합니다.
- rate-unit: rate의 단위를 설정합니다.( seconds, minutes, hours, days )
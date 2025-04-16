---
name: 버그 리포트
about: 버그를 발견하셨나요? 리포트를 작성해주세요
title: "[BUG] "
labels: "bug"
assignees: ""
---

## 🐛 버그 설명

<!-- 버그를 명확하고 간결하게 설명해주세요 -->

## 📋 재현 방법

<!-- 버그를 재현하는 단계를 자세히 설명해주세요 -->

1. '...'로 이동
2. '....'를 클릭
3. '....'로 스크롤
4. 오류 발생

**최소 재현 코드**

```java
// 버그를 재현할 수 있는 최소한의 코드를 작성해주세요
@Service
public class ExampleService {
    @RateLimiting(name = "test", cacheKey = "#id")
    public void method(String id) {
        // ...
    }
}
```

**설정 파일**

```yaml
# application.yml
rate-limiter:
    enabled: true
    rate-type: token_bucket
```

## ✅ 예상 동작

<!-- 어떻게 동작해야 하는지 설명해주세요 -->

## ❌ 실제 동작

<!-- 실제로 어떻게 동작하는지 설명해주세요 -->

## 📸 스크린샷

<!-- 가능하면 스크린샷을 추가해주세요 -->

## 🔍 에러 로그

```
// 에러 로그나 스택 트레이스를 붙여넣어주세요
```

## 🌍 환경

**데스크탑 환경**

-   OS: [예: Ubuntu 20.04]
-   Java Version: [예: OpenJDK 21]
-   Spring Boot Version: [예: 3.4.0]
-   Rate Limiter Version: [예: 1.0.0]

**서버 환경**

-   Redis Version: [예: 7.0]
-   Redisson Version: [예: 3.40.2]

**의존성 버전**

```gradle
dependencies {
    // 관련 의존성 버전을 명시해주세요
}
```

## 📝 추가 컨텍스트

<!-- 버그와 관련된 추가 정보를 제공해주세요 -->

## 🔗 관련 이슈

<!-- 관련된 다른 이슈가 있다면 링크해주세요 -->

-   #이슈번호

## ✅ 체크리스트

-   [ ] 최신 버전에서 테스트했습니다
-   [ ] 기존 이슈를 검색했습니다
-   [ ] 최소 재현 코드를 작성했습니다
-   [ ] 에러 로그를 첨부했습니다
-   [ ] 환경 정보를 모두 작성했습니다

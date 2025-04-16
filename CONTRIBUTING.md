# 기여 가이드 (Contributing Guide)

Rate Limiter Spring Boot Starter 프로젝트에 관심을 가져주셔서 감사합니다! 🎉

이 문서는 프로젝트에 기여하는 방법을 안내합니다.

## 목차

-   [시작하기 전에](#시작하기-전에)
-   [개발 환경 설정](#개발-환경-설정)
-   [기여 방법](#기여-방법)
-   [코딩 컨벤션](#코딩-컨벤션)
-   [커밋 메시지 가이드](#커밋-메시지-가이드)
-   [Pull Request 프로세스](#pull-request-프로세스)
-   [이슈 리포팅](#이슈-리포팅)
-   [커뮤니티 가이드라인](#커뮤니티-가이드라인)

---

## 시작하기 전에

### 기여할 수 있는 것들

-   🐛 **버그 수정**: 버그를 발견하면 이슈를 등록하거나 직접 수정
-   ✨ **새로운 기능**: 새로운 알고리즘이나 기능 추가
-   📝 **문서화**: 문서 개선, 번역, 예제 추가
-   🧪 **테스트**: 테스트 커버리지 개선
-   🎨 **코드 품질**: 리팩토링, 성능 개선
-   💡 **아이디어**: 새로운 기능이나 개선사항 제안

### 기여하기 전 확인사항

-   [ ] 이미 비슷한 이슈나 PR이 있는지 확인했나요?
-   [ ] 문제를 재현할 수 있나요?
-   [ ] 변경사항이 프로젝트의 범위에 맞나요?

---

## 개발 환경 설정

### 필수 요구사항

-   **Java**: 21 이상
-   **Gradle**: 8.0 이상 (wrapper 사용 권장)
-   **Redis**: 7.0 이상 (테스트용)
-   **Docker**: 최신 버전 (TestContainers 사용)
-   **IDE**: IntelliJ IDEA 또는 Eclipse 권장

### 프로젝트 클론 및 빌드

```bash
# 1. 저장소 Fork
# GitHub에서 Fork 버튼 클릭

# 2. 로컬에 Clone
git clone https://github.com/YOUR_USERNAME/rate-limiter-spring-boot-starter.git
cd rate-limiter-spring-boot-starter

# 3. 원본 저장소를 upstream으로 추가
git remote add upstream https://github.com/ORIGINAL_OWNER/rate-limiter-spring-boot-starter.git

# 4. 의존성 다운로드 및 빌드
./gradlew build

# 5. 테스트 실행
./gradlew test

# 6. Redis 실행 (로컬 테스트용)
docker run -d -p 6379:6379 redis:7.0-alpine
```

### IDE 설정

**IntelliJ IDEA**

1. `File` → `Open` → 프로젝트 폴더 선택
2. Gradle 프로젝트로 자동 인식
3. `Build` → `Build Project`로 빌드 확인

**Code Style 설정**

프로젝트 루트의 `.editorconfig` 파일을 따릅니다:

```editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true
indent_style = space
indent_size = 4

[*.yml]
indent_size = 2

[*.md]
trim_trailing_whitespace = false
```

---

## 기여 방법

### 1. 브랜치 생성

작업할 브랜치를 생성합니다:

```bash
# 최신 코드 동기화
git checkout main
git pull upstream main

# 기능 브랜치 생성
git checkout -b feature/your-feature-name

# 또는 버그 수정 브랜치
git checkout -b fix/bug-description
```

### 2. 코드 작성

-   명확하고 읽기 쉬운 코드 작성
-   주석으로 복잡한 로직 설명
-   JavaDoc 작성 (public 메서드)
-   테스트 코드 작성

### 3. 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 모듈 테스트
./gradlew :rate-limiter:test

# 커버리지 리포트 생성
./gradlew jacocoTestReport

# 리포트 확인
open build/reports/jacoco/test/html/index.html
```

### 4. 커밋

```bash
# 변경사항 확인
git status

# 스테이징
git add .

# 커밋 (커밋 메시지 가이드 참조)
git commit -m "feat: Add new token bucket algorithm"

# 푸시
git push origin feature/your-feature-name
```

### 5. Pull Request

GitHub에서 PR을 생성합니다:

1. Fork한 저장소로 이동
2. `Compare & pull request` 버튼 클릭
3. PR 템플릿에 따라 작성
4. 리뷰어 지정 (옵션)

---

## 코딩 컨벤션

### Java 코딩 스타일

프로젝트는 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)를 따릅니다.

#### 주요 규칙

**1. 네이밍**

```java
// 클래스: PascalCase
public class TokenBucketHandler implements RateLimitHandler {

    // 상수: UPPER_SNAKE_CASE
    private static final int DEFAULT_CAPACITY = 100;

    // 변수/메서드: camelCase
    private int tokenCount;

    public void allowRequest() {
        // ...
    }

    // 패키지: lowercase
    // com.innercicle.handler
}
```

**2. 들여쓰기**

```java
// 4 spaces (탭 아님)
public class Example {
    public void method() {
        if (condition) {
            // code
        }
    }
}
```

**3. 중괄호**

```java
// 항상 중괄호 사용 (한 줄이어도)
if (condition) {
    doSomething();
}

// K&R style
public void method() {
    // code
}
```

**4. JavaDoc**

```java
/**
 * Token Bucket 알고리즘을 구현한 Rate Limit Handler
 *
 * <p>이 클래스는 고정된 속도로 토큰을 생성하고, 요청마다 토큰을 소비하여
 * Rate Limiting을 수행합니다.</p>
 *
 * @author Your Name
 * @since 1.0.0
 * @see RateLimitHandler
 */
public class TokenBucketHandler implements RateLimitHandler {

    /**
     * 요청을 허용할지 결정합니다.
     *
     * @param cacheKey 캐시 키
     * @return 토큰 정보
     * @throws RateLimitException 토큰이 부족할 경우
     */
    @Override
    public AbstractTokenInfo allowRequest(String cacheKey) {
        // implementation
    }
}
```

**5. 로깅**

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Example {

    public void method() {
        // DEBUG: 상세한 디버그 정보
        log.debug("Token count: {}", tokenCount);

        // INFO: 일반적인 정보
        log.info("Rate limit applied for user: {}", userId);

        // WARN: 경고 (에러는 아님)
        log.warn("Token bucket is empty for key: {}", cacheKey);

        // ERROR: 에러 발생
        log.error("Failed to acquire lock: {}", e.getMessage(), e);
    }
}
```

### 테스트 코드 작성

**1. 테스트 클래스 네이밍**

```java
// 대상 클래스 + Test
public class TokenBucketHandlerTest {
    // ...
}
```

**2. 테스트 메서드 네이밍**

```java
@Test
void allowRequest_WhenTokensAvailable_ShouldSucceed() {
    // Given
    String cacheKey = "test-key";
    TokenBucketInfo info = createTokenBucketInfo(5);

    // When
    AbstractTokenInfo result = handler.allowRequest(cacheKey);

    // Then
    assertThat(result.getRemaining()).isEqualTo(4);
}

@Test
void allowRequest_WhenNoTokens_ShouldThrowException() {
    // Given
    String cacheKey = "test-key";
    TokenBucketInfo info = createTokenBucketInfo(0);

    // When & Then
    assertThatThrownBy(() -> handler.allowRequest(cacheKey))
        .isInstanceOf(RateLimitException.class)
        .hasMessage("Rate limit exceeded");
}
```

**3. Given-When-Then 패턴**

```java
@Test
void testExample() {
    // Given: 테스트 준비
    String input = "test";

    // When: 실제 동작
    String result = service.process(input);

    // Then: 결과 검증
    assertThat(result).isEqualTo("expected");
}
```

**4. 테스트 커버리지**

-   새로운 코드는 최소 80% 커버리지 달성
-   중요한 비즈니스 로직은 100% 커버리지
-   Edge case 테스트 포함

---

## 커밋 메시지 가이드

[Conventional Commits](https://www.conventionalcommits.org/) 스펙을 따릅니다.

### 형식

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type

-   `feat`: 새로운 기능 추가
-   `fix`: 버그 수정
-   `docs`: 문서만 변경
-   `style`: 코드 포맷팅 (기능 변경 없음)
-   `refactor`: 코드 리팩토링
-   `perf`: 성능 개선
-   `test`: 테스트 추가/수정
-   `build`: 빌드 시스템, 의존성 변경
-   `ci`: CI 설정 변경
-   `chore`: 기타 변경사항

### 예시

```bash
# 기능 추가
git commit -m "feat: Add sliding window counter algorithm"

# 버그 수정
git commit -m "fix: Correct token refill calculation in TokenBucketHandler"

# 문서 업데이트
git commit -m "docs: Update algorithm comparison table in README"

# 리팩토링
git commit -m "refactor: Extract lock acquisition logic to separate method"

# 테스트 추가
git commit -m "test: Add integration tests for Redis-based rate limiting"

# Breaking change
git commit -m "feat!: Change RateLimiting annotation default behavior

BREAKING CHANGE: waitTime default changed from 5000ms to 3000ms"
```

### 작성 규칙

-   제목은 50자 이내
-   제목은 명령형으로 작성 (Add, Fix, Update)
-   제목 끝에 마침표 없음
-   본문은 72자마다 줄바꿈
-   본문은 "무엇을, 왜" 했는지 설명

---

## Pull Request 프로세스

### PR 템플릿

PR 생성 시 다음 템플릿을 사용해주세요:

```markdown
## 변경사항 설명

간단히 변경사항을 설명해주세요.

## 변경 타입

-   [ ] 버그 수정
-   [ ] 새로운 기능
-   [ ] Breaking change
-   [ ] 문서 업데이트
-   [ ] 리팩토링
-   [ ] 테스트 추가/수정

## 관련 이슈

Closes #(이슈 번호)

## 테스트

어떻게 테스트했는지 설명해주세요:

-   [ ] 유닛 테스트 추가
-   [ ] 통합 테스트 추가
-   [ ] 수동 테스트 완료
-   [ ] 기존 테스트 모두 통과

## 체크리스트

-   [ ] 코드가 프로젝트의 코딩 컨벤션을 따릅니다
-   [ ] 자체 리뷰를 완료했습니다
-   [ ] 코드에 주석을 추가했습니다 (복잡한 로직)
-   [ ] 문서를 업데이트했습니다
-   [ ] 변경사항이 새로운 경고를 발생시키지 않습니다
-   [ ] 테스트를 추가했습니다
-   [ ] 모든 테스트가 통과합니다

## 스크린샷 (해당하는 경우)

변경사항을 보여주는 스크린샷이나 GIF를 추가해주세요.
```

### 리뷰 프로세스

1. **자동 체크**: CI가 자동으로 빌드 및 테스트 실행
2. **코드 리뷰**: 메인테이너가 코드 리뷰
3. **피드백 반영**: 리뷰 코멘트에 따라 수정
4. **승인**: 2명 이상의 승인 필요
5. **머지**: Squash and merge 방식 사용

### PR 작성 팁

✅ **해야 할 것**

-   작은 단위로 PR 생성 (한 PR에 한 가지 변경사항)
-   명확한 제목과 설명 작성
-   테스트 코드 포함
-   스크린샷/GIF 추가 (UI 변경 시)
-   관련 이슈 연결

❌ **하지 말아야 할 것**

-   너무 큰 PR (500줄 이상)
-   여러 기능을 한 PR에 포함
-   테스트 없이 PR 생성
-   리뷰 피드백 무시

---

## 이슈 리포팅

### 버그 리포트

버그를 발견하면 다음 템플릿을 사용해주세요:

```markdown
## 버그 설명

버그를 명확하고 간결하게 설명해주세요.

## 재현 방법

1. '...'로 이동
2. '....'를 클릭
3. '....'로 스크롤
4. 오류 발생

## 예상 동작

어떻게 동작해야 하는지 설명해주세요.

## 실제 동작

실제로 어떻게 동작하는지 설명해주세요.

## 스크린샷

가능하면 스크린샷을 추가해주세요.

## 환경

-   OS: [예: Ubuntu 20.04]
-   Java Version: [예: 21]
-   Spring Boot Version: [예: 3.4.0]
-   Redis Version: [예: 7.0]

## 추가 정보

다른 컨텍스트나 정보를 추가해주세요.

## 로그

관련 로그를 추가해주세요:
```

로그 내용

```

```

### 기능 요청

```markdown
## 기능 설명

제안하는 기능을 명확하고 간결하게 설명해주세요.

## 문제/동기

이 기능이 해결하는 문제나 필요한 이유를 설명해주세요.

## 제안하는 해결책

원하는 동작을 명확하고 간결하게 설명해주세요.

## 대안

고려한 대안들을 설명해주세요.

## 추가 정보

다른 컨텍스트나 스크린샷을 추가해주세요.
```

---

## 커뮤니티 가이드라인

### 행동 강령

모든 기여자는 다음 원칙을 따라야 합니다:

-   🤝 **존중**: 모든 사람을 존중하고 배려합니다
-   💡 **건설적**: 건설적인 피드백을 제공합니다
-   🎯 **집중**: 프로젝트의 목표에 집중합니다
-   🌈 **포용**: 다양성을 존중하고 포용합니다
-   📚 **학습**: 함께 배우고 성장합니다

### 금지 행위

-   ❌ 욕설, 모욕적 언어 사용
-   ❌ 개인 공격
-   ❌ 괴롭힘
-   ❌ 스팸
-   ❌ 부적절한 콘텐츠

위반 시 프로젝트에서 제외될 수 있습니다.

---

## 질문이 있나요?

-   📧 **이메일**: your-email@example.com
-   💬 **Discussions**: [GitHub Discussions](https://github.com/your-repo/discussions)
-   🐛 **이슈**: [GitHub Issues](https://github.com/your-repo/issues)

---

## 라이센스

기여하신 코드는 프로젝트의 [MIT 라이센스](LICENSE)에 따라 배포됩니다.

---

## 감사합니다! 🎉

여러분의 기여가 이 프로젝트를 더 나아지게 만듭니다. 감사합니다! ❤️

---

**Happy Contributing!** 🚀

# Pumpkin Log SDK

HTTP 요청 로그를 일관된 JSON 형식으로 남기는 SDK입니다.

## 주요 기능

- **stdout JSON 로그 출력**: HTTP 요청마다 JSON 형식의 로그를 stdout에 출력
- **파일 JSONL 로그 저장**: `/tmp/log.{pid}.jsonl` 파일에 JSONL 형식으로 append
  - 동기 모드 (FileLogAppender): 즉시 쓰기, 로그 유실 없음
  - 비동기 모드 (AsyncFileLogAppender): 고성능, 논블로킹
- **extra 필드**: 개발자가 자유롭게 커스텀 데이터 추가 가능
- **경로 제외 필터**: 특정 경로 패턴 로그 제외 (예: `/actuator/**`)

## 빠른 시작

### 요구 사항

- JDK 21
- Gradle 8.x

### 데모 서버 실행

```bash
# 저장소 클론 후 데모 서버 실행
./gradlew :demo-server-mvc:bootRun

# 다른 터미널에서 요청 테스트
curl http://localhost:8080/health
curl http://localhost:8080/api/users/123
curl -X POST http://localhost:8080/api/orders
```

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :pumpkin-log-core:test
./gradlew :pumpkin-log-spring-mvc:test
./gradlew :demo-server-mvc:test
```

## 로그 JSON 형식

```json
{
  "type": "log.v1.http",
  "user_agent": "Mozilla/5.0...",
  "duration": 12,
  "http_status_code": 200,
  "http_method": "GET",
  "http_path": "/health",
  "http_query": "foo=bar",
  "extra": {
    "userId": "123"
  },
  "timestamp": "2024-01-01T00:00:00.000Z"
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| type | String | 로그 타입 (고정값: `log.v1.http`) |
| user_agent | String | User-Agent 헤더 값 |
| duration | Long | 요청 처리 시간 (ms) |
| http_status_code | Int | HTTP 상태 코드 |
| http_method | String | HTTP 메서드 (GET, POST 등) |
| http_path | String | 요청 경로 |
| http_query | String | 쿼리 스트링 (없으면 빈 문자열) |
| extra | Object | 커스텀 데이터 (선택) |
| timestamp | String | 타임스탬프 (ISO 8601) |

## 설정 옵션

`application.yml`에서 설정 가능:

```yaml
pumpkin:
  log:
    enabled: true                    # SDK 활성화 (기본: true)

    console:
      enabled: true                  # stdout 출력 (기본: true)

    file:
      enabled: true                  # 파일 출력 (기본: true)
      path: /tmp/log.{pid}.jsonl     # 파일 경로 ({pid}는 자동 치환)
      async:
        enabled: false               # 비동기 모드 (기본: false)
        buffer-size: 10000           # 큐 버퍼 크기 (기본: 10000)
        batch-size: 100              # 배치 쓰기 크기 (기본: 100)

    exclude-paths:                   # 로그 제외 경로 (Ant 패턴)
      - /actuator/**
      - /favicon.ico
```

### 설정 옵션 상세

| 설정 | 타입 | 기본값 | 설명 |
|-----|------|-------|------|
| `pumpkin.log.enabled` | Boolean | true | SDK 전체 활성화 |
| `pumpkin.log.console.enabled` | Boolean | true | stdout 로그 출력 |
| `pumpkin.log.file.enabled` | Boolean | true | 파일 로그 출력 |
| `pumpkin.log.file.path` | String | /tmp/log.{pid}.jsonl | 로그 파일 경로 |
| `pumpkin.log.file.async.enabled` | Boolean | false | 비동기 모드 활성화 |
| `pumpkin.log.file.async.buffer-size` | Int | 10000 | 비동기 큐 버퍼 크기 |
| `pumpkin.log.file.async.batch-size` | Int | 100 | 한 번에 쓰는 로그 수 |
| `pumpkin.log.exclude-paths` | List | [] | 로그 제외 경로 패턴 |

### 동기 vs 비동기 파일 로깅

| 항목 | 동기 (FileLogAppender) | 비동기 (AsyncFileLogAppender) |
|------|----------------------|------------------------------|
| 처리 방식 | 요청 스레드에서 직접 write | Worker Thread가 batch로 write |
| 응답 지연 | 파일 I/O 만큼 지연 | 거의 없음 (offer만 하고 리턴) |
| 로그 유실 | 없음 | 가능 (큐 가득 차면 버림) |
| 적합한 상황 | 저트래픽, 로그 유실 불가 | 고트래픽, 성능 우선 |

**비동기 모드 권장 설정:**
- `buffer-size`: 10,000 (기본값) - 대부분의 환경에서 drop 없이 처리 가능
- `batch-size`: 100 (기본값) - I/O 효율과 지연 사이의 균형점

## 아키텍처

### 모듈 구조

```
pumpkin-log/
├── pumpkin-log-core/           # 순수 Kotlin 핵심 모듈 (Spring 의존 X)
├── pumpkin-log-spring-mvc/     # Spring WebMVC 통합 모듈
└── demo-server-mvc/            # 데모 서버
```

### 의존성 방향

```
demo-server-mvc → pumpkin-log-spring-mvc → pumpkin-log-core
```

### 요청 처리 흐름

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              HTTP Request                                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           AccessLogFilter                                    │
│  (OncePerRequestFilter)                                                      │
│                                                                             │
│  1. 경로 제외 체크 → 제외 대상이면 로그 없이 통과                           │
│  2. LogContextHolder.init()                                                 │
│  3. startTime 기록                                                          │
│  4. chain.doFilter() → Controller 실행                                      │
│  5. finally: duration 계산 → AccessLogger.log() → LogContextHolder.clear() │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Controller                                      │
│                                                                             │
│  - 비즈니스 로직 처리                                                        │
│  - LogContextHolder.put("key", value) 로 extra 데이터 추가                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                             AccessLogger                                     │
│                                                                             │
│  - HttpLog 객체 생성 (요청 정보 + extra 데이터)                              │
│  - 등록된 모든 Appender에 dispatch                                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
┌───────────────────────────────────┐   ┌───────────────────────────────────┐
│      ConsoleLogAppender           │   │   FileLogAppender (동기)          │
│                                   │   │   AsyncFileLogAppender (비동기)   │
│  - stdout에 JSON 출력             │   │  - 파일에 JSONL 출력              │
└───────────────────────────────────┘   └───────────────────────────────────┘
```

### 핵심 클래스

| 클래스 | 모듈 | 역할 |
|--------|------|------|
| HttpLog | core | 로그 데이터 클래스 |
| LogAppender | core | 로그 출력 인터페이스 |
| ConsoleLogAppender | core | stdout JSON 출력 |
| FileLogAppender | core | 파일 JSONL 출력 (동기) |
| AsyncFileLogAppender | core | 파일 JSONL 출력 (비동기) |
| CompositeLogAppender | core | 여러 Appender 조합 |
| LogContextHolder | core | ThreadLocal 기반 extra 저장 |
| AccessLogger | core | 로그 생성 + Appender dispatch |
| AccessLogFilter | spring-mvc | 요청 가로채기 + 경로 제외 |

## 사용 예시

### 기본 사용

SDK를 적용하면 별도의 코드 없이 자동으로 HTTP 요청 로그가 기록됩니다.

### extra 필드 추가 (LogContextHolder 사용)

Controller에서 `LogContextHolder`를 사용하여 커스텀 데이터를 추가할 수 있습니다:

```kotlin
import com.pumpkin.log.context.LogContextHolder

@RestController
class UserController {

    @GetMapping("/api/users/{id}")
    fun getUser(@PathVariable id: String): ResponseEntity<User> {
        // extra 필드에 데이터 추가
        LogContextHolder.put("userId", id)
        LogContextHolder.put("action", "user_lookup")

        val user = userService.findById(id)

        // 중첩 객체도 가능
        LogContextHolder.put("userInfo", mapOf(
            "name" to user.name,
            "role" to user.role
        ))

        return ResponseEntity.ok(user)
    }
}
```

출력 예시:
```json
{
  "type": "log.v1.http",
  "http_method": "GET",
  "http_path": "/api/users/123",
  "http_status_code": 200,
  "duration": 15,
  "extra": {
    "userId": "123",
    "action": "user_lookup",
    "userInfo": {
      "name": "홍길동",
      "role": "admin"
    }
  },
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

### 경로 제외 설정

특정 경로의 로그를 제외하려면 `exclude-paths`를 설정합니다:

```yaml
pumpkin:
  log:
    exclude-paths:
      - /actuator/**      # Spring Actuator 전체
      - /health           # 헬스체크
      - /favicon.ico      # 파비콘
      - /api/internal/**  # 내부 API
```

Ant 스타일 패턴을 지원합니다:
- `*`: 단일 경로 세그먼트 매칭
- `**`: 모든 하위 경로 매칭

### 비동기 파일 로깅 설정

고트래픽 환경에서는 비동기 모드를 권장합니다:

```yaml
pumpkin:
  log:
    file:
      enabled: true
      async:
        enabled: true       # 비동기 모드 활성화
        buffer-size: 10000  # 큐 버퍼 크기 (로그 개수)
        batch-size: 100     # 한 번에 파일에 쓰는 로그 수
```

**buffer-size 선택 가이드:**
| 트래픽 | buffer-size | 메모리 사용 |
|-------|-------------|------------|
| 저트래픽 | 1,000 | ~300KB |
| 일반 | 10,000 (기본) | ~3MB |
| 고트래픽 | 50,000 | ~15MB |

## 파일 구조

```
pumpkin-log/
├── pumpkin-log-core/
│   └── src/main/kotlin/com/pumpkin/log/
│       ├── model/
│       │   └── HttpLog.kt                # 로그 데이터 클래스
│       ├── appender/
│       │   ├── LogAppender.kt            # 로그 출력 인터페이스
│       │   ├── ConsoleLogAppender.kt     # stdout 출력
│       │   ├── FileLogAppender.kt        # 파일 출력 (동기)
│       │   ├── AsyncFileLogAppender.kt   # 파일 출력 (비동기)
│       │   └── CompositeLogAppender.kt   # 여러 Appender 조합
│       ├── context/
│       │   └── LogContextHolder.kt       # ThreadLocal 기반 extra 저장
│       ├── logger/
│       │   └── AccessLogger.kt           # 로그 생성 + dispatch
│       └── util/
│           ├── ObjectMapperFactory.kt    # 공통 Jackson 설정
│           └── FilePathResolver.kt       # PID 치환 유틸리티
│
├── pumpkin-log-spring-mvc/
│   └── src/main/kotlin/com/pumpkin/log/spring/mvc/
│       ├── filter/
│       │   └── AccessLogFilter.kt        # OncePerRequestFilter 구현
│       └── config/
│           ├── PumpkinLogProperties.kt   # 설정 바인딩
│           └── PumpkinLogAutoConfiguration.kt  # 자동 설정
│
└── demo-server-mvc/
    └── src/main/kotlin/com/pumpkin/demo/mvc/
        ├── DemoServerMvcApplication.kt   # Spring Boot 메인
        └── controller/
            └── SampleController.kt       # 데모 API
```

## 기술 스택

- Language: Kotlin 1.9.25
- JDK: 21
- Build: Gradle 8.x (Kotlin DSL)
- Framework: Spring Boot 3.4.12
- Test: JUnit 5, MockK, AssertJ, Awaitility

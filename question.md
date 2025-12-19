# Pumpkin Log SDK 면접 질문 & 답변

## Part 1: Gradle 빌드 설정

### 질문 1-1: Version Catalog

**Q:** `gradle/libs.versions.toml` 파일을 사용하셨는데요, 왜 기존 방식(build.gradle에 직접 버전 명시)이 아닌 Version Catalog를 선택하셨나요?

**A:** 버전 관리를 좀 더 한눈에 보고 쉽게 관리하기 위해 version catalog를 선택했습니다.

**피드백:** 맞습니다. 하지만 조금 더 구체적으로 설명할 수 있으면 좋겠어요.

Version Catalog의 핵심 장점은:
1. **중앙 집중식 버전 관리**: 멀티모듈 프로젝트에서 모든 모듈이 동일한 버전을 참조
2. **타입 안전성**: `libs.spring.boot.starter.web`처럼 IDE 자동완성 지원
3. **Bundle 기능**: 자주 함께 쓰는 의존성을 그룹화 (`bundles.test-common`)
4. **버전 충돌 방지**: 한 곳에서 관리하므로 모듈별 버전 불일치 예방

면접에서는 "왜?"라는 질문에 2-3가지 구체적 이유를 대면 좋습니다.

---

### 질문 1-2: 버전 명시 vs 미명시

**Q:** `libs.versions.toml`을 보면 같은 라이브러리가 두 가지 버전으로 정의되어 있습니다:

```toml
# 버전 없음
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }

# 버전 명시
spring-boot-starter-web-versioned = { module = "org.springframework.boot:spring-boot-starter-web", version.ref = "spring-boot" }
```

왜 이렇게 두 가지로 나눠서 정의하셨나요? 각각 어디서 사용되나요?

**A:** 잘 모르겠습니다만 버젼 없음은 pumpkin-log-core에서 사용될 것 같고, 버전 명시한 것은 pumpkin-log-spring-mvc에서 사용될 것 같습니다.

**피드백:** 방향은 좋은데 정확하지 않습니다. 실제 코드를 보면:

**버전 없음 (`spring-boot-starter-web`)** → `demo-server-mvc`에서 사용:
```kotlin
// demo-server-mvc/build.gradle.kts
plugins {
    alias(libs.plugins.spring.dependency.management)  // 이 플러그인이 버전 관리
}
implementation(libs.spring.boot.starter.web)
```

**버전 명시 (`spring-boot-starter-web-versioned`)** → `pumpkin-log-spring-mvc`에서 사용:
```kotlin
// pumpkin-log-spring-mvc/build.gradle.kts
// spring-dependency-management 플러그인 없음!
compileOnly(libs.spring.boot.starter.web.versioned)
```

**핵심 차이:**
- `demo-server-mvc`는 **애플리케이션**이라 `spring-dependency-management` 플러그인이 버전을 자동 관리
- `pumpkin-log-spring-mvc`는 **라이브러리**라서 해당 플러그인을 적용하지 않음 → 버전을 명시적으로 지정해야 함

참고로 `pumpkin-log-core`는 Spring 의존성이 아예 없습니다.

**추가 설명: `spring-dependency-management` 플러그인이 뭘 하나?**

이 플러그인은 Spring Boot BOM(Bill of Materials)을 가져와서 **Spring 관련 의존성 버전을 자동으로 관리**합니다.

```kotlin
// 플러그인이 있으면 내부적으로 이렇게 동작:
dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.12")
    }
}

// 이 BOM 안에 이런 정보가 있음:
// spring-boot-starter-web → 3.4.12
// jackson-databind → 2.17.0
// logback → 1.4.x
// ... 수백 개의 의존성 버전이 미리 정의됨
```

**결과:**
```kotlin
// 버전을 안 써도 자동으로 3.4.12가 적용됨
implementation("org.springframework.boot:spring-boot-starter-web")
```

**왜 라이브러리 모듈에는 이 플러그인을 안 쓰나?**

만약 pumpkin-log-spring-mvc에 이 플러그인을 적용하면:

```kotlin
plugins {
    alias(libs.plugins.spring.dependency.management)  // 적용!
}

dependencies {
    implementation(libs.spring.boot.starter.web)  // 버전 없이
}
```

빌드된 JAR의 pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.4.12</version>  <!-- 버전이 고정됨! -->
</dependency>
```

사용자 프로젝트에서:
```kotlin
// 사용자는 Spring Boot 3.5.0을 사용 중
implementation("com.pumpkin:pumpkin-log-spring-mvc:1.0.0")
implementation("org.springframework.boot:spring-boot-starter-web:3.5.0")

// 결과: 3.4.12 vs 3.5.0 버전 충돌!
```

**compileOnly를 쓰면?**

```kotlin
// pumpkin-log-spring-mvc
compileOnly(libs.spring.boot.starter.web.versioned)  // 3.4.12로 컴파일
```

빌드된 JAR의 pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.4.12</version>
    <scope>provided</scope>  <!-- 런타임에 포함 안 됨! -->
</dependency>
```

사용자 프로젝트에서:
```kotlin
implementation("com.pumpkin:pumpkin-log-spring-mvc:1.0.0")
implementation("org.springframework.boot:spring-boot-starter-web:3.5.0")

// pumpkin-log가 3.5.0 버전을 그대로 사용 → 충돌 없음!
```

**정리:**

| 모듈 | 역할 | 플러그인 | 의존성 선언 | 이유 |
|------|------|---------|-----------|------|
| **demo-server-mvc** | 애플리케이션 | `spring-dependency-management` ✅ | 버전 없이 | 플러그인이 버전 관리 |
| **pumpkin-log-spring-mvc** | 라이브러리 | 플러그인 없음 ❌ | 버전 명시 | 플러그인 없으면 버전 필수 |

**한 줄 요약:** `spring-dependency-management` 플러그인이 없으면 Gradle이 어떤 버전을 가져올지 모르기 때문에 버전을 명시해야 합니다.

**추가 질문: libs.versions.toml에 플러그인을 선언하면 그게 버전 관리를 하는 건가요?**

아니요, 두 가지는 다른 역할입니다.

**1. `libs.versions.toml` - 플러그인/라이브러리 참조용 별칭**

```toml
# libs.versions.toml
[plugins]
spring-dependency-management = { id = "io.spring.dependency-management", version.ref = "spring-dependency-management" }
```

이건 그냥 **"이 플러그인을 `libs.plugins.spring.dependency.management`라는 이름으로 부르겠다"**는 선언입니다.

**2. `io.spring.dependency-management` 플러그인 - 실제 버전 관리 수행**

```kotlin
// demo-server-mvc/build.gradle.kts
plugins {
    alias(libs.plugins.spring.dependency.management)  // 플러그인 적용
}
```

이 플러그인이 **실제로 빌드 시점에** Spring Boot BOM을 가져와서 버전을 관리합니다.

**흐름 정리:**

```
┌─────────────────────────────────────────────────────────────┐
│ libs.versions.toml                                          │
│                                                             │
│ spring-dependency-management 플러그인의 "별칭"만 정의        │
│ (실제 버전 관리 X, 그냥 이름표)                               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ demo-server-mvc/build.gradle.kts                            │
│                                                             │
│ plugins {                                                   │
│     alias(libs.plugins.spring.dependency.management)        │
│ }                                                           │
│                                                             │
│ → 플러그인이 "적용"됨                                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ 플러그인이 빌드 시점에 하는 일                                 │
│                                                             │
│ 1. Maven Central에서 spring-boot-dependencies BOM 다운로드   │
│ 2. BOM 안의 버전 정보를 읽음                                  │
│ 3. 프로젝트의 의존성에 버전이 없으면 BOM에서 찾아서 채움        │
└─────────────────────────────────────────────────────────────┘
```

**비유:**
- `libs.versions.toml` = "전화번호부" (이름 → 번호 매핑)
- 플러그인 적용 = "전화 거는 것" (전화번호부에서 번호 찾아서 실제로 전화함)
- 플러그인 동작 = "전화 받은 사람이 일하는 것" (실제로 버전 관리 작업 수행)

**결론:** `libs.versions.toml`은 플러그인의 **이름과 버전만 정의**하고, 실제 버전 관리는 **플러그인이 적용된 후 빌드 시점에** 수행됩니다.

---

### 질문 1-3: compileOnly vs implementation

**Q:** `pumpkin-log-spring-mvc/build.gradle.kts`에서:

```kotlin
compileOnly(libs.spring.boot.starter.web.versioned)
compileOnly(libs.spring.boot.autoconfigure.versioned)
```

왜 `implementation`이 아니라 `compileOnly`를 사용하셨나요?

**A:** implementation이 아니라 compileOnly를 사용한건 말 그대로 compile 시점에만 적용하고 싶어서야.

**피드백:** 맞는 방향이지만, "왜 compile 시점에만 적용하고 싶은지"가 핵심입니다.

`pumpkin-log-spring-mvc`는 **라이브러리 모듈**입니다. 이 라이브러리를 사용하는 쪽(demo-server-mvc)에서 이미 `spring-boot-starter-web`을 가지고 있습니다.

만약 `implementation`을 쓰면:
- 라이브러리가 Spring 의존성을 **전이(transitive)**시킴
- 사용자 프로젝트와 버전 충돌 가능성 발생
- 불필요하게 의존성 중복

`compileOnly`를 쓰면:
- 컴파일할 때만 Spring 클래스 참조 가능
- 런타임에는 **사용자가 제공한 Spring**을 사용
- "이 라이브러리를 쓰려면 Spring이 있어야 해요"라는 의미 (provided scope와 유사)

이런 패턴을 **"provided dependency"** 또는 **"peer dependency"**라고 합니다.

---

### 질문 1-4: api vs implementation

**Q:** 같은 파일에서:

```kotlin
api(project(":pumpkin-log-core"))
```

왜 `implementation`이 아니라 `api`를 사용하셨나요? `api`와 `implementation`의 차이는 무엇인가요?

**A:** pumpkin-log-core에서 사용된 dependency들을 pumpkin-log-spring-mvc에서도 사용할 수 있도록 전이시키기 위해서 사용했습니다.

**피드백:** 정확합니다! 핵심을 잘 짚으셨어요.

조금 더 보충하면:

| 구분 | `implementation` | `api` |
|------|------------------|-------|
| 전이 의존성 | X (숨김) | O (노출) |
| 컴파일 속도 | 빠름 | 느림 |
| 사용 시점 | 내부 구현용 | 외부 노출용 |

이 프로젝트에서 `api`를 쓴 이유:
- `demo-server-mvc`가 `pumpkin-log-spring-mvc`만 의존해도
- `pumpkin-log-core`의 클래스들(`HttpLog`, `LogContextHolder` 등)을 직접 사용 가능

만약 `implementation`이었다면:
```kotlin
// demo-server-mvc에서 이 코드가 컴파일 에러!
LogContextHolder.put("userId", "123")  // LogContextHolder를 찾을 수 없음
```

라이브러리의 public API에 포함되는 의존성은 `api`, 내부 구현에만 쓰이는 건 `implementation`이 원칙입니다.

---

### 질문 1-5: kapt

**Q:**
```kotlin
plugins {
    alias(libs.plugins.kotlin.kapt)
}

kapt(libs.spring.boot.configuration.processor.versioned)
```

`kapt`가 무엇인가요? 왜 일반 의존성이 아니라 `kapt`로 `configuration-processor`를 추가하셨나요?

**A:** kapt는 kotlin annotation processing tool의 약자로 annotation 처리를 위해 사용했습니다.

**피드백:** 맞습니다! 조금 더 구체적으로 설명하면 완벽해요.

**kapt가 필요한 이유:**
- Java의 annotation processor는 컴파일 시점에 코드/메타데이터를 생성
- Kotlin은 Java와 다른 컴파일러를 사용하므로, Java annotation processor가 바로 동작하지 않음
- `kapt`는 Kotlin 코드를 먼저 Java stub으로 변환 → annotation processor 실행 → 결과물 생성

**spring-boot-configuration-processor의 역할:**
- `@ConfigurationProperties` 클래스를 스캔
- `META-INF/spring-configuration-metadata.json` 파일을 자동 생성
- 이 파일 덕분에 IDE에서 `application.yml` 작성 시 **자동완성**이 됨

```yaml
pumpkin:
  log:
    file:
      async:
        buffer-size: |  # ← IDE가 타입, 기본값, 설명을 보여줌
```

`kapt` 대신 `annotationProcessor`를 쓰면 Kotlin 클래스(`PumpkinLogProperties`)를 인식하지 못합니다.

**추가 Q&A: 컴파일러 개념 정리**

**Q:** Kotlin은 Java와 다른 컴파일러를 사용한다고 했는데, Java는 보통 JIT 컴파일러를 사용하는 것으로 알고 있는데 Kotlin은 다른 컴파일러를 사용한다는 거야? kapt는 왜 Kotlin 코드를 Java stub으로 변환하고 annotation processor를 실행하는 거야?

**A:** (질문)

**피드백:**

**JIT는 런타임 컴파일러**입니다. 혼동하신 것 같아요.

```
[컴파일 시점]                    [런타임]
Java 소스 → javac → 바이트코드 → JVM → JIT → 기계어
Kotlin 소스 → kotlinc → 바이트코드 → JVM → JIT → 기계어
```

- **javac**: Java 소스코드 → 바이트코드 (.class) 변환 (컴파일 시점)
- **kotlinc**: Kotlin 소스코드 → 바이트코드 (.class) 변환 (컴파일 시점)
- **JIT (Just-In-Time)**: 바이트코드 → 기계어 변환 (런타임)

제가 말한 "다른 컴파일러"는 **javac vs kotlinc**입니다. 둘 다 결과물은 JVM 바이트코드지만, 컴파일 과정이 다릅니다.

**kapt가 Java stub으로 변환하는 이유:**

Java Annotation Processor의 한계:
- Annotation Processor는 **javac의 플러그인**으로 동작
- javac가 컴파일할 때만 실행됨
- kotlinc는 javac가 아니므로, Java Annotation Processor가 Kotlin 코드를 인식 못함

kapt의 해결 방법:
```
1. Kotlin 소스에서 "껍데기"(stub) Java 파일 생성
   - 클래스 시그니처, 어노테이션만 있는 빈 Java 클래스

2. 이 stub을 javac로 컴파일하면서 Annotation Processor 실행
   - spring-boot-configuration-processor가 @ConfigurationProperties 인식

3. 메타데이터 생성 완료

4. 실제 Kotlin 코드는 kotlinc로 정상 컴파일
```

즉, **Annotation Processor를 속이기 위한 우회 방법**입니다.

---

### 질문 1-6: 루트 build.gradle.kts의 apply false

**Q:**
```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
}
```

왜 `apply false`를 붙이셨나요? 이게 없으면 어떻게 되나요?

**A:** 모르겠어요.

**피드백:**

`apply false`를 붙이면 플러그인을 **"등록만 하고 적용은 안 함"**입니다.

**apply false가 없으면:**
```kotlin
// 루트 build.gradle.kts
plugins {
    alias(libs.plugins.spring.boot)  // 루트 + 모든 서브모듈에 적용됨
}
```
- `pumpkin-log-core`에도 Spring Boot 플러그인이 적용됨 (불필요!)
- 모든 모듈이 Spring Boot 앱으로 취급됨

**apply false가 있으면:**
```kotlin
// 루트 build.gradle.kts
plugins {
    alias(libs.plugins.spring.boot) apply false  // 등록만
}

// demo-server-mvc/build.gradle.kts
plugins {
    alias(libs.plugins.spring.boot)  // 여기서만 실제 적용
}
```
- 서브모듈에서 **선택적으로** 플러그인 적용 가능
- `pumpkin-log-core`는 Spring Boot 플러그인 없이 순수 Kotlin 모듈 유지

**핵심:** 멀티모듈에서 모듈별로 다른 플러그인 조합을 쓰기 위한 패턴입니다.

---

### 질문 1-7: freeCompilerArgs의 -Xjsr305=strict

**Q:** 루트 `build.gradle.kts`에서:

```kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}
```

`-Xjsr305=strict`는 무엇이고, 왜 설정하셨나요?

**A:** 이건 잘 모르겠습니다. 프로젝트 생성 시부터 설정되어 있던 것 같아요.

**피드백:** 솔직하게 모른다고 하신 건 좋습니다. 면접에서는 모르는 걸 아는 척하는 것보다 낫습니다.

**JSR-305란?**
- Java Specification Request 305: null-safety 관련 어노테이션 표준
- `@Nullable`, `@NonNull`, `@ParametersAreNonnullByDefault` 등

**Spring과의 관계:**
- Spring Framework는 JSR-305 어노테이션을 사용해서 null-safety를 표시
- 예: `@Nullable String getName()` → 이 메서드는 null을 반환할 수 있음

**Kotlin에서의 의미:**
```kotlin
// Spring의 Java 코드
@Nullable
public String getName() { ... }

// Kotlin에서 호출할 때
val name = something.getName()  // name의 타입은?
```

| 옵션 | name의 타입 | 의미 |
|------|------------|------|
| 설정 없음 | `String!` (플랫폼 타입) | null 가능성 모름, 컴파일러가 체크 안 함 |
| `-Xjsr305=strict` | `String?` | null 가능성 있음, 컴파일러가 강제로 null 체크 요구 |

**strict를 쓰는 이유:**
- Kotlin의 null-safety를 최대한 활용
- Java 코드에서 `@Nullable`로 표시된 걸 Kotlin이 `?` 타입으로 인식
- 런타임 NPE를 컴파일 타임에 잡을 수 있음

**Spring Boot + Kotlin 프로젝트에서 권장되는 설정**입니다. start.spring.io에서 Kotlin 프로젝트 생성 시 자동으로 포함됩니다.

**추가 Q&A: String? 타입 처리 상세 설명**

**Q:** -Xjsr305=strict를 설정하면 Kotlin이 이 정보를 인식해서 String? 타입으로 처리한다는 이 말이 무슨 뜻이야?

**A:** (질문)

**피드백:**

**Kotlin의 Null Safety 시스템:**
```kotlin
val a: String = "hello"   // null 불가능 (Non-null)
val b: String? = null     // null 가능 (Nullable)

a.length  // OK
b.length  // 컴파일 에러! null 체크 필요
b?.length // OK (safe call)
```

**문제: Java 코드를 Kotlin에서 호출할 때**

Java에는 Kotlin처럼 타입 시스템에 null 정보가 없습니다:
```java
// Java 코드 (Spring Framework 내부)
public class HttpServletRequest {
    public String getHeader(String name) {  // null을 반환할 수도 있고 아닐 수도 있음
        ...
    }
}
```

**플랫폼 타입 (String!)**

기본적으로 Kotlin은 Java에서 온 타입을 플랫폼 타입으로 처리합니다:
```kotlin
val userAgent = request.getHeader("User-Agent")
// userAgent: String!  ← 플랫폼 타입 (느낌표)
// "이게 null인지 아닌지 Kotlin은 모른다"
// 컴파일러가 null 체크를 강제하지 않음

userAgent.length           // 런타임에 NPE 가능!
userAgent?.length          // 안전하지만 필수 아님
```

**JSR-305 어노테이션이 있는 경우**

Spring은 메서드에 null 가능성을 어노테이션으로 표시합니다:
```java
// Spring의 Java 코드
@Nullable  // ← JSR-305 어노테이션
public String getHeader(String name) { ... }
```

**-Xjsr305=strict의 효과:**

설정 없을 때:
```kotlin
val userAgent = request.getHeader("User-Agent")
// userAgent: String!  (플랫폼 타입, null 체크 강제 안 함)
userAgent.length  // 컴파일 OK, 런타임 NPE 가능!
```

-Xjsr305=strict 설정 시:
```kotlin
val userAgent = request.getHeader("User-Agent")
// userAgent: String?  (Nullable 타입으로 인식!)
userAgent.length  // 컴파일 에러!
userAgent?.length  // OK
userAgent!!.length // OK (개발자가 명시적으로 책임짐)
```

**실제 예시 - AccessLogFilter.kt:**
```kotlin
userAgent = request.getHeader(USER_AGENT_HEADER).orEmpty()
```
- `-Xjsr305=strict` 때문에 `getHeader()`가 `String?`를 반환
- `.orEmpty()`는 `String?`를 `String`으로 변환 (null이면 빈 문자열)

| 상황 | 타입 | 컴파일러 체크 | 런타임 NPE |
|------|------|-------------|-----------|
| 설정 없음 | `String!` | 안 함 | 가능 |
| `-Xjsr305=strict` | `String?` | **강제** | 방지됨 |

**핵심:** strict 설정은 Java의 `@Nullable` 어노테이션 정보를 Kotlin 타입 시스템에 반영해서, 컴파일 타임에 null 체크를 강제합니다.

---

## Part 2: Spring 설정

### 질문 2-1: AutoConfiguration.imports 파일

**Q:**
```
// META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.pumpkin.log.spring.mvc.config.PumpkinLogAutoConfiguration
```

이 파일의 역할은 무엇인가요? 이 파일이 없으면 어떻게 되나요?

**A:** 이 파일은 PumpkinLogAutoConfiguration 정보를 읽어서 pumpkin-log-spring-mvc를 사용하는 프로젝트에서 자동으로 설정할 수 있게 만들어주는 파일입니다.

**피드백:** 맞습니다! 핵심을 잘 이해하고 계세요. 조금 더 보충하면:

**Spring Boot의 자동 설정 메커니즘:**

1. Spring Boot 앱이 시작되면 classpath의 모든 JAR에서 이 파일을 스캔
2. 파일에 나열된 클래스들을 자동으로 `@Configuration`으로 등록
3. 각 Configuration 클래스의 `@Conditional*` 조건에 따라 Bean 생성 여부 결정

**이 파일이 없으면:**
```kotlin
// demo-server-mvc에서
@SpringBootApplication
class DemoServerMvcApplication
```
- `PumpkinLogAutoConfiguration`이 자동으로 로드되지 않음
- `AccessLogFilter`, `ConsoleLogAppender` 등의 Bean이 생성되지 않음
- SDK가 전혀 동작하지 않음

**수동으로 해결하려면:**
```kotlin
@SpringBootApplication
@Import(PumpkinLogAutoConfiguration::class)  // 명시적으로 import 필요
class DemoServerMvcApplication
```

**Spring Boot 버전별 차이:**
- Spring Boot 2.7 이전: `META-INF/spring.factories` 사용
- Spring Boot 3.0 이후: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 사용 (새로운 방식)

이 프로젝트는 Spring Boot 3.4를 사용하므로 새로운 방식을 사용한 것이 올바릅니다.

---

### 질문 2-2: PumpkinLogAutoConfiguration 어노테이션들

**Q:**
```kotlin
@Configuration
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(PumpkinLogProperties::class)
class PumpkinLogAutoConfiguration {
```

4개의 어노테이션이 붙어있는데, 각각의 역할이 무엇인가요? 하나씩 설명해주세요.

**A:**
- `@Configuration`은 PumpkinLogAutoConfiguration을 스프링 컨테이너에 bean으로 등록하며 동시에 해당 파일에 있는 설정 정보들을 bean으로 등록하기 위한 어노테이션
- `@AutoConfiguration`은 Spring Boot가 해당 class를 자동으로 로드하도록 하는 어노테이션
- `@ConditionalOnWebApplication(type = Type.SERVLET)`은 Spring MVC에서만 동작하도록 설정하는 어노테이션
- `@EnableConfigurationProperties(PumpkinLogProperties::class)`은 PumpkinLogProperties::class에 설정되어 있는 정보들을 PumpkinLogAutoConfiguration에서 사용하도록 하는 어노테이션

**피드백:** 전반적으로 잘 이해하고 계십니다! 각각 조금씩 보충하면:

**1. @Configuration**
- 정확합니다
- 이 클래스가 Bean 정의를 포함하는 설정 클래스임을 표시
- 내부의 `@Bean` 메서드들이 Spring 컨테이너에 Bean으로 등록됨

**2. @AutoConfiguration**
- 방향은 맞지만, 조금 더 정확히 말하면:
- `@AutoConfiguration`은 **`@Configuration`을 이미 포함**하고 있음 (메타 어노테이션)
- 따라서 사실 `@Configuration`은 중복이에요! `@AutoConfiguration`만 있어도 됩니다
- 하지만 명시적으로 붙여도 문제는 없음

```java
// @AutoConfiguration 내부
@Configuration(proxyBeanMethods = false)
public @interface AutoConfiguration { ... }
```

**3. @ConditionalOnWebApplication(type = Type.SERVLET)**
- 정확합니다
- `Type.SERVLET`: Spring MVC (서블릿 기반)
- `Type.REACTIVE`: Spring WebFlux (리액티브 기반)
- 이 조건이 있어서 WebFlux 프로젝트에서는 이 AutoConfiguration이 로드되지 않음

**4. @EnableConfigurationProperties**
- 방향은 맞지만, 더 정확히:
- `@ConfigurationProperties` 클래스를 **Bean으로 등록**하고
- `application.yml`의 값을 해당 클래스에 **바인딩**해줌
- 이 어노테이션 없이 `PumpkinLogProperties`를 주입받으려 하면 Bean을 찾을 수 없다는 에러 발생

```kotlin
// 이 어노테이션 덕분에 가능:
@Bean
fun fileLogAppender(properties: PumpkinLogProperties): FileLogAppender
//                  ↑ 자동 주입됨
```

**추가 질문:** `@AutoConfiguration`이 `@Configuration`을 포함한다면, 왜 둘 다 붙였을까요? 이건 실수일 수도 있고, 명시적으로 보여주려는 의도일 수도 있습니다. 실제로는 `@AutoConfiguration`만 있어도 됩니다.

---

### 질문 2-3: @ConditionalOnProperty vs @ConditionalOnExpression

**Q:**
```kotlin
@Bean
@ConditionalOnProperty(
    prefix = "pumpkin.log.console",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
fun consoleLogAppender(): ConsoleLogAppender

@Bean
@ConditionalOnExpression(FILE_SYNC_CONDITION)
fun fileLogAppender(properties: PumpkinLogProperties): FileLogAppender

companion object {
    private const val FILE_SYNC_CONDITION =
        "\${pumpkin.log.file.enabled:true} && !\${pumpkin.log.file.async.enabled:false}"
}
```

1. `@ConditionalOnProperty`의 `matchIfMissing = true`는 무슨 의미인가요?
2. `fileLogAppender`에서는 왜 `@ConditionalOnProperty` 대신 `@ConditionalOnExpression`을 사용하셨나요?

**A:**
1. `matchIfMissing = true`는 값이 없을 때 true로 설정한다는 말
2. `asyncFileLogAppender`과 반대되는 옵션으로 AsyncFileLogAppender에 이미 `@ConditionalOnProperty`로 설정했기 때문에 `FILE_SYNC_CONDITION`으로 더 간단하게 표현하기 위해 `@ConditionalOnExpression`을 사용

**피드백:**

**1. matchIfMissing = true**
- 정확합니다!
- `application.yml`에 `pumpkin.log.console.enabled` 설정이 **아예 없을 때** → `true`로 간주
- 즉, **기본적으로 활성화** (opt-out 방식)

```yaml
# 이렇게 명시적으로 끄지 않으면 기본 활성화
pumpkin:
  log:
    console:
      enabled: false  # 명시적으로 꺼야 비활성화
```

**2. @ConditionalOnExpression 사용 이유**
- 방향은 맞지만, 핵심 이유는 **복합 조건(AND 조건)** 때문입니다
- `@ConditionalOnProperty`는 **단일 속성**만 체크 가능
- `FileLogAppender`는 두 가지 조건을 **동시에** 만족해야 함:
  - `file.enabled == true` AND
  - `file.async.enabled == false`

```kotlin
// 이건 불가능! @ConditionalOnProperty는 AND 조건 지원 안 함
@ConditionalOnProperty(name = ["file.enabled", "file.async.enabled"], ...)

// 그래서 SpEL(Spring Expression Language) 사용
"\${pumpkin.log.file.enabled:true} && !\${pumpkin.log.file.async.enabled:false}"
//  ↑ file.enabled가 true         ↑ async.enabled가 false
```

**SpEL 문법 설명:**
- `\${property:default}`: 프로퍼티 값 읽기, 없으면 기본값 사용
- `&&`: AND 조건
- `!`: NOT (부정)

**결과:**
| file.enabled | async.enabled | FileLogAppender | AsyncFileLogAppender |
|--------------|---------------|-----------------|---------------------|
| true (기본) | false (기본) | ✅ 생성 | ❌ 미생성 |
| true | true | ❌ 미생성 | ✅ 생성 |
| false | - | ❌ 미생성 | ❌ 미생성 |

이렇게 **상호 배타적(mutually exclusive)** Bean 등록을 구현한 것입니다.

**추가 Q&A: SpEL 조건문 상세 설명**

**Q:** `"\${pumpkin.log.file.enabled:true} && !\${pumpkin.log.file.async.enabled:false}"` 이 SpEL의 조건문이 헷갈려. `!`이 들어가서 더 헷갈리는 것 같아. file.enabled: true로 설정되어 있고, file.async.enabled는 아예 설정 안 되어 있다면 자동으로 file.async.enabled: false가 되는 건가?

**A:** (질문)

**피드백:**

**기본 문법 이해:**
```kotlin
"\${pumpkin.log.file.enabled:true} && !\${pumpkin.log.file.async.enabled:false}"
```

| 부분 | 의미 |
|------|------|
| `\${pumpkin.log.file.enabled:true}` | `file.enabled` 값을 읽음. **없으면 `true`** |
| `\${pumpkin.log.file.async.enabled:false}` | `async.enabled` 값을 읽음. **없으면 `false`** |
| `!` | NOT (반전) |
| `&&` | AND |

**조건 정리:**

- **FileLogAppender 생성 조건:** `file.enabled == true AND async.enabled == false`
- **AsyncFileLogAppender 생성 조건:** `async.enabled == true` (matchIfMissing 없음 → 기본 false)

**케이스별 분석:**

**케이스 1: 아무것도 설정 안 함 (기본값)**
```yaml
pumpkin:
  log:
    # file 설정 없음
```
- `file.enabled` → `true` (기본값)
- `async.enabled` → `false` (기본값)
- 조건: `true && !false = true && true = true` ✅
- **결과:** FileLogAppender 생성됨, AsyncFileLogAppender 생성 안 됨

**케이스 2: async.enabled: true 설정**
```yaml
pumpkin:
  log:
    file:
      enabled: true
      async:
        enabled: true
```
- `file.enabled` → `true`
- `async.enabled` → `true`
- 조건: `true && !true = true && false = false` ❌
- **결과:** FileLogAppender 생성 안 됨, AsyncFileLogAppender 생성됨

**케이스 3: file.enabled: false 설정**
```yaml
pumpkin:
  log:
    file:
      enabled: false
```
- 조건: `false && !false = false && true = false` ❌
- **결과:** FileLogAppender 생성 안 됨, AsyncFileLogAppender 생성 안 됨 (파일 로깅 완전 비활성화)

**전체 정리표:**

| file.enabled | async.enabled | FileLogAppender | AsyncFileLogAppender |
|--------------|---------------|-----------------|---------------------|
| 미설정 (→true) | 미설정 (→false) | ✅ | ❌ |
| true | 미설정 (→false) | ✅ | ❌ |
| true | false | ✅ | ❌ |
| true | true | ❌ | ✅ |
| false | 미설정 (→false) | ❌ | ❌ |
| false | true | ❌ | ✅ (설계 허점!) |

**핵심 포인트:**
1. **`:기본값` 문법**: 설정이 없을 때만 적용되고, 명시적 설정이 있으면 그 값을 사용
2. **`!` 연산자**: async.enabled가 false일 때 true로 반전
3. **상호 배타적 설계**: 정상적인 사용에서는 FileLogAppender와 AsyncFileLogAppender 중 하나만 생성

---

### 질문 2-4: additional-spring-configuration-metadata.json

**Q:**
```json
{
  "properties": [
    {
      "name": "pumpkin.log.file.async.buffer-size",
      "type": "java.lang.Integer",
      "description": "Queue buffer size for async file appender.",
      "defaultValue": 1000
    }
  ]
}
```

이 파일의 역할은 무엇인가요? `kapt`가 자동으로 메타데이터를 생성해주는데, 왜 이 파일을 추가로 작성하셨나요?

**A:** 잘 모르겠어요. kapt가 자동으로 생성해주는 줄도 몰랐어요.

**피드백:** 솔직하게 말씀해주셔서 좋습니다. 설명드릴게요.

**spring-boot-configuration-processor의 동작:**

1. `kapt`가 `@ConfigurationProperties` 클래스를 스캔
2. **자동으로** `build/tmp/kapt3/.../spring-configuration-metadata.json` 생성
3. 빌드 시 이 파일이 JAR에 포함됨

**자동 생성되는 정보:**
- 프로퍼티 이름 (`pumpkin.log.file.enabled`)
- 타입 (`java.lang.Boolean`)
- 기본값 (코드에서 `= true`로 설정한 값)

**자동 생성의 한계:**
- **description(설명)이 없음!**
- Kotlin의 경우 일부 타입 추론이 부정확할 수 있음

**additional-spring-configuration-metadata.json의 역할:**
- 자동 생성된 메타데이터를 **보완/덮어쓰기**
- 주로 **description 추가** 목적
- IDE에서 `application.yml` 작성 시 설명이 표시됨

```yaml
pumpkin:
  log:
    file:
      async:
        buffer-size: |
        # ↑ IDE가 "Queue buffer size for async file appender." 설명을 보여줌
```

**이 파일이 없으면:**
- 자동완성은 되지만 설명이 없음
- 개발자가 각 설정의 의미를 알기 어려움

**파일 위치:**
- 자동 생성: `build/` 디렉토리 (빌드 산출물)
- 수동 작성: `src/main/resources/META-INF/` (소스 코드)
- 최종 JAR에서 두 파일이 **병합(merge)**됨

**요약:**
| 구분 | 자동 생성 | additional (수동) |
|------|----------|------------------|
| 프로퍼티 이름 | ✅ | ✅ (덮어쓰기 가능) |
| 타입 | ✅ | ✅ |
| 기본값 | ✅ | ✅ |
| description | ❌ | ✅ **이게 핵심!** |

---

## Part 3: Core 구현체 상세 분석

### 질문 3-1: ObjectMapperFactory

**Q:**
```kotlin
object ObjectMapperFactory {

    val instance: ObjectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}
```

1. 왜 `class`가 아니라 `object`로 선언하셨나요?
2. `KotlinModule`을 등록하는 이유는 무엇인가요?
3. `JavaTimeModule`을 등록하는 이유는 무엇인가요?
4. `SNAKE_CASE`를 설정한 이유는 무엇인가요?
5. `WRITE_DATES_AS_TIMESTAMPS`를 disable한 이유는 무엇인가요?

**A:** (설명 요청)

**피드백:**

**1. 왜 `object`로 선언했는가?**

`object`는 Kotlin의 싱글톤 패턴입니다.

| 구분 | `class` | `object` |
|------|---------|----------|
| 인스턴스 | 여러 개 생성 가능 | **단 하나만 존재** |
| 생성 방식 | `ObjectMapperFactory()` | `ObjectMapperFactory` (생성자 없음) |
| 초기화 시점 | 호출할 때마다 | 첫 접근 시 한 번 (lazy) |

ObjectMapper를 싱글톤으로 만든 이유:
- ObjectMapper는 **생성 비용이 비쌈** (내부적으로 많은 캐시, 리플렉션 정보 구축)
- 하지만 **thread-safe**하므로 여러 스레드에서 공유해도 안전
- 매번 새로 만들면 성능 낭비 → 하나만 만들어서 재사용

**2. KotlinModule 등록 이유**

Jackson은 원래 Java 라이브러리입니다. Kotlin의 특수한 기능을 이해하지 못합니다.

```kotlin
data class HttpLog(
    val type: String = "log.v1.http",  // 기본값 있음
    val userAgent: String,              // 기본값 없음 (필수)
)
```

KotlinModule이 해결하는 것들:
| 기능 | 설명 |
|------|------|
| Default Parameter | 기본값이 있는 파라미터 지원 |
| data class | 주 생성자(primary constructor)를 통한 역직렬화 |
| nullable 타입 | `String?`을 제대로 처리 |
| `val` 프로퍼티 | getter만 있는 프로퍼티 직렬화 |

**3. JavaTimeModule 등록 이유**

Java 8의 날짜/시간 API(`java.time.*`) 지원:

```kotlin
data class HttpLog(
    val timestamp: Instant = Instant.now()  // java.time.Instant
)
```

JavaTimeModule 없으면:
```
InvalidDefinitionException: Java 8 date/time type `java.time.Instant` not supported
```

지원하는 타입들: `Instant`, `LocalDate`, `LocalDateTime`, `ZonedDateTime`, `Duration` 등

**4. SNAKE_CASE 설정 이유**

| 설정 | JSON 출력 |
|------|----------|
| 기본값 (그대로) | `{"userAgent": "...", "httpStatusCode": 200}` |
| **SNAKE_CASE** | `{"user_agent": "...", "http_status_code": 200}` |

과제 요구사항에서 JSON 필드가 `user_agent`, `http_status_code` 등 **snake_case**로 명시됨. Kotlin 코드는 camelCase 유지하면서, JSON 출력만 snake_case로 변환.

**5. WRITE_DATES_AS_TIMESTAMPS disable 이유**

| 설정 | JSON 출력 |
|------|----------|
| 기본값 (enabled) | `{"timestamp": 1704110400.000000000}` (숫자) |
| **disabled** | `{"timestamp": "2024-01-01T12:00:00Z"}` (ISO 8601) |

과제 요구사항: `"timestamp": "1970-01-01T00:00:00.000Z"` (ISO 8601 형식)

---

**추가 Q&A: ISO 8601과 Instant**

**Q:** Java 8의 Instant 클래스만 ISO 8601 형식을 지원하는 거야?

**A:** (질문)

**피드백:**

아니요, **Instant만 지원하는 건 아닙니다.**

**ISO 8601이란?**
국제 표준 날짜/시간 표현 형식: `2024-01-01T12:30:45.123Z`

**Java 8 java.time 패키지의 클래스들 (모두 ISO 8601 지원):**

| 클래스 | 설명 | ISO 8601 출력 예시 |
|--------|------|-------------------|
| `Instant` | UTC 기준 시점 | `2024-01-01T12:00:00Z` |
| `LocalDateTime` | 타임존 없는 날짜+시간 | `2024-01-01T12:00:00` |
| `LocalDate` | 날짜만 | `2024-01-01` |
| `LocalTime` | 시간만 | `12:00:00` |
| `ZonedDateTime` | 타임존 있는 날짜+시간 | `2024-01-01T12:00:00+09:00[Asia/Seoul]` |
| `OffsetDateTime` | 오프셋 있는 날짜+시간 | `2024-01-01T12:00:00+09:00` |

**왜 Instant를 선택했는가?**

Instant의 특징:
- **UTC 기준** (타임존 정보 없음)
- 전 세계 어디서든 **동일한 시점**을 나타냄
- 로그에 적합 (서버가 어디에 있든 일관된 시간)

```kotlin
// LocalDateTime을 쓰면:
// 서울 서버: 2024-01-01T21:00:00
// 미국 서버: 2024-01-01T12:00:00
// 같은 시점인데 다른 값!

// Instant를 쓰면:
// 서울 서버: 2024-01-01T12:00:00Z
// 미국 서버: 2024-01-01T12:00:00Z
// 항상 UTC 기준으로 동일!
```

---

**추가 Q&A: apply 메서드**

**Q:** `val instance: ObjectMapper = ObjectMapper().apply {}`에서 왜 apply 메서드를 사용했는지, apply 메서드는 보통 어떨 때에 사용하는지 알려줘.

**A:** (질문)

**피드백:**

**apply 함수란?**

```kotlin
val result = 객체.apply {
    // this = 객체
    // 여기서 객체의 메서드/프로퍼티에 접근
}
// result = 객체 자신이 반환됨
```

**apply 없이 쓴다면:**
```kotlin
val instance: ObjectMapper = ObjectMapper()
instance.registerModule(KotlinModule.Builder().build())
instance.registerModule(JavaTimeModule())
instance.propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
instance.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
```
- `instance.`를 반복해서 써야 함
- 코드가 장황해짐

**apply를 쓰면:**
```kotlin
val instance: ObjectMapper = ObjectMapper().apply {
    registerModule(KotlinModule.Builder().build())
    registerModule(JavaTimeModule())
    propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}
```
- `this`가 ObjectMapper를 가리킴 → `instance.` 생략 가능
- 객체 초기화 로직이 **한 블록에 모임**
- 가독성 향상

**Kotlin Scope 함수 비교:**

| 함수 | 컨텍스트 객체 | 반환값 | 주 용도 |
|------|-------------|--------|---------|
| `apply` | `this` | **객체 자신** | 객체 초기화/설정 |
| `also` | `it` | 객체 자신 | 부수 효과 (로깅 등) |
| `let` | `it` | **람다 결과** | null 체크, 변환 |
| `run` | `this` | 람다 결과 | 객체에서 연산 후 결과 반환 |
| `with` | `this` | 람다 결과 | 이미 있는 객체에 여러 작업 |

**apply를 쓰는 대표적 상황:**
1. 객체 초기화/설정 (가장 흔함)
2. Builder 패턴 대용
3. 테스트에서 Mock 설정

**왜 ObjectMapperFactory에서 apply를 썼나?**
1. ObjectMapper 생성과 설정을 **한 곳에서** 처리
2. 설정이 4줄이나 되는데, `instance.`를 반복하지 않아도 됨
3. `apply`는 **객체 자신을 반환**하므로 바로 `val instance`에 할당 가능
4. "이 ObjectMapper는 이렇게 설정되어 있다"가 한눈에 보임

---

### 질문 3-2: FilePathResolver

**Q:**
```kotlin
object FilePathResolver {

    const val DEFAULT_PATH = "/tmp/log.{pid}.jsonl"
    const val PID_PLACEHOLDER = "{pid}"

    private val currentPid: String by lazy {
        ProcessHandle.current().pid().toString()
    }

    fun resolve(path: String? = null): String {
        val basePath = path ?: DEFAULT_PATH

        if (basePath.contains(PID_PLACEHOLDER)) {
            return basePath.replace(PID_PLACEHOLDER, currentPid)
        }

        return insertPidBeforeExtension(basePath)
    }

    private fun insertPidBeforeExtension(path: String): String {
        val dotIndex = path.lastIndexOf('.')
        return if (dotIndex > 0) {
            "${path.substring(0, dotIndex)}.$currentPid${path.substring(dotIndex)}"
        } else {
            "$path.$currentPid"
        }
    }
}
```

1. `currentPid`를 `by lazy`로 선언한 이유는 무엇인가요?
2. `insertPidBeforeExtension`에서 `dotIndex > 0` 조건을 사용한 이유는 무엇인가요? `>= 0`이 아니라?
3. `{pid}` 플레이스홀더가 없을 때 왜 자동으로 PID를 삽입하도록 구현하셨나요?

**A:**
1. by lazy는 잘 모르겠어요.
2. dotIndex를 path.lastIndexOf('.')으로 선언했으니 dotIndex가 0이라면 path에 '.'이 없다는 의미여서 dotIndex > 0으로 설정했어요.
3. {pid}는 문제의 요구사항에 있었기 때문이에요.

**피드백:**

**1. by lazy란?**

`by lazy`는 **지연 초기화(Lazy Initialization)** 패턴입니다.

```kotlin
private val currentPid: String by lazy {
    ProcessHandle.current().pid().toString()
}
```

| 일반 초기화 | by lazy |
|------------|---------|
| 객체 생성 시 즉시 실행 | **처음 접근할 때** 실행 |
| 안 쓰여도 실행됨 | 안 쓰이면 실행 안 됨 |
| 매번 새로 계산 (val 아니면) | **한 번만 계산**, 결과 캐시 |

**왜 여기서 lazy를 썼나?**
- `ProcessHandle.current().pid()`는 시스템 호출 → 약간의 비용
- FilePathResolver가 로드되자마자 PID를 구할 필요 없음
- 실제로 `resolve()`가 호출될 때 처음 한 번만 PID를 구하고 캐시

```kotlin
// 첫 번째 접근
FilePathResolver.resolve("/tmp/log.jsonl")  // 이때 pid() 호출, 결과 저장

// 두 번째 접근
FilePathResolver.resolve("/var/log/app.jsonl")  // 캐시된 값 사용, pid() 호출 안 함
```

**2. dotIndex > 0 조건 - 수정이 필요해요!**

`lastIndexOf('.')`의 반환값:
| 값 | 의미 |
|----|------|
| `-1` | '.'이 **없음** |
| `0` | '.'이 **맨 앞**에 있음 (예: `.gitignore`) |
| `> 0` | '.'이 중간이나 끝에 있음 (예: `log.jsonl`) |

**dotIndex가 0이면 '.'이 없다는 의미가 아닙니다!**

```kotlin
".hidden".lastIndexOf('.')  // 0 (맨 앞에 있음)
"noext".lastIndexOf('.')    // -1 (없음)
"log.jsonl".lastIndexOf('.') // 3
```

**그러면 왜 > 0인가?**

실제 의도는 아마 이랬을 거예요:
- `.hidden` 같은 파일은 확장자가 아니라 **숨김 파일**
- 이런 경우 `.` 앞에 PID를 넣으면 이상해짐: `12345.hidden`
- 그래서 `dotIndex > 0`으로 해서 `.`이 맨 앞인 경우는 **else 분기**로 보냄

```kotlin
// dotIndex > 0일 때 (정상 확장자)
"log.jsonl" → "log.12345.jsonl"

// dotIndex == 0일 때 (숨김 파일) → else 분기
".hidden" → ".hidden.12345"

// dotIndex == -1일 때 (확장자 없음) → else 분기
"logfile" → "logfile.12345"
```

하지만 `-1`과 `0`을 구분하지 않고 같은 else로 보내는 건 **의도한 건지 실수인지** 확인이 필요합니다.

**3. 자동 PID 삽입**

맞습니다. 과제 요구사항에 `/tmp/log.{pid}.jsonl` 형식이 명시되어 있었죠.

추가로 **사용자 편의성** 측면도 있어요:
- 사용자가 `{pid}` 넣는 걸 잊어버려도 자동으로 PID가 들어감
- 여러 인스턴스가 같은 파일에 쓰는 충돌 방지

**추가 Q&A: PID는 바뀌지 않나?**

**Q:** `resolve()`가 호출될 때 처음 한 번만 PID를 구하고 캐시한다면 이 PID가 바뀔 일은 없나? 없다면 왜 바뀔 일이 없는 거지?

**A:** (질문)

**피드백:**

**PID(Process ID)란?**
- 운영체제가 **각 프로세스에 부여하는 고유 번호**
- 프로세스가 **시작될 때 할당**되고, **종료될 때까지 유지**
- 프로세스가 실행 중인 동안은 **절대 변하지 않음**

**프로세스 생명주기:**
```
[Spring Boot 앱 시작]
    ↓
OS가 PID 할당 (예: 12345)
    ↓
앱 실행 중... (PID = 12345 유지)
    ↓
[앱 종료]
    ↓
PID 12345 반환 (다른 프로세스가 나중에 재사용 가능)
```

**왜 바뀌지 않나?**
1. **하나의 JVM = 하나의 프로세스**: Spring Boot 앱이 실행되면 JVM 프로세스가 생성되고, 이 JVM이 종료될 때까지 같은 PID 유지
2. **PID가 바뀌려면 프로세스가 재시작되어야 함**: 앱을 재시작하면 새로운 PID가 할당되지만, 그건 새로운 프로세스이고 `FilePathResolver`도 새로 로드됨

| 상황 | PID | by lazy |
|------|-----|---------|
| 앱 실행 중 | 고정 | 한 번 계산 후 캐시 |
| 앱 재시작 | 새 PID | 새로 초기화됨 |

**결론:** 같은 프로세스 내에서 PID는 절대 바뀌지 않으므로, `by lazy`로 한 번만 구해서 캐시해도 문제없습니다.

---

### 질문 3-3: FileLogAppender

**Q:**
```kotlin
class FileLogAppender(
    filePath: String? = null,
    private val createDirectories: Boolean = true
) : LogAppender {

    private val resolvedFilePath: String = FilePathResolver.resolve(filePath)
    private val objectMapper = ObjectMapperFactory.instance

    init {
        validateAndPrepareDirectory()
    }

    private fun validateAndPrepareDirectory() {
        val parentDir = File(resolvedFilePath).parentFile ?: return
        if (parentDir.exists()) return

        if (createDirectories) {
            require(parentDir.mkdirs()) {
                "Failed to create directory: ${parentDir.absolutePath}"
            }
        } else {
            throw IllegalArgumentException("Directory does not exist: ${parentDir.absolutePath}")
        }
    }

    override fun append(log: HttpLog) {
        FileWriter(resolvedFilePath, true).use { writer ->
            writer.appendLine(objectMapper.writeValueAsString(log))
        }
    }
}
```

1. `init` 블록에서 `validateAndPrepareDirectory()`를 호출한 이유는 무엇인가요?
2. `parentFile ?: return`에서 `?: return`은 어떤 경우에 실행되나요?
3. `require`와 `throw IllegalArgumentException`의 차이는 무엇인가요?
4. `FileWriter(resolvedFilePath, true)`에서 두 번째 파라미터 `true`의 의미는 무엇인가요?
5. `.use { }`는 왜 사용하셨나요?

**A:**
1. init 블록에서 validateAndPrepareDirectory()를 호출한 이유는 파일의 directory가 유효한지, 파일이 없다면 파일 경로를 만들어주기 위해서
2. parentFile ?: return에서 ?: return은 파일 경로가 존재한다면 return
3. require이 좀 더 kotlin에서 많이 쓰이는 문법으로 if 대신에 사용하는 것
4. FileWriter(resolvedFilePath, true)에서 두 번째 파라미터 true의 의미는 쓰기가 가능하게 한다는 의미
5. .use { }는 해당 FileWriter를 연다는 의미

**피드백:**

**1. init 블록에서 validateAndPrepareDirectory() 호출 - ✅ 맞습니다**

추가로 이런 패턴을 **Fail-Fast**라고 합니다:
- 객체 생성 시점에 문제를 빨리 발견
- 나중에 `append()` 호출할 때 실패하는 것보다 **생성자에서 바로 실패**하는 게 디버깅에 유리
- 잘못된 설정으로 앱이 시작되는 걸 방지

**2. parentFile ?: return - ❌ 틀렸습니다**

`?: return`은 **왼쪽이 null일 때** return합니다.

```kotlin
val parentDir = File(resolvedFilePath).parentFile ?: return
```

| resolvedFilePath | parentFile | 동작 |
|------------------|------------|------|
| `/tmp/log.jsonl` | `/tmp` (File 객체) | 계속 진행 |
| `log.jsonl` (상대 경로) | `null` | **return** (검증 스킵) |

`parentFile`이 null인 경우:
- 파일 경로가 상대 경로이고 부모 디렉토리가 없을 때
- 예: `"log.jsonl"` → 부모 디렉토리 없음 → 현재 디렉토리에 생성될 것이므로 검증 불필요

**추가 Q&A: 상대 경로가 들어오면 잘못된 건가?**

**Q:** `"log.jsonl"` → 부모 디렉토리 없음 → 현재 디렉토리에 생성될 것이므로 검증 불필요라고 했는데 log.jsonl이 path 값으로 들어왔다면 잘못된 거 아니야? log.jsonl은 파일이름 아닌가?

**A:** (질문)

**피드백:**

**File 클래스의 parentFile 동작:**
```kotlin
// 절대 경로
File("/tmp/log.jsonl").parentFile      // → File("/tmp")
File("/var/log/app.jsonl").parentFile  // → File("/var/log")

// 상대 경로
File("log.jsonl").parentFile           // → null (부모 정보 없음)
File("logs/app.jsonl").parentFile      // → File("logs")
```

**"log.jsonl"이 path로 들어오면?**
1. `File("log.jsonl").parentFile` → `null`
2. `?: return` 실행 → 검증 스킵
3. `append()` 호출 시 → **현재 작업 디렉토리(CWD)**에 파일 생성

**잘못된 건가?**

| 구분 | 상대 경로 (`log.jsonl`) | 절대 경로 (`/tmp/log.jsonl`) |
|------|----------------------|---------------------------|
| 동작 | CWD에 생성 | 지정된 위치에 생성 |
| 문제점 | CWD가 어딘지에 따라 달라짐 | 항상 같은 위치 |
| 실무 권장 | ❌ | ✅ |

**기술적으로는 동작합니다.** 하지만 실무적으로는 절대 경로가 권장됩니다.

`?: return`의 의도:
- "부모 디렉토리 정보가 없으면 검증할 게 없으니 그냥 넘어가자"
- 현재 디렉토리는 이미 존재하니까 디렉토리 생성이 필요 없음

더 엄격하게 하려면 `require(file.isAbsolute)`로 절대 경로를 강제할 수도 있지만, 현재 구현은 "유연함"을 선택한 것입니다.

**3. require vs throw - 부분적으로 맞습니다**

```kotlin
// require 사용
require(parentDir.mkdirs()) {
    "Failed to create directory: ${parentDir.absolutePath}"
}

// 위와 동일한 코드
if (!parentDir.mkdirs()) {
    throw IllegalArgumentException("Failed to create directory: ${parentDir.absolutePath}")
}
```

| 함수 | 던지는 예외 | 용도 |
|------|-----------|------|
| `require(조건)` | `IllegalArgumentException` | **인자/상태 검증** |
| `check(조건)` | `IllegalStateException` | **상태 검증** |
| `assert(조건)` | `AssertionError` | 디버깅용 |

`require`는 **가독성**과 **의도 표현**이 더 명확합니다:
- "이 조건이 **필수(require)**다"라는 의미가 코드에 드러남

**4. FileWriter의 두 번째 파라미터 - ❌ 틀렸습니다**

```kotlin
FileWriter(resolvedFilePath, true)
//                          ↑ append 모드
```

| 값 | 의미 |
|----|------|
| `true` | **Append 모드**: 기존 내용 유지, 끝에 추가 |
| `false` (기본값) | **Overwrite 모드**: 기존 내용 삭제, 새로 쓰기 |

```kotlin
// true (append)
// 파일 내용: "line1\n"
writer.write("line2\n")
// 결과: "line1\nline2\n"

// false (overwrite)
// 파일 내용: "line1\n"
writer.write("line2\n")
// 결과: "line2\n" (line1 사라짐)
```

JSONL 로그는 **계속 추가**해야 하므로 `true`가 필수입니다.

**5. .use { } - ❌ 틀렸습니다**

`.use { }`는 **자동으로 리소스를 닫아주는** 함수입니다. Java의 try-with-resources와 동일.

```kotlin
// use 사용
FileWriter(path, true).use { writer ->
    writer.appendLine(json)
}  // ← 여기서 자동으로 writer.close() 호출

// use 없이 (위험!)
val writer = FileWriter(path, true)
writer.appendLine(json)
writer.close()  // 직접 닫아야 함. 예외 발생 시 안 닫힐 수 있음!
```

**use의 장점:**
- 블록이 끝나면 **자동으로 close()** 호출
- **예외가 발생해도** close()가 호출됨
- 리소스 누수 방지

```kotlin
// use 내부 구현 (간략화)
inline fun <T : Closeable, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        this.close()  // 항상 실행!
    }
}
```

---

**추가 Q&A: FileWriter 선택 이유**

**Q:** 왜 AsyncFileLogAppender, FileLogAppender에서 FileWriter를 사용했어?

**A:** (질문)

**피드백:**

**1. 텍스트 기반 출력**

JSON 로그는 **문자열(텍스트)**입니다. Java I/O에서:
- **Character Stream** (Writer 계열): 텍스트 처리에 적합
- **Byte Stream** (OutputStream 계열): 바이너리 데이터에 적합

```kotlin
// FileWriter - Character Stream (텍스트용)
FileWriter(path, true)

// FileOutputStream - Byte Stream (바이너리용)
FileOutputStream(path, true)
```

**2. 간단한 API**

FileWriter는 문자열을 직접 쓸 수 있습니다:
```kotlin
writer.write("hello")  // 바로 문자열 쓰기 가능
```

FileOutputStream은 바이트 배열로 변환해야 합니다:
```kotlin
outputStream.write("hello".toByteArray(Charsets.UTF_8))
```

**3. Append 모드 지원**

두 번째 생성자 파라미터로 append 모드를 간단히 설정:
```kotlin
FileWriter(path, true)  // append = true
```

**4. BufferedWriter와 조합**

```kotlin
FileWriter(path, true).buffered()
```
- `buffered()` 확장 함수로 BufferedWriter 래핑
- 버퍼링으로 I/O 성능 향상 (매번 디스크 쓰기 대신 버퍼에 모았다가 flush)

**대안들과 비교:**

| 클래스 | 특징 | 선택하지 않은 이유 |
|--------|------|-------------------|
| `FileOutputStream` | 바이너리 스트림 | 텍스트 처리에 불필요한 변환 필요 |
| `PrintWriter` | print/println 메서드 | 예외 삼킴 문제, 불필요한 기능 |
| `Files.newBufferedWriter()` | NIO API | 간단한 용도에 과함 |
| `RandomAccessFile` | 랜덤 액세스 | append only에 불필요 |

**실제 사용 패턴:**

FileLogAppender (동기):
```kotlin
FileWriter(resolvedFilePath, true).buffered().use { writer ->
    writer.appendLine(objectMapper.writeValueAsString(log))
}
```
- 매 로그마다 파일 열고 닫음 (안전하지만 느림)

AsyncFileLogAppender (비동기):
```kotlin
FileWriter(resolvedFilePath, true).buffered().use { writer ->
    while (running.get() || queue.isNotEmpty()) {
        // batch로 여러 로그 쓰기
        writeBatch(writer, batch)
    }
}
```
- Worker Thread에서 한 번 열고 계속 재사용 (효율적)

---

### 질문 3-4: AsyncFileLogAppender 동시성 처리

**Q:**
```kotlin
private val queue = LinkedBlockingQueue<HttpLog>(bufferSize)
private val running = AtomicBoolean(true)
```

1. 왜 LinkedBlockingQueue를 선택했나요? (ArrayBlockingQueue, ConcurrentLinkedQueue 등과 비교)
2. running에 AtomicBoolean을 사용한 이유는? (일반 Boolean과 차이)
3. Closeable 인터페이스를 구현한 이유는?

**A:**
1. ArrayBlockingQueue와 비교했지만, bufferSize가 있어서 각각 미리 생성되는 것은 똑같지만, LinkedBlockingQueue는 양 끝에 lock을 걸어서 producer와 consumer가 각각 경쟁하는 일이 없게 해주는 것으로 알고 있습니다. ArrayBlockingQueue는 lock이 1개 있구요. 하지만 그렇다고 2개가 성능에 엄청 큰 차이는 없지만 조금이라도 효율적으로 하고자 LinkedBlockingQueue를 사용했습니다.
2. running에서 AtomicBoolean을 사용한 이유는 멀티스레드 상황에서 Worker Thread를 확인할 때 원자성을 보장하기 위해서입니다.
3. Closeable 인터페이스를 구현한 이유는 Worker Thread가 계속 열려 있으면 자원 낭비가 생기기 때문입니다. shutdown hook을 통해 Spring app이 종료될 때 Worker thread도 함께 종료하게 만들기 위함입니다.

**피드백:**

**1. LinkedBlockingQueue vs ArrayBlockingQueue - ✅ 정확합니다!**

핵심을 정확히 짚으셨어요.

| 구분 | LinkedBlockingQueue | ArrayBlockingQueue |
|------|--------------------|--------------------|
| Lock 개수 | **2개** (putLock, takeLock) | **1개** |
| Producer/Consumer | 동시 작업 가능 | 상호 배제 |
| 메모리 | 노드 객체 생성 (약간의 GC 부담) | 배열 고정 할당 |

```java
// LinkedBlockingQueue 내부 (간략화)
private final ReentrantLock putLock = new ReentrantLock();
private final ReentrantLock takeLock = new ReentrantLock();

// ArrayBlockingQueue 내부
private final ReentrantLock lock = new ReentrantLock();
```

**이 프로젝트에서 LinkedBlockingQueue가 더 적합한 이유:**
- Producer(HTTP 요청 스레드): 여러 개가 동시에 `offer()` 호출
- Consumer(Worker Thread): 1개가 `drainTo()` 호출
- Two-lock으로 Producer와 Consumer가 서로 블로킹하지 않음

**2. AtomicBoolean - ✅ 맞습니다, 조금 더 보충하면:**

두 가지 문제를 해결합니다:

**a) 가시성 (Visibility)**
```kotlin
// 일반 Boolean
private var running = true

// Thread A (shutdown)
running = false  // 메인 메모리에 언제 반영될지 모름

// Thread B (worker)
while (running) { ... }  // CPU 캐시의 옛날 값(true)을 볼 수 있음!
```

**b) 원자성 (Atomicity) - compareAndSet**
```kotlin
fun shutdown() {
    if (!running.compareAndSet(true, false)) return  // 원자적 비교+교환
    // ...
}
```
- 여러 스레드가 동시에 `shutdown()` 호출해도 **한 번만** 실행됨
- 일반 Boolean로는 이런 원자적 연산 불가능

**3. Closeable 구현 - 방향은 맞지만 구분이 필요해요**

Closeable vs Shutdown Hook은 다른 개념입니다:

| 구분 | Closeable | Shutdown Hook |
|------|-----------|---------------|
| 호출 시점 | **명시적**으로 `close()` 호출 시 | JVM 종료 시 **자동** |
| 용도 | try-with-resources, `.use {}` | 비정상 종료 대비 |

```kotlin
class AsyncFileLogAppender : LogAppender, Closeable {

    init {
        // Shutdown Hook: JVM 종료 시 자동 호출 (안전망)
        shutdownHook = Thread({ shutdown() }, "...")
        Runtime.getRuntime().addShutdownHook(shutdownHook)
    }

    // Closeable: 명시적 호출용
    override fun close() = shutdown()
}
```

**Closeable을 구현한 진짜 이유:**
1. **`.use {}` 패턴 지원**: `appender.use { ... }` 가능
2. **Spring Bean 생명주기**: `@Bean` + `destroyMethod`와 호환
3. **명시적 자원 해제**: 테스트에서 `appender.close()` 호출 가능

**Shutdown Hook은 보험:**
- 개발자가 `close()` 호출을 잊어도 JVM 종료 시 정리됨
- 하지만 **정상적인 경우에는 명시적으로 `close()` 호출**하는 게 좋음

---

### 질문 3-5: AsyncFileLogAppender의 drainBatch 메서드

**Q:**
```kotlin
private fun drainBatch(batch: MutableList<HttpLog>) {
    queue.drainTo(batch, batchSize)
    if (batch.isEmpty()) {
        queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)?.let { log ->
            batch.add(log)
            queue.drainTo(batch, batchSize - 1)
        }
    }
}
```

1. `drainTo`와 `poll`을 함께 사용한 이유는 무엇인가요? 왜 `drainTo`만 쓰지 않았나요?
2. `poll`에 timeout을 준 이유는 무엇인가요? timeout 없이 `take()`를 쓰면 어떤 문제가 있나요?
3. `?.let { }`을 사용한 이유는 무엇인가요?

**A:**
1. Worker Thread가 계속 실행되는 것을 방지하기 위해 그리고 queue에 log가 없을 때도 Thread가 실행되는 것을 방지하기 위해 POLL_TIMEOUT_MS만큼 쉬고 1개만 poll을 하는 형태로 구현했습니다만 Thread.sleep()을 사용하고 drainTo만 사용하는게 훨씬 가독성이 나아질 것 같습니다.
2. 어떤 문제가 있는지는 잘 모르겠어요.
3. 잘 모르겠어요.

**피드백:**

**1. drainTo와 poll을 함께 사용한 이유 - 부분적으로 맞습니다**

방향은 맞지만, 핵심 이유를 더 명확히 설명하면:

**drainTo의 특성:**
```kotlin
queue.drainTo(batch, batchSize)  // Non-blocking!
```
- **즉시 반환**: 큐가 비어있으면 0개를 담고 바로 리턴
- 대기하지 않음

**만약 drainTo만 썼다면:**
```kotlin
// 문제 있는 코드
while (running.get()) {
    queue.drainTo(batch, batchSize)
    if (batch.isNotEmpty()) {
        writeBatch(writer, batch)
    }
    // 큐가 비어있으면? → 무한 루프로 CPU 100% 점유!
}
```

**poll(timeout)의 역할:**
```kotlin
queue.poll(100, TimeUnit.MILLISECONDS)  // 최대 100ms 대기
```
- 큐가 비어있으면 **100ms 동안 블로킹** (CPU 쉼)
- 로그가 들어오면 즉시 깨어남
- **"바쁜 대기(busy waiting)" 방지**

**전체 흐름:**
```
1. drainTo로 한 번에 여러 개 가져오기 시도 (빠름)
2. 못 가져왔으면? → poll(timeout)으로 대기하면서 1개 기다림
3. 1개 가져왔으면 → 다시 drainTo로 나머지 가져오기
```

`Thread.sleep()` + `drainTo`만 쓰는 방법도 가능하지만, `poll(timeout)`이 더 **반응성이 좋습니다**:
- `sleep(100)`: 로그가 들어와도 무조건 100ms 대기
- `poll(100)`: 로그가 들어오면 **즉시** 깨어남

**2. poll(timeout) vs take() - 핵심 차이**

```kotlin
// take() - 무한 대기
val log = queue.take()  // 로그가 올 때까지 영원히 블로킹

// poll(timeout) - 제한 대기
val log = queue.poll(100, TimeUnit.MILLISECONDS)  // 최대 100ms만 대기
```

**take()의 문제:**
```kotlin
while (running.get()) {
    val log = queue.take()  // ← shutdown해도 여기서 영원히 대기!
    // ...
}
```

shutdown 시나리오:
```
1. shutdown() 호출 → running = false
2. Worker Thread는 take()에서 블로킹 중
3. running.get()을 체크할 기회가 없음!
4. 로그가 들어올 때까지 종료 불가
```

**poll(timeout)의 장점:**
```kotlin
while (running.get() || queue.isNotEmpty()) {
    // ...
    queue.poll(100, TimeUnit.MILLISECONDS)  // 100ms 후 깨어남
    // → 다음 루프에서 running.get() 체크 가능!
}
```

shutdown 시나리오:
```
1. shutdown() 호출 → running = false
2. Worker Thread는 poll()에서 최대 100ms 대기
3. 100ms 후 깨어나서 running.get() = false 확인
4. 루프 종료 → graceful shutdown 완료
```

**3. ?.let { } 사용 이유**

```kotlin
queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)?.let { log ->
    batch.add(log)
    queue.drainTo(batch, batchSize - 1)
}
```

**poll()의 반환값:**
| 상황 | 반환값 |
|------|--------|
| 로그가 있음 | `HttpLog` 객체 |
| timeout 경과 | **`null`** |

**?.let의 역할:**
```kotlin
// poll 결과가 null이 아닐 때만 블록 실행
poll()?.let { log ->
    // log는 non-null HttpLog
    batch.add(log)
}

// 위 코드는 아래와 동일
val log = poll()
if (log != null) {
    batch.add(log)
}
```

**왜 if 대신 ?.let을 썼나?**
- Kotlin 관용적 표현 (idiomatic)
- null 체크 + 스코프 지정을 한 줄로
- `log` 변수가 블록 안에서만 유효 (스코프 제한)

---

### 질문 3-6: AsyncFileLogAppender의 shutdown 메서드

**Q:**
```kotlin
fun shutdown() {
    if (!running.compareAndSet(true, false)) return

    worker.join(SHUTDOWN_TIMEOUT_MS)
    if (worker.isAlive) worker.interrupt()

    runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
}
```

1. `compareAndSet(true, false)`을 사용한 이유는 무엇인가요? 단순히 `running.set(false)`를 쓰면 어떤 문제가 있나요?
2. `worker.join(SHUTDOWN_TIMEOUT_MS)` 후에 `interrupt()`를 호출하는 이유는 무엇인가요?
3. `removeShutdownHook`을 `runCatching`으로 감싼 이유는 무엇인가요?

**A:** (설명 요청)

**피드백:**

**1. compareAndSet(true, false) 사용 이유**

단순히 `set(false)`를 쓰면:
```kotlin
fun shutdown() {
    running.set(false)  // 항상 실행됨

    worker.join(5000)   // 5초 대기
    worker.interrupt()  // interrupt 시도
    removeShutdownHook(...)
}
```

**문제점: 여러 스레드가 동시에 shutdown() 호출 시**
```
Thread A: running.set(false)
Thread B: running.set(false)
Thread A: worker.join(5000)  ← 5초 대기
Thread B: worker.join(5000)  ← 또 5초 대기 (불필요!)
Thread A: interrupt()
Thread B: interrupt()         ← 중복 호출
```

**compareAndSet 사용 시:**
```kotlin
fun shutdown() {
    if (!running.compareAndSet(true, false)) return  // 첫 번째만 통과
    // ...
}
```

| 호출 | compareAndSet 결과 | 동작 |
|------|-------------------|------|
| Thread A (첫 번째) | `true` → `false`, 성공 | 계속 진행 |
| Thread B (두 번째) | 이미 `false`, 실패 | **즉시 return** |

**핵심:** shutdown 로직이 **딱 한 번만** 실행되도록 보장 (멱등성)

**2. join() 후 interrupt() 호출 이유**

```kotlin
worker.join(SHUTDOWN_TIMEOUT_MS)  // 5초 대기
if (worker.isAlive) worker.interrupt()  // 아직 살아있으면 강제 종료
```

**Graceful Shutdown 전략:**
```
1단계: 협조적 종료 시도
   - running = false 설정
   - Worker가 스스로 루프 종료하길 5초간 기다림

2단계: 강제 종료 (5초 초과 시)
   - Worker가 poll()에서 블로킹 중일 수 있음
   - interrupt()로 InterruptedException 발생시켜 깨움
```

**processQueue의 InterruptedException 처리:**
```kotlin
private fun processQueue() {
    // ...
    try {
        drainBatch(batch)
        // ...
    } catch (_: InterruptedException) {
        return  // ← interrupt 받으면 즉시 종료
    }
}
```

**왜 바로 interrupt() 안 하나?**
- 가능하면 **정상적으로** 남은 로그를 처리하고 종료하길 원함
- interrupt는 **최후의 수단** (데이터 유실 가능성)

**3. removeShutdownHook을 runCatching으로 감싼 이유**

```kotlin
runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
```

**Shutdown Hook 제거가 실패하는 경우:**
```kotlin
// 상황: JVM이 종료 중일 때 shutdown() 호출
Runtime.getRuntime().removeShutdownHook(shutdownHook)
// → IllegalStateException: Shutdown in progress
```

| 호출 시점 | removeShutdownHook 결과 |
|----------|------------------------|
| 정상 실행 중 `close()` 호출 | ✅ 성공 |
| JVM 종료 중 (Shutdown Hook 내부) | ❌ **IllegalStateException** |

**왜 이런 상황이 발생하나?**
```
1. JVM 종료 시작 → Shutdown Hook 실행
2. shutdownHook Thread가 shutdown() 호출
3. shutdown() 내부에서 removeShutdownHook 시도
4. "이미 종료 중이라 Hook 제거 불가" → 예외 발생
```

**runCatching의 역할:**
- 예외가 발생해도 **무시**하고 계속 진행
- 이미 JVM이 종료 중이면 Hook 제거는 의미 없으니 무시해도 됨

```kotlin
// runCatching 없이
try {
    Runtime.getRuntime().removeShutdownHook(shutdownHook)
} catch (e: Exception) {
    // 무시
}

// Kotlin 관용적 표현
runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
```

---

### 질문 3-7: AsyncFileLogAppender의 에러 처리

**Q:**
```kotlin
private fun createWorkerThread(): Thread =
    Thread({ processQueue() }, WORKER_THREAD_NAME).apply {
        isDaemon = true
        uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            onError?.invoke(e)
        }
        start()
    }

private fun handleWriteError(e: Exception, batch: MutableList<HttpLog>) {
    onError?.invoke(e)
    if (batch.isNotEmpty()) {
        _droppedCount.addAndGet(batch.size.toLong())
        batch.clear()
    }
    Thread.sleep(ERROR_RETRY_DELAY_MS)
}
```

1. Worker Thread를 `isDaemon = true`로 설정한 이유는 무엇인가요?
2. `uncaughtExceptionHandler`를 설정한 이유는 무엇인가요? 설정하지 않으면 어떻게 되나요?
3. `handleWriteError`에서 `Thread.sleep(ERROR_RETRY_DELAY_MS)`를 하는 이유는 무엇인가요?

**A:** (설명 요청)

**피드백:**

**1. isDaemon = true 설정 이유**

Daemon Thread vs User Thread:

| 구분 | User Thread (기본) | Daemon Thread |
|------|-------------------|---------------|
| JVM 종료 | 모든 User Thread 종료 대기 | **기다리지 않음** |
| 용도 | 핵심 비즈니스 로직 | 백그라운드 작업 |
| 예시 | main, 요청 처리 스레드 | GC, 로깅, 모니터링 |

**만약 isDaemon = false (기본값)이면:**
```
1. Spring Boot 앱 종료 요청
2. main 스레드 종료
3. Worker Thread가 아직 실행 중 (User Thread)
4. JVM이 Worker Thread 종료를 기다림
5. Worker Thread는 while(running) 루프 중...
6. 앱이 종료되지 않음! (Shutdown Hook이 running=false 하기 전까지)
```

**isDaemon = true이면:**
```
1. Spring Boot 앱 종료 요청
2. main 스레드 종료
3. Worker Thread는 Daemon이므로 JVM이 기다리지 않음
4. (하지만 Shutdown Hook이 먼저 실행되어 graceful shutdown 시도)
5. JVM 종료
```

**핵심:** 로깅은 **보조 기능**이므로, 로깅 때문에 앱 종료가 막히면 안 됨

**2. uncaughtExceptionHandler 설정 이유**

스레드에서 예외가 발생하면:
```kotlin
// Worker Thread 내부
private fun processQueue() {
    // ...
    throw RuntimeException("예상치 못한 에러!")
}
```

**uncaughtExceptionHandler가 없으면:**
- 예외가 **조용히** 스레드를 죽임
- stderr에 스택트레이스만 출력
- **아무도 모르게** Worker Thread가 죽음
- 이후 로그가 큐에 쌓이기만 하고 파일에 안 써짐

**uncaughtExceptionHandler가 있으면:**
```kotlin
uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
    onError?.invoke(e)  // 사용자 정의 콜백 호출
}
```
- 예외 발생 시 **onError 콜백**이 호출됨
- 사용자가 알림 전송, 로깅, 모니터링 등 처리 가능

```kotlin
// 사용 예시
val appender = AsyncFileLogAppender("/tmp/log.jsonl")
appender.onError = { e ->
    alertSystem.send("로그 Worker 죽음: ${e.message}")
    metrics.increment("log.worker.crash")
}
```

**3. Thread.sleep(ERROR_RETRY_DELAY_MS) 이유**

```kotlin
private fun handleWriteError(e: Exception, batch: MutableList<HttpLog>) {
    onError?.invoke(e)
    if (batch.isNotEmpty()) {
        _droppedCount.addAndGet(batch.size.toLong())
        batch.clear()
    }
    Thread.sleep(ERROR_RETRY_DELAY_MS)  // 100ms 대기
}
```

**왜 대기하나?**

에러가 발생하는 상황들:
- 디스크 꽉 참
- 파일 권한 문제
- 네트워크 드라이브 연결 끊김

**sleep 없이 바로 재시도하면:**
```
에러 발생 → 즉시 재시도 → 에러 → 즉시 재시도 → 에러 → ...
```
- **무한 루프**처럼 CPU 100% 점유
- 에러 로그 폭발
- 시스템 리소스 낭비

**sleep 있으면:**
```
에러 발생 → 100ms 대기 → 재시도 → (에러면) 100ms 대기 → ...
```
- CPU 부하 감소
- 일시적 문제(디스크 I/O 지연 등)가 해결될 시간 제공
- **백오프(backoff)** 전략의 단순한 형태

더 나은 방법 (참고):
```kotlin
// Exponential Backoff
var delay = 100L
while (error) {
    Thread.sleep(delay)
    delay = minOf(delay * 2, 10000)  // 최대 10초
}
```

---

### 질문 3-8: AsyncFileLogAppender의 메트릭과 콜백

**Q:**
```kotlin
// 왜 _ 붙였는지?
private val _droppedCount = AtomicLong(0)
private val _writtenCount = AtomicLong(0)

val droppedCount: Long get() = _droppedCount.get()
val writtenCount: Long get() = _writtenCount.get()

// 왜 Unit 으로 했는지?
var onDropped: ((HttpLog) -> Unit)? = null
var onError: ((Throwable) -> Unit)? = null
```

코드에 주석으로 질문이 달려있네요.
1. 왜 `_` (언더스코어)를 붙였는지?
2. 왜 `Unit`으로 했는지?

**A:** (코드 내 주석으로 질문)

**피드백:**

**1. 왜 `_` (언더스코어)를 붙였는지?**

이 패턴을 **Backing Property** 패턴이라고 합니다.

```kotlin
// 내부용 (mutable)
private val _droppedCount = AtomicLong(0)

// 외부 공개용 (read-only)
val droppedCount: Long get() = _droppedCount.get()
```

**목적: 캡슐화**

| 접근 | 내부 (`_droppedCount`) | 외부 (`droppedCount`) |
|------|----------------------|---------------------|
| 읽기 | ✅ | ✅ |
| 쓰기 | ✅ (`incrementAndGet()`) | ❌ **불가능** |

```kotlin
// 내부에서
_droppedCount.incrementAndGet()  // OK

// 외부에서
appender.droppedCount            // OK (읽기)
appender._droppedCount           // 컴파일 에러 (private)
appender.droppedCount = 100      // 컴파일 에러 (val, setter 없음)
```

**왜 이렇게 하나?**
- 메트릭 값은 **SDK 내부에서만 수정** 가능해야 함
- 외부에서 임의로 수정하면 메트릭이 왜곡됨
- `val` + `get()`만 제공하면 **읽기 전용**으로 노출

**Kotlin 네이밍 컨벤션:**
- `_` 접두사: "이건 내부 구현용" 암묵적 표시
- Flow, LiveData 등에서도 같은 패턴 사용

```kotlin
// Android/Flow에서 흔히 보는 패턴
private val _state = MutableStateFlow(initial)
val state: StateFlow<T> = _state.asStateFlow()
```

**2. 왜 `Unit`으로 했는지?**

```kotlin
var onDropped: ((HttpLog) -> Unit)? = null
```

**`Unit`이란?**
- Kotlin에서 **"반환값 없음"**을 나타내는 타입
- Java의 `void`와 유사

```kotlin
// Java
void doSomething() { ... }

// Kotlin
fun doSomething(): Unit { ... }
// 또는 (Unit 생략 가능)
fun doSomething() { ... }
```

**`(HttpLog) -> Unit`의 의미:**
```kotlin
// "HttpLog를 받아서 아무것도 반환하지 않는 함수"
(HttpLog) -> Unit

// 사용 예시
onDropped = { log ->
    println("로그 유실: $log")
    // 반환값 없음
}
```

**왜 반환값이 없나?**
- 콜백은 **알림/통지** 목적
- "이 로그가 버려졌어요"라고 알려주기만 하면 됨
- 콜백의 결과로 뭔가를 하지 않음

**다른 반환 타입이면?**
```kotlin
// 만약 Boolean을 반환한다면?
var onDropped: ((HttpLog) -> Boolean)? = null

// "재시도할까요?" 같은 의미가 될 수 있음
onDropped = { log ->
    shouldRetry(log)  // true면 재시도?
}
```
- 하지만 현재 구현에서는 재시도 로직이 없음
- 단순 알림이므로 `Unit`이 적절

---

## Part 4: 테스트 코드 분석

### 질문 4-1: MockK 기본 사용법

**추가 질문: 왜 MockK를 사용했나요? 다른 Mocking 라이브러리도 있을 텐데요.**

**A:**

## MockK vs 대안들 비교

### 1. Mockito (Java 표준)

```kotlin
// Mockito 사용 시
val accessLogger = mock(AccessLogger::class.java)
`when`(accessLogger.log(any(), any(), any())).thenReturn(Unit)

// 문제점
verify(accessLogger).log(
    eq("Mozilla/5.0"),  // eq() 필요
    any(),
    any()
)
```

**Mockito의 Kotlin 호환 문제:**
- `when`이 Kotlin 예약어 → 백틱 필요 `` `when` ``
- `any()`가 non-null 타입에서 NPE 발생 가능
- final 클래스/메서드 mock 불가 (Kotlin은 기본이 final)
- 코루틴 지원 미흡

### 2. Mockito-Kotlin (Mockito Kotlin 래퍼)

```kotlin
// Mockito-Kotlin 사용 시
val accessLogger = mock<AccessLogger>()
whenever(accessLogger.log(any(), any())).thenReturn(Unit)
```

**개선되었지만:**
- 여전히 Mockito 기반이라 final 클래스 문제 있음
- `mockito-inline` 추가 필요
- 코루틴 suspend 함수 mock 불편

### 3. MockK (Kotlin Native)

```kotlin
// MockK 사용 시
val accessLogger = mockk<AccessLogger>(relaxed = true)

verify {
    accessLogger.log(
        userAgent = "Mozilla/5.0",
        method = any(),
        path = any()
    )
}
```

**MockK의 장점:**
- **Kotlin 전용 설계**: 언어 특성에 최적화
- **final 클래스 기본 지원**: 추가 설정 불필요
- **named parameter 지원**: `userAgent = "..."` 형태로 가독성 좋음
- **코루틴 지원**: `coEvery`, `coVerify`로 suspend 함수 테스트
- **relaxed mock**: 기본값 자동 반환으로 boilerplate 감소

## 비교 표

| 항목 | Mockito | Mockito-Kotlin | MockK |
|------|---------|----------------|-------|
| **Kotlin 친화성** | ❌ 낮음 | 🔶 중간 | ✅ 높음 |
| **final 클래스 mock** | ❌ 추가 설정 | ❌ 추가 설정 | ✅ 기본 지원 |
| **코루틴 지원** | ❌ 미흡 | 🔶 제한적 | ✅ 완벽 |
| **DSL 가독성** | 🔶 중간 | 🔶 중간 | ✅ 좋음 |
| **named parameter** | ❌ 불가 | ❌ 불가 | ✅ 지원 |

## 결론

**Kotlin 프로젝트에서는 MockK가 사실상 표준입니다.**

```kotlin
// MockK의 직관적인 문법
every { repository.findById(any()) } returns user
verify(exactly = 1) { repository.save(match { it.name == "test" }) }

// Kotlin 스타일에 자연스럽게 녹아듦
```

---

**Q:**
```kotlin
@BeforeEach
fun setUp() {
    accessLogger = mockk(relaxed = true)
    filterChain = mockk(relaxed = true)
}

@Test
fun `should pass correct userAgent to AccessLogger`() {
    // ...
    verify {
        accessLogger.log(
            userAgent = "Mozilla/5.0 TestBrowser",
            method = any(),
            path = any(),
            query = any(),
            statusCode = any(),
            duration = any(),
            extra = any()
        )
    }
}
```

1. `mockk(relaxed = true)`에서 `relaxed = true`의 의미는 무엇인가요? `relaxed = false`(기본값)이면 어떻게 되나요?
2. `verify { ... }` 블록의 역할은 무엇인가요?
3. `any()`는 어떤 용도로 사용되나요?

**A:**
1. 모르겠어요.
2. {} 안의 것이 true인지, false인지 확인하는 용도인가?
3. 아무 값이나 random으로 생성하는 거 아닌가?

**피드백:**

**1. relaxed = true의 의미**

**relaxed = false (기본값):**
```kotlin
val accessLogger = mockk<AccessLogger>()  // relaxed = false

// 정의하지 않은 메서드 호출 시
accessLogger.log(...)  // 💥 예외 발생!
// io.mockk.MockKException: no answer found for: AccessLogger.log(...)
```
- **모든 메서드 호출을 미리 정의**해야 함
- 정의 안 된 메서드 호출 → 예외

**relaxed = true:**
```kotlin
val accessLogger = mockk<AccessLogger>(relaxed = true)

// 정의하지 않은 메서드 호출 시
accessLogger.log(...)  // ✅ 예외 없음, 기본값 반환
```
- 정의 안 된 메서드도 **기본값 반환** (Int → 0, String → "", Boolean → false 등)
- "일단 다 허용하고, 검증만 할게" 스타일

**언제 relaxed = true를 쓰나?**
```kotlin
// 테스트에서 accessLogger의 동작은 상관없고,
// "호출되었는지"만 확인하고 싶을 때
val accessLogger = mockk<AccessLogger>(relaxed = true)

// 호출해도 예외 안 남
filter.doFilter(request, response, filterChain)

// 나중에 검증만
verify { accessLogger.log(...) }
```

**2. verify { } 블록의 역할 - 방향은 맞지만 더 정확히**

**verify는 "호출되었는지 확인"입니다:**

```kotlin
verify {
    accessLogger.log(
        userAgent = "Mozilla/5.0 TestBrowser",
        // ...
    )
}
```

| 상황 | 결과 |
|------|------|
| `log(userAgent = "Mozilla/5.0 TestBrowser")`가 호출됨 | ✅ 테스트 통과 |
| `log()`가 호출 안 됨 | ❌ 테스트 실패 |
| `log(userAgent = "다른값")`으로 호출됨 | ❌ 테스트 실패 |

**true/false를 확인하는 게 아니라:**
- "이 메서드가 이 파라미터로 호출되었는가?"를 검증

```kotlin
// verify vs assert 비교
verify { accessLogger.log(...) }  // "log()가 호출되었나?"
assertThat(result).isTrue()       // "result가 true인가?"
```

**verify 옵션들:**
```kotlin
verify(exactly = 1) { ... }   // 정확히 1번 호출
verify(exactly = 0) { ... }   // 호출 안 됨
verify(atLeast = 2) { ... }   // 최소 2번 호출
```

**3. any()의 용도 - ❌ 틀렸습니다**

**any()는 값을 생성하지 않습니다!**

**any() = "어떤 값이든 상관없다" (Argument Matcher)**

```kotlin
verify {
    accessLogger.log(
        userAgent = "Mozilla/5.0 TestBrowser",  // 정확히 이 값
        method = any(),      // ← 무슨 값이든 OK
        path = any(),        // ← 무슨 값이든 OK
        statusCode = any(),  // ← 무슨 값이든 OK
        // ...
    )
}
```

| 실제 호출 | 매칭 결과 |
|----------|----------|
| `log(userAgent="Mozilla/5.0 TestBrowser", method="GET", ...)` | ✅ |
| `log(userAgent="Mozilla/5.0 TestBrowser", method="POST", ...)` | ✅ |
| `log(userAgent="다른값", method="GET", ...)` | ❌ (userAgent 불일치) |

**왜 any()를 쓰나?**
```kotlin
// 이 테스트의 목적: "userAgent가 제대로 전달되는가?"
// method, path, statusCode 등은 이 테스트에서 관심 없음

verify {
    accessLogger.log(
        userAgent = "Mozilla/5.0 TestBrowser",  // 이것만 검증!
        method = any(),      // 나머지는 뭐든 OK
        path = any(),
        // ...
    )
}
```

**any() vs match { } vs eq():**
```kotlin
any()                    // 아무 값이나 OK
match { it > 0 }         // 조건을 만족하는 값만 OK
eq("정확한값")            // 정확히 이 값만 OK
```

---

### 질문 4-2: 비동기 테스트와 JUnit 기능

**Q:**
```kotlin
@TempDir
lateinit var tempDir: Path

@Test
fun `should write log to file asynchronously`() {
    val filePath = tempDir.resolve("test.{pid}.jsonl").toString()
    val appender = AsyncFileLogAppender(filePath = filePath)

    appender.append(createTestLog())

    val actualFile = tempDir.resolve("test.$pid.jsonl").toFile()
    await.atMost(Duration.ofSeconds(2)).untilAsserted {
        assertThat(actualFile.readLines()).hasSize(1)
    }

    appender.shutdown()
}
```

1. `@TempDir`은 무엇이고, 왜 사용하나요?
2. `await.atMost(Duration.ofSeconds(2)).untilAsserted { ... }`는 어떻게 동작하나요? 왜 비동기 테스트에서 이런 방식이 필요한가요?
3. `lateinit var`는 무엇이고, 왜 사용하나요?

**A:** 다 잘 모르겠어요.

**피드백:**

**1. @TempDir이란?**

JUnit 5의 임시 디렉토리 기능입니다:

```kotlin
@TempDir
lateinit var tempDir: Path
```

**역할:**
- 테스트 실행 전에 **임시 디렉토리 자동 생성**
- 테스트 끝나면 **자동 삭제** (cleanup)

**왜 사용하나?**

```kotlin
// @TempDir 없이 (문제 있음)
@Test
fun `test file writing`() {
    val file = File("/tmp/test.jsonl")  // 실제 파일 시스템에 생성
    // 테스트 실행...
    file.delete()  // 수동으로 삭제해야 함 (잊으면 찌꺼기 남음)
}

// @TempDir 사용 (권장)
@TempDir
lateinit var tempDir: Path

@Test
fun `test file writing`() {
    val file = tempDir.resolve("test.jsonl")  // 임시 디렉토리에 생성
    // 테스트 실행...
    // 자동 삭제됨!
}
```

**장점:**
| 문제 | @TempDir로 해결 |
|------|----------------|
| 테스트 간 파일 충돌 | 매 테스트마다 새 디렉토리 |
| 찌꺼기 파일 남음 | 자동 삭제 |
| 경로 하드코딩 | OS 독립적인 임시 경로 |
| 권한 문제 | 쓰기 가능한 경로 보장 |

**2. await.atMost().untilAsserted { }**

Awaitility 라이브러리의 비동기 테스트 도구입니다:

```kotlin
await.atMost(Duration.ofSeconds(2)).untilAsserted {
    assertThat(actualFile.readLines()).hasSize(1)
}
```

**동작 방식:**
```
1. assertThat(actualFile.readLines()).hasSize(1) 실행
2. 실패하면? → 잠깐 대기 후 재시도
3. 성공하면? → 통과!
4. 2초 지나도 실패? → 테스트 실패
```

**왜 필요한가?**

```kotlin
// 문제 있는 코드 (비동기 테스트)
appender.append(createTestLog())  // 비동기로 파일에 쓰기 시작
assertThat(actualFile.readLines()).hasSize(1)  // 💥 실패! 아직 안 써짐
```

```
Timeline:
[0ms] append() 호출 → 큐에 넣음
[0ms] assertThat() 호출 → 파일 아직 비어있음 → 💥 실패!
[50ms] Worker Thread가 파일에 쓰기 완료
```

**Awaitility 사용:**
```kotlin
appender.append(createTestLog())
await.atMost(Duration.ofSeconds(2)).untilAsserted {
    assertThat(actualFile.readLines()).hasSize(1)  // 성공할 때까지 재시도
}
```

```
Timeline:
[0ms] append() 호출
[0ms] await 시작 → assertThat 실패 → 대기
[10ms] 재시도 → 실패 → 대기
[50ms] Worker Thread가 파일에 쓰기 완료
[60ms] 재시도 → 성공! ✅
```

**Thread.sleep()과 비교:**
```kotlin
// Thread.sleep() 사용 (비추천)
appender.append(createTestLog())
Thread.sleep(2000)  // 항상 2초 대기 (느림!)
assertThat(actualFile.readLines()).hasSize(1)

// Awaitility 사용 (추천)
appender.append(createTestLog())
await.atMost(Duration.ofSeconds(2)).untilAsserted {
    assertThat(actualFile.readLines()).hasSize(1)
}  // 성공하면 바로 통과 (빠름!)
```

| 방식 | 빠른 경우 (50ms에 완료) | 느린 경우 (1.5초에 완료) |
|------|----------------------|----------------------|
| `Thread.sleep(2000)` | 2000ms 대기 | 2000ms 대기 |
| `await.atMost(2초)` | **~60ms** 대기 | **~1500ms** 대기 |

**3. lateinit var**

"나중에 초기화할게"라는 의미입니다:

```kotlin
lateinit var tempDir: Path  // 선언만, 값 없음
```

**왜 필요한가?**

```kotlin
// 일반 var는 초기값 필수
var tempDir: Path = ???  // 뭘 넣어야 할지 모름!

// null로 초기화? → 매번 null 체크 필요
var tempDir: Path? = null
tempDir!!.resolve("test.jsonl")  // !! 필요

// lateinit 사용 → null 체크 없이 사용 가능
lateinit var tempDir: Path
tempDir.resolve("test.jsonl")  // OK (JUnit이 주입해줌)
```

**lateinit의 특징:**

| 특징 | 설명 |
|------|------|
| `var`만 가능 | `val`은 불가 |
| non-null 타입만 | `Path?`는 불가 |
| primitive 불가 | `Int`, `Boolean` 등 불가 |
| 초기화 안 하고 접근 시 | `UninitializedPropertyAccessException` |

**@TempDir과 함께 사용하는 이유:**
```kotlin
@TempDir  // JUnit이 테스트 실행 전에 값을 주입해줌
lateinit var tempDir: Path  // 그래서 lateinit으로 선언
```

---

### 질문 4-3: 테스트 메서드 네이밍

**Q:**
```kotlin
@Test
fun `should pass correct userAgent to AccessLogger`() { ... }

@Test
fun `should not log for excluded paths`() { ... }

@Test
fun `single wildcard should not match nested paths`() { ... }
```

1. 테스트 메서드 이름에 백틱(`)을 사용한 이유는 무엇인가요?
2. 테스트 이름이 `should ...` 패턴으로 작성된 이유는 무엇인가요?

**A:**
1. 잘 모르겠어요.
2. ~해야 한다의 의미로 테스트가 이렇게 되어야만 한다를 표시하기 위해서야.

**피드백:**

**1. 백틱(`) 사용 이유**

Kotlin에서 백틱은 "공백과 특수문자를 함수명에 포함"할 수 있게 해줍니다:

```kotlin
// Java 스타일 (공백 불가)
@Test
fun shouldPassCorrectUserAgentToAccessLogger() { ... }

// Kotlin 백틱 스타일 (공백 가능)
@Test
fun `should pass correct userAgent to AccessLogger`() { ... }
```

**왜 백틱을 쓰나?**

| 스타일 | 예시 | 가독성 |
|--------|------|--------|
| camelCase | `shouldNotLogForExcludedPaths` | 읽기 어려움 |
| snake_case | `should_not_log_for_excluded_paths` | 중간 |
| **백틱** | `should not log for excluded paths` | **자연어처럼 읽힘** |

**테스트 실패 시 리포트:**
```
❌ shouldNotLogForExcludedPathsWithDoubleWildcard  // 뭔 소리지?
❌ should not log for excluded paths with double wildcard  // 이해하기 쉬움!
```

**주의:** 백틱 함수명은 **테스트 코드에서만** 사용하는 것이 관례입니다. 프로덕션 코드에서는 사용하지 않아요.

**2. should ... 패턴 - ✅ 정확합니다!**

BDD(Behavior-Driven Development) 스타일입니다:

```kotlin
// "~해야 한다" = 기대 동작을 명시
fun `should pass correct userAgent to AccessLogger`()
//   ~~~~~~ ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//   해야 한다   무엇을

fun `should not log for excluded paths`()
//   ~~~~~~~~~~ ~~~~~~~~~~~~~~~~~~~~~~~
//   하지 않아야 한다   언제
```

**테스트 이름 패턴들:**

| 패턴 | 예시 | 특징 |
|------|------|------|
| `should ...` | `should return empty list` | 기대 결과 중심 |
| `given ... when ... then` | `given valid input when save then return id` | 상황-행동-결과 |
| `test ...` | `testUserAgentParsing` | 단순하지만 덜 명확 |

**should 패턴의 장점:**
- 테스트가 **문서 역할**을 함
- "이 컴포넌트는 ~해야 한다"가 명확
- 실패 시 **무엇이 깨졌는지** 바로 알 수 있음

```
✅ should pass correct userAgent to AccessLogger
✅ should measure duration correctly
❌ should clear LogContextHolder after request  ← 이게 깨졌구나!
```

---

### 질문 4-4: AppenderPerformanceTest

**Q:**
```kotlin
@Disabled("Performance test - run manually")
class AppenderPerformanceTest {

    @Test
    fun `async appender multi producer throughput - 10 threads x 10,000 logs`() {
        // ...
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        val elapsed = measureTimeMillis {
            repeat(threadCount) { threadIdx ->
                executor.submit {
                    try {
                        repeat(logsPerThread) { i ->
                            appender.append(createTestLog(threadIdx * logsPerThread + i))
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            appender.shutdown()
        }
        // ...
    }
}
```

1. `@Disabled("Performance test - run manually")`를 사용한 이유는 무엇인가요?
2. `CountDownLatch`는 무엇이고, 왜 사용했나요? `latch.countDown()`과 `latch.await()`의 역할은?
3. `measureTimeMillis { }`는 무엇인가요?

**A:**
1. performance test는 전체 테스트를 수행할 때 명시적으로 빼기 위해서 사용했어요.
2. 잘 모르겠어요.
3. 몇 ms가 걸린건지 측정하기 위해서 사용했어요.

**피드백:**

**1. @Disabled 사용 이유 - ✅ 정확합니다!**

```kotlin
@Disabled("Performance test - run manually")
class AppenderPerformanceTest
```

**왜 성능 테스트를 제외하나?**

| 일반 단위 테스트 | 성능 테스트 |
|----------------|-----------|
| 빠름 (ms 단위) | 느림 (초~분 단위) |
| CI/CD에서 항상 실행 | 필요할 때만 실행 |
| 결과: Pass/Fail | 결과: 수치 분석 필요 |

```bash
# 일반 빌드 - 성능 테스트 제외
./gradlew test  # @Disabled 테스트는 스킵

# 성능 테스트만 수동 실행
./gradlew test --tests "*.AppenderPerformanceTest"
```

**2. CountDownLatch - 설명**

**CountDownLatch = "모든 스레드가 끝날 때까지 기다리는 장치"**

```kotlin
val latch = CountDownLatch(10)  // 카운터 = 10
```

**동작 원리:**
```
초기 상태: latch = 10

Thread 1 완료 → latch.countDown() → latch = 9
Thread 2 완료 → latch.countDown() → latch = 8
...
Thread 10 완료 → latch.countDown() → latch = 0

Main Thread: latch.await()
→ latch가 0이 될 때까지 블로킹
→ 0이 되면 진행
```

**코드 흐름:**
```kotlin
val latch = CountDownLatch(threadCount)  // 10개 스레드 대기

repeat(threadCount) { threadIdx ->
    executor.submit {
        try {
            // 로그 10,000개 전송
            repeat(logsPerThread) { i ->
                appender.append(createTestLog(...))
            }
        } finally {
            latch.countDown()  // "나 끝났어!" (카운터 -1)
        }
    }
}

latch.await()  // 모든 스레드가 countDown() 할 때까지 대기
appender.shutdown()  // 모두 끝난 후 shutdown
```

**왜 CountDownLatch가 필요한가?**
```kotlin
// CountDownLatch 없이
repeat(threadCount) { executor.submit { ... } }
appender.shutdown()  // 💥 스레드들이 아직 실행 중인데 shutdown!

// CountDownLatch 사용
repeat(threadCount) { executor.submit { ... } }
latch.await()  // 모든 스레드 완료 대기
appender.shutdown()  // ✅ 안전하게 shutdown
```

**비유:**
```
레스토랑에서 10명이 주문함
→ 각자 식사 완료하면 "다 먹었어요" (countDown)
→ 매니저는 10명 모두 "다 먹었어요" 할 때까지 대기 (await)
→ 모두 완료 후 정산
```

**3. measureTimeMillis - ✅ 정확합니다!**

```kotlin
val elapsed = measureTimeMillis {
    // 측정할 코드
}
println("걸린 시간: ${elapsed}ms")
```

**내부 구현 (간략화):**
```kotlin
inline fun measureTimeMillis(block: () -> Unit): Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}
```

**비슷한 함수들:**
```kotlin
measureTimeMillis { }  // Long (ms)
measureNanoTime { }    // Long (ns, 더 정밀)
measureTime { }        // Duration 객체 (Kotlin 1.9+)
```

---

### 질문 4-5: AccessLogIntegrationTest

**Q:**
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension::class)
class AccessLogIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `should output log to stdout on GET health request`(output: CapturedOutput) {
        restTemplate.getForEntity("/health", String::class.java)

        val logLine = findLogLine(output, "/health")
        assertThat(logLine).isNotNull()
        // ...
    }

    private fun findLogLine(output: CapturedOutput, path: String): String? {
        return output.toString().lines()
            .find { it.contains("\"type\":\"log.v1.http\"") && it.contains("\"http_path\":\"$path\"") }
    }
}
```

1. `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`에서 `RANDOM_PORT`를 사용한 이유는 무엇인가요?
2. `@ExtendWith(OutputCaptureExtension::class)`와 `CapturedOutput`은 무엇인가요?
3. `TestRestTemplate`은 무엇이고, 왜 일반 `RestTemplate` 대신 사용하나요?

**A:** 다 잘 모르겠어요.

**피드백:**

**1. RANDOM_PORT 사용 이유**

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

**WebEnvironment 옵션들:**

| 옵션 | 설명 | 서버 실행 |
|------|------|----------|
| `MOCK` (기본값) | 가짜 서블릿 환경 | ❌ |
| `RANDOM_PORT` | **랜덤 포트**로 실제 서버 실행 | ✅ |
| `DEFINED_PORT` | 설정된 포트(8080)로 실제 서버 실행 | ✅ |
| `NONE` | 웹 환경 없음 | ❌ |

**왜 RANDOM_PORT인가?**

```kotlin
// DEFINED_PORT 사용 시 문제
// 테스트 A: 8080 포트 사용
// 테스트 B: 8080 포트 사용 → 💥 "Port already in use"

// RANDOM_PORT 사용 시
// 테스트 A: 52341 포트 (랜덤)
// 테스트 B: 58723 포트 (랜덤) → ✅ 충돌 없음
```

**장점:**
- 테스트 **병렬 실행** 가능
- 다른 프로세스와 **포트 충돌 방지**
- CI/CD 환경에서 안전

**2. OutputCaptureExtension과 CapturedOutput**

```kotlin
@ExtendWith(OutputCaptureExtension::class)
class AccessLogIntegrationTest {

    @Test
    fun `should output log to stdout`(output: CapturedOutput) {
        // output에 stdout 내용이 캡처됨
    }
}
```

**역할: stdout/stderr를 캡처해서 검증**

```kotlin
// 콘솔에 출력되는 내용:
// {"type":"log.v1.http","http_path":"/health",...}

@Test
fun `should output log to stdout`(output: CapturedOutput) {
    restTemplate.getForEntity("/health", String::class.java)

    // output.toString() → 콘솔에 출력된 모든 내용
    val logLine = output.toString().lines()
        .find { it.contains("/health") }

    assertThat(logLine).isNotNull()  // 콘솔에 로그가 출력되었는지 검증
}
```

**왜 필요한가?**
- `ConsoleLogAppender`는 **stdout으로 출력**
- 출력된 내용을 **테스트에서 검증**하려면 캡처 필요
- `CapturedOutput` 파라미터를 선언하면 JUnit이 자동 주입

**동작 흐름:**
```
1. @ExtendWith(OutputCaptureExtension::class) → stdout 캡처 시작
2. 테스트 메서드에 CapturedOutput 파라미터 → 캡처된 내용 주입
3. restTemplate.getForEntity() → 로그가 stdout에 출력됨
4. output.toString() → 캡처된 stdout 내용 확인
```

**3. TestRestTemplate vs RestTemplate**

```kotlin
@Autowired
lateinit var restTemplate: TestRestTemplate
```

**TestRestTemplate = 테스트 전용 RestTemplate**

| 기능 | RestTemplate | TestRestTemplate |
|------|-------------|------------------|
| 상대 경로 | ❌ `http://localhost:8080/health` | ✅ `/health` |
| 랜덤 포트 자동 처리 | ❌ 포트 번호 직접 지정 | ✅ 자동으로 연결 |
| 에러 응답 | 예외 발생 | **예외 없이** ResponseEntity 반환 |
| 용도 | 프로덕션 | **테스트 전용** |

**TestRestTemplate의 장점:**

```kotlin
// RestTemplate 사용 시 (번거로움)
@LocalServerPort
var port: Int = 0

val restTemplate = RestTemplate()
restTemplate.getForEntity("http://localhost:$port/health", String::class.java)

// TestRestTemplate 사용 시 (간편)
@Autowired
lateinit var restTemplate: TestRestTemplate

restTemplate.getForEntity("/health", String::class.java)  // 상대 경로만!
```

**에러 응답 처리:**
```kotlin
// RestTemplate - 4xx, 5xx 응답 시 예외 발생
restTemplate.getForEntity("/not-found", String::class.java)
// → HttpClientErrorException 발생!

// TestRestTemplate - 예외 없이 응답 반환
val response = testRestTemplate.getForEntity("/not-found", String::class.java)
assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)  // 검증 가능
```

---

### 질문 4-6: 추가 테스트 코드 질문들

**Q:** 여러 가지 추가 질문들

**A:** (설명 요청)

**피드백:**

**1. Executors.newFixedThreadPool(threadCount)**

```kotlin
val executor = Executors.newFixedThreadPool(threadCount)
```

ThreadPool = 스레드를 미리 만들어두고 재사용하는 풀

```kotlin
// ThreadPool 없이 (비효율)
repeat(100) {
    Thread { doWork() }.start()  // 매번 스레드 생성 → 비용 큼
}

// ThreadPool 사용 (효율)
val executor = Executors.newFixedThreadPool(10)  // 10개 스레드 미리 생성
repeat(100) {
    executor.submit { doWork() }  // 기존 스레드 재사용
}
```

**2. executor.submit { }**

```kotlin
executor.submit {
    // 이 코드가 ThreadPool의 스레드에서 실행됨
    appender.append(createTestLog())
}
```

submit = "이 작업을 스레드풀에 제출해줘"

| 메서드 | 반환값 | 용도 |
|--------|--------|------|
| `submit { }` | `Future<?>` | 결과/완료 여부 확인 가능 |
| `execute { }` | 없음 | 단순 실행 |

**3. restTemplate.getForEntity() / postForEntity()**

```kotlin
restTemplate.getForEntity("/health", String::class.java)
restTemplate.postForEntity("/api/orders", null, String::class.java)
```

HTTP 요청을 보내는 메서드:

| 메서드 | HTTP 메서드 | 용도 |
|--------|-----------|------|
| `getForEntity(url, 응답타입)` | GET | 조회 |
| `postForEntity(url, body, 응답타입)` | POST | 생성 |
| `put(url, body)` | PUT | 수정 |
| `delete(url)` | DELETE | 삭제 |

**4. logCount.coerceAtMost()**

```kotlin
bufferSize = logCount.coerceAtMost(500_000)
```

coerceAtMost(max) = "최대 max까지만"

```kotlin
100.coerceAtMost(50)      // → 50 (100 > 50이므로 50으로 제한)
30.coerceAtMost(50)       // → 30 (30 < 50이므로 그대로)
1_000_000.coerceAtMost(500_000)  // → 500_000
```

왜 사용했나?
- `logCount`가 2,000,000일 때 bufferSize도 2,000,000이면 **메모리 부족** 위험
- 최대 500,000으로 **제한**하여 OOM 방지

**5. Runtime.getRuntime()**

```kotlin
val runtime = Runtime.getRuntime()
```

Runtime = JVM 런타임 환경에 대한 정보/제어

```kotlin
runtime.totalMemory()  // JVM에 할당된 총 메모리
runtime.freeMemory()   // 사용 가능한 메모리
runtime.maxMemory()    // JVM이 사용할 수 있는 최대 메모리
```

**6. totalMemory() / freeMemory()**

```kotlin
val heapBefore = runtime.totalMemory() - runtime.freeMemory()
// ... 작업 ...
val heapAfter = runtime.totalMemory() - runtime.freeMemory()
val heapDelta = heapAfter - heapBefore  // 사용된 메모리 증가량
```

| 메서드 | 의미 |
|--------|------|
| `totalMemory()` | 현재 JVM에 **할당된** 메모리 |
| `freeMemory()` | 할당된 메모리 중 **사용 가능한** 양 |
| `totalMemory() - freeMemory()` | **실제 사용 중**인 메모리 |

**7. find 메서드 사용 이유**

```kotlin
output.toString().lines()
    .find { it.contains("\"http_path\":\"/health\"") }
```

find = 조건을 만족하는 첫 번째 요소 반환

```kotlin
listOf(1, 2, 3, 4, 5).find { it > 3 }  // → 4
listOf(1, 2, 3).find { it > 10 }       // → null
```

왜 find를 썼나?
- stdout에 여러 줄이 출력됨
- 그 중 **특정 경로를 포함한 로그 한 줄**만 찾고 싶음

**8. objectMapper.readTree()**

```kotlin
val parsed = objectMapper.readTree(logLine)
assertThat(parsed.get("http_method").asText()).isEqualTo("GET")
```

readTree = JSON 문자열을 JsonNode 트리로 파싱

```kotlin
val json = """{"name": "홍길동", "age": 30}"""
val node = objectMapper.readTree(json)

node.get("name").asText()  // → "홍길동"
node.get("age").asInt()    // → 30
```

vs readValue:
```kotlin
// readValue - 특정 클래스로 변환 (타입 필요)
val user = objectMapper.readValue(json, User::class.java)

// readTree - JsonNode로 변환 (타입 불필요, 동적 접근)
val node = objectMapper.readTree(json)
```

**9. TIMEOUT = Duration.ofSeconds(5)**

왜 5초?
- 비동기 작업이 완료될 때까지 **충분한 시간**
- 너무 짧으면: 느린 환경에서 **flaky test**
- 너무 길면: 실패 시 **테스트가 오래 걸림**
- **5초는 일반적인 관례**

**10. @Autowired와 필드 주입**

```kotlin
@Autowired
lateinit var restTemplate: TestRestTemplate
```

**필드 주입이 안티패턴인 이유:**
```kotlin
// 필드 주입 (안티패턴)
@Autowired
lateinit var service: MyService
// 문제: 테스트에서 mock 주입 어려움, 의존성이 숨겨짐

// 생성자 주입 (권장)
class MyController(
    private val service: MyService  // 의존성이 명확히 드러남
)
```

**그런데 테스트에서는 괜찮은 이유:**
- 테스트 클래스는 **Spring이 생성**함
- 생성자 주입 사용하려면 복잡한 설정 필요
- 테스트에서는 **편의성 > 원칙**
- 공식 문서에서도 테스트에서는 필드 주입 허용

**11. WebApplicationContextRunner**

```kotlin
private val contextRunner = WebApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(PumpkinLogAutoConfiguration::class.java))
```

WebApplicationContextRunner = AutoConfiguration 테스트용 도구

왜 사용하나?
- `@SpringBootTest` 없이 **가볍게** AutoConfiguration 테스트
- 다양한 **조건**을 쉽게 테스트 가능

```kotlin
// 기본 상태 테스트
contextRunner.run { context ->
    assertThat(context).hasSingleBean(ConsoleLogAppender::class.java)
}

// 특정 프로퍼티로 테스트
contextRunner
    .withPropertyValues("pumpkin.log.console.enabled=false")
    .run { context ->
        assertThat(context).doesNotHaveBean(ConsoleLogAppender::class.java)
    }
```

vs @SpringBootTest:
| | WebApplicationContextRunner | @SpringBootTest |
|--|----------------------------|-----------------|
| 속도 | **빠름** | 느림 |
| 용도 | AutoConfiguration 단위 테스트 | 통합 테스트 |
| 조건 변경 | **쉬움** | 어려움 |

---

## Part 5: 설계/아키텍처 질문

### 질문 5-1: 모듈 구조

**Q:**
```
pumpkin-log/
├── pumpkin-log-core/           # 순수 Kotlin
├── pumpkin-log-spring-mvc/     # Spring WebMVC 통합
└── demo-server-mvc/            # 데모 서버
```

1. 왜 core 모듈을 Spring 의존성 없이 순수 Kotlin으로 분리했나요?
2. 만약 core에 Spring 의존성이 있었다면 어떤 문제가 있었을까요?

**A:**
1. 너무 spring에 종속적이게 되어버려요. 순수 core 모듈로 만들어야 다른 모듈에도 붙여서 webflux나 gRPC 같은 곳에도 붙여서 확장성있게 사용할 수 있어요.
2. core가 spring 의존성이 있었다면 spring을 사용안하는 모듈과 붙였을 때 문제가 발생할 것 같아요.

**피드백:** ✅ 정확합니다!

**1. 순수 Kotlin으로 분리한 이유**

```
pumpkin-log-core (순수 Kotlin)
    ↑              ↑              ↑
Spring MVC     WebFlux        gRPC
모듈           모듈           모듈 (미래)
```

- **프레임워크 독립적**: 어떤 프레임워크든 붙일 수 있음
- **확장성**: 새로운 프레임워크 지원 시 core 수정 없이 통합 모듈만 추가
- **테스트 용이**: Spring Context 없이 단위 테스트 가능

**2. core에 Spring 의존성이 있었다면?**

| 문제 | 설명 |
|------|------|
| **불필요한 의존성** | gRPC 서버에서 Spring 전체를 끌어와야 함 |
| **버전 충돌** | 사용자 프로젝트의 Spring 버전과 충돌 가능 |
| **무거운 JAR** | Spring 의존성으로 라이브러리 크기 증가 |
| **순수 Kotlin 환경 불가** | Spring 없는 프로젝트에서 사용 불가 |

**좋은 라이브러리 설계 원칙:**
> "Core는 최소한의 의존성으로, 통합 모듈에서 프레임워크별 어댑터 제공"

---

### 추가 질문: 왜 Awaitility를 선택했나요?

**Q:** 비동기 테스트에서 `await().atMost(Duration.ofSeconds(2)).untilAsserted {}`를 사용하셨는데, 왜 Awaitility를 선택하셨나요? 다른 방법도 있을 텐데요.

**A:** (인터뷰어 설명)

**대안들과 비교:**

**1. Thread.sleep() - 가장 단순한 방법**
```kotlin
// 안티패턴
Thread.sleep(2000)
assertThat(appender.writtenCount).isEqualTo(100)
```
문제점:
- 항상 고정 시간 대기 → 테스트 느려짐
- 환경에 따라 2초가 부족할 수도, 과할 수도 있음
- 실패 시 원인 파악 어려움

**2. CountDownLatch - 직접 제어**
```kotlin
val latch = CountDownLatch(1)
// 작업 완료 시 latch.countDown() 호출 필요
latch.await(5, TimeUnit.SECONDS)
```
문제점:
- 프로덕션 코드에 테스트용 코드 침투
- `countDown()` 호출 위치 관리 필요

**3. Awaitility - 폴링 기반 검증**
```kotlin
await().atMost(Duration.ofSeconds(2)).untilAsserted {
    assertThat(appender.writtenCount).isEqualTo(100)
}
```
장점:
- 프로덕션 코드 수정 불필요
- 조건 만족 시 즉시 통과 (빠름)
- 타임아웃까지 자동 재시도
- 실패 시 마지막 상태 출력 (디버깅 용이)
- DSL이 직관적

**4. 코루틴 기반 (kotlinx-coroutines-test)**
```kotlin
runTest {
    advanceUntilIdle()
}
```
한계: 이 프로젝트는 코루틴을 사용하지 않음. Thread 기반 비동기라서 적합하지 않음.

**Awaitility 선택 이유:**
AsyncFileLogAppender는 별도 Worker Thread가 큐를 처리하는 구조입니다. 테스트에서 "언제 Worker가 처리를 완료할지" 정확히 알 수 없으므로, **폴링 기반으로 조건 만족 여부를 반복 확인**하는 Awaitility가 가장 적합합니다.

---

### 질문 5-2: 동기 vs 비동기 Appender

**Q:**
1. 어떤 상황에서 동기(FileLogAppender)를 선택하고, 어떤 상황에서 비동기(AsyncFileLogAppender)를 선택해야 하나요?
2. 비동기 Appender의 단점/한계점은 무엇인가요?

**A:** 동기는 느리지만 log 하나라도 놓치고 싶지 않을 때, 비동기는 성능이 더 우선일 때

**피드백:** ✅ 핵심을 잘 짚으셨습니다!

**1. 동기 vs 비동기 선택 기준 (상세)**

| 기준 | 동기 (FileLogAppender) | 비동기 (AsyncFileLogAppender) |
|------|----------------------|------------------------------|
| **트래픽** | 저트래픽 (수십~수백 RPS) | 고트래픽 (수천+ RPS) |
| **로그 중요도** | 감사 로그, 결제 로그 등 **유실 불가** | 일반 액세스 로그 |
| **응답 지연** | 파일 I/O만큼 지연 발생 | 거의 없음 (큐에 넣고 즉시 반환) |
| **적합한 환경** | 배치 서버, 내부 툴 | 실시간 API 서버 |

**2. 비동기 Appender 단점/한계점**

말씀하신 **로그 유실 가능성**이 가장 큰 단점:

```
1. 큐가 가득 차면 → 새 로그 버림 (droppedCount 증가)
2. JVM 강제 종료 시 → 큐에 남은 로그 유실
3. Worker Thread 에러 시 → 배치 단위 유실
```

추가 단점:
- **메모리 사용량 증가**: 큐 버퍼 크기만큼 메모리 점유
- **복잡도 증가**: shutdown 처리, 모니터링(droppedCount) 필요
- **디버깅 어려움**: 로그가 요청 완료 후 비동기로 기록됨

---

### 질문 5-3: LogContextHolder 설계

**Q:**
```kotlin
object LogContextHolder {
    private val contextHolder = ThreadLocal<MutableMap<String, Any?>>()
    // ...
}
```

1. 왜 `ThreadLocal`을 사용했나요?
2. WebFlux(리액티브) 환경에서 이 설계가 동작할까요? 동작하지 않는다면 어떻게 해결해야 할까요?

**A:** ThreadLocal은 스레드별 데이터 저장용. WebFlux는 안될 것 같다. Reactor Context를 사용해야 하는 것으로 알고 있지만 왜인지는 모르겠다.

**피드백:** ✅ 핵심을 정확히 짚으셨습니다!

**1. ThreadLocal을 사용한 이유**

Spring MVC의 스레드 모델:
```
요청 1 → Thread-1 → Controller → Service → Repository → 응답
요청 2 → Thread-2 → Controller → Service → Repository → 응답
```
- **1 요청 = 1 스레드**가 끝까지 처리
- ThreadLocal에 저장하면 같은 요청 내에서 어디서든 접근 가능

**2. WebFlux에서 안 되는 이유**

WebFlux(리액티브)의 스레드 모델:
```
요청 1 시작 → Thread-1
    ↓ (DB 호출, 대기)
요청 1 재개 → Thread-3  ← 다른 스레드!
    ↓ (외부 API 호출, 대기)
요청 1 완료 → Thread-2  ← 또 다른 스레드!
```
- **1 요청이 여러 스레드를 넘나듦**
- ThreadLocal 데이터가 중간에 사라짐

**해결책: Reactor Context**
```kotlin
// WebFlux에서의 컨텍스트 전달
Mono.just("data")
    .contextWrite(Context.of("userId", "user-123"))
    .flatMap { data ->
        Mono.deferContextual { ctx ->
            val userId = ctx.get<String>("userId")
            // ...
        }
    }
```
- Reactor의 **구독 체인**을 따라 컨텍스트가 전파됨
- 스레드가 바뀌어도 유지됨

**3. gRPC에서의 해결책 (추가 설명)**

gRPC Context:
```kotlin
// gRPC 인터셉터에서 컨텍스트 설정
val USER_ID_KEY = Context.key<String>("userId")

class LoggingInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val context = Context.current()
            .withValue(USER_ID_KEY, headers.get(USER_ID_HEADER))

        return Contexts.interceptCall(context, call, headers, next)
    }
}

// 서비스에서 사용
val userId = USER_ID_KEY.get()  // 현재 gRPC Context에서 가져옴
```

**프레임워크별 컨텍스트 전달 방식:**

| 프레임워크 | 스레드 모델 | 컨텍스트 전달 방식 |
|-----------|-----------|------------------|
| **Spring MVC** | 1요청 = 1스레드 | `ThreadLocal` |
| **Spring WebFlux** | 1요청 = N스레드 | `Reactor Context` |
| **gRPC** | 1요청 = N스레드 가능 | `gRPC Context` |
| **코루틴** | 1요청 = N스레드 가능 | `CoroutineContext` |

---

### 질문 5-4: 확장성과 개선점

**Q:**
1. 이 SDK를 실제 프로덕션에 배포한다면, 어떤 기능을 추가하고 싶으신가요?
2. 현재 구현에서 개선하고 싶은 부분이 있다면 무엇인가요?

**A:** 로그 샘플링이나 로그 레벨 기능 추가하고 싶어요.

**피드백:** ✅ 실제 프로덕션에서 필요한 핵심 기능들입니다!

**말씀하신 기능들:**

1. **로그 샘플링**
```yaml
pumpkin:
  log:
    sampling:
      rate: 0.1  # 10%만 로깅
```
- 고트래픽 환경에서 로그 볼륨 감소
- 비용 절감 (로그 저장/전송 비용)

2. **로그 레벨 (상태코드 기반 필터링)**
```yaml
pumpkin:
  log:
    file:
      min-status-code: 400  # 4xx, 5xx만 파일에 기록
```
- 중요한 로그만 선별 저장

**추가로 고려할 만한 기능들:**

| 기능 | 설명 |
|------|------|
| **로그 로테이션** | 파일 크기/날짜 기준 자동 분할 |
| **민감정보 마스킹** | 비밀번호, 카드번호 등 자동 마스킹 |
| **Request/Response Body** | 요청/응답 본문 로깅 옵션 |
| **Trace ID 연동** | Spring Cloud Sleuth, OpenTelemetry 연동 |
| **원격 전송** | ELK, Datadog, CloudWatch로 직접 전송 |
| **메트릭 연동** | Micrometer로 로그 통계 노출 |

**현재 구현의 개선점 예시:**

```kotlin
// 1. 로그 로테이션 (현재 미지원)
class RotatingFileLogAppender(
    maxFileSize: Long = 100_MB,
    maxFiles: Int = 10
)

// 2. Exponential Backoff (현재 고정 100ms)
private fun handleWriteError(e: Exception, retryCount: Int) {
    val delay = minOf(100L * (1 shl retryCount), 10000L)
    Thread.sleep(delay)
}

// 3. Health Check 엔드포인트
fun isHealthy(): Boolean =
    running.get() && worker.isAlive && droppedCount < threshold
```

---

## 면접 종합 피드백

### 잘한 점
1. **모듈 분리 설계**: core를 순수 Kotlin으로 분리하여 프레임워크 독립성 확보
2. **비동기 처리 이해**: AsyncFileLogAppender의 트레이드오프(성능 vs 유실 가능성) 명확히 인지
3. **ThreadLocal 한계 인식**: WebFlux에서 동작하지 않는 이유와 대안(Reactor Context) 파악
4. **실용적 사고**: 로그 샘플링, 레벨 필터링 등 실제 운영에 필요한 기능 제시

### 보완하면 좋을 점
1. **Gradle 세부 설정**: `apply false`, `-Xjsr305=strict` 등 빌드 설정의 의미 학습
2. **동시성 프리미티브**: `compareAndSet`, `AtomicLong` 등의 사용 이유 더 깊이 이해
3. **테스트 라이브러리**: Awaitility, MockK의 내부 동작 원리 학습

### 면접 팁
- "모르겠습니다"라고 솔직하게 답한 것은 좋습니다. 다만, 추측이라도 해보면 더 좋아요.
- 코드 한 줄 한 줄의 의도를 설명할 수 있도록 연습하세요.
- 트레이드오프를 항상 생각하세요. (예: 동기 vs 비동기, 성능 vs 안정성)

---

**수고하셨습니다!** 🎉

---

## 추가 Q&A

### Q: libs.versions.toml을 실제로 배포해서 library를 사용한다고 해도 사용하나요? 다른 오픈소스에서도 많이 사용하는 방법인가요?

**A:**

## Version Catalog는 **빌드 시점** 도구

```
┌─────────────────────────────────────────────────────────┐
│  SDK 개발자 (당신)                                        │
│                                                         │
│  libs.versions.toml → build.gradle.kts → JAR 빌드       │
│        ↓                                                │
│  (빌드할 때만 사용, JAR에 포함 안 됨)                      │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼ Maven Central / JitPack 배포
┌─────────────────────────────────────────────────────────┐
│  SDK 사용자                                              │
│                                                         │
│  implementation("com.pumpkin:pumpkin-log:1.0.0")        │
│        ↓                                                │
│  POM 파일에서 의존성 정보 읽음 (Version Catalog 아님)      │
└─────────────────────────────────────────────────────────┘
```

### 배포 시 전달되는 것

| 파일 | 포함 여부 | 역할 |
|------|----------|------|
| `libs.versions.toml` | ❌ 포함 안 됨 | 개발용 빌드 설정 |
| `pom.xml` (자동 생성) | ✅ 포함됨 | 의존성 정보 전달 |
| JAR 파일 | ✅ 포함됨 | 실제 코드 |

### 사용자가 보는 것

```xml
<!-- Maven Central에 배포된 pom.xml -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.0</version>
    <scope>compile</scope>
</dependency>
```

사용자는 **POM 파일**을 통해 의존성을 파악합니다. Version Catalog는 전혀 모릅니다.

---

## 오픈소스에서의 사용 현황

### Version Catalog 사용하는 프로젝트
- **Android 공식 라이브러리** (Jetpack)
- **Kotlin 공식 프로젝트**
- **최신 Gradle 기반 프로젝트**

### 아직 사용 안 하는 프로젝트
- **Spring Framework** - `gradle.properties` + ext 변수 사용
- **많은 레거시 프로젝트** - buildSrc, ext 변수 방식

### 왜 아직 보편적이지 않나?
```
Version Catalog 도입: Gradle 7.0 (2021년)
안정화: Gradle 7.4.2 (2022년)
```
- 비교적 **최신 기능** (3년 정도)
- 기존 프로젝트는 마이그레이션 비용 때문에 유지

---

## 결론

| 질문 | 답변 |
|------|------|
| SDK 배포 시 Version Catalog 전달되나? | ❌ 아니요, POM 파일로 전달 |
| 오픈소스에서 많이 쓰나? | 🔄 점점 늘어나는 추세 (특히 Android/Kotlin 생태계) |
| 사용해야 하나? | ✅ 멀티모듈 프로젝트에서는 권장 |

**핵심:** Version Catalog는 **개발자 편의 도구**이지, 배포 아티팩트의 일부가 아닙니다.

---

### Q: onError, onDropped 콜백에서 왜 다른 클래스들을 사용 안하고 Unit을 사용한거야?

**A:**

## 대안 1: Java 함수형 인터페이스

```kotlin
// Java의 Consumer 사용
var onError: Consumer<Throwable>? = null

// 사용 시
appender.onError = Consumer { e ->
    println("Error: $e")
}
```

**단점:**
- Java 인터페이스라 Kotlin스럽지 않음
- `Consumer { }` 명시 필요 (SAM 변환)
- 2개 이상 파라미터면 `BiConsumer` 필요, 3개면? 없음

## 대안 2: 커스텀 인터페이스

```kotlin
// 인터페이스 정의
interface ErrorCallback {
    fun onError(appender: LogAppender, error: Throwable)
}

// 클래스에서
var onError: ErrorCallback? = null

// 사용 시
appender.onError = object : ErrorCallback {
    override fun onError(appender: LogAppender, error: Throwable) {
        println("Error: $error")
    }
}
```

**단점:**
- 인터페이스 파일 추가 필요
- 사용 시 `object : Interface` 보일러플레이트
- 콜백마다 인터페이스 정의해야 함

## 현재 방식: Kotlin 함수 타입

```kotlin
// 정의
var onError: ((LogAppender, Throwable) -> Unit)? = null

// 사용 시 - 람다로 간단하게
appender.onError = { appender, e ->
    println("Error in $appender: $e")
}
```

**장점:**
- 추가 인터페이스 불필요
- 람다로 간결하게 사용
- 파라미터 개수 자유로움
- Kotlin 관용적 표현

## `Unit`의 의미

```kotlin
(LogAppender, Throwable) -> Unit
//                          ↑
//                   "반환값 없음"
```

| 반환 타입 | 의미 | 사용 사례 |
|----------|------|----------|
| `Unit` | 반환값 없음 (알림만) | 로깅, 메트릭, 알림 |
| `Boolean` | 결과/결정 반환 | 재시도 여부, 필터링 |
| `T` | 변환된 값 반환 | 데이터 변환 |

```kotlin
// 만약 Boolean을 반환한다면?
var onError: ((Throwable) -> Boolean)? = null

appender.onError = { e ->
    shouldRetry(e)  // true면 재시도?
}
```

현재 구현에서는 **"에러 발생했어요"라고 알리기만** 하면 되므로 `Unit`이 적절합니다.

---

## 정리

| 방식 | 장점 | 단점 |
|------|------|------|
| `Consumer<T>` (Java) | Java 호환 | Kotlin스럽지 않음, 파라미터 제한 |
| 커스텀 인터페이스 | 명시적 | 보일러플레이트 많음 |
| **`(T) -> Unit` (Kotlin)** | 간결, 유연 | Java에서 사용 시 약간 불편 |

**Kotlin 프로젝트에서는 함수 타입 `() -> Unit`이 표준적인 콜백 방식입니다.**

---

### Q: JAR, WAR의 차이는 뭐야?

**A:**

## JAR vs WAR

### 기본 개념

| 구분 | JAR | WAR |
|------|-----|-----|
| **풀네임** | Java ARchive | Web Application ARchive |
| **용도** | 라이브러리, 독립 실행 앱 | 웹 애플리케이션 |
| **실행 환경** | JVM에서 직접 실행 | 서블릿 컨테이너 필요 (Tomcat 등) |

### 구조 비교

```
// JAR 구조
my-app.jar
├── META-INF/
│   └── MANIFEST.MF
├── com/
│   └── example/
│       └── MyClass.class
└── application.properties

// WAR 구조
my-webapp.war
├── META-INF/
│   └── MANIFEST.MF
├── WEB-INF/
│   ├── web.xml          ← 웹 설정 (필수였음)
│   ├── classes/         ← 컴파일된 클래스
│   └── lib/             ← 의존성 JAR들
└── index.html           ← 정적 리소스
```

### 실행 방식

```bash
# JAR - 직접 실행
java -jar my-app.jar

# WAR - 서블릿 컨테이너에 배포
cp my-webapp.war /tomcat/webapps/
```

### Spring Boot에서의 변화

```
전통적 방식:
  WAR 빌드 → Tomcat 서버에 배포 → 실행

Spring Boot 방식:
  JAR 빌드 (내장 Tomcat 포함) → 직접 실행
```

```kotlin
// build.gradle.kts
plugins {
    id("org.springframework.boot")
}

// 기본: JAR 생성 (내장 Tomcat)
// war { enabled = true }  // WAR도 생성 가능
```

### 언제 뭘 쓰나?

| 상황 | 선택 |
|------|------|
| **Spring Boot 앱** | JAR (권장) |
| **라이브러리/SDK** | JAR |
| **레거시 Tomcat 환경** | WAR |
| **외부 WAS 필수 (기업 정책)** | WAR |

### 요약

```
JAR = "실행 가능한 ZIP" (라이브러리 또는 독립 앱)
WAR = "웹앱 전용 ZIP" (서블릿 컨테이너 필요)

현재 트렌드: Spring Boot + JAR (내장 서버)
```

**pumpkin-log 프로젝트:**
- `pumpkin-log-core`, `pumpkin-log-spring-mvc` → **JAR** (라이브러리)
- `demo-server-mvc` → **JAR** (Spring Boot 앱, 내장 Tomcat)

---

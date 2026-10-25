# Pebble Forge

[English](README.md) | [한국어](README.ko.md)

Pebble Forge는 Pebble 4 확장팩입니다. Pebble 템플릿을 그대로 유효하게 두고, 다른 템플릿 엔진이 더 잘하는 기능을 더합니다. 0.1 버전은 템플릿으로 정의하는 컴포넌트, 권한 기반 샌드박스, Spring Boot와 Spring Security 연동을 제공합니다.

이름은 임시 이름입니다. Pebble 포크가 아니라 확장 라이브러리입니다.

Maven 그룹과 Java 패키지는 `io.github.allbegray.pebbleforge`입니다. GitHub 계정 `allbegray`와 Maven Central의 `io.github.*` 네임스페이스에 맞췄습니다.

## 상태

- Java 21
- Pebble 4.1.2
- Spring Boot 4.0.6
- Spring Security 7.0.8
- 버전 0.1.0-SNAPSHOT, 아직 배포하지 않음

현재 스냅샷은 `./gradlew publishToMavenLocal`로 로컬 Maven 저장소에 설치할 수 있습니다.

테스트는 337개입니다. 코어, 계약 검사, 진단, Gradle 플러그인, Spring Security 헬퍼, Boot 자동 설정을 검증하고, 공식 Pebble 뷰 리졸버를 통과하는 MockMvc 요청도 확인합니다.

## 모듈

| 모듈 | 역할 |
| --- | --- |
| `pebble-forge-core` | 컴포넌트, 샌드박스, 언어 확장, `PebbleForge.engine()` 빌더 |
| `pebble-forge-contracts` | 빌드 시 모델 변수와 컴포넌트 props 검사 |
| `pebble-forge-diagnostics` | 템플릿 오류의 소스 프레임과 include/extends 추적 |
| `pebble-forge-gradle-plugin` | `checkPebbleTemplates`와 `renderPebbleTemplate` Gradle 태스크 |
| `pebble-forge-spring` | Spring Security 템플릿 함수와 샌드박스 허용 목록 헬퍼 |
| `pebble-forge-spring-boot-4-starter` | Spring Boot 4 자동 설정, 프로퍼티, 서블릿 뷰 연동 |
| `pebble-forge-demo` | 컴포넌트와 샌드박스를 실행하는 데모 |

## 코어

```kotlin
implementation("io.github.allbegray.pebbleforge:pebble-forge-core:0.1.0-SNAPSHOT")
```

```java
ClasspathLoader loader = new ClasspathLoader();
loader.setPrefix("templates/");
loader.setSuffix(".peb");

PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .components(Components.builder()
                .root("components")
                .suffix("")
                .maxDepth(16)
                .build())
        .sandbox(SandboxPolicy.builder()
                .allowComponents()
                .allowTags("extends", "block")
                .maxRenderedChars(200_000)
                .build())
        .build();
```

`PebbleForge`는 엄격한 변수 검사를 기본으로 켭니다. 일반 Pebble은 이 검사를 끈 상태로 시작합니다. 나머지 Pebble 설정은 빌더가 그대로 전달하는 메서드로 넘길 수 있습니다.

### 컴포넌트

컴포넌트는 평범한 Pebble 템플릿입니다. `props`로 프롭스를 선언합니다. 필수 프롭스가 빠지면 컴포넌트 이름과 소스 줄 번호를 담은 오류가 납니다.

```twig
{# templates/components/card.peb #}
{% props title, subtitle = "" %}
<article class="card">
  <h2>{{ title }}</h2>
  {% if subtitle %}<p>{{ subtitle }}</p>{% endif %}
  <div class="body">{{ slot("body") }}</div>
</article>
```

```twig
{% component "card" with {"title": page.title, "subtitle": page.summary} %}
  {% slot "body" %}
    <p>{{ page.body }}</p>
  {% endslot %}
{% endcomponent %}
```

`slot` 태그를 쓰지 않은 본문은 기본 슬롯이 됩니다.

`Components.suffix`는 Forge가 붙입니다. 로더가 이미 `.peb`를 붙인다면 `Components.suffix("")`로 두세요. Spring Boot 스타터는 이렇게 설정합니다. 접미사가 두 번 붙는 문제가 가장 흔한 설정 실수입니다.

슬롯은 미리 렌더링하고 안전한 문자열로 표시합니다. `{{ slot("body") }}`처럼 직접 출력하세요. 슬롯을 문자열과 이어 붙이거나 필터에 넘기면 일반 문자열로 바뀌어 다시 이스케이프됩니다. 슬롯 함수는 실제로 캡처한 슬롯 값만 출력하므로, `with` 블록으로 슬롯 내용을 위조해서 이스케이프를 우회할 수 없습니다.

### 템플릿 언어 확장

Pebble Forge는 Pebble에 없는 태그를 추가합니다.

| 태그 | 동작 |
| --- | --- |
| `switch`, `case`, `default`, `endswitch` | 처음 일치하는 case 하나만 렌더합니다. case에 여러 값을 나열할 수 있습니다. fall-through는 없습니다. |
| `break`, `continue` | `for` 반복을 제어합니다. `break`는 `switch`도 빠져나갑니다. |
| `for key, value in ...` | Map 엔트리나 인덱스와 값을 구조 분해합니다. |
| `with { ... }` | 추가 변수를 가진 스코프에서 본문을 렌더합니다. |
| `with { ... } only` | 바깥 변수를 숨깁니다. 내부 슬롯 상태, locale, template은 유지합니다. |

```twig
{% with {"role": page.role} %}
  {% switch role %}
    {% case "admin", "owner" %}Admin
    {% case "user" %}User
    {% default %}Guest
  {% endswitch %}
{% endwith %}

{% for key, value in page.metrics %}
  {{ key }}: {{ value }}
{% endfor %}

{% with {"inner": page.value} only %}
  {{ inner }}
{% endwith %}
```

`for`는 break와 continue를 이해하는 버전으로 교체됩니다. `loop.index`, `loop.first`, `loop.last`, `loop.length`, `loop.revindex`, `else` 분기는 그대로 유지합니다. `for index, value`로 리스트를 구조 분해하면 인덱스는 0부터 시작합니다.

`with only`는 모델과 확장 스코프를 숨깁니다. 내부 슬롯 상태를 격리 스코프에 복사하므로 슬롯 내용은 계속 렌더됩니다.

`break`와 `continue`는 템플릿 소스 기준으로 판정합니다. 반드시 `for`나 `switch` 안에 있어야 합니다. 매크로 안에 있는 `break`를 루프 밖에서 호출하는 경우는 이 보장에 포함되지 않습니다.

템플릿을 순수 Pebble로 파싱해야 한다면 언어 확장을 끄면 됩니다.

```java
PebbleForge.engine().languageExtensions(false)
```

### 계약 검사

`pebble-forge-contracts`는 빌드 시점에 템플릿을 파싱해서 모델 변수와 컴포넌트 props를 검사합니다. 템플릿 내부 직접 읽기는 소스 순서에 따라 검사합니다. 정적 include와 extends 부모는 호출자 또는 자식 스코프, 리터럴 `with` 키, 블록 소유권으로 따라갑니다.

```kotlin
plugins {
    id("io.github.allbegray.pebbleforge.contracts")
}

pebbleForgeContracts {
    variables.add("page")
    variables.add("items")
    componentRoot.set("components")

    rootTemplates.add("pages/account")     // 루트로 강제
    partialTemplates.add("fragments/row")  // 루트 추론에서 제외
}
```

```shell
./gradlew checkPebbleTemplates
```

정적 include나 extends가 들어오지 않는 템플릿은 루트입니다. 직접 컴포넌트와 정적 컴포넌트 호출 대상은 루트입니다. 컴포넌트 루트 아래에서 include로만 쓰이는 중첩 템플릿은 partial입니다. partial은 자신을 호출하는 경로를 통해서만 검사합니다. extends로만 도달하는 부모는 루트가 아닙니다. 어떤 루트에서도 도달하지 않는 non-partial 템플릿은 fallback 루트로 검사합니다. `rootTemplates`는 루트이면서 partial인 템플릿을 강제로 검사합니다. `partialTemplates`는 동적 include로만 도달하는 조각을 루트 추론에서 뺍니다. 이름은 템플릿 루트 기준 상대 경로에서 접미사를 뺀 값입니다.

없는 모델 변수나 필수 컴포넌트 prop이 있으면 빌드가 실패합니다. 동적 include 대상, 동적 `with` 키, 알 수 없는 prop, 동적 컴포넌트 이름은 경고입니다. include, extends, 컴포넌트 순환은 오류입니다.

```text
page:4: ERROR: Variable [page] is not declared in the model schema
page:8: ERROR: Component [card] requires prop [title]
page:8: WARNING: Component [card] does not declare prop [footer]
```

기본 템플릿 루트는 `src/main/resources/templates`이고 기본 접미사는 `.peb`입니다. `allowAnyVariable`을 `true`로 두면 모델 변수 검사를 끄고 컴포넌트만 검사합니다.

Gradle 플러그인은 아직 Plugin Portal에 없습니다. `pebble-forge-contracts` 모듈을 Java에서 직접 사용할 수도 있습니다. 루트 하나는 `check(name)`, 템플릿 집합은 `checkAll(names)`를 사용합니다. 자세한 내용은 [docs/CONTRACTS.md](docs/CONTRACTS.md)를 참고하세요.

### 진단

`pebble-forge-diagnostics`는 Pebble 오류를 소스 프레임과 include/extends 경로로 바꿉니다.

```java
PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .build();

try {
    TemplateTrace.run(() -> engine.getTemplate("page").evaluate(writer, model));
} catch (PebbleException error) {
    System.err.println(TemplateDiagnostics.builder()
            .loader(loader)
            .build()
            .render(error)
            .format());
}
```

```text
Root attribute [missing] does not exist (layout.peb:2)
page.peb:1 extends layout.peb
  1 | {% extends "layout.peb" %}
    | ^
layout.peb:2
  1 | layout
> 2 | {{ missing }}
    | ^
```

Pebble Forge의 include와 extends 노드가 기본으로 체인을 기록합니다. 부모 본문에서 난 오류까지 extends 프레임을 얻으려면 `evaluate` 전체를 `TemplateTrace.run`으로 감싸세요. 자세한 내용은 [docs/DIAGNOSTICS.md](docs/DIAGNOSTICS.md)를 참고하세요.

Gradle 플러그인은 렌더 태스크도 제공합니다.

```kotlin
plugins {
    id("io.github.allbegray.pebbleforge.diagnostics")
}

pebbleForgeDiagnostics {
    templateName.set("page")
    model.put("page", mapOf("title" to "Account"))
}
```

```shell
./gradlew renderPebbleTemplate --template=page
```

### 개발 모드

개발 모드는 템플릿 소스가 바뀌면 같은 엔진에서 다시 읽습니다.

```java
FileLoader loader = new FileLoader("/srv/templates");
loader.setSuffix(".peb");

PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .devMode(true)
        .build();
```

로더가 템플릿 소스의 해시를 캐시 키에 넣습니다. 파일이 바뀌면 새 키가 생기고 다음 렌더에서 다시 파싱합니다. 이전 키는 엔진을 버릴 때까지 캐시에 남습니다. 개발 환경에서 쓰고, 오래 실행되는 프로덕션 프로세스에서는 끄세요.

jar 안의 classpath 리소스는 실행 중에 바뀌지 않습니다. 실시간 리로드가 필요하면 디렉터리를 가리키는 로더를 사용하세요.

Spring Boot에서는 같은 스위치를 씁니다.

```properties
pebble.forge.dev-mode=true
```

### 샌드박스

`SandboxPolicy.strict()`가 코어 샌드박스의 기본 정책입니다.

| 항목 | 기본값 |
| --- | --- |
| 태그 | `if`, `for`, `set`, `verbatim`, `include`, `component`, `slot`, `props`, `switch`, `break`, `continue`, `with` |
| 필터 | `escape`, `default`, `length`, `join`, `upper`, `lower`, `capitalize`, `title`, `trim`, `replace`, `slice`, `first`, `last`, `reverse`, `sort`, `split`, `abs`, `numberformat`, `date`, `abbreviate`, `urlencode` |
| 함수 | `slot`, `i18n`, `max`, `min` |
| 테스트 | `empty`, `null`, `iterable`, `even`, `odd`, `map` |
| 원본 출력 | 금지 |
| 출력 제한 | 1,000,000자 |
| 루프 제한 | 루프당 100,000회 |
| 전체 루프 제한 | 렌더당 1,000,000회 |
| include 깊이 | 중첩 include 16회 |
| 슬롯 캡처 | 컴포넌트당 1,000,000자 |
| 메서드 접근 | 위험한 클래스, 메서드, 반환 타입을 차단합니다. public 필드도 차단합니다. |
| 템플릿 경로 | 절대 경로, `..`, `.` 세그먼트, 역슬래시, NUL, 콜론, 공백, `{`, `}`, `%`, 빈 세그먼트를 거부합니다. |
| 심볼릭 링크 | `SandboxedLoader`는 문자열 기준입니다. `SandboxedFileLoader`는 실제 경로를 확인하고 루트 밖을 거부합니다. |

`range`는 기본 함수 목록에 없습니다. `..` 연산자는 렌더 시점에 `range`를 호출하므로 엄격 모드에서는 `..`도 막힙니다.

기능을 더하려면 `allowTags`, `allowFilters`, `allowFunctions`, `allowTests`, `allowGlobalVariables`, `allowComponents`, `allowRawOutput`을 사용하세요. `allowOnly*` 메서드는 기본 목록을 교체합니다.

샌드박스는 프로세스 내부의 권한 보호 장치이지 CPU 할당량이 아닙니다. 권한 악용과 출력 폭탄은 막지만, 큰 사용자 목록이 CPU를 오래 쓰는 것은 막지 못합니다. 루프 제한은 루프별이고, 전체 루프 예산은 렌더 전체가 공유합니다. 단 parallel 분기는 별도 예산을 받습니다. include 깊이는 기본 16회로 제한합니다. `SandboxedLoader`는 문자열 기준입니다. 심볼릭 링크가 가능한 환경에서는 `SandboxedFileLoader`를 사용하세요.

경로 제한은 문자열 기준입니다. 템플릿 이름의 상위 경로 이동은 막지만 심볼릭 링크는 해석하지 않습니다. 파일 로더 루트에 심볼릭 링크가 없도록 하거나, 실제 경로를 검사하는 로더를 사용하세요.

### 템플릿별 정책

템플릿 이름에 따라 허용 목록, 메서드 접근, 경로 검증을 다르게 적용할 수 있습니다.

```java
SandboxPolicy publicPolicy = SandboxPolicy.strict();
SandboxPolicy adminPolicy = SandboxPolicy.builder()
        .allowRawOutput()
        .build();

SandboxPolicyResolver policies = SandboxPolicyResolver
        .builder(publicPolicy)
        .prefix("admin/", adminPolicy)
        .build();

PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .policies(policies)
        .build();
```

엔진은 모든 정책의 합집합으로 파싱합니다. 태그는 파싱 시점에 검사합니다. 필터, 함수, 테스트, 메서드는 노드를 소유한 템플릿의 정책으로 렌더 시점에 검사합니다. include, macro, embed, component는 노드를 선언한 템플릿의 정책을 사용합니다. `escape`는 자동 이스케이프에 필요합니다. `default`와 `defined`는 정책이 거부할 때도 Pebble의 특수 처리를 유지합니다.

정확한 규칙이 prefix 규칙보다 우선하고, 가장 긴 prefix가 이깁니다. 출력 제한은 정책 중 가장 엄격한 유한 값입니다. 어느 정책이든 자동 이스케이프를 강제하면 켜집니다. 전역 변수는 모든 정책이 허용할 때만 노출합니다. 자세한 내용은 [docs/TEMPLATE-POLICIES.md](docs/TEMPLATE-POLICIES.md)를 참고하세요.

### 템플릿별 자원 한도

템플릿 이름에 따라 자원 한도를 다르게 적용할 수 있습니다.

```java
SandboxLimitsResolver limits = SandboxLimitsResolver
        .builder(SandboxLimits.from(policy))
        .prefix("public/", SandboxLimits.builder()
                .maxLoopIterations(1_000)
                .maxTotalIterations(10_000)
                .maxIncludeDepth(8)
                .maxSlotChars(100_000)
                .build())
        .build();

PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .sandbox(policy)
        .limitsResolver(limits)
        .build();
```

정확한 규칙이 prefix 규칙보다 우선하고, 가장 긴 prefix가 이깁니다. Spring Boot는 `SandboxLimitsResolver` 빈을 자동으로 사용합니다.

한도 리졸버를 넘기지 않으면 각 정책의 한도를 사용합니다.

## Spring Boot 4

```kotlin
implementation("io.github.allbegray.pebbleforge:pebble-forge-spring-boot-4-starter:0.1.0-SNAPSHOT")
```

이 스타터는 Spring Boot 4.0.6을 대상으로 합니다. 모듈 디렉터리와 아티팩트 이름에 `boot-4`를 넣어 메이저 버전 경계를 분명히 했습니다. Spring Boot 3 연동이 필요하면 별도 모듈로 만들어야 합니다.

스타터는 Pebble의 `PebbleAutoConfiguration`보다 먼저 실행됩니다. `PebbleForge`로 `pebbleEngine` 빈을 만들기 때문에 공식 엔진 빈은 등록되지 않습니다. `PebbleProperties`, 로더 빈, `SpringExtension`, 모든 `Extension` 빈, 공식 뷰 리졸버는 그대로 재사용합니다.

```properties
pebble.forge.components.enabled=true
pebble.forge.components.root=components
pebble.forge.components.suffix=
pebble.forge.components.max-depth=16

pebble.forge.sandbox.enabled=false
pebble.forge.sandbox.max-rendered-chars=1000000
pebble.forge.sandbox.max-loop-iterations=100000
pebble.forge.sandbox.max-total-iterations=1000000
pebble.forge.sandbox.max-include-depth=16
pebble.forge.sandbox.max-slot-chars=1000000
pebble.forge.sandbox.allow-raw-output=false
pebble.forge.sandbox.force-auto-escaping=true

pebble.forge.diagnostics.enabled=true
pebble.forge.diagnostics.context-lines=2
pebble.forge.diagnostics.expose-in-error-attributes=false

pebble.forge.security.enabled=true
pebble.forge.language.enabled=true
pebble.forge.dev-mode=false
```

언어 확장은 기본으로 켜집니다. 순수 Pebble로 파싱하려면 `pebble.forge.language.enabled=false`로 두세요.

`SandboxPolicy` 빈을 직접 정의하면 프로퍼티로 만든 정책을 대체합니다. `SandboxPolicyResolver` 빈은 템플릿별 허용 목록을 제공하고 프로퍼티 정책을 대체합니다. `SandboxPolicy` 빈과 `SandboxPolicyResolver` 빈 중 하나만 정의하세요. `SandboxLimitsResolver` 빈은 템플릿별 자원 한도를 제공합니다. `PebbleEngine` 빈을 직접 정의하면 스타터는 적용되지 않습니다. `pebbleLoader`나 `springExtension` 빈을 직접 정의하면 그 빈을 사용합니다.

스타터가 만든 샌드박스 정책에는 Pebble Spring 함수와 `extends`, `block` 태그가 자동으로 추가됩니다. 정책을 직접 만들 때는 `SpringWebFunctions.allow(builder)`를 호출하고, 레이아웃에 필요한 태그를 허용하세요.

진단이 켜져 있으면 스타터가 공식 Pebble 뷰를 감쌉니다. 렌더 실패는 실제 500 오류 디스패치가 되고, 애플리케이션 오류 페이지는 `pebbleDiagnostics` 또는 `pebbleDiagnosticsText`를 읽을 수 있습니다. 스타터는 오류 페이지를 소유하지 않습니다. Boot 오류 속성에 값을 노출하려면 `pebble.forge.diagnostics.expose-in-error-attributes=true`를 설정하세요. 기본값은 꺼짐입니다.

## Spring Security

보안 확장은 안전한 함수만 노출합니다. 템플릿에 `Authentication` 객체를 넘기지 않습니다.

| 함수 | 결과 |
| --- | --- |
| `isAuthenticated()` | 로그인한 사용자면 true입니다. 익명 인증은 false입니다. remember-me 인증은 true입니다. |
| `isAnonymous()` | 익명이거나 인증이 없으면 true입니다. |
| `isFullyAuthenticated()` | 익명도 remember-me도 아닌 사용자면 true입니다. |
| `isRememberMe()` | remember-me 인증이면 true입니다. |
| `username()` | 인증 이름입니다. 익명 사용자면 빈 문자열입니다. |
| `hasRole("ADMIN")` | `ROLE_ADMIN`을 확인합니다. 이미 `ROLE_`로 시작하는 역할은 그대로 사용합니다. |
| `hasAnyRole("ADMIN", "OWNER")` | 나열한 역할 중 하나라도 일치하면 true입니다. |
| `hasAuthority("order:read")` | 권한을 정확히 비교합니다. |
| `hasAnyAuthority("order:read", "order:write")` | 나열한 권한 중 하나라도 일치하면 true입니다. |
| `authorities()` | 권한 문자열 목록입니다. 익명 사용자면 빈 목록입니다. |
| `csrfToken()` | 현재 CSRF 토큰 값입니다. 요청이 없으면 빈 문자열입니다. |
| `csrfParameterName()` | CSRF 파라미터 이름입니다. 토큰이 없으면 `_csrf`입니다. |
| `csrfHeaderName()` | CSRF 헤더 이름입니다. 토큰이 없으면 `X-CSRF-TOKEN`입니다. |

```twig
{% if hasAnyRole("ADMIN", "OWNER") %}
  <a href="/admin">Admin</a>
{% endif %}

<form method="post">
  <input type="hidden" name="{{ csrfParameterName() }}" value="{{ csrfToken() }}">
</form>
```

샌드박스가 켜져 있으면 Boot 스타터가 이 함수들을 정책에 추가합니다. 정책을 직접 만들 때는 `SpringSecurityFunctions.allow(builder)`를 호출하세요.

보안 확장은 Spring Security Core, Spring Security Web, Spring Web, 서블릿 API가 모두 있을 때만 등록됩니다. 0.1 버전은 서블릿 스택만 검증했습니다. WebFlux는 검증하지 않았습니다.

## 데모

```shell
./gradlew :pebble-forge-demo:run
```

데모는 이름 있는 슬롯을 가진 컴포넌트를 렌더합니다. 샌드박스가 슬롯 안의 HTML을 이스케이프하는 것과 `raw` 필터를 차단하는 것, `switch`, `with`, `break`, `continue`가 동작하는 것, 그리고 include 오류의 소스 프레임 진단을 보여줍니다.

## 빌드

```shell
./gradlew test
./gradlew build
./gradlew publishToMavenLocal
```

## 프로젝트 문서

- [변경 기록](CHANGELOG.md)
- [기여 가이드](CONTRIBUTING.md)
- [계약 검사](docs/CONTRACTS.md)
- [템플릿 정책](docs/TEMPLATE-POLICIES.md)
- [진단](docs/DIAGNOSTICS.md)
- [보안 정책](SECURITY.md)
- [릴리스 체크리스트](docs/RELEASING.md)

## 라이선스

Apache License 2.0입니다. [LICENSE](LICENSE)를 참고하세요.

## 백로그

다음 작업, 릴리스 기준, 열린 결정은 [docs/BACKLOG.md](docs/BACKLOG.md)에 정리했습니다.

## 설계

기능 비교, 범위 결정, 허용한 한계는 [docs/DESIGN.md](docs/DESIGN.md)에 정리했습니다.

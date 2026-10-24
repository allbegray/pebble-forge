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

테스트는 69개입니다. 코어, Spring Security 헬퍼, Boot 자동 설정을 검증하고, 공식 Pebble 뷰 리졸버를 통과하는 MockMvc 요청도 확인합니다.

## 모듈

| 모듈 | 역할 |
| --- | --- |
| `pebble-forge-core` | 컴포넌트, 샌드박스, `PebbleForge.engine()` 빌더 |
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
| `switch`, `case`, `default`, `endswitch` | 처음 일치하는 case 하나만 렌더합니다. fall-through는 없습니다. |
| `break`, `continue` | `for` 반복을 제어합니다. `break`는 `switch`도 빠져나갑니다. |
| `with { ... }` | 추가 변수를 가진 스코프에서 본문을 렌더합니다. |

```twig
{% with {"role": page.role} %}
  {% switch role %}
    {% case "admin" %}Admin
    {% case "user" %}User
    {% default %}Guest
  {% endswitch %}
{% endwith %}

{% for item in items %}
  {% if item.hidden %}{% continue %}{% endif %}
  {% if item.stop %}{% break %}{% endif %}
  {{ item.name }}
{% endfor %}
```

`for`는 break와 continue를 이해하는 버전으로 교체됩니다. `loop.index`, `loop.first`, `loop.last`, `loop.length`, `loop.revindex`, `else` 분기는 그대로 유지합니다.

`break`와 `continue`는 템플릿 소스 기준으로 판정합니다. 반드시 `for`나 `switch` 안에 있어야 합니다. 매크로 안에 있는 `break`를 루프 밖에서 호출하는 경우는 이 보장에 포함되지 않습니다.

템플릿을 순수 Pebble로 파싱해야 한다면 언어 확장을 끄면 됩니다.

```java
PebbleForge.engine().languageExtensions(false)
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
| 메서드 접근 | 위험한 클래스, 메서드, 반환 타입을 차단합니다. public 필드도 차단합니다. |
| 템플릿 경로 | 절대 경로, `..`, `.` 세그먼트, 역슬래시, NUL, 콜론, 빈 세그먼트를 거부합니다. |

`range`는 기본 함수 목록에 없습니다. `..` 연산자는 렌더 시점에 `range`를 호출하므로 엄격 모드에서는 `..`도 막힙니다.

기능을 더하려면 `allowTags`, `allowFilters`, `allowFunctions`, `allowTests`, `allowGlobalVariables`, `allowComponents`, `allowRawOutput`을 사용하세요. `allowOnly*` 메서드는 기본 목록을 교체합니다.

샌드박스는 프로세스 내부의 권한 보호 장치이지 CPU 할당량이 아닙니다. 권한 악용과 출력 폭탄은 막지만, 큰 사용자 목록이나 재귀 include가 CPU를 오래 쓰는 것은 막지 못합니다. CPU와 메모리 공격까지 막아야 한다면 별도 프로세스에서 렌더하세요.

경로 제한은 문자열 기준입니다. 템플릿 이름의 상위 경로 이동은 막지만 심볼릭 링크는 해석하지 않습니다. 파일 로더 루트에 심볼릭 링크가 없도록 하거나, 실제 경로를 검사하는 로더를 사용하세요.

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
pebble.forge.sandbox.allow-raw-output=false
pebble.forge.sandbox.force-auto-escaping=true

pebble.forge.security.enabled=true
pebble.forge.language.enabled=true
```

언어 확장은 기본으로 켜집니다. 순수 Pebble로 파싱하려면 `pebble.forge.language.enabled=false`로 두세요.

`SandboxPolicy` 빈을 직접 정의하면 프로퍼티로 만든 정책을 대체합니다. `PebbleEngine` 빈을 직접 정의하면 스타터는 적용되지 않습니다. `pebbleLoader`나 `springExtension` 빈을 직접 정의하면 그 빈을 사용합니다.

스타터가 만든 샌드박스 정책에는 Pebble Spring 함수와 `extends`, `block` 태그가 자동으로 추가됩니다. 정책을 직접 만들 때는 `SpringWebFunctions.allow(builder)`를 호출하고, 레이아웃에 필요한 태그를 허용하세요.

## Spring Security

보안 확장은 안전한 함수만 노출합니다. 템플릿에 `Authentication` 객체를 넘기지 않습니다.

| 함수 | 결과 |
| --- | --- |
| `isAuthenticated()` | 로그인한 사용자면 true입니다. 익명 인증은 false입니다. |
| `username()` | 인증 이름입니다. 익명 사용자면 빈 문자열입니다. |
| `hasRole("ADMIN")` | `ROLE_ADMIN`을 확인합니다. 이미 `ROLE_`로 시작하는 역할은 그대로 사용합니다. |
| `hasAuthority("order:read")` | 권한을 정확히 비교합니다. |
| `authorities()` | 권한 문자열 목록입니다. 익명 사용자면 빈 목록입니다. |
| `csrfToken()` | 현재 CSRF 토큰 값입니다. 요청이 없으면 빈 문자열입니다. |
| `csrfParameterName()` | CSRF 파라미터 이름입니다. 토큰이 없으면 `_csrf`입니다. |
| `csrfHeaderName()` | CSRF 헤더 이름입니다. 토큰이 없으면 `X-CSRF-TOKEN`입니다. |

```twig
{% if hasRole("ADMIN") %}
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

데모는 이름 있는 슬롯을 가진 컴포넌트를 렌더합니다. 샌드박스가 슬롯 안의 HTML을 이스케이프하는 것과 `raw` 필터를 차단하는 것, 그리고 `switch`, `with`, `break`, `continue`가 동작하는 것을 보여줍니다.

## 빌드

```shell
./gradlew test
```

## 라이선스

Apache License 2.0입니다. [LICENSE](LICENSE)를 참고하세요.

## 백로그

다음 작업, 릴리스 기준, 열린 결정은 [docs/BACKLOG.md](docs/BACKLOG.md)에 정리했습니다.

## 설계

기능 비교, 범위 결정, 허용한 한계는 [docs/DESIGN.md](docs/DESIGN.md)에 정리했습니다.

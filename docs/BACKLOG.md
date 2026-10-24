# Pebble Forge 백로그

> English summary: P0 covers the public 0.1 release. P1 covers typed contracts, source-frame diagnostics, hot reload, language follow-ups, and sandbox hardening. P2 covers context-aware escaping, Natural HTML, WebFlux, and component libraries. The tables below hold the full list and acceptance criteria.

이 문서는 0.1 이후 작업을 추적한다. 계획이지 약속이 아니다. 기술 식별자는 영어로 두고 설명은 한국어로 적는다.

## 사용 규칙

- ID는 한 번 정하면 바꾸지 않는다.
- 완료한 항목은 표에서 `Done`으로 옮기고 관련 커밋이나 PR을 적는다.
- 새 항목은 완료 기준을 먼저 쓰고, 그 다음에 범위를 적는다.
- 결정이 필요한 항목은 `결정 필요`로 표시하고 문서 끝의 결정 목록에 추가한다.

## 현재 상태

- 버전은 `0.1.0-SNAPSHOT`이고 아직 배포하지 않았다.
- 테스트는 105개다. 컴포넌트 12, 언어 확장 23, for 호환성 9, 샌드박스 20, 계약 검사 16, 진단 8, Gradle 플러그인 3, Spring Security 5, Boot 컨텍스트 8, Boot 웹 1.
- 모듈은 `pebble-forge-core`, `pebble-forge-contracts`, `pebble-forge-diagnostics`, `pebble-forge-gradle-plugin`, `pebble-forge-spring`, `pebble-forge-spring-boot-4-starter`, `pebble-forge-demo`다.
- 컴포넌트, 이름 있는 슬롯, 샌드박스, `switch`, `break`, `continue`, `with`, 계약 검사, 소스 프레임 진단, Spring Boot 4, Spring Security 함수가 들어가 있다.
- Maven group과 Java 패키지는 `io.github.allbegray.pebbleforge`다.
- 라이선스, CI, Maven 배포 설정은 아직 없다.
- 알려진 한계는 [DESIGN.md](DESIGN.md)의 Accepted limits에 정리되어 있다.

## 우선순위

| 등급 | 의미 |
| --- | --- |
| P0 | 공개 0.1 릴리스 전에 반드시 끝낸다. |
| P1 | 0.2와 0.3 초반에 다룰 개발 경험과 안전성 작업이다. |
| P2 | 0.3 이후 출력 형식과 통합 작업이다. |
| P3 | 지금은 하지 않는다. 필요해지면 다시 판단한다. |

## 요약

| ID | 우선순위 | 작업 | 크기 | 상태 | 의존 |
| --- | --- | --- | --- | --- | --- |
| PF-001 | P0 | 라이선스와 저장소 메타데이터 | S | Done | 없음 |
| PF-002 | P0 | Maven 배포 설정 | M | Done | PF-001 |
| PF-003 | P0 | GitHub Actions CI | S | 작성 완료 | 없음 |
| PF-004 | P0 | `for` 호환성 매트릭스 | M | Done | 없음 |
| PF-005 | P0 | 공개 문서와 릴리스 체크리스트 | S | Done | PF-001, PF-002 |
| PF-006 | P0 | 보안 정책과 위협 모델 | S | Done | 없음 |
| PF-101 | P1 | 타입 계약 검사 모듈 | XL | 진행 중 | PF-004 |
| PF-102 | P1 | 소스 프레임 진단 | L | 진행 중 | 없음 |
| PF-103 | P1 | 핫 리로드 | M | 설계 필요 | 없음 |
| PF-104 | P1 | 언어 확장 후속 | M | 대기 | PF-004 |
| PF-105 | P1 | 샌드박스 강화 | L | 설계 필요 | 없음 |
| PF-106 | P1 | WebFlux 지원 | M | 대기 | 없음 |
| PF-107 | P1 | Spring Security 함수 확장 | S | 대기 | 없음 |
| PF-201 | P2 | 컨텍스트 인식 이스케이프 | XL | 보류 | 없음 |
| PF-202 | P2 | Natural HTML | XL | 보류 | PF-201 |
| PF-203 | P2 | 컴포넌트 라이브러리 | M | 보류 | 없음 |
| PF-204 | P2 | 성능 벤치마크 | M | 보류 | PF-004 |
| PF-205 | P2 | 퍼징과 속성 기반 테스트 | M | 보류 | PF-004 |
| PF-206 | P2 | Javadoc과 API 문서 | S | 보류 | PF-002 |
| PF-207 | P2 | 코드 품질 도구 | S | 보류 | PF-003 |
| PF-208 | P2 | Spring Boot 데모 앱 | M | 보류 | 없음 |

## 완료

| ID | 커밋 |
| --- | --- |
| PF-001 | `3be7c93` Apache-2.0 LICENSE |
| PF-002 | `20a7384`, `28ee531` Maven 배포와 Central Portal 설정 |
| PF-003 | `3d24f85` CI와 Dependabot. 첫 push에서 초록색 확인 필요 |
| PF-004 | `3233407` `for` 호환성 테스트 9개 |
| PF-005 | `79f8123` CHANGELOG, RELEASING, CONTRIBUTING |
| PF-006 | `79f8123` SECURITY.md |

## P0. 공개 0.1 릴리스

### PF-001 라이선스와 저장소 메타데이터

- 크기: S
- 상태: Done. Apache-2.0을 적용했다.
- 문제: 현재 라이선스가 없다. 공개 저장소만으로는 재사용과 기여가 어렵고, Maven Central도 라이선스 메타데이터를 요구한다.
- 범위: `LICENSE` 파일, README 라이선스 절, POM의 `licenses`, GitHub 저장소 설명과 토픽.
- 완료 기준: `LICENSE`가 저장소 루트에 있고, README와 POM이 같은 라이선스를 가리킨다.
- 결정: Apache-2.0을 권장한다. Java 생태계에서 익숙하고 특허 조항이 있다. MIT는 더 짧지만 특허 조항이 없다.

### PF-002 Maven 배포 설정

- 크기: M
- 상태: Done. `publishToMavenLocal`, staging 저장소, `nmcpCheckAggregationFiles`로 검증했다. 실제 Central 업로드는 `CENTRAL_USERNAME`과 `CENTRAL_PASSWORD`가 필요하다.
- 문제: `maven-publish`가 없어서 소비자가 의존성으로 가져갈 수 없다.
- 범위: core, spring, boot 4 starter에 `maven-publish`를 적용한다. demo는 배포하지 않는다. POM의 `name`, `description`, `url`, `scm`, `developers`, `licenses`를 채운다. sources jar와 javadoc jar를 만든다. Central Portal 배포와 GPG 서명을 설정한다.
- 완료 기준: `./gradlew publishToMavenLocal`이 세 모듈의 POM과 jar를 만든다. 별도 샘플 프로젝트가 로컬 저장소에서 의존성을 받아 컴파일한다. Central staging 배포는 자격 증명이 있을 때 검증한다.
- 의존: PF-001.
- 메모: group은 `io.github.allbegray.pebbleforge`다. 저장소는 `https://github.com/allbegray/pebble-forge`로 잡았다. 처음에는 GitHub Packages가 아니라 Maven Central을 목표로 한다.

### PF-003 GitHub Actions CI

- 크기: S
- 상태: 작성 완료. 첫 push에서 초록색을 확인해야 한다.
- 문제: 로컬 Gradle 실행에만 의존한다.
- 범위: JDK 21에서 `./gradlew build`를 실행하는 workflow, Gradle 캐시, 테스트 리포트 업로드, `main`과 PR 트리거, Dependabot 또는 Renovate 설정.
- 완료 기준: push와 PR에서 workflow가 초록색이다. 실패한 테스트가 리포트에 남는다. 의존성 업데이트 PR이 자동으로 열린다.
- 메모: Boot 4 테스트가 무거우므로 캐시를 켠다.

### PF-004 `for` 호환성 매트릭스

- 크기: M
- 상태: Done. 9개 테스트가 일반 Pebble과 출력을 비교한다.
- 문제: 언어 확장이 Pebble의 기본 `for`를 교체한다. `break`와 `continue`를 위해 필요했지만, 기본 동작이 미묘하게 달라지면 기존 템플릿이 깨진다.
- 범위: List, Set, Map, 배열, Enumeration, 빈 iterable과 `else`, `loop` 변수, 중첩 루프, parallel, strict variables 조합을 테스트한다. 가능하면 같은 입력을 일반 Pebble 엔진과 비교한다.
- 완료 기준: 매트릭스 테스트가 모두 통과한다. 의도한 차이는 문서에 적는다.
- 메모: Pebble 4.1.2의 `ForNode` 동작을 기준으로 삼는다.

### PF-005 공개 문서와 릴리스 체크리스트

- 크기: S
- 상태: Done. CHANGELOG, RELEASING, CONTRIBUTING, 로컬 설치 안내를 추가했다.
- 문제: 문서가 기능 설명에 치우쳐 있고, 릴리스 절차가 없다.
- 범위: README의 설치와 빠른 시작을 새로 확인한다. 코드 블록이 실제로 컴파일되는지 확인한다. `CHANGELOG.md`와 `docs/RELEASING.md`를 만든다. 버전 올리기, 태그, Central 배포, 배포 후 데모 확인 절차를 적는다.
- 완료 기준: 새 디렉터리에서 README만 보고 데모를 실행할 수 있다. 릴리스 체크리스트를 따라 0.1.0을 배포할 수 있다.
- 의존: PF-001, PF-002.

### PF-006 보안 정책과 위협 모델

- 크기: S
- 상태: Done. SECURITY.md에 신고 경로, 범위, 위협 모델을 정리했다.
- 문제: 샌드박스를 제공하지만 취약점 신고 경로와 보안 경계가 문서화되어 있지 않다.
- 범위: `SECURITY.md`, 지원 버전, 신고 이메일 또는 GitHub Security Advisory, 샌드박스가 막는 것과 막지 못하는 것.
- 완료 기준: 보안 신고 경로가 README에서 링크된다. 샌드박스의 비보장 영역이 DESIGN.md와 일치한다.

## P1. 0.2 개발 경험과 안전성

### PF-101 타입 계약 검사 모듈

- 크기: XL
- 상태: 진행 중. 1차 구현으로 `pebble-forge-contracts`와 `checkPebbleTemplates` Gradle 태스크를 추가했다.
- 문제: Pebble은 동적이라서 모델 변수와 컴포넌트 props 오류가 렌더 시점에야 드러난다. JTE, Qute, Rocker의 장점과 가장 큰 격차다.
- 범위: `pebble-forge-contracts` 모듈을 새로 만든다. 모델 스키마와 컴포넌트 props 계약을 입력으로 받고, 템플릿을 파싱해서 변수 읽기, 컴포넌트 호출, include와 extends를 검사한다. Gradle 태스크 `checkPebbleTemplates`를 제공한다.
- 현재 구현: AST 기반 변수 존재 검사, 컴포넌트 props 검사, 동적 이름 경고, Gradle DSL 스키마, `checkPebbleTemplates` 태스크, TestKit 3개. 자세한 내용은 [CONTRACTS.md](CONTRACTS.md)에 있다.
- 1차 구현 커밋: `f177277` AST 접근자, `1d53d5d` contracts 모듈과 Gradle 플러그인.
- 남은 것: JSON/YAML 스키마, 흐름 기반 지역 변수 분석, include와 extends 스코프 전파, 타입 검사, Plugin Portal 배포.
- 완료 기준: 좋은 fixture는 통과하고, 없는 변수나 잘못된 props를 쓰는 fixture는 파일과 줄 번호를 담아 빌드를 실패시킨다. 지원 문법 밖의 표현식은 오탐 대신 경고로 처리한다.
- 의존: PF-004.
- 결정: 첫 범위는 전체 Java 타입 시스템이 아니라 변수 존재와 컴포넌트 props 검사로 제한한다. 동적 include와 동적 컴포넌트 이름은 경고로 남긴다.

### PF-102 소스 프레임 진단

- 크기: L
- 상태: 진행 중. 소스 프레임, include 스택, 중첩 include, 파싱 오류, 소스 없음 처리를 구현했다.
- 문제: Pebble 오류는 줄 번호와 파일 이름은 있지만 소스 일부와 include 경로를 보여주지 않는다.
- 범위: 템플릿 소스를 읽어서 오류 줄과 캐럿을 출력한다. include와 extends 체인을 쌓아서 보여준다. Spring Boot 오류 처리와 연결할 수 있는 진단 객체를 제공한다.
- 현재 구현: `DiagnosticsExtension` include 추적, `TemplateDiagnostics` 소스 프레임, `DiagnosticReport` 구조화 출력, 8개 테스트. 자세한 내용은 [DIAGNOSTICS.md](DIAGNOSTICS.md)에 있다.
- 남은 것: extends 체인 캡처, Spring Boot 오류 페이지 연동, CLI와 Gradle 진단 태스크.
- 1차 구현 커밋: `d61932d` diagnostics 모듈, include 추적, 데모.
- 완료 기준: 중첩 include 안의 오류가 바깥 템플릿부터 안쪽 템플릿까지의 프레임을 보여준다. 테스트는 실제 템플릿 소스로 검증한다.
- 메모: 렌더 시점 예외를 감싸는 방식과 파싱 시점 예외를 그대로 두는 방식을 비교한다.

### PF-103 핫 리로드

- 크기: M
- 상태: 설계 필요
- 문제: Pebble 캐시 때문에 템플릿을 수정해도 엔진을 다시 만들기 전까지 반영되지 않는다. JTE와 Qute의 개발 모드와 격차가 있다.
- 범위: 파일 기반 로더에서 수정 시간을 확인하고 캐시를 무효화하는 dev mode를 만든다. `PebbleForge.devMode(true)`와 Boot 프로퍼티를 제공한다.
- 완료 기준: 임시 디렉터리의 템플릿을 수정한 뒤 같은 엔진의 다음 렌더가 새 내용을 출력한다. jar 안의 classpath 리소스는 대상이 아니라는 점을 문서화한다.
- 결정: FileLoader와 사용자 파일 로더만 지원할지, ClasspathLoader의 개발 디렉터리도 지원할지 정한다.

### PF-104 언어 확장 후속

- 크기: M
- 상태: 대기
- 문제: 첫 언어 확장은 최소 문법만 제공한다.
- 범위: `for key, value in map` 구조 분해, `with only` 스코프 격리, `case` 다중 값 문법, `do` 태그 필요성 검토.
- 완료 기준: 새 문법마다 파서 테스트, 샌드박스 테스트, README 예제가 있다.
- 결정: `case` 다중 값은 `case "a", "b"`와 `case "a" or "b"` 중 하나를 고른다. `do`는 `set`으로 대체 가능하므로 후순위다.

### PF-105 샌드박스 강화

- 크기: L
- 상태: 설계 필요
- 문제: 현재 샌드박스는 출력 크기와 경로 문자열은 막지만, 재귀 include와 큰 루프는 CPU를 소모할 수 있다. 파일 로더의 심볼릭 링크도 해결하지 않는다.
- 범위: include 깊이 제한, 루프 반복 횟수 제한, 심볼릭 링크를 해석하는 파일 로더, 템플릿별 정책, 슬롯 캡처 메모리 제한을 검토한다.
- 완료 기준: 재귀 include와 반복 폭탄 fixture가 제한된 시간 안에 실패한다. 심볼릭 링크가 루트 밖을 가리키면 로더가 거부한다.
- 메모: 프로세스 내부 CPU 제한은 완전하지 않다. 적대적 CPU는 별도 프로세스로 격리한다는 원칙을 유지한다.

### PF-106 WebFlux 지원

- 크기: M
- 상태: 대기
- 문제: Boot 4 스타터는 서블릿 뷰 리졸버만 검증했다. 리액티브 앱에서는 샌드박스 로더의 prefix 문제와 CSRF 접근이 다르다.
- 범위: 리액티브 뷰 리졸버의 prefix 정규화, `ServerWebExchange`에서 CSRF 토큰을 읽는 함수, WebTestClient 테스트.
- 완료 기준: WebFlux 앱이 템플릿을 렌더하고 CSRF 토큰을 출력한다. 기존 서블릿 테스트는 그대로 통과한다.
- 의존: PF-107의 함수 구조를 재사용할 수 있다.

### PF-107 Spring Security 함수 확장

- 크기: S
- 상태: 대기
- 문제: 현재는 `hasRole`과 `hasAuthority`만 있다. 여러 역할을 한 번에 검사하는 함수가 없다.
- 범위: `hasAnyRole`, `hasAnyAuthority`, `isAnonymous`, 필요하면 `isFullyAuthenticated`를 추가한다. 샌드박스 허용 목록도 함께 갱신한다.
- 완료 기준: 각 함수의 단위 테스트와 샌드박스 허용 테스트가 있다. README 함수 표가 갱신된다.

## P2. 0.3 출력과 통합

| ID | 작업 | 크기 | 설명 | 완료 기준 |
| --- | --- | --- | --- | --- |
| PF-201 | 컨텍스트 인식 이스케이프 | XL | HTML 본문, 속성, URL, JavaScript, CSS 문맥을 구분한다. | XSS fixture 세트가 통과하고, 문맥별 출력 테스트가 있다. |
| PF-202 | Natural HTML | XL | 브라우저에서 바로 열리는 HTML 템플릿과 Pebble 표현식을 함께 쓴다. | HTML 파서, 원본 위치 매핑, 브라우저 미리보기 테스트가 있다. |
| PF-203 | 컴포넌트 라이브러리 | M | 여러 컴포넌트 루트, 네임스페이스, jar에 담긴 컴포넌트를 지원한다. | 두 개의 루트에서 컴포넌트를 찾고 충돌을 감지한다. |
| PF-204 | 성능 벤치마크 | M | 일반 Pebble, 컴포넌트, 샌드박스, 언어 확장의 비용을 측정한다. | JMH 결과와 회귀 기준이 저장소에 있다. |
| PF-205 | 퍼징과 속성 기반 테스트 | M | 파서, 경로 검증, 샌드박스 allowlist를 무작위 입력으로 검증한다. | 실패 입력이 최소 재현 케이스로 저장된다. |
| PF-206 | Javadoc과 API 문서 | S | 공개 API에 Javadoc을 붙이고 문서 사이트를 만든다. | `javadoc` 태스크가 경고 없이 통과하고 링크가 배포된다. |
| PF-207 | 코드 품질 도구 | S | Spotless, Checkstyle 또는 ErrorProne, 의존성 업데이트를 붙인다. | CI에서 포맷과 정적 검사가 실행된다. |
| PF-208 | Spring Boot 데모 앱 | M | MVC, Security, 컴포넌트, 언어 확장을 한 앱에서 보여준다. | 앱을 실행하고 브라우저에서 페이지를 확인할 수 있다. |

## P3. 지금 하지 않는 것

- 클라이언트 런타임과 islands.
- 새로운 표현식 언어.
- Pebble 파서 전체 교체.
- 프로세스 내부 CPU 격리 보장.
- Spring Boot 3 전용 모듈. 필요해지면 별도 모듈로 만든다.
- 모든 표현식을 검사하는 완전한 정적 타입 시스템. PF-101은 변수와 props 범위에서 시작한다.

## 결정이 필요한 것

1. 첫 배포 대상을 Maven Central로 할지 GitHub Packages로 할지.
2. PF-101의 다음 스키마. 지금은 Gradle DSL을 쓴다. JSON과 YAML을 언제 추가할지 정한다.
3. PF-103의 핫 리로드 범위. 파일 로더만인지, classpath 개발 디렉터리까지인지.
4. PF-201과 PF-202의 순서. 보안을 먼저 할지, HTML 작성 경험을 먼저 할지.
5. PF-106 WebFlux를 0.2에 넣을지 0.3으로 미룰지.

## 릴리스 기준

| 릴리스 | 포함 | 완료 조건 |
| --- | --- | --- |
| 0.1.0 | 현재 기능 | P0 완료, Maven Central 배포, CI 초록색, 라이선스와 릴리스 노트 |
| 0.2.0 | PF-101, PF-102, PF-103, PF-104, PF-105, PF-107 | 계약 검사, 진단, 핫 리로드, 언어 후속, 샌드박스 강화, 보안 함수 확장 |
| 0.3.0 | PF-106, PF-201, PF-202, PF-203 | WebFlux, 컨텍스트 이스케이프, Natural HTML, 컴포넌트 라이브러리 |
| 1.0.0 | 안정 API | 공개 API 동결, 성능 기준, 문서 사이트, 마이그레이션 가이드 |

# Pebble Forge 백로그

> English summary: This file tracks only active work and a compact ledger of completed work. P0, P1, and P2 are closed. Follow-ups are listed separately.

이 문서는 남은 작업을 추적한다. 완료된 항목은 상세 절을 지우고 아래 완료 원장에 한 줄로 남긴다. 계획이지 약속이 아니다. 기술 식별자는 영어로 두고 설명은 한국어로 적는다.

## 사용 규칙

- ID는 한 번 정하면 바꾸지 않는다.
- 완료한 항목은 `남은 작업`에서 제거하고 `완료 원장`에 결과와 대표 커밋을 적는다.
- 완료된 항목의 상세 범위와 결정은 [DESIGN.md](DESIGN.md), 모듈 README, `CHANGELOG.md`가 맡는다.
- 새 항목은 완료 기준을 먼저 쓰고, 그 다음에 범위를 적는다.
- 결정이 필요한 항목은 `결정 필요`로 표시하고 문서 끝의 결정 목록에 추가한다.

## 현재 상태

- 버전은 `0.1.0-SNAPSHOT`이고 아직 배포하지 않았다.
- 테스트는 675개다. core 215, contracts 151, diagnostics 23, natural-html 130, Gradle 플러그인 45, CLI 14, Spring Security 10, Boot 39, benchmarks 5, demo app 8, fuzz 35.
- 모듈은 `pebble-forge-core`, `pebble-forge-natural-html`, `pebble-forge-contracts`, `pebble-forge-diagnostics`, `pebble-forge-gradle-plugin`, `pebble-forge-cli`, `pebble-forge-spring`, `pebble-forge-spring-boot-4-starter`, `pebble-forge-demo`, `pebble-forge-demo-app`, `pebble-forge-benchmarks`, `pebble-forge-fuzz`다.
- P0, P1, P2는 완료했다. WebFlux는 지원하지 않으며 WebMVC만 지원한다.
- PF-101과 PF-202.1은 완료로 닫았고, 아래 `완료 항목의 후속`에 남은 범위를 따로 적는다.
- Maven group과 Java 패키지는 `io.github.allbegray.pebbleforge`다.
- 라이선스, CI, Maven Central 배포 설정, Gradle Plugin Portal 설정은 들어가 있다. 원격 push 뒤 CI 초록색과 실제 배포를 확인해야 한다.
- 알려진 한계는 [DESIGN.md](DESIGN.md)의 Accepted limits에 정리되어 있다.

## 우선순위

| 등급 | 의미 |
| --- | --- |
| P1 | 0.2 개발 경험과 안전성 작업이다. 완료했다. |
| P2 | 0.3 출력 형식과 통합 작업이다. 완료했다. |
| P3 | 지금은 하지 않는다. 필요해지면 다시 판단한다. |

## 남은 작업

현재 활성 항목은 없다. 아래 `완료 항목의 후속`은 완료한 범위에서 의도적으로 남긴 확장 지점이다.

## 완료 항목의 후속

완료한 항목에서 의도적으로 남긴 범위다. 새 ID를 만들지 않고, 필요해지면 별도 항목으로 승격한다.

### PF-101 계약 검사

- 교차 템플릿 블록 실행 순서. 현재 루트 읽기는 같은 템플릿에서 블록 시작 줄만 비교한다.
- 표현식 수준 타입 검사, union, optional, nullability, 컴포넌트 prop 타입.
- `required` 속성 강제. 현재는 스키마에서 파싱만 한다.
- inline suppression comments.
- 실제 Gradle Plugin Portal 업로드. 설정과 메타데이터는 들어갔고, 릴리스 버전과 자격 증명이 필요하다.

### PF-202.1 Natural HTML

- `{% %}` 제어 흐름, include, extends, 컴포넌트, 매크로.
- JavaScript와 CSS 보간.
- cross-template 문맥 전파와 partial 문맥 선언.

## P3. 지금 하지 않는 것

- WebFlux 지원. PF-106은 영구 비목표이며 WebMVC만 지원한다.
- 클라이언트 런타임과 islands.
- 새로운 표현식 언어.
- Pebble 파서 전체 교체.
- 프로세스 내부 CPU 격리 보장.
- Spring Boot 3 전용 모듈. 필요해지면 별도 모듈로 만든다.
- 모든 표현식을 검사하는 완전한 정적 타입 시스템.
- 전체 Java 포맷터 도입. 현재 Spotless는 공백과 파일 끝 개행만 검사한다.

## 완료 원장

| ID | 작업 | 결과와 대표 커밋 |
| --- | --- | --- |
| PF-001 | 라이선스와 저장소 메타데이터 | Apache-2.0, README/POM 메타데이터. `3be7c93` |
| PF-002 | Maven 배포 설정 | 6개 모듈 배포, sources/javadoc jar, Central Portal 설정. `20a7384`, `28ee531` |
| PF-003 | GitHub Actions CI | CI, 테스트 리포트, Dependabot. 첫 원격 push에서 초록색 확인 필요. `3d24f85` |
| PF-004 | `for` 호환성 매트릭스 | Pebble 기본 `for`와 9개 fixture 비교. `3233407` |
| PF-005 | 공개 문서와 릴리스 체크리스트 | CHANGELOG, RELEASING, CONTRIBUTING. `79f8123` |
| PF-006 | 보안 정책과 위협 모델 | `SECURITY.md`와 신고 경로. `79f8123` |
| PF-101 | 타입 계약 검사 모듈 | 계약 검사, 소스 순서 흐름, include/extends, definite summaries, typed schema, 로컬 블록 순서, named roots. `f177277`부터 `b5fc1df`, `eb6d405` |
| PF-102 | 소스 프레임 진단 | include/extends 프레임, 구조화 frame kind, CLI, source-mapped error. `d61932d`부터 `8356c7a` |
| PF-103 | 핫 리로드 | 콘텐츠 해시 캐시 키, `devMode(true)`, Boot 프로퍼티. `cd8a34a` |
| PF-104 | 언어 확장 후속 | `for key, value`, `with only`, 다중 case. `d344ff8`, `c5cc808`, `5aa3bd5`, `4553588` |
| PF-105 | 샌드박스 강화 | 루프/예산/include/슬롯 제한, symlink-safe loader. `4038221`, `ee9c89e`, `705dace`, `f71e5de`, `bbcfda6`, `934694d` |
| PF-107 | Spring Security 함수 확장 | any-role, any-authority, anonymous, fully-authenticated, remember-me. `f861709` |
| PF-108 | 템플릿별 전체 allowlist 정책 | exact/longest-prefix 정책, include/extends/embed/macro/component 경계. `c91c3e1` |
| PF-201 | 컨텍스트 인식 이스케이프 | `html_attr`, `url`, 내장 `html`/`js`/`css` 고정, XSS fixture. `759ddb0` |
| PF-202.1 | Natural HTML | 브라우저형 `.html` 로더, jsoup, 소스 맵, 표현식 전용. `3460bbc` |
| PF-203 | 컴포넌트 라이브러리 | 네임스페이스 루트, jar classpath 컴포넌트, 충돌 검출, Boot/Gradle roots. `3c62eb5`, `2d290f5`, `c4c858b`, `eb6d405` |
| PF-204 | 성능 벤치마크 | JMH 5종, 실제 M1 Max 기준선, 25% 로컬 규칙. `44c7832` |
| PF-205 | 퍼징과 속성 기반 테스트 | jqwik 600 trials, 경로 jail, allowlist, minimizer, corpus. `63fa223` |
| PF-206 | Javadoc과 API 문서 | Javadoc 경고 0, `-Werror`, `javadocAll`, Pages 워크플로. `7c02dfa` |
| PF-207 | 코드 품질 도구 | Spotless, Checkstyle, CI 연결. `7c02dfa` |
| PF-208 | Spring Boot 데모 앱 | MVC, Security, CSRF 로그인, 컴포넌트, 언어 확장. `7a7b750` |

## 결정이 필요한 것

1. 첫 배포 대상을 Maven Central로 할지 GitHub Packages로 할지.
2. PF-101의 다음 스키마 범위. 표현식 타입, union, optional, `required` 강제를 어디까지 넣을지.
4. PF-108의 메서드 컨텍스트를 스레드 로컬 대신 명시적 렌더 컨텍스트로 바꿀지.
5. Natural HTML의 제어 흐름과 cross-template 문맥을 어떻게 설계할지.
6. URL 전략이 percent-encoding과 protocol-relative URL 거부까지 맡을지.
7. Gradle Plugin Portal 첫 배포 시점과 릴리스 버전.

## 릴리스 기준

| 릴리스 | 포함 | 완료 조건 |
| --- | --- | --- |
| 0.1.0 | P0 | Maven Central 배포, CI 초록색, 라이선스와 릴리스 노트 |
| 0.2.0 | PF-101, PF-102, PF-103, PF-104, PF-105, PF-107, PF-108 | 계약 검사, 진단, 핫 리로드, 언어 후속, 샌드박스 강화, 보안 함수 확장 |
| 0.3.0 | PF-201부터 PF-208까지 | 컨텍스트 이스케이프, Natural HTML PF-202.1, 컴포넌트 루트, 벤치마크, 퍼징, Javadoc/API 사이트, 코드 품질, Spring Boot 데모 앱 |
| 1.0.0 | 안정 API | 공개 API 동결, 성능 기준, 문서 사이트, 마이그레이션 가이드 |

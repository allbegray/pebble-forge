# pebble-forge-demo-app

Runnable Spring Boot 4 MVC application for Pebble Forge. It renders Pebble templates through
`pebble-forge-spring-boot-4-starter` and keeps the sandbox enabled.

## Run

```shell
./gradlew :pebble-forge-demo-app:run
```

Open <http://localhost:8080>.

Demo accounts:

| Username | Password | Authority |
| --- | --- | --- |
| `user` | `password` | `ROLE_USER` |
| `admin` | `password` | `ROLE_ADMIN` |

## Routes

| Route | Description |
| --- | --- |
| `/` | Home page with the feature list and current security state |
| `/components` | Two card components with different props, named slots, and a nested badge |
| `/language` | `switch`, `for`, `break`, `continue`, and `with` |
| `/admin` | Requires `ROLE_ADMIN`; shows the username and authorities |
| `/login` | Form login page with CSRF fields and an error message after a failed login |

The app uses an application plugin, not the Spring Boot Gradle plugin. The Spring Boot 4.0.6
dependency BOM supplies versions for the web, security, and test dependencies.

## Verify

```shell
./gradlew :pebble-forge-demo-app:test
./gradlew :pebble-forge-demo-app:build
```

The MockMvc tests cover every route, the unauthenticated admin redirect, an authenticated admin
request, and the CSRF login form.

## Configuration

`src/main/resources/application.yml` points Pebble at `templates/*.peb`, enables components under
`components`, enables the language extensions, and enables the sandbox with autoescaping. The
starter policy allows components, `extends` and `block`, the language tags, and the Spring Security
functions used by the templates.

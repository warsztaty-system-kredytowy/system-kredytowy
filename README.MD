# System kredytowy (SpringTrial)

Symulowany backend wniosków kredytowych: REST API, baza w pamięci, prosty formularz w `static`.

## Technologie

| Obszar | Stack |
|--------|--------|
| Język / JVM | Java **25** (Gradle Java toolchain) |
| Framework | **Spring Boot** 4.0.4 |
| API | Spring Web MVC (REST), **Springdoc OpenAPI** 3.0 (Swagger UI) |
| Persystencja | **Spring Data JPA**, **Hibernate**, baza **H2** (in-memory) |
| Bezpieczeństwo | Spring Security (obecnie szeroki dostęp + jawne `permitAll` pod dokumentację API) |
| Narzędzia | **Gradle**, **Lombok**, JUnit 5 / Spring Boot Test, Testcontainers (testy) |

## Wymagania

- **JDK 25** zainstalowany lokalnie **albo** [auto-provisioning toolchainu](https://docs.gradle.org/current/userguide/toolchains.html#sub:download_repositories) w `settings.gradle`. Bez tego polecenia Gradle zakończą się błędem `Cannot find a Java installation ... languageVersion=25`.

## Uruchomienie

Z katalogu projektu:

```bash
# Windows (PowerShell / CMD)
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

Aplikacja startuje zwykle na **http://localhost:8080** (port domyślny Spring Boot).

## Przydatne adresy (po starcie)

- Formularz wniosku: http://localhost:8080/index.html  
- REST (przykład): `POST http://localhost:8080/api/loans`  
- **Swagger UI:** http://localhost:8080/swagger-ui.html  
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs  

## Testy

```bash
.\gradlew.bat test
```

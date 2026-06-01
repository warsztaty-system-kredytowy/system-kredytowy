# System kredytowy (SpringTrial)

Symulowany backend wniosków kredytowych: REST API, baza w pamięci, prosty formularz w `static`.

## Technologie

| Obszar | Stack |
|--------|--------|
| Język / JVM | Java **25** (Gradle Java toolchain) |
| Framework | **Spring Boot** 4.0.4 |
| API | Spring Web MVC (REST), **Springdoc OpenAPI** 3.0 (Swagger UI) |
| Persystencja | **Spring Data JPA**, **Hibernate**, baza **PostgreSQL** |
| Bezpieczeństwo | Spring Security (obecnie szeroki dostęp + jawne `permitAll` pod dokumentację API) |
| Narzędzia | **Gradle**, **Lombok**, JUnit 5 / Spring Boot Test, Testcontainers (testy) |

## Wymagania

- **JDK 25** zainstalowany lokalnie **albo** [auto-provisioning toolchainu](https://docs.gradle.org/current/userguide/toolchains.html#sub:download_repositories) w `settings.gradle`. Bez tego polecenia Gradle zakończą się błędem `Cannot find a Java installation ... languageVersion=25`.

## Uruchomienie

### Lokalnie
Z katalogu projektu:

```bash
# Windows (PowerShell / CMD)
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```


### Przez Dockera
Nie musisz mieć lokalnie JDK/Gradle. Projekt zawiera wieloetapowy `Dockerfile`, który sam zbuduje JAR wewnątrz kontenera.

Aby keycloack działał poprawnie znajdź na swoim komputerze lokalizacje: ```C:\Windows\System32\drivers\etc```

Dodaj:
```127.0.0.1  localhost keycloak.local```

Z katalogu projektu uruchom:
```bash
docker compose up --build
```

Po starcie aplikacja jest dostępna na: **http://localhost:8080**


## Przydatne adresy (po starcie)

- Formularz wniosku: http://localhost:8080/index.html  
- REST (przykład): `POST http://localhost:8080/api/loans`  
- **Swagger UI:** http://localhost:8080/swagger-ui.html  
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs  

## Testy

```bash
.\gradlew.bat test
```

## Integracja z Keycloak i Bezpieczeństwo

Aplikacja integruje się z **Keycloak OIDC** w celu uwierzytelniania i autoryzacji użytkowników za pomocą standardowego przepływu OAuth 2.0. Wspierany jest podział na role użytkowników standardowych oraz pracowników (konsultantów kredytowych).

### Mapowanie Rol i Automatyczna Promocja Pracowników
Dostęp do zasobów zabezpieczony jest mechanizmem RBAC (Role-Based Access Control):
- **Zwykły użytkownik (`ROLE_USER`):** Może przeglądać swoje wnioski kredytowe, składać nowe i składać odwołania.
- **Pracownik / Konsultant (`ROLE_EMPLOYEE`):** Ma dostęp do panelu ocen (`/employee/**`, `/api/loans/*/approve`, etc.), może akceptować/odrzucać wnioski oraz generować oceny AI.

#### Automatyczne Promowanie na Pracownika (Indirect Promotion)
Aplikacja posiada elastyczny mechanizm automatycznego przyznawania uprawnień pracowniczych bez konieczności ręcznego przypisywania ról w Keycloaku:
1. **Domena e-mail:** Każdy użytkownik, którego adres e-mail kończy się na `@company.com` lub `@employee.loan.com`, automatycznie otrzymuje uprawnienia pracownika.
2. **Nazwa użytkownika:** Każdy użytkownik, którego login rozpoczyna się od prefiksu `employee_` (np. `employee_jan`) lub jest dokładnie równy `employee`, automatycznie otrzymuje rolę `ROLE_EMPLOYEE`.

### Konta testowe
W konfiguracji Keycloaka (`realm-export.json`) zdefiniowane są dwa gotowe konta testowe do szybkiego sprawdzenia działania:
- **Konto Konsultanta (Pracownik):**
  - **Login:** `employee`
  - **Hasło:** `password`
- **Konto Standardowego Użytkownika:**
  - **Login:** `user`
  - **Hasło:** `password`

---

## Manual Rebuild Status — 2026-07-23

A new Arc Transit System repository is being rebuilt manually. The previous
generated implementation remains available as a read-only architectural reference.

### Verified in the new repository

- Java 21 project target
- Spring Boot 4.1.0
- Maven Wrapper
- Successful `mvnw.cmd validate`
- Successful `mvnw.cmd clean test`
- Successful executable JAR startup
- Vaadin 25.2.3 BOM and dependencies
- Vaadin Maven frontend plugin
- VS Code Vaadin extension and Hotswap Agent setup

### Current limitations

- No Arc Transit Vaadin route has rendered yet
- Spring Modulith has not been added
- PostgreSQL and Docker Compose have not been configured
- Flyway migrations have not been created
- JPA persistence and Spring Security have not been added

### Documentation rule

Only features verified in the new repository should be marked Completed.
The previous generated repository must not be used as evidence that the
manual rebuild task is complete.


add on implementation plan:

> **Current implementation note — 2026-07-23:**  
> This guide originally described the previous generated implementation.
> The project is now being rebuilt manually in a new repository. Treat
> unverified sections as reference material until they are reproduced,
> tested, and recorded in the new repository.
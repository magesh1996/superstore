```
root/
├── pom.xml                      (already exists)
├── docker-compose.yml           (new)
├── .dockerignore                (new)
├── service-app/
│   ├── pom.xml                  (already exists)
│   └── Dockerfile               (new)
├── service-eureka/
│   ├── pom.xml
│   └── Dockerfile               (new)
├── service-gateway/
│   ├── pom.xml
│   └── Dockerfile               (new)
├── service-order/
│   ├── pom.xml
│   └── Dockerfile               (new)
└── service-product/
    ├── pom.xml
    └── Dockerfile               (new)
```

every Dockerfile is written to be built with the **root as build context** 
(`context: .` in docker-compose.yml), not the module folder itself - 
that's required so Maven can see the parent POM.

## running it

```bash
docker compose up --build
```

first build will take a while (Maven downloads the full dependency tree, and
`service-app` additionally installs Node and runs the Vaadin frontend build).
subsequent builds reuse the `~/.m2` cache mount, so they're much faster.

- Eureka dashboard: http://localhost:8761 (user `eureka` / `eureka`)
- Gateway: http://localhost:9000
- App (Vaadin UI): http://localhost:8080
- Product service (direct): http://localhost:8081
- Order service (direct): http://localhost:8082

## What I changed vs. what I left alone

did **not** touch `application.properties` files. 
local/IDE runs still work exactly as before, hitting `localhost` for everything. 
inside Docker, `docker-compose.yml` overrides the relevant properties with
environment variables (Spring Boot's relaxed binding : `SPRING_DATASOURCE_URL`
overrides `spring.datasource.url`, etc.), pointing them at the Docker service
names (`eureka`, `postgres`, `oracle`, `redis`, `kafka`, `gateway`) instead of `localhost`.

## Notes / things worth double-checking

- **Oracle startup time.** 
  `gvenzl/oracle-free` can take 30–90s to become healthy on first boot (it's initializing the database files). 
  the `depends_on: condition: service_healthy` + generous `start_period` should handle it, 
  but the first `docker compose up` will look like it's hanging on `oracle-db` - that's normal.

- **Kafka.** 
  using `apache/kafka:3.8.0` in KRaft mode (no separate Zookeeper container needed).

- **Vaadin production mode.** 
  our `service-app/pom.xml` runs `vaadin-maven-plugin:build-frontend` 
  but didn't see a `production` Maven profile. 
  if we have one elsewhere (common in Vaadin starters, sets `vaadin.productionMode=true`), 
  add `-Pproduction` to the `mvn package` line in `service-app/Dockerfile` for a production frontend bundle instead of the dev bundle.

- **health checks / basic auth.** 
  every module inherits `spring-boot-starter-security` from the root POM, 
  so actuator's `/actuator/health` is protected. 
  the `eureka` healthcheck in docker-compose.yml uses the `eureka`/`eureka` credentials from its `application.properties`.

- **secrets.** 
  DB passwords, the JWT secret, etc. are still the plaintext dev values from your properties files, 
  just relocated into docker-compose.yml.
  fine for local dev; 
  swap for Docker secrets / a `.env` file (git-ignored) or a real secrets manager before this goes anywhere near production.

- **recommended change**
  keep build stages as-is (eclipse-temurin:25-jdk-jammy), 
  switch every runtime stage from eclipse-temurin:25-jre-jammy -> eclipse-temurin:25-jre-alpine.
  one catch : Alpine doesn't have apt-get, so the service-eureka/Dockerfile runtime-stage curl install needs apk add --no-cache curl instead, and useradd needs adduser (Alpine's busybox doesn't ship useradd).
# superstore project context

use this file as project-specific guidance when analyzing or changing this repository. 
treat the build files and runtime configuration as the source of truth; update this document when those files change.

## project structure

this is a Maven multi-module Spring application:

- `service-app`: Vaadin web UI and frontend entry point.
- `service-gateway`: reactive Spring Cloud Gateway; routes requests to backend services
- `service-eureka`: service registry
- `service-security`: authentication/security service; JWT and Liquibase
- `service-product`: product service; Spring AI, Ollama, PGVector, and MCP server
- `service-order`: order service
- `service-chatbot`: chatbot service; Spring AI, Ollama, Google GenAI, PGVector, and MCP client

the usual request path is browser -> `service-app` -> `service-gateway` -> backend services. 
the gateway discovers backend services through Eureka.

supporting files and directories include `postman/` for API collections, `documents/` for project material, and the sibling `keys/` directory for local key files.

## declared versions

versions below are read from the project manifests and compose files. 
Maven-managed transitive versions may differ from these properties and should be checked with Maven when an exact resolved version is needed.

### Java and Maven

| component | declared version |
| --- | --- |
| Java | 25 |
| Maven Wrapper | 3.3.4 |
| Maven Distribution | 3.9.15 |
| Spring Boot | 4.0.8 |
| Spring Cloud BOM | 2025.1.2 |

all service Dockerfiles use Eclipse Temurin 25 on Ubuntu Jammy (`eclipse-temurin:25-jdk-jammy` for builds and `eclipse-temurin:25-jre-jammy` at runtime).

### shared Java dependencies and properties

declared in the root `pom.xml`:

| component/property | version |
| --- | --- |
| Vaadin Maven property | 25.1.5 |
| Spring AI BOM | 2.0.0 |
| Ollama property | 1.1.8 |
| PGVector Spring AI dependency property | 2.0.0 |
| Lombok | 1.18.46 |
| MapStruct | 1.6.3 |
| Lombok-MapStruct binding | 0.2.0 |
| Byte Buddy | 1.17.8 |
| Mockito property | 5.23.0 |
| JJWT | 0.12.6 |
| ZXing | 3.5.2 |
| ONNX Runtime | 1.27.0 |

Spring Boot manages versions for dependencies without an explicit version in the POM. Spring Cloud dependencies use the Spring Cloud BOM. do not add versions to individual Spring dependencies without a project-specific reason.

### frontend

`service-app/package.json` and `service-app/package-lock.json` declare the frontend toolchain. 
the lockfile is version 3 and pins transitive npm package versions.

| component | version |
| --- | --- |
| Vaadin Maven property | 25.1.5 |
| Vaadin frontend packages (most) | 25.1.2 |
| Node used in the service-app Docker build | 20.x |
| React / React DOM | 19.2.4 |
| React Router | 7.13.1 |
| Vite | 7.3.2 |
| TypeScript | 5.9.3 |
| Lit | 3.3.2 |
| date-fns | 4.1.0 |
| Workbox packages | 7.4.0 |

note: the Vaadin Maven property is 25.1.5 while the frontend package versions are mostly 25.1.2. 
preserve this distinction unless intentionally aligning and validating the toolchain.

### infrastructure and observability images

declared in the Compose files:

| component | image/version |
| --- | --- |
| PostgreSQL with pgvector | `pgvector/pgvector:pg16` |
| Redis | `redis:7-alpine` |
| Apache Kafka | `apache/kafka:3.8.0` |
| AKHQ (optional tools profile) | `tchiotludo/akhq:latest` |
| Prometheus | `prom/prometheus:v2.54.1` |
| Grafana | `grafana/grafana:11.1.0` |
| Loki | `grafana/loki:3.2.1` |
| Tempo | `grafana/tempo:2.6.1` |
| Pyroscope | `grafana/pyroscope:1.13.0` |
| Grafana Alloy | `grafana/alloy:v1.4.2` |

Oracle Free (`gvenzl/oracle-free:23-slim`) appears only in commented Compose configuration; the active database configuration is PostgreSQL.

## ports and local services

| service | port |
| --- | ---: |
| `service-app` | 8080 |
| `service-product` | 8081 |
| `service-order` | 8082 |
| `service-chatbot` | 8083 |
| `service-eureka` | 8761 |
| `service-gateway` | 9000 |
| `service-security` | 9100 |

local infrastructure defaults in application configuration include PostgreSQL on `localhost:5432`, Redis on `localhost:6379`, Kafka on `localhost:9092`, and Ollama on `localhost:11434`. Compose host port mappings can differ; use the active Compose file and `.env` file when running containers.

the observability Compose overlay exposes Grafana on 3000, Prometheus on 9090, Loki on 3100, Tempo on 3200, and Pyroscope on 4040.

## data, AI, and messaging notes

- the configured database is PostgreSQL; Hibernate uses `ddl-auto=validate`. do not change this to schema mutation casually.
- the configured schema is `superstore`. `service-security` Liquibase changelogs are under its resources.
- Redis is used for caching and related session/rate-limiting functionality.
- Kafka listeners are configured not to auto-start in several services; preserve that behavior unless changing messaging startup is part of the task.
- Spring AI model identifiers include Ollama `llama3.2:1b` for chat and `nomic-embed-text` for embeddings. the chatbot also configures Google GenAI `gemini-2.5-flash`.
- PGVector is configured for 768-dimensional embeddings and the `superstore.vector_store` table.
- `service-product` exposes an MCP server at `/mcp`; `service-chatbot` is configured as an MCP client.

model identifiers are runtime configuration, not Maven/npm dependency versions.

## development guidance

- keep changes within the owning service where possible; shared dependency and version changes belong in the root POM.
- use the Maven Wrapper from the `root/` directory on Windows, a targeted test command is:
  `.\mvnw.cmd -pl service-order -am test`
- for the Vaadin UI module, use a targeted Maven build such as:
  `.\mvnw.cmd -pl service-app -am package`
- frontend dependency changes should update `service-app/package.json` and its lockfile together.
- prefer focused tests for the service being changed. do not update unrelated modules or dependencies as cleanup.
- runtime configuration is often overridden by environment variables in Compose. inspect both the service properties and Compose definitions when debugging deployment behavior.
- treat `.env` values, API tokens, passwords, and private keys as secrets. use the example environment file for variable names; do not commit real credentials.
- some local key paths are Windows-specific. avoid introducing new absolute machine-specific paths; prefer configurable paths or container-appropriate mounts.
- check the `postman/collections/superstore/` collections when modifying or debugging API contracts.
- do not infer resolved Maven dependency versions from the root properties alone. use the Maven dependency tree when exact runtime versions matter.
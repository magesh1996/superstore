# syntax=docker/dockerfile:1

# build context for this Dockerfile MUST be the root (the folder that contains pom.xml and all service-* modules), 
# because Maven needs the parent POM to resolve dependencyManagement. 
# see docker-compose.yml (context: .)

#   docker build -f service-eureka/Dockerfile -t superstore/service-eureka .

# ---------- build stage ----------
FROM eclipse-temurin:25-jdk-jammy AS build

RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

# copy only the POM first so Docker can cache the dependency download layer as long as no POM has changed.
COPY pom.xml ./
COPY service-app/pom.xml service-app/pom.xml
COPY service-eureka/pom.xml service-eureka/pom.xml
COPY service-gateway/pom.xml service-gateway/pom.xml
COPY service-order/pom.xml service-order/pom.xml
COPY service-product/pom.xml service-product/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl service-eureka -am dependency:go-offline

# now bring in the actual source and build just this module 
# (-am builds the parent reactor metadata it needs, -pl restricts the packaged artifact).
COPY service-eureka service-eureka

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl service-eureka -am clean package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:25-jre-jammy AS runtime

# curl is only added here so docker-compose's healthcheck can hit /actuator/health.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --create-home --shell /usr/sbin/nologin spring
USER spring
WORKDIR /app

COPY --from=build /workspace/service-eureka/target/*.jar app.jar

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

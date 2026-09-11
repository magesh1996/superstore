# syntax=docker/dockerfile:1

# build context for this Dockerfile MUST be the root.
#   docker build -f service-app/Dockerfile -t superstore/service-app .

# NOTE : this is the Vaadin front-end module, so the build stage needs Node/npm in addition to Maven+JDK. 
# vaadin-maven-plugin CAN download its own pinned Node/npm automatically if none is found, 
# but installing Node explicitly here makes the build faster 
# and avoids relying on that auto-download succeeding in a locked-down network.

# if we have a Maven "production" profile that sets vaadin.productionMode=true
# (common in Vaadin starters, not present in the POM), 
# add "-Pproduction" to the final `mvn package` command below.

# ---------- build stage ----------
FROM eclipse-temurin:25-jdk-jammy AS build

RUN apt-get update \
    && apt-get install -y --no-install-recommends maven curl ca-certificates gnupg \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

COPY pom.xml ./
COPY service-app/pom.xml service-app/pom.xml
COPY service-eureka/pom.xml service-eureka/pom.xml
COPY service-gateway/pom.xml service-gateway/pom.xml
COPY service-order/pom.xml service-order/pom.xml
COPY service-product/pom.xml service-product/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl service-app -am dependency:go-offline

COPY service-app service-app

# cache npm packages across builds too, keyed on the mount target.
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.cache/npm \
    mvn -q -pl service-app -am clean package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:25-jre-jammy AS runtime

RUN useradd --create-home --shell /usr/sbin/nologin spring
USER spring
WORKDIR /app

COPY --from=build /workspace/service-app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

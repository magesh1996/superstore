# syntax=docker/dockerfile:1
#
# build context for this Dockerfile MUST be the reactor root.
#   docker build -f service-security/Dockerfile -t superstore/service-security .

# ---------- build stage ----------
FROM eclipse-temurin:25-jdk-jammy AS build

RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

COPY pom.xml ./
COPY service-app/pom.xml service-app/pom.xml
COPY service-eureka/pom.xml service-eureka/pom.xml
COPY service-gateway/pom.xml service-gateway/pom.xml
COPY service-order/pom.xml service-order/pom.xml
COPY service-product/pom.xml service-product/pom.xml
COPY service-chatbot/pom.xml service-chatbot/pom.xml
COPY service-security/pom.xml service-security/pom.xml

# RUN --mount=type=cache,target=/root/.m2 \
# RUN --mount=type=cache,target=/root/.m2,sharing=locked \
RUN --mount=type=cache,id=maven-service-security,target=/root/.m2 \
    mvn -q -pl service-security -am dependency:go-offline

COPY service-security service-security

# RUN --mount=type=cache,target=/root/.m2 \
# RUN --mount=type=cache,target=/root/.m2,sharing=locked \
RUN --mount=type=cache,id=maven-service-security,target=/root/.m2 \
    mvn -q -pl service-security -am clean package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:25-jre-jammy AS runtime

RUN useradd --create-home --shell /usr/sbin/nologin spring
USER spring
WORKDIR /app

COPY --from=build /workspace/service-security/target/*.jar app.jar

EXPOSE 9100
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

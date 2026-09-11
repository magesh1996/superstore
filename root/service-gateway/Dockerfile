# syntax=docker/dockerfile:1
#
# build context for this Dockerfile MUST be the root.
#   docker build -f service-gateway/Dockerfile -t superstore/service-gateway .

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

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl service-gateway -am dependency:go-offline

COPY service-gateway service-gateway

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl service-gateway -am clean package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:25-jre-jammy AS runtime

RUN useradd --create-home --shell /usr/sbin/nologin spring
USER spring
WORKDIR /app

COPY --from=build /workspace/service-gateway/target/*.jar app.jar

EXPOSE 9000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

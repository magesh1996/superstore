# superstore: a platform designed to scale retail and business operations.

```
[browser]
  |
  v
[service-app]:8080
  |
  V
[service-gateway]:9000 <---[service-eureka]:8761
  |
  v
[service-security]  :9100
[service-product]   :8081
[service-order]     :8082
[service-chatbot]   :8083
[service-payment]   :8084
  |
  V
-----------------------------------------------------
    PostgreSQL (2 DB + pgvector) | Redis | Kafka
-----------------------------------------------------
```

[service-monolith]: (module-order) ---> (Kafka topic: [order-created-event]  ) ---> [service-payment]
[service-monolith]: (module-order) <--- (Kafka topic: [payment-status-event] ) <--- [service-payment]

# observability URLs:
  Grafana   : http://localhost:3000
  Prometheus: http://localhost:9090
  Loki      : http://localhost:3100
  Tempo     : http://localhost:3200
  Pyroscope : http://localhost:4040 / http://host.docker.internal:4040
  AKHQ      : http://localhost:8090

# dev: base services only, no observability
docker compose --env-file .env.dev up --build -d

# test: base services only, no observability
docker compose --env-file .env.test up --build -d

# uat: include observability
docker compose --env-file .env.uat -f docker-compose.yml -f docker-compose.observability.yml up -d

# prod: include observability
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.observability.yml up -d

# starts all default services plus AKHQ:
docker compose --env-file .env.dev --profile tools up -d

# start only AKHQ and its dependencies (Kafka, Redis, and Postgres):
docker compose --env-file .env.dev --profile tools up -d akhq

# for specific services
docker compose --env-file .env.dev up -d --build service-product service-chatbot

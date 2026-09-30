// Grafana Alloy configuration file, which acts as the universal collector agent. 
// it performs two major jobs: 
    // 1. collecting stdout logs from your Docker containers and forwarding them to Loki.
    // 2. receiving OpenTelemetry traces from your microservices and forwarding them to Tempo.

// sets the internal logging verbosity for Grafana Alloy itself to "info" so you can see standard operational messages in its console.
logging {
  level = "info"
}

// tells Alloy to connect directly to the local Docker daemon via the UNIX socket (/var/run/docker.sock) to automatically find all running containers.
discovery.docker "containers" {
  host = "unix:///var/run/docker.sock"
}

// takes the raw metadata discovered from Docker and cleans it up into readable labels for your logs.
discovery.relabel "containers" {
  targets = discovery.docker.containers.targets

  // removes the leading slash (/) from the Docker container name (e.g., turns /my-app into my-app) and assigns it to a Loki label called container.
  rule {
    source_labels = ["__meta_docker_container_name"]
    regex         = "/(.*)"
    target_label  = "container"
  }

  // extracts the Docker Compose service name (if you use Docker Compose) and turns it into a service label.
  rule {
    source_labels = ["__meta_docker_container_label_com_docker_compose_service"]
    target_label  = "service"
  }

  // extracts the Docker Compose project (stack) name and assigns it to a compose_project label.
  rule {
    source_labels = ["__meta_docker_container_label_com_docker_compose_project"]
    target_label  = "compose_project"
  }
}

// log collection & forwarding to Loki.
// tells Alloy to actually tail the stdout and stderr streams of the Docker containers discovered in the previous steps.
loki.source.docker "containers" {
  // tags every log entry with a custom host identifier (superstore), which is useful if you run multiple servers/nodes.
  host_id    = "superstore"
  // sends the collected logs to the next block in the pipeline (loki.write.default).
  targets    = discovery.relabel.containers.output  
  forward_to = [loki.write.default.receiver]
}

// the exporter block that packages the logs and pushes them over HTTP to your Loki instance running at http://loki:3100/loki/api/v1/push.
loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}

// acts as an OpenTelemetry collector receiver, allowing your microservices to send traces directly to Alloy instead of sending them to Tempo directly.
// listens on port 4317 (gRPC) and 4318 (HTTP).
otelcol.receiver.otlp "default" {
  grpc {
    endpoint = "0.0.0.0:4317"
  }

  http {
    endpoint = "0.0.0.0:4318"
  }

  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}

otelcol.exporter.otlp "tempo" {
  client {
    endpoint = "tempo:4317"

    // disables TLS verification, which is standard for internal Docker container-to-container communication.
    tls {
      insecure = true
    }
  }
}
# ELK and Harmony AP Example Environment

This example demonstrates how to deploy a secure ELK stack alongside a Harmony AP application, using Docker Compose for orchestration.

## Overview

This environment shows how to:

1. Generate a self-signed CA and TLS certificates for Elasticsearch.
2. Initialize Elasticsearch with built-in users, roles, and TLS encryption.
3. Run Elasticsearch as a single-node cluster.
4. Deploy Kibana connected to the secured Elasticsearch instance.
5. Configure Logstash for JSON and GELF input, with structured filtering for application logs.
6. Run a MySQL database for Harmony AP data storage.
7. Launch the Harmony AP service with environment-driven configuration and optional GELF logging.

All services are defined in a single docker-compose.yml file and coordinated using Docker Compose.

## Prerequisites

- Docker & Docker Compose (installed & running)
- Bash shell (Unix-like environment)
- cURL (for health checks and API calls)

## Directory Structure

```
├── .env                   # Environment variables for ELK and Harmony AP
├── docker-compose.yml     # Docker Compose configuration for all services
├── elasticsearch-setup.sh # Script to generate certificates and configure Elasticsearch
├── logback.xml            # Java logging configuration for Harmony AP
└── logstash.conf          # Logstash pipeline definition
```

## Environment Variables

In the `.env` file, configure the ELK stack version and credentials:

```
ELK_VERSION=9.1.0

ELASTIC_PASSWORD=elasticchangeme
KIBANA_SYSTEM_PASSWORD=kibanasystemchangeme
LOGSTASH_WRITER_PASSWORD=logstashwriterchangeme
```

You may override these values to suit your requirements.

## Setup and Usage

### 1. Configure Environment Variables

Ensure the .env file exists in the project root and update any default passwords as needed.

### 2. Launch the Environment

Start all services in detached mode:

```bash
docker compose -p ap-centralized-logging up -d
```

Docker Compose will create and start:

- Elasticsearch-setup container to create a CA, issue node certificates, and set built-in user passwords. It will stop after setup.
- Elasticsearch-node container on port 9200 with TLS and security enabled.
- Kibana container on port 5601 for the web interface.
- Logstash container on ports 5014 (JSON input), 9600 (monitoring), and 12201/udp (GELF).
- MySQL container for the Harmony AP schema.
- Harmony-ap container on port 8443 for the application.

## Accessing the Services

- Kibana UI: http://localhost:5601
  - Username: `elastic`
  - Password: value of `ELASTIC_PASSWORD`, default is `elasticchangeme`
- Harmony AP Admin UI: https://localhost:8443
  - Username: `harmony`
  - Password: `changeme`

Logs will be available in Kibana under the Observability > Logs > Explorer section. This is the direct link: http://localhost:5601/app/observability-logs-explorer.

## Tear Down

To stop and remove all containers, networks, and volumes created by the `docker compose up` command, run:

```bash
docker compose -p ap-centralized-logging down -v
```

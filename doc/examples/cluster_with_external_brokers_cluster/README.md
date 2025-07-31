# Harmony AP cluster with external broker and load balancer

This example shows a minimal setup deploying a Harmony AP cluster connected to an ActiveMQ messaging broker and a MySQL database. Nginx is used as a reverse proxy and load balancer to distribute traffic across the service nodes.

This setup is designed for development and testing purposes.

## Overview

This environment demonstrates how to:

1. Generate self-signed TLS certificates and a PKCS#12 keystore for secure communication.
2. Run two instances of the Harmony-AP service in clustered mode.
3. Connect the service cluster to a MySQL database and a cluster of two ActiveMQ brokers.
4. Use Nginx as a reverse proxy, providing both stateless and sticky-session routing.

All components are orchestrated using Docker Compose.

## Prerequisites

- Docker & Docker Compose (installed & running)
- OpenSSL
- Bash shell (Unix-like environment)

## Setup and Usage

### 1. Generate TLS Certificates

Make the script `generate-tls.sh` executable and run it:

```bash
chmod +x generate-tls.sh
./generate-tls.sh
```

This will:

- Create a `./certs` directory
- Generate a private key and a self-signed certificate with the alias `harmony-ap`
- Export a PKCS#12 keystore
- Base64-encode the keystore into the `.env` file (used by the Harmony-AP containers)

### 2. Launch the Stack

Start all services in detached mode:

```bash
docker compose up -d
```

Docker Compose will create and start:
- Nginx (HTTPS entry point and load balancer)
  - Terminates TLS using the generated certificates
  - Routes `/services/wsplugin`, `/services/msh` calls to the stateless upstream
  - Routes admin UI traffic with session stickiness to the sticky upstream for in-memory session affinity
- `harmony-ap-luke` & `harmony-ap-leia` (clustered Harmony AP instances)
- `harmony-broker-gandalf` & `harmony-broker-frodo` (clustered ActiveMQ brokers)
- MySQL database

## Accessing the Services

Navigate to `https://localhost:8443/` to access the Harmony AP admin UI. The credentials to log in are:
- **Username:** `harmony`
- **Password:** `changeme`

## Tear Down

To stop and remove all containers, networks, and volumes created by the `docker compose up` command, run:

```bash
docker compose down -v
```

Using the `-v` flag will **also remove any named or anonymous volumes**, which means all persistent data will be deleted. The next time you run docker compose up, the environment will start from scratch — as if it’s a fresh setup. If you want to preserve volume data (e.g., for databases or other services that store state), omit the `-v` flag.

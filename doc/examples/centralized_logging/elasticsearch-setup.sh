#!/bin/bash
set -euo pipefail

if [ ! -f config/certs/ca.zip ]; then
  echo "Creating CA"
  bin/elasticsearch-certutil ca --silent --pem -out config/certs/ca.zip
  unzip config/certs/ca.zip -d config/certs
fi

if [ ! -f config/certs/certs.zip ]; then
  echo "Creating certs"
  cat > config/certs/instances.yml <<EOF
instances:
  - name: elasticsearch-node
    dns:
      - elasticsearch-node
      - localhost
    ip:
      - 127.0.0.1
EOF
  bin/elasticsearch-certutil cert --silent --pem \
    -out config/certs/certs.zip \
    --in config/certs/instances.yml \
    --ca-cert config/certs/ca/ca.crt \
    --ca-key config/certs/ca/ca.key
  unzip config/certs/certs.zip -d config/certs
fi

echo "Setting file permissions"
chown -R root:root config/certs
find config/certs -type d -exec chmod 755 {} \;
find config/certs -type f -exec chmod 644 {} \;

echo "Waiting for Elasticsearch availability"
until curl -s \
    --cacert config/certs/ca/ca.crt \
    https://elasticsearch-node:9200 \
  | grep -q "missing authentication credentials"; do
  sleep 5
done

echo "Setting kibana_system password"
curl -s -X POST https://elasticsearch-node:9200/_security/user/kibana_system/_password \
  -u elastic:${ELASTIC_PASSWORD} \
  --cacert config/certs/ca/ca.crt \
  -H "Content-Type: application/json" \
  -d "{\"password\":\"${KIBANA_SYSTEM_PASSWORD}\"}"

echo "Creating logstash_writer role"
curl -s -X PUT https://elasticsearch-node:9200/_security/role/logstash_writer \
  -u elastic:${ELASTIC_PASSWORD} \
  --cacert config/certs/ca/ca.crt \
  -H "Content-Type: application/json" \
  -d '{
    "cluster": ["manage_index_templates", "monitor"],
    "indices": [
      {
        "names": ["harmony-ap-*","logs-generic-default","logs-generic-default-*",".ds-logs-generic-default*"],
        "privileges": ["auto_configure", "create_index", "write"]
      }
    ]
  }'

echo "Create logstash_writer user"
curl -s -X POST https://elasticsearch-node:9200/_security/user/logstash_writer \
  -u elastic:${ELASTIC_PASSWORD} \
  --cacert config/certs/ca/ca.crt \
  -H "Content-Type: application/json" \
  -d '{
    "password":"'$LOGSTASH_WRITER_PASSWORD'",
    "roles": ["logstash_writer"],
    "full_name": "Logstash Writer Service"
  }'

echo "Passwords set and permissions configured."
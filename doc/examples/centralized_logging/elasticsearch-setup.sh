#!/bin/bash

if [ ! -f config/certs/ca.zip ]; then
  echo "Creating CA"
  bin/elasticsearch-certutil ca --silent --pem -out config/certs/ca.zip
  unzip config/certs/ca.zip -d config/certs
fi

if [ ! -f config/certs/certs.zip ]; then
  echo "Creating certs"
  echo -ne \
  "instances:\n"\
  "  - name: elasticsearch-node\n"\
  "    dns:\n"\
  "      - elasticsearch-node\n"\
  "      - localhost\n"\
  "    ip:\n"\
  "      - 127.0.0.1\n"\
  > config/certs/instances.yml
  bin/elasticsearch-certutil cert --silent --pem -out config/certs/certs.zip --in config/certs/instances.yml --ca-cert config/certs/ca/ca.crt --ca-key config/certs/ca/ca.key
  unzip config/certs/certs.zip -d config/certs
fi

echo "Setting file permissions"
chown -R root:root config/certs
find . -type d -exec chmod 755 {} \;
find . -type f -exec chmod 644 {} \;

echo "Waiting for Elasticsearch availability"
until curl -s --cacert config/certs/ca/ca.crt https://elasticsearch-node:9200 | grep -q "missing authentication credentials"; do sleep 30; done

echo "Setting kibana_system password"
until curl -s -X POST --cacert config/certs/ca/ca.crt -u "elastic:${ELASTIC_PASSWORD}" -H "Content-Type: application/json" https://elasticsearch-node:9200/_security/user/kibana_system/_password -d "{\"password\":\"${KIBANA_SYSTEM_PASSWORD}\"}" | grep -q "^{}"; do sleep 10; done

echo "Create logstash_writer role"
until curl -s -X POST --cacert config/certs/ca/ca.crt -u "elastic:${ELASTIC_PASSWORD}" -H "Content-Type: application/json" https://elasticsearch-node:9200/_security/role/logstash_writer -d "{\"cluster\":[\"manage_index_templates\",\"manage_ilm\",\"monitor\"],\"indices\":[{\"names\":[\"logs-*\",\".ds.*\",\"syslog-*\",\"unifi-*\"],\"privileges\":[\"write\",\"create\",\"create_index\",\"manage\",\"manage_ilm\"]}]}" | grep -q "^{\"role\":{\"created\":true}}"; do sleep 10; done

echo "Create logstash_internal user"
until curl -s -X POST --cacert config/certs/ca/ca.crt -u "elastic:${ELASTIC_PASSWORD}" -H "Content-Type: application/json" https://elasticsearch-node:9200/_security/user/logstash_internal -d "{\"password\":\"${LOGSTASH_INTERNAL_PASSWORD}\",\"roles\":[\"logstash_writer\"]}" | grep -q "^{\"created\":true}"; do sleep 10; done

echo "Create Agent Policy"
until curl -s -X POST -u "elastic:${ELASTIC_PASSWORD}" -H "Content-Type: application/json" -H "kbn-xsrf: true" kibana:5601/api/fleet/agent_policies?sys_monitoring=true -d "{\"name\":\"Agent policy 1\",\"namespace\":\"default\",\"monitoring_enabled\":[\"logs\",\"metrics\"]}" | grep -q "^{\"item\""; do sleep 10; done

echo "Elasticsearch setup completed"

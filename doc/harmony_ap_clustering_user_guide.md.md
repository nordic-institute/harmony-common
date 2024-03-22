# Harmony eDelivery Access - Access Point Clustering Guide <!-- omit in toc -->

Version: 1.0  
Doc. ID: UG-AP-C

---

## Version history <!-- omit in toc -->

Date       | Version | Description                                           | Author
---------- |---------|-------------------------------------------------------| --------------------
22.03.2024 | 1.0     | Initial version                                       | Diego Martin

## License <!-- omit in toc -->

This document is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
    To view a copy of this license, visit <https://creativecommons.org/licenses/by-sa/4.0/>

## Table of Contents <!-- omit in toc -->

<!-- vim-markdown-toc GFM -->

* [1 Introduction](#1-introduction)
  * [1.1 Target Audience](#11-target-audience)
  * [1.2 Terms and abbreviations](#12-terms-and-abbreviations)
  * [1.3 References](#13-references)
* [2. Running AP in a clustered environment](#2-running-ap-in-a-clustered-environment)
  * [2.1 Load balancing AP instances](#21-load-balancing-ap-instances)
    * [2.1.1 Sticky sessions](#211-sticky-sessions) 
  * [2.2 Message brokers](#22-message-brokers)
    * [2.2.1 Primary-secondary roles](#221-primary-secondary-roles)
    * [2.2.2 Shared storage](#222-shared-storage)
    * [2.2.3 Monitoring](#223-monitoring)
    * [2.2.4 Recovering from failures](#224-recovering-from-failures)
  * [2.3 Data storage](#23-data-storage)
* [3. Example of a clustered AP environment](#3-example-of-a-clustered-ap-environment)
  * [3.1 Docker Compose configuration](#31-docker-compose-configuration)
  * [3.2 ActiveMQ configuration](#32-activemq-configuration)
  * [3.3 Nginx configuration](#33-nginx-configuration)

<!-- vim-markdown-toc -->

## 1 Introduction

Harmony eDelivery Access Access Point offers the ability of being deployed in a cluster. This configuration will provide high availability capabilities, so it can operate continuously without intervention if some of the cluster components fail.

The high availability is achieved by deploying multiple instances of the Access Point in a cluster. The Access Point instances are configured to use the same message broker subclusters so that all the services involved in the setup have a fallback instance.

### 1.1 Target Audience

This document describes how to configure and run a containerized version of the Harmony eDelivery Access Access Point in a clustered environment.

The intended audience of this Clustering Guide are Access Point system administrators responsible for installing and using the Access Point software.

The document is intended for readers with a moderate knowledge of Docker, computer networks, and the eDelivery working principles.

### 1.2 Terms and abbreviations

The main terms used in this document are:

- AP - Access Point, a component that participants use to send and receive messages in an eDelivery network.

See eDelivery documentation \[[TERMS](#Ref_TERMS)\].

### 1.3 References

1. <a id="Ref_TERMS" class="anchor"></a>\[TERMS\] eDelivery Documentation, <https://ec.europa.eu/digital-building-blocks/sites/display/DIGITAL/eDelivery>
2. <a id="Ref_DOMIBUS_ADMIN_GUIDE" class="anchor"></a>\[DOMIBUS_ADMIN_GUIDE\] Access Point Administration Guide - Domibus 5.1.2, <https://ec.europa.eu/digital-building-blocks/sites/download/attachments/706380233/%28eDelivery%29%28AP%29%28AG%29%28Domibus%205.1.2%29%2820.7%29.pdf>
3. <a id="Ref_AMQ-CLUSTERING" class="anchor"></a>\[AMQ-CLUSTERING\] ActiveMQ Classic Clustering Documentation <https://activemq.apache.org/components/classic/documentation/clustering>
4. <a id="Ref_AMQ-SHAREDFILE-MASTERSLAVE" class="anchor"></a>\[AMQ-SHAREDFILE-MASTERSLAVE\] ActiveMQ Classic Shared File System Master Slave <https://activemq.apache.org/components/classic/documentation/shared-file-system-master-slave>

## 2. Running AP in a clustered environment

AP can be deployed in a clustered environment using any container orchestration tool (i.e. Docker Swarm, Kubernetes, or others). This section describes how to configure and run AP in a clustered environment.

When using the same environment variables in a set of AP Docker containers, they will form a cluster unit. The environment variables involved in the clustering configuration are:

| Environment variable      | Default    | Notes 
|---------------------------|------------|--------------
| `DEPLOYMENT_CLUSTERED`    | false      | Enables the clustered mode when set to `true`. Unless this variable is set to `true`, the rest of the clustering configuration variables will be ignored. Required as `true` when used in a cluster, the default value is `false`.
| `ACTIVEMQ_BROKER_HOST`    | *required* | The hostname of the ActiveMQ broker. It can be a comma-separated list of hostnames to define a subcluster. Required.
| `ACTIVEMQ_BROKER_NAME`    | localhost  | The name of the ActiveMQ broker. When using a subcluster of brokers, it must be a comma-separated list of broker names with the same number of elements and in the same order as `ACTIVEMQ_BROKER_HOST`. Required, default value `localhost`.
| `ACTIVEMQ_TRANSPORT_PORT` | 61616      | Port used in the ActiveMQ/s connection URI to the TCP socket that the clients will use to connect to the broker. Optional, default value `61616`.
| `ACTIVEMQ_JMX_PORT`       | 1199       | Port used in the ActiveMQ/s JMX monitoring URI. Optional, default value `1199`
| `ACTIVEMQ_USERNAME`       | *required* | The username to connect to the ActiveMQ broker, needs to be the same for all the members of the subcluster. Required.
| `ACTIVEMQ_PASSWORD`       | *required* | The password to connect to the ActiveMQ broker, needs to be the same for all the members of the subcluster. Required.

Along with the docker environment variables it's also mandatory to share the folder `/var/opt/harmony-ap` with read and write permissions. This folder contains the configuration files that are shared among the AP instances in the cluster unit.

### 2.1 Load balancing AP instances

When using multiple instances of AP, it is recommended to use a load balancer to distribute the incoming requests among the instances. The load balancer can be a hardware device, a software solution like [HAProxy](http://www.haproxy.org/), [Nginx](https://www.nginx.com/), [Traefik](https://traefik.io/), or a cloud service like [Amazon ELB](https://aws.amazon.com/elasticloadbalancing/), [Google Cloud Load Balancing](https://cloud.google.com/load-balancing),  [Azure Load Balancer](https://azure.microsoft.com/en-us/products/load-balancer/).

Using a load balancer will provide high availability and scalability to the AP instances. If one of the instances fails, the load balancer will stop sending requests to it, ensuring that the service is still available.

#### 2.1.1 Sticky sessions

The AP Admin UI requires sticky sessions enabled in the load balancer. Sticky sessions ensure that the requests from the same user are always sent to the same AP instance.

Depending on the load balancer used, the sticky sessions setup may vary.

### 2.2 Message brokers

While AP can run with a single message broker, it is recommended to use a subcluster of brokers to provide high availability. The subcluster can be formed by two or more brokers, and the AP instances will manage the connections to them.

When using a subcluster of brokers, the `ACTIVEMQ_BROKER_HOST` and `ACTIVEMQ_BROKER_NAME` environment variables must be set with the comma-separated list of hostnames and broker names, respectively. The number of elements in both lists must be the same, and the order of the elements must match. For example:

```bash
ACTIVEMQ_BROKER_HOST=harmony-jms-london,harmony-jms-roma,harmony-jms-berlin
ACTIVEMQ_BROKER_NAME=london,roma,berlin
```

#### 2.2.1 Primary-secondary roles

ActiveMQ brokers follow a primary-secondary architecture, where one broker is the primary and the rest are secondaries. The primary broker is the one that receives the messages from the AP instances, and the secondaries are the ones that replicate the messages from the primary and monitor the system to eventually become the new primary. The primary broker is elected by the boot order, the first ActiveMQ that completed the boot gets the primary role, locking the subcluster to ensure that there is only one primary broker.

The primary-secondary architecture is transparent to the AP instances, so they can connect to any broker in the subcluster. The AP instances use a failover URI generated from the `ACTIVEMQ_BROKER_HOST` and the `ACTIVEMQ_TRANSPORT_PORT` environment variables to connect to the brokers, so they can connect to the active primary broker at any time.

In order to have a reliable primary-secondary configuration, the brokers in the subcluster must have a unique name in the `brokerName` property in the `broker` node that can be found in the ActiveMQ XML configuration. 

For more information about ActiveMQ clustering, see the ActiveMQ documentation \[[AMQ-CLUSTERING](#Ref_AMQ-CLUSTERING)\].

#### 2.2.2 Shared storage

The brokers persist their work in a KahaDB database. KahaDB is a file based persistence database that stores the messages.

The brokers in the subcluster must share a volume to persist the lock status and the internal KahaDB database. The volume must be shared between all the brokers in the subcluster with read and write permissions. By default, the volume is located in the `/var/opt/apache-activemq` directory, but the location can be changed by modifying the `dataDirectory` in the `broker` node and the `directory` in the `kahaDB` node under `persistenceAdapter`.

For more information about ActiveMQ the shared storage between brokers, visit the ActiveMQ Shared File System Master Slave documentation \[[AMQ-SHAREDFILE-MASTERSLAVE](#Ref_AMQ-SHAREDFILE-MASTERSLAVE)\].

#### 2.2.3 Monitoring

When using a subcluster of brokers, AP works simultaneously with all the brokers attached to the subcluster through the JMX interface to ensure that they are working correctly.

The JMX monitoring URI is generated using the `ACTIVEMQ_BROKER_HOST` and the `ACTIVEMQ_JMX_PORT` environment variables. Creating a JMX connection per broker in the subcluster allowing AP to monitor the brokers individually.

#### 2.2.4 Recovering from failures

The primary broker creates and maintains a lock file in the shared volume to prevent other brokers from becoming the primary. If the primary broker fails, the lock file is removed and one of the secondaries will be promoted to primary locking again the system. If the old primary broker is recovered, it will join the secondary pool.

As the KahaDB database is shared between the brokers, the messages are available to all the brokers in the subcluster, even if one or more brokers fail. Any broker in the subcluster is able to continue the work of the failed broker.

The AP instances will automatically reconnect to the brokers in the subcluster if the connection is lost. The failover URI will allow the AP instances to connect to the new primary broker if the old one fails.

### 2.3 Data storage

The AP database can be a single instance or a cluster, depending on the requirements of the deployment. iIt is recommended to use a database cluster to achieve high availability.

The database connection is configured using the following environment variables of the AP Docker container:

| Environment variable | Default    | Notes
|----------------------|------------|--------------
| `DB_HOST`            | *required* | Database host name.
| `DB_PORT`            | 3306       | Database port. Optional, default value `3306`.
| `DB_SCHEMA`          | harmony_ap | Database schema. Optional, default value `harmony_ap`.
| `DB_USER`            | harmony_ap | Database user. Optional, default value `harmony_ap`.
| `DB_PASSWORD`        | *required* | Database password.

To achieve high availability, it's recommended to use a clustered database solution, such as [Amazon RDS](https://aws.amazon.com/rds/), [Google Cloud SQL](https://cloud.google.com/sql), or [Azure SQL Database](https://azure.microsoft.com/en-us/products/azure-sql/database).

## 3. Example of a clustered AP environment

In the following example, we will deploy a clustered AP environment using Docker Compose. The environment will consist of two AP instances connected simultaneously to two ActiveMQ brokers forming a subcluster, and a MySQL database:

### 3.1 Docker Compose configuration

```yaml
services:
  nginx:
    image: nginx:1.25.4-alpine
    depends_on:
      - harmony-ap-main
      - harmony-ap-replica
    ports:
      - "8080:80"
    volumes:
      - './nginx.conf:/etc/nginx/nginx.conf'

  harmony-ap-main:
    image: artifactory.niis.org/harmony-snapshot-docker/niis/harmony-ap:2.3.0
    depends_on:
      - harmony-db
      - harmony-jms-london
      - harmony-jms-berlin
    environment:
      - DB_HOST=harmony-db
      - DB_SCHEMA=harmony_ap
      - DB_PASSWORD=Pa$$sword
      - ADMIN_PASSWORD=Secret
      - USE_DYNAMIC_DISCOVERY=false
      - PARTY_NAME=org1_gw
      - SERVER_FQDN=harmony-ap
      - SECURITY_DN=CN=org1_gw,O=MyOrg,C=FI
      - SERVER_SAN=DNS:harmony-ap
      - SERVER_DN=CN=harmony-ap,O=MyOrg,C=FI
      - SECURITY_KEYSTORE_PASSWORD=password
      - SECURITY_TRUSTSTORE_PASSWORD=password
      - DEPLOYMENT_CLUSTERED=true
      - ACTIVEMQ_BROKER_HOST=harmony-jms-london,harmony-jms-berlin
      - ACTIVEMQ_BROKER_NAME=london,berlin
      - ACTIVEMQ_USERNAME=admin
      - ACTIVEMQ_PASSWORD=admin
    restart: on-failure
    mem_limit: 1500m
    volumes:
      - harmony-ap-data:/var/opt/harmony-ap

  harmony-ap-replica:
    image: artifactory.niis.org/harmony-snapshot-docker/niis/harmony-ap:2.3.0
    depends_on:
      - harmony-db
      - harmony-jms-london
      - harmony-jms-berlin
    environment:
      - DB_HOST=harmony-db
      - DB_SCHEMA=harmony_ap
      - DB_PASSWORD=Pa$$sword
      - ADMIN_PASSWORD=Secret
      - USE_DYNAMIC_DISCOVERY=false
      - PARTY_NAME=org1_gw
      - SERVER_FQDN=harmony-ap
      - SECURITY_DN=CN=org1_gw,O=MyOrg,C=FI
      - SERVER_SAN=DNS:harmony-ap
      - SERVER_DN=CN=harmony-ap,O=MyOrg,C=FI
      - SECURITY_KEYSTORE_PASSWORD=password
      - SECURITY_TRUSTSTORE_PASSWORD=password
      - DEPLOYMENT_CLUSTERED=true
      - ACTIVEMQ_BROKER_HOST=harmony-jms-london,harmony-jms-berlin
      - ACTIVEMQ_BROKER_NAME=london,berlin
      - ACTIVEMQ_USERNAME=admin
      - ACTIVEMQ_PASSWORD=admin
    restart: on-failure
    mem_limit: 1500m
    volumes:
      - harmony-ap-data:/var/opt/harmony-ap

  harmony-db:
    image: mysql:8
    environment:
      - MYSQL_RANDOM_ROOT_PASSWORD=1
      - MYSQL_DATABASE=harmony_ap
      - MYSQL_USER=harmony_ap
      - MYSQL_PASSWORD=Pa\$$sword
    command:
      - "--character-set-server=utf8mb4"
      - "--collation-server=utf8mb4_bin"
    restart: on-failure
    mem_limit: 512m

  harmony-jms-london:
    image: apache/activemq-classic:5.18.2
    environment:
      - ACTIVEMQ_CONNECTION_USER=admin
      - ACTIVEMQ_CONNECTION_PASSWORD=admin
      - ACTIVEMQ_JMX_USER=admin
      - ACTIVEMQ_JMX_PASSWORD=admin
      - ACTIVEMQ_WEB_USER=webadmin
      - ACTIVEMQ_WEB_PASSWORD=admin
      - ACTIVEMQ_DATA=/var/opt/apache-activemq
      - ACTIVEMQ_TMP=/var/tmp
    mem_limit: 1000m
    ports:
      - 8161:8161
    healthcheck:
      test: "exit 0"
      start_period: 10s
    volumes:
      - ./activemq-main.xml:/opt/apache-activemq/conf/activemq.xml
      - harmony-jms-data:/var/opt/apache-activemq

  harmony-jms-berlin:
    image: apache/activemq-classic:5.18.2
    environment:
      - ACTIVEMQ_CONNECTION_USER=admin
      - ACTIVEMQ_CONNECTION_PASSWORD=admin
      - ACTIVEMQ_JMX_USER=admin
      - ACTIVEMQ_JMX_PASSWORD=admin
      - ACTIVEMQ_WEB_USER=webadmin
      - ACTIVEMQ_WEB_PASSWORD=admin
      - ACTIVEMQ_DATA=/var/opt/apache-activemq
      - ACTIVEMQ_TMP=/var/tmp
    mem_limit: 1000m
    volumes:
      - ./activemq-replica.xml:/opt/apache-activemq/conf/activemq.xml
      - harmony-jms-data:/var/opt/apache-activemq

volumes:
  harmony-ap-data:
  harmony-jms-data:
```

### 3.2 ActiveMQ configuration

The configuration file `activemq.xml` used in the brokers looks like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<spring:beans
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:spring="http://www.springframework.org/schema/beans"
        xmlns:context="http://www.springframework.org/schema/context"
        xmlns="http://activemq.apache.org/schema/core"
        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
  http://activemq.apache.org/schema/core http://activemq.apache.org/schema/core/activemq-core.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

    <context:property-placeholder system-properties-mode="ENVIRONMENT" ignore-resource-not-found="false" ignore-unresolvable="false"/>

    <broker useJmx="true" brokerName="london"  persistent="true" schedulerSupport="true" dataDirectory="/var/opt/apache-activemq" tmpDataDirectory="/var/tmp">
        <managementContext>
            <managementContext createConnector="true" connectorPort="1199"/>
        </managementContext>
        <transportConnectors>
            <transportConnector uri="tcp://harmony-jms-london:61616" disableAsyncDispatch="true"/>
        </transportConnectors>
        <persistenceAdapter>
            <kahaDB directory="/var/opt/apache-activemq/work/kahadb" />
        </persistenceAdapter>
        <systemUsage>
            <systemUsage>
                <memoryUsage>
                    <memoryUsage limit="64 mb" />
                </memoryUsage>
                <storeUsage>
                    <storeUsage limit="512 mb" />
                </storeUsage>
                <tempUsage>
                    <tempUsage limit="128 mb" />
                </tempUsage>
            </systemUsage>
        </systemUsage>
        <destinations>
            <queue id="sendMessageQueue"
                   physicalName="domibus.internal.dispatch.queue"/>
            <queue id="sendLargeMessageQueue"
                   physicalName="domibus.internal.largeMessage.queue"/>
            <queue id="splitAndJoinQueue"
                   physicalName="domibus.internal.splitAndJoin.queue"/>
            <queue id="pullMessageQueue"
                   physicalName="domibus.internal.pull.queue"/>
            <queue id="retentionMessageQueue"
                   physicalName="domibus.internal.retentionMessage.queue"/>
            <queue id="sendPullReceiptQueue"
                   physicalName="domibus.internal.pull.receipt.queue"/>
            <queue id="alertMessageQueue"
                   physicalName="domibus.internal.alert.queue"/>
            <queue id="eArchiveQueue"
                   physicalName="domibus.internal.earchive.queue"/>
            <queue id="eArchiveNotificationQueue"
                   physicalName="domibus.internal.earchive.notification.queue"/>
            <queue id="eArchiveNotificationDLQ"
                   physicalName="domibus.internal.earchive.notification.dlq"/>

            <!--queue id="notifyBackendQueue"
                   physicalName="domibus.internal.notification.queue"/-->
            <!-- If no backend with matching policy found notifcations are sent to this queue -->
            <queue id="unknownReceiverQueue"
                   physicalName="domibus.internal.notification.unknown"/>

            <!-- Backend plugin notification queues -->
            <queue id="webserviceBackendNotificationQueue"
                   physicalName="domibus.notification.webservice"/>
            <queue id="jmsBackendNotificationQueue"
                   physicalName="domibus.notification.jms"/>
            <queue id="filesystemBackendNotificationQueue"
                   physicalName="domibus.notification.filesystem"/>

            <queue id="notifyAdapterKerkoviQueue"
                   physicalName="domibus.notification.kerkovi"/>

            <!-- FSPlugin queues -->
            <queue id="fsPluginSendQueue"
                   physicalName="domibus.fsplugin.send.queue"/>

            <!-- WSPlugin queues -->
            <queue id="wsPluginSendQueue"
                   physicalName="domibus.wsplugin.send.queue"/>

            <!-- Internal queues of JMS backend plugin -->
            <queue id="jmsPluginToBackendQueue"
                   physicalName="domibus.backend.jms.replyQueue"/>
            <queue id="businessMessageOutQueue"
                   physicalName="domibus.backend.jms.outQueue"/>
            <queue id="businessMessageInQueue"
                   physicalName="domibus.backend.jms.inQueue"/>
            <queue id="errorNotifyConsumerQueue" physicalName="domibus.backend.jms.errorNotifyConsumer" />
            <queue id="errorNotifyProducerQueue" physicalName="domibus.backend.jms.errorNotifyProducer" />
            <queue id="domibusDLQ" physicalName="domibus.DLQ" />
            <topic id="clusterCommandTopic" physicalName="domibus.internal.command"/>

        </destinations>
        <destinationPolicy>
            <policyMap>
                <policyEntries>
                    <policyEntry queue=">">
                        <deadLetterStrategy>
                            <!--<individualDeadLetterStrategy queuePrefix="DLQ."/>-->
                            <sharedDeadLetterStrategy processExpired="false">
                                <deadLetterQueue>
                                    <queue physicalName="domibus.DLQ"/>
                                </deadLetterQueue>
                            </sharedDeadLetterStrategy>
                        </deadLetterStrategy>
                        <dispatchPolicy>
                            <roundRobinDispatchPolicy/>
                        </dispatchPolicy>
                    </policyEntry>
                    <policyEntry queue="domibus.internal.earchive.notification.queue">
                        <deadLetterStrategy>
                            <sharedDeadLetterStrategy processExpired="false">
                                <deadLetterQueue>
                                    <queue physicalName="domibus.internal.earchive.notification.dlq"/>
                                </deadLetterQueue>
                            </sharedDeadLetterStrategy>
                        </deadLetterStrategy>
                        <dispatchPolicy>
                            <roundRobinDispatchPolicy/>
                        </dispatchPolicy>
                    </policyEntry>
                </policyEntries>
            </policyMap>
        </destinationPolicy>

        <plugins>
            <simpleAuthenticationPlugin anonymousAccessAllowed="false">
                <users>
                    <authenticationUser username="admin" password="admin" groups="admins,users" />
                </users>
            </simpleAuthenticationPlugin>
            <authorizationPlugin>
                <map>
                    <authorizationMap>
                        <authorizationEntries>
                            <authorizationEntry queue="domibus.>" read="users" write="users" admin="admins" />
                            <authorizationEntry queue="*.domibus.>" read="users" write="users" admin="admins" />
                            <authorizationEntry topic="domibus.>" read="users" write="users" admin="admins"/>
                            <authorizationEntry topic="ActiveMQ.Advisory.>" read="users" write="users" admin="users"/>
                        </authorizationEntries>
                    </authorizationMap>
                </map>
            </authorizationPlugin>
            <redeliveryPlugin fallbackToDeadLetter="true"
                              sendToDlqIfMaxRetriesExceeded="true">
                <redeliveryPolicyMap>
                    <redeliveryPolicyMap>
                        <defaultEntry>
                            <!-- default policy-->
                            <redeliveryPolicy maximumRedeliveries="10" redeliveryDelay="300000"/>
                        </defaultEntry>
                        <redeliveryPolicyEntries>
                            <redeliveryPolicy queue="domibus.internal.retentionMessage.queue" maximumRedeliveries="0"/>
                            <redeliveryPolicy queue="domibus.internal.dispatch.queue" maximumRedeliveries="0"/>
                            <redeliveryPolicy queue="domibus.internal.largeMessage.queue" maximumRedeliveries="0"/>
                            <redeliveryPolicy queue="domibus.internal.splitAndJoin.queue" maximumRedeliveries="3"/>
                            <redeliveryPolicy queue="domibus.internal.pull.queue" maximumRedeliveries="0"/>
                            <redeliveryPolicy queue="domibus.internal.pull.receipt.queue" maximumRedeliveries="3"/>
                            <redeliveryPolicy queue="domibus.internal.alert.queue" maximumRedeliveries="0"/>
                            <redeliveryPolicy queue="domibus.internal.earchive.queue" maximumRedeliveries="0"/>
                            <redeliveryPolicy queue="domibus.internal.earchive.notification.queue" maximumRedeliveries="6" redeliveryDelay="1800000"/>
                            <redeliveryPolicy queue="domibus.internal.earchive.notification.dlq" maximumRedeliveries="0"/>
                            <redeliveryPolicy queue="domibus.fsplugin.send.queue" maximumRedeliveries="0"/>
                            <redeliveryPolicy queue="domibus.wsplugin.send.queue" maximumRedeliveries="0"/>
                        </redeliveryPolicyEntries>
                    </redeliveryPolicyMap>
                </redeliveryPolicyMap>
            </redeliveryPlugin>

            <discardingDLQBrokerPlugin dropAll="false" dropOnly="domibus.internal.earchive.notification.dlq domibus.internal.dispatch.queue domibus.internal.pull.queue domibus.internal.alert.queue domibus.internal.earchive.queue domibus.internal.largeMessage.queue domibus.internal.retentionMessage.queue domibus.fsplugin.send.queue domibus.wsplugin.send.queue" reportInterval="10000"/>
        </plugins>
    </broker>

</spring:beans>
```

### 3.3 Nginx configuration

Nginx is used as a load balancer. The most basic configuration for the `nginx.conf` configuration file looks like this:

```nginx
events { worker_connections 1024; }

http {
    upstream harmony-ap {
        server harmony-ap-main:8080;
        server harmony-ap-replica:8080;
    }

    server {
        listen 80;

        location / {
            proxy_pass http://harmony-ap/;
        }
    }
}
```
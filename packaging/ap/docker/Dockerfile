FROM ubuntu:22.04 AS tmp

COPY packaging/commonbin/mysql-connector-j-8.0.33.jar /opt/harmony-ap/lib/
COPY packaging/commonbin/harmony-ap /opt/harmony-ap/webapps/ROOT/
COPY packaging/commonbin/ws-plugin.jar /opt/harmony-ap/plugins/lib/
COPY packaging/commonbin/tomcat9/bin/*.sh /opt/harmony-ap/bin/
COPY packaging/commonbin/tomcat9/bin/*.jar /opt/harmony-ap/bin/
COPY packaging/commonbin/tomcat9/lib /opt/harmony-ap/lib/
COPY packaging/commonbin/tomcat9/conf/web.xml /opt/harmony-ap/conf/
COPY packaging/commonbin/tomcat9/conf/catalina.properties /opt/harmony-ap/conf/
COPY packaging/ap/config/activemq.xml /opt/harmony-ap/internal/
COPY packaging/ap/config/*Policy*.xml /opt/harmony-ap/policies/
COPY packaging/ap/config/signOnly.xml /opt/harmony-ap/policies/
COPY packaging/ap/config/doNothingPolicy.xml /opt/harmony-ap/policies/
COPY packaging/ap/config/LICENSE.txt /usr/share/doc/harmony-ap/
COPY packaging/ap/config/3RD-PARTY-NOTICES.txt /usr/share/doc/harmony-ap/
COPY packaging/ap/config/liquibase /opt/harmony-ap/liquibase
COPY packaging/ap/config/db /opt/harmony-ap/db
COPY packaging/ap/docker/clientauthentication.xml.template /opt/harmony-ap/setup/
COPY packaging/ap/docker/domibus.properties.template /opt/harmony-ap/setup/
COPY packaging/ap/docker/logback.xml /opt/harmony-ap/setup/
COPY packaging/ap/docker/logging.properties /opt/harmony-ap/conf/
COPY packaging/ap/docker/server.xml.template /opt/harmony-ap/setup/
COPY packaging/ap/docker/entrypoint.sh /opt/harmony-ap/bin/
COPY CHANGELOG.md /usr/share/doc/harmony-ap/

RUN mkdir /opt/harmony-ap/plugins/config \
 && chmod -R ugo=rX /opt/harmony-ap/ \
 && chmod a+rx /opt/harmony-ap/bin/*.sh

FROM ubuntu:22.04

RUN apt-get -qqy update \
 && apt-get -qqy upgrade \
 && apt-get -qqy install openjdk-11-jre-headless mysql-client tzdata crudini xmlstarlet sed grep tini \
 && apt-get autoremove \
 && apt-get clean

RUN groupadd --gid 999 harmony-ap \
 && useradd --system --uid 999 --gid 999 harmony-ap \
 && mkdir -p -m 750 \
  /var/opt/harmony-ap \
  /var/tmp/harmony-ap \
 && chown -R 999:999 \
  /var/opt/harmony-ap \
  /var/tmp/harmony-ap \
 && ln -sf /var/opt/harmony-ap/etc /etc/harmony-ap

COPY --from=tmp /opt/harmony-ap /opt/harmony-ap

ARG VERSION
ARG BUILD_ID
LABEL org.opencontainers.image.vendor="Nordic Institute for Interoperability Solutions (NIIS)"
LABEL org.opencontainers.image.title="Harmony eDelivery Access - Access Point"
LABEL org.opencontainers.image.ref.name=niis/harmony-ap
LABEL org.opencontainers.image.version="$VERSION"
LABEL org.opencontainers.image.source=https://github.com/nordic-institute/harmony-access-point
LABEL org.niis.harmony.build-id="$BUILD_ID"

USER  harmony-ap
WORKDIR /var/opt/harmony-ap
ENV HARMONY_VERSION=$VERSION
ENTRYPOINT ["/opt/harmony-ap/bin/entrypoint.sh"]
EXPOSE 8443

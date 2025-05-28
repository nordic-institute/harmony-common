FROM ubuntu:24.04 AS builder

ARG S6_OVERLAY_VERSION=3.2.0.2
#ARG S6_OVERLAY_ARCH="x86_64"
ARG S6_OVERLAY_ARCH="aarch64"

# Documentation
COPY packaging/ap/config/LICENSE.txt            /usr/share/doc/harmony-ap/
COPY packaging/ap/config/3RD-PARTY-NOTICES.txt  /usr/share/doc/harmony-ap/
COPY CHANGELOG.md                               /usr/share/doc/harmony-ap/

WORKDIR /opt/harmony-ap

# Copy files
COPY packaging/commonbin/jdbc-drivers/*.jar             lib/
COPY packaging/commonbin/harmony-ap                     webapps/ROOT/
COPY packaging/commonbin/ws-plugin.jar                  plugins/lib/
COPY packaging/commonbin/tomcat/bin/*.sh                bin/
COPY packaging/commonbin/tomcat/bin/*.jar               bin/
COPY packaging/commonbin/tomcat/lib                     lib/
COPY packaging/commonbin/tomcat/conf/web.xml            conf/
COPY packaging/commonbin/tomcat/conf/catalina.properties   conf/
COPY packaging/ap/config/activemq.xml                   internal/
COPY packaging/ap/config/*Policy*.xml                   policies/
COPY packaging/ap/config/signOnly.xml                   policies/
COPY packaging/ap/config/doNothingPolicy.xml            policies/
COPY packaging/ap/config/liquibase                      liquibase/
COPY packaging/ap/config/db                             db/
COPY packaging/ap/config/clientauthentication.xml.template  setup/
COPY packaging/ap/config/domibus.properties.template    setup/
COPY packaging/ap/docker/logback.xml                    setup/
COPY packaging/ap/docker/logging.properties             conf/
COPY packaging/ap/docker/server.xml.template            setup/

# Download s6-overlay
ADD https://github.com/just-containers/s6-overlay/releases/download/v${S6_OVERLAY_VERSION}/s6-overlay-noarch.tar.xz /tmp
ADD https://github.com/just-containers/s6-overlay/releases/download/v${S6_OVERLAY_VERSION}/s6-overlay-${S6_OVERLAY_ARCH}.tar.xz /tmp

RUN \
   command -v xz || apt-get update && apt-get install -y xz-utils && \
   mkdir -p \
    plugins/config \
    extensions/lib \
    /root-out && \
   chmod -R ugo=rX /opt/harmony-ap/ && \
   chmod a+rx /opt/harmony-ap/bin/*.sh && \
   for file in /tmp/s6-overlay-*.tar.xz ; do tar -C /root-out -Jxpf "$file" && rm -f "$file" ; done

FROM ubuntu:24.04

ARG VERSION
ARG BUILD_ID
ARG AP_UID="999"
ARG AP_GID="999"

ENV PS1="$(whoami)@$(hostname):$(pwd)\\$ " \
    DEBIAN_FRONTEND=noninteractive \
    HARMONY_VERSION=$VERSION \
    S6_VERBOSITY=1

LABEL org.opencontainers.image.vendor="Nordic Institute for Interoperability Solutions (NIIS)" \
      org.opencontainers.image.title="Harmony eDelivery Access - Access Point" \
      org.opencontainers.image.ref.name=niis/harmony-ap \
      org.opencontainers.image.version="$VERSION" \
      org.opencontainers.image.source="https://github.com/nordic-institute/harmony-access-point" \
      org.niis.harmony.build-id="$BUILD_ID"

RUN \
   echo "**** Install packages ****" && \
   apt-get update -qq && \
   apt-get upgrade -qqy && \
   apt-get install -qqy --no-install-recommends \
     crudini \
     curl \
     grep \
     mysql-client \
     openjdk-11-jre-headless \
     sed \
     tzdata \
     xmlstarlet && \
   echo "**** Create harmony-ap user and make harmony-ap folders ****" && \
   groupadd --gid "$AP_GID" harmony-ap && \
   useradd --system --uid "$AP_UID" --gid "$AP_GID" harmony-ap && \
   mkdir -p -m 750 \
    /var/opt/harmony-ap \
    /var/tmp/harmony-ap && \
   chown -R harmony-ap:harmony-ap \
    /var/opt/harmony-ap \
    /var/tmp/harmony-ap && \
   ln -sf /var/opt/harmony-ap/etc /etc/harmony-ap && \
   echo "**** Cleanup ****" && \
   userdel ubuntu && \
   apt-get autoremove -qqy && \
   apt-get clean -qqy && \
   rm -rf \
     /tmp/* \
     /var/lib/apt/lists/* \
     /var/log/* \
     /var/tmp/*

COPY --from=builder /root-out/ /
COPY packaging/ap/docker/root/ /
COPY --from=builder /opt/harmony-ap /opt/harmony-ap

USER harmony-ap

ENTRYPOINT ["/init"]

EXPOSE 8443
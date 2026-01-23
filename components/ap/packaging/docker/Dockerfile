FROM alpine:3.22 AS builder

ARG TARGETARCH

RUN set -eux; \
    apk add --no-cache xz

COPY vendor-archives/*.tar.xz /tmp/

RUN set -eux; \
    : "${TARGETARCH:?TARGETARCH must be set (buildx recommended)}"; \
    mkdir -p /output; \
    tar -xJf /tmp/s6-overlay-noarch.tar.xz -C /output; \
    tar -xJf "/tmp/s6-overlay-${TARGETARCH}.tar.xz" -C /output; \
    rm -rf /tmp/*

FROM ubuntu:24.04

ARG VERSION
ARG VCS_REVISION
ARG BUILD_NUMBER
ARG AP_UID="999"
ARG AP_GID="999"

ENV PS1="$(whoami)@$(hostname):$(pwd)\\$ " \
    DEBIAN_FRONTEND=noninteractive \
    HARMONY_VERSION=$VERSION \
    S6_VERBOSITY=1

LABEL org.opencontainers.image.vendor="Nordic Institute for Interoperability Solutions (NIIS)" \
      org.opencontainers.image.title="Harmony eDelivery Access - Access Point" \
      org.opencontainers.image.ref.name=niis/harmony-ap \
      org.opencontainers.image.source="https://github.com/nordic-institute/harmony-access-point" \
      org.opencontainers.image.version="$VERSION" \
      org.opencontainers.image.revision="${VCS_REVISION:-local}" \
      org.niis.harmony.build-number="${BUILD_NUMBER:-0}"

RUN set -eux; \
    apt-get update -qq; \
    apt-get install -qqy --no-install-recommends \
      crudini \
      curl \
      grep \
      jq \
      mysql-client \
      openjdk-11-jre-headless \
      sed \
      tzdata \
      xmlstarlet; \
    groupadd --gid "$AP_GID" harmony-ap; \
    useradd --system --uid "$AP_UID" --gid "$AP_GID" harmony-ap; \
    install -d -m 750 -o harmony-ap -g harmony-ap /var/opt/harmony-ap /var/tmp/harmony-ap; \
    rm -rf /var/lib/apt/lists/* /var/log/* /var/tmp/*

COPY --from=builder /output/ /
COPY --chown=harmony-ap:harmony-ap opt/ /opt/
COPY etc/ /etc/
COPY usr/ /usr/

RUN set -eux; \
    mkdir -p /run && \
    chown harmony-ap:harmony-ap /run

USER harmony-ap

ENTRYPOINT ["/usr/bin/harmony-entrypoint"]
CMD []

EXPOSE 8443

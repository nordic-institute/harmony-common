FROM ubuntu:24.04

LABEL maintainer="Nordic Institute for Interoperability Solutions <edelivery@niis.org>" \
      org.opencontainers.image.title="Harmony Debian Builder (Ubuntu 24.04)" \
      org.opencontainers.image.description="Builder for Harmony AP/SMP .deb packaging with debhelper, devscripts and fakeroot." \
      org.opencontainers.image.vendor="NIIS" \
      org.opencontainers.image.source="https://github.com/niis/harmony-common"

ARG TZ=UTC
ARG UNAME=builder

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    TZ=${TZ} \
    APT_LISTCHANGES_FRONTEND=none \
    DEBIAN_FRONTEND=noninteractive \
    HOME=/build

RUN apt-get update -qq \
 && apt-get install -qqy --no-install-recommends \
        build-essential \
        debhelper \
        devscripts \
        fakeroot \
        gnupg \
        gzip \
        libdistro-info-perl \
        tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && ln -snf "/usr/share/zoneinfo/${TZ}" /etc/localtime \
 && echo "${TZ}" > /etc/timezone \
 && useradd --system --shell /bin/bash "${UNAME}" \
 && mkdir /build \
 && chown "${UNAME}:${UNAME}" /build

USER "${UNAME}"
WORKDIR /build
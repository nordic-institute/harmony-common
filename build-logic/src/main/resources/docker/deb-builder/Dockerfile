FROM ubuntu:24.04

LABEL maintainer="Nordic Institute for Interoperability Solutions <edelivery@niis.org>" \
      org.opencontainers.image.title="Harmony Debian Builder (Ubuntu 24.04)" \
      org.opencontainers.image.description="Builder for Harmony AP/SMP .deb packaging with debhelper, devscripts and fakeroot." \
      org.opencontainers.image.vendor="NIIS" \
      org.opencontainers.image.source="https://github.com/niis/harmony-common"

ARG TZ=UTC
ARG UNAME=builder
ARG UID=1000
ARG GID=1000

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    TZ=${TZ} \
    APT_LISTCHANGES_FRONTEND=none \
    DEBIAN_FRONTEND=noninteractive

RUN apt-get -qq update \
 && apt-get -qqy install --no-install-recommends \
      build-essential \
      debhelper \
      devscripts \
      fakeroot \
      gnupg \
      gzip \
      libdistro-info-perl \
      tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && echo "${TZ}" > /etc/timezone \
 && ln -snf "/usr/share/zoneinfo/${TZ}" /etc/localtime \
 && groupadd -g "${GID}" "${UNAME}" 2>/dev/null \
    || groupmod -n "${UNAME}" "$(getent group "${GID}" | cut -d: -f1)" \
 && useradd -m -u "${UID}" -g "${GID}" "${UNAME}" 2>/dev/null \
    || usermod -l "${UNAME}" -d /home/"${UNAME}" -m "$(getent passwd "${UID}" | cut -d: -f1)" \
 && install -d -o "${UID}" -g "${GID}" /home/"${UNAME}"/.m2

USER ${UNAME}
WORKDIR /mnt

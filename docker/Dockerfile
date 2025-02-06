FROM ubuntu:24.04
LABEL maintainer="Nordic Institute for Interoperability Solutions <edelivery@niis.org>"

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get -qq update \
  && apt-get -qq upgrade \
  && apt-get -qq install --no-install-recommends \
    build-essential \
    debhelper \
    devscripts \
    fakeroot \
    libdistro-info-perl \
    openjdk-8-jdk-headless \
    unzip \
  && apt-get -qq autoremove \
  && apt-get -qq clean \
  && rm -rf /var/lib/apt/lists/*

ARG UNAME=builder
ARG UID
ARG GID

RUN groupadd -g "$GID" "$UNAME" 2>/dev/null || groupmod -n "$UNAME" $(getent group "$GID" | cut -d: -f1) \
  && useradd -m -u "$UID" -g "$GID" "$UNAME" 2>/dev/null || usermod -l "$UNAME" -d /home/"$UNAME" -m $(getent passwd "$UID" | cut -d: -f1) \
  && mkdir -p /home/"$UNAME"/.m2 && chown -R "$UNAME:$UNAME" /home/"$UNAME"/.m2 \
  && echo "export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))" >> /home/"$UNAME"/.bashrc

USER $UNAME
WORKDIR /mnt
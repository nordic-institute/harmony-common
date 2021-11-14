# Compiling Harmony eDelivery Access - SMP

## License <!-- omit in toc -->

This document is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
To view a copy of this license, visit <https://creativecommons.org/licenses/by-sa/4.0/>

## About

Harmony eDelivery Access SMP has been tested to run with the following configuration:

- Ubuntu 20.04
- OpenJDK 8 (1.8.0_292)
- Maven 3.8.1
- `ca-certificates-java` package (latest)
- curl (latest)
- git (latest)

Required dependencies can be installed locally using the `prepare_buildhost.sh` script following the instructions explained [here](../Build.md).

Build process is tested to succeed with at east 8GB of RAM.

If using Docker check if there is enough memory allocated for the virtual machine in which Docker runs and also for the container.

Run `docker stats` and check the `MEM USAGE / LIMIT` column's `LIMIT` value for this.

## Compile natively

Sample commands to build Harmony eDelivery Access SMP (note that we are using separate `neds-pom.xml` build tree):

```
mkdir /app
cd /app
git clone https://bitbucket.niis.org/scm/neds/harmony-smp.git
cd harmony-smp
git checkout master
mvn -f neds-pom.xml clean install
```

Integration tests can be skipped using `skipITs` property

```
mvn -f neds-pom.xml clean install -DskipITs=true
```

All tests can be skipped using `maven.test.skip` property

```
mvn -f neds-pom.xml clean install -Dmaven.test.skip=true
```

## Compile in Docker

First, build the [Docker image](../docker/Dockerfile-compile) that's used to compile the code: 

```
docker build -q -f {/PATH/TO}/harmony-common/docker/Dockerfile-compile -t harmony-compile --build-arg uid=$(id -u) --build-arg gid=$(id -g) .
```

Second, compile the code using the image:

```
docker run -it --rm \
    -u builder \
    -v "$(pwd)":/mnt \
    harmony-compile \
    mvn clean -f neds-pom.xml install
```

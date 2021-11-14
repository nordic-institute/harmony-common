# Compiling Harmony eDelivery Access - Access Point

Harmony eDelivery Access access point has been tested to run with the following configuration:

- Ubuntu 20.04
- OpenJDK 8 (1.8.0_292)
- Maven 3.8.1
- `ca-certificates-java` package (latest)
- curl (latest)
- git (latest)

Build process is tested to succeed with at least 8GB of RAM.

If using Docker check if there is enough memory allocated for the virtual machine in which Docker runs and also for the container.

Run `docker stats` and check the `MEM USAGE / LIMIT` column's `LIMIT` value for this.

Sample commands to build the Harmony eDelivery Access access point (note that we are using separate `neds-pom.xml` build tree):

```
mkdir /app
cd /app
git clone https://bitbucket.niis.org/scm/neds/harmony-access-point.git
cd harmony-access-point
git checkout master
mvn -f neds-pom.xml clean install -Ptomcat -Pdefault-plugins -Pdatabase -PUI 
```
Note that running the tests takes a long time (~20 min or more).

Integration tests can be skipped using `skipITs` property

```
mvn -f neds-pom.xml clean install -Ptomcat -Pdefault-plugins -Pdatabase -PUI -DskipITs=true
```

All tests can be skipped using `maven.test.skip` property

```
mvn -f neds-pom.xml clean install -Ptomcat -Pdefault-plugins -Pdatabase -PUI -Dmaven.test.skip=true
```
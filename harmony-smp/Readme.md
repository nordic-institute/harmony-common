eDelivery SMP has been tested to run with the following configuration:

Ubuntu 20.04
OpenJDK 8 (1.8.0_292)
Maven 3.8.1
MySQL (latest)
Ca-certificates-java package (latest)
Curl (latest)
Git (latest)

Build process is tested to succeed with at least 8GB of RAM.

If using Docker check if there is enough memory allocated for the virtual machine in which Docker runs and also for the container.

Run `docker stats` and check the `MEM USAGE / LIMIT` column's `LIMIT` value for this.

Sample commands to build Harmony SMP (note that we are using separate neds-pom.xml build tree):

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

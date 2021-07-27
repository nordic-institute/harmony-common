eDelivery SMP has been tested to run with the following configuration:

Ubuntu 20.04
OpenJDK 11
Maven 3.8.1
MySQL (latest)
Ca-certificates-java package (latest)
Curl (latest)
Git (latest)

Build process is tested to succeed with at least 8GB of RAM.

If using Docker check if there is enough memory allocated for the virtual machine in which Docker runs and also for the container.

Run `docker stats` and check the `MEM USAGE / LIMIT` column's `LIMIT` value for this.

Sample commands to build SMP without running the tests:

```
mkdir /app
cd /app
git clone https://ec.europa.eu/cefdigital/code/scm/edelivery/smp.git
cd smp
git checkout master
mvn clean install
```

Integration tests can be skipped using `skipITs` property

```
mvn clean install -DskipITs=true
```

All tests can be skipped using `maven.test.skip` property

```
mvn clean install -Dmaven.test.skip=true
```
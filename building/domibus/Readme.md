eDelivery Domibus has been tested to run with the following configuration:

Ubuntu 20.04
OpenJDK 11
Maven 3.8.1
Ca-certificates-java package (latest)
Curl (latest)
Git (latest)

Build process is tested to succeed with at least 8GB of RAM.

If using Docker check if there is enough memory allocated for the virtual machine in which Docker runs and also for the container.

Run `docker stats` and check the `MEM USAGE / LIMIT` column's `LIMIT` value for this.



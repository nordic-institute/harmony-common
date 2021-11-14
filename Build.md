# Building Harmony eDelivery Access

Running the Harmony eDelivery Access software requires Ubuntu 20.04. As a development environment, only Ubuntu 20.04 is currently supported. Alternatively the software can be built entirely inside Docker containers (see below) making the build host distribution agnostic but also a bit slower. If you are using some other operating system (e.g. Windows or macOS), the easiest option is to first install Ubuntu into a virtual machine.

**Tools**

*Required for deb packaging and/or building in Docker*
* Docker

*Required for building natively (without Docker)*
* OpenJDK / JDK version 8
* Maven
* GCC

**Prerequisites**

* Checkout `harmony-common`, `harmony-access-point` and `harmony-smp` repositories in the same base directory.
* The directory structure should look like this:

    ```
    - <BASE_DIR>
     |-- harmony-access-point
     |-- harmony-common
     |-- harmony-smp
    ```
* The build script assumes the above directory structure regardless of the build type (native / Docker).

## Dependency installation and instructions for building in Docker

* Install Docker

* Build the software and installation packages:

    `./build_packages.sh -d`
    
* Tests can be skipped by adding `-s` to the command:

  `./build_packages.sh -d -s`

## Dependency installation and instructions for building natively (without Docker)

* Requires Ubuntu 20.04.

* Execute the following command once to install the required dependencies on a clean building host. The script is supposed to be run as the user who will build the source. The script will ask for user password (using sudo) for installing some new packages as well.

    `./prepare_buildhost.sh`

* Build the software and installation packages:

    `./build_packages.sh`

* Tests can be skipped by adding `-s` to the command:

  `./build_packages.sh -s`
  
Once you have successfully built the software, please see the installation instructions.

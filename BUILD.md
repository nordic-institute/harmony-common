# Building Harmony eDelivery Access

## License

This document is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
To view a copy of this license, visit <https://creativecommons.org/licenses/by-sa/4.0/>

## About this guide

This guide provides instructions for building Harmony eDelivery Access from source. The build process can produce two types of artifacts:

1. **Debian packages** (`.deb`) for installing Access Point and SMP on Ubuntu 22.04, and 24.04.
2. A multi-platform **Docker image** for deploying the Access Point.

## Prerequisites

### Tools

- **Docker:** Creates a consistent environment for generating both the Debian packages and the final Docker image, making the build process independent of your host system. Docker Desktop or Engine (version 20.10 or newer) must be installed.
- **For native builds only:**
  - An Ubuntu host (22.04 or 24.04).
  - OpenJDK / JDK (version 8 or compatible).
  - Maven.
  - Standard build tools like `gcc`.

### Directory structure

Check out the [harmony-common](https://github.com/nordic-institute/harmony-common), [harmony-access-point](https://github.com/nordic-institute/harmony-access-point), and [harmony-smp](https://github.com/nordic-institute/harmony-smp) repositories into the same base directory. The build scripts rely on the following folder structure:

```
- <BASE_DIR>
 |-- harmony-access-point
 |-- harmony-common
 |-- harmony-smp
```

## Step 1: Compile the Source Code and Build Debian Packages

This process compiles the source code for both Access Point and SMP and packages them into `.deb` files for Ubuntu.

### Option A: Build inside a Docker container

This host-agnostic method uses a Docker container to compile the code and build the packages.

1. Navigate to the `harmony-common` directory.
2. Run the build script with the `-d` flag:
    ```bash
    ./build_packages.sh -d
    ```
3. To speed up the build by skipping the test execution, add the `-s` flag:
    ```bash
    ./build_packages.sh -d -s
    ```
   
### Option B: Build natively on an Ubuntu Host

This method compiles the code directly on the host system.

1. Ensure you are on a supported Ubuntu version.
2. Navigate to the `harmony-common` directory.
3. Run the preparation script once to install all necessary dependencies on your machine. The script will ask for your `sudo` password to install packages.
    ```bash 
    ./prepare_buildhost.sh
    ```
4. Run the build script:
    ```bash
    ./build_packages.sh
    ```
5. To skip tests, use the `-s` flag:
    ```bash
    ./build_packages.sh -s
    ```

Once the script finishes, the compiled Java artifacts (`.war` files) will be in the `target` directories of their respective projects, and the generated `.deb` packages will be located in the `harmony-common/packaging/build/` directory, organized by Ubuntu version (`ubuntu2X.04`).

## Step 2: Build the Docker Image (Optional)

After completing Step 1, the necessary Java artifacts are ready to be packaged into a Docker image. This image is multi-platform (`linux/amd64` and `linux/arm64`).

1. Ensure you are in the `harmony-common` directory.
2. Run the Docker build script located in the `packaging` folder:
    ```bash
    ./packaging/build-docker.sh
    ```
3. The script will automatically assign a tag to the image based on the version defined in `_build_common.sh`. You can specify a custom tag using the `-t` flag:
    ```bash
    ./packaging/build-docker.sh -t <custom-tag>
    ```

After the build is complete, the new Docker image will be available in your local Docker image repository. You can check for it by running `docker images`.

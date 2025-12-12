# Building Harmony eDelivery Access

## License

This document is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
To view a copy of this license, visit <https://creativecommons.org/licenses/by-sa/4.0/>

## Overview

Harmony Common provides the Gradle build system that produces deployable artifacts for **Harmony Access Point (AP)** and **Harmony Service Metadata Publisher (SMP)** components. The build compiles upstream Maven projects, assembles file trees based on manifest definitions, and outputs Debian packages and Docker images.

This guide covers how to build these artifacts locally and configure the build for different scenarios.

## Prerequisites

| Tool                      | Why it is needed                        | Notes                                          |
|---------------------------|-----------------------------------------|------------------------------------------------|
| Git 2.30+                 | Clone and manage Harmony repositories   | -                                              |
| JDK 17+                   | Run Gradle wrapper and build-logic      | Set `JAVA_HOME` if not using system default    |
| Docker 20.10+ with Buildx | Build Docker images and Debian packages | Run `docker buildx create --use` once per host |
| GnuPG (optional)          | Sign Debian packages                    | Only needed if you plan to sign `.deb` files   |

## Repository Layout

Clone the Harmony repositories into a shared parent directory:

```
<BASE_DIR>/
 ├─ harmony-access-point/    # Maven project for AP
 ├─ harmony-common/          # Gradle build orchestrator (this repo)
 └─ harmony-smp/             # Maven project for SMP
```

**Important**: Run all build commands from the `harmony-common/` directory.

## Quick Start

### Build Debian packages for all distributions

```bash
./gradlew buildDebAp buildDebSmp
```

Outputs: `build/deb/ap/<version>/<distro>/` and `build/deb/smp/<version>/<distro>/`

### Build Docker image

```bash
./gradlew buildDockerAp
```

Image is loaded into your local Docker daemon, tagged with component version (e.g., `niis/harmony-ap:1.0.0`).

### Build without running tests

```bash
./gradlew buildDebAp -Pharmony.compile.skipTests=true
```

### Clean build outputs

```bash
./gradlew clean
```

## Build Tasks Reference

The build system generates tasks dynamically based on components and distributions. Here are the main tasks:

### Compilation Tasks
- `compileJavaAp` - Compile the Access Point using Maven
- `compileJavaSmp` - Compile the Service Metadata Publisher using Maven

### Debian Package Tasks
- `buildDebAp` - Build Debian packages for all configured distributions (default: jammy, noble)
- `buildDebSmp` - Build SMP Debian packages for all distributions
- `buildDebAp<Distro>` - Build for specific distribution (e.g., `buildDebApJammy`)
- `buildDebSmp<Distro>` - Build SMP for specific distribution

### Docker Image Tasks
- `buildDockerAp` - Build Access Point Docker image

### Staging Tasks
These run automatically as dependencies but can be invoked directly:
- `assembleStagingApDocker` - Prepare Docker build context for AP
- `assembleStagingApDeb<Distro>` - Prepare Debian build tree for AP
- Similar tasks exist for SMP

### Cleanup Tasks
- `clean` - Remove `build/` directory
- `cleanAll` - Comprehensive cleanup (build outputs, Maven projects)
- `cleanAp` / `cleanSmp` - Component-specific cleanup
- `cleanDebArtifactsAp` / `cleanDebArtifactsSmp` - Remove built `.deb` files only
- `cleanStagingApDocker` - Remove Docker staging directory

## Configuration

### How Configuration Works

The build system uses a **three-level property fallback system**:

1. **Component-specific**: `harmony.<component>.<property>` (e.g., `harmony.ap.compile.skipTests`)
2. **Global fallback**: `harmony.<property>` (e.g., `harmony.compile.skipTests`)
3. **Built-in default**: Hard-coded defaults in the build logic

This means you can set global defaults and override them per-component when needed.

**Example**: Setting `harmony.compile.skipTests=true` skips tests for all components, but `harmony.ap.compile.skipTests=false` would override this for the Access Point specifically.

### Setting Properties

Properties can be set in three ways:

1. **In `gradle.properties`** (project-level):
   ```properties
   harmony.ap.version=1.0.0
   harmony.compile.skipTests=true
   ```

2. **In `~/.gradle/gradle.properties`** (user-level):
   ```properties
   harmony.deb.builder.image=my-custom-builder
   ```

3. **On command line** (one-time override):
   ```bash
   ./gradlew buildDebAp -Pharmony.ap.version=1.1.1-SNAPSHOT
   ```

### Essential Properties

#### Component Versions
```properties
harmony.ap.version=X.Y.Z          # Access Point version
harmony.smp.version=X.Y.Z        # SMP version
```

#### Maven Compilation
```properties
# Global settings (apply to all components unless overridden)
harmony.compile.skipTests=false
harmony.compile.maven.goals=clean,package
harmony.compile.maven.profiles=
harmony.compile.javaVersion=8

# Component-specific overrides
harmony.ap.compile.skipTests=true
harmony.ap.compile.maven.profiles=tomcat,production
harmony.ap.compile.javaVersion=11
```

#### Debian Packages
```properties
# Global distro list (default: jammy,noble)
harmony.deb.distros=jammy,noble

# Component-specific distros
harmony.ap.deb.distros=jammy
harmony.smp.deb.distros=noble

# Debian builder image
harmony.deb.builder.image=artifactory.niis.org/harmony-release-docker/niis/harmony-deb-builder
harmony.deb.builder.tag=1.0.0
```

#### Docker Images
```properties
# Image naming
harmony.ap.docker.imageName=niis/harmony-ap
harmony.smp.docker.imageName=my-registry/harmony-smp

# Tags (comma-separated, defaults to component version)
harmony.ap.docker.tags=X.Y.Z,latest

# Multi-platform builds (comma-separated, empty = host architecture)
harmony.docker.platforms=linux/amd64,linux/arm64

# Docker output mode: load | push | tar | oci-dir | oci-tar
harmony.ap.docker.outputMode=load
```

#### Vendor Dependencies
The build downloads third-party dependencies like Tomcat, Liquibase, and JDBC drivers based on version properties:
```properties
harmony.vendor.tomcat.version=...
harmony.vendor.liquibase.version=...
harmony.vendor.mysqlj.version=...
harmony.vendor.mariadbj.version=...
harmony.vendor.s6overlay.version=...
```

Additional metadata properties exist for each vendor (group coordinates, classifiers, extensions) - see `gradle.properties` for the complete configuration.

**Note**: When changing vendor versions, you must update dependency verification metadata:
```bash
./gradlew --write-verification-metadata sha256 \
  assembleStagingApDocker assembleStagingSmpDocker \
  -Pharmony.compile.skipTests=true
```
This regenerates `gradle/verification-metadata.xml` with checksums for the new versions. The staging tasks download all dependencies including build-logic dependencies and scope-specific vendor dependencies like `s6-overlay` (docker-only).

### Complete Property Reference

Properties are organized by category. **Global or per-component** means you can set `harmony.<property>` as a default and override with `harmony.<component>.<property>` for specific components.

#### Global Configuration

| Property                    | Description                               | Default                                                                |
|-----------------------------|-------------------------------------------|------------------------------------------------------------------------|
| `harmony.components`        | Components to build (CSV)                 | `ap,smp`                                                               |
| `harmony.exec.docker`       | Docker CLI path                           | `docker`                                                               |
| `harmony.exec.git`          | Git CLI path                              | `git`                                                                  |
| `harmony.exec.timeoutSec`   | External command timeout (seconds)        | `120`                                                                  |

#### Component Version (Required, Per-Component)

| Property                      | Description                                                      |
|-------------------------------|------------------------------------------------------------------|
| `harmony.<component>.version` | **Mandatory**. Version string used for artifacts and Docker tags |

#### Maven Compilation (Global or Per-Component)

| Property                                       | Description                                           | Default         |
|------------------------------------------------|-------------------------------------------------------|-----------------|
| `harmony.compile.skipTests`                    | Skip Maven tests                                      | `false`         |
| `harmony.compile.maven.goals`                  | Maven goals to execute (CSV)                          | `clean,package` |
| `harmony.compile.maven.profiles`               | Maven profiles to activate (CSV)                      | (empty)         |
| `harmony.compile.javaVersion`                  | Java toolchain version                                | `8`             |
| `harmony.<component>.compile.repo`             | **Required**. Path to Maven project                   | (none)          |
| `harmony.<component>.compile.artifact.<alias>` | Per-component only. Path template for build artifacts | (none)          |

#### Debian Packaging (Global or Per-Component)

| Property                         | Description                                                       | Default                                                                |
|----------------------------------|-------------------------------------------------------------------|------------------------------------------------------------------------|
| `harmony.deb.distros`            | Ubuntu/Debian distributions to build (CSV)                        | `jammy,noble`                                                          |
| `harmony.deb.sign`               | Sign packages with GPG                                            | `false`                                                                |
| `harmony.deb.keyId`              | GPG key ID for signing (**required if `sign=true`**)              | (none)                                                                 |
| `harmony.deb.gpgHome`            | Path to GnuPG home directory                                      | `$GNUPGHOME` or `~/.gnupg`                                             |
| `harmony.deb.packageName`        | Debian package name                                               | `harmony-<component>`                                                  |
| `harmony.deb.builder.image`      | Docker image for building .deb packages                           | `artifactory.niis.org/harmony-release-docker/niis/harmony-deb-builder` |
| `harmony.deb.builder.tag`        | Tag of the deb builder image                                      | `1.0.0`                                                                |
| `harmony.deb.builder.pullPolicy` | When to pull the builder image: `always`, `ifNotPresent`, `never` | `ifNotPresent`                                                         |

#### Docker Images (Global or Per-Component)

| Property                           | Description                                                               | Default                     |
|------------------------------------|---------------------------------------------------------------------------|-----------------------------|
| `harmony.docker.imageName`         | Docker image repository                                                   | `niis/harmony-<component>`  |
| `harmony.docker.tags`              | Image tags (CSV)                                                          | Component version           |
| `harmony.docker.platforms`         | Build platforms (CSV)                                                     | (empty = host architecture) |
| `harmony.docker.outputMode`        | Output destination: `load`, `push`, `tar`, `oci-dir`, or `oci-tar`        | `load`                      |
| `harmony.docker.pullPolicy`        | When to pull base images in Dockerfile: `always`, `ifNotPresent`, `never` | `ifNotPresent`              |
| `harmony.docker.provenanceEnabled` | Enable provenance attestation generation                                  | `false`                     |
| `harmony.docker.trackBase`         | Track base image digests for reproducibility                              | `true`                      |
| `harmony.docker.baseImageDigests`  | Pre-computed base image digests (JSON)                                    | (auto-resolved)             |

##### Pull Policies

The `pullPolicy` property controls when Docker images are pulled:

| Policy         | Behavior                                                                     |
|----------------|------------------------------------------------------------------------------|
| `always`       | Always pull before build. Fails if pull fails. Recommended for CI.           |
| `ifNotPresent` | Pull only if image doesn't exist locally. Recommended for local development. |
| `never`        | Never pull. Fails if image doesn't exist locally.                            |

#### Build Reproducibility (Global or Per-Component)

| Property                 | Description                 | Default                                                   |
|--------------------------|-----------------------------|-----------------------------------------------------------|
| `harmony.build.epoch`    | SOURCE_DATE_EPOCH timestamp | Maximum git commit timestamp across involved repositories |
| `harmony.build.revision` | VCS revision hash           | Git HEAD commit from component repository                 |
| `harmony.build.number`   | Build number                | `0`                                                       |

**Note**: If `harmony.build.epoch` or `harmony.build.revision` are not explicitly set and cannot be determined from git, the build will fail.

#### Vendor Dependencies (Global or Per-Component)

These properties configure external dependencies like Tomcat, Liquibase, and JDBC drivers.

| Property Pattern                                | Description                                                | Default        |
|-------------------------------------------------|------------------------------------------------------------|----------------|
| `harmony.vendor.<name>.version`                 | **Required**. Dependency version                           | (none)         |
| `harmony.vendor.<name>.group`                   | Maven group coordinate                                     | `<name>`       |
| `harmony.vendor.<name>.name`                    | Maven artifact name                                        | `<name>`       |
| `harmony.vendor.<name>.extension`               | File extension                                             | (none)         |
| `harmony.vendor.<name>.classifier.<classifier>` | Classifier mapping (e.g., for platform-specific artifacts) | `<classifier>` |

**Example vendor configurations** (see `gradle.properties` for complete list):
- `harmony.vendor.tomcat.version`
- `harmony.vendor.liquibase.version`
- `harmony.vendor.mysqlj.version` (MySQL JDBC)
- `harmony.vendor.mariadbj.version` (MariaDB JDBC)
- `harmony.vendor.s6overlay.version`

## Common Scenarios

### Build for a single distribution

Instead of building for all distros (jammy, noble), build only what you need:

```bash
./gradlew buildDebApJammy
```

### Skip tests for faster builds

```bash
./gradlew buildDebAp buildDockerAp -Pharmony.compile.skipTests=true
```

### Force rebuild without cache

Use `--rerun-tasks` to ignore up-to-date checks and `--no-build-cache` to disable the build cache:

```bash
./gradlew buildDockerAp --rerun-tasks --no-build-cache
```

### Build with custom version

```bash
./gradlew buildDockerAp -Pharmony.ap.version=1.0.0-dev
```

The resulting image will be tagged as `niis/harmony-ap:1.0.0-dev`.

### Build with custom Docker tags

By default, the image is tagged with the component version. You can override or add additional tags:

```bash
./gradlew buildDockerAp -Pharmony.ap.docker.tags=latest,dev,feature-xyz
```

This creates: `niis/harmony-ap:latest`, `niis/harmony-ap:dev`, `niis/harmony-ap:feature-xyz` (replaces the version tag).

### Use custom Maven profiles

```bash
./gradlew buildDebAp -Pharmony.ap.compile.maven.profiles=tomcat,custom-profile
```

### Build only specific components

To work on just the Access Point:

```bash
./gradlew compileJavaAp assembleStagingApDocker buildDockerAp
```

### Clean and rebuild

```bash
./gradlew clean buildDebAp buildDockerAp
```

## Advanced Topics

### Multi-Platform Docker Builds

To build images for multiple architectures (e.g., for ARM servers or Apple Silicon):

```bash
./gradlew buildDockerAp \
  -Pharmony.ap.docker.platforms=linux/amd64,linux/arm64 \
  -Pharmony.ap.docker.outputMode=push
```

**Docker Output Modes:**

The `outputMode` property controls how the built image is handled:

- **`load`** (default): Load image into local Docker daemon
  - Fast for local development
  - Supports multi-platform with containerd image store (enabled by default in Docker Desktop; enable with `"features": {"containerd-snapshotter": true}` in `/etc/docker/daemon.json` on Linux)
  - Requires Docker daemon running locally

- **`push`**: Push image to registry
  - Supports multi-platform builds with manifest list
  - Requires registry credentials configured (`docker login`)
  - Image available for other systems to pull

- **`tar`**: Export image filesystem as tarball to `build/docker/<component>/<version>/image.tar`
  - Useful for extracting filesystem contents
  - Exports only the filesystem layers without OCI manifest list, losing multi-architecture support
  - Cannot be loaded with `docker load` as a complete image

- **`oci-dir`**: Export image in OCI format as directory to `build/docker/<component>/<version>/image-oci/`
  - Standard OCI image layout (directory structure)
  - Preserves multi-platform manifest list
  - Can be imported by OCI-compatible tools (e.g., `skopeo`, `crane`)
  - Cannot be loaded directly with `docker load`

- **`oci-tar`**: Export image in OCI format as tarball to `build/docker/<component>/<version>/image-oci.tar`
  - Preserves the OCI manifest list, maintaining multi-architecture support
  - Load later with: `docker load < build/docker/ap/1.0.0/image-oci.tar`
  - Requires containerd image store for multi-platform `docker load` support

**CI Pipeline Example:**

```bash
# Build job: Generate cached OCI image
./gradlew buildDockerAp -Pharmony.ap.docker.outputMode=oci-tar

# Test job (different agent): Load from Gradle cache
docker load < build/docker/ap/1.0.0/image-oci.tar
docker run --rm niis/harmony-ap:1.0.0 /test-script.sh

# Publish job: Push to registry
./gradlew buildDockerAp -Pharmony.ap.docker.outputMode=push
```

**Important notes:**
- Ensure Docker Buildx is properly configured: `docker buildx create --use`
- You must be authenticated to the target registry when pushing
- For `docker load` with multi-platform OCI images, containerd image store must be enabled:
  - Docker Desktop: Enabled by default
  - Docker Engine 29+: Enabled by default on new installations
  - Docker Engine < 29: Add `"features": {"containerd-snapshotter": true}` to `/etc/docker/daemon.json`

### Signing Debian Packages

To sign `.deb` packages with GPG:

1. Ensure your GPG key is available:
   ```bash
   gpg --list-secret-keys
   ```

2. Configure signing properties:
   ```properties
   harmony.ap.deb.sign=true
   harmony.ap.deb.keyId=YOUR_KEY_ID
   ```

3. Optionally specify a custom GPG home:
   ```properties
   harmony.ap.deb.gpgHome=/path/to/.gnupg
   ```

4. Build:
   ```bash
   ./gradlew buildDebAp
   ```

**Security note**: The GPG home directory is copied into a local Docker build container (not transmitted over the network). The container is temporary and removed after the build completes. Your private key remains on your local machine and is never pushed to any remote system.

### Custom Debian Builder Image

> **Image availability**: Builder images follow the lifecycle of the Harmony versions they support. Images for end-of-life versions may be removed after support ends. If you need long-term access to a specific builder image, consider mirroring it to your own registry.

If you maintain a custom builder image with specific tooling:

```bash
./gradlew buildDebAp \
  -Pharmony.deb.builder.image=my-registry/custom-builder \
  -Pharmony.deb.builder.tag=1.2.3
```

#### Using Local Builder Images

The build automatically attempts to pull the configured builder image but **will continue using a local copy if the pull fails**. This allows you to use custom local builder images without pushing to a registry.

**To use a local builder image**:

1. Build or tag your local image:
   ```bash
   docker build -t my-custom-builder:1.0 .
   # or
   docker tag existing-image:tag my-custom-builder:1.0
   ```

2. Configure the build to use it:
   ```bash
   ./gradlew buildDebAp \
     -Pharmony.deb.builder.image=my-custom-builder \
     -Pharmony.deb.builder.tag=1.0
   ```

The build will attempt to pull the image, show a warning if it fails, then proceed with your local image.

### Reproducible Builds

The build system implements reproducible builds by default:

- **Source date epoch**: Derived from the maximum git commit timestamp across the component repository and harmony-common, ensuring changes in either repository result in a new epoch
- **VCS revision**: Taken from the component repository's HEAD commit
- **Build number**: Build ID tracked in output markers
- **Base image digest tracking**: Immutable references to base images for traceability

**Important**: Both `Source date epoch` and `VCS revision` require valid git repositories. If git is unavailable or the repositories are not valid git repositories, the build will fail. You can override these values explicitly:

```properties
harmony.ap.build.epoch=1234567890
harmony.ap.build.revision=abc123
harmony.ap.build.number=456
```

#### Base Image Digest Tracking

When `harmony.docker.trackBase=true` (default), the build resolves and records the immutable digest of each base image referenced in the Dockerfile. This provides:

- **Traceability**: Know exactly which base image version was used
- **Security monitoring**: Detect when base images have been updated with security patches
- **Reproducibility**: Rebuild with the same base image by pinning digests

The digests are stored in the build marker file (`build/metadata/docker/<component>/<version>.json`) and follow the [OCI Image Spec](https://github.com/opencontainers/image-spec/blob/main/descriptor.md) format, supporting multiple algorithms (sha256, sha512, blake3).

**Example marker output:**
```json
{
  "baseImageDigests": {
    "ubuntu:24.04": "sha256:abc123...",
    "scratch": null
  }
}
```

Note: The special `scratch` image (empty base) has no digest and is recorded as `null`.

**CI usage**: In CI pipelines, you can pre-compute digests and pass them to avoid repeated registry lookups:

```bash
./gradlew buildDockerAp \
  -Pharmony.docker.baseImageDigests='{"ubuntu:24.04":"sha256:abc...","scratch":null}'
```

### CI/CD Integration

#### Basic CI Pipeline Command

```bash
./gradlew --no-daemon \
  buildDebAp buildDebSmp buildDockerAp buildDockerSmp \
  -Pharmony.compile.skipTests=false
```

#### CI Pipeline with Remote Build Cache

To leverage remote build cache in CI for faster rebuilds:

```bash
./gradlew --no-daemon --build-cache \
  buildDebAp buildDockerAp \
  -Pharmony.build.number=${BUILD_NUMBER} \
  -Pharmony.ap.version=1.0.0 \
  -Pharmony.cache.url=https://artifactory.example.com/cache-repo/ \
  -Pharmony.cache.component=ap \
  -Pharmony.cache.version=1.0.0 \
  -Pharmony.cache.discriminator=${BUILD_NUMBER} \
  -Pharmony.cache.push=true \
  -Pharmony.cache.username=${CACHE_USER} \
  -Pharmony.cache.password=${CACHE_PASSWORD}
```

**Cache property explanation:**
- `harmony.build.number` - Identifies this specific build (embedded in artifacts)
- `harmony.cache.component` - First path segment: `<url>/<component>/...`
- `harmony.cache.version` - Second path segment: `<url>/<component>/<version>/...`
- `harmony.cache.discriminator` - Discriminates between builds: `<url>/<component>/<version>/<discriminator>/`
- `harmony.cache.push=true` - Uploads build results to cache after successful build
- `harmony.cache.restoreOnly=true` - (Optional) Fails if cache miss, useful for publish jobs

**Example cache paths:**
- Without discriminator: `https://artifactory.example.com/cache-repo/ap/1.0.0/`
- With discriminator=123: `https://artifactory.example.com/cache-repo/ap/1.0.0/123/`

**CI environment considerations:**
- Use `--no-daemon` to avoid leaving background processes
- Use `--build-cache` to enable remote caching
- Authenticate to Docker registries before building if pushing images
- Set `harmony.exec.docker=docker` explicitly if needed
- Archive outputs from `build/deb/` and `build/metadata/` for provenance
- Use same `harmony.cache.discriminator` across jobs to share build artifacts via cache

### Customizing Component Repositories

If your Maven projects are in non-standard locations:

```properties
harmony.ap.compile.repo=/custom/path/to/access-point
harmony.smp.compile.repo=/custom/path/to/smp
```

## Troubleshooting

### Docker Buildx not found or not initialized

**Error**: `docker: 'buildx' is not a docker command`

**Solution**: Initialize Buildx:
```bash
docker buildx create --use
```

Or upgrade to a Docker version that includes Buildx (Docker 19.03+).

### Manifest alias not found

**Error**: `Unknown artifact alias 'xyz' in manifest`

**Cause**: The manifest references an artifact that wasn't produced during compilation.

**Solution**: Check `components/<component>/manifest.yml` and verify:
- Artifact aliases match those defined in `gradle.properties` under `harmony.<component>.compile.artifact.*`
- The compilation step succeeded: `./gradlew compileJava<Component> --info`

### Vendor download failures

**Error**: `Could not resolve harmony.vendor.<name>` or `Dependency verification failed`

**Causes**:
1. Missing or incorrect vendor version configuration
2. Checksum mismatch in `gradle/verification-metadata.xml`
3. Network connectivity issues

**Solutions**:

1. Verify vendor properties in `gradle.properties`:
   ```properties
   harmony.vendor.<name>.version=X.Y.Z
   ```

2. If you changed vendor versions, update verification metadata:
   ```bash
   ./gradlew --write-verification-metadata sha256 \
     assembleStagingApDocker assembleStagingSmpDocker \
     -Pharmony.compile.skipTests=true
   ```

3. Check network access to dependency repositories (Maven Central, GitHub releases, Apache archives)

4. If verification keeps failing, you can temporarily disable it (not recommended for production):
   ```bash
   ./gradlew buildDebAp --no-dependency-verification
   ```

### Permission denied on build directories

**Error**: `Permission denied` when writing to `build/staging/` or `build/deb/`

**Cause**: Docker mounts may have incorrect permissions, especially on Linux.

**Solution**:
- Add your user to the `docker` group: `sudo usermod -aG docker $USER` (requires logout)
- Ensure the project directory has correct ownership
- On Linux, consider using Docker rootless mode

### Out of date task outputs

**Symptom**: Tasks show `UP-TO-DATE` but you expect them to re-run.

**Solution**: Force re-execution:
```bash
./gradlew <task> --rerun-tasks --no-build-cache
```

Or clean first:
```bash
./gradlew clean <task>
```

### Maven compilation fails

**Error**: Maven build fails during `compileJava<Component>` task

**Debug steps**:
1. Check the Maven project builds independently:
   ```bash
   cd ../harmony-access-point
   ./mvnw clean package
   ```

2. Verify Java version compatibility:
   ```properties
   harmony.compile.javaVersion=11
   ```

3. Check Maven profiles are correct:
   ```bash
   ./gradlew compileJavaAp --info
   ```

4. Review full output with `--stacktrace`:
   ```bash
   ./gradlew compileJavaAp --info --stacktrace
   ```

### Docker build runs out of space

**Error**: `no space left on device`

**Solution**: Clean up Docker resources:
```bash
docker system prune -a
docker builder prune -a
```

Also clean Gradle build directory:
```bash
./gradlew cleanAll
```

---

## Need Help?

- **Build issues**: Run with `--info --stacktrace` for detailed output
- **Task dependencies**: Use `./gradlew <task> --dry-run` to see execution plan
- **Available tasks**: Run `./gradlew tasks --all` to list all tasks
- **Property values**: Check resolved configuration with `./gradlew properties`

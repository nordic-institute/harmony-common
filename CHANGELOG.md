# Changelog

## Access Point 2.2.2 - 2023-12-07

- NEDS-156: Fix constraint in AP database migrations
- NEDS-159: Fix security assessment finding 5.2.1
- NEDS-163: Merge Domibus 5.1.1 into Harmony Access Point
- NEDS-166: Release Harmony Access Point version 2.2.2

## SMP - 2.1.1 - 2023-11-24

- NEDS-164: Merge DomiSMP 5.0.1 into Harmony SMP code base

## Access Point 2.2.1 - 2023-11-14

- NEDS-157: Update dependencies with known vulnerabilities

## Access Point 2.2.0 - 2023-10-11

- NEDS-110: Add support for running the Harmony Access Point in a Docker container.
- NEDS-150: Change the Access Point default keystore type from JKS to PKCS12.
- NEDS-151: Add support for defining the TLS certificate and security (sign + encryption) certificate Distinguished Name (DN) field values separately during the installation process.
- NEDS-153: Update third party dependencies.

## Access Point and SMP - 2.1.0 - 2023-08-17

- NEDS-133: Access Point: Support Ubuntu 22.04
- NEDS-134: SMP: Support Ubuntu 22.04

## SMP - 2.0.0 - 2023-08-09

- NEDS-127: Merge DomiSMP 5.0 into Harmony SMP code base.
- NEDS-128: Run the SMP on Java 11.
- NEDS-129: Support using a remote database.
- NEDS-130: Support configuring the database schema name.
- NEDS-132: Resolve issues that were detected during eDelivery conformance tests.

## Access Point - 2.0.0 - 2023-06-05

- NEDS-100: Add support for defining message attachment name using the `businessContentAttachment` property.
- NEDS-107: Improve remote database support.
- NEDS-108: Make database schema name configurable.
- NEDS-111: Merge changes from Domibus 5.1 into Harmony Access Point.
- NEDS-126: Fix incorrect support URL in the Access Point UI.

## Access Point - 1.4.0 - 2023-02-08

- NEDS-104: Implement database migrations using Liquibase for Harmony Access Point.
- NEDS-105: Update the Harmony Access Point 3rd party dependencies.

## SMP - 1.4.0 - 2023-01-23

- NEDS-101: Merge changes from SMP 4.2 into Harmony SMP. [More information](https://ec.europa.eu/digital-building-blocks/wikis/display/DIGITAL/SMP+-+v4.2).
- NEDS-101: Update Harmony SMP properties. [More information](https://ec.europa.eu/digital-building-blocks/code/projects/EDELIVERY/repos/smp/browse/changelog.txt?at=refs%2Ftags%2F4.2).
- NEDS-101: Divide Access Point and SMP debian build jobs.
- NEDS-101: Implement database migrations using Liquibase for Harmony SMP.
- NEDS-101: Replace Log4j with Logback.
- NEDS-101: Update the Harmony SMP 3rd party dependencies.

## Access Point and SMP - 1.3.1 - 2022-11-15

- NEDS-98: Update the `commons-text` dependency.

## Access Point and SMP - 1.3.0 - 2022-10-22

- NEDS-96: Add support for OpenJDK 11 for the Harmony Access Point.
- NEDS-97: Update the Harmony Access Point 3rd party dependencies.

## Access Point and SMP - 1.2.0 - 2022-05-10

- NEDS-62: Enable the use of Peppol IDs in the Harmony Access Point Plugin User configuration.
- NEDS-86: Make the Harmony Access Point HTTPS port configurable during the installation process.
- NEDS-87: Make the Harmony SMP HTTPS port configurable during the installation process.
- NEDS-90: Improve the Harmony Access Point's Peppol AS4 specification.
- NEDS-91: Update the Harmony Access Point 3rd party dependencies.
- NEDS-93: Update the Harmony SMP 3rd party dependencies.

## Access Point and SMP - 1.1.0 - 2022-02-17

- NEDS-24: Add support for upgrading Access Point using package manager.
- NEDS-25: Add support for upgrading SMP using package manager.
- NEDS-33: Conduct European Commission eDelivery AS4 and SMP conformance testing for Access Point and SMP.
- NEDS-71: Create and configure TLS truststore automatically during Access Point installation.
- NEDS-72: Configure one-way SSL automatically during Access Point installation.
- NEDS-73: Change Access Point sign and TLS key size from 3096 to 3072.
- NEDS-74: Change SMP sign and TLS key size from 3096 to 3072.
- NEDS-75: Set the Access Point sign key alias automatically during the installation using a user defined value.
- NEDS-76: Add additional policies in the Access Point default configuration.
- NEDS-79: Update Access Point error messages so that they don't disclose excessive or sensitive information.
- NEDS-80: Update SMP error messages so that they don't disclose excessive or sensitive information.
- NEDS-81: Make the Access Point dynamic discovery client use the Access Point's TLS truststore instead of the system's default truststore.
- NEDS-82: Create and configure TLS truststore "/etc/harmony-smp/tls-trustore.jks" during fresh SMP installation.
- NEDS-85: Create a changelog document.

## Access Point and SMP - 1.0.0 - 2021-12-17

- NEDS-20: Implement Debian packaging for the Harmony Access Point component.
- NEDS-21: Implement Debian packaging for the Harmony SMP component.
- NEDS-28: Add relevant metadata to the Harmony Access Point package.
- NEDS-29: Add relevant metadata to the Harmony SMP package.
- NEDS-31: Create installation manual for the Harmony Access Point component.
- NEDS-32: Create installation manual for the Harmony SMP component.
- NEDS-47: Remove URL path segment from the Harmony Access Point component web interface, which had inconsistent behaviour.
- NEDS-48: Add a password hashing utility class to the Harmony Access Point component so that installation packages can hash the created user passwords at install time.
- NEDS-53: Add licensing information to the Harmony Access Point and SMP web interfaces.
- NEDS-54: Modify default property values so that the correctly point to the Harmony product and support.
- NEDS-59: Add documentation on how to configure the Harmony Access Point and SMP components for dynamic discovery.
- NEDS-63: Update dynamic-discovery-client to version 1.13 to resolve a bug where dynamic discovery was not usable with the OASIS identifiers.
- NEDS-65: Customise the look and feel of Harmony components web interfaces so that they match the branding.

# Changelog

## 2.0.0 - 2022-12-02

- NEDS-Update_4_2 Update to eDelivery 4.2 version

## 1.3.1 - 2022-11-15

- NEDS-98: Update the `commons-text` dependency.

## 1.3.0 - 2022-10-22

- NEDS-96: Add support for OpenJDK 11 for the Harmony Access Point.
- NEDS-97: Update the Harmony Access Point 3rd party dependencies.

## 1.2.0 - 2022-05-10

- NEDS-62: Enable the use of Peppol IDs in the Harmony Access Point Plugin User configuration.
- NEDS-86: Make the Harmony Access Point HTTPS port configurable during the installation process.
- NEDS-87: Make the Harmony SMP HTTPS port configurable during the installation process.
- NEDS-90: Improve the Harmony Access Point's Peppol AS4 specification.
- NEDS-91: Update the Harmony Access Point 3rd party dependencies.
- NEDS-93: Update the Harmony SMP 3rd party dependencies.

## 1.1.0 - 2022-02-17

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

## 1.0.0 - 2021-12-17

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

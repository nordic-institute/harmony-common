# Security and Vulnerability Disclosure Policy

[![Go to Harmony Community Slack](https://img.shields.io/badge/Go%20to%20Community%20Slack-grey.svg)](https://harmonyedelivery.slack.com/)
[![Get invited](https://img.shields.io/badge/No%20Slack-Get%20invited-green.svg)](https://edelivery.digital/harmony-edelivery-access-community)
[![License badge](https://img.shields.io/badge/license-EUPL-blue.svg)](LICENSE.md)
[![Documentation badge](https://img.shields.io/badge/docs-latest-brightgreen.svg)](doc/)
[![Support badge]( https://img.shields.io/badge/support-sof-yellowgreen.svg)](https://edelivery.digital/contact)

## Supported Versions

Latest version and two earlier versions of Harmony eDelivery Access are officially supported by NIIS. The supported versions are defined on `MAJOR.MINOR` level so the release of patch versions (`MAJOR.MINOR.PATCH`) does not affect the support.

| Version | Supported |
|----------|-----------|
| Latest major.minor release | ✅ |
| Previous major.minor release | ✅ |
| Second previous major.minor release | ✅ |
| Older releases | ❌ |

## Security Updates

NIIS provides security fixes, mitigation guidance and other remediation measures for supported versions of Harmony eDelivery Access where appropriate.

Users are encouraged to apply security updates without undue delay and to keep their deployments on supported versions.

## Role of NIIS

The [Nordic Institute for Interoperability Solutions (NIIS)](https://www.niis.org/) acts as the steward of the Harmony eDelivery Access project and coordinates security vulnerability handling for [Harmony Access Point](https://github.com/nordic-institute/harmony-access-point) and [Harmony SMP](https://github.com/nordic-institute/harmony-smp) maintained by NIIS.

Users of Harmony Access Point and Harmony SMP remain responsible for the security of their own deployments, infrastructure, operating environments and operational processes.

## Scope

This policy applies to software components maintained by NIIS as part of the Harmony eDelivery Access project:

- [Harmony Access Point](https://github.com/nordic-institute/harmony-access-point)
- [Harmony SMP](https://github.com/nordic-institute/harmony-smp)

Security issues affecting deployment-specific configurations, infrastructure, third-party extensions or integrations maintained by other organizations should be reported to the responsible operators or maintainers.

## Reporting a Vulnerability

If you believe you have discovered a security vulnerability in Harmony eDelivery Access, please report it privately through the following channel:

- [Harmony eDelivery Access Service Desk](https://nordic-institute.atlassian.net/servicedesk/customer/portal/3)
    - Use the `Report a bug` request type.
    - [Sign up](https://id.atlassian.com/signup) for an account and get access to the [Harmony eDelivery Access Service Desk](https://nordic-institute.atlassian.net/servicedesk/customer/portal/3).

NIIS treats vulnerability reports as confidential and shares information only as necessary to investigate, remediate and coordinate disclosure of the issue.

Please do not disclose security vulnerabilities publicly before NIIS has had an opportunity to investigate and coordinate remediation.

### Information to Include

To help us assess and address the issue efficiently, please include as much of the following information as possible:

- Affected component
- Affected version(s)
- Description of the vulnerability
- Steps required to reproduce the issue
- Proof-of-concept code or screenshots, if available
- Potential impact
- Suggested mitigation, if known

Reports that do not provide sufficient information to reproduce the issue may not be actionable.

## Vulnerability Handling

NIIS works to develop and distribute remediation measures, security updates and mitigation guidance for vulnerabilities affecting supported versions of Harmony eDelivery Access.

Reported vulnerabilities are reviewed, assessed and prioritized according to their severity, exploitability and potential impact on Harmony eDelivery Access users.

NIIS may request additional information from the reporter during the assessment process.

NIIS aims to acknowledge vulnerability reports in a timely manner and may provide status updates during the investigation and remediation process.

## Coordinated Vulnerability Disclosure

NIIS follows a coordinated vulnerability disclosure process.

We ask security researchers and reporters to refrain from public disclosure until:

- The vulnerability has been investigated;
- Appropriate remediation or mitigation measures have been identified; and
- A coordinated disclosure plan has been agreed where appropriate.

NIIS works with reporters and relevant stakeholders to validate vulnerabilities, develop fixes, coordinate disclosures and communicate remediation guidance.

## Security Advisories

When vulnerabilities are confirmed and disclosure is appropriate, NIIS may publish security advisories describing:

- Affected versions
- Impact and severity
- Available mitigations
- Fixed versions
- Additional guidance for operators

Security-related information may be communicated through:

- Harmony eDelivery Access release notes
- GitHub Security Advisories
- Harmony eDelivery Access documentation
- Other official Harmony eDelivery Access communication channels

## Vulnerability Information Sharing

To support effective remediation and risk management, NIIS may share vulnerability information with:

- Affected stakeholders
- Relevant users
- Security researchers involved in the disclosure process
- Competent authorities where required by applicable legislation

Information sharing is conducted in a manner that supports coordinated vulnerability disclosure and minimizes risks to affected users.

## Good Faith Security Research

NIIS welcomes responsible security research that helps improve the security of Harmony eDelivery Access.

Researchers are expected to:

- Act in good faith
- Avoid privacy violations
- Avoid service disruption
- Avoid destruction or modification of data
- Refrain from public disclosure until coordinated disclosure activities have been completed

## Questions

If you have questions regarding the security of Harmony eDelivery Access or this policy, please contact the [Harmony eDelivery Access Service Desk](https://nordic-institute.atlassian.net/servicedesk/customer/portal/3) or email `security@niis.org`.
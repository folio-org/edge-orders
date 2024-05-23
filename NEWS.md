## 3.1.0 - Unreleased

## 3.0.1 - Released (Quesnelia R1 2024)
The primary focus of this release was enhancing HTTP Endpoint Security with TLS and FIPS-140-2 Compliant Cryptography feature
[Full Changelog](https://github.com/folio-org/edge-orders/compare/v3.0.0...v3.0.1)

## Stories
* [EDGORDERS-83](https://folio-org.atlassian.net/browse/EDGORDERS-83) - Enhance HTTP Endpoint Security with TLS and FIPS-140-2 Compliant Cryptography
* [EDGORDERS-82](https://folio-org.atlassian.net/browse/EDGORDERS-82) - Enhance WebClient TLS Configuration for Secure Connections to OKAPI


## 3.0.0 - Released (Quesnelia R1 2024)
The primary focus of this release was to update vertx and edge-common versions

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.9.0...v3.0.0)

### Dependencies
* Bump `vertx` from `4.3.4` to `4.5.4`
* Bump `edge-coommon` from `4.4.1` to `4.6.0`

## 2.9.0 - Released (Poppy R2 2023)
The primary focus of this release was to update to Java 17

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.8.0...v2.9.0)

### Stories
* [EDGORDERS-76](https://issues.folio.org/browse/EDGORDERS-76) Update to Java 17 edge-orders

### Bugfixes
* [EDGORDERS-72](https://issues.folio.org/browse/EDGORDERS-72) Patch Jackson, Vert.x, Aws and Alpine

### Dependencies
* Bump `java version` from `11` to `17`

## 2.8.0 - Released (Orchid R1 2023)
The primary focus of this release was to improvement logging

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.7.0...v2.8.0)

### Stories
* [EDGORDERS-70](https://issues.folio.org/browse/EDGORDERS-70) Logging improvement - Configuration
* [EDGORDERS-55](https://issues.folio.org/browse/EDGORDERS-55) Logging improvement

## 2.7.0 Nolana R3 2022 - Released
This release focused on order line updates fix

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.6.0...v2.7.0)

### Bugfixes
* [EDGORDERS-65](https://issues.folio.org/browse/EDGORDERS-65) Order line updates via ebsconet throws 406

## 2.6.0 Morning Glory R2 2022 - Released
This release focused on upgrading Vertx to 43.1 and tenant header injection fix

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.5.0...v2.6.0)

### Stories
* [EDGORDERS-57](https://issues.folio.org/browse/EDGORDERS-57) - Edge-orders - Morning Glory 2022 R2 - Vert.x 3.9.3/v4 upgrade

### Bugfixes
* [EDGEORDERS-60](https://issues.folio.org/browse/EDGORDERS-60) - Edge-common 4.3.0 fixing tenant header injection

## 2.5.0 - Released
This release focused on log4j security fix

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.4.0...v2.5.0)

### Stories
* [EDGORDERS-50](https://issues.folio.org/browse/EDGORDERS-50) - Log4j edge- modules 2.17.0 upgrade

## 2.4.0 - Released
This release contains updates regarding integration with mod-ebsconet module. Added personal data disclosure form

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.3.0...v2.4.0)

### Stories
* [EDGEORDERS-40](https://issues.folio.org/browse/EDGORDERS-40) - Add routing definition to mod-ebsconet
* [EDGEORDERS-37](https://issues.folio.org/browse/EDGORDERS-37) - Add personal data disclosure form

### Bugfixes
* [EDGEORDERS-34](https://issues.folio.org/browse/EDGORDERS-34) - Type parameter needs to be case-insensitive


## 2.3.0 - Released

This release has focussed on upgrade to JDK 11

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.2.1...v2.3.0)

### Stories
* [EDGEORDERS-30](https://issues.folio.org/browse/EDGORDERS-30) - Migrate edge-orders to JDK 11

## 2.2.1 - Released

This bugfix release includes includes changes regarding to processing Accept header

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.2.0...v2.2.1)

### Bugs
* [EDGEORDERS-27](https://issues.folio.org/browse/EDGORDERS-27) - Honor Accept header

## 2.2.0 - Released

This release includes upgrade RMB and configuration based routing

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.1.0...v2.2.0)

### Stories
* [EDGEORDERS-24](https://issues.folio.org/browse/EDGORDERS-24) - edge-orders: Update to RMB v30.0.1
* [EDGEORDERS-22](https://issues.folio.org/browse/EDGORDERS-22) - Configuration based request routing

## 2.1.0 - Released

This release includes tuning environment settings

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.0.2...v2.1.0)

### Stories
* [EDGEORDERS-20](https://issues.folio.org/browse/EDGORDERS-20) - Use JVM features to manage container memory
* [FOLIO-2235](https://issues.folio.org/browse/FOLIO-2235) - Add LaunchDescriptor settings to each backend non-core module repository

## 2.0.2 - Released

This release includes updating of login interface version because of breaking change in mod-login

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.0.1...v2.0.2)

## 2.0.1 - Released

This release includes a bug fix to handle a timeout in POST endpoint which contains body

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.0.0...v2.0.1)

### Bugs
* [EDGEORDERS-15](https://issues.folio.org/browse/EDGORDERS-15) - POST /orders/validate timesout with invalid body content

## 2.0.0 - Released

Major changes for this release include conforming to the new API Key format changed in edge-common and creating a new /validate API to support both GET and POST Http 
methods.

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v1.0.0...v2.0.0)

### Stories
* [EDGEORDERS-12](https://issues.folio.org/browse/EDGORDERS-12) - Upgrade edge-common to use the latest release with new API key structure
* [EDGEORDERS-10](https://issues.folio.org/browse/EDGORDERS-10) - Add new POST API for validation and change response code to 200
* [EDGEORDERS-9](https://issues.folio.org/browse/EDGORDERS-9) - Upgrading to RAML 1.0

### Bugs
* [EDGEORDERS-11](https://issues.folio.org/browse/EDGORDERS-11) - Add dependency on login interface

## 1.0.0 2018-12-06
 * Updated edge-common dependency to 1.0.0

## 0.0.1 2018-05-14
 * Initial Commit

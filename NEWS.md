## 2.7.0 - Unreleased

## 2.6.3 (Morning Glory R2 Bug Fix) - Released
This release focused on fixing a bug related to updating orders via Ebsconet.

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.6.2...v2.6.3)

### Bug Fixes
* [EDGORDERS-65](https://issues.folio.org/browse/EDGORDERS-65) - Order line updates via ebsconet throws 406

## 2.6.2 (Bug Fix) - Released
This release focused on fixing a bug in a Vert.x dependency.

### Bug Fixes
* [EDGORDERS-63](https://issues.folio.org/browse/EDGORDERS-63) - edge-common 4.4.1 fixing disabled SSL in Vert.x WebClient

## 2.6.1 (Bug Fix) - Released
This release focused on updating contract for required mod-ebsconet module to support fix of issue when renewal note is blank after update

[Full Changelog](https://github.com/folio-org/edge-orders/compare/v2.6.0...v2.6.1)

### Bug Fixes
* [MODEBSNET-39](https://issues.folio.org/browse/MODEBSNET-39) - Renewal note is blank after update


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

## 2.3.0 - Unreleased
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

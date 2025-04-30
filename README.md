# edge-orders

Copyright (C) 2018-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Overview

The purpose of this edge API is to bridge the gap between 3rd party purchasing systems/vendors and FOLIO. More
specifically, the initial implementation was built with EBSCO's GOBI (Global Online Bibliographic Interface) service in
mind.

## Security

See [edge-common](https://github.com/folio-org/edge-common) for a description of the security model.

## Required Permissions

Institutional users should be granted the following permissions in order to use this edge API:

- `gobi.all`

## Configuration

See [edge-common](https://github.com/folio-org/edge-common) for a description of how configuration works.

Api configuration can be specified by `api_config` system property as URL or path.

By default `/resources/api_configuration.json` will be used.

Api Configuration format:

| Property    | Example          | Description             |
|-------------|------------------|-------------------------|
| type        | GOBI             | Type of the system      |
| pathPattern | /orders/validate | URL Path for mapping    |
| method      | POST             | HTTP method for mapping |
| proxyMethod | POST             | HTTP method for proxy   |
| proxyPath   | /gobi/validate   | Path for proxy          |

Example of api mapping content:

```json
{
  "routing": [
    {
      "type": "GOBI",
      "method": "POST",
      "pathPattern": "/orders/validate",
      "proxyPath": "/gobi/validate"
    },
    {
      "type": "GOBI",
      "method": "GET",
      "pathPattern": "/orders/validate",
      "proxyMethod": "POST",
      "proxyPath": "/gobi/validate"
    }
  ]
}
``` 

## Security Configuration

Configuration information is specified in two forms:

1. System Properties - General configuration
2. Properties File - Configuration specific to the desired secure store

### System Properties

| Property                    | Default             | Description                                                                                                                                        |
|-----------------------------|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| `port`                      | `8081`              | Server port to listen on                                                                                                                           |
| `okapi_url`                 | *required*          | Where to find Okapi (URL)                                                                                                                          |
| `secure_store`              | `Ephemeral`         | Type of secure store to use.  Valid: `Ephemeral`, `AwsSsm`, `Vault`                                                                                |
| `secure_store_props`        | `NA`                | Path to a properties file specifying secure store configuration                                                                                    |
| `token_cache_ttl_ms`        | `3600000`           | How long to cache JWTs, in milliseconds (ms)                                                                                                       |
| `null_token_cache_ttl_ms`   | `30000`             | How long to cache login failure (null JWTs), in milliseconds (ms)                                                                                  |
| `token_cache_capacity`      | `100`               | Max token cache size                                                                                                                               |
| `log_level`                 | `INFO`              | Log4j Log Level                                                                                                                                    |
| `request_timeout_ms`        | `30000`             | Request Timeout                                                                                                                                    |
| `api_key_sources`           | `PARAM,HEADER,PATH` | Defines the sources (order of precedence) of the API key                                                                                           |

### Env variables for TLS configuration for Http server

To configure Transport Layer Security (TLS) for the HTTP server in an edge module, the following configuration parameters should be used.
Parameters marked as Required are required only in case when TLS for the server should be enabled.

| Property                                            | Default          | Description                                                                                 |
|-----------------------------------------------------|------------------|---------------------------------------------------------------------------------------------|
| `SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_TYPE`     | `NA`             | (Required). Set the type of the keystore. Common types include `JKS`, `PKCS12`, and `BCFKS` |
| `SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_LOCATION` | `NA`             | (Required). Set the location of the keystore file in the local file system                  |
| `SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_PASSWORD` | `NA`             | (Required). Set the password for the keystore                                               |
| `SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEY_ALIAS`         | `NA`             | Set the alias of the key within the keystore.                                               |
| `SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEY_PASSWORD`      | `NA`             | Optional param that points to a password of `KEY_ALIAS` if it protected                     |

### Env variables for TLS configuration for Web Client

To configure Transport Layer Security (TLS) for Web clients in the edge module, you can use the following configuration parameters.
Truststore parameters for configuring Web clients are optional even when `FOLIO_CLIENT_TLS_ENABLED = true`.
If truststore parameters need to be populated, `FOLIO_CLIENT_TLS_TRUSTSTORETYPE`, `FOLIO_CLIENT_TLS_TRUSTSTOREPATH` and `FOLIO_CLIENT_TLS_TRUSTSTOREPASSWORD` are required.

| Property                                | Default           | Description                                                                      |
|-----------------------------------------|-------------------|----------------------------------------------------------------------------------|
| `FOLIO_CLIENT_TLS_ENABLED`              | `false`           | Set whether SSL/TLS is enabled for Vertx Http Server                             |
| `FOLIO_CLIENT_TLS_TRUSTSTORETYPE`       | `NA`              | Set the type of the keystore. Common types include `JKS`, `PKCS12`, and `BCFKS`  |
| `FOLIO_CLIENT_TLS_TRUSTSTOREPATH`       | `NA`              | Set the location of the keystore file in the local file system                   |
| `FOLIO_CLIENT_TLS_TRUSTSTOREPASSWORD`   | `NA`              | Set the password for the keystore                                                |


## Additional information

There will be a single instance of okapi client per OkapiClientFactory and per tenant, which means that this client
should never be closed or else there will be runtime errors. To enforce this behaviour, method close() has been removed
from OkapiClient class.

## Error processing

Priority of return types if the client accepts more than one type:

1. application/json
2. application/xml
3. text/plain
   default : application/xml

Examples:

| Accept Header                    | Error Format     |
|----------------------------------|------------------|
| application/json                 | application/json |
| application/xml                  | application/xml  |
| text/plain                       | text/plain       |
| */*                              | application/xml  |
| text/plain,application/json      | application/json |
| application/json,application/xml | application/json |
| application/xml,application/json | application/json |
| text/plain,application/xml       | application/xml  |
| other                            | application/xml  |

## Additional information

### Issue tracker

See project [EDGORDERS](https://issues.folio.org/browse/EDGORDERS)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)


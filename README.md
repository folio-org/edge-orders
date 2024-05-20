# edge-orders

Copyright (C) 2018-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Overview

The purpose of this edge API is to bridge the gap between 3rd party purchasing systems/vendors and FOLIO.  More specifically, the initial implementation was built with EBSCO's GOBI (Global Online Bibliographic Interface) service in mind.

## Security

See [edge-common](https://github.com/folio-org/edge-common) for a description of the security model.

## Required Permissions

Institutional users should be granted the following permissions in order to use this edge API:
- `gobi.all`

## Configuration

See [edge-common](https://github.com/folio-org/edge-common) for a description of how configuration works.

Api configuration can be specified by `api_config` system property as URL or path.

By default `/reources/api_configuration.json` will be used.

Api Configuration format:

Property               | Example           | Description
---------------------- | ----------------- | -------------
type                   |  GOBI             | Type of the system 
pathPattern            |  /orders/validate | URL Path for mapping
method                 |  POST             | HTTP method for mapping
proxyMehtod            |  POST             | HTTP method for proxy 
proxyPath              |  /gobi/validate   | Path for proxy

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
### System Properties

| Property               | Default           | Description                                                             |
|------------------------|-------------------|-------------------------------------------------------------------------|
| `port`                 | `8081`            | Server port to listen on                                                |
| `okapi_url`            | *required*        | Where to find Okapi (URL)                                               |
| `request_timeout_ms`   | `30000`           | Request Timeout                                                         |
| `log_level`            | `INFO`            | Log4j Log Level                                                         |
| `token_cache_capacity` | `100`             | Max token cache size                                                    |
| `token_cache_ttl_ms`   | `100`             | How long to cache JWTs, in milliseconds (ms)                            |
| `secure_store`         | `Ephemeral`       | Type of secure store to use.  Valid: `Ephemeral`, `AwsSsm`, `Vault`     |
| `secure_store_props`   | `NA`              | Path to a properties file specifying secure store configuration         |

### System Properties for TLS configuration for Http server
To configure Transport Layer Security (TLS) for HTTP server in edge module, the following configuration parameters should be used.
Parameters marked as Required are required only in case when ssl_enabled is set to true.

| Property                          | Default           | Description                                                                                 |
|-----------------------------------|-------------------|---------------------------------------------------------------------------------------------|
| `http-server.ssl_enabled`         | `false`           | Set whether SSL/TLS is enabled for Vertx Http Server                                        |
| `http-server.keystore_type`       | `NA`              | (Required). Set the type of the keystore. Common types include `JKS`, `PKCS12`, and `BCFKS` |
| `http-server.keystore_provider`   | `NA`              | Set the provider name of the key store                                                      |
| `http-server.keystore_path`       | `NA`              | (Required). Set the location of the keystore file in the local file system                  |
| `http-server.keystore_password`   | `NA`              | (Required). Set the password for the keystore                                               |
| `http-server.key_alias`           | `NA`              | Set the alias of the key within the keystore.                                               |
| `http-server.key_alias_password`  | `NA`              | Optional param that points to a password of `key_alias` if it protected                     |

### System Properties for TLS configuration for Web Client
To configure Transport Layer Security (TLS) for Web clients in the edge module, you can use the following configuration parameters.
Truststore parameters for configuring Web clients are optional even when ssl_enabled = true.
If truststore parameters need to be populated - truststore_type, truststore_path, truststore_password - are required.

| Property                          | Default           | Description                                                                      |
|-----------------------------------|-------------------|----------------------------------------------------------------------------------|
| `web-client.ssl_enabled`          | `false`           | Set whether SSL/TLS is enabled for Vertx Http Server                             |
| `web-client.truststore_type`      | `NA`              | Set the type of the keystore. Common types include `JKS`, `PKCS12`, and `BCFKS`  |
| `web-client.truststore_provider`  | `NA`              | Set the provider name of the key store                                           |
| `web-client.truststore_path`      | `NA`              | Set the location of the keystore file in the local file system                   |
| `web-client.truststore_password`  | `NA`              | Set the password for the keystore                                                |
| `web-client.key_alias`            | `NA`              | Set the alias of the key within the keystore.                                    |
| `web-client.key_alias_password`   | `NA`              | Optional param that points to a password of `key_alias` if it protected          |

## Error processing
Priority of return types if the client accepts more than one type:
1. application/json
2. application/xml 
3. text/plain 
default : application/xml

Examples:

Accept Header                    | Error Format 
---------------------------------| ---------------- 
application/json                 | application/json  
application/xml                  | application/xml   
text/plain                       | text/plain
*/*                              | application/xml   
text/plain,application/json      | application/json
application/json,application/xml | application/json
application/xml,application/json | application/json
text/plain,application/xml       | application/xml               
other                            | application/xml  

## Additional information

### Issue tracker

See project [EDGORDERS](https://issues.folio.org/browse/EDGORDERS)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)


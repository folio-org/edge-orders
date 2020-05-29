# edge-orders

Copyright (C) 2018-2019 The Open Library Foundation

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

## Additional information

### Issue tracker

See project [EDGORDERS](https://issues.folio.org/browse/EDGORDERS)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)


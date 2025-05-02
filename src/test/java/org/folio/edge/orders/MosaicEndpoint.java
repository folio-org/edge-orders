package org.folio.edge.orders;

import io.vertx.core.http.HttpMethod;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

public enum MosaicEndpoint {
  VALIDATE(GET, "/validate", "/mosaic/validate"),
  CREATE_ORDERS(POST, "/orders", "/mosaic/orders");

  private final HttpMethod method;
  private final String ingressUrl;
  private final String egressUrl;

  MosaicEndpoint(HttpMethod method, String ingressUrl, String egressUrl) {
    this.method = method;
    this.ingressUrl = ingressUrl;
    this.egressUrl = egressUrl;
  }

  public HttpMethod getMethod() {
    return method;
  }

  public String getIngressUrl() {
    return ingressUrl;
  }

  public String getEgressUrl() {
    return egressUrl;
  }
}

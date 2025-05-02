package org.folio.edge.orders;

public enum MosaicEndpoint {
  VALIDATE("/mosaic/validate", "/mosaic/validate"),
  CREATE_ORDERS("/mosaic/orders", "/mosaic/orders");

  private final String ingressUrl;
  private final String egressUrl;

  MosaicEndpoint(String ingressUrl, String egressUrl) {
    this.ingressUrl = ingressUrl;
    this.egressUrl = egressUrl;
  }

  public String getIngressUrl() {
    return ingressUrl;
  }

  public String getEgressUrl() {
    return egressUrl;
  }
}

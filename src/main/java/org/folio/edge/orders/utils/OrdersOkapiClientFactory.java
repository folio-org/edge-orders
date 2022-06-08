package org.folio.edge.orders.utils;

import org.folio.edge.core.utils.OkapiClientFactory;

import io.vertx.core.Vertx;

public class OrdersOkapiClientFactory extends OkapiClientFactory {

  public OrdersOkapiClientFactory(Vertx vertx, String okapiURL, int reqTimeoutMs) {
    super(vertx, okapiURL, reqTimeoutMs);
  }

  public OrdersOkapiClient getOrdersOkapiClient(String tenant) {
    return new OrdersOkapiClient(vertx, okapiURL, tenant, reqTimeoutMs);
  }
}

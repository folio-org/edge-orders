package org.folio.edge.orders.utils;

import org.folio.edge.core.utils.OkapiClient;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;

public class OrdersOkapiClient extends OkapiClient {

  protected OrdersOkapiClient(Vertx vertx, String okapiURL, String tenant, long timeout) {
    super(vertx, okapiURL, tenant, timeout);
  }

  public OrdersOkapiClient(OkapiClient client) {
    super(client);
  }

  public void placeGobiOrder(String payload, Handler<HttpClientResponse> responseHandler,
      Handler<Throwable> exceptionHandler) {
    placeGobiOrder(payload, null, responseHandler, exceptionHandler);
  }

  public void placeGobiOrder(String payload, MultiMap headers, Handler<HttpClientResponse> responseHandler,
      Handler<Throwable> exceptionHandler) {
    post(
        String.format("%s/gobi/orders", okapiURL),
        tenant,
        payload,
        combineHeadersWithDefaults(headers),
        responseHandler,
        exceptionHandler);
  }

  public void validate(Handler<HttpClientResponse> responseHandler,
      Handler<Throwable> exceptionHandler) {
    validate(null, responseHandler, exceptionHandler);
  }

  public void validate(MultiMap headers, Handler<HttpClientResponse> responseHandler,
      Handler<Throwable> exceptionHandler) {
    get(
        String.format("%s/gobi/validate", okapiURL),
        tenant,
        combineHeadersWithDefaults(headers),
        responseHandler,
        exceptionHandler);
  }
}

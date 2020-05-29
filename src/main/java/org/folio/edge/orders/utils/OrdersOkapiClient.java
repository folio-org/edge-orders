package org.folio.edge.orders.utils;

import static org.folio.edge.core.Constants.XML_OR_TEXT;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.rest.mappings.model.Routing;

public class OrdersOkapiClient extends OkapiClient {

  protected OrdersOkapiClient(Vertx vertx, String okapiURL, String tenant, long timeout) {
    super(vertx, okapiURL, tenant, timeout);
  }

  public OrdersOkapiClient(OkapiClient client) {
    super(client);
  }

  @Override
  protected void initDefaultHeaders() {
    super.initDefaultHeaders();
    defaultHeaders.set(HttpHeaders.ACCEPT, XML_OR_TEXT);
  }

  public void send(Routing routing, String payload, MultiMap headers, Handler<HttpClientResponse> responseHandler,
    Handler<Throwable> exceptionHandler) {

    final String method = routing.getProxyMethod() == null ? routing.getMethod() : routing.getProxyMethod();

    if (method.equals("POST")) {
      post(
        okapiURL + routing.getProxyPath(),
        tenant,
        payload.isEmpty() ? null : payload,
        combineHeadersWithDefaults(headers),
        responseHandler,
        exceptionHandler);
    } else if (method.equals("GET")) {
      get(
        okapiURL + routing.getProxyPath(),
        tenant,
        combineHeadersWithDefaults(headers),
        responseHandler,
        exceptionHandler);
    }
  }
}

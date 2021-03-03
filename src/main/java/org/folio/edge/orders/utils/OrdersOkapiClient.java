package org.folio.edge.orders.utils;

import static org.folio.edge.core.Constants.XML_OR_TEXT;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.Constants;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.rest.mappings.model.Routing;

public class OrdersOkapiClient extends OkapiClient {

  private static final Logger logger = LogManager.getLogger(OrdersOkapiClient.class);

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

  public void send(Routing routing, String payload, MultiMap params, MultiMap headers, Handler<HttpClientResponse> responseHandler,
    Handler<Throwable> exceptionHandler) {

    final String method = routing.getProxyMethod() == null ? routing.getMethod() : routing.getProxyMethod();

    String resultPath = Optional.ofNullable(params)
        .map(it -> it.names().stream().reduce(routing.getProxyPath(), (acc, item) -> acc.replace(":" + item , params.get(item))))
        .orElse(routing.getProxyPath());

    if (method.equals("POST")) {
      post(
        okapiURL + resultPath,
        tenant,
        StringUtils.isEmpty(payload)? null : payload,
        combineHeadersWithDefaults(headers),
        responseHandler,
        exceptionHandler);
    } else if (method.equals("GET")) {
      get(
        okapiURL + resultPath,
        tenant,
        combineHeadersWithDefaults(headers),
        responseHandler,
        exceptionHandler);
    } else if (method.equals("PUT")) {
      put(
        okapiURL + resultPath,
        tenant,
        StringUtils.isEmpty(payload) ? null : payload,
        combineHeadersWithDefaults(headers),
        responseHandler,
        exceptionHandler);
    }
  }

  public void put(String url, String tenant, String payload, MultiMap headers,
    Handler<HttpClientResponse> responseHandler, Handler<Throwable> exceptionHandler) {

    final HttpClientRequest request = client.putAbs(url);

    if (headers != null) {
      request.headers().setAll(combineHeadersWithDefaults(headers));
    } else {
      request.headers().setAll(defaultHeaders);
    }

    logger.info(String.format("PUT %s tenant: %s token: %s", url, tenant, request.headers().get(Constants.X_OKAPI_TOKEN)));

    request.handler(responseHandler)
      .exceptionHandler(exceptionHandler)
      .setTimeout(reqTimeout);

    if (payload != null) {
      request.end(payload);
    } else {
      request.end();
    }
  }
}

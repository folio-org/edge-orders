package org.folio.edge.orders.utils;

import static org.folio.edge.core.Constants.XML_OR_TEXT;

import org.apache.commons.lang3.NotImplementedException;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.orders.Constants.PurchasingSystems;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;

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

  public void validate(HttpMethod method, PurchasingSystems ps, MultiMap headers, Handler<HttpClientResponse> responseHandler,
      Handler<Throwable> exceptionHandler) {
    if (PurchasingSystems.GOBI == ps) {
      validateGobi(method, headers, responseHandler, exceptionHandler);
    } else {
      exceptionHandler
        .handle(new NotImplementedException("validate(...) for " + ps + " is not supported yet"));
    }
  }

  public void placeOrder(PurchasingSystems ps, String payload, MultiMap headers,
      Handler<HttpClientResponse> responseHandler,
      Handler<Throwable> exceptionHandler) {
    if (PurchasingSystems.GOBI == ps) {
      placeGobiOrder(payload, headers, responseHandler, exceptionHandler);
    } else {
      exceptionHandler.handle(new NotImplementedException("placeOrder(...) for " + ps + " is not supported yet"));
    }
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

  public void validateGobi(HttpMethod method, MultiMap headers, Handler<HttpClientResponse> responseHandler,
      Handler<Throwable> exceptionHandler) {
    if(HttpMethod.GET.equals(method))
     get(
        String.format("%s/gobi/validate", okapiURL),
        tenant,
        combineHeadersWithDefaults(headers),
        responseHandler,
        exceptionHandler);
    else
      post(
          String.format("%s/gobi/validate", okapiURL),
          null,
          tenant,
          combineHeadersWithDefaults(headers),
          responseHandler,
          exceptionHandler);
  }
}

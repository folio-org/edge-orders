package org.folio.edge.orders.utils;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.APPLICATION_XML;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.folio.edge.orders.Constants.HTTP_METHOD_GET;
import static org.folio.edge.orders.Constants.HTTP_METHOD_POST;
import static org.folio.edge.orders.Constants.HTTP_METHOD_PUT;

import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.orders.Param;
import org.folio.rest.mappings.model.Routing;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;

public class OrdersOkapiClient extends OkapiClient {

  private static final Logger logger = LogManager.getLogger(OrdersOkapiClient.class);

  public OrdersOkapiClient(OkapiClient client) {
    super(client);
  }

  @Override
  protected void initDefaultHeaders() {
    super.initDefaultHeaders();
    defaultHeaders.set(HttpHeaders.ACCEPT, String.format("%s, %s, %s",APPLICATION_JSON, APPLICATION_XML, TEXT_PLAIN));
  }

  public void send(Routing routing, String payload, MultiMap params, MultiMap headers, Handler<HttpResponse<Buffer>> responseHandler,
                   Handler<Throwable> exceptionHandler) {
    logger.debug("send:: Trying to send request to Okapi with routing: {}, payload: {}", routing, payload);
    final String method = routing.getProxyMethod() == null ? routing.getMethod() : routing.getProxyMethod();
    final String proxyPath = prepareProxyPathWithQueryStringParams(routing.getProxyPath(), params);

    String resultPath = Optional.ofNullable(params)
      .map(it -> it.names().stream().reduce(proxyPath, (acc, item) -> acc.replace(String.format(":%s", item), params.get(item))))
      .orElse(proxyPath);
    switch (method) {
      case HTTP_METHOD_POST:
        logger.info("Sending POST request to Okapi with routing: {}, payload: {}, resultPath: {}", routing, payload, resultPath);
        post(
          okapiURL + resultPath,
          tenant,
          StringUtils.isEmpty(payload) ? null : payload,
          combineHeadersWithDefaults(headers),
          responseHandler,
          exceptionHandler);
        break;
      case HTTP_METHOD_GET:
        logger.info("Sending GET request to Okapi with routing: {}, payload: {}, resultPath: {}", routing, payload, resultPath);
        get(
          okapiURL + resultPath,
          tenant,
          combineHeadersWithDefaults(headers),
          responseHandler,
          exceptionHandler);
        break;
      case HTTP_METHOD_PUT:
        logger.info("Sending PUT request to Okapi with routing: {}, payload: {}, resultPath: {}", routing, payload, resultPath);
        put(
          okapiURL + resultPath,
          tenant,
          StringUtils.isEmpty(payload) ? null : payload,
          defaultHeaders,
          responseHandler,
          exceptionHandler);
        break;
      default:
        throw new UnsupportedOperationException(String.format("Unsupported method %s", method));
    }
  }

  private String prepareProxyPathWithQueryStringParams(String proxyPath, MultiMap params) {
    if (Objects.isNull(params)) {
      return proxyPath;
    }
    for (Param param : Param.values()) {
      if (StringUtils.containsAny(proxyPath, String.format(":%s", param.getName())) && StringUtils.isEmpty(params.get(param.getName()))) {
        proxyPath = !param.getDefaultValue().isBlank() ? setDefaultParamValue(proxyPath, param) : fullyRemoveParam(proxyPath, param);
      }
    }
    return proxyPath;
  }

  private String setDefaultParamValue(String proxyPath, Param param) {
    return proxyPath.replace(String.format(":%s", param.getName()), param.getDefaultValue());
  }

  private String fullyRemoveParam(String proxyPath, Param param) {
    String placeholderPattern = String.format("(\\?|&)(%1$s=:%1$s)", param.getName());
    return proxyPath.replaceAll(placeholderPattern, "");
  }

  public void put(String url, String tenant, String payload, MultiMap headers, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {
    logger.debug("put:: Trying to send request to Okapi with url: {}, payload: {}, tenant: {}", url, payload, tenant);
    HttpRequest<Buffer> request = client.putAbs(url);

    if (headers != null) {
      request.headers().setAll(combineHeadersWithDefaults(headers));
    } else {
      request.headers().setAll(defaultHeaders);
    }

    logger.info("PUT '{}' tenant: {} token: {}", () -> url, () -> tenant, () -> request.headers()
      .get(X_OKAPI_TOKEN));

    request.timeout(reqTimeout);
    if (StringUtils.isEmpty(payload)) {
      logger.info("put:: Payload is empty");
      request.send()
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);
    } else {
      request.sendBuffer(Buffer.buffer(payload))
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);
    }
  }
}

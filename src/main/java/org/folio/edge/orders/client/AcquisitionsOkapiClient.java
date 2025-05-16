package org.folio.edge.orders.client;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.APPLICATION_XML;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.folio.edge.orders.Constants.HTTP_METHOD_GET;
import static org.folio.edge.orders.Constants.HTTP_METHOD_POST;
import static org.folio.edge.orders.Constants.HTTP_METHOD_PUT;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.orders.Param;
import org.folio.okapi.common.ChattyHttpResponseExpectation;
import org.folio.okapi.common.ModuleId;
import org.folio.rest.mappings.model.Routing;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;

public class AcquisitionsOkapiClient extends OkapiClient {

  private static final Logger logger = LogManager.getLogger(AcquisitionsOkapiClient.class);

  public AcquisitionsOkapiClient(OkapiClient client) {
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

  /**
   * Get the module id header for the interface.
   *
   * <p>See <a href="https://github.com/folio-org/okapi/blob/master/doc/guide.md#multiple-interfaces">
   * multiple interfaces</a> documentation.
   *
   * @param interfaceName the interface with "interfaceType": "multiple", for example custom-fields
   * @param moduleName the module name (product name) without version, for example mod-orders-storage
   * @param headers the headers to set
   * @return future with the module id
   */
  public Future<String> getModuleIdForMultipleInterface(String interfaceName, String moduleName, MultiMap headers) {
    String requestUri = "/_/proxy/tenants/%s/modules?provide=%s"
      .formatted(tenant, interfaceName);
    return get(okapiURL + requestUri, tenant, combineHeadersWithDefaults(headers))
      .expecting(ChattyHttpResponseExpectation.SC_OK)
      .compose(res -> {
        var list = extractModuleIds(res.bodyAsJsonArray(), moduleName);
        if (list.isEmpty()) {
          return Future.failedFuture("No module found.");
        }
        if (list.size() != 1) {
          return Future.failedFuture("Multiple modules found: " + list);
        }
        return Future.succeededFuture(list.getFirst());
      }).onFailure( e -> logger.error("Failed to get moduleId for module name: {}", moduleName, e));
  }

  private static List<String> extractModuleIds(JsonArray jsonArray, String moduleName) {
    return IntStream.range(0, jsonArray.size())
      .mapToObj(jsonArray::getJsonObject)
      .map(o -> o.getString("id"))
      .filter(moduleId -> new ModuleId(moduleId).getProduct().equals(moduleName))
      .distinct()
      .toList();
  }
}

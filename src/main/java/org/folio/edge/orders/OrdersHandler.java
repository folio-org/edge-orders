package org.folio.edge.orders;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.APPLICATION_XML;
import static org.folio.edge.core.Constants.MSG_ACCESS_DENIED;
import static org.folio.edge.core.Constants.MSG_INVALID_API_KEY;
import static org.folio.edge.core.Constants.MSG_REQUEST_TIMEOUT;
import static org.folio.edge.orders.Constants.PARAM_TYPE;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.folio.edge.core.Handler;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.orders.Constants.ErrorCodes;
import org.folio.edge.orders.model.ErrorWrapper;
import org.folio.edge.orders.model.ResponseWrapper;
import org.folio.edge.orders.utils.OrdersOkapiClient;
import org.folio.edge.orders.utils.OrdersOkapiClientFactory;
import org.folio.rest.mappings.model.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrdersHandler extends Handler {
  private static final Logger logger = LoggerFactory.getLogger(OrdersHandler.class);

  public OrdersHandler(SecureStore secureStore, OrdersOkapiClientFactory ocf) {
    super(secureStore, ocf);
  }

  @Override
  protected void handleCommon(RoutingContext ctx, String[] requiredParams, String[] optionalParams,
      TwoParamVoidFunction<OkapiClient, Map<String, String>> action) {

    // the responses are short, we can safely drop the encoding header if passed
    if (null != ctx.request().getHeader(HttpHeaders.ACCEPT_ENCODING)) {
      ctx.request().headers().remove(HttpHeaders.ACCEPT_ENCODING);
    }

    String type = ctx.request()
        .getParam(PARAM_TYPE);
    if (type == null || type.isEmpty()) {
      badRequest(ctx, "Missing required parameter: " + PARAM_TYPE);
      return;
    }

    super.handleCommon(ctx, requiredParams, optionalParams, (client, params) -> {
      final OrdersOkapiClient ordersClient = new OrdersOkapiClient(client);

      params.put(PARAM_TYPE, type);
      action.apply(ordersClient, params);
    });
  }

  protected void handle(RoutingContext ctx, List<Routing> routingMapping) {
    handleCommon(ctx, new String[]{}, new String[]{}, (client, params) -> {
      String type = params.get(PARAM_TYPE);

      Routing routing;
      String currentPath = ctx.currentRoute().getPath();
      String requestNormalisedPath = ctx.normalisedPath();
      String requestMethod = ctx.request().rawMethod();

      try {
        routing = routingMapping.stream()
          .filter(r -> r.getType().equalsIgnoreCase(type)
            && r.getMethod().equals(requestMethod)
            && r.getPathPattern().equals(currentPath))
          .findFirst()
          .orElseThrow(Exception::new);
      } catch (Exception e) {
        logger.error("API configuration doesn't exist for type={} method={} pathPattern={}", type, requestMethod, requestNormalisedPath);
        badRequest(ctx, "Unknown Purchasing System Specified: " + type);
        return;
      }

      logger.info("Request is from purchasing system: {}", type);
      ((OrdersOkapiClient) client).send(routing, ctx.getBodyAsString(), ctx.request().params(), ctx.request().headers(),
        resp -> handleResponse(ctx, resp),
        t -> handleProxyException(ctx, t));
    });
  }

  protected void handleResponse(RoutingContext ctx, HttpClientResponse resp) {
    final StringBuilder body = new StringBuilder();
    resp.handler(buf -> {
          if (logger.isTraceEnabled()) {
            logger.trace("read bytes: {}", buf);
          }
          body.append(buf);
        })
        .endHandler(v -> {
          ctx.response().setStatusCode(resp.statusCode());
          if (body.length() > 0) {
            String respBody = body.toString();
            handleResponseWithBody(ctx, resp, respBody);
            if (logger.isDebugEnabled()) {
              logger.debug("status: {} response: {}", + resp.statusCode(), respBody);
            }
          } else {
            ctx.response().end();
          }
        });
  }

  @Override
  protected void invalidApiKey(RoutingContext ctx, String key) {
    ResponseWrapper resp = new ResponseWrapper(
        new ErrorWrapper(ErrorCodes.API_KEY_INVALID.name(), MSG_INVALID_API_KEY + ": " + key));
    handleErrorResponse(ctx, 401, resp);
  }

  @Override
  protected void accessDenied(RoutingContext ctx, String msg) {
    ResponseWrapper resp = new ResponseWrapper(new ErrorWrapper(ErrorCodes.ACCESS_DENIED.name(), MSG_ACCESS_DENIED));
    handleErrorResponse(ctx, 401, resp);
  }

  @Override
  protected void badRequest(RoutingContext ctx, String body) {
    ResponseWrapper resp = new ResponseWrapper(new ErrorWrapper(ErrorCodes.BAD_REQUEST.name(), body));
    handleErrorResponse(ctx, 400, resp);
  }

  @Override
  protected void requestTimeout(RoutingContext ctx, String msg) {
    ResponseWrapper resp = new ResponseWrapper(
        new ErrorWrapper(ErrorCodes.REQUEST_TIMEOUT.name(), MSG_REQUEST_TIMEOUT));
    handleErrorResponse(ctx, 408, resp);
  }

  @Override
  protected void internalServerError(RoutingContext ctx, String msg) {
    Thread.dumpStack();
    if (!ctx.response().ended()) {
      ResponseWrapper resp = new ResponseWrapper(new ErrorWrapper(ErrorCodes.INTERNAL_SERVER_ERROR.name(), msg));
      handleErrorResponse(ctx, 500, resp);
    }
  }

  private void handleResponseWithBody(RoutingContext ctx, HttpClientResponse response, String respBody) {
    String contentType = response.headers().get(HttpHeaders.CONTENT_TYPE);
    int status = response.statusCode();
    if (isSuccessStatus(status)) {
      ctx.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, contentType)
        .setStatusCode(status)
        .end(respBody);
    } else {
      processErrorResponse(ctx, respBody, contentType, status);
    }
  }

  private void processErrorResponse(RoutingContext ctx, String respBody, String contentType, int status) {
    String acceptHeader = ctx.request().getHeader(HttpHeaders.ACCEPT);
    if (contentType.equals(acceptHeader)) {
      ctx.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, acceptHeader)
        .setStatusCode(status)
        .end(respBody);
    } else {
      ErrorCodes errorCode = Optional.ofNullable(ErrorCodes.fromValue(status)).orElse(ErrorCodes.INTERNAL_SERVER_ERROR);
      ResponseWrapper resp = new ResponseWrapper(new ErrorWrapper(errorCode.name(), respBody));
      handleErrorResponse(ctx, status, resp);
    }
  }

  private void handleErrorResponse(RoutingContext ctx, int status, ResponseWrapper responseWrapper) {
    String acceptHeaders = Optional.ofNullable(ctx.request().getHeader(HttpHeaders.ACCEPT)).orElse(APPLICATION_XML);
    ctx.response().setStatusCode(status);
    try {
      if (acceptHeaders.contains(APPLICATION_JSON)) {
        ctx.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
          .end(responseWrapper.toJson());
      } else if (acceptHeaders.contains(APPLICATION_XML)){
        ctx.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_XML)
          .end(responseWrapper.toXml());
      } else {
        ctx.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_XML)
          .end(responseWrapper.toXml());
      }
    } catch (Exception e) {
      logger.error("Exception marshalling", e);
      internalServerError(ctx, "Failed to convert FOLIO response to " + acceptHeaders);
    }
  }

  private boolean isSuccessStatus(int statusCode) {
    return statusCode >= 200 && statusCode <= 299;
  }
}

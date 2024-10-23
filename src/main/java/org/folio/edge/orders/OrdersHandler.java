package org.folio.edge.orders;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.APPLICATION_XML;
import static org.folio.edge.core.Constants.MSG_ACCESS_DENIED;
import static org.folio.edge.core.Constants.MSG_INVALID_API_KEY;
import static org.folio.edge.core.Constants.MSG_REQUEST_TIMEOUT;
import static org.folio.edge.orders.Constants.PARAM_TYPE;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.folio.edge.core.Handler;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.core.utils.OkapiClientFactory;
import org.folio.edge.orders.Constants.ErrorCodes;
import org.folio.edge.orders.model.ErrorWrapper;
import org.folio.edge.orders.model.ResponseWrapper;
import org.folio.edge.orders.utils.OrdersOkapiClient;
import org.folio.rest.mappings.model.Routing;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;

public class OrdersHandler extends Handler {
  private static final Logger logger = LogManager.getLogger(OrdersHandler.class);

  public OrdersHandler(SecureStore secureStore, OkapiClientFactory ocf) {
    super(secureStore, ocf);
  }

  @Override
  protected void handleCommon(RoutingContext ctx, String[] requiredParams, String[] optionalParams,
      TwoParamVoidFunction<OkapiClient, Map<String, String>> action) {
    logger.debug("handleCommon:: Trying to handle request with required params: {}, optional params: {}", requiredParams, optionalParams);
    // the responses are short, we can safely drop the encoding header if passed
    if (null != ctx.request().getHeader(HttpHeaders.ACCEPT_ENCODING)) {
      ctx.request().headers().remove(HttpHeaders.ACCEPT_ENCODING);
    }

    String type = ctx.request()
        .getParam(PARAM_TYPE);
    if (type == null || type.isEmpty()) {
      logger.warn("handleCommon:: Type is not specified");
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
      String requestNormalisedPath = ctx.normalizedPath();
      String requestMethod = ctx.request().method().name();

      logger.debug("handle:: Trying to find routing for path: {}, method: {}", requestNormalisedPath, requestMethod);
      try {
        routing = routingMapping.stream()
          .filter(r -> r.getType().equalsIgnoreCase(type)
            && r.getMethod().equals(requestMethod)
            && r.getPathPattern().equals(currentPath))
          .findFirst()
          .orElseThrow(Exception::new);
      } catch (Exception e) {
        logger.error("API configuration doesn't exist for type: {} method: {} pathPattern: {}", type, requestMethod, requestNormalisedPath);
        badRequest(ctx, "Unknown Purchasing System Specified: " + type);
        return;
      }

      logger.info("handle:: Request is from purchasing system: {}", type);
      ((OrdersOkapiClient) client).send(routing, ctx.getBodyAsString(), ctx.request().params(), ctx.request().headers(),
        resp -> handleResponse(ctx, resp),
        t -> handleProxyException(ctx, t));
    });
  }

  protected void handleResponse(RoutingContext ctx, HttpResponse<Buffer> resp) {
    logger.debug("handleResponse:: Trying to handle response");
    final StringBuilder body = new StringBuilder();
    var buf = resp.body();

    if (logger.isTraceEnabled()) {
      logger.trace("read bytes: {}", buf);
    }
    if (buf != null) {
      body.append(buf);
    }

    ctx.response().setStatusCode(resp.statusCode());
    if (body.length() > 0) {
      String respBody = body.toString();
      handleResponseWithBody(ctx, resp, respBody);
      if (logger.isDebugEnabled()) {
        logger.debug("handleResponse:: Response status: {}, body: {}", resp.statusCode(), respBody);
      }
    } else {
      ctx.response().end();
    }
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

  private void handleResponseWithBody(RoutingContext ctx, HttpResponse<Buffer> response, String respBody) {
    logger.debug("handleResponseWithBody:: Trying to handle response with body: {}", respBody);
    String contentType = response.headers().get(HttpHeaders.CONTENT_TYPE);
    int status = response.statusCode();
    if (isSuccessStatus(status)) {
      ctx.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, contentType)
        .setStatusCode(status)
        .end(respBody);
    } else {
      logger.error("handleResponseWithBody:: Response status: {}, body: {}", status, respBody);
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
    logger.warn("handleErrorResponse:: Trying to handle error response with status: {}, responseWrapper: {}", status, responseWrapper);
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

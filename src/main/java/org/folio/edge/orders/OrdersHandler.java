package org.folio.edge.orders;

import static org.folio.edge.core.Constants.APPLICATION_XML;
import static org.folio.edge.core.Constants.MSG_ACCESS_DENIED;
import static org.folio.edge.core.Constants.MSG_REQUEST_TIMEOUT;
import static org.folio.edge.orders.Constants.PARAM_TYPE;

import java.util.Map;

import org.apache.log4j.Logger;
import org.folio.edge.core.Handler;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.orders.Constants.ErrorCodes;
import org.folio.edge.orders.Constants.PurchasingSystems;
import org.folio.edge.orders.model.ErrorWrapper;
import org.folio.edge.orders.model.ResponseWrapper;
import org.folio.edge.orders.utils.OrdersOkapiClient;
import org.folio.edge.orders.utils.OrdersOkapiClientFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class OrdersHandler extends Handler {

  private static final Logger logger = Logger.getLogger(OrdersHandler.class);

  public OrdersHandler(SecureStore secureStore, OrdersOkapiClientFactory ocf) {
    super(secureStore, ocf);
  }

  @Override
  protected void handleCommon(RoutingContext ctx, String[] requiredParams, String[] optionalParams,
      TwoParamVoidFunction<OkapiClient, Map<String, String>> action) {

    String type = ctx.request().getParam(PARAM_TYPE);
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

  protected void handle(RoutingContext ctx) {
    handleCommon(ctx,
        new String[] {},
        new String[] {},
        (client, params) -> {
          String type = params.get(PARAM_TYPE);
          PurchasingSystems ps = PurchasingSystems.fromValue(type);
          if (PurchasingSystems.GOBI == ps) {
            logger.info("Request is from purchasing system: " + ps.toString());
            ((OrdersOkapiClient) client).placeGobiOrder(
                ctx.getBodyAsString(),
                ctx.request().headers(),
                resp -> handleProxyResponse(ctx, resp),
                t -> handleProxyException(ctx, t));
          } else {
            badRequest(ctx, "Unknown Purchasing System Specified: " + type);
          }
        });
  }

  @Override
  protected void handleProxyResponse(RoutingContext ctx, HttpClientResponse resp) {
    final StringBuilder body = new StringBuilder();
    resp.handler(buf -> {

      if (logger.isTraceEnabled()) {
        logger.trace("read bytes: " + buf.toString());
      }

      body.append(buf);
    }).endHandler(v -> {
      ctx.response().setStatusCode(resp.statusCode());

      if (body.length() > 0) {
        String respBody = body.toString();

        if (logger.isDebugEnabled()) {
          logger.debug("response: " + respBody);
        }

        String xml;
        try {
          xml = ResponseWrapper.fromJson(respBody).toXml();
        } catch (Exception e) {
          logger.error("Failed to convert FOLIO response from JSON -> XML", e);
          internalServerError(ctx, "Failed to convert FOLIO response from JSON -> XML");
          return;
        }

        String contentType = resp.getHeader(HttpHeaders.CONTENT_TYPE);
        if (contentType != null) {
          ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_XML);
        }

        ctx.response().end(xml);
        return;
      } else {
        ctx.response().end();
      }
    });
  }

  private void handleError(RoutingContext ctx, int status, ResponseWrapper respBody) {
    String xml = null;
    try {
      xml = respBody.toXml();
    } catch (JsonProcessingException e) {
      logger.error("Exception marshalling XML", e);
    }
    ctx.response().setStatusCode(status);

    if (xml != null) {
      ctx.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_XML)
        .end(xml);
    } else {
      ctx.response().end();
    }
  }

  @Override
  protected void accessDenied(RoutingContext ctx, String msg) {
    ResponseWrapper resp = new ResponseWrapper(new ErrorWrapper(ErrorCodes.ACCESS_DENIED.name(), MSG_ACCESS_DENIED));
    handleError(ctx, 401, resp);
  }

  @Override
  protected void badRequest(RoutingContext ctx, String body) {
    ResponseWrapper resp = new ResponseWrapper(new ErrorWrapper(ErrorCodes.BAD_REQUEST.name(), body));
    handleError(ctx, 400, resp);
  }

  @Override
  protected void requestTimeout(RoutingContext ctx, String msg) {
    ResponseWrapper resp = new ResponseWrapper(
        new ErrorWrapper(ErrorCodes.REQUEST_TIMEOUT.name(), MSG_REQUEST_TIMEOUT));
    handleError(ctx, 408, resp);
  }

  @Override
  protected void internalServerError(RoutingContext ctx, String msg) {
    if (!ctx.response().ended()) {
      ResponseWrapper resp = new ResponseWrapper(new ErrorWrapper(ErrorCodes.INTERNAL_SERVER_ERROR.name(), msg));
      handleError(ctx, 500, resp);
    }
  }
}

package org.folio.edge.orders.utils;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.APPLICATION_XML;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;

import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.Mappers;
import org.folio.edge.core.utils.test.MockOkapi;
import org.folio.edge.orders.model.ResponseWrapper;
import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import software.amazon.awssdk.utils.StringInputStream;

public class OrdersMockOkapi extends MockOkapi {

  private static final Logger logger = LogManager.getLogger(OrdersMockOkapi.class);

  public static final String BODY_REQUEST_FOR_HEADER_INCONSISTENCY = "{Body request for exception}";

  public OrdersMockOkapi(int port, List<String> knownTenants) {
    super(port, knownTenants);
  }

  @Override
  public Router defineRoutes() {
    Router router = super.defineRoutes();
    router.route(HttpMethod.GET, "/gobi/validate").method(HttpMethod.POST).handler(this::validateHandler);
    router.route(HttpMethod.POST, "/gobi/orders").handler(this::placeOrdersHandler);
    router.route(HttpMethod.PUT, "/ebsconet/order-lines/:id").handler(this::putOrderLinesHandler);
    return router;
  }

  public void validateHandler(RoutingContext ctx) {
    String token = ctx.request().getHeader(X_OKAPI_TOKEN);
    String status = ctx.request().getHeader(X_ECHO_STATUS);

    if (token == null || !token.equals(MOCK_TOKEN)) {
      ctx.response()
          .setStatusCode(403)
          .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
          .end("Access requires permission: gobi.order.post");
    } else if (status != null && !status.isEmpty()) {
      ctx.response()
          .setStatusCode(Integer.parseInt(status))
          .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
          .end("No suitable module found for path /gobi/validate");
    } else {
      if (ctx.request().method().equals(HttpMethod.GET)) {
        ctx.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_XML)
            .setStatusCode(200)
            .end("<test>GET - OK</test>");
      }
      else {
        ctx.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_XML)
            .setStatusCode(200)
            .end("<test>POST - OK</test>");
      }
    }
  }

  public void putOrderLinesHandler(RoutingContext ctx) {
    String token = ctx.request().getHeader(X_OKAPI_TOKEN);
    if (token == null || !token.equals(MOCK_TOKEN)) {
      ctx.response()
        .setStatusCode(403)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end("Access requires permission: gobi.order.post");
    }
    else if (ctx.body().isEmpty()) {
      ctx.response()
        .setStatusCode(204)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end(ctx.request().getParam("id"));
    }
  }

  public void placeOrdersHandler(RoutingContext ctx) {
    String token = ctx.request().getHeader(X_OKAPI_TOKEN);

    if (token == null || !token.equals(MOCK_TOKEN)) {
      ctx.response()
        .setStatusCode(403)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end("Access requires permission: gobi.order.post");
    }
    else if (BODY_REQUEST_FOR_HEADER_INCONSISTENCY.equals(ctx.getBodyAsString())) {
      ctx.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .setStatusCode(400)
        .end("Bad request");
    }
    else if (StringUtils.isEmpty(ctx.body().asString())) {
      ctx.response()
        .setStatusCode(201)
        .end();
    } else {
      String id;
      try {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(new StringInputStream(ctx.body().asString()));
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "//ItemPONumber";
        id = xPath.compile(expression).evaluate(xmlDocument);

        String body;
        String accept = ctx.request().getHeader(HttpHeaders.ACCEPT);
        logger.info("ACCEPT = {}", accept);
        if (accept != null && accept.equals(APPLICATION_JSON)) {
          body = getGobiOrderAsJson("PO-" + id);
          ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
        } else {
          body = getGobiOrderAsXml("PO-" + id);
          ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_XML);
        }

        ctx.response()
          .setStatusCode(201)
          .end(body);
      } catch (Exception e) {
        logger.error("Exception parsing request", e);
        ctx.response()
          .setStatusCode(400)
          .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
          .end(e.getMessage());
      }
    }
  }

  public static String getGobiOrderAsXml(String id) {
    try {
      ResponseWrapper resp = new ResponseWrapper(id);
      return resp.toXml();
    } catch (JsonProcessingException e) {
      return Mappers.XML_PROLOG + "<Response><PoLineNumber>" + id + "</PoLineNumber></Response>";
    }
  }

  public static String getGobiOrderAsJson(String id) {
    try {
      ResponseWrapper resp = new ResponseWrapper(id);
      return resp.toJson();
    } catch (JsonProcessingException e) {
      return "{ \"id\": \"" + id + "\" }";
    }
  }
}

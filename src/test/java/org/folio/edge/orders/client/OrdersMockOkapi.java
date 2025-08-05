package org.folio.edge.orders.client;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.APPLICATION_XML;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.folio.edge.orders.MosaicEndpoint.CREATE_ORDERS;
import static org.folio.edge.orders.MosaicEndpoint.VALIDATE;
import static org.folio.edge.orders.Param.LIMIT;
import static org.folio.edge.orders.Param.OFFSET;
import static org.folio.edge.orders.Param.QUERY;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.Mappers;
import org.folio.edge.core.utils.test.MockOkapi;
import org.folio.edge.orders.CommonEndpoint;
import org.folio.edge.orders.MosaicEndpoint;
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

  public static final String ID = "id";
  public static final String TOTAL_RECORDS = "totalRecords";
  public static final String NO_DATA_ID = "5bafea52-57ea-40a7-9164-4e31b9473781";
  public static final String HAD_DATA_ID = "5bafea52-57ea-40a7-9164-4e31b9473782";
  public static final String FISCAL_YEAR_CODE_FY2025 = "FY2025";
  public static final String DELIMITER = "delimiter";
  public static final String COLON_DELIMITER = ":";
  public static final String FUND_CODE = "AFRICAHIST";
  public static final String LEDGER_CODE = "ONETIME";
  public static final String X_ECHO_STATUS_HEADER = "X-Echo-Status";

  public OrdersMockOkapi(int port, List<String> knownTenants) {
    super(port, knownTenants);
  }

  @Override
  public Router defineRoutes() {
    Router router = super.defineRoutes();
    // GOBI
    router.route(GET, "/gobi/validate").method(POST).handler(this::validateHandler);
    router.route(POST, "/gobi/orders").handler(this::placeOrdersHandler);
    // EBSCONET
    router.route(HttpMethod.PUT, "/ebsconet/order-lines/:id").handler(this::putOrderLinesHandler);
    // MOSAIC
    Arrays.stream(MosaicEndpoint.values()).forEach(endpoint ->
      router.route(endpoint.getMethod(), endpoint.getEgressUrl()).handler(this::handleMosaicRequest));
    // OKAPI PROXY MODULES
    router.route(GET, "/_/proxy/tenants/diku/modules").handler(this::handleOkapiModuleIdRequest);
    // COMMON
    Arrays.stream(CommonEndpoint.values()).forEach(endpoint ->
      router.route(GET, endpoint.getEgressUrl()).handler(ctx -> handleCommonGetRequest(endpoint, ctx)));
    return router;
  }

  public void validateHandler(RoutingContext ctx) {
    String token = ctx.request().getHeader(X_OKAPI_TOKEN);
    String status = ctx.request().getHeader(X_ECHO_STATUS_HEADER);

    if (token == null || !token.equals(MOCK_TOKEN)) {
      ctx.response()
          .setStatusCode(403)
          .putHeader(CONTENT_TYPE, TEXT_PLAIN)
          .end("Access requires permission: gobi.order.post");
    } else if (status != null && !status.isEmpty()) {
      ctx.response()
          .setStatusCode(Integer.parseInt(status))
          .putHeader(CONTENT_TYPE, TEXT_PLAIN)
          .end("No suitable module found for path /gobi/validate");
    } else {
      if (ctx.request().method().equals(GET)) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_XML)
          .setStatusCode(200)
          .end("<test>GET - OK</test>");
      }
      else {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_XML)
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
        .putHeader(CONTENT_TYPE, TEXT_PLAIN)
        .end("Access requires permission: gobi.order.post");
    }
    else if (ctx.body().isEmpty()) {
      ctx.response()
        .setStatusCode(204)
        .putHeader(CONTENT_TYPE, TEXT_PLAIN)
        .end(ctx.request().getParam("id"));
    }
  }

  public void placeOrdersHandler(RoutingContext ctx) {
    String token = ctx.request().getHeader(X_OKAPI_TOKEN);

    if (token == null || !token.equals(MOCK_TOKEN)) {
      ctx.response()
        .setStatusCode(403)
        .putHeader(CONTENT_TYPE, TEXT_PLAIN)
        .end("Access requires permission: gobi.order.post");
    }
    else if (BODY_REQUEST_FOR_HEADER_INCONSISTENCY.equals(ctx.body().asString())) {
      ctx.response()
        .putHeader(CONTENT_TYPE, TEXT_PLAIN)
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
          ctx.response().putHeader(CONTENT_TYPE, APPLICATION_JSON);
        } else {
          body = getGobiOrderAsXml("PO-" + id);
          ctx.response().putHeader(CONTENT_TYPE, APPLICATION_XML);
        }

        ctx.response()
          .setStatusCode(201)
          .end(body);
      } catch (Exception e) {
        logger.error("Exception parsing request", e);
        ctx.response()
          .setStatusCode(400)
          .putHeader(CONTENT_TYPE, TEXT_PLAIN)
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

  public void handleMosaicRequest(RoutingContext ctx) {
    String status = ctx.request().getHeader(X_ECHO_STATUS_HEADER);

    if (status != null && !status.isEmpty()) {
      ctx.response()
        .setStatusCode(Integer.parseInt(status))
        .putHeader(CONTENT_TYPE, TEXT_PLAIN)
        .end("No suitable module found for path /mosaic/validate");
    } else {
      if (ctx.request().method().equals(GET) && StringUtils.contains(ctx.request().uri(), VALIDATE.getEgressUrl()) ) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(SC_OK)
          .end(new JsonObject().put("status", "SUCCESS").toString());
      } else if (ctx.request().method().equals(POST) && StringUtils.contains(ctx.request().uri(), CREATE_ORDERS.getEgressUrl())) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(SC_OK)
          .end("10000-2");
      }
    }
  }

  public void handleOkapiModuleIdRequest(RoutingContext ctx) {
    ctx.response()
      .putHeader(CONTENT_TYPE, APPLICATION_JSON)
      .setStatusCode(SC_OK)
      .end("""
        [{"id": "mod-orders-storage-13.8.0"}, {"id": "mod-users-1.2.3"}]
      """);
  }

  public void handleCommonGetRequest(CommonEndpoint endpoint, RoutingContext ctx) {
    String offset = ctx.request().getParam(OFFSET.getName());
    String limit = ctx.request().getParam(LIMIT.getName());
    String query = ctx.request().getParam(QUERY.getName());
    String dataKey = endpoint.getDataKey();

    logger.info("handleGeneric:: Created handler: Ingress URL: {}, Egress URL: {}", endpoint.getIngressUrl(), endpoint.getEgressUrl());

    String responseBody;
    if (StringUtils.equals(query, "id==" + NO_DATA_ID)) {
      responseBody = new JsonObject()
        .put(dataKey, new JsonObject())
        .put(TOTAL_RECORDS, 0)
        .toString();
    } else if (StringUtils.equals(query, "id==" + HAD_DATA_ID)) {
      responseBody = new JsonObject()
        .put(dataKey, new JsonArray()
          .add(new JsonObject().put(ID, HAD_DATA_ID)))
        .put(TOTAL_RECORDS, 1)
        .toString();
    } else if (StringUtils.equals(offset, "0") && StringUtils.equals(limit, "2")) {
      responseBody = new JsonObject()
        .put(dataKey, new JsonArray()
          .add(new JsonObject().put(ID, UUID.randomUUID().toString()))
          .add(new JsonObject().put(ID, UUID.randomUUID().toString())))
        .put(TOTAL_RECORDS, 2)
        .toString();
    } else {
      if (endpoint == CommonEndpoint.FUND_CODES_EXPENSE_CLASSES
        && (StringUtils.contains(query, "fiscalYearCode==")
        || StringUtils.containsNone(query, "fiscalYearCode=="))) {
        responseBody = new JsonObject()
          .put(DELIMITER, COLON_DELIMITER)
          .put(dataKey, new JsonArray()
            .add(new JsonObject()
              .put("fundCode", FUND_CODE)
              .put("ledgerCode", LEDGER_CODE)
              .put("activeFundCodeVsExpClasses", new JsonArray())
              .put("inactiveFundCodeVsExpClasses", new JsonArray())))
          .put(TOTAL_RECORDS, 1)
          .toString();
      } else {
        responseBody = new JsonObject()
          .put(dataKey, new JsonArray()
            .add(new JsonObject().put(ID, UUID.randomUUID().toString()))
            .add(new JsonObject().put(ID, UUID.randomUUID().toString()))
            .add(new JsonObject().put(ID, UUID.randomUUID().toString())))
          .put(TOTAL_RECORDS, 3)
          .toString();
      }
    }

    ctx.response()
      .putHeader(CONTENT_TYPE, APPLICATION_JSON)
      .setStatusCode(SC_OK)
      .end(responseBody);
  }
}

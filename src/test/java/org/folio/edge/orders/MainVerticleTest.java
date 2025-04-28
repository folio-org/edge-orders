package org.folio.edge.orders;

import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.APPLICATION_XML;
import static org.folio.edge.core.Constants.MSG_ACCESS_DENIED;
import static org.folio.edge.core.Constants.MSG_INVALID_API_KEY;
import static org.folio.edge.core.Constants.MSG_REQUEST_TIMEOUT;
import static org.folio.edge.core.Constants.SYS_LOG_LEVEL;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.orders.Constants.API_CONFIGURATION_PROPERTY_NAME;
import static org.folio.edge.orders.utils.OrdersMockOkapi.BODY_REQUEST_FOR_HEADER_INCONSISTENCY;
import static org.folio.edge.orders.utils.OrdersMockOkapi.HAD_DATA_ID;
import static org.folio.edge.orders.utils.OrdersMockOkapi.NO_DATA_ID;
import static org.folio.edge.orders.utils.OrdersMockOkapi.TOTAL_RECORDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.ApiKeyUtils;
import org.folio.edge.core.utils.test.TestUtils;
import org.folio.edge.orders.Constants.ErrorCodes;
import org.folio.edge.orders.model.ErrorWrapper;
import org.folio.edge.orders.model.ResponseWrapper;
import org.folio.edge.orders.utils.OrdersMockOkapi;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

  private static final Logger logger = LogManager.getLogger(MainVerticleTest.class);

  private static final String API_KEY = ApiKeyUtils.generateApiKey(10, "diku", "diku");
  private static final String BAD_API_KEY = API_KEY + "0000";
  private static final String UNKNOWN_TENANT_API_KEY = ApiKeyUtils.generateApiKey(10, "bogus", "diku");
  private static final long REQUEST_TIMEOUT_MS = 3000L;
  private static final String MOD_ORDERS_STORAGE_VERSION = "mod-orders-storage-14.0.0-SNAPSHOT.999";

  private static OrdersMockOkapi mockOkapi;
  private static Map<String, String> mockRequests;

  @BeforeClass
  public static void setUpOnce(TestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();
    int serverPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(ApiKeyUtils.parseApiKey(API_KEY).tenantId);

    mockOkapi = spy(new OrdersMockOkapi(okapiPort, knownTenants));
    mockOkapi.start().onComplete(context.asyncAssertSuccess());

    Vertx vertx = Vertx.vertx();
    System.setProperty(SYS_PORT, String.valueOf(serverPort));
    System.setProperty(SYS_OKAPI_URL, "http://localhost:" + okapiPort);
    System.setProperty(SYS_SECURE_STORE_PROP_FILE, "src/main/resources/ephemeral.properties");
    System.setProperty(API_CONFIGURATION_PROPERTY_NAME, "src/main/resources/api_configuration.json");
    System.setProperty(Constants.MOD_ORDERS_STORAGE_VERSION, MOD_ORDERS_STORAGE_VERSION);
    System.setProperty(SYS_LOG_LEVEL, "TRACE");
    System.setProperty(SYS_REQUEST_TIMEOUT_MS, String.valueOf(REQUEST_TIMEOUT_MS));

    final DeploymentOptions opt = new DeploymentOptions();
    vertx.deployVerticle(MainVerticle.class.getName(), opt, context.asyncAssertSuccess());

    RestAssured.baseURI = "http://localhost:" + serverPort;
    RestAssured.port = serverPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

    mockRequests = new HashMap<>();

    File folder = new File("src/test/resources/requests");
    File[] listOfFiles = folder.listFiles();

    for (File listOfFile : Objects.requireNonNull(listOfFiles)) {
      String filename = listOfFile.getName();
      String woExt = filename.substring(0, filename.lastIndexOf("."));
      StringBuilder sb = new StringBuilder();
      try {
        try (Stream<String> linesStream = Files.lines(Paths.get(listOfFile.toURI()), StandardCharsets.UTF_8)) {
          linesStream.forEachOrdered(value -> sb.append(value).append('\n'));
          mockRequests.put(woExt, sb.toString());
        }
      } catch (IOException e) {
        logger.error("Exception loading mock requests", e);
      }
    }
  }

  @AfterClass
  public static void tearDownOnce(TestContext context) {
    logger.info("Shutting down server");
    mockOkapi.close().onComplete(context.asyncAssertSuccess());
  }

  // GOBI

  @Test
  public void testAdminHealth() {
    logger.info("=== Test the health check endpoint ===");

    final Response resp = RestAssured
      .get("/admin/health")
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
      .extract()
      .response();

    assertEquals("\"OK\"", resp.body().asString());
  }

  @Test
  public void testGetValidateSuccess() {
    logger.info("=== Test GET validate w/ valid key ===");

    RestAssured
      .get("/orders/validate?type=GOBI&apiKey=" + API_KEY)
      .then()
      .statusCode(200)
      .assertThat()
      .body(containsString("<test>GET - OK</test>"));
  }

  @Test
  public void testGetValidateSuccessWithEncoding() {
    logger.info("=== Test GET validate w/ valid key and Accept Encoding header===");

    RestAssured
      .with()
      .header(new Header(HttpHeaders.ACCEPT_ENCODING, "gzip deflate"))
      .get("/orders/validate?type=GOBI&apiKey=" + API_KEY)
      .then()
      .statusCode(200)
      .assertThat()
      .body(containsString("<test>GET - OK</test>"));
  }

  @Test
  public void testPostValidateSuccessIgnoreBody() {
    // EDGORDERS-15 - Ignore processing request body
    logger.info("=== Test POST validate w/ valid key and ignore processing request body ===");

    String purchaseOrder = "118279";
    String body = mockRequests.get(purchaseOrder);

    RestAssured
      .with().body(body)
      .post("/orders/validate?type=GOBI&apiKey=" + API_KEY)
      .then()
      .statusCode(200)
      .assertThat()
      .body(containsString("<test>POST - OK</test>"));
  }

  @Test
  public void testPostValidateSuccess() {
    logger.info("=== Test POST validate w/ valid key ===");

    RestAssured
      .post("/orders/validate?type=GOBI&apiKey=" + API_KEY)
      .then()
      .statusCode(200)
      .assertThat()
      .body(containsString("<test>POST - OK</test>"));
  }

  @Test
  public void testValidateNotFound() throws JsonProcessingException {
    logger.info("=== Test validate w/ module not found ===");

    final Response resp = RestAssured
      .with()
      .header("X-Echo-Status", 404)
      .get("/orders/validate?type=GOBI&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(404)
      .extract()
      .response();

    ResponseWrapper respBody = new ResponseWrapper(
      new ErrorWrapper(ErrorCodes.NOT_FOUND.toString(), "No suitable module found for path /gobi/validate"));
    assertEquals(respBody.toXml(), resp.body().asString());
  }

  @Test
  public void testValidateFailedToParse() throws JsonProcessingException {
    logger.info("=== Test validate w/ invalid key ===");

    final Response resp = RestAssured
      .get("/orders/validate?type=GOBI&apiKey=" + BAD_API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(401)
      .extract()
      .response();

    ResponseWrapper respBody = new ResponseWrapper(
      new ErrorWrapper(ErrorCodes.API_KEY_INVALID.name(), MSG_INVALID_API_KEY + ": " + BAD_API_KEY));
    assertEquals(respBody.toXml(), resp.body().asString());
  }

  @Test
  public void testValidateBadType() throws JsonProcessingException {
    logger.info("=== Test validate w/ bad type ===");

    final Response resp = RestAssured
      .get("/orders/validate?type=bogus&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(400)
      .extract()
      .response();

    ResponseWrapper respBody = new ResponseWrapper(
      new ErrorWrapper("BAD_REQUEST", "Unknown Purchasing System Specified: bogus"));
    assertEquals(respBody.toXml(), resp.body().asString());
  }

  @Test
  public void testValidateMissingType() throws JsonProcessingException {
    logger.info("=== Test validate w/ missing type ===");

    final Response resp = RestAssured
      .get("/orders/validate?apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(400)
      .extract()
      .response();

    ResponseWrapper respBody = new ResponseWrapper(
      new ErrorWrapper("BAD_REQUEST", "Missing required parameter: type"));
    assertEquals(respBody.toXml(), resp.body().asString());
  }

  @Test
  public void testValidateEmptyType() throws JsonProcessingException {
    logger.info("=== Test validate w/ empty type ===");

    final Response resp = RestAssured
      .get("/orders/validate?type=&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(400)
      .extract()
      .response();

    ResponseWrapper respBody = new ResponseWrapper(
      new ErrorWrapper("BAD_REQUEST", "Missing required parameter: type"));
    assertEquals(respBody.toXml(), resp.body().asString());
  }

  @Test
  public void testPlaceOrderSuccess() throws JsonProcessingException {
    logger.info("=== Test place order - Success (XML) ===");

    String purchaseOrder = "118279";
    String body = mockRequests.get(purchaseOrder);

    final Response resp = RestAssured
      .with()
      .body(body)
      .post("/orders?type=GOBI&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(201)
      .extract()
      .response();

    assertEquals(new ResponseWrapper("PO-" + purchaseOrder).toXml(), resp.body().asString());
  }

  @Test
  public void testPlaceOrderJson() throws JsonProcessingException {
    logger.info("=== Test place order - Success (JSON) ===");

    String purchaseOrder = "118279";
    String body = mockRequests.get(purchaseOrder);

    final Response resp = RestAssured
      .with()
      .accept(APPLICATION_JSON)
      .body(body)
      .post("/orders?type=GOBI&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(201)
      .extract()
      .response();

    assertEquals(new ResponseWrapper("PO-" + purchaseOrder).toJson(), resp.body().asString());
  }

  @Test
  public void testPlaceOrderXml() throws JsonProcessingException {
    logger.info("=== Test place order - Success (JSON) ===");

    String purchaseOrder = "118279";
    String body = mockRequests.get(purchaseOrder);

    final Response resp = RestAssured
      .with()
      .accept(APPLICATION_XML)
      .body(body)
      .post("/orders?type=GOBI&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(201)
      .extract()
      .response();

    assertEquals(new ResponseWrapper("PO-" + purchaseOrder).toXml(), resp.body().asString());
  }

  @Test
  public void testPlaceOrderBadType() throws JsonProcessingException {
    logger.info("=== Test place order - bad type argument ===");

    String purchaseOrder = "118279";
    String body = mockRequests.get(purchaseOrder);

    final Response resp = RestAssured
      .with()
      .body(body)
      .post("/orders?type=bogus&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(400)
      .extract()
      .response();

    ResponseWrapper respBody = new ResponseWrapper(
      new ErrorWrapper("BAD_REQUEST", "Unknown Purchasing System Specified: bogus"));
    assertEquals(respBody.toXml(), resp.body().asString());
  }

  @Test
  public void testPlaceOrderBadApiKey() throws JsonProcessingException {
    logger.info("=== Test place order - Bad API Key ===");

    String purchaseOrder = "118279";
    String body = mockRequests.get(purchaseOrder);

    final Response resp = RestAssured
      .with()
      .body(body)
      .post("/orders?type=GOBI&apiKey=" + BAD_API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(401)
      .extract()
      .response();

    ResponseWrapper expected = new ResponseWrapper(
      new ErrorWrapper(ErrorCodes.API_KEY_INVALID.name(), MSG_INVALID_API_KEY + ": " + BAD_API_KEY));
    assertEquals(expected.toXml(), resp.body().asString());
  }

  @Test
  public void testPlaceOrderMissingApiKey() throws JsonProcessingException {
    logger.info("=== Test place order - Missing API Key ===");

    String purchaseOrder = "118279";
    String body = mockRequests.get(purchaseOrder);

    final Response resp = RestAssured
      .with()
      .body(body)
      .post("/orders?type=GOBI&apiKey=")
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(401)
      .extract()
      .response();

    ResponseWrapper respBody = new ResponseWrapper(
      new ErrorWrapper(ErrorCodes.API_KEY_INVALID.name(), MSG_INVALID_API_KEY + ": "));
    assertEquals(respBody.toXml(), resp.body().asString());
  }

  @Test
  public void testNonExistingEndpoint() {
    logger.info("=== Test non existing endpoint ===");

    RestAssured
      .with()
      .post("/test" + UNKNOWN_TENANT_API_KEY)
      .then()
      .statusCode(404)
      .assertThat()
      .body(containsString("Resource not found"));
  }

  @Test
  public void testPlaceOrderUnknownTenant() throws JsonProcessingException {
    logger.info("=== Test place order - Unknown Tenant ===");

    String purchaseOrder = "118279";
    String body = mockRequests.get(purchaseOrder);

    final Response resp = RestAssured
      .with()
      .body(body)
      .post("/orders?type=GOBI&apiKey=" + UNKNOWN_TENANT_API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(401)
      .extract()
      .response();

    ResponseWrapper respBody = new ResponseWrapper(
      new ErrorWrapper("ACCESS_DENIED", MSG_ACCESS_DENIED));
    assertEquals(respBody.toXml(), resp.body().asString());
  }

  @Test
  public void testPlaceOrderTimeout() throws JsonProcessingException {
    logger.info("=== Test place order - Timeout ===");

    String purchaseOrder = "118279";
    String body = mockRequests.get(purchaseOrder);

    final Response resp = RestAssured
      .with()
      .header("X-Duration", REQUEST_TIMEOUT_MS + 1000)
      .body(body)
      .post("/orders?type=GOBI&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(408)
      .extract()
      .response();

    ResponseWrapper respBody = new ResponseWrapper(
      new ErrorWrapper("REQUEST_TIMEOUT", MSG_REQUEST_TIMEOUT));
    assertEquals(respBody.toXml(), resp.body().asString());
  }

  @Test
  public void testShouldReturnEmptyResponseIfResponseFromVendorIsEmpty() {
    String body = StringUtils.EMPTY;
    final Response resp = RestAssured
      .with()
      .body(body)
      .post("/orders?type=GOBI&apiKey=" + API_KEY)
      .then()
      .statusCode(201)
      .extract()
      .response();

    assertTrue(StringUtils.isEmpty(resp.body().asString()));
  }

  @Test
  public void testShouldReturnErrorWithInternalFormatIfAcceptAndContentHeaderAreDifferent() {
    RestAssured
      .with()
      .accept(APPLICATION_JSON)
      .body(BODY_REQUEST_FOR_HEADER_INCONSISTENCY)
      .post("/orders?type=GOBI&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(400)
      .extract()
      .response();
  }

  @Test
  public void testShouldReturnXMLErrorWithInternalFormatIfAcceptHeaderIsXML() {
    final Response resp = RestAssured
      .with()
      .accept(APPLICATION_XML)
      .body(BODY_REQUEST_FOR_HEADER_INCONSISTENCY)
      .post("/orders?type=GOBI&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(400)
      .extract()
      .response();
    Object error = resp.path("Error");

    assertNotNull(error);
  }

  @Test
  public void testShouldReturnJSONErrorWithInternalFormatIfAcceptHeaderIsJSON() {
    final Response resp = RestAssured
      .with()
      .accept(APPLICATION_JSON)
      .body(BODY_REQUEST_FOR_HEADER_INCONSISTENCY)
      .post("/orders?type=GOBI&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(400)
      .extract()
      .response();
    Object error = resp.path("Error");

    assertNotNull(error);
  }

  @Test
  public void testShouldReturnTextErrorWithInternalFormatIfAcceptHeaderIsText() {
    RestAssured
      .with()
      .accept(TEXT_PLAIN)
      .body(BODY_REQUEST_FOR_HEADER_INCONSISTENCY)
      .post("/orders?type=GOBI&apiKey=" + API_KEY)
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(400)
      .extract()
      .response();
  }

  @Test
  public void testShouldReturnXMLErrorWithInternalFormatIfAcceptHeaderIsAny() {
    final Response resp = RestAssured
      .with()
      .accept("*/*")
      .body(BODY_REQUEST_FOR_HEADER_INCONSISTENCY)
      .post("/orders?type=GOBI&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(400)
      .extract()
      .response();
    Object error = resp.path("Error");

    assertNotNull(error);
  }

  @Test
  public void testShouldReturnXMLErrorWithInternalFormatIfAcceptHeaderIsEmpty() {
    final Response resp = RestAssured
      .with()
      .body(BODY_REQUEST_FOR_HEADER_INCONSISTENCY)
      .post("/orders?type=GOBI&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(400)
      .extract()
      .response();
    Object error = resp.path("Error");

    assertNotNull(error);
  }

  @Test
  public void testShouldReturnJSONErrorWithInternalFormatIfAcceptHeaderIsJSONAndXML() {
    final Response resp = RestAssured
      .with()
      .accept(APPLICATION_JSON + "," + APPLICATION_XML)
      .body(BODY_REQUEST_FOR_HEADER_INCONSISTENCY)
      .post("/orders?type=GOBI&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(400)
      .extract()
      .response();
    Object error = resp.path("Error");

    assertNotNull(error);
  }

  @Test
  public void testShouldReturnXMLErrorWithInternalFormatIfAcceptHeaderIsNotUndefined() {
    final Response resp = RestAssured
      .with()
      .accept("text/xml")
      .body(BODY_REQUEST_FOR_HEADER_INCONSISTENCY)
      .post("/orders?type=GOBI&apiKey=" + API_KEY)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(400)
      .extract()
      .response();
    Object error = resp.path("Error");

    assertNotNull(error);
  }

  // MOSAIC

  @Test
  public void testShouldReturnMosaicEndpointData() {
    Arrays.stream(MosaicEndpoint.values())
      .forEach(endpoint -> {
        logger.info("testShouldReturnMosaicEndpointData:: Ingress: {}", endpoint.getIngressUrl());
        RestAssured
          .get(endpoint.getIngressUrl() + "?type=MOSAIC&apikey=" + API_KEY)
          .then()
          .contentType(APPLICATION_JSON)
          .statusCode(SC_OK)
          .body(endpoint.getDataKey(), notNullValue())
          .body(TOTAL_RECORDS, equalTo(3));
      });
  }

  @Test
  public void testShouldReturnMosaicEndpointDataWithOffsetAndLimit() {
    Arrays.stream(MosaicEndpoint.values())
      .filter(MosaicEndpoint::isHasFiltering)
      .forEach(endpoint -> {
        logger.info("testShouldReturnMosaicEndpointDataWithOffsetAndLimit:: Ingress: {}", endpoint.getIngressUrl());
        RestAssured
          .get(endpoint.getIngressUrl() + "?type=MOSAIC&apikey=" + API_KEY + "&offset=0&limit=2")
          .then()
          .contentType(APPLICATION_JSON)
          .statusCode(SC_OK)
          .body(endpoint.getDataKey(), notNullValue())
          .body(TOTAL_RECORDS, equalTo(2));
      });
  }

  @Test
  public void testShouldReturnMosaicEndpointDataWithQuery() {
    Arrays.stream(MosaicEndpoint.values())
      .filter(MosaicEndpoint::isHasFiltering)
      .forEach(endpoint -> {
        logger.info("testShouldReturnMosaicEndpointDataWithQuery:: Ingress: {}", endpoint.getIngressUrl());
        RestAssured
          .get(endpoint.getIngressUrl() + "?type=MOSAIC&apikey=" + API_KEY + "&query=id==" + HAD_DATA_ID)
          .then()
          .contentType(APPLICATION_JSON)
          .statusCode(SC_OK)
          .body(endpoint.getDataKey(), notNullValue())
          .body(TOTAL_RECORDS, equalTo(1));
      });
  }

  @Test
  public void testShouldReturnMosaicEndpointDataWithQueryNoData() {
    Arrays.stream(MosaicEndpoint.values())
      .filter(MosaicEndpoint::isHasFiltering)
      .forEach(endpoint -> {
        logger.info("testShouldReturnMosaicEndpointDataWithQueryNoData:: Ingress: {}", endpoint.getIngressUrl());
        RestAssured
          .get(endpoint.getIngressUrl() + "?type=MOSAIC&apikey=" + API_KEY + "&query=id==" + NO_DATA_ID)
          .then()
          .contentType(APPLICATION_JSON)
          .statusCode(SC_OK)
          .body(endpoint.getDataKey(), notNullValue())
          .body(TOTAL_RECORDS, equalTo(0));
      });
  }

  @Test
  public void testShouldReturnMosaicEndpointDataWithEmptyQuery() {
    Arrays.stream(MosaicEndpoint.values())
      .filter(MosaicEndpoint::isHasFiltering)
      .forEach(endpoint -> {
        logger.info("testShouldReturnMosaicEndpointDataWithEmptyQuery:: Ingress: {}", endpoint.getIngressUrl());
        RestAssured
          .get(endpoint.getIngressUrl() + "?type=MOSAIC&apikey=" + API_KEY + "&query=")
          .then()
          .contentType(APPLICATION_JSON)
          .statusCode(SC_OK)
          .body(endpoint.getDataKey(), notNullValue())
          .body(TOTAL_RECORDS, equalTo(3));
      });
  }
}

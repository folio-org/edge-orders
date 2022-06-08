package org.folio.edge.orders;

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
import static org.folio.edge.core.utils.test.MockOkapi.X_DURATION;
import static org.folio.edge.core.utils.test.MockOkapi.X_ECHO_STATUS;
import static org.folio.edge.orders.Constants.API_CONFIGURATION_PROPERTY_NAME;
import static org.folio.edge.orders.utils.OrdersMockOkapi.BODY_REQUEST_FOR_HEADER_INCONSISTENCY;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.cache.TokenCache;
import org.folio.edge.core.model.ClientInfo;
import org.folio.edge.core.utils.ApiKeyUtils;
import org.folio.edge.core.utils.ApiKeyUtils.MalformedApiKeyException;
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

  private static final String apiKey = ApiKeyUtils.generateApiKey(10, "diku", "diku");
  private static final String badApiKey = apiKey + "0000";
  private static final String unknownTenantApiKey = ApiKeyUtils.generateApiKey(10, "bogus", "diku");

  private static final long requestTimeoutMs = 3000L;

  private static Vertx vertx;
  private static OrdersMockOkapi mockOkapi;

  private static Map<String, String> mockRequests;

  @BeforeClass
  public static void setUpOnce(TestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();
    int serverPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(ApiKeyUtils.parseApiKey(apiKey).tenantId);

    mockOkapi = spy(new OrdersMockOkapi(okapiPort, knownTenants));
    mockOkapi.start(context);

    vertx = Vertx.vertx();
    System.setProperty(SYS_PORT, String.valueOf(serverPort));
    System.setProperty(SYS_OKAPI_URL, "http://localhost:" + okapiPort);
    System.setProperty(SYS_SECURE_STORE_PROP_FILE, "src/main/resources/ephemeral.properties");
    System.setProperty(API_CONFIGURATION_PROPERTY_NAME, "src/main/resources/api_configuration.json");
    System.setProperty(SYS_LOG_LEVEL, "TRACE");
    System.setProperty(SYS_REQUEST_TIMEOUT_MS, String.valueOf(requestTimeoutMs));

    final DeploymentOptions opt = new DeploymentOptions();
    vertx.deployVerticle(MainVerticle.class.getName(), opt, context.asyncAssertSuccess());

    RestAssured.baseURI = "http://localhost:" + serverPort;
    RestAssured.port = serverPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

    mockRequests = new HashMap<>();

    File folder = new File("src/test/resources/requests");
    File[] listOfFiles = folder.listFiles();

    for (File listOfFile : listOfFiles) {
      String filename = listOfFile.getName();
      String woExt = filename.substring(0, filename.lastIndexOf("."));
      StringBuilder sb = new StringBuilder();
      try {
        Files.lines(Paths.get(listOfFile.toURI()), StandardCharsets.UTF_8)
          .forEachOrdered(value -> sb.append(value).append('\n'));
        mockRequests.put(woExt, sb.toString());
      } catch (IOException e) {
        logger.error("Exception loading mock requests", e);
      }
    }
  }

  @AfterClass
  public static void tearDownOnce(TestContext context) {
    logger.info("Shutting down server");
    vertx.close(res -> {
      if (res.failed()) {
        logger.error("Failed to shut down edge-orders server", res.cause());
        fail(res.cause().getMessage());
      } else {
        logger.info("Successfully shut down edge-orders server");
      }

      logger.info("Shutting down mock Okapi");
      mockOkapi.close(context);
    });
  }

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
      .get("/orders/validate?type=GOBI&apiKey=" + apiKey)
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
      .get("/orders/validate?type=GOBI&apiKey=" + apiKey)
      .then()
      .statusCode(200)
      .assertThat()
      .body(containsString("<test>GET - OK</test>"));
  }

  @Test
  public void testPostValidateSuccessIgnoreBody() {
  	// EDGORDERS-15 - Ignore processing request body
    logger.info("=== Test POST validate w/ valid key and ignore processing request body ===");

    String PO = "118279";
    String body = mockRequests.get(PO);

    RestAssured
    .with().body(body)
      .post("/orders/validate?type=GOBI&apiKey=" + apiKey)
      .then()
      .statusCode(200)
      .assertThat()
      .body(containsString("<test>POST - OK</test>"));
  }

  @Test
  public void testPostValidateSuccess() {
    logger.info("=== Test POST validate w/ valid key ===");

    RestAssured
      .post("/orders/validate?type=GOBI&apiKey=" + apiKey)
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
      .header(X_ECHO_STATUS, 404)
      .get("/orders/validate?type=GOBI&apiKey=" + apiKey)
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
      .get("/orders/validate?type=GOBI&apiKey=" + badApiKey)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(401)
      .extract()
      .response();

    ResponseWrapper respBody = new ResponseWrapper(
        new ErrorWrapper(ErrorCodes.API_KEY_INVALID.name(), MSG_INVALID_API_KEY + ": " + badApiKey));
    assertEquals(respBody.toXml(), resp.body().asString());
  }

  @Test
  public void testValidateBadType() throws JsonProcessingException {
    logger.info("=== Test validate w/ bad type ===");

    final Response resp = RestAssured
      .get("/orders/validate?type=bogus&apiKey=" + apiKey)
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
      .get("/orders/validate?apiKey=" + apiKey)
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
      .get("/orders/validate?type=&apiKey=" + apiKey)
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

    String PO = "118279";
    String body = mockRequests.get(PO);

    final Response resp = RestAssured
      .with()
      .body(body)
      .post("/orders?type=GOBI&apiKey=" + apiKey)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(201)
      .extract()
      .response();

    assertEquals(new ResponseWrapper("PO-" + PO).toXml(), resp.body().asString());
  }

  @Test
  public void testPlaceOrderJson() throws JsonProcessingException {
    logger.info("=== Test place order - Success (JSON) ===");

    String PO = "118279";
    String body = mockRequests.get(PO);

    final Response resp = RestAssured
      .with()
      .accept(APPLICATION_JSON)
      .body(body)
      .post("/orders?type=GOBI&apiKey=" + apiKey)
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(201)
      .extract()
      .response();

    assertEquals(new ResponseWrapper("PO-" + PO).toJson(), resp.body().asString());
  }

  @Test
  public void testPlaceOrderXml() throws JsonProcessingException {
    logger.info("=== Test place order - Success (JSON) ===");

    String PO = "118279";
    String body = mockRequests.get(PO);

    final Response resp = RestAssured
      .with()
      .accept(APPLICATION_XML)
      .body(body)
      .post("/orders?type=GOBI&apiKey=" + apiKey)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(201)
      .extract()
      .response();

    assertEquals(new ResponseWrapper("PO-" + PO).toXml(), resp.body().asString());
  }

  @Test
  public void testPlaceOrderBadType() throws JsonProcessingException {
    logger.info("=== Test place order - bad type argument ===");

    String PO = "118279";
    String body = mockRequests.get(PO);

    final Response resp = RestAssured
      .with()
      .body(body)
      .post("/orders?type=bogus&apiKey=" + apiKey)
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

    String PO = "118279";
    String body = mockRequests.get(PO);

    final Response resp = RestAssured
      .with()
      .body(body)
      .post("/orders?type=GOBI&apiKey=" + badApiKey)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(401)
      .extract()
      .response();

    ResponseWrapper expected = new ResponseWrapper(
        new ErrorWrapper(ErrorCodes.API_KEY_INVALID.name(), MSG_INVALID_API_KEY + ": " + badApiKey));
    assertEquals(expected.toXml(), resp.body().asString());
  }

  @Test
  public void testPlaceOrderMissingApiKey() throws JsonProcessingException {
    logger.info("=== Test place order - Missing API Key ===");

    String PO = "118279";
    String body = mockRequests.get(PO);

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
      .post("/test" + unknownTenantApiKey)
      .then()
      .statusCode(404)
      .assertThat()
      .body(containsString("Resource not found"));
  }

  @Test
  public void testPlaceOrderUnknownTenant() throws JsonProcessingException {
    logger.info("=== Test place order - Unknown Tenant ===");

    String PO = "118279";
    String body = mockRequests.get(PO);

    final Response resp = RestAssured
      .with()
      .body(body)
      .post("/orders?type=GOBI&apiKey=" + unknownTenantApiKey)
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
  public void testPlaceOrderTimeout(TestContext context) throws MalformedApiKeyException, JsonProcessingException {
    logger.info("=== Test place order - Timeout ===");

    String PO = "118279";
    String body = mockRequests.get(PO);

    ClientInfo clientInfo = ApiKeyUtils.parseApiKey(apiKey);
    TokenCache.getInstance().put(clientInfo.salt, clientInfo.tenantId, clientInfo.username, null);

    final Response resp = RestAssured
      .with()
      .header(X_DURATION, requestTimeoutMs + 1000)
      .body(body)
      .post("/orders?type=GOBI&apiKey=" + apiKey)
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
      .post("/orders?type=GOBI&apiKey=" + apiKey)
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
      .post("/orders?type=GOBI&apiKey=" + apiKey)
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
      .post("/orders?type=GOBI&apiKey=" + apiKey)
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
      .post("/orders?type=GOBI&apiKey=" + apiKey)
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
      .post("/orders?type=GOBI&apiKey=" + apiKey)
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
      .post("/orders?type=GOBI&apiKey=" + apiKey)
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
      .post("/orders?type=GOBI&apiKey=" + apiKey)
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
      .accept(APPLICATION_JSON+","+APPLICATION_XML)
      .body(BODY_REQUEST_FOR_HEADER_INCONSISTENCY)
      .post("/orders?type=GOBI&apiKey=" + apiKey)
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(400)
      .extract()
      .response();
    Object error = resp.path("Error");

    assertNotNull(error);
  }

  @Test
  public void testShouldReturnXMLErrorWithInternalFormatIfAcceptHeaderIsNotIndefined() {
    final Response resp = RestAssured
      .with()
      .accept("text/xml")
      .body(BODY_REQUEST_FOR_HEADER_INCONSISTENCY)
      .post("/orders?type=GOBI&apiKey=" + apiKey)
      .then()
      .contentType(APPLICATION_XML)
      .statusCode(400)
      .extract()
      .response();
    Object error = resp.path("Error");

    assertNotNull(error);
  }
}

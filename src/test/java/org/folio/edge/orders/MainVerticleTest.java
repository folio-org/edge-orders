package org.folio.edge.orders;

import static org.folio.edge.core.Constants.APPLICATION_XML;
import static org.folio.edge.core.Constants.MSG_ACCESS_DENIED;
import static org.folio.edge.core.Constants.MSG_REQUEST_TIMEOUT;
import static org.folio.edge.core.Constants.SYS_LOG_LEVEL;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.core.utils.test.MockOkapi.X_DURATION;
import static org.folio.edge.core.utils.test.MockOkapi.X_ECHO_STATUS;
import static org.junit.Assert.assertEquals;
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

import org.apache.http.HttpHeaders;
import org.apache.log4j.Logger;
import org.folio.edge.core.InstitutionalUserHelper;
import org.folio.edge.core.InstitutionalUserHelper.MalformedApiKeyException;
import org.folio.edge.core.cache.TokenCache;
import org.folio.edge.core.model.ClientInfo;
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
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

  private static final Logger logger = Logger.getLogger(MainVerticleTest.class);

  private static final String apiKey = "Z1luMHVGdjNMZl9kaWt1X2Rpa3U=";
  private static final String badApiKey = "ZnMwMDAwMDAwMA==0000";
  private static final String unknownTenantApiKey = "Z1luMHVGdjNMZl9ib2d1c19ib2d1cw==";

  private static final long requestTimeoutMs = 3000L;

  private static Vertx vertx;
  private static OrdersMockOkapi mockOkapi;

  private static Map<String, String> mockRequests;

  @BeforeClass
  public static void setUpOnce(TestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();
    int serverPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(InstitutionalUserHelper.parseApiKey(apiKey).tenantId);

    mockOkapi = spy(new OrdersMockOkapi(okapiPort, knownTenants));
    mockOkapi.start(context);

    vertx = Vertx.vertx();

    System.setProperty(SYS_PORT, String.valueOf(serverPort));
    System.setProperty(SYS_OKAPI_URL, "http://localhost:" + okapiPort);
    System.setProperty(SYS_SECURE_STORE_PROP_FILE, "src/main/resources/ephemeral.properties");
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

    for (int i = 0; i < listOfFiles.length; i++) {
      String filename = listOfFiles[i].getName();
      String woExt = filename.substring(0, filename.lastIndexOf("."));
      StringBuilder sb = new StringBuilder();
      try {
        Files.lines(Paths.get(listOfFiles[i].toURI()), StandardCharsets.UTF_8)
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
      mockOkapi.close();
    });
  }

  @Test
  public void testAdminHealth(TestContext context) {
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
  public void testValidateSuccess(TestContext context) {
    logger.info("=== Test validate w/ valid key ===");

    RestAssured
      .get("/orders/validate?type=GOBI&apiKey=" + apiKey)
      .then()
      .statusCode(204);
  }

  @Test
  public void testValidateNotFound(TestContext context) throws JsonProcessingException {
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
  public void testValidateFailedToParse(TestContext context) throws JsonProcessingException {
    logger.info("=== Test validate w/ invalid key ===");

    final Response resp = RestAssured
      .get("/orders/validate?type=GOBI&apiKey=" + badApiKey)
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
  public void testValidateBadType(TestContext context) throws JsonProcessingException {
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
  public void testValidateMissingType(TestContext context) throws JsonProcessingException {
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
  public void testValidateEmptyType(TestContext context) throws JsonProcessingException {
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
  public void testPlaceOrderSuccess(TestContext context) throws JsonProcessingException {
    logger.info("=== Test place order - Success ===");

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
  public void testPlaceOrderBadType(TestContext context) throws JsonProcessingException {
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
  public void testPlaceOrderBadApiKey(TestContext context) throws JsonProcessingException {
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
        new ErrorWrapper("ACCESS_DENIED", MSG_ACCESS_DENIED));
    assertEquals(expected.toXml(), resp.body().asString());
  }

  @Test
  public void testPlaceOrderMissingApiKey(TestContext context) throws JsonProcessingException {
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
        new ErrorWrapper("ACCESS_DENIED", MSG_ACCESS_DENIED));
    assertEquals(respBody.toXml(), resp.body().asString());
  }

  @Test
  public void testPlaceOrderUnknownTenant(TestContext context) throws JsonProcessingException {
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

    ClientInfo clientInfo = InstitutionalUserHelper.parseApiKey(apiKey);
    TokenCache.getInstance().put(clientInfo.clientId, clientInfo.tenantId, clientInfo.username, null);

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
}

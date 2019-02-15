package org.folio.edge.orders.utils;

import static org.folio.edge.core.Constants.APPLICATION_XML;
import static org.folio.edge.core.utils.test.MockOkapi.MOCK_TOKEN;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.log4j.Logger;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class OrdersOkapiClientTest {

  private static final Logger logger = Logger.getLogger(OrdersOkapiClientTest.class);

  private static final String tenant = "diku";
  private static final long reqTimeout = 3000L;

  private static Map<String, String> mockRequests;

  private OrdersOkapiClient client;
  private OrdersMockOkapi mockOkapi;

  @Before
  public void setUp(TestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(tenant);

    mockOkapi = new OrdersMockOkapi(okapiPort, knownTenants);
    mockOkapi.start(context);

    client = new OrdersOkapiClientFactory(Vertx.vertx(),
        "http://localhost:" + okapiPort, reqTimeout)
          .getOrdersOkapiClient(tenant);

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

  @After
  public void tearDown(TestContext context) {
    client.close();
    mockOkapi.close(context);
  }

  @Test
  public void testValidateGetGobiSuccess(TestContext context) {
    logger.info("=== Test successful GET validate ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      // Redundant - also checked in testLogin(...), but can't hurt
      context.assertEquals(MOCK_TOKEN, client.getToken());

      client.validateGobi(HttpMethod.GET,
          null,
          resp -> {
            context.assertEquals(200, resp.statusCode());
            async.complete();
          },
          t -> {
            context.fail(t.getMessage());
          });
    });
  }

  @Test
  public void testValidatePostGobiSuccess(TestContext context) {
    logger.info("=== Test successful POST validate ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      // Redundant - also checked in testLogin(...), but can't hurt
      context.assertEquals(MOCK_TOKEN, client.getToken());

      client.validateGobi(HttpMethod.POST,
          null,
          resp -> {
            context.assertEquals(200, resp.statusCode());
            async.complete();
          },
          t -> {
            context.fail(t.getMessage());
          });
    });
  }

  @Test
  public void testValidateUnsupportedPurchasingSystem(TestContext context) {
    logger.info("=== Test successful GET validate ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      // Redundant - also checked in testLogin(...), but can't hurt
      context.assertEquals(MOCK_TOKEN, client.getToken());

      client.validate(
          HttpMethod.GET,
          null,
          null,
          resp -> {
            context.fail("Expected a NotImplementedException to be thrown");
          },
          t -> {
            context.assertEquals(NotImplementedException.class, t.getClass());
            async.complete();
          });
    });
  }

  @Test
  public void testPlaceGobiOrderSuccess(TestContext context) {
    logger.info("=== Test successful GOBI order placement ===");

    String PO = mockRequests.keySet().iterator().next();
    String reqBody = mockRequests.get(PO);
    String expected = OrdersMockOkapi.getGobiOrderAsXml("PO-" + PO);

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      // Redundant - also checked in testLogin(...), but can't hurt
      context.assertEquals(MOCK_TOKEN, client.getToken());

      client.placeGobiOrder(reqBody,
          resp -> resp.bodyHandler(actual -> {
            logger.info("mod-gobi response body: " + actual);
            context.assertEquals(201, resp.statusCode());
            context.assertEquals(APPLICATION_XML, resp.getHeader(HttpHeaders.CONTENT_TYPE));
            context.assertEquals(expected, actual.toString());
            async.complete();
          }),
          t -> {
            context.fail(t.getMessage());
          });
    });
  }

  @Test
  public void testPlaceOrderUnsupportedPurchasingSystem(TestContext context) {
    logger.info("=== Test successful GOBI order placement ===");

    String PO = mockRequests.keySet().iterator().next();
    String reqBody = mockRequests.get(PO);

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      // Redundant - also checked in testLogin(...), but can't hurt
      context.assertEquals(MOCK_TOKEN, client.getToken());

      client.placeOrder(null,
          reqBody,
          null,
          resp -> {
            context.fail("Expected a NotImplementedException to be thrown");
          },
          t -> {
            context.assertEquals(NotImplementedException.class, t.getClass());
            async.complete();
          });
    });
  }

}

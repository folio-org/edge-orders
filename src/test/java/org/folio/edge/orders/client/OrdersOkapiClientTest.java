package org.folio.edge.orders.client;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.OkapiClientFactory;
import org.folio.edge.core.utils.test.TestUtils;
import org.folio.rest.mappings.model.Routing;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.folio.edge.core.utils.test.MockOkapi.MOCK_TOKEN;

@RunWith(VertxUnitRunner.class)
public class OrdersOkapiClientTest {

  private static final Logger logger = LogManager.getLogger(OrdersOkapiClientTest.class);

  private static final String TENANT = "diku";
  private static final int REQ_TIMEOUT = 3000;

  private AcquisitionsOkapiClient client;
  private OrdersMockOkapi mockOkapi;

  @Before
  public void setUp(TestContext context) {
    int okapiPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(TENANT);

    mockOkapi = new OrdersMockOkapi(okapiPort, knownTenants);
    mockOkapi.start().onComplete(context.asyncAssertSuccess());

    client = new AcquisitionsOkapiClient(new OkapiClientFactory(Vertx.vertx(),
      "http://localhost:" + okapiPort, REQ_TIMEOUT).getOkapiClient(TENANT));

    Map<String, String> mockRequests = new HashMap<>();

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

  @After
  public void tearDown(TestContext context) {
    client.client.close();
    mockOkapi.close().onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testValidateGetGobiSuccess(TestContext context) {
    logger.info("=== Test successful GET validate ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      // Redundant - also checked in testLogin(...), but can't hurt
      context.assertEquals(MOCK_TOKEN, client.getToken());

      Routing routing = new Routing();
      routing.setMethod("GET");
      routing.setPathPattern("/orders/validate");
      routing.setProxyPath("/gobi/validate");

      client.send(routing,
        "",null,null,
        resp -> {
          context.assertEquals(200, resp.statusCode());
          async.complete();
        },
        t -> context.fail(t.getMessage()));
    });
  }

  @Test
  public void testValidatePostGobiSuccess(TestContext context) {
    logger.info("=== Test successful POST validate ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      // Redundant - also checked in testLogin(...), but can't hurt
      context.assertEquals(MOCK_TOKEN, client.getToken());
      Routing routing = new Routing();
      routing.setMethod("POST");
      routing.setPathPattern("/orders/validate");
      routing.setProxyPath("/gobi/validate");

      client.send(routing,
        "",null, null,
        resp -> {
          context.assertEquals(200, resp.statusCode());
          async.complete();
        },
        t -> context.fail(t.getMessage()));
    });
  }

  @Test
  public void testPutEbsconetSuccess(TestContext context) {
    logger.info("=== Test successful PUT ebsconet order-line with id===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      context.assertEquals(MOCK_TOKEN, client.getToken());
      Routing routing = new Routing();
      routing.setMethod("PUT");
      routing.setPathPattern("/orders/order-lines/:id");
      routing.setProxyPath("/ebsconet/order-lines/:id");
      MultiMap entries = new HeadersMultiMap();
      entries.add("id", "123");

      client.send(routing, "", entries, null, resp -> {
        context.assertEquals(204, resp.statusCode());
        async.complete();
      },
      t -> context.fail(t.getMessage()));
    });
  }
}

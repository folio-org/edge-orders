package org.folio.edge.orders.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.folio.edge.orders.utils.OrdersOkapiClient;
import org.folio.edge.orders.utils.OrdersOkapiClientFactory;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Vertx;

public class OrdersOkapiClientFactoryTest {

  private static final long reqTimeout = 5000L;

  private OrdersOkapiClientFactory ocf;

  @Before
  public void setUp() throws Exception {

    Vertx vertx = Vertx.vertx();
    ocf = new OrdersOkapiClientFactory(vertx, "http://mocked.okapi:9130", reqTimeout);
  }

  @Test
  public void testGetOkapiClient() {
    OrdersOkapiClient client = ocf.getOrdersOkapiClient("tenant");
    assertNotNull(client);
    assertEquals(reqTimeout, client.reqTimeout);
  }

}

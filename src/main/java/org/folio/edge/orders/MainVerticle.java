package org.folio.edge.orders;

import org.folio.edge.core.EdgeVerticle;
import org.folio.edge.orders.utils.OrdersOkapiClientFactory;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends EdgeVerticle {

  public MainVerticle() {
    super();
  }

  @Override
  public Router defineRoutes() {
    OrdersOkapiClientFactory ocf = new OrdersOkapiClientFactory(vertx, okapiURL, reqTimeoutMs);

    OrdersHandler ordersHandler = new OrdersHandler(secureStore, ocf);
    ValidateHandler validateHandler = new ValidateHandler(secureStore, ocf);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.route(HttpMethod.GET, "/admin/health").handler(this::handleHealthCheck);
    router.route(HttpMethod.GET, "/orders/validate").handler(validateHandler::handle);
    router.route(HttpMethod.POST, "/orders/validate").handler(validateHandler::handlePost);
    router.route(HttpMethod.POST, "/orders").handler(ordersHandler::handle);
    return router;
  }
}

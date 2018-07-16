package org.folio.edge.orders;

import static org.folio.edge.orders.Constants.PARAM_TYPE;

import org.apache.log4j.Logger;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.orders.Constants.PurchasingSystems;
import org.folio.edge.orders.utils.OrdersOkapiClient;
import org.folio.edge.orders.utils.OrdersOkapiClientFactory;

import io.vertx.ext.web.RoutingContext;

public class ValidateHandler extends OrdersHandler {

  private static final Logger logger = Logger.getLogger(ValidateHandler.class);

  public ValidateHandler(SecureStore secureStore, OrdersOkapiClientFactory ocf) {
    super(secureStore, ocf);
  }

  @Override
  protected void handle(RoutingContext ctx) {
    handleCommon(ctx,
        new String[] {},
        new String[] {},
        (client, params) -> {
          PurchasingSystems ps = PurchasingSystems.fromValue(params.get(PARAM_TYPE));
          logger.info("Request is from purchasing system: " + ps.toString());
          ((OrdersOkapiClient) client).validate(
              ps,
              ctx.request().headers(),
              resp -> handleProxyResponse(ps, ctx, resp),
              t -> handleProxyException(ctx, t));
        });
  }
}

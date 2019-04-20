package org.folio.edge.orders;

import static org.folio.edge.orders.Constants.PARAM_TYPE;

import io.vertx.ext.web.RoutingContext;
import org.apache.log4j.Logger;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.orders.Constants.PurchasingSystems;
import org.folio.edge.orders.utils.OrdersOkapiClient;
import org.folio.edge.orders.utils.OrdersOkapiClientFactory;

public class ValidateHandler extends OrdersHandler {

	private static final String CONTENT_LENGTH_ZERO = "0";
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

          // EDGORDERS-15: Set request header content-length to zero to ignore processing body
          ctx.request().headers().set("Content-Length", CONTENT_LENGTH_ZERO);
          ((OrdersOkapiClient) client).validate(ctx.request().method(),
              ps,
              ctx.request().headers(),
              resp -> handleProxyResponse(ps, ctx, resp),
              t -> handleProxyException(ctx, t));
        });
  }
}

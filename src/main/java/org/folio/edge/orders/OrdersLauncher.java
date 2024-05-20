package org.folio.edge.orders;

import io.vertx.core.Launcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

import java.security.Security;

public class OrdersLauncher extends Launcher {
  private static final Logger logger = LogManager.getLogger(OrdersLauncher.class);

  public static void main(String[] args) {
    if (Security.getProvider(BouncyCastleFipsProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleFipsProvider());
      logger.info("BouncyCastleFipsProvider has been added");
    }
    new OrdersLauncher().dispatch(args);
  }
}

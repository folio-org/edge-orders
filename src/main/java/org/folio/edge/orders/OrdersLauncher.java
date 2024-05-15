package org.folio.edge.orders;

import io.vertx.core.Launcher;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

import java.security.Security;

public class OrdersLauncher extends Launcher {
  public static void main(String[] args) {
    Security.addProvider(new BouncyCastleFipsProvider());
    new OrdersLauncher().dispatch(args);
  }
}

package org.folio.edge.orders;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.folio.edge.orders.Constants.API_CONFIGURATION_DEFAULT;
import static org.folio.edge.orders.Constants.API_CONFIGURATION_PROPERTY_NAME;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.folio.edge.core.EdgeVerticle;
import org.folio.edge.orders.utils.OrdersOkapiClientFactory;
import org.folio.rest.mappings.model.ApiConfiguration;
import org.folio.rest.mappings.model.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends EdgeVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  public MainVerticle() {
    super();
  }

  @Override
  public Router defineRoutes() {
    OrdersOkapiClientFactory ocf = new OrdersOkapiClientFactory(vertx, okapiURL, reqTimeoutMs);

    OrdersHandler ordersHandler = new OrdersHandler(secureStore, ocf);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.route(HttpMethod.GET, "/admin/health").handler(this::handleHealthCheck);

    try {
      final List<Routing> routingConfiguration = initApiConfiguration(System.getProperty(API_CONFIGURATION_PROPERTY_NAME));

      Map<String, List<Routing>> groupedRouting = routingConfiguration.stream()
        .collect(groupingBy(Routing::getPathPattern));

      groupedRouting.forEach((path, routing) -> {
        Set<String> routingMethods = routing.stream().map(Routing::getMethod).collect(toSet());
        routingMethods.forEach(method -> router.route(HttpMethod.valueOf(method), path)
          .handler(ctx -> ordersHandler.handle(ctx, routingConfiguration)));
      });

    } catch (Exception e) {
      logger.error("Failed to load api configuration", e);
    }

    router.route().handler(ctx -> {
      String path = ctx.normalisedPath();
      logger.warn("Current path {} is missing from API configuration", path);
      ctx.next();
    });

    return router;
  }

  private List<Routing> initApiConfiguration(String apiConfigurationPropFile) throws Exception {

    ApiConfiguration apiConfiguration = null;
    final Pattern isURL = Pattern.compile("(?i)^http[s]?://.*");
    ObjectMapper mapper = new ObjectMapper();

    if (apiConfigurationPropFile != null) {
      URL url = null;
      try {
        if (isURL.matcher(apiConfigurationPropFile).matches()) {
          url = new URL(apiConfigurationPropFile);
        }

        try (InputStream in = url == null ? new FileInputStream(apiConfigurationPropFile) : url.openStream()) {

          apiConfiguration = mapper.readValue(in, ApiConfiguration.class);
          logger.info("ApiConfiguration has been loaded from file {}", apiConfigurationPropFile);
        }
      } catch (Exception e) {
        logger.warn("Failed to load ApiConfiguration from {}", apiConfigurationPropFile, e);
      }
    } else {
      logger.warn("No api configuration file specified. Using default {}", API_CONFIGURATION_DEFAULT);
      apiConfiguration = mapper
        .readValue(ClassLoader.getSystemClassLoader().getResource(API_CONFIGURATION_DEFAULT), ApiConfiguration.class);
    }

    return Objects.requireNonNull(apiConfiguration).getRouting();
  }
}

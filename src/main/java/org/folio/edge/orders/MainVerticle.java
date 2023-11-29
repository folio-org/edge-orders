package org.folio.edge.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.folio.edge.core.EdgeVerticleHttp;
import org.folio.edge.orders.utils.OrdersOkapiClientFactory;
import org.folio.rest.mappings.model.ApiConfiguration;
import org.folio.rest.mappings.model.Routing;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.folio.edge.orders.Constants.API_CONFIGURATION_DEFAULT;
import static org.folio.edge.orders.Constants.API_CONFIGURATION_PROPERTY_NAME;

public class MainVerticle extends EdgeVerticleHttp {

  private static final Logger logger = LogManager.getLogger(MainVerticle.class);

  public MainVerticle() {
    super();
  }

  @Override
  public Router defineRoutes() {
    logger.debug("defineRoutes:: Trying to define routes");
    OrdersOkapiClientFactory ocf = new OrdersOkapiClientFactory(vertx, config().getString(org.folio.edge.core.Constants.SYS_OKAPI_URL),
      config().getInteger(org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS));

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
      String path = ctx.normalizedPath();
      logger.warn("Current path '{}' is missing from API configuration", path);
      ctx.next();
    });

    return router;
  }

  private List<Routing> initApiConfiguration(String apiConfigurationPropFile) throws IOException {
    logger.debug("initApiConfiguration:: Initializing API configuration: {}", apiConfigurationPropFile);
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
          logger.info("initApiConfiguration:: ApiConfiguration has been loaded from file {}", apiConfigurationPropFile);
        }
      } catch (Exception e) {
        logger.warn("Failed to load ApiConfiguration from '{}'", apiConfigurationPropFile, e);
      }
    } else {
      logger.warn("No api configuration file specified. Using default '{}'", API_CONFIGURATION_DEFAULT);
      apiConfiguration = mapper
        .readValue(ClassLoader.getSystemClassLoader().getResource(API_CONFIGURATION_DEFAULT), ApiConfiguration.class);
    }

    return Objects.requireNonNull(apiConfiguration).getRouting();
  }
}

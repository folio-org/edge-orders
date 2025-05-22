package org.folio.edge.orders;

import io.vertx.core.MultiMap;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.mappings.model.Routing;

import java.util.Objects;
import java.util.Optional;

import static org.folio.edge.orders.Param.QUERY;

public class QueryUtil {

  private QueryUtil() {
  }

  public static String getRequestMethod(Routing routing) {
    return routing.getProxyMethod() == null ? routing.getMethod() : routing.getProxyMethod();
  }

  public static void addOrUpsertExtraQueryString(String extraQuery, MultiMap params) {
    if (StringUtils.isEmpty(extraQuery)) {
      return;
    }
    if (hasQueryStringPlaceHolder(extraQuery, QUERY) && !params.contains(QUERY.getName())) {
      params.add(QUERY.getName(), extraQuery);
    } else {
      String paramValue = params.get(QUERY.getName());
      params.set(QUERY.getName(), paramValue.concat(" and ").concat(extraQuery));
    }
  }

  public static String getProxyPath(String proxyPath, MultiMap params) {
    if (Objects.isNull(params)) {
      return proxyPath;
    }
    for (Param param : Param.values()) {
      if (checkPathHasQueryStringPlaceholderAndValue(proxyPath, params, param)) {
        proxyPath = param.isDefaultNonBlankValue() ? setDefaultValue(proxyPath, param) : fullyRemovePlaceholder(proxyPath, param);
      }
    }
    return proxyPath;
  }

  public static String getResultPath(MultiMap params, String proxyPath) {
    if (Optional.ofNullable(params).isEmpty()) {
      return proxyPath;
    }
    return QueryUtil.getNormalizedProxyPath(params, proxyPath);
  }

  public static String getNormalizedProxyPath(MultiMap params, String proxyPath) {
    return params.names().stream()
      .reduce(proxyPath, (aggregate, paramKey) -> aggregate.replace(":%s".formatted(paramKey), params.get(paramKey)))
      .replaceAll("\\s+", "+");
  }

  private static boolean checkPathHasQueryStringPlaceholderAndValue(String proxyPath, MultiMap params, Param param) {
    return hasQueryStringPlaceHolder(proxyPath, param) && emptyQueryStringValue(params, param);
  }

  private static boolean emptyQueryStringValue(MultiMap params, Param param) {
    return StringUtils.isEmpty(params.get(param.getName()));
  }

  private static boolean hasQueryStringPlaceHolder(String proxyPath, Param param) {
    return StringUtils.containsAny(proxyPath, ":%s".formatted(param.getName()));
  }

  private static String setDefaultValue(String proxyPath, Param param) {
    return proxyPath.replace(":%s".formatted(param.getName()), param.getDefaultValue());
  }

  private static String fullyRemovePlaceholder(String proxyPath, Param param) {
    String placeholderPattern = "(\\?|&)(%1$s=:%1$s)".formatted(param.getName());
    return proxyPath.replaceAll(placeholderPattern, "");
  }
}

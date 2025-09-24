package org.folio.edge.orders;

import static org.folio.edge.orders.Param.LIMIT;
import static org.folio.edge.orders.Param.OFFSET;
import static org.folio.edge.orders.Param.QUERY;
import static org.junit.Assert.*;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import org.folio.rest.mappings.model.Routing;
import org.junit.Test;

public class QueryUtilTest {

  // getRequestMethod

  @Test
  public void testGetRequestMethod_withProxyMethod() {
    var routing = new Routing();
    routing.setMethod("GET");
    routing.setProxyMethod("POST");
    var result = QueryUtil.getRequestMethod(routing);
    assertEquals("POST", result);
  }

  @Test
  public void testGetRequestMethod_withoutProxyMethod() {
    var routing = new Routing();
    routing.setMethod("GET");
    routing.setProxyMethod(null);
    var result = QueryUtil.getRequestMethod(routing);
    assertEquals("GET", result);
  }

  // addOrUpsertExtraQueryString

  @Test
  public void testAddOrUpsertExtraQueryString_withEmptyExtraQuery() {
    var params = HeadersMultiMap.headers();
    QueryUtil.addOrUpsertExtraQueryString("", params);
    assertTrue(params.isEmpty());
  }

  @Test
  public void testAddOrUpsertExtraQueryString_withNoExistingQuery() {
    var params = HeadersMultiMap.headers();
    var extraQuery = "status=active";
    QueryUtil.addOrUpsertExtraQueryString(extraQuery, params);
    assertEquals(extraQuery, params.get(QUERY.getName()));
  }

  @Test
  public void testAddOrUpsertExtraQueryString_withExistingQuery() {
    var params = HeadersMultiMap.headers();
    params.add(QUERY.getName(), "name=book");
    var extraQuery = "status=active";
    QueryUtil.addOrUpsertExtraQueryString(extraQuery, params);
    assertEquals("name=book and status=active", params.get(QUERY.getName()));
  }

  // getResultPath

  @Test
  public void testGetResultPath_withNullParams() {
    var proxyPath = "/orders/:orderId";
    var result = QueryUtil.getResultPath(null, proxyPath);
    assertEquals(proxyPath, result);
  }

  @Test
  public void testGetResultPath_withParams() {
    var params = HeadersMultiMap.headers();
    params.add("orderId", "123456");
    var proxyPath = "/orders/:orderId";
    var result = QueryUtil.getResultPath(params, proxyPath);
    assertEquals("/orders/123456", result);
  }

  @Test
  public void testGetResultPath_withOffsetParam() {
    var params = HeadersMultiMap.headers();
    params.add(OFFSET.getName(), "20");
    var proxyPath = "/orders?offset=:offset";
    var result = QueryUtil.getResultPath(params, proxyPath);
    assertEquals("/orders?offset=20", result);
  }

  @Test
  public void testGetResultPath_withLimitParam() {
    var params = HeadersMultiMap.headers();
    params.add(LIMIT.getName(), "10");
    var proxyPath = "/orders?limit=:limit";
    var result = QueryUtil.getResultPath(params, proxyPath);
    assertEquals("/orders?limit=10", result);
  }

  @Test
  public void testGetResultPath_withQueryParam() {
    var params = HeadersMultiMap.headers();
    params.add(QUERY.getName(), "title=test");
    var proxyPath = "/orders?query=:query";
    var result = QueryUtil.getResultPath(params, proxyPath);
    assertEquals("/orders?query=title=test", result);
  }

  @Test
  public void testGetResultPath_withMultipleQueryParams() {
    var params = HeadersMultiMap.headers();
    params.add(OFFSET.getName(), "20");
    params.add(LIMIT.getName(), "10");
    params.add(QUERY.getName(), "title=test");
    var proxyPath = "/orders?offset=:offset&limit=:limit&query=:query";
    var result = QueryUtil.getResultPath(params, proxyPath);
    assertEquals("/orders?offset=20&limit=10&query=title=test", result);
  }

  @Test
  public void testGetResultPath_withEmptyParamValues() {
    var params = HeadersMultiMap.headers();
    params.add(OFFSET.getName(), "");
    params.add(LIMIT.getName(), "");
    params.add(QUERY.getName(), "");
    var proxyPath = "/orders?offset=:offset&limit=:limit&query=:query";
    var result = QueryUtil.getResultPath(params, proxyPath);
    assertEquals("/orders?offset=&limit=&query=", result);
  }

  @Test
  public void testGetResultPath_withParamsNotInPath() {
    var params = HeadersMultiMap.headers();
    params.add(OFFSET.getName(), "20");
    params.add(LIMIT.getName(), "10");
    var proxyPath = "/orders/search";
    var result = QueryUtil.getResultPath(params, proxyPath);
    assertEquals("/orders/search", result);
  }

  // getNormalizedProxyPath

  @Test
  public void testGetNormalizedProxyPath_withSingleParam() {
    var params = HeadersMultiMap.headers();
    params.add("orderId", "123456");
    var proxyPath = "/orders/:orderId";
    var result = QueryUtil.getNormalizedProxyPath(params, proxyPath);
    assertEquals("/orders/123456", result);
  }

  @Test
  public void testGetNormalizedProxyPath_withMultipleParams() {
    var params = HeadersMultiMap.headers();
    params.add("orderId", "123456");
    params.add("lineId", "789012");
    var proxyPath = "/orders/:orderId/lines/:lineId";
    var result = QueryUtil.getNormalizedProxyPath(params, proxyPath);
    assertEquals("/orders/123456/lines/789012", result);
  }

  @Test
  public void testGetNormalizedProxyPath_withSpacesInParam() {
    var params = HeadersMultiMap.headers();
    params.add("query", "title contains Books");
    var proxyPath = "/orders?query=:query";
    var result = QueryUtil.getNormalizedProxyPath(params, proxyPath);
    assertEquals("/orders?query=title+contains+Books", result);
  }

  // getProxyPath

  @Test
  public void testGetProxyPath_withNullParams() {
    var proxyPath = "/orders/:orderId";
    var result = QueryUtil.getProxyPath(proxyPath, null);
    assertEquals(proxyPath, result);
  }

  @Test
  public void testGetProxyPath_withDefaultValue() {
    var params = HeadersMultiMap.headers();
    var proxyPath = "/orders?limit=:limit";
    var result = QueryUtil.getProxyPath(proxyPath, params);
    assertEquals("/orders?limit=" + LIMIT.getDefaultValue(), result);
  }

  @Test
  public void testGetProxyPath_withoutDefaultValue() {
    var params = HeadersMultiMap.headers();
    var proxyPath = "/orders?query=:query";
    var result = QueryUtil.getProxyPath(proxyPath, params);
    assertEquals("/orders", result);
  }

  @Test
  public void testGetProxyPath_withPlaceholderAndValue() {
    var params = HeadersMultiMap.headers();
    params.add(LIMIT.getName(), "10");
    var proxyPath = "/orders?limit=:limit";
    var result = QueryUtil.getProxyPath(proxyPath, params);
    assertEquals("/orders?limit=:limit", result);
  }

  @Test
  public void testGetProxyPath_withoutPlaceholder() {
    var params = HeadersMultiMap.headers();
    var proxyPath = "/orders?status=active";
    var result = QueryUtil.getProxyPath(proxyPath, params);
    assertEquals("/orders?status=active", result);
  }

  @Test
  public void testGetProxyPath_withEmptyString() {
    var params = HeadersMultiMap.headers();
    params.add(QUERY.getName(), "");
    var proxyPath = "/orders?query=:query";
    var result = QueryUtil.getProxyPath(proxyPath, params);
    assertEquals("/orders", result);
  }

  @Test
  public void testGetProxyPath_withNonEmptyString() {
    var params = HeadersMultiMap.headers();
    params.add(QUERY.getName(), "value");
    var proxyPath = "/orders?query=:query";
    var result = QueryUtil.getProxyPath(proxyPath, params);
    assertEquals("/orders?query=:query", result);
  }

  @Test
  public void testGetProxyPath_multipleParams() {
    var params = HeadersMultiMap.headers();
    var proxyPath = "/orders?offset=:offset&limit=:limit&query=:query";
    var result = QueryUtil.getProxyPath(proxyPath, params);
    assertEquals("/orders?offset=" + OFFSET.getDefaultValue() +
      "&limit=" + LIMIT.getDefaultValue(), result);
  }
}

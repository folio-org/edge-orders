package org.folio.edge.orders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

public class ConfigResponseConverterTest {

  private final ConfigResponseConverter converter = new ConfigResponseConverter();

  @Test
  public void testFromTenantAddresses() {
    JsonObject metadata = new JsonObject()
      .put("createdDate", "2026-03-18T07:44:52.875+00:00")
      .put("createdByUserId", "user-1")
      .put("updatedDate", "2026-03-18T07:44:52.875+00:00")
      .put("updatedByUserId", "user-1");

    JsonObject address = new JsonObject()
      .put("id", "addr-1")
      .put("name", "Main Library")
      .put("address", "123 Main St")
      .put("metadata", metadata);

    JsonObject input = new JsonObject()
      .put("addresses", new JsonArray().add(address))
      .put("resultInfo", new JsonObject()
        .put("totalRecords", 1)
        .put("diagnostics", new JsonArray()));

    JsonObject result = converter.fromTenantAddresses(input);

    // totalRecords derived from configs size
    assertEquals(1, result.getInteger("totalRecords").intValue());
    assertFalse(result.containsKey("resultInfo"));

    JsonArray configs = result.getJsonArray("configs");
    assertNotNull(configs);
    assertEquals(1, configs.size());

    JsonObject config = configs.getJsonObject(0);
    assertEquals("addr-1", config.getString("id"));
    assertNotNull(config.getString("value"));
    assertNotNull(config.getJsonObject("metadata"));

    // Should NOT have legacy or unnecessary fields
    assertFalse(config.containsKey("module"));
    assertFalse(config.containsKey("configName"));
    assertFalse(config.containsKey("code"));
    assertFalse(config.containsKey("enabled"));

    // Value should be JSON with name and address
    JsonObject value = new JsonObject(config.getString("value"));
    assertEquals("Main Library", value.getString("name"));
    assertEquals("123 Main St", value.getString("address"));
  }

  @Test
  public void testFromTenantAddressesEmptyList() {
    JsonObject input = new JsonObject()
      .put("addresses", new JsonArray())
      .put("resultInfo", new JsonObject()
        .put("totalRecords", 0)
        .put("diagnostics", new JsonArray()));

    JsonObject result = converter.fromTenantAddresses(input);

    assertEquals(0, result.getInteger("totalRecords").intValue());
    assertTrue(result.getJsonArray("configs").isEmpty());
  }

  @Test
  public void testFromTenantAddressesWithoutMetadata() {
    JsonObject address = new JsonObject()
      .put("id", "addr-2")
      .put("name", "Branch")
      .put("address", "456 Oak Ave");

    JsonObject input = new JsonObject()
      .put("addresses", new JsonArray().add(address))
      .put("resultInfo", new JsonObject().put("totalRecords", 1));

    JsonObject result = converter.fromTenantAddresses(input);

    assertEquals(1, result.getInteger("totalRecords").intValue());
    JsonObject config = result.getJsonArray("configs").getJsonObject(0);
    assertEquals("addr-2", config.getString("id"));
    assertFalse(config.containsKey("metadata"));
  }

  @Test
  public void testTotalRecordsDerivedFromConfigsSize() {
    JsonArray addresses = new JsonArray()
      .add(new JsonObject().put("id", "a1").put("name", "n1").put("address", "a1"))
      .add(new JsonObject().put("id", "a2").put("name", "n2").put("address", "a2"))
      .add(new JsonObject().put("id", "a3").put("name", "n3").put("address", "a3"));

    JsonObject input = new JsonObject()
      .put("addresses", addresses);

    JsonObject result = converter.fromTenantAddresses(input);

    assertEquals(3, result.getInteger("totalRecords").intValue());
    assertEquals(3, result.getJsonArray("configs").size());
  }
}
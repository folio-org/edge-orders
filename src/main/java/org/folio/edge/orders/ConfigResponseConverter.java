package org.folio.edge.orders;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Converts responses from various FOLIO APIs into a unified config format.
 * The output contract is:
 * <pre>{
 *   "configs": [
 *     { "id": "...", "value": "...", "metadata": { ... } }
 *   ],
 *   "totalRecords": N
 * }</pre>
 */
public class ConfigResponseConverter {

  /**
   * Converts a tenant-addresses response into the unified config format.
   *
   * @param response the raw response from /tenant-addresses
   * @return the response in config format
   */
  public JsonObject fromTenantAddresses(JsonObject response) {
    JsonArray addresses = response.getJsonArray("addresses", new JsonArray());
    JsonArray configs = new JsonArray();

    for (int i = 0; i < addresses.size(); i++) {
      JsonObject address = addresses.getJsonObject(i);

      JsonObject valueObj = new JsonObject()
        .put("name", address.getString("name", ""))
        .put("address", address.getString("address", ""));

      JsonObject config = new JsonObject()
        .put("id", address.getString("id"))
        .put("value", valueObj.encode());

      if (address.containsKey("metadata")) {
        config.put("metadata", address.getJsonObject("metadata"));
      }
      configs.add(config);
    }

    return new JsonObject()
      .put("configs", configs)
      .put("totalRecords", configs.size());
  }
}
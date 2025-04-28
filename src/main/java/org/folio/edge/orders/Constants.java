package org.folio.edge.orders;

import java.util.HashMap;
import java.util.Map;

public class Constants {

  public static final String API_CONFIGURATION_PROPERTY_NAME = "api_config";
  public static final String API_CONFIGURATION_DEFAULT = "api_configuration.json";
  public static final String MOD_ORDERS_STORAGE_VERSION = "mod_order_storage_version";
  public static final String CUSTOM_FIELDS_ENDPOINT = "custom-fields";
  public static final String HTTP_METHOD_POST = "POST";
  public static final String HTTP_METHOD_GET = "GET";
  public static final String HTTP_METHOD_PUT = "PUT";

  public enum ErrorCodes {
    // Subject to change pending additional information from the GOBI folks
    BAD_REQUEST(400),
    API_KEY_INVALID(-1),
    ACCESS_DENIED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    REQUEST_TIMEOUT(408),
    INTERNAL_SERVER_ERROR(500);

    private final Integer value;
    private static final Map<Integer, ErrorCodes> CONSTANTS = new HashMap<>();

    static {
      for (ErrorCodes c : values()) {
        CONSTANTS.put(c.value, c);
      }
    }

    ErrorCodes(Integer value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.name();
    }

    public static ErrorCodes fromValue(Integer value) {
      return CONSTANTS.get(value);
    }
  }

  private Constants() {
  }
}

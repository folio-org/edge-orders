package org.folio.edge.orders;

import java.util.HashMap;
import java.util.Map;

public class Constants {

  public static final String PARAM_TYPE = "type";

  public enum ErrorCodes {

    // Subject to change pending additional information from the GOBI folks
    BAD_REQUEST(400),
    ACCESS_DENIED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    REQUEST_TIMEOUT(408),
    INTERNAL_SERVER_ERROR(null);

    private final Integer value;
    private static final Map<Integer, ErrorCodes> CONSTANTS = new HashMap<>();

    static {
      for (ErrorCodes c : values()) {
        CONSTANTS.put(c.value, c);
      }
    }

    private ErrorCodes(Integer value) {
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

  public enum PurchasingSystems {

    GOBI("GOBI");
    private final String value;
    private static final Map<String, PurchasingSystems> CONSTANTS = new HashMap<>();

    static {
      for (PurchasingSystems c : values()) {
        CONSTANTS.put(c.value, c);
      }
    }

    private PurchasingSystems(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }

    public static PurchasingSystems fromValue(String value) {
      return CONSTANTS.get(value.toUpperCase());
    }
  }

  private Constants() {

  }

}

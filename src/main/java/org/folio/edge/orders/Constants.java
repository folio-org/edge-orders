package org.folio.edge.orders;

import java.util.HashMap;
import java.util.Map;

public class Constants {

  public static final String PARAM_TYPE = "type";

  public enum ErrorCodes {
    ACCESS_DENIED, BAD_REQUEST, REQUEST_TIMEOUT, INTERNAL_SERVER_ERROR;
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
      PurchasingSystems constant = CONSTANTS.get(value);
      if (constant == null) {
        return null;
      } else {
        return constant;
      }
    }
  }

  private Constants() {

  }

}

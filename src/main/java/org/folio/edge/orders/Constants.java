package org.folio.edge.orders;

import java.util.HashMap;
import java.util.Map;

public class Constants {

  public static final String PARAM_TYPE = "type";

  public enum ErrorCodes {

    ACCESS_DENIED(401), FORBIDDEN(403), BAD_REQUEST(400), REQUEST_TIMEOUT(408), INTERNAL_SERVER_ERROR(500), NOT_FOUND(
        404);

    private final int value;
    private static final Map<Integer, ErrorCodes> CONSTANTS = new HashMap<>();

    static {
      for (ErrorCodes c : values()) {
        CONSTANTS.put(c.value, c);
      }
    }

    private ErrorCodes(int value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.name();
    }

    public static ErrorCodes fromValue(int value) {
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

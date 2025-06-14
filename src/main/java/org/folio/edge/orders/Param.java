package org.folio.edge.orders;

public enum Param {
  TYPE("type", ""),
  OFFSET("offset", "0"),
  LIMIT("limit", "20"),
  QUERY("query", ""),
  FISCAL_YEAR_CODE("fiscalYearCode", "");

  private final String name;
  private final String defaultValue;

  Param(String name, String defaultValue) {
    this.name = name;
    this.defaultValue = defaultValue;
  }

  public String getName() {
    return name;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public boolean isDefaultNonBlankValue() {
    return !defaultValue.isBlank();
  }
}

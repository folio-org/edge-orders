package org.folio.edge.orders;

public enum CommonEndpoint {
  ORDER_TEMPLATES("/orders/order-templates", "/orders/order-templates", "orderTemplates", true),
  ORDER_CUSTOM_FIELDS("/orders/custom-fields", "/custom-fields", "customFields", true),
  FUNDS("/finance/funds", "/finance/funds", "funds", true),
  EXPENSE_CLASSES("/finance/expense-classes", "/finance/expense-classes", "expenseClasses", true),
  ACQUISITIONS_UNITS("/acquisitions-units", "/acquisitions-units/units", "acquisitionsUnits", true),
  ACQUISITIONS_METHODS("/acquisition-methods", "/orders/acquisition-methods", "acquisitionMethods", true),
  ORGANIZATIONS("/organizations", "/organizations/organizations", "organizations", true),
  BILLING_AND_SHIPPING("/addresses/billing-and-shipping", "/configurations/entries", "configs", false);

  private final String ingressUrl;
  private final String egressUrl;
  private final String dataKey;
  private final boolean hasFiltering;

  CommonEndpoint(String ingressUrl, String egressUrl, String dataKey, boolean hasFiltering) {
    this.ingressUrl = ingressUrl;
    this.egressUrl = egressUrl;
    this.dataKey = dataKey;
    this.hasFiltering = hasFiltering;
  }

  public String getIngressUrl() {
    return ingressUrl;
  }

  public String getEgressUrl() {
    return egressUrl;
  }

  public String getDataKey() {
    return dataKey;
  }

  public boolean isHasFiltering() {
    return hasFiltering;
  }
}

package org.folio.edge.orders;

public enum CommonEndpoint {
  ORDER_TEMPLATES("/orders/order-templates", "/orders/order-templates", "orderTemplates", true, true),
  ORDER_CUSTOM_FIELDS("/orders/custom-fields", "/custom-fields", "customFields", true, true),
  FUNDS("/finance/funds", "/finance/funds", "funds", true, true),
  FUND_EXPENSE_CLASSES("/finance/funds/:id/expense-classes", "/finance/funds/:id/expense-classes", "fundExpenseClasses", true, true),
  EXPENSE_CLASSES("/finance/expense-classes", "/finance/expense-classes", "expenseClasses", true, true),
  FUND_CODES_EXPENSE_CLASSES("/finance/fund-codes-expense-classes", "/finance/fund-codes-expense-classes", "fundCodeVsExpClassesTypes", false, true),
  ACQUISITIONS_UNITS("/orders/acquisitions-units", "/acquisitions-units/units", "acquisitionsUnits", true, true),
  ACQUISITIONS_METHODS("/orders/acquisition-methods", "/orders/acquisition-methods", "acquisitionMethods", true, true),
  ORGANIZATIONS("/organizations", "/organizations/organizations", "organizations", true, true),
  BILLING_AND_SHIPPING("/orders/addresses/billing-and-shipping", "/settings/entries", "configs", true, false),
  LOCATIONS("/locations-for-order", "/locations", "locations", true, true),
  MATERIAL_TYPES("/material-types-for-order", "/material-types", "mtypes", true, true),
  IDENTIFIER_TYPES("/identifier-types-for-order", "/identifier-types", "identifierTypes", true, true),
  CONTRIBUTOR_NAME_TYPES("/contributor-name-types-for-order", "/contributor-name-types", "contributorNameTypes", true, true),
  USERS("/users-for-order", "/users", "users", true, true);

  private final String ingressUrl;
  private final String egressUrl;
  private final String dataKey;
  private final boolean isAdvancedQuerySupported;
  private final boolean isEmptyExtraQuery;

  CommonEndpoint(String ingressUrl, String egressUrl, String dataKey, boolean isAdvancedQuerySupported, boolean isEmptyExtraQuery) {
    this.ingressUrl = ingressUrl;
    this.egressUrl = egressUrl;
    this.dataKey = dataKey;
    this.isAdvancedQuerySupported = isAdvancedQuerySupported;
    this.isEmptyExtraQuery = isEmptyExtraQuery;
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

  public boolean isAdvancedQuerySupported() {
    return isAdvancedQuerySupported;
  }

  public boolean isEmptyExtraQuery() {
    return isEmptyExtraQuery;
  }
}

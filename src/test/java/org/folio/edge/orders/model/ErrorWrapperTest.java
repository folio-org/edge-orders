package org.folio.edge.orders.model;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ErrorWrapperTest {

  @Test
  public void testEqualsContract() {
    EqualsVerifier.forClass(ErrorWrapper.class).verify();
  }

}

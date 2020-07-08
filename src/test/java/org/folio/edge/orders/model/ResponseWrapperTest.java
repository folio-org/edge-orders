package org.folio.edge.orders.model;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ResponseWrapperTest {
  private static final Logger logger = Logger.getLogger(ResponseWrapperTest.class);
  private ResponseWrapper respError;
  private ResponseWrapper respSuccess;

  @Before
  public void setUp() {
    respError = new ResponseWrapper(new ErrorWrapper("CODE", "Message"));
    respSuccess = new ResponseWrapper("PO-12345678910");
  }

  @Test
  public void testEqualsContract() {
    EqualsVerifier.forClass(ResponseWrapper.class).verify();
  }

  @Test
  public void testErrorToFromJson() throws IOException {
    String json = respError.toJson();
    logger.info("JSON: " + json);

    ResponseWrapper fromJson = ResponseWrapper.fromJson(json);
    assertEquals(respError, fromJson);
  }

  @Test
  public void testErrorToFromXml() throws IOException {
    String xml = respError.toXml();
    logger.info("XML: " + xml);

    ResponseWrapper fromXml = ResponseWrapper.fromXml(xml);
    assertEquals(respError, fromXml);
  }

  @Test
  public void testErrorJsonToXml() throws IOException {
    String json = respError.toJson();
    ResponseWrapper fromJson = ResponseWrapper.fromJson(json);
    String xml = fromJson.toXml();
    ResponseWrapper fromXml = ResponseWrapper.fromXml(xml);

    logger.info(json);
    logger.info(xml);

    assertEquals(respError, fromJson);
    assertEquals(respError, fromXml);
  }

  @Test
  public void testSuccessToFromJson() throws IOException {
    String json = respSuccess.toJson();
    logger.info("JSON: " + json);

    ResponseWrapper fromJson = ResponseWrapper.fromJson(json);
    assertEquals(respSuccess, fromJson);
  }

  @Test
  public void testSuccessToFromXml() throws IOException {
    String xml = respSuccess.toXml();
    logger.info("XML: " + xml);

    ResponseWrapper fromXml = ResponseWrapper.fromXml(xml);
    assertEquals(respSuccess, fromXml);
  }

  @Test
  public void testSuccessJsonToXml() throws IOException {
    String json = respSuccess.toJson();
    ResponseWrapper fromJson = ResponseWrapper.fromJson(json);
    String xml = fromJson.toXml();
    ResponseWrapper fromXml = ResponseWrapper.fromXml(xml);

    logger.info(json);
    logger.info(xml);

    assertEquals(respSuccess, fromJson);
    assertEquals(respSuccess, fromXml);
  }

}

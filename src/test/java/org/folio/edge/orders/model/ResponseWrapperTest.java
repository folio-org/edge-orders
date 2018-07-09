package org.folio.edge.orders.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ResponseWrapperTest {

  private static final Logger logger = Logger.getLogger(ResponseWrapperTest.class);

  private static final String XSD = "ramls/orders.xsd";
  private Validator validator;

  private ResponseWrapper respError;
  private ResponseWrapper respSuccess;

  @Before
  public void setUp() throws Exception {
    respError = new ResponseWrapper(new ErrorWrapper("CODE", "Message"));
    respSuccess = new ResponseWrapper("PO-12345678910");

    SchemaFactory schemaFactory = SchemaFactory
      .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema(new File(XSD));
    validator = schema.newValidator();
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

    Source source = new StreamSource(new StringReader(xml));
    try {
      validator.validate(source);
    } catch (SAXException e) {
      fail("XML validation failed: " + e.getMessage());
    }

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

    Source source = new StreamSource(new StringReader(xml));
    try {
      validator.validate(source);
    } catch (SAXException e) {
      fail("XML validation failed: " + e.getMessage());
    }

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

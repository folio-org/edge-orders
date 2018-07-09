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

public class ErrorWrapperTest {

  private static final Logger logger = Logger.getLogger(ErrorWrapperTest.class);

  private static final String XSD = "ramls/orders.xsd";
  private Validator validator;

  private ErrorWrapper error;

  @Before
  public void setUp() throws Exception {
    error = new ErrorWrapper("CODE", "Message");

    SchemaFactory schemaFactory = SchemaFactory
      .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema(new File(XSD));
    validator = schema.newValidator();
  }

  @Test
  public void testEqualsContract() {
    EqualsVerifier.forClass(ErrorWrapper.class).verify();
  }

  @Test
  public void testToFromJson() throws IOException {
    String json = error.toJson();
    logger.info("JSON: " + json);

    ErrorWrapper fromJson = ErrorWrapper.fromJson(json);
    assertEquals(error, fromJson);
  }

  @Test
  public void testToFromXml() throws IOException {
    String xml = error.toXml();
    logger.info("XML: " + xml);

    Source source = new StreamSource(new StringReader(xml));
    try {
      validator.validate(source);
    } catch (SAXException e) {
      fail("XML validation failed: " + e.getMessage());
    }

    ErrorWrapper fromXml = ErrorWrapper.fromXml(xml);
    assertEquals(error, fromXml);
  }

  @Test
  public void testJsonToXml() throws IOException {
    String json = error.toJson();
    ErrorWrapper fromJson = ErrorWrapper.fromJson(json);
    String xml = fromJson.toXml();
    ErrorWrapper fromXml = ErrorWrapper.fromXml(xml);

    logger.info(json);
    logger.info(xml);

    assertEquals(error, fromJson);
    assertEquals(error, fromXml);
  }

}

package org.folio.edge.orders.model;

import java.io.IOException;

import org.folio.edge.core.utils.Mappers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "Response")
@JsonInclude(Include.NON_NULL)
public final class ResponseWrapper {

  @JsonProperty("PoLineNumber")
  public final String poLineNumber;

  @JsonProperty("Error")
  public final ErrorWrapper error;

  public ResponseWrapper(String poLineNumber) {
    this(poLineNumber, null);
  }

  public ResponseWrapper(ErrorWrapper error) {
    this(null, error);
  }

  private ResponseWrapper(@JsonProperty("PoLineNumber") String poLineNumber,
                          @JsonProperty("Error") ErrorWrapper error) {
    this.poLineNumber = poLineNumber;
    this.error = error;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((error == null) ? 0 : error.hashCode());
    result = prime * result + ((poLineNumber == null) ? 0 : poLineNumber.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ResponseWrapper other = (ResponseWrapper) obj;
    if (error == null) {
      if (other.error != null) {
        return false;
      }
    } else if (!error.equals(other.error)) {
      return false;
    }
    if (poLineNumber == null) {
      if (other.poLineNumber != null) {
        return false;
      }
    } else if (!poLineNumber.equals(other.poLineNumber)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "ResponseWrapper{" +
      "poLineNumber='" + poLineNumber + '\'' +
      ", error=" + error +
      '}';
  }

  public String toXml() throws JsonProcessingException {
    return Mappers.XML_PROLOG + Mappers.xmlMapper.writeValueAsString(this);
  }

  public String toJson() throws JsonProcessingException {
    return Mappers.jsonMapper.writeValueAsString(this);
  }

  public static ResponseWrapper fromJson(String json) throws IOException {
    return Mappers.jsonMapper.readValue(json, ResponseWrapper.class);
  }

  public static ResponseWrapper fromXml(String xml) throws IOException {
    return Mappers.xmlMapper.readValue(xml, ResponseWrapper.class);
  }

}

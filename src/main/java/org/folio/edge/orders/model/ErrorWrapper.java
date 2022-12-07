package org.folio.edge.orders.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "Error")
public final class ErrorWrapper {

  @JsonProperty("Code")
  public final String code;

  @JsonProperty("Message")
  public final String message;

  public ErrorWrapper(@JsonProperty("Code") String code, @JsonProperty("Message") String message) {
    this.code = code;
    this.message = message;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((code == null) ? 0 : code.hashCode());
    result = prime * result + ((message == null) ? 0 : message.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ErrorWrapper other = (ErrorWrapper) obj;
    if (code == null) {
      if (other.code != null)
        return false;
    } else if (!code.equals(other.code)) {
      return false;
    }
    if (message == null) {
      if (other.message != null)
        return false;
    } else if (!message.equals(other.message)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "ErrorWrapper{" +
      "code='" + code + '\'' +
      ", message='" + message + '\'' +
      '}';
  }
}

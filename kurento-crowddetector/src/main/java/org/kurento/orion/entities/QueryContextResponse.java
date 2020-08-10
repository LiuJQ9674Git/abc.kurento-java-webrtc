package org.kurento.orion.entities;

import com.google.gson.annotations.SerializedName;

public class QueryContextResponse extends AbstractOrionResponse {

  @SerializedName("contextElement")
  private OrionContextElement element;

  public QueryContextResponse() {
  }

  public OrionContextElement getElement() {
    return element;
  }

  public void setElement(OrionContextElement element) {
    this.element = element;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(element).append("\n");
    sb.append(super.toString());

    return sb.toString();
  }
}
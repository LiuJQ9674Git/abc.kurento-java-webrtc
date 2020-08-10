package org.kurento.orion.entities;

import com.google.gson.annotations.SerializedName;

/**
 * Context element with a status code
 */
public class OrionContextElementResponse extends AbstractOrionResponse {

  @SerializedName("contextElement")
  private OrionContextElement contextElement;

  public OrionContextElement getContextElement() {
    return contextElement;
  }

  public void setContextElement(OrionContextElement contextElement) {
    this.contextElement = contextElement;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(contextElement).append("\n");
    sb.append(super.toString());

    return sb.toString();
  }
}

package org.kurento.orion.entities;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

/**
 * A context element from Orion.
 */
public class OrionContextElement {

  private String type;

  private boolean isPattern;
  private String id;
  private final List<OrionAttribute<?>> attributes = newArrayList();

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isPattern() {
    return isPattern;
  }

  public void setPattern(boolean isPattern) {
    this.isPattern = isPattern;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<OrionAttribute<?>> getAttributes() {
    return attributes;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(" Type: ").append(type).append("\n");
    sb.append(" Id: ").append(id).append("\n");
    sb.append(" IsPattern: ").append(isPattern).append("\n");

    return sb.toString();
  }
}

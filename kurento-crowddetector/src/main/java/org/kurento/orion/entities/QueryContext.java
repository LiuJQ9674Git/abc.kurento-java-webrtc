package org.kurento.orion.entities;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;

/**
 * Query context request object
 */
public class QueryContext {

  @SerializedName("entities")
  private List<OrionContextElement> entities;

  @SerializedName("updateAction")
  private List<OrionAttribute<?>> attributes;

  public QueryContext(List<OrionContextElement> entities, OrionAttribute<?>... attributes) {
    this.attributes = ImmutableList.copyOf(attributes);
    this.entities = ImmutableList.copyOf(entities);
  }

  public QueryContext(List<OrionContextElement> entities) {
    this.entities = ImmutableList.copyOf(entities);
    this.attributes = ImmutableList.of();
  }

  public QueryContext(OrionContextElement entity) {
    this.entities = ImmutableList.of(entity);
    this.attributes = ImmutableList.of();
  }

  public List<OrionContextElement> getEntities() {
    return entities;
  }

  public void setEntities(List<OrionContextElement> entities) {
    this.entities = entities;
  }

  public List<OrionAttribute<?>> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<OrionAttribute<?>> attributes) {
    this.attributes = attributes;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (OrionContextElement element : entities) {
      sb.append(element).append("\n");
    }

    return sb.toString();
  }
}

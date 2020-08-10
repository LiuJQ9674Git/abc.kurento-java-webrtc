package org.kurento.orion.entities;

import com.google.gson.annotations.SerializedName;

/**
 * Status object, that holds the result of the request.
 *
 */
public final class StatusCode {

  @SerializedName("code")
  private int code;

  @SerializedName("reasonPhrase")
  private String reason;

  @SerializedName("details")
  private String details;

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(" Code: ").append(code).append("\n");
    sb.append(" Reason: ").append(reason).append("\n");
    sb.append(" Details: ").append(details).append("\n");

    return sb.toString();
  }
}

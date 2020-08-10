package org.kurento.orion;

/**
 * @author Ivan Gracia (igracia@kurento.org)
 *
 */
public class OrionConnectorConfiguration {

  private String orionHost = "130.206.85.186";
  private int orionPort = 1026;
  private String orionScheme = "http";

  public String getOrionHost() {
    return this.orionHost;
  }

  public void setOrionHost(String orionHost) {
    this.orionHost = orionHost;
  }

  public int getOrionPort() {
    return this.orionPort;
  }

  public void setOrionPort(int orionPort) {
    this.orionPort = orionPort;
  }

  public String getOrionScheme() {
    return this.orionScheme;
  }

  public void setOrionScheme(String orionSchema) {
    this.orionScheme = orionSchema;
  }
}

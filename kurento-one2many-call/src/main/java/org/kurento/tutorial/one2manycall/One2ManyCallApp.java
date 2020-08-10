package org.kurento.tutorial.one2manycall;

import org.kurento.client.KurentoClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Video call 1 to N demo (main).
 */
@SpringBootApplication
@EnableWebSocket
public class One2ManyCallApp implements WebSocketConfigurer {

  @Bean
  public CallHandler callHandler() {
    return new CallHandler();
  }

  @Bean
  public KurentoClient kurentoClient() {
    return KurentoClient.create();
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(callHandler(), "/call");
  }

  public static void main(String[] args) throws Exception {
    new SpringApplication(One2ManyCallApp.class).run(args);
  }

}

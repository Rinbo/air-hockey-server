package se.docksidelabs.airhockeyserver.config;

import org.apache.coyote.AbstractProtocol;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Disables Nagle's algorithm (TCP_NODELAY) on the embedded Tomcat connector.
 *
 * <p>Without this, the OS buffers small outgoing packets (like our 48-byte
 * game state frames) for up to 40ms before sending, introducing artificial
 * latency on every server tick.
 */
@Configuration
public class TcpNoDelayCustomizer {

  @Bean
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tcpNoDelay() {
    return factory -> factory.addConnectorCustomizers(connector -> {
      if (connector.getProtocolHandler() instanceof AbstractProtocol<?> protocol) {
        protocol.setTcpNoDelay(true);
      }
    });
  }
}

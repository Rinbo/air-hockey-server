package nu.borjessons.airhockeyserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AirHockeyServerApplication {
  private static final Logger logger = LoggerFactory.getLogger(AirHockeyServerApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(AirHockeyServerApplication.class, args);
  }

/*  @Bean
  CommandLineRunner checkSslConfiguration(ServletWebServerApplicationContext applicationContext) {
    return args -> {
      SslStoreProvider sslStoreProvider = applicationContext.getBean(SslStoreProvider.class);
      logger.info("Keystore loaded successfully with aliases: {}", sslStoreProvider.getKeyStore().aliases());
    };
  }*/
}

package nu.borjessons.airhockeyserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AirHockeyServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(AirHockeyServerApplication.class, args);
  }
}

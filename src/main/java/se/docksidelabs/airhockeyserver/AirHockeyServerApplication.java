package se.docksidelabs.airhockeyserver;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.model.Player;
import se.docksidelabs.airhockeyserver.model.Username;

@SpringBootApplication
@EnableScheduling
@RegisterReflectionForBinding({ Player.class, Username.class, GameId.class })
public class AirHockeyServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(AirHockeyServerApplication.class, args);
  }
}

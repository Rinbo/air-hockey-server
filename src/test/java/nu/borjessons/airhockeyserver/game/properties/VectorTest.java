package nu.borjessons.airhockeyserver.game.properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nu.borjessons.airhockeyserver.game.objects.Puck;

/**
 * Board origin is in upper left corner to conform to default HTML Canvas settings
 */
class VectorTest {
  private static final double ALLOWED_DELTA = 0.0001;
  private static final int BOARD_HEIGHT = 500;
  private static final int BOARD_WIDTH = 200;
  private static final double BOARD_ASPECT_RATIO = (double) BOARD_WIDTH / BOARD_HEIGHT;
  private static final Position HANDLE_POS_REAL = new Position(50, 50);
  private static final Position HANDLE_POS = new Position(HANDLE_POS_REAL.x() / BOARD_WIDTH, HANDLE_POS_REAL.y() / BOARD_HEIGHT);
  private static final Position PUCK_POS_REAL = new Position(25, 75);
  private static final Position PUCK_POS = new Position(PUCK_POS_REAL.x() / BOARD_WIDTH, PUCK_POS_REAL.y() / BOARD_HEIGHT);
  private static final double X_DIFF = PUCK_POS.x() - HANDLE_POS.x();
  private static final double Y_DIFF = PUCK_POS.y() - HANDLE_POS.y();
  private static final double X_DIFF_REAL = PUCK_POS_REAL.x() - HANDLE_POS_REAL.x();
  private static final double Y_DIFF_REAL = PUCK_POS_REAL.y() - HANDLE_POS_REAL.y();
  private static final double ANGLE_REAL = Math.atan(Y_DIFF_REAL / X_DIFF_REAL);

  /**
   * The next step I think is to add the delta of the puck and handle edges onto the puck position
   */

  @Test
  void angleTest() {
    Vector vector = Vector.from(PUCK_POS, HANDLE_POS);
    double angle = vector.angle(BOARD_ASPECT_RATIO);
    double hypotenuseRelativeToWidth = X_DIFF / Math.cos(angle);
    double hypotenuseRelativeToHeight = Y_DIFF / Math.sin(angle);

    Assertions.assertEquals(ANGLE_REAL, angle, ALLOWED_DELTA);
    Assertions.assertEquals(hypotenuseRelativeToWidth * BOARD_WIDTH, hypotenuseRelativeToHeight * BOARD_HEIGHT, ALLOWED_DELTA);
  }

  @Test
  void assertConstantsTest() {
    double hypotenuse = Math.sqrt(Math.pow(X_DIFF_REAL, 2) + Math.pow(X_DIFF_REAL, 2));
    Assertions.assertEquals(X_DIFF_REAL, (PUCK_POS.x() - HANDLE_POS.x()) * BOARD_WIDTH, ALLOWED_DELTA);
    Assertions.assertEquals(Y_DIFF_REAL, (PUCK_POS.y() - HANDLE_POS.y()) * BOARD_HEIGHT, ALLOWED_DELTA);

    Assertions.assertEquals(hypotenuse, Math.abs(X_DIFF_REAL / Math.cos(ANGLE_REAL)), ALLOWED_DELTA);
    Assertions.assertEquals(hypotenuse, Math.abs(Y_DIFF_REAL / Math.sin(ANGLE_REAL)), ALLOWED_DELTA);
  }

  @Test
  void puckRadiusTest() {
    Radius radius = new Radius(0.08, 0.08 * BOARD_ASPECT_RATIO);
    Puck puck = Puck.create(PUCK_POS, radius);
    Vector vector = Vector.from(puck.getPosition(), HANDLE_POS);
    double angle = vector.angle(BOARD_ASPECT_RATIO);

    double xr = Math.cos(angle) * radius.x();
    double yr = Math.sin(angle) * radius.y();

    double realRadius = radius.x() * BOARD_WIDTH;
    double realXProjection = Math.cos(angle) * realRadius;
    double realYProjection = Math.sin(angle) * realRadius;

    Assertions.assertEquals(radius.x() * BOARD_WIDTH, radius.y() * BOARD_HEIGHT, ALLOWED_DELTA);
    Assertions.assertEquals(realXProjection, xr * BOARD_WIDTH, ALLOWED_DELTA);
    Assertions.assertEquals(realYProjection, yr * BOARD_HEIGHT, ALLOWED_DELTA);
    Assertions.assertEquals(realXProjection, radius.getAngledProjection(angle).x() * BOARD_WIDTH, ALLOWED_DELTA);
    Assertions.assertEquals(realYProjection, radius.getAngledProjection(angle).y() * BOARD_HEIGHT, ALLOWED_DELTA);
  }
}
package nu.borjessons.airhockeyserver.game.properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nu.borjessons.airhockeyserver.utils.TestUtils;

class VectorTest {
  /**
   * The next step I think is to add the delta of the puck and handle edges onto the puck position
   */
  @Test
  void angleTest() {
    Vector vector = Vector.from(TestUtils.PUCK_POS, TestUtils.HANDLE_POS);
    double angle = vector.angle(TestUtils.BOARD_ASPECT_RATIO);
    double hypotenuseRelativeToWidth = TestUtils.X_DIFF / Math.cos(angle);
    double hypotenuseRelativeToHeight = TestUtils.Y_DIFF / Math.sin(angle);

    Assertions.assertEquals(TestUtils.ANGLE_REAL, angle, TestUtils.ALLOWED_DELTA);
    Assertions.assertEquals(hypotenuseRelativeToWidth * TestUtils.BOARD_WIDTH, hypotenuseRelativeToHeight * TestUtils.BOARD_HEIGHT, TestUtils.ALLOWED_DELTA);
  }
}

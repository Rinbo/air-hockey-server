package nu.borjessons.airhockeyserver.game.objects;

import nu.borjessons.airhockeyserver.game.properties.Radius;
import nu.borjessons.airhockeyserver.game.properties.Vector;
import nu.borjessons.airhockeyserver.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PuckTest {
    @Test
    void puckRadiusTest() {
        Radius radius = new Radius(0.08, 0.08 * TestUtils.BOARD_ASPECT_RATIO);
        Puck puck = Puck.create(TestUtils.PUCK_POS, radius);
        Vector vector = Vector.from(TestUtils.HANDLE_POS, puck.getPosition());
        double angle = vector.angle(TestUtils.BOARD_ASPECT_RATIO);

        double xr = Math.cos(angle) * radius.x();
        double yr = Math.sin(angle) * radius.y();

        double realRadius = radius.x() * TestUtils.BOARD_WIDTH;
        double realXProjection = Math.cos(angle) * realRadius;
        double realYProjection = Math.sin(angle) * realRadius;

        Assertions.assertEquals(radius.x() * TestUtils.BOARD_WIDTH, radius.y() * TestUtils.BOARD_HEIGHT, TestUtils.ALLOWED_DELTA);
        Assertions.assertEquals(realXProjection, xr * TestUtils.BOARD_WIDTH, TestUtils.ALLOWED_DELTA);
        Assertions.assertEquals(realYProjection, yr * TestUtils.BOARD_HEIGHT, TestUtils.ALLOWED_DELTA);
        Assertions.assertEquals(realXProjection, radius.getAngledProjection(angle).x() * TestUtils.BOARD_WIDTH, TestUtils.ALLOWED_DELTA);
        Assertions.assertEquals(realYProjection, radius.getAngledProjection(angle).y() * TestUtils.BOARD_HEIGHT, TestUtils.ALLOWED_DELTA);
    }
}

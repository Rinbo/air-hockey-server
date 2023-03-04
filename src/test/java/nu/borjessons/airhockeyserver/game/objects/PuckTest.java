package nu.borjessons.airhockeyserver.game.objects;

import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Radius;
import nu.borjessons.airhockeyserver.game.properties.Vector;
import nu.borjessons.airhockeyserver.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.*;


class PuckTest {
    private static int w(double percent) {
        return (int) (TestUtils.BOARD_WIDTH * percent);
    }

    private static int h(double percent) {
        return (int) (TestUtils.BOARD_HEIGHT * percent);
    }

    private static void drawCircle(Graphics graphics, Circle circle) {
        Position pos = circle.getPosition();
        Radius radius = circle.getRadius();

        graphics.drawArc(w(pos.x()), h(pos.y()), w(radius.x()), h(radius.y()), 0, 360);
    }

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

    @Test
    void canvasTest() throws InterruptedException {
        Puck puck = Puck.create(TestUtils.PUCK_POS, new Radius(0.08, 0.08 * TestUtils.BOARD_ASPECT_RATIO));
        Handle handle = Handle.create(TestUtils.HANDLE_POS, new Radius(0.1, 0.1 * TestUtils.BOARD_ASPECT_RATIO));
        Vector vector = Vector.from(puck.getPosition(), handle.getPosition());
        double angle = vector.angle(TestUtils.BOARD_ASPECT_RATIO);
        //puck.offsetCollisionWith(handle, angle);

        Frame frame = new Frame("Canvas Example");
        Canvas canvas = new Canvas() {
            public void paint(Graphics graphics) {
                graphics.setColor(Color.BLUE);
                drawCircle(graphics, puck);
                graphics.setColor(Color.RED);
                drawCircle(graphics, handle);
                graphics.setColor(Color.CYAN);
                Position pos = puck.getRadiusEdgePosition(angle);
                graphics.drawArc(w(pos.x()), h(pos.y()), 5, 5, 0, 360);
                graphics.setColor(Color.BLACK);
                int w = w(puck.getPosition().x());
                int h = h(puck.getPosition().y());
                graphics.drawArc(0, 0, 50, 50, 0, 360);
                //graphics.drawLine(w, h, w + w(vector.x()), h + h(vector.y()));


            }
        };
        canvas.setSize(TestUtils.BOARD_WIDTH, TestUtils.BOARD_HEIGHT);
        canvas.setBackground(Color.GRAY);

        Panel panel = new Panel();
        panel.add(canvas);

        frame.add(panel);
        frame.setSize(1000, 1000);
        frame.setVisible(true);

        Thread.sleep(10000);
    }
}

package nu.borjessons.airhockeyserver.canvas;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.objects.Puck;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Radius;
import nu.borjessons.airhockeyserver.game.properties.Vector;
import nu.borjessons.airhockeyserver.utils.TestUtils;

public class CanvasTest extends Application {
    public static void go() {
        launch();
    }

    private static double w(double percent) {
        return TestUtils.BOARD_WIDTH * percent;
    }

    private static double h(double percent) {
        return TestUtils.BOARD_HEIGHT * percent;
    }

    private static Circle createCircle(nu.borjessons.airhockeyserver.game.objects.Circle circle) {
        return createCircle(circle, Color.BLACK);
    }

    private static Circle createCircle(nu.borjessons.airhockeyserver.game.objects.Circle circle, Color color) {
        Position pos = circle.getPosition();
        Radius radius = circle.getRadius();

        javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(w(pos.x()), h(pos.y()), w(radius.x()));
        c.setStroke(color);
        c.setFill(Color.TRANSPARENT);
        return c;
    }

    private static Line createLine(Vector vector, Position pos) {
        double w = w(pos.x());
        double h = h(pos.y());
        return new Line(w, h, w + w(vector.x()), h + h(vector.y()));
    }

    @Override
    public void start(Stage stage) {
        Handle handle = Handle.create(TestUtils.HANDLE_POS, new Radius(0.1, 0.1 * TestUtils.BOARD_ASPECT_RATIO));
        Radius puckRadius = new Radius(0.08, 0.08 * TestUtils.BOARD_ASPECT_RATIO);

        Puck puckQ1 = Puck.create(TestUtils.PUCK_POS, puckRadius);
        Puck puckQ2 = Puck.create(TestUtils.createPosition(45, 40), puckRadius);
        Puck puckQ3 = Puck.create(TestUtils.createPosition(85, 40), puckRadius);
        Puck puckQ4 = Puck.create(TestUtils.createPosition(85, 75), puckRadius);

        Vector vector1 = Vector.from(handle.getPosition(), puckQ1.getPosition());
        Vector vector2 = Vector.from(handle.getPosition(), puckQ2.getPosition());
        Vector vector3 = Vector.from(handle.getPosition(), puckQ3.getPosition());
        Vector vector4 = Vector.from(handle.getPosition(), puckQ4.getPosition());

        double angle1 = vector1.angle(TestUtils.BOARD_ASPECT_RATIO);
        double angle2 = vector2.angle(TestUtils.BOARD_ASPECT_RATIO);
        double angle3 = vector3.angle(TestUtils.BOARD_ASPECT_RATIO);
        double angle4 = vector4.angle(TestUtils.BOARD_ASPECT_RATIO);

        Circle puckStartQ1 = createCircle(Puck.copyOf(puckQ1), Color.BLUE);
        Circle puckStartQ2 = createCircle(Puck.copyOf(puckQ2), Color.BLUE);
        Circle puckStartQ3 = createCircle(Puck.copyOf(puckQ3), Color.BLUE);
        Circle puckStartQ4 = createCircle(Puck.copyOf(puckQ4), Color.BLUE);

        Position pEdgePos = puckQ1.getRadiusEdgePosition(angle1);
        Position hEdgePos = handle.getRadiusEdgePosition(angle1 + Math.PI);
        Circle pEdge = new Circle(w(pEdgePos.x()), h(pEdgePos.y()), 2);
        Circle hEdge = new Circle(w(hEdgePos.x()), h(hEdgePos.y()), 2);
        Line line = createLine(vector1, puckQ1.getPosition());

        puckQ1.offsetCollisionWith(handle, angle1);
        puckQ2.offsetCollisionWith(handle, angle2);
        puckQ3.offsetCollisionWith(handle, angle3);
        puckQ4.offsetCollisionWith(handle, angle4);

        Pane pane = new Pane(
                puckStartQ1,
                puckStartQ2,
                puckStartQ3,
                puckStartQ4,
                createCircle(handle, Color.RED),
                pEdge,
                hEdge,
                line,
                createCircle(puckQ1, Color.CYAN),
                createCircle(puckQ2, Color.GREEN),
                createCircle(puckQ3, Color.CYAN),
                createCircle(puckQ4, Color.GREEN));

        pane.setPrefSize(TestUtils.BOARD_WIDTH, TestUtils.BOARD_HEIGHT);

        Scene scene = new Scene(pane);
        stage.setScene(scene);
        stage.setTitle("Canvas test");
        stage.show();


    }
}

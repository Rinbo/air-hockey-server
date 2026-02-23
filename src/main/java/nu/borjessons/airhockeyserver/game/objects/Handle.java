package nu.borjessons.airhockeyserver.game.objects;

import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Radius;
import nu.borjessons.airhockeyserver.game.properties.Speed;

import java.util.Objects;

public final class Handle extends Circle {
    private volatile double speedX;
    private volatile double speedY;
    private double previousX;
    private double previousY;

    private Handle(Position position, Radius radius) {
        super(position, radius);

        this.previousX = position.x();
        this.previousY = position.y();
    }

    public static Handle create(Position position) {
        Objects.requireNonNull(position, "position must not be null");

        return new Handle(position, GameConstants.HANDLE_RADIUS);
    }

    public static Handle create(Position position, Radius radius) {
        Objects.requireNonNull(position, "position must not be null");
        Objects.requireNonNull(radius, "radius must not be null");

        return new Handle(position, radius);
    }

    public Speed getSpeed() {
        return new Speed(speedX, speedY);
    }

    public void updateSpeed() {
        Position position = super.getPosition();
        speedX = position.x() - previousX;
        speedY = position.y() - previousY;
        previousX = position.x();
        previousY = position.y();
    }
}

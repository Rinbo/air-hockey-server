package nu.borjessons.airhockeyserver.game.objects;

import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Radius;
import nu.borjessons.airhockeyserver.game.properties.Speed;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class Handle extends Circle {
    private final AtomicReference<Speed> speedReference;
    private Position previousPosition;

    private Handle(Position position, Radius radius) {
        super(position, radius);

        this.previousPosition = position;
        this.speedReference = new AtomicReference<>(GameConstants.ZERO_SPEED);
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
        return speedReference.get();
    }

    public void setSpeed(Speed speed) {
        speedReference.set(speed);
    }

    public void updateSpeed() {
        Position position = super.getPosition();
        setSpeed(new Speed(position.x() - previousPosition.x(), position.y() - previousPosition.y()));
        previousPosition = position;
    }
}

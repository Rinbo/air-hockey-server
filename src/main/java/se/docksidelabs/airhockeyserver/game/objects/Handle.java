package se.docksidelabs.airhockeyserver.game.objects;

import se.docksidelabs.airhockeyserver.game.properties.GameConstants;
import se.docksidelabs.airhockeyserver.game.properties.Position;
import se.docksidelabs.airhockeyserver.game.properties.Radius;
import se.docksidelabs.airhockeyserver.game.properties.Speed;

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

    public Position getPreviousPosition() {
        return new Position(previousX, previousY);
    }

    public Speed getSpeed() {
        return new Speed(speedX, speedY);
    }

    @Override
    public void setPosition(Position position) {
        Position current = getPosition();
        double dx = position.x() - current.x();
        double dy = position.y() - current.y();
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > GameConstants.MAX_HANDLE_DISTANCE) {
            double scale = GameConstants.MAX_HANDLE_DISTANCE / dist;
            position = new Position(current.x() + dx * scale, current.y() + dy * scale);
        }
        super.setPosition(position);
    }

    /**
     * Set the handle position without velocity clamping.
     * Used for resets and initialization only.
     */
    public void forcePosition(Position position) {
        super.setPosition(position);
    }

    public void updateSpeed() {
        Position position = super.getPosition();
        speedX = position.x() - previousX;
        speedY = position.y() - previousY;
        previousX = position.x();
        previousY = position.y();
    }
}

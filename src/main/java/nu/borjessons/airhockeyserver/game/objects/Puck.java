package nu.borjessons.airhockeyserver.game.objects;

import nu.borjessons.airhockeyserver.game.properties.*;

import java.util.Objects;

public final class Puck extends Circle {
    private Speed speed;

    private Puck(Position position, Radius radius) {
        super(position, radius);

        speed = GameConstants.ZERO_SPEED;
    }

    public static Puck copyOf(Puck puck) {
        return Puck.create(puck.getPosition(), puck.getRadius());
    }

    public static Puck create(Position position) {
        return Puck.create(position, GameConstants.PUCK_RADIUS);
    }

    public static Puck create(Position position, Radius radius) {
        Objects.requireNonNull(position, "position must not be null");
        Objects.requireNonNull(radius, "radius must not be null");

        return new Puck(position, radius);
    }

    private static double dotProduct(Speed speed, Vector vector) {
        return speed.x() * vector.x() + speed.y() * vector.y();
    }

    public Speed getSpeed() {
        return speed;
    }

    public void setSpeed(Speed speed) {
        this.speed = speed;
    }

    public void move() {
        Position position = super.getPosition();
        Radius radius = getRadius();

        // TODO add some event if we see that we actually reached the limit? Right now handle can force the puck to become stale
        // We could trigger a small speed change to the puck if it gets stuck at them limit?
        // Maybe a special event at the very end. If puck y speed is zero and we are at the margin, give it a little push?
        double x = Math.max(0 + radius.x(), position.x() + speed.x());
        double y = Math.max(0 + radius.y(), position.y() + speed.y());
        setPosition(new Position(Math.min(1 - radius.x(), x), Math.min(1 - radius.y(), y)));
    }


    public void offsetCollisionWith(Handle handle, double angle) {
        Position handleRadiusEdgePos = handle.getRadiusEdgePosition(angle + Math.PI);
        Position puckRadiusEdgePosition = super.getRadiusEdgePosition(angle);

        Position position = getPosition();
        double xOffset = handleRadiusEdgePos.x() - puckRadiusEdgePosition.x();
        double yOffset = handleRadiusEdgePos.y() - puckRadiusEdgePosition.y();
        setPosition(new Position(position.x() + xOffset, position.y() + yOffset));
    }

    public void ricochet(Handle handle) {
        Vector vector = Vector.from(handle.getPosition(), getPosition());
        double scalar = dotProduct(speed, vector) / dotProduct(Speed.from(vector), vector) * -1;

        speed = new Speed(vector.x() * scalar, vector.y() * scalar);
    }
}

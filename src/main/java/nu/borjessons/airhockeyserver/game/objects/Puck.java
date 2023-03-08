package nu.borjessons.airhockeyserver.game.objects;

import nu.borjessons.airhockeyserver.game.properties.*;

import java.util.Objects;

public final class Puck extends Circle {
    private long friction;
    private Speed speed;

    private Puck(Position position, Radius radius) {
        super(position, radius);

        friction = 0;
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

    private static double getRecoverySpeed(double yCoordinate, double yRadius) {
        if (yCoordinate == 0 + yRadius) return 1;
        if (yCoordinate == 1 - yRadius) return -1;
        return 0;
    }

    public void resetFriction() {
        this.friction = 0;
    }

    private double getFrictionCoefficient() {
        return 1 / ((GameConstants.FRICTION_MODIFIER + friction) / GameConstants.FRICTION_MODIFIER);
    }

    public Speed getSpeed() {
        return speed;
    }

    public void setSpeed(Speed speed) {
        this.speed = speed;
    }

    public void onTick() {
        Position position = super.getPosition();
        Radius radius = getRadius();

        movePuck(position, radius);
        handleFriction();
        handleStalePuck(position, radius);
    }

    private void handleStalePuck(Position position, Radius radius) {
        if (speed.y() == 0) setSpeed(new Speed(speed.x(), getRecoverySpeed(position.y(), radius.y())));
    }

    private void handleFriction() {
        double frictionCoefficient = getFrictionCoefficient();
        setSpeed(new Speed(speed.x() * frictionCoefficient, speed.y() * frictionCoefficient));
        this.friction++;
    }

/*    private void movePuck(Position position, Radius radius) {
        double x = Math.max(0 + radius.x(), position.x() + speed.x());
        double y = Math.max(0 + radius.y(), position.y() + speed.y());
        setPosition(new Position(Math.min(1 - radius.x(), x), Math.min(1 - radius.y(), y)));
    }*/

    private void movePuck(Position position, Radius radius) {
        setPosition(new Position(position.x() + speed.x(), position.y() + speed.y()));
    }


    public void offsetCollisionWith(Handle handle, double angle) {
        Position handleRadiusEdgePos = handle.getRadiusEdgePosition(angle + Math.PI);
        Position puckRadiusEdgePosition = super.getRadiusEdgePosition(angle);

        Position position = getPosition();
        double xOffset = handleRadiusEdgePos.x() - puckRadiusEdgePosition.x();
        double yOffset = handleRadiusEdgePos.y() - puckRadiusEdgePosition.y();
        setPosition(new Position(position.x() + xOffset, position.y() + yOffset));
    }

    public void ricochet(Vector vector) {
        double scalar = dotProduct(speed, vector) / dotProduct(Speed.from(vector), vector) * -1;
        speed = new Speed(vector.x() * scalar, vector.y() * scalar);
    }
}

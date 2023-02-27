package nu.borjessons.airhockeyserver.game.properties;

public record Speed(double x, double y) {
    public static Speed from(Vector vector) {
        return new Speed(vector.x(), vector.y());
    }
}

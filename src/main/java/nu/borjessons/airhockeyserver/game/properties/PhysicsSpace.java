package nu.borjessons.airhockeyserver.game.properties;

/**
 * Converts between the normalized coordinate system (x ∈ [0,1], y ∈ [0,1])
 * and a physical coordinate system where distances are isotropic (1 unit of x
 * equals 1 unit of y in real space).
 * <p>
 * The board is taller than it is wide (aspect ratio = width / height ≈ 0.625).
 * In normalized coords a vertical distance of 0.1 covers more real pixels than
 * a horizontal distance of 0.1. Physical space corrects this by stretching Y
 * so that both axes use the same scale (width = 1 unit).
 * <p>
 * Physical Y = normalized Y / aspect ratio
 * Normalized Y = physical Y * aspect ratio
 */
public final class PhysicsSpace {

    private PhysicsSpace() {
        throw new IllegalStateException();
    }

    public static double toPhysicalY(double normalizedY) {
        return normalizedY / GameConstants.BOARD_ASPECT_RATIO;
    }

    public static double toNormalizedY(double physicalY) {
        return physicalY * GameConstants.BOARD_ASPECT_RATIO;
    }
}

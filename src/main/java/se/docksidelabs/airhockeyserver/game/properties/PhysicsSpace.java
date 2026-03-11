package se.docksidelabs.airhockeyserver.game.properties;

/**
 * Converts between normalized and physical coordinate spaces.
 *
 * <p>The board is taller than it is wide (aspect ratio ≈ 0.625). In
 * normalized coords a vertical distance of 0.1 covers more real
 * pixels than a horizontal distance of 0.1. Physical space stretches
 * Y so that both axes use the same scale (1 unit = board width).
 *
 * <pre>
 *   Physical Y  = normalized Y / aspect ratio
 *   Normalized Y = physical Y × aspect ratio
 * </pre>
 */
public final class PhysicsSpace {

  private PhysicsSpace() {
    throw new IllegalStateException("Utility class");
  }

  public static double toPhysicalY(double normalizedY) {
    return normalizedY / GameConstants.BOARD_ASPECT_RATIO;
  }

  public static double toNormalizedY(double physicalY) {
    return physicalY * GameConstants.BOARD_ASPECT_RATIO;
  }
}

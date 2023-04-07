package nu.borjessons.airhockeyserver.utils;

import java.util.Locale;

public final class AppUtils {
  private AppUtils() {
    throw new IllegalStateException();
  }

  public static String format(String message, Object... args) {
    return String.format(Locale.ROOT, message, args);
  }
}

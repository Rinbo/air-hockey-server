package nu.borjessons.airhockeyserver.engine;

import java.util.Optional;

final class ThreadHolder {
  private Thread thread;

  Optional<Thread> getThread() {
    return Optional.ofNullable(thread);
  }

  void setThread(Thread thread) {
    this.thread = thread;
  }
}

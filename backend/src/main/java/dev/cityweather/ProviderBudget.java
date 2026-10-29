package dev.cityweather;

import java.time.Instant;
import java.util.ArrayDeque;

/** Conservative per-instance outgoing budget, including failed requests. */
final class ProviderBudget {
  static class Exhausted extends RuntimeException {}

  private final ArrayDeque<Long> calls = new ArrayDeque<>();

  synchronized void acquire() {
    long now = Instant.now().getEpochSecond();
    while (!calls.isEmpty() && calls.peekFirst() <= now - 86400) calls.removeFirst();
    long minute = calls.stream().filter(t -> t > now - 60).count();
    long hour = calls.stream().filter(t -> t > now - 3600).count();
    if (calls.size() >= 9000 || hour >= 4500 || minute >= 500) throw new Exhausted();
    calls.addLast(now);
  }
}

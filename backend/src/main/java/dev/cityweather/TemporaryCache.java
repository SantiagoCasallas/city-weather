package dev.cityweather;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Bounded, expiring cache. Failures are never stored; synchronized load coalesces requests. */
final class TemporaryCache<T> {
  private record Entry<T>(T value, long expires) {}

  private final Map<String, Entry<T>> entries = new LinkedHashMap<>(16, .75f, true);
  private final long ttlNanos;
  private final int limit;

  TemporaryCache(Duration ttl, int limit) {
    this.ttlNanos = ttl.toNanos();
    this.limit = limit;
  }

  synchronized T get(String key, Supplier<T> load) {
    long now = System.nanoTime();
    entries.entrySet().removeIf(e -> now - e.getValue().expires() >= 0);
    Entry<T> hit = entries.get(key);
    if (hit != null) return hit.value();
    T value = load.get();
    if (entries.size() >= limit) entries.remove(entries.keySet().iterator().next());
    entries.put(key, new Entry<>(value, System.nanoTime() + ttlNanos));
    return value;
  }
}

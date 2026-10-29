package dev.cityweather;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TemporaryCacheTest {
  @Test
  void expiresEvictsAndDoesNotCacheFailures() throws Exception {
    var cache = new TemporaryCache<Integer>(Duration.ofMillis(20), 1);
    var calls = new AtomicInteger();
    assertEquals(1, cache.get("a", calls::incrementAndGet));
    assertEquals(1, cache.get("a", calls::incrementAndGet));
    Thread.sleep(30);
    assertEquals(2, cache.get("a", calls::incrementAndGet));
    cache.get("b", calls::incrementAndGet);
    assertEquals(4, cache.get("a", calls::incrementAndGet));
    assertThrows(
        IllegalStateException.class,
        () ->
            cache.get(
                "bad",
                () -> {
                  throw new IllegalStateException();
                }));
    assertEquals(5, cache.get("bad", calls::incrementAndGet));
  }

  @Test
  void enforcesOutgoingBudget() {
    var budget = new ProviderBudget();
    for (int i = 0; i < 500; i++) budget.acquire();
    assertThrows(ProviderBudget.Exhausted.class, budget::acquire);
  }
}

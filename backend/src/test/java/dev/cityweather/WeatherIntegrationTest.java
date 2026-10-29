package dev.cityweather;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WeatherIntegrationTest {
  static HttpServer provider;
  static ExecutorService executor = Executors.newCachedThreadPool();
  static AtomicInteger calls = new AtomicInteger();
  static AtomicInteger status = new AtomicInteger(200);
  static AtomicLong delay = new AtomicLong();
  static AtomicReference<String> payload = new AtomicReference<>("{}");
  static AtomicReference<String> path = new AtomicReference<>();

  static {
    try {
      provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      provider.createContext(
          "/",
          exchange -> {
            calls.incrementAndGet();
            path.set(exchange.getRequestURI().toString());
            try {
              Thread.sleep(delay.get());
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            byte[] body = payload.get().getBytes(StandardCharsets.UTF_8);
            try {
              exchange.getResponseHeaders().set("Content-Type", "application/json");
              exchange.sendResponseHeaders(status.get(), body.length);
              exchange.getResponseBody().write(body);
            } finally {
              exchange.close();
            }
          });
      provider.setExecutor(executor);
      provider.start();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    String base = "http://127.0.0.1:" + provider.getAddress().getPort();
    r.add("weather.geocoding-url", () -> base);
    r.add("weather.forecast-url", () -> base);
    r.add("weather.timeout-ms", () -> 500);
  }

  @Autowired MockMvc api;

  @BeforeEach
  void reset() {
    calls.set(0);
    status.set(200);
    delay.set(0);
    payload.set("{}");
  }

  @AfterAll
  static void stop() {
    provider.stop(0);
    executor.shutdownNow();
  }

  @Test
  void ambiguousCitiesAndNormalizedCache() throws Exception {
    payload.set(
        """
        {"results":[{"id":1,"name":"Córdoba","admin1":"Andalucía","country":"España","country_code":"ES","latitude":37.88,"longitude":-4.77,"timezone":"Europe/Madrid"},{"id":2,"name":"Córdoba","admin1":"Córdoba","country":"Argentina","country_code":"AR","latitude":-31.4,"longitude":-64.18,"timezone":"America/Argentina/Cordoba"}]}
        """);
    api.perform(get("/api/cities").param("q", "  Córdoba  "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[1].country").value("Argentina"));
    api.perform(get("/api/cities").param("q", "córdoba")).andExpect(status().isOk());
    assertEquals(1, calls.get());
    assertTrue(path.get().contains("language=es"));
  }

  @Test
  void validatesBeforeCallingProvider() throws Exception {
    for (String q : new String[] {"a", " ", "<script>", "x".repeat(81)})
      api.perform(get("/api/cities").param("q", q)).andExpect(status().isBadRequest());
    for (String lat : new String[] {"91", "NaN", "Infinity", "abc"})
      api.perform(get("/api/weather").param("latitude", lat).param("longitude", "0"))
          .andExpect(status().isBadRequest());
    api.perform(get("/api/weather").param("latitude", "0").param("longitude", "181"))
        .andExpect(status().isBadRequest());
    api.perform(get("/api/cities")).andExpect(status().isBadRequest());
    assertEquals(0, calls.get());
  }

  @Test
  void noMatchesIsEmptyArray() throws Exception {
    api.perform(get("/api/cities").param("q", "zzzzzz"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void providerFailureIsNotCached() throws Exception {
    status.set(503);
    api.perform(get("/api/cities").param("q", "Failure"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("PROVIDER_UNAVAILABLE"));
    status.set(200);
    api.perform(get("/api/cities").param("q", "Failure")).andExpect(status().isOk());
    assertEquals(2, calls.get());
  }

  @Test
  void malformedProviderResponseIsSafe() throws Exception {
    payload.set("{bad");
    api.perform(get("/api/cities").param("q", "Malformed")).andExpect(status().isBadGateway());
    payload.set("{\"daily\":{}}");
    api.perform(get("/api/weather").param("latitude", "10").param("longitude", "0"))
        .andExpect(status().isBadGateway());
  }

  @Test
  void timeoutReturnsActionable504() throws Exception {
    delay.set(1500);
    api.perform(get("/api/cities").param("q", "Timeout"))
        .andExpect(status().isGatewayTimeout())
        .andExpect(jsonPath("$.code").value("PROVIDER_TIMEOUT"));
  }

  @Test
  void forecastPreservesCityCalendarUnitsNullsAndCache() throws Exception {
    payload.set(forecastJson());
    api.perform(get("/api/weather").param("latitude", "4.71").param("longitude", "-74.07"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timezone").value("America/Bogota"))
        .andExpect(jsonPath("$.days.length()").value(7))
        .andExpect(jsonPath("$.days[0].date").value("2026-10-29"))
        .andExpect(jsonPath("$.days[6].date").value("2026-11-04"))
        .andExpect(jsonPath("$.current.temperature").value(18.5))
        .andExpect(jsonPath("$.current.humidity").isEmpty())
        .andExpect(jsonPath("$.units.temperature").value("°C"));
    assertTrue(path.get().contains("forecast_days=8"));
    assertTrue(path.get().contains("timezone=auto"));
    api.perform(get("/api/weather").param("latitude", "4.71").param("longitude", "-74.07"))
        .andExpect(status().isOk());
    assertEquals(1, calls.get());
  }

  static String forecastJson() {
    return """
    {"timezone":"America/Bogota","current":{"time":"2026-10-28T23:45","temperature_2m":18.5,"apparent_temperature":18,"relative_humidity_2m":null,"weather_code":3,"wind_speed_10m":5,"is_day":0},"current_units":{"temperature_2m":"°C","relative_humidity_2m":"%","wind_speed_10m":"km/h"},"daily_units":{"precipitation_probability_max":"%"},"daily":{"time":["2026-10-28","2026-10-29","2026-10-30","2026-10-31","2026-11-01","2026-11-02","2026-11-03","2026-11-04"],"temperature_2m_min":[10,11,12,13,14,15,16,17],"temperature_2m_max":[20,21,22,23,24,25,26,27],"weather_code":[0,1,2,3,61,95,45,0],"precipitation_probability_max":[0,10,20,30,40,50,60,70]}}
    """;
  }
}

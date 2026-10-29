package dev.cityweather;

import static dev.cityweather.WeatherModels.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
public class WeatherService {
  private final RestClient geo, weather;
  private final ProviderBudget budget = new ProviderBudget();
  private final TemporaryCache<List<City>> cities;
  private final TemporaryCache<Forecast> forecasts;

  public WeatherService(
      @Value("${weather.geocoding-url}") String geoUrl,
      @Value("${weather.forecast-url}") String weatherUrl,
      @Value("${weather.timeout-ms}") int timeout,
      @Value("${weather.cache-seconds}") int ttl,
      @Value("${weather.cache-size}") int size) {
    if (timeout < 1 || ttl < 1 || size < 1)
      throw new IllegalArgumentException("Invalid weather configuration");
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeout)).build());
    factory.setReadTimeout(Duration.ofMillis(timeout));
    geo = RestClient.builder().baseUrl(geoUrl).requestFactory(factory).build();
    weather = RestClient.builder().baseUrl(weatherUrl).requestFactory(factory).build();
    cities = new TemporaryCache<>(Duration.ofSeconds(ttl), size);
    forecasts = new TemporaryCache<>(Duration.ofSeconds(ttl), size);
  }

  public List<City> search(String query) {
    return cities.get(
        query.toLowerCase(Locale.ROOT),
        () ->
            guarded(
                () -> {
                  JsonNode body =
                      geo.get()
                          .uri(
                              b ->
                                  b.path("/v1/search")
                                      .queryParam("name", "{q}")
                                      .queryParam("count", 10)
                                      .queryParam("language", "es")
                                      .queryParam("format", "json")
                                      .build(query))
                          .retrieve()
                          .body(JsonNode.class);
                  if (body == null || !body.isObject() || body.has("error"))
                    throw new IllegalStateException();
                  var results = body.path("results");
                  if (results.isMissingNode()) return List.of();
                  if (!results.isArray()) throw new IllegalStateException();
                  List<City> list = new ArrayList<>();
                  for (JsonNode n : results)
                    list.add(
                        new City(
                            n.required("id").longValue(),
                            requiredText(n, "name"),
                            n.path("admin1").asText(""),
                            n.path("country").asText(""),
                            n.path("country_code").asText(""),
                            number(n, "latitude"),
                            number(n, "longitude"),
                            requiredText(n, "timezone")));
                  return List.copyOf(list);
                }));
  }

  public Forecast forecast(double latitude, double longitude) {
    String key = latitude + ":" + longitude;
    return forecasts.get(
        key,
        () ->
            guarded(
                () -> {
                  JsonNode n =
                      weather
                          .get()
                          .uri(
                              b ->
                                  b.path("/v1/forecast")
                                      .queryParam("latitude", latitude)
                                      .queryParam("longitude", longitude)
                                      .queryParam(
                                          "current",
                                          "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day")
                                      .queryParam(
                                          "daily",
                                          "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                                      .queryParam("timezone", "auto")
                                      .queryParam("forecast_days", 8)
                                      .queryParam("temperature_unit", "celsius")
                                      .queryParam("wind_speed_unit", "kmh")
                                      .build())
                          .retrieve()
                          .body(JsonNode.class);
                  if (n == null) throw new IllegalStateException();
                  String zone = requiredText(n, "timezone");
                  ZoneId.of(zone);
                  JsonNode c = n.required("current"),
                      d = n.required("daily"),
                      u = n.required("current_units"),
                      du = n.required("daily_units");
                  String time = requiredText(c, "time");
                  LocalDate today = LocalDateTime.parse(time).toLocalDate();
                  List<Day> days = new ArrayList<>();
                  String[] fields = {
                    "time",
                    "temperature_2m_min",
                    "temperature_2m_max",
                    "weather_code",
                    "precipitation_probability_max"
                  };
                  for (String f : fields)
                    if (!d.path(f).isArray() || d.path(f).size() != 8)
                      throw new IllegalStateException();
                  for (int i = 1; i < 8; i++) {
                    String date = d.path("time").get(i).asText();
                    if (!LocalDate.parse(date).equals(today.plusDays(i)))
                      throw new IllegalStateException();
                    days.add(
                        new Day(
                            date,
                            nullableNumber(d.path("temperature_2m_min").get(i)),
                            nullableNumber(d.path("temperature_2m_max").get(i)),
                            nullableInt(d.path("weather_code").get(i)),
                            nullableNumber(d.path("precipitation_probability_max").get(i))));
                  }
                  return new Forecast(
                      latitude,
                      longitude,
                      zone,
                      Instant.now(),
                      new Current(
                          time,
                          nullableNumber(c.required("temperature_2m")),
                          nullableNumber(c.required("apparent_temperature")),
                          nullableNumber(c.required("relative_humidity_2m")),
                          nullableNumber(c.required("wind_speed_10m")),
                          nullableInt(c.required("weather_code")),
                          c.required("is_day").asInt() == 1),
                      new Units(
                          requiredText(u, "temperature_2m"),
                          requiredText(u, "wind_speed_10m"),
                          requiredText(u, "relative_humidity_2m"),
                          requiredText(du, "precipitation_probability_max")),
                      List.copyOf(days));
                }));
  }

  private static String requiredText(JsonNode n, String field) {
    var v = n.required(field);
    if (!v.isTextual() || v.asText().isBlank()) throw new IllegalStateException();
    return v.asText();
  }

  private static double number(JsonNode n, String field) {
    var v = nullableNumber(n.required(field));
    if (v == null) throw new IllegalStateException();
    return v;
  }

  private static Double nullableNumber(JsonNode n) {
    if (n.isNull()) return null;
    if (!n.isNumber() || !Double.isFinite(n.doubleValue())) throw new IllegalStateException();
    return n.doubleValue();
  }

  private static Integer nullableInt(JsonNode n) {
    if (n.isNull()) return null;
    if (!n.isIntegralNumber()) throw new IllegalStateException();
    return n.intValue();
  }

  private <T> T guarded(java.util.function.Supplier<T> fn) {
    try {
      budget.acquire();
      return fn.get();
    } catch (ResourceAccessException e) {
      Throwable cause = e;
      while (cause != null) {
        if (cause instanceof HttpTimeoutException
            || cause instanceof java.net.SocketTimeoutException) throw new ProviderException(true);
        cause = cause.getCause();
      }
      throw new ProviderException(false);
    } catch (ProviderBudget.Exhausted e) {
      throw e;
    } catch (RuntimeException e) {
      throw new ProviderException(false);
    }
  }
}

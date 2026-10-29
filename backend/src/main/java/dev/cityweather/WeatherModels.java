package dev.cityweather;

import java.time.Instant;
import java.util.List;

public final class WeatherModels {
  private WeatherModels() {}

  public record City(
      long id,
      String name,
      String region,
      String country,
      String countryCode,
      double latitude,
      double longitude,
      String timezone) {}

  public record Current(
      String time,
      Double temperature,
      Double apparentTemperature,
      Double humidity,
      Double windSpeed,
      Integer weatherCode,
      boolean isDay) {}

  public record Day(
      String date, Double min, Double max, Integer weatherCode, Double precipitationProbability) {}

  public record Units(
      String temperature, String windSpeed, String humidity, String precipitationProbability) {}

  public record Forecast(
      double latitude,
      double longitude,
      String timezone,
      Instant fetchedAt,
      Current current,
      Units units,
      List<Day> days) {}
}

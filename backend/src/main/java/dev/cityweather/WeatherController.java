package dev.cityweather;

import static dev.cityweather.WeatherModels.*;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class WeatherController {
  private final WeatherService service;

  public WeatherController(WeatherService service) {
    this.service = service;
  }

  @GetMapping("/health")
  public java.util.Map<String, String> health() {
    return java.util.Map.of("status", "UP");
  }

  @GetMapping("/cities")
  public List<City> cities(@RequestParam String q) {
    String query = q.strip().replaceAll("\\s+", " ");
    if (query.length() < 2 || query.length() > 80 || !query.matches("[\\p{L}\\p{M}0-9 .,'’()\\-]+"))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Escribe entre 2 y 80 caracteres: letras, números o puntuación de un lugar.");
    return service.search(query);
  }

  @GetMapping("/weather")
  public Forecast weather(@RequestParam double latitude, @RequestParam double longitude) {
    if (!Double.isFinite(latitude)
        || !Double.isFinite(longitude)
        || latitude < -90
        || latitude > 90
        || longitude < -180
        || longitude > 180)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las coordenadas no son válidas.");
    return service.forecast(latitude, longitude);
  }
}

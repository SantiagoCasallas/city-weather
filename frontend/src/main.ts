import { bootstrapApplication } from "@angular/platform-browser";
import {
  provideHttpClient,
  HttpClient,
  HttpErrorResponse,
} from "@angular/common/http";
import {
  Component,
  ElementRef,
  ViewChild,
  inject,
  signal,
} from "@angular/core";
import { FormsModule } from "@angular/forms";
import { DecimalPipe } from "@angular/common";
import { Subscription, timeout } from "rxjs";
import {
  City,
  Forecast,
  condition,
  currentCondition,
  dayLabel,
  utcLabel,
} from "./weather";

@Component({
  selector: "app-root",
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: "./app.html",
})
export class App {
  private http = inject(HttpClient);
  private searchRequest?: Subscription;
  private weatherRequest?: Subscription;
  @ViewChild("weatherTitle") weatherTitle?: ElementRef<HTMLHeadingElement>;
  @ViewChild("resultsTitle") resultsTitle?: ElementRef<HTMLHeadingElement>;
  query = "";
  cities = signal<City[]>([]);
  searched = signal(false);
  searching = signal(false);
  loading = signal(false);
  selected = signal<City | null>(null);
  forecast = signal<Forecast | null>(null);
  searchError = signal("");
  weatherError = signal("");
  condition = condition;
  currentCondition = currentCondition;
  utcLabel = utcLabel;
  dayLabel = dayLabel;
  search() {
    this.searchRequest?.unsubscribe();
    this.cities.set([]);
    this.searched.set(false);
    this.searchError.set("");
    this.searching.set(false);
    const q = this.query.trim().replace(/\s+/g, " ");
    if (
      q.length < 2 ||
      q.length > 80 ||
      !/^[\p{L}\p{M}0-9 .,'’()\-]+$/u.test(q)
    ) {
      this.searchError.set(
        "Escribe entre 2 y 80 caracteres: letras, números o puntuación de un lugar.",
      );
      return;
    }
    this.searching.set(true);
    this.searchRequest = this.http
      .get<City[]>("/api/cities", { params: { q } })
      .pipe(timeout(20000))
      .subscribe({
        next: (data) => {
          this.cities.set(data);
          this.searched.set(true);
          this.searching.set(false);
          setTimeout(() => this.resultsTitle?.nativeElement.focus());
        },
        error: (e) => {
          this.searching.set(false);
          this.searchError.set(this.message(e));
        },
      });
  }
  select(city: City) {
    this.weatherRequest?.unsubscribe();
    this.selected.set(city);
    this.forecast.set(null);
    this.weatherError.set("");
    this.loading.set(true);
    this.weatherRequest = this.http
      .get<Forecast>("/api/weather", {
        params: { latitude: city.latitude, longitude: city.longitude },
      })
      .pipe(timeout(20000))
      .subscribe({
        next: (data) => {
          this.forecast.set(data);
          this.loading.set(false);
          setTimeout(() => this.weatherTitle?.nativeElement.focus());
        },
        error: (e) => {
          this.loading.set(false);
          this.weatherError.set(this.message(e));
        },
      });
  }
  private message(e: unknown): string {
    if (e instanceof HttpErrorResponse && typeof e.error?.message === "string")
      return e.error.message;
    return "No pudimos conectar con el servicio. Espera unos momentos y vuelve a intentarlo.";
  }
  location(city: City): string {
    return [city.region, city.country].filter(Boolean).join(", ");
  }
}
bootstrapApplication(App, { providers: [provideHttpClient()] }).catch(
  console.error,
);

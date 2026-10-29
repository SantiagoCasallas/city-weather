import { defineConfig, devices } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  timeout: 30000,
  use: { baseURL: "http://127.0.0.1:14200", trace: "retain-on-failure" },
  projects: [
    { name: "desktop", use: { ...devices["Desktop Chrome"] } },
    { name: "mobile", use: { ...devices["Pixel 7"] } },
  ],
  webServer: [
    {
      command: "python3 e2e/provider.py",
      url: "http://127.0.0.1:9099/health",
      reuseExistingServer: false,
    },
    {
      command: "java -jar ../backend/target/city-weather-0.1.0.jar",
      url: "http://127.0.0.1:18080/api/health",
      reuseExistingServer: false,
      env: {
        PORT: "18080",
        GEOCODING_URL: "http://127.0.0.1:9099",
        FORECAST_URL: "http://127.0.0.1:9099",
      },
    },
    {
      command:
        "node node_modules/@angular/cli/bin/ng.js serve --host 127.0.0.1 --port 14200 --proxy-config proxy.e2e.json",
      url: "http://127.0.0.1:14200",
      reuseExistingServer: false,
    },
  ],
});

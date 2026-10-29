import { test, expect } from "@playwright/test";
test("keyboard search → disambiguate → real backend → simulated external provider", async ({
  page,
}) => {
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "El mundo cambia. Consulta su clima." }),
  ).toBeVisible();
  await expect(page.locator(".temperature")).toHaveCount(0);
  const search = page.getByRole("textbox", {
    name: "¿Qué ciudad tienes en mente?",
  });
  await search.fill("Córdoba");
  await search.press("Enter");
  await expect(page.locator("#results-title")).toBeFocused();
  await expect(
    page.getByRole("button", { name: /Córdoba Andalucía, España/ }),
  ).toBeVisible();
  const argentina = page.getByRole("button", {
    name: /Córdoba Córdoba, Argentina/,
  });
  await argentina.focus();
  await page.keyboard.press("Enter");
  await expect(page.locator("#weather-title")).toBeFocused();
  await expect(page.locator(".temperature")).toContainText("18.5");
  await expect(page.locator(".timezone")).toHaveText(
    "America/Argentina/Cordoba",
  );
  await expect(page.locator(".day-card")).toHaveCount(7);
  await expect(page.locator(".day-card").first()).toContainText("29 oct");
  await expect(page.locator(".day-card").last()).toContainText("4 nov");
  await expect(page.locator(".current-details")).toContainText("— %");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
});
test("clear empty and unavailable states without invented weather", async ({
  page,
}) => {
  await page.goto("/");
  const search = page.getByRole("textbox", {
    name: "¿Qué ciudad tienes en mente?",
  });
  await search.fill("sinresultados");
  await search.press("Enter");
  await expect(
    page.getByText("No encontramos ciudades.", { exact: false }),
  ).toBeVisible();
  await search.fill("error");
  await search.press("Enter");
  await expect(page.getByRole("alert")).toContainText(
    "No pudimos obtener datos del proveedor",
  );
  await expect(page.locator(".temperature")).toHaveCount(0);
});

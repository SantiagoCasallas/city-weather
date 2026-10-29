import { describe, it, expect } from "vitest";
import { condition, currentCondition, dayLabel, utcLabel } from "./weather";
describe("provider presentation", () => {
  it("does not label missing or unknown codes as sunshine", () => {
    expect(condition(null).label).toBe("Sin información");
    expect(condition(500).label).toBe("Condición desconocida");
  });
  it("covers WMO rain, freezing rain, snow and storms", () => {
    expect(condition(67).label).toBe("Lluvia");
    expect(condition(86).label).toBe("Nieve");
    expect(condition(99).label).toBe("Tormenta");
  });
  it("preserves provider calendar dates across browser timezones", () => {
    expect(dayLabel("2026-01-01")).toContain("1");
    expect(dayLabel("2026-01-01")).toContain("ene");
  });
});

it("uses night icons without changing WMO meaning", () => {
  expect(currentCondition(0, false).icon).toBe("☾");
  expect(currentCondition(2, false).label).toBe("Parcialmente nublado");
});
it("shows fetch instants explicitly in UTC", () => {
  expect(utcLabel("2026-10-29T04:20:00Z")).toContain("4:20");
});

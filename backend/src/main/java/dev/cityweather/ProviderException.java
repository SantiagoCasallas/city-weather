package dev.cityweather;

public class ProviderException extends RuntimeException {
  private final boolean timeout;

  public ProviderException(boolean timeout) {
    super("Proveedor de clima no disponible");
    this.timeout = timeout;
  }

  public boolean isTimeout() {
    return timeout;
  }
}

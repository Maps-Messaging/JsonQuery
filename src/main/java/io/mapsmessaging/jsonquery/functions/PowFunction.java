package io.mapsmessaging.jsonquery.functions;

public final class PowFunction extends AbstractNumericBinaryFunction {

  @Override
  public String getName() {
    return "pow";
  }

  @Override
  protected double apply(double left, double right) {
    return Math.pow(left, right);
  }

  @Override
  protected String symbol() {
    return "^";
  }
}

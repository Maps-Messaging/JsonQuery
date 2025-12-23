package io.mapsmessaging.jsonquery.functions.numeric;

public final class ModFunction extends AbstractNumericBinaryFunction {

  @Override
  public String getName() {
    return "mod";
  }

  @Override
  protected double apply(double left, double right) {
    if (right == 0.0) {
      throw new IllegalArgumentException("Division by zero");
    }
    return left % right;
  }

  @Override
  protected String symbol() {
    return "%";
  }
}

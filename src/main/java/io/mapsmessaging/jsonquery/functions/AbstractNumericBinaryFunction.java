package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractNumericBinaryFunction extends AbstractFunction {

  protected abstract double apply(double left, double right);

  protected abstract String symbol();

  @Override
  public final Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 2, "2 arguments: " + getName() + "(a,b)");
    Function<JsonElement, JsonElement> leftExpr = compileArg(rawArgs.get(0), compiler);
    Function<JsonElement, JsonElement> rightExpr = compileArg(rawArgs.get(1), compiler);

    return data -> {
      double leftValue = JsonQueryFunction.asNumber(leftExpr.apply(data), "Number expected");
      double rightValue = JsonQueryFunction.asNumber(rightExpr.apply(data), "Number expected");
      double result = apply(leftValue, rightValue);
      return JsonQueryFunction.numberValue(result);
    };
  }
}

package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

public final class RoundFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "round";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1 && rawArgs.size() != 2) {
      throw new IllegalArgumentException("round expects 1 or 2 arguments: round(value, [digits])");
    }

    Function<JsonElement, JsonElement> valueExpr = compileArg(rawArgs.get(0), compiler);
    Function<JsonElement, JsonElement> digitsExpr =
        (rawArgs.size() == 2) ? compileArg(rawArgs.get(1), compiler) : null;

    return data -> {
      double value = JsonQueryFunction.asNumber(valueExpr.apply(data), "Number expected");

      int digits = 0;
      if (digitsExpr != null) {
        digits = JsonQueryFunction.asInt(digitsExpr.apply(data), "Number expected");
      }

      BigDecimal bd = BigDecimal.valueOf(value);
      bd = bd.setScale(digits, RoundingMode.HALF_UP);

      return JsonQueryFunction.numberValue(bd.doubleValue());
    };
  }
}

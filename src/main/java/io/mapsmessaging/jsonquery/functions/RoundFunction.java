package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class RoundFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "round";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 1, "1 argument: round(value)");
    Function<JsonElement, JsonElement> arg = compileArg(rawArgs.get(0), compiler);

    return data -> {
      double value = JsonQueryFunction.asNumber(arg.apply(data), "Number expected");
      double roundedValue = Math.rint(value);
      return JsonQueryFunction.numberValue(roundedValue);
    };
  }
}

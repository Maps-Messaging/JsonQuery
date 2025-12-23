package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class NumberFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "number";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 1, "1 argument: number(value)");
    Function<JsonElement, JsonElement> arg = compileArg(rawArgs.get(0), compiler);

    return data -> {
      JsonElement value = arg.apply(data);
      if (JsonQueryFunction.isNull(value)) {
        return JsonNull.INSTANCE;
      }
      if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
        return value;
      }
      if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
        String text = value.getAsString().trim();
        if (text.isEmpty()) {
          return JsonNull.INSTANCE;
        }
        try {
          double parsed = Double.parseDouble(text);
          return JsonQueryFunction.numberValue(parsed);
        } catch (NumberFormatException exception) {
          return JsonNull.INSTANCE;
        }
      }
      return JsonNull.INSTANCE;
    };
  }
}

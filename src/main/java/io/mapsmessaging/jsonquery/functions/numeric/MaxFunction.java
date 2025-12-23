package io.mapsmessaging.jsonquery.functions.numeric;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.List;
import java.util.function.Function;

public final class MaxFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "max";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs,
                                                    JsonQueryCompiler compiler) {

    if (!rawArgs.isEmpty()) {
      throw new IllegalArgumentException("max expects 0 arguments");
    }

    return value -> {
      if (!value.isJsonArray() || value.getAsJsonArray().isEmpty()) {
        return new JsonPrimitive(0);
      }

      Double max = null;
      for (JsonElement e : value.getAsJsonArray()) {
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
          double v = e.getAsDouble();
          max = (max == null) ? v : Math.max(max, v);
        }
      }
      return new JsonPrimitive(max == null ? 0 : max);
    };
  }
}


package io.mapsmessaging.jsonquery.functions.numeric;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.List;
import java.util.function.Function;

public final class MinFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "min";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs,
                                                    JsonQueryCompiler compiler) {

    if (!rawArgs.isEmpty()) {
      throw new IllegalArgumentException("min expects 0 arguments");
    }

    return value -> {
      if (!value.isJsonArray() || value.getAsJsonArray().isEmpty()) {
        return new JsonPrimitive(0);
      }

      Double min = null;
      for (JsonElement e : value.getAsJsonArray()) {
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
          double v = e.getAsDouble();
          min = (min == null) ? v : Math.min(min, v);
        }
      }
      return new JsonPrimitive(min == null ? 0 : min);
    };
  }
}


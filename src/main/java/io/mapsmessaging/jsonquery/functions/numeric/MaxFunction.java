package io.mapsmessaging.jsonquery.functions.numeric;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
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

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        throw new IllegalArgumentException("Array expected");
      }

      JsonArray array = data.getAsJsonArray();
      if (array.isEmpty()) {
        return JsonNull.INSTANCE;
      }

      Double max = null;
      for (JsonElement element : array) {
        if (element == null || element.isJsonNull()) {
          continue;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
          throw new IllegalArgumentException("Number expected");
        }
        double value = element.getAsDouble();
        max = (max == null) ? value : Math.max(max, value);
      }

      return max == null ? JsonNull.INSTANCE : new JsonPrimitive(max);
    };
  }
}

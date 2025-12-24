package io.mapsmessaging.jsonquery.functions.numeric;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.List;
import java.util.function.Function;

public final class AverageFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "average";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (!rawArgs.isEmpty()) {
      throw new IllegalArgumentException("avg expects 0 arguments");
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

      double sum = 0.0;
      long count = 0;

      for (JsonElement element : array) {
        if (element == null || element.isJsonNull()) {
          continue;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
          throw new IllegalArgumentException("Number expected");
        }
        sum += element.getAsDouble();
        count++;
      }

      if (count == 0) {
        return JsonNull.INSTANCE;
      }

      double avg = sum / (double) count;

      if (avg == Math.rint(avg)) {
        return new JsonPrimitive((long) avg);
      }
      return new JsonPrimitive(avg);
    };
  }
}

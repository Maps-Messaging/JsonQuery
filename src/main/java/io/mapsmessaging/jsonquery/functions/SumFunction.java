package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class SumFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "sum";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (!rawArgs.isEmpty()) {
      throw new IllegalArgumentException("sum expects 0 arguments");
    }

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }

      if (!data.isJsonArray()) {
        return JsonNull.INSTANCE;
      }

      JsonArray array = data.getAsJsonArray();
      if (array.isEmpty()) {
        return new JsonPrimitive(0);
      }

      double sum = 0.0;
      for (JsonElement element : array) {
        if (element == null || element.isJsonNull()) {
          continue;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
          throw new IllegalArgumentException("Number expected");
        }
        sum += element.getAsDouble();
      }

      if (sum == Math.rint(sum)) {
        return new JsonPrimitive((long) sum);
      }
      return new JsonPrimitive(sum);
    };
  }
}

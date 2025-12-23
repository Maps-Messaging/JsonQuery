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
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (!rawArgs.isEmpty()) {
      throw new IllegalArgumentException("sum expects no arguments");
    }

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }

      if (!data.isJsonArray()) {
        throw new IllegalArgumentException("Array expected");
      }

      JsonArray array = data.getAsJsonArray();
      double sum = 0.0;

      for (JsonElement element : array) {
        if (element == null || element.isJsonNull()) {
          continue;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
          throw new IllegalArgumentException("Array expected");
        }
        sum += element.getAsDouble();
      }

      if (array.size() == 0) {
        return new JsonPrimitive(0);
      }

      if (sum == Math.rint(sum)) {
        return new JsonPrimitive((long) sum);
      }

      return new JsonPrimitive(sum);
    };
  }
}

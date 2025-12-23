package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

public final class GetFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "get";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.isEmpty()) {
      return data -> data == null ? JsonNull.INSTANCE : data;
    }

    for (JsonElement rawArg : rawArgs) {
      if (rawArg == null || rawArg.isJsonNull() || !rawArg.isJsonPrimitive()) {
        throw new IllegalArgumentException("get expects path segments of type string or number");
      }

      JsonPrimitive primitive = rawArg.getAsJsonPrimitive();
      if (!primitive.isString() && !primitive.isNumber()) {
        throw new IllegalArgumentException("get expects path segments of type string or number");
      }

      if (primitive.isNumber()) {
        BigDecimal bigDecimal = primitive.getAsBigDecimal();
        if (bigDecimal.scale() > 0) {
          throw new IllegalArgumentException("get expects an integer array index");
        }
        if (bigDecimal.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) < 0
            || bigDecimal.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
          throw new IllegalArgumentException("get array index out of int range");
        }
      }
    }

    return data -> {
      JsonElement current = data == null ? JsonNull.INSTANCE : data;

      for (JsonElement rawArg : rawArgs) {
        if (current == null || current.isJsonNull()) {
          return JsonNull.INSTANCE;
        }

        JsonPrimitive primitive = rawArg.getAsJsonPrimitive();

        if (primitive.isString()) {
          if (!current.isJsonObject()) {
            return JsonNull.INSTANCE;
          }

          JsonObject object = current.getAsJsonObject();
          JsonElement next = object.get(primitive.getAsString());
          current = next == null ? JsonNull.INSTANCE : next;
          continue;
        }

        int index = primitive.getAsInt();
        if (!current.isJsonArray()) {
          return JsonNull.INSTANCE;
        }

        JsonArray array = current.getAsJsonArray();
        if (index < 0 || index >= array.size()) {
          return JsonNull.INSTANCE;
        }

        JsonElement next = array.get(index);
        current = next == null ? JsonNull.INSTANCE : next;
      }

      return current == null ? JsonNull.INSTANCE : current;
    };
  }
}

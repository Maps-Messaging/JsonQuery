package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class GetFunction implements JsonQueryFunction {

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.isEmpty()) {
      return data -> data == null ? JsonNull.INSTANCE : data;
    }

    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("get expects 0 or 1 arguments");
    }

    JsonElement rawArg = rawArgs.get(0);
    if (rawArg == null || rawArg.isJsonNull()) {
      throw new IllegalArgumentException("get expects a key (string) or index (number)");
    }

    if (!rawArg.isJsonPrimitive()) {
      throw new IllegalArgumentException("get expects a key (string) or index (number)");
    }

    JsonPrimitive primitive = rawArg.getAsJsonPrimitive();

    if (primitive.isString()) {
      String key = primitive.getAsString();
      return data -> {
        if (data == null || data.isJsonNull()) {
          return JsonNull.INSTANCE;
        }
        if (!data.isJsonObject()) {
          return JsonNull.INSTANCE;
        }
        JsonElement value = data.getAsJsonObject().get(key);
        return value == null ? JsonNull.INSTANCE : value;
      };
    }

    if (primitive.isNumber()) {
      int index = primitive.getAsInt();
      return data -> {
        if (data == null || data.isJsonNull()) {
          return JsonNull.INSTANCE;
        }
        if (!data.isJsonArray()) {
          return JsonNull.INSTANCE;
        }

        JsonArray array = data.getAsJsonArray();
        if (index < 0 || index >= array.size()) {
          return JsonNull.INSTANCE;
        }

        JsonElement value = array.get(index);
        return value == null ? JsonNull.INSTANCE : value;
      };
    }

    throw new IllegalArgumentException("get expects a key (string) or index (number)");
  }
}

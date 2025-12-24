package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class KeyByFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "keyBy";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("keyBy expects 1 argument: keyBy(keySelector)");
    }

    Function<JsonElement, JsonElement> keySelector = compiler.compile(rawArgs.get(0));

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        return JsonNull.INSTANCE;
      }

      JsonObject keyed = new JsonObject();
      JsonArray input = data.getAsJsonArray();

      for (JsonElement element : input) {
        JsonElement keyValue = keySelector.apply(element);
        String key = toKey(keyValue);
        if (key == null) {
          continue;
        }
        if (keyed.has(key)) {
          continue; // first wins
        }
        keyed.add(key, element);
      }


      return keyed;
    };
  }

  private static String toKey(JsonElement keyValue) {
    if (keyValue == null || keyValue.isJsonNull()) {
      return null;
    }
    if (!keyValue.isJsonPrimitive()) {
      return null;
    }

    JsonPrimitive primitive = keyValue.getAsJsonPrimitive();
    if (primitive.isString()) {
      return primitive.getAsString();
    }
    if (primitive.isNumber()) {
      return primitive.getAsNumber().toString();
    }
    if (primitive.isBoolean()) {
      return Boolean.toString(primitive.getAsBoolean());
    }
    return null;
  }
}

package io.mapsmessaging.jsonquery.functions;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class GroupByFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "groupBy";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("groupBy expects 1 argument: groupBy(keySelector)");
    }

    Function<JsonElement, JsonElement> keySelector = compiler.compile(rawArgs.get(0));

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        return JsonNull.INSTANCE;
      }

      JsonObject grouped = new JsonObject();
      JsonArray input = data.getAsJsonArray();

      for (JsonElement element : input) {
        JsonElement keyValue = keySelector.apply(element);
        String key = toGroupKey(keyValue);
        if (key == null) {
          continue;
        }

        JsonArray bucket;
        JsonElement existing = grouped.get(key);
        if (existing == null || !existing.isJsonArray()) {
          bucket = new JsonArray();
          grouped.add(key, bucket);
        } else {
          bucket = existing.getAsJsonArray();
        }

        bucket.add(element);
      }

      return grouped;
    };
  }

  private static String toGroupKey(JsonElement keyValue) {
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

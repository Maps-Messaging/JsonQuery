package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class PickFunction implements JsonQueryFunction {
  @Override
  public String getName() {
    return "pick";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.isEmpty()) {
      throw new IllegalArgumentException("pick expects at least one selector");
    }

    List<JsonElement> selectorElements = normalizeSelectors(rawArgs);
    List<Function<JsonElement, JsonElement>> selectorFunctions = new ArrayList<>();
    for (JsonElement selectorElement : selectorElements) {
      Function<JsonElement, JsonElement> selectorFunction = compiler.compile(selectorElement);
      selectorFunctions.add(selectorFunction);
    }

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonObject()) {
        return JsonNull.INSTANCE;
      }

      JsonObject sourceObject = data.getAsJsonObject();
      JsonObject resultObject = new JsonObject();

      for (Function<JsonElement, JsonElement> selectorFunction : selectorFunctions) {
        JsonElement keyElement = selectorFunction.apply(data);
        if (keyElement == null || keyElement.isJsonNull()) {
          continue;
        }
        if (!keyElement.isJsonPrimitive()) {
          continue;
        }

        JsonPrimitive keyPrimitive = keyElement.getAsJsonPrimitive();
        if (!keyPrimitive.isString()) {
          continue;
        }

        String key = keyPrimitive.getAsString();
        JsonElement value = sourceObject.get(key);
        if (value == null) {
          continue;
        }

        resultObject.add(key, value);
      }

      return resultObject;
    };
  }

  private List<JsonElement> normalizeSelectors(List<JsonElement> rawArgs) {
    if (rawArgs.size() == 1 && rawArgs.get(0) != null && rawArgs.get(0).isJsonArray()) {
      JsonArray selectorArray = rawArgs.get(0).getAsJsonArray();
      List<JsonElement> selectorElements = new ArrayList<>();
      for (JsonElement selectorElement : selectorArray) {
        selectorElements.add(selectorElement);
      }
      return selectorElements;
    }

    List<JsonElement> selectorElements = new ArrayList<>();
    for (JsonElement selectorElement : rawArgs) {
      selectorElements.add(selectorElement);
    }
    return selectorElements;
  }
}

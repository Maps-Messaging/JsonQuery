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

    List<String> outputKeys = new ArrayList<>();
    List<Function<JsonElement, JsonElement>> valueSelectors = new ArrayList<>();
    List<Function<JsonElement, JsonElement>> dynamicKeySelectors = new ArrayList<>();

    for (JsonElement selectorElement : selectorElements) {
      String outputKey = tryExtractLiteralGetLeafKey(selectorElement);
      if (outputKey != null) {
        outputKeys.add(outputKey);
        valueSelectors.add(compiler.compile(selectorElement));
      } else {
        dynamicKeySelectors.add(compiler.compile(selectorElement));
      }
    }

    Function<JsonElement, JsonElement> pickOne = element -> {
      if (element == null || element.isJsonNull() || !element.isJsonObject()) {
        return JsonNull.INSTANCE;
      }

      JsonObject sourceObject = element.getAsJsonObject();
      JsonObject resultObject = new JsonObject();

      // literal get paths: key = leaf, value = selector result
      for (int index = 0; index < outputKeys.size(); index++) {
        String key = outputKeys.get(index);
        JsonElement value = valueSelectors.get(index).apply(element);
        if (value != null && !value.isJsonNull()) {
          resultObject.add(key, value);
        }
      }

      // dynamic keys: selector returns a string key to fetch from top-level
      for (Function<JsonElement, JsonElement> selectorFunction : dynamicKeySelectors) {
        JsonElement keyElement = selectorFunction.apply(element);
        if (keyElement == null || keyElement.isJsonNull() || !keyElement.isJsonPrimitive()) {
          continue;
        }
        JsonPrimitive primitive = keyElement.getAsJsonPrimitive();
        if (!primitive.isString()) {
          continue;
        }
        String key = primitive.getAsString();
        JsonElement value = sourceObject.get(key);
        if (value != null) {
          resultObject.add(key, value);
        }
      }

      return resultObject;
    };

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }

      if (data.isJsonArray()) {
        JsonArray in = data.getAsJsonArray();
        JsonArray out = new JsonArray();
        for (JsonElement element : in) {
          out.add(pickOne.apply(element));
        }
        return out;
      }

      return pickOne.apply(data);
    };
  }


  private static String tryExtractLiteralGetLeafKey(JsonElement selectorElement) {
    if (selectorElement == null || !selectorElement.isJsonArray()) {
      return null;
    }
    JsonArray array = selectorElement.getAsJsonArray();
    if (array.size() < 2) {
      return null;
    }

    JsonElement op = array.get(0);
    if (op == null || !op.isJsonPrimitive()) {
      return null;
    }
    JsonPrimitive opPrim = op.getAsJsonPrimitive();
    if (!opPrim.isString() || !"get".equals(opPrim.getAsString())) {
      return null;
    }

    // all args must be string literals
    for (int index = 1; index < array.size(); index++) {
      JsonElement arg = array.get(index);
      if (arg == null || !arg.isJsonPrimitive()) {
        return null;
      }
      JsonPrimitive argPrim = arg.getAsJsonPrimitive();
      if (!argPrim.isString()) {
        return null;
      }
    }

    // output key is the last path segment
    return array.get(array.size() - 1).getAsString();
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

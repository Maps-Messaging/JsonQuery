package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class PickFunction implements JsonQueryFunction {

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.isEmpty()) {
      throw new IllegalArgumentException("pick expects at least 1 argument: one or more field names");
    }

    List<String> fieldNames = new ArrayList<>();
    for (JsonElement arg : rawArgs) {
      fieldNames.add(JsonQueryGson.requireString(arg, "pick arguments must be strings"));
    }

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }

      if (data.isJsonArray()) {
        JsonArray input = data.getAsJsonArray();
        JsonArray output = new JsonArray();

        for (int i = 0; i < input.size(); i++) {
          JsonElement element = input.get(i);
          if (element != null && element.isJsonObject()) {
            output.add(pickFromObject(element.getAsJsonObject(), fieldNames));
          }
        }
        return output;
      }

      if (data.isJsonObject()) {
        return pickFromObject(data.getAsJsonObject(), fieldNames);
      }

      return data;
    };
  }

  private JsonObject pickFromObject(JsonObject input, List<String> fieldNames) {
    JsonObject output = new JsonObject();
    for (String fieldName : fieldNames) {
      JsonElement value = input.get(fieldName);
      if (value != null && !value.isJsonNull()) {
        output.add(fieldName, value);
      }
    }
    return output;
  }
}

package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class GetFunction implements JsonQueryFunction {

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("get expects 1 argument");
    }

    String key = JsonQueryGson.requireString(rawArgs.get(0), "get expects a string key");

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonObject()) {
        return JsonNull.INSTANCE;
      }

      JsonObject object = data.getAsJsonObject();
      JsonElement value = object.get(key);
      if (value == null) {
        return JsonNull.INSTANCE;
      }
      return value;
    };
  }
}

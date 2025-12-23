package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class ObjectFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "object";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("object expects 1 argument");
    }

    JsonElement arg = rawArgs.get(0);
    if (arg == null || arg.isJsonNull() || !arg.isJsonObject()) {
      throw new IllegalArgumentException("object expects a JSON object literal");
    }

    JsonObject template = arg.getAsJsonObject();
    JsonObject copy = deepCopyObject(template);

    return data -> deepCopyObject(copy);
  }

  private static JsonObject deepCopyObject(JsonObject object) {
    return JsonParser.parseString(object.toString()).getAsJsonObject();
  }
}

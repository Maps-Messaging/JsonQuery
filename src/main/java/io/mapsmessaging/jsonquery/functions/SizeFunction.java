package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class SizeFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "size";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs,
                                                    JsonQueryCompiler compiler) {

    if (!rawArgs.isEmpty()) {
      throw new IllegalArgumentException("size expects 0 arguments");
    }

    return data -> {
      if (data == null || data.isJsonNull()) {
        return new JsonPrimitive(0);
      }

      if (data.isJsonArray()) {
        JsonArray array = data.getAsJsonArray();
        return new JsonPrimitive(array.size());
      }

      if (data.isJsonObject()) {
        JsonObject object = data.getAsJsonObject();
        return new JsonPrimitive(object.size());
      }

      if (data.isJsonPrimitive()) {
        JsonPrimitive primitive = data.getAsJsonPrimitive();
        if (primitive.isString()) {
          return new JsonPrimitive(primitive.getAsString().length());
        }
      }

      return new JsonPrimitive(0);
    };
  }
}

package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public final class JsonQueryGson {

  private JsonQueryGson() {
  }

  public static boolean isString(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return false;
    }
    if (!element.isJsonPrimitive()) {
      return false;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    return primitive.isString();
  }

  public static String requireString(JsonElement element, String message) {
    if (!isString(element)) {
      throw new IllegalArgumentException(message);
    }
    return element.getAsString();
  }

  public static JsonElement nullToJsonNull(JsonElement element) {
    if (element == null) {
      return JsonNull.INSTANCE;
    }
    return element;
  }
}

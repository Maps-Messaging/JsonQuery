package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;

public final class JsonQueryTruthiness {

  private JsonQueryTruthiness() {
  }

  public static boolean isTruthy(JsonElement value) {
    if (value == null || value.isJsonNull()) {
      return false;
    }
    if (value.isJsonPrimitive()) {
      if (value.getAsJsonPrimitive().isBoolean()) {
        return value.getAsBoolean();
      }
      if (value.getAsJsonPrimitive().isNumber()) {
        return value.getAsDouble() != 0.0d;
      }
      if (value.getAsJsonPrimitive().isString()) {
        return !value.getAsString().isEmpty();
      }
    }
    if (value.isJsonArray()) {
      return value.getAsJsonArray().size() > 0;
    }
    if (value.isJsonObject()) {
      return value.getAsJsonObject().size() > 0;
    }
    return true;
  }
}

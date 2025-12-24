package io.mapsmessaging.jsonquery.functions;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public interface JsonQueryFunction {

  String getName();

  Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler
  );

  static Function<JsonElement, JsonElement> compileArg(JsonElement rawArg, JsonQueryCompiler compiler) {
    if (rawArg == null || rawArg.isJsonNull()) {
      return input -> JsonNull.INSTANCE;
    }
    if (rawArg.isJsonArray()) {
      return compiler.compile(rawArg.getAsJsonArray());
    }
    return input -> rawArg;
  }

  static JsonNull nullValue() {
    return JsonNull.INSTANCE;
  }

  static boolean isNull(JsonElement element) {
    return element == null || element.isJsonNull();
  }

  static boolean isTruthy(JsonElement element) {
    if (isNull(element)) {
      return false;
    }
    if (!element.isJsonPrimitive()) {
      return true;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    if (primitive.isBoolean()) {
      return primitive.getAsBoolean();
    }
    if (primitive.isNumber()) {
      return primitive.getAsDouble() != 0.0;
    }
    if (primitive.isString()) {
      return !primitive.getAsString().isEmpty();
    }
    return true;
  }
  
  static JsonElement booleanValue(boolean value) {
    return new JsonPrimitive(value);
  }

  static boolean isIntegral(double value) {
    return value == Math.rint(value);
  }

  static JsonElement numberValue(double value) {
    if (isIntegral(value)) {
      return new JsonPrimitive((long) value);
    }
    return new JsonPrimitive(value);
  }

  static String asString(JsonElement element, String errorMessage) {
    if (isNull(element)) {
      throw new IllegalArgumentException(errorMessage);
    }
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
      throw new IllegalArgumentException(errorMessage);
    }
    return element.getAsString();
  }

  static double asNumber(JsonElement element, String errorMessage) {
    if (isNull(element)) {
      throw new IllegalArgumentException(errorMessage);
    }
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
      throw new IllegalArgumentException(errorMessage);
    }
    return element.getAsDouble();
  }

  static int asInt(JsonElement element, String errorMessage) {
    double value = asNumber(element, errorMessage);
    return (int) value;
  }


}

package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class FilterFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "filter";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("filter expects 1 argument");
    }

    JsonElement predicateExpression = rawArgs.get(0);
    Function<JsonElement, JsonElement> predicate = compiler.compile(predicateExpression);

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }

      if (!data.isJsonArray()) {
        throw new IllegalArgumentException("Array expected");
      }

      JsonArray inputArray = data.getAsJsonArray();
      JsonArray outputArray = new JsonArray();

      for (int index = 0; index < inputArray.size(); index++) {
        JsonElement element = inputArray.get(index);
        JsonElement predicateResult = predicate.apply(element);

        if (isTruthy(predicateResult)) {
          outputArray.add(element);
        }
      }

      return outputArray;
    };
  }

  private static boolean isTruthy(JsonElement value) {
    if (value == null || value.isJsonNull()) {
      return false;
    }

    if (value.isJsonPrimitive()) {
      JsonPrimitive primitive = value.getAsJsonPrimitive();

      if (primitive.isBoolean()) {
        return primitive.getAsBoolean();
      }

      if (primitive.isNumber()) {
        return primitive.getAsDouble() != 0.0d;
      }

      return true;
    }

    return true;
  }
}

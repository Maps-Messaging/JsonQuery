package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class AbsFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "abs";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() > 1) {
      throw new IllegalArgumentException("abs expects 0 or 1 arguments");
    }

    Function<JsonElement, JsonElement> expression =
        rawArgs.isEmpty()
            ? data -> data == null ? JsonNull.INSTANCE : data
            : compiler.compile(rawArgs.get(0));

    return data -> {
      JsonElement value = expression.apply(data);
      if (value == null || value.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
        return JsonNull.INSTANCE;
      }

      double number = value.getAsDouble();
      double abs = Math.abs(number);

      // Preserve integer-ness when possible
      if (Math.floor(abs) == abs) {
        return new JsonPrimitive((long) abs);
      }

      return new JsonPrimitive(abs);
    };
  }
}

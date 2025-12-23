package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class InFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "in";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 2, "2 arguments: in(value, array)");
    Function<JsonElement, JsonElement> valueExpr = compileArg(rawArgs.get(0), compiler);
    Function<JsonElement, JsonElement> arrayExpr = compileArg(rawArgs.get(1), compiler);

    return data -> {
      JsonElement value = valueExpr.apply(data);
      JsonElement arrayValue = arrayExpr.apply(data);

      if (JsonQueryFunction.isNull(arrayValue)) {
        return new JsonPrimitive(false);
      }
      if (!arrayValue.isJsonArray()) {
        throw new IllegalArgumentException("Array expected");
      }

      JsonArray array = arrayValue.getAsJsonArray();
      for (JsonElement element : array) {
        if (element == null) {
          continue;
        }
        if (value == null) {
          if (element.isJsonNull()) {
            return new JsonPrimitive(true);
          }
          continue;
        }
        if (value.equals(element)) {
          return new JsonPrimitive(true);
        }
      }
      return new JsonPrimitive(false);
    };
  }
}

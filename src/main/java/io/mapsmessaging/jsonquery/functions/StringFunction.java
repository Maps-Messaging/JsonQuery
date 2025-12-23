package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class StringFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "string";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 1, "1 argument: string(value)");
    Function<JsonElement, JsonElement> arg = compileArg(rawArgs.get(0), compiler);

    return data -> {
      JsonElement value = arg.apply(data);
      if (JsonQueryFunction.isNull(value)) {
        return JsonNull.INSTANCE;
      }
      if (value.isJsonPrimitive()) {
        return new JsonPrimitive(value.getAsJsonPrimitive().getAsString());
      }
      return JsonNull.INSTANCE;
    };
  }
}

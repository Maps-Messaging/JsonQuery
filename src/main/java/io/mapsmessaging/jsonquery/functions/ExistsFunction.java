package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class ExistsFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "exists";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 1, "1 argument: exists(expr)");
    Function<JsonElement, JsonElement> arg = compileArg(rawArgs.get(0), compiler);

    return data -> {
      JsonElement value = arg.apply(data);
      return new JsonPrimitive(value != null && !value.isJsonNull());
    };
  }
}

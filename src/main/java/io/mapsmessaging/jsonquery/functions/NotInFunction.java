package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class NotInFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "not in";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    Function<JsonElement, JsonElement> inFunction = new InFunction().compile(rawArgs, compiler);
    return data -> {
      JsonElement inValue = inFunction.apply(data);
      return new JsonPrimitive(!JsonQueryFunction.isTruthy(inValue));
    };
  }
}

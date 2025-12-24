package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class IfFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "if";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 3, "3 arguments: if(condition, thenExpr, elseExpr)");
    Function<JsonElement, JsonElement> condition = compileArg(rawArgs.get(0), compiler);
    Function<JsonElement, JsonElement> thenExpr = compileArg(rawArgs.get(1), compiler);
    Function<JsonElement, JsonElement> elseExpr = compileArg(rawArgs.get(2), compiler);

    return data -> {
      JsonElement conditionValue = condition.apply(data);

      boolean cond;
      if (conditionValue == null || conditionValue.isJsonNull()) {
        cond = false;
      } else if (conditionValue.isJsonPrimitive()) {
        if (conditionValue.getAsJsonPrimitive().isBoolean()) {
          cond = conditionValue.getAsBoolean();
        } else if (conditionValue.getAsJsonPrimitive().isNumber()) {
          cond = conditionValue.getAsDouble() != 0.0;
        } else {
          // strings (including "") count as true
          cond = true;
        }
      } else {
        // arrays/objects count as true
        cond = true;
      }

      if (cond) {
        return thenExpr.apply(data);
      }
      return elseExpr.apply(data);
    };


  }
}

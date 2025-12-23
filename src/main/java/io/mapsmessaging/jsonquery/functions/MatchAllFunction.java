package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MatchAllFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "matchAll";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 2, "2 arguments: matchAll(text, pattern)");
    Function<JsonElement, JsonElement> textExpr = compileArg(rawArgs.get(0), compiler);
    Function<JsonElement, JsonElement> patternExpr = compileArg(rawArgs.get(1), compiler);

    return data -> {
      String text = JsonQueryFunction.asString(textExpr.apply(data), "String expected");
      String patternText = JsonQueryFunction.asString(patternExpr.apply(data), "String expected");
      Pattern pattern = Pattern.compile(patternText);
      Matcher matcher = pattern.matcher(text);

      JsonArray matches = new JsonArray();
      while (matcher.find()) {
        matches.add(new JsonPrimitive(matcher.group()));
      }
      return matches;
    };
  }
}

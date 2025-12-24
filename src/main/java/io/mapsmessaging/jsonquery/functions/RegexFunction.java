package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RegexFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "regex";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 2 && rawArgs.size() != 3) {
      throw new IllegalArgumentException("regex expects 2 or 3 arguments: regex(text, pattern, [flags])");
    }

    Function<JsonElement, JsonElement> textExpr = compileArg(rawArgs.get(0), compiler);
    Function<JsonElement, JsonElement> patternExpr = compileArg(rawArgs.get(1), compiler);
    Function<JsonElement, JsonElement> flagsExpr = (rawArgs.size() == 3) ? compileArg(rawArgs.get(2), compiler) : null;

    return data -> {
      String text = JsonQueryFunction.asString(textExpr.apply(data), "String expected");
      String patternText = JsonQueryFunction.asString(patternExpr.apply(data), "String expected");

      int flags = 0;
      if (flagsExpr != null) {
        String flagText = JsonQueryFunction.asString(flagsExpr.apply(data), "String expected");
        flags = parseFlags(flagText);
      }

      Pattern pattern = Pattern.compile(patternText, flags);
      Matcher matcher = pattern.matcher(text);
      return new JsonPrimitive(matcher.find());
    };
  }

  private static int parseFlags(String flagText) {
    int flags = 0;
    if (flagText == null) {
      return 0;
    }
    for (int i = 0; i < flagText.length(); i++) {
      char c = flagText.charAt(i);
      switch (c) {
        case 'i':
        case 'I':
          flags |= Pattern.CASE_INSENSITIVE;
          flags |= Pattern.UNICODE_CASE;
          break;
        case 'm':
        case 'M':
          flags |= Pattern.MULTILINE;
          break;
        case 's':
        case 'S':
          flags |= Pattern.DOTALL;
          break;
        case 'u':
        case 'U':
          flags |= Pattern.UNICODE_CASE;
          break;
        default:
          throw new IllegalArgumentException("Unsupported regex flag: " + c);
      }
    }
    return flags;
  }

}

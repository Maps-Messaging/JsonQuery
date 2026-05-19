/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RegexFunction extends AbstractFunction {

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

}

/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.jsonquery.functions.matcher;

import com.google.gson.*;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.AbstractFunction;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MatchFunction extends AbstractFunction {

  // Minimal named-group support: parse (?<name>...) occurrences.
  // This is enough for the conformance suite cases you showed.
  private static JsonObject extractNamedGroups(String patternText, Matcher matcher) {
    List<String> names = NamedGroupParser.parse(patternText);
    if (names.isEmpty()) {
      return null;
    }
    JsonObject obj = new JsonObject();
    for (String name : names) {
      String value;
      try {
        value = matcher.group(name);
      } catch (IllegalArgumentException e) {
        continue;
      }
      if (value == null) {
        obj.add(name, JsonNull.INSTANCE);
      } else {
        obj.add(name, new JsonPrimitive(value));
      }
    }
    return obj.size() == 0 ? null : obj;
  }

  @Override
  public String getName() {
    return "match";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 2 && rawArgs.size() != 3) {
      throw new IllegalArgumentException("match expects 2 or 3 arguments: match(text, pattern, [flags])");
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
        flags = RegexFunctionFlags.parseFlags(flagText);
      }

      Pattern pattern = Pattern.compile(patternText, flags);
      Matcher matcher = pattern.matcher(text);

      if (!matcher.find()) {
        return JsonNull.INSTANCE;
      }

      JsonObject result = new JsonObject();
      result.add("value", new JsonPrimitive(matcher.group(0)));

      int groupCount = matcher.groupCount();
      if (groupCount > 0) {
        JsonArray groups = new JsonArray();
        for (int i = 1; i <= groupCount; i++) {
          String groupValue = matcher.group(i);
          if (groupValue == null) {
            groups.add(JsonNull.INSTANCE);
          } else {
            groups.add(new JsonPrimitive(groupValue));
          }
        }
        result.add("groups", groups);
      }

      JsonObject namedGroups = extractNamedGroups(patternText, matcher);
      if (namedGroups != null) {
        result.add("namedGroups", namedGroups);
      }

      return result;
    };
  }
}

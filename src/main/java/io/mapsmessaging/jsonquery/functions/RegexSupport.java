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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.matcher.NamedGroupParser;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared compilation and result helpers for regex-based functions. */
public final class RegexSupport {

  public record MatchInput(Matcher matcher, List<String> namedGroupNames) {
  }

  private RegexSupport() {
  }

  public static Function<JsonElement, MatchInput> compile(
      String functionName,
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    if (rawArgs.size() != 2 && rawArgs.size() != 3) {
      throw new IllegalArgumentException(
          functionName + " expects 2 or 3 arguments: "
              + functionName + "(text, pattern, [flags])");
    }

    Function<JsonElement, JsonElement> textExpression =
        JsonQueryFunction.compileArg(rawArgs.get(0), compiler);
    Function<JsonElement, JsonElement> patternExpression =
        JsonQueryFunction.compileArg(rawArgs.get(1), compiler);
    Function<JsonElement, JsonElement> flagsExpression =
        rawArgs.size() == 3 ? JsonQueryFunction.compileArg(rawArgs.get(2), compiler) : null;

    return data -> evaluate(data, textExpression, patternExpression, flagsExpression);
  }

  public static JsonArray captureGroups(Matcher matcher) {
    int groupCount = matcher.groupCount();
    if (groupCount == 0) {
      return null;
    }

    JsonArray groups = new JsonArray();
    for (int index = 1; index <= groupCount; index++) {
      addNullableString(groups, matcher.group(index));
    }
    return groups;
  }

  public static JsonObject namedGroups(List<String> names, Matcher matcher) {
    if (names.isEmpty()) {
      return null;
    }

    JsonObject groups = new JsonObject();
    for (String name : names) {
      addNamedGroup(groups, name, matcher);
    }
    return groups.isEmpty() ? null : groups;
  }

  public static int parseFlags(String flagText) {
    if (flagText == null) {
      return 0;
    }

    int flags = 0;
    for (int index = 0; index < flagText.length(); index++) {
      flags |= flag(flagText.charAt(index));
    }
    return flags;
  }

  private static MatchInput evaluate(
      JsonElement data,
      Function<JsonElement, JsonElement> textExpression,
      Function<JsonElement, JsonElement> patternExpression,
      Function<JsonElement, JsonElement> flagsExpression) {

    String text = JsonQueryFunction.asString(textExpression.apply(data), "String expected");
    String patternText =
        JsonQueryFunction.asString(patternExpression.apply(data), "String expected");

    int flags = 0;
    if (flagsExpression != null) {
      String flagText =
          JsonQueryFunction.asString(flagsExpression.apply(data), "String expected");
      flags = parseFlags(flagText);
    }

    Matcher matcher = Pattern.compile(patternText, flags).matcher(text);
    return new MatchInput(matcher, NamedGroupParser.parse(patternText));
  }

  private static int flag(char value) {
    return switch (Character.toLowerCase(value)) {
      case 'i' -> Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
      case 'm' -> Pattern.MULTILINE;
      case 's' -> Pattern.DOTALL;
      case 'u' -> Pattern.UNICODE_CASE;
      default -> throw new IllegalArgumentException("Unsupported regex flag: " + value);
    };
  }

  private static void addNullableString(JsonArray array, String value) {
    if (value == null) {
      array.add(JsonNull.INSTANCE);
    } else {
      array.add(new JsonPrimitive(value));
    }
  }

  private static void addNamedGroup(JsonObject groups, String name, Matcher matcher) {
    try {
      String value = matcher.group(name);
      groups.add(name, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    } catch (IllegalArgumentException ignored) {
      // Pattern metadata can contain a name the Matcher does not expose.
    }
  }
}

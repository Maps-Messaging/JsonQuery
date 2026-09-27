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

package io.mapsmessaging.jsonquery.functions.matcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.AbstractFunction;
import io.mapsmessaging.jsonquery.functions.RegexSupport;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;

public final class MatchFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "match";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    Function<JsonElement, RegexSupport.MatchInput> regex =
        RegexSupport.compile(getName(), rawArgs, compiler);

    return data -> match(regex.apply(data));
  }

  private static JsonElement match(RegexSupport.MatchInput input) {
    Matcher matcher = input.matcher();
    if (!matcher.find()) {
      return JsonNull.INSTANCE;
    }

    JsonObject result = new JsonObject();
    result.add("value", new JsonPrimitive(matcher.group()));

    JsonArray groups = RegexSupport.captureGroups(matcher);
    if (groups != null) {
      result.add("groups", groups);
    }

    JsonObject namedGroups = RegexSupport.namedGroups(input.namedGroupNames(), matcher);
    if (namedGroups != null) {
      result.add("namedGroups", namedGroups);
    }
    return result;
  }
}

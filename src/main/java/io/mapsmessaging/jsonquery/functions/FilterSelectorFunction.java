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

package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;

import java.util.List;
import java.util.function.Function;

public final class FilterSelectorFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "selector";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("filter expects 1 argument: a JMS selector string");
    }

    String selector = JsonQueryGson.requireString(rawArgs.get(0), "filter selector must be a string");

    ParserExecutor executor;
    try {
      executor = SelectorParser.compile(selector);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Invalid selector: " + selector, e);
    }

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        if(executor.evaluate(data)){
          return data;
        }
        else{
          return new JsonNull();
        }
      }

      JsonArray inputArray = data.getAsJsonArray();
      JsonArray outputArray = new JsonArray();

      for (int i = 0; i < inputArray.size(); i++) {
        JsonElement element = inputArray.get(i);
        if (element != null && element.isJsonObject()) {
          JsonObject object = element.getAsJsonObject();
          if (executor.evaluate(object)) {
            outputArray.add(object);
          }
        }
      }
      return outputArray;
    };
  }
}

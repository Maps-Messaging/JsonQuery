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
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

final class LiteralGetPath {

  private LiteralGetPath() {
  }

  static List<String> strings(JsonElement expression) {
    if (expression == null || !expression.isJsonArray()) {
      return null;
    }

    JsonArray array = expression.getAsJsonArray();
    if (array.size() < 2 || !"get".equals(string(array.get(0)))) {
      return null;
    }

    List<String> path = new ArrayList<>(array.size() - 1);
    for (int index = 1; index < array.size(); index++) {
      String segment = string(array.get(index));
      if (segment == null) {
        return null;
      }
      path.add(segment);
    }
    return path;
  }

  private static String string(JsonElement element) {
    if (element == null || !element.isJsonPrimitive()) {
      return null;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    return primitive.isString() ? primitive.getAsString() : null;
  }
}

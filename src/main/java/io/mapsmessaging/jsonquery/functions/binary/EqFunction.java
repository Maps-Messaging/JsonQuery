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

package io.mapsmessaging.jsonquery.functions.binary;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.function.BiPredicate;

public final class EqFunction extends AbstractBinaryPredicateFunction {

  private static boolean deepEquals(JsonElement left, JsonElement right) {
    if (left == null || left.isJsonNull()) {
      return right == null || right.isJsonNull();
    }
    if (right == null || right.isJsonNull()) {
      return false;
    }

    // primitives
    if (left.isJsonPrimitive() && right.isJsonPrimitive()) {
      if ((isNumber(left) && isNumber(right))
          || (isString(left) && isString(right))
          || (isBoolean(left) && isBoolean(right))) {
        return compare(left, right) == 0;
      }
      return false;
    }

    // arrays: order matters
    if (left.isJsonArray() && right.isJsonArray()) {
      JsonArray la = left.getAsJsonArray();
      JsonArray ra = right.getAsJsonArray();
      if (la.size() != ra.size()) {
        return false;
      }
      for (int i = 0; i < la.size(); i++) {
        if (!deepEquals(la.get(i), ra.get(i))) {
          return false;
        }
      }
      return true;
    }

    // objects: key order does NOT matter
    if (left.isJsonObject() && right.isJsonObject()) {
      JsonObject lo = left.getAsJsonObject();
      JsonObject ro = right.getAsJsonObject();
      if (lo.size() != ro.size()) {
        return false;
      }
      for (Map.Entry<String, JsonElement> entry : lo.entrySet()) {
        String key = entry.getKey();
        if (!ro.has(key)) {
          return false;
        }
        if (!deepEquals(entry.getValue(), ro.get(key))) {
          return false;
        }
      }
      return true;
    }

    // different JSON types
    return false;
  }

  @Override
  public String getName() {
    return "eq";
  }

  @Override
  protected BiPredicate<JsonElement, JsonElement> predicate() {
    return EqFunction::deepEquals;
  }

  @Override
  protected String functionName() {
    return "eq";
  }
}

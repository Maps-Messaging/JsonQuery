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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NamedGroupParser {

  private NamedGroupParser() {
  }

  public static List<String> parse(String patternText) {
    Set<String> names = new HashSet<>();
    if (patternText == null || patternText.isEmpty()) {
      return List.of();
    }

    // crude but effective: find "(?<name>" not escaped
    for (int i = 0; i < patternText.length() - 3; i++) {
      char ch = patternText.charAt(i);
      if (ch == '\\') {
        i++;
        continue;
      }
      if (ch == '(' && i + 3 < patternText.length()
          && patternText.charAt(i + 1) == '?'
          && patternText.charAt(i + 2) == '<') {

        int start = i + 3;
        int end = start;
        while (end < patternText.length()) {
          char c = patternText.charAt(end);
          if (c == '>') {
            break;
          }
          end++;
        }
        if (end < patternText.length() && end > start) {
          String name = patternText.substring(start, end);
          names.add(name);
        }
        i = end;
      }
    }

    return new ArrayList<>(names);
  }
}

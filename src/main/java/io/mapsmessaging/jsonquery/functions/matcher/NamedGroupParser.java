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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class NamedGroupParser {

  private NamedGroupParser() {
  }

  public static List<String> parse(String patternText) {
    if (patternText == null || patternText.isEmpty()) {
      return List.of();
    }

    Set<String> names = new LinkedHashSet<>();
    int index = 0;
    while (index < patternText.length()) {
      if (patternText.charAt(index) == '\\') {
        index += 2;
      } else if (isNamedGroupStart(patternText, index)) {
        index = captureName(patternText, index, names);
      } else {
        index++;
      }
    }
    return new ArrayList<>(names);
  }

  private static boolean isNamedGroupStart(String pattern, int index) {
    return index + 3 < pattern.length()
        && pattern.charAt(index) == '('
        && pattern.charAt(index + 1) == '?'
        && pattern.charAt(index + 2) == '<';
  }

  private static int captureName(String pattern, int groupStart, Set<String> names) {
    int nameStart = groupStart + 3;
    int nameEnd = pattern.indexOf('>', nameStart);
    if (nameEnd < 0) {
      return pattern.length();
    }
    if (nameEnd > nameStart) {
      names.add(pattern.substring(nameStart, nameEnd));
    }
    return nameEnd + 1;
  }
}

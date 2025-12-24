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

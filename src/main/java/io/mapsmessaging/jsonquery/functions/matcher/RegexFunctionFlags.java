package io.mapsmessaging.jsonquery.functions.matcher;

import java.util.regex.Pattern;

public final class RegexFunctionFlags {

  private RegexFunctionFlags() {
  }

  public static int parseFlags(String flagText) {
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
}

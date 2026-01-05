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

package io.mapsmessaging.jsonquery.parser;

import java.util.HashMap;
import java.util.Map;

public final class JsonQueryTokenizer {

  private static final Map<String, Token.Type> KEYWORDS = new HashMap<>();

  static {
    KEYWORDS.put("and", Token.Type.AND);
    KEYWORDS.put("or", Token.Type.OR);
    KEYWORDS.put("in", Token.Type.IN);
    KEYWORDS.put("not", Token.Type.NOT);
    KEYWORDS.put("null", Token.Type.NULL);
    KEYWORDS.put("true", Token.Type.TRUE);
    KEYWORDS.put("false", Token.Type.FALSE);
  }

  private final String input;
  private final int length;
  private int index;

  public JsonQueryTokenizer(String input) {
    this.input = input;
    this.length = input.length();
    this.index = 0;
  }

  private static boolean isDigit(char ch) {
    return ch >= '0' && ch <= '9';
  }

  private static boolean isIdentStart(char ch) {
    return (ch >= 'A' && ch <= 'Z')
        || (ch >= 'a' && ch <= 'z')
        || ch == '_'
        || ch == '$';
  }

  private static boolean isIdentPart(char ch) {
    return isIdentStart(ch) || isDigit(ch);
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public int getIndex() {
    return index;
  }

  public void skipWhitespace() {
    while (index < length) {
      char ch = input.charAt(index);
      if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
        index++;
      } else {
        break;
      }
    }
  }

  public Token nextToken() throws JsonQueryParseException {
    skipWhitespace();
    if (index >= length) {
      return new Token(Token.Type.EOF, "", null, index);
    }

    int pos = index;
    char ch = input.charAt(index);

    switch (ch) {
      case '.':
        index++;
        return new Token(Token.Type.DOT, ".", null, pos);
      case ',':
        index++;
        return new Token(Token.Type.COMMA, ",", null, pos);
      case ':':
        index++;
        return new Token(Token.Type.COLON, ":", null, pos);
      case '(':
        index++;
        return new Token(Token.Type.LPAREN, "(", null, pos);
      case ')':
        index++;
        return new Token(Token.Type.RPAREN, ")", null, pos);
      case '[':
        index++;
        return new Token(Token.Type.LBRACKET, "[", null, pos);
      case ']':
        index++;
        return new Token(Token.Type.RBRACKET, "]", null, pos);
      case '{':
        index++;
        return new Token(Token.Type.LBRACE, "{", null, pos);
      case '}':
        index++;
        return new Token(Token.Type.RBRACE, "}", null, pos);
      case '|':
        index++;
        return new Token(Token.Type.PIPE, "|", null, pos);
      case '+':
        index++;
        return new Token(Token.Type.PLUS, "+", null, pos);
      case '-':
        index++;
        return new Token(Token.Type.MINUS, "-", null, pos);
      case '*':
        index++;
        return new Token(Token.Type.STAR, "*", null, pos);
      case '/':
        index++;
        return new Token(Token.Type.SLASH, "/", null, pos);
      case '%':
        index++;
        return new Token(Token.Type.PERCENT, "%", null, pos);
      case '^':
        index++;
        return new Token(Token.Type.CARET, "^", null, pos);
      case '"':
        return readString();
      case '=':
        return readEquals();
      case '!':
        return readNotEquals();
      case '<':
        return readLess();
      case '>':
        return readGreater();
      default:
        break;
    }

    if (isDigit(ch) || ch == '-') {
      return readNumber();
    }
    if (isIdentStart(ch)) {
      return readIdentifierOrKeyword();
    }

    // Unknown character: let parser report it as Unexpected part with a slice.
    index++;
    return new Token(Token.Type.IDENT, String.valueOf(ch), ch, pos);
  }

  private Token readEquals() {
    int pos = index;
    if (index + 1 < length && input.charAt(index + 1) == '=') {
      index += 2;
      return new Token(Token.Type.EQEQ, "==", null, pos);
    }
    // single '=' is not an operator in suite: parser will fail as Value expected.
    index++;
    return new Token(Token.Type.IDENT, "=", null, pos);
  }

  private Token readNotEquals() {
    int pos = index;
    if (index + 1 < length && input.charAt(index + 1) == '=') {
      index += 2;
      return new Token(Token.Type.NEQ, "!=", null, pos);
    }
    index++;
    return new Token(Token.Type.IDENT, "!", null, pos);
  }

  private Token readLess() {
    int pos = index;
    if (index + 1 < length && input.charAt(index + 1) == '=') {
      index += 2;
      return new Token(Token.Type.LTE, "<=", null, pos);
    }
    index++;
    return new Token(Token.Type.LT, "<", null, pos);
  }

  private Token readGreater() {
    int pos = index;
    if (index + 1 < length && input.charAt(index + 1) == '=') {
      index += 2;
      return new Token(Token.Type.GTE, ">=", null, pos);
    }
    index++;
    return new Token(Token.Type.GT, ">", null, pos);
  }

  private Token readIdentifierOrKeyword() {
    int pos = index;
    int start = index;
    index++;
    while (index < length) {
      char ch = input.charAt(index);
      if (isIdentPart(ch)) {
        index++;
      } else {
        break;
      }
    }
    String text = input.substring(start, index);
    Token.Type keyword = KEYWORDS.get(text);
    if (keyword != null) {
      return new Token(keyword, text, null, pos);
    }
    return new Token(Token.Type.IDENT, text, text, pos);
  }

  private Token readString() throws JsonQueryParseException {
    int pos = index;
    index++; // opening quote
    StringBuilder sb = new StringBuilder();
    while (index < length) {
      char ch = input.charAt(index);
      if (ch == '"') {
        index++;
        return new Token(Token.Type.STRING, sb.toString(), sb.toString(), pos);
      }
      if (ch == '\\') {
        if (index + 1 >= length) {
          break;
        }
        char esc = input.charAt(index + 1);
        switch (esc) {
          case 'n':
            sb.append('\n');
            break;
          case 'r':
            sb.append('\r');
            break;
          case 't':
            sb.append('\t');
            break;
          case '"':
            sb.append('"');
            break;
          case '\\':
            sb.append('\\');
            break;
          default:
            sb.append(esc);
            break;
        }
        index += 2;
      } else {
        sb.append(ch);
        index++;
      }
    }
    // Missing closing quote: suite expects Value expected (pos: 0) for "\"hello"
    throw JsonQueryParseException.valueExpected(pos);
  }

  private Token readNumber() throws JsonQueryParseException {
    int pos = index;
    int start = index;

    if (input.charAt(index) == '-') {
      index++;
      if (index >= length || !isDigit(input.charAt(index))) {
        throw JsonQueryParseException.valueExpected(pos);
      }
    }

    while (index < length && isDigit(input.charAt(index))) {
      index++;
    }

    boolean hasDot = false;
    if (index < length && input.charAt(index) == '.') {
      hasDot = true;
      index++;
      if (index >= length || !isDigit(input.charAt(index))) {
        // "2." in suite yields Property expected (pos: 2)
        throw JsonQueryParseException.propertyExpected(index);
      }
      while (index < length && isDigit(input.charAt(index))) {
        index++;
      }
    }

    boolean hasExp = false;
    if (index < length) {
      char ch = input.charAt(index);
      if (ch == 'e' || ch == 'E') {
        hasExp = true;
        int expStart = index;
        index++;
        if (index < length && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
          index++;
        }
        if (index >= length || !isDigit(input.charAt(index))) {
          String part = input.substring(expStart, Math.min(length, expStart + 2));
          if (index < length && (input.charAt(index - 1) == '+' || input.charAt(index - 1) == '-')) {
            part = input.substring(expStart, Math.min(length, expStart + 3));
          }
          // suite: "2.3e" -> Unexpected part 'e' (pos: 3)
          // "2.3e+" -> Unexpected part 'e+' (pos: 3)
          // "2.3e-" -> Unexpected part 'e-' (pos: 3)
          throw JsonQueryParseException.unexpectedPart(part, expStart);
        }
        while (index < length && isDigit(input.charAt(index))) {
          index++;
        }
      }
    }

    String text = input.substring(start, index);
    Double number = Double.valueOf(text);

    if (!hasDot && !hasExp) {
      long asLong = number.longValue();
      if (number.doubleValue() == (double) asLong) {
        return new Token(Token.Type.NUMBER, text, (int) asLong, pos);
      }
    }
    return new Token(Token.Type.NUMBER, text, number, pos);
  }

  public String sliceUnexpectedFrom(int pos) {
    int end = pos;
    while (end < length) {
      char ch = input.charAt(end);
      if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
        break;
      }
      if (ch == ')' || ch == ']' || ch == '}' || ch == ',' || ch == '|' || ch == '(' || ch == '[' || ch == '{') {
        break;
      }
      end++;
    }
    if (end == pos) {
      end = Math.min(length, pos + 1);
    }
    return input.substring(pos, end);
  }
}

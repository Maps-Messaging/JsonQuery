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

// File: JsonQueryParser.java
package io.mapsmessaging.jsonquery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.parser.JsonQueryParseException;

import java.util.ArrayList;
import java.util.List;

public final class JsonQueryParser {

  private final String input;
  private int index;

  public JsonQueryParser(String input) {
    this.input = input == null ? "" : input;
    this.index = 0;
  }

  public static JsonElement parse(String input) throws JsonQueryParseException {
    JsonQueryParser parser = new JsonQueryParser(input);
    JsonElement value = parser.parsePipe();
    parser.skipWhitespace();
    if (!parser.isEof()) {
      throw JsonQueryParseException.unexpectedPart(parser.remainingFrom(parser.index), parser.index);
    }
    return value;
  }

  // Lowest precedence: pipe "|"
  private JsonElement parsePipe() throws JsonQueryParseException {
    JsonElement left = parseOr();

    skipWhitespace();
    if (!peekChar('|')) {
      return left;
    }

    JsonElement pipe = ensurePipeNode(left);
    while (true) {
      skipWhitespace();
      if (!consumeChar('|')) {
        break;
      }
      skipWhitespace();
      if (isEof()) {
        throw JsonQueryParseException.valueExpected(index);
      }
      JsonElement right = parseOr();
      appendPipe(pipe, right);
      skipWhitespace();
    }
    return pipe;
  }

  // or (vararg)
  private JsonElement parseOr() throws JsonQueryParseException {
    JsonElement left = parseAnd();

    while (true) {
      skipWhitespace();
      if (!peekKeyword("or")) {
        break;
      }
      consumeKeyword("or");
      skipWhitespace();
      JsonElement right = parseAnd();
      left = mergeVarArg("or", left, right);
    }
    return left;
  }

  // and (vararg)
  private JsonElement parseAnd() throws JsonQueryParseException {
    JsonElement left = parseIn();

    while (true) {
      skipWhitespace();
      if (!peekKeyword("and")) {
        break;
      }
      consumeKeyword("and");
      skipWhitespace();
      JsonElement right = parseIn();
      left = mergeVarArg("and", left, right);
    }
    return left;
  }

  // in / not in
  private JsonElement parseIn() throws JsonQueryParseException {
    JsonElement left = parseEquality();
    skipWhitespace();

    if (peekKeyword("not")) {
      int save = index;
      consumeKeyword("not");
      if (requireWhitespaceAfterKeyword(save + 3)) {
        skipWhitespace();
        if (peekKeyword("in")) {
          consumeKeyword("in");
          skipWhitespace();
          JsonElement right = parseEquality();
          return makeCall("not in", left, right);
        }
      }
      index = save;
    }

    if (peekKeyword("in")) {
      consumeKeyword("in");
      skipWhitespace();
      JsonElement right = parseEquality();
      return makeCall("in", left, right);
    }
    return left;
  }

  // ==, != (non-chainable)
  private JsonElement parseEquality() throws JsonQueryParseException {
    JsonElement left = parseComparison();
    skipWhitespace();

    if (peekString("==") || peekString("!=")) {
      String op = consumeString(peekString("==") ? "==" : "!=");
      skipWhitespace();
      JsonElement right = parseComparison();
      JsonElement node = makeCall(op.equals("==") ? "eq" : "ne", left, right);

      skipWhitespace();
      if (peekString("==") || peekString("!=")) {
        String slice = sliceOperatorRhs();
        throw JsonQueryParseException.unexpectedPart(slice);
      }
      return node;
    }
    return left;
  }

  // < <= > >= (non-chainable)
  private JsonElement parseComparison() throws JsonQueryParseException {
    JsonElement left = parseAdditive();
    skipWhitespace();

    String op = null;
    if (peekString("<=")) {
      op = "<=";
    } else if (peekString(">=")) {
      op = ">=";
    } else if (peekChar('<')) {
      op = "<";
    } else if (peekChar('>')) {
      op = ">";
    }

    if (op == null) {
      return left;
    }

    consumeString(op);
    skipWhitespace();
    JsonElement right = parseAdditive();

    String name;
    if ("<".equals(op)) {
      name = "lt";
    } else if ("<=".equals(op)) {
      name = "lte";
    } else if (">".equals(op)) {
      name = "gt";
    } else {
      name = "gte";
    }

    JsonElement node = makeCall(name, left, right);

    skipWhitespace();
    if (peekString("<=") || peekString(">=") || peekChar('<') || peekChar('>')) {
      String slice = sliceOperatorRhs();
      throw JsonQueryParseException.unexpectedPart(slice);
    }
    return node;
  }

  // + -
  private JsonElement parseAdditive() throws JsonQueryParseException {
    JsonElement left = parseMultiplicative();
    while (true) {
      skipWhitespace();
      if (peekChar('+')) {
        consumeChar('+');
        skipWhitespace();
        JsonElement right = parseMultiplicative();
        left = makeCall("add", left, right);
        continue;
      }
      if (peekChar('-')) {
        consumeChar('-');
        skipWhitespace();
        JsonElement right = parseMultiplicative();
        left = makeCall("subtract", left, right);
        continue;
      }
      break;
    }
    return left;
  }

  // * / %
  private JsonElement parseMultiplicative() throws JsonQueryParseException {
    JsonElement left = parsePow();
    while (true) {
      skipWhitespace();
      if (peekChar('*')) {
        consumeChar('*');
        skipWhitespace();
        JsonElement right = parsePow();
        left = makeCall("multiply", left, right);
        continue;
      }
      if (peekChar('/')) {
        consumeChar('/');
        skipWhitespace();
        JsonElement right = parsePow();
        left = makeCall("divide", left, right);
        continue;
      }
      if (peekChar('%')) {
        consumeChar('%');
        skipWhitespace();
        JsonElement right = parsePow();
        left = makeCall("mod", left, right);
        continue;
      }
      break;
    }
    return left;
  }

  // ^ (non-chainable)
  private JsonElement parsePow() throws JsonQueryParseException {
    JsonElement left = parsePostfix();
    skipWhitespace();
    if (!peekChar('^')) {
      return left;
    }

    consumeChar('^');
    skipWhitespace();
    JsonElement right = parsePostfix();
    JsonElement node = makeCall("pow", left, right);

    skipWhitespace();
    if (peekChar('^')) {
      String slice = sliceOperatorRhs();
      throw JsonQueryParseException.unexpectedPart(slice);
    }
    return node;
  }

  // Postfix includes implicit property piping: expr .prop
  private JsonElement parsePostfix() throws JsonQueryParseException {
    JsonElement base = parsePrimary();

    while (true) {
      skipWhitespace();

      if (peekChar('.')) {
        int dotPos = index;
        consumeChar('.');
        Object property = parsePropertyAfterDot(dotPos);
        JsonElement getNode = makeGetNode(property);

        // implicit pipe: base .prop
        JsonElement pipe = ensurePipeNode(base);
        appendPipe(pipe, getNode);
        base = pipe;
        continue;
      }

      break;
    }

    return base;
  }

  private JsonElement parsePrimary() throws JsonQueryParseException {
    skipWhitespace();
    if (isEof()) {
      throw JsonQueryParseException.valueExpected(index);
    }

    if (peekChar('.')) {
      int dotPos = index;
      consumeChar('.');
      List<Object> segments = new ArrayList<>();
      segments.add(parsePropertyAfterDot(dotPos));

      while (true) {
        int save = index;
        skipWhitespace();
        if (!peekChar('.')) {
          index = save;
          break;
        }
        int nextDot = index;
        consumeChar('.');
        segments.add(parsePropertyAfterDot(nextDot));
      }

      return makeGetChain(segments);
    }

    if (peekChar('(')) {
      consumeChar('(');
      JsonElement inside = parsePipe();
      skipWhitespace();
      if (!consumeChar(')')) {
        throw JsonQueryParseException.characterExpected(')', index);
      }
      return inside;
    }

    if (peekChar('[')) {
      return parseArray();
    }

    if (peekChar('{')) {
      return parseObject();
    }

    if (peekChar('"')) {
      return new JsonPrimitive(parseStringValueOrThrowValueContext());
    }

    if (peekKeyword("true")) {
      consumeKeyword("true");
      return new JsonPrimitive(true);
    }

    if (peekKeyword("false")) {
      consumeKeyword("false");
      return new JsonPrimitive(false);
    }

    if (peekKeyword("null")) {
      consumeKeyword("null");
      return JsonNull.INSTANCE;
    }

    if (peekChar('-') || isDigit(peekChar())) {
      return parseNumberValue();
    }

    if (isIdentStart(peekChar())) {
      String name = parseIdentifier();
      skipWhitespace();
      if (peekChar('(')) {
        return parseFunctionCall(name);
      }
      // bare identifiers are not valid values in this language (conformance expects Value expected)
      throw JsonQueryParseException.valueExpected(index - name.length());
    }

    throw JsonQueryParseException.valueExpected(index);
  }

  private JsonElement parseFunctionCall(String name) throws JsonQueryParseException {
    consumeCharOrThrow('(');
    skipWhitespace();

    List<JsonElement> args = new ArrayList<>();

    // No-arg call: sort()
    if (consumeChar(')')) {
      JsonArray call = new JsonArray();
      call.add(name);
      return call;
    }

    // First arg
    args.add(parsePipe());

    while (true) {
      skipWhitespace();

      if (consumeChar(',')) {
        skipWhitespace();
        if (isEof()) {
          // sort(.age,
          throw JsonQueryParseException.valueExpected(index);
        }
        args.add(parsePipe());
        continue;
      }

      if (consumeChar(')')) {
        break;
      }

      if (isEof()) {
        // sort(.age, "desc"
        throw JsonQueryParseException.characterExpected(')', index);
      }

      // sort(.age "desc")
      throw JsonQueryParseException.characterExpected(',', index);
    }



    JsonArray call = new JsonArray();
    call.add(name);
    for (JsonElement arg : args) {
      call.add(arg);
    }
    return call;
  }


  private JsonElement parseArray() throws JsonQueryParseException {
    int startPos = index;
    consumeChar('[');
    skipWhitespace();

    JsonArray array = new JsonArray();
    array.add("array");

    if (consumeChar(']')) {
      return array;
    }

    while (true) {
      skipWhitespace();
      if (peekChar(']')) {
        throw JsonQueryParseException.valueExpected(index);
      }

      JsonElement value = parsePipe();
      array.add(value);

      skipWhitespace();
      if (consumeChar(',')) {
        skipWhitespace();
        if (peekChar(']')) {
          // trailing comma -> value expected at the ']'
          throw JsonQueryParseException.valueExpected(index);
        }
        continue;
      }

      if (consumeChar(']')) {
        break;
      }

      if (isEof()) {
        throw JsonQueryParseException.characterExpected(']', index);
      }

      // Missing comma
      throw JsonQueryParseException.characterExpected(',', index);
    }

    return array;
  }

  private JsonElement parseObject() throws JsonQueryParseException {
    consumeChar('{');
    skipWhitespace();

    JsonObject object = new JsonObject();

    if (consumeChar('}')) {
      JsonArray node = new JsonArray();
      node.add("object");
      node.add(object);
      return node;
    }

    while (true) {
      skipWhitespace();
      if (peekChar('}')) {
        throw JsonQueryParseException.keyExpected(index);
      }

      String key = parseObjectKey();

      skipWhitespace();
      if (!consumeChar(':')) {
        throw JsonQueryParseException.characterExpected(':', index);
      }

      skipWhitespace();
      if (peekChar('}') || peekChar(',')) {
        throw JsonQueryParseException.valueExpected(index);
      }

      JsonElement value = parsePipe();
      object.add(key, value);

      skipWhitespace();
      if (consumeChar(',')) {
        skipWhitespace();
        if (peekChar('}')) {
          throw JsonQueryParseException.keyExpected(index);
        }
        continue;
      }

      if (consumeChar('}')) {
        break;
      }

      if (isEof()) {
        throw JsonQueryParseException.characterExpected('}', index);
      }

      throw JsonQueryParseException.characterExpected(',', index);
    }

    JsonArray node = new JsonArray();
    node.add("object");
    node.add(object);
    return node;
  }

  private String parseObjectKey() throws JsonQueryParseException {
    if (peekChar('"')) {
      return parseStringValueOrThrowValueContext();
    }

    if (peekKeyword("null")) {
      consumeKeyword("null");
      return "null";
    }

    if (isDigit(peekChar())) {
      // numeric keys allowed, become strings (e.g. {2:"two"} -> key "2")
      int start = index;
      while (!isEof() && isDigit(peekChar())) {
        index++;
      }
      return input.substring(start, index);
    }

    if (isIdentStart(peekChar())) {
      return parseIdentifier();
    }

    throw JsonQueryParseException.keyExpected(index);
  }

  private Object parsePropertyAfterDot(int dotPos) throws JsonQueryParseException {
    int pos = dotPos + 1;
    if (pos >= input.length()) {
      throw JsonQueryParseException.propertyExpected(pos);
    }

    char ch = input.charAt(pos);
    if (Character.isWhitespace(ch)) {
      throw JsonQueryParseException.propertyExpected(pos);
    }

    // quoted property
    if (ch == '"') {
      index = pos;
      try {
        return parseStringValueOrThrowPropertyContext(dotPos + 1);
      } finally {
        // parseStringValue* advances index already
      }
    }

    // numeric property
    if (ch >= '0' && ch <= '9') {
      if (ch == '0') {
        if (pos + 1 < input.length()) {
          char next = input.charAt(pos + 1);
          if (next >= '0' && next <= '9') {
            throw JsonQueryParseException.unexpectedPart(String.valueOf(next));
          }
        }
        index = pos + 1;
        return 0;
      }

      int value = 0;
      int i = pos;
      while (i < input.length()) {
        char d = input.charAt(i);
        if (d < '0' || d > '9') {
          break;
        }
        value = (value * 10) + (d - '0');
        i++;
      }

      // ".1abc" -> Unexpected part 'abc'
      if (i < input.length() && isIdentStart(input.charAt(i))) {
        int j = i + 1;
        while (j < input.length() && isIdentPart(input.charAt(j))) {
          j++;
        }
        throw JsonQueryParseException.unexpectedPart(input.substring(i, j));
      }

      index = i;
      return value;
    }

// identifier property
    if (isIdentStart(ch)) {
      int i = pos + 1;
      while (i < input.length() && isIdentPart(input.charAt(i))) {
        i++;
      }

      // After an unquoted property, "#" is illegal and must report "Unexpected part" WITHOUT pos
      if (i < input.length() && input.charAt(i) == '#') {
        int j = i + 1;
        while (j < input.length() && isIdentPart(input.charAt(j))) {
          j++;
        }
        throw JsonQueryParseException.unexpectedPart(input.substring(i, j)); // <-- no pos
      }

      String name = input.substring(pos, i);
      index = i;
      return name;
    }

    throw JsonQueryParseException.propertyExpected(pos);
  }

  private JsonElement makeGetNode(Object property) {
    JsonArray get = new JsonArray();
    get.add("get");
    if (property instanceof String) {
      get.add((String) property);
    } else if (property instanceof Integer) {
      get.add((Integer) property);
    } else {
      get.add(String.valueOf(property));
    }
    return get;
  }

  private JsonElement makeGetChain(List<Object> segments) {
    JsonArray get = new JsonArray();
    get.add("get");
    for (Object seg : segments) {
      if (seg instanceof String) {
        get.add((String) seg);
      } else if (seg instanceof Integer) {
        get.add((Integer) seg);
      } else {
        get.add(String.valueOf(seg));
      }
    }
    return get;
  }

  private JsonElement makeCall(String name, JsonElement left, JsonElement right) {
    JsonArray call = new JsonArray();
    call.add(name);
    call.add(left);
    call.add(right);
    return call;
  }

  private JsonElement mergeVarArg(String name, JsonElement left, JsonElement right) {
    if (isCallNamed(left, name)) {
      JsonArray arr = left.getAsJsonArray();
      arr.add(right);
      return left;
    }
    JsonArray call = new JsonArray();
    call.add(name);
    call.add(left);
    call.add(right);
    return call;
  }

  private boolean isCallNamed(JsonElement el, String name) {
    if (el == null || !el.isJsonArray()) {
      return false;
    }
    JsonArray arr = el.getAsJsonArray();
    if (arr.size() < 1) {
      return false;
    }
    JsonElement head = arr.get(0);
    return head.isJsonPrimitive() && head.getAsJsonPrimitive().isString() && name.equals(head.getAsString());
  }

  private JsonElement ensurePipeNode(JsonElement base) {
    if (isCallNamed(base, "pipe")) {
      return base;
    }
    JsonArray pipe = new JsonArray();
    pipe.add("pipe");
    pipe.add(base);
    return pipe;
  }

  private void appendPipe(JsonElement pipeNode, JsonElement next) {
    JsonArray pipe = pipeNode.getAsJsonArray();

    // If next is itself a pipe, flatten it (drop the "pipe" head)
    if (isCallNamed(next, "pipe")) {
      JsonArray nextPipe = next.getAsJsonArray();
      for (int i = 1; i < nextPipe.size(); i++) {
        appendPipe(pipeNode, nextPipe.get(i));
      }
      return;
    }

    // If last stage is get(...) and next is get(...), merge into one get chain
    if (pipe.size() >= 2) {
      JsonElement last = pipe.get(pipe.size() - 1);
      if (isCallNamed(last, "get") && isCallNamed(next, "get")) {
        JsonArray lastGet = last.getAsJsonArray();
        JsonArray nextGet = next.getAsJsonArray();
        for (int i = 1; i < nextGet.size(); i++) {
          lastGet.add(nextGet.get(i));
        }
        return;
      }
    }

    pipe.add(next);
  }

  private String sliceOperatorRhs() {
    int start = index;
    skipWhitespace();
    start = index;
    int i = index;
    while (i < input.length() && !Character.isWhitespace(input.charAt(i))) {
      i++;
    }
    int opEnd = i;

    int j = i;
    while (j < input.length() && Character.isWhitespace(input.charAt(j))) {
      j++;
    }
    int rhsStart = j;

    int k = rhsStart;
    if (k >= input.length()) {
      return input.substring(start);
    }

    char ch = input.charAt(k);
    if (ch == '"') {
      k++;
      while (k < input.length()) {
        char c = input.charAt(k);
        if (c == '\\' && k + 1 < input.length()) {
          k += 2;
          continue;
        }
        if (c == '"') {
          k++;
          break;
        }
        k++;
      }
    } else if (isIdentStart(ch)) {
      k++;
      while (k < input.length() && isIdentPart(input.charAt(k))) {
        k++;
      }
    } else if (isDigit(ch) || ch == '-') {
      k++;
      while (k < input.length()) {
        char d = input.charAt(k);
        if (!(isDigit(d) || d == '.' || d == 'e' || d == 'E' || d == '+' || d == '-')) {
          break;
        }
        k++;
      }
    } else {
      k++;
    }

    String op = input.substring(start, opEnd);
    String rhs = input.substring(rhsStart, k);
    return op + " " + rhs;
  }

  private String remainingFrom(int pos) {
    return input.substring(pos);
  }

  private String parseIdentifier() {
    int start = index;
    index++;
    while (!isEof() && isIdentPart(peekChar())) {
      index++;
    }
    return input.substring(start, index);
  }

  private JsonElement parseNumberValue() throws JsonQueryParseException {
    int start = index;

    if (consumeChar('-')) {
      if (isEof() || !isDigit(peekChar())) {
        throw JsonQueryParseException.valueExpected(start);
      }
    }

    boolean sawDigit = false;
    while (!isEof() && isDigit(peekChar())) {
      sawDigit = true;
      index++;
    }

    if (peekChar('.')) {
      int dotPos = index;
      index++;
      if (isEof() || !isDigit(peekChar())) {
        // suite expects "2." -> Property expected (pos: 2)
        throw JsonQueryParseException.propertyExpected(dotPos + 1);
      }
      while (!isEof() && isDigit(peekChar())) {
        index++;
      }
    }

    if (!isEof() && (peekChar('e') || peekChar('E'))) {
      int ePos = index;
      index++;
      if (!isEof() && (peekChar('+') || peekChar('-'))) {
        index++;
      }
      if (isEof() || !isDigit(peekChar())) {
        // 2.3e / 2.3e+ / 2.3e- : "Unexpected part 'e' (pos: 3)" etc.
        String part = input.substring(ePos, Math.min(input.length(), ePos + (index - ePos)));
        throw JsonQueryParseException.unexpectedPart(part, ePos);
      }
      while (!isEof() && isDigit(peekChar())) {
        index++;
      }
    }

    if (!sawDigit) {
      throw JsonQueryParseException.valueExpected(start);
    }

    String raw = input.substring(start, index);
    double value = Double.parseDouble(raw);
    if (value == Math.rint(value)) {
      long asLong = (long) value;
      if (asLong >= Integer.MIN_VALUE && asLong <= Integer.MAX_VALUE) {
        return new JsonPrimitive((int) asLong);
      }
    }
    return new JsonPrimitive(value);
  }

  private String parseStringValueOrThrowValueContext() throws JsonQueryParseException {
    return parseStringValueOrThrow(JsonQueryParseException.valueExpected(index));
  }

  private String parseStringValueOrThrowPropertyContext(int pos) throws JsonQueryParseException {
    return parseStringValueOrThrow(JsonQueryParseException.propertyExpected(pos));
  }

  private String parseStringValueOrThrow(JsonQueryParseException onError) throws JsonQueryParseException {
    if (!consumeChar('"')) {
      throw onError;
    }

    StringBuilder sb = new StringBuilder();
    while (!isEof()) {
      char c = input.charAt(index);
      if (c == '"') {
        index++;
        return sb.toString();
      }
      if (c == '\\') {
        if (index + 1 >= input.length()) {
          throw onError;
        }
        char esc = input.charAt(index + 1);
        switch (esc) {
          case 'n': sb.append('\n'); break;
          case 'r': sb.append('\r'); break;
          case 't': sb.append('\t'); break;
          case '"': sb.append('"'); break;
          case '\\': sb.append('\\'); break;
          default: sb.append(esc); break;
        }
        index += 2;
        continue;
      }
      sb.append(c);
      index++;
    }

    throw onError;
  }

  private void skipWhitespace() {
    while (!isEof()) {
      char c = input.charAt(index);
      if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
        index++;
        continue;
      }
      break;
    }
  }

  private boolean peekKeyword(String keyword) {
    int len = keyword.length();
    if (index + len > input.length()) {
      return false;
    }
    if (!input.regionMatches(index, keyword, 0, len)) {
      return false;
    }
    int end = index + len;
    if (end < input.length() && isIdentPart(input.charAt(end))) {
      return false;
    }
    if (index > 0 && isIdentPart(input.charAt(index - 1))) {
      return false;
    }
    return true;
  }

  private void consumeKeyword(String keyword) {
    index += keyword.length();
  }

  private boolean requireWhitespaceAfterKeyword(int posAfterKeyword) {
    if (posAfterKeyword >= input.length()) {
      return true;
    }
    return Character.isWhitespace(input.charAt(posAfterKeyword));
  }

  private boolean peekString(String s) {
    if (index + s.length() > input.length()) {
      return false;
    }
    return input.regionMatches(index, s, 0, s.length());
  }

  private String consumeString(String s) {
    index += s.length();
    return s;
  }

  private boolean consumeChar(char ch) {
    if (peekChar(ch)) {
      index++;
      return true;
    }
    return false;
  }

  private void consumeCharOrThrow(char ch) throws JsonQueryParseException {
    if (!consumeChar(ch)) {
      throw JsonQueryParseException.characterExpected(ch, index);
    }
  }

  private boolean peekChar(char ch) {
    if (index >= input.length()) {
      return false;
    }
    return input.charAt(index) == ch;
  }

  private char peekChar() {
    return input.charAt(index);
  }

  private boolean isEof() {
    return index >= input.length();
  }

  private static boolean isIdentStart(char ch) {
    return (ch >= 'A' && ch <= 'Z')
        || (ch >= 'a' && ch <= 'z')
        || ch == '_' || ch == '$';
  }

  private static boolean isIdentPart(char ch) {
    return isIdentStart(ch) || isDigit(ch);
  }

  private static boolean isDigit(char ch) {
    return ch >= '0' && ch <= '9';
  }

  public static void main(String[] args){
    System.out.println(JsonQueryParser.parse("pick(\"timestamp\", \"particles_gt_10\") | wrap(\"payload\")"));
  }
}

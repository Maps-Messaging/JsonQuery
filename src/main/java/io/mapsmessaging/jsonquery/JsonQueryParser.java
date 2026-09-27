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
import java.util.function.IntFunction;
import java.util.function.Supplier;

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

    String operator = comparisonOperator();
    if (operator == null) {
      return left;
    }

    consumeString(operator);
    skipWhitespace();
    JsonElement result = makeCall(comparisonName(operator), left, parseAdditive());

    skipWhitespace();
    if (comparisonOperator() != null) {
      throw JsonQueryParseException.unexpectedPart(sliceOperatorRhs());
    }
    return result;
  }

  // + -
  private JsonElement parseAdditive() throws JsonQueryParseException {
    return parseBinaryChain(
        this::parseMultiplicative,
        operator -> switch (operator) {
          case '+' -> "add";
          case '-' -> "subtract";
          default -> null;
        });
  }

  // * / %
  private JsonElement parseMultiplicative() throws JsonQueryParseException {
    return parseBinaryChain(
        this::parsePow,
        operator -> switch (operator) {
          case '*' -> "multiply";
          case '/' -> "divide";
          case '%' -> "mod";
          default -> null;
        });
  }

  private JsonElement parseBinaryChain(
      Supplier<JsonElement> operandParser,
      IntFunction<String> operatorName) {

    JsonElement left = operandParser.get();
    while (true) {
      skipWhitespace();
      int operator = isEof() ? -1 : peekChar();
      String name = operatorName.apply(operator);
      if (name == null) {
        return left;
      }

      index++;
      skipWhitespace();
      left = makeCall(name, left, operandParser.get());
    }
  }

  private String comparisonOperator() {
    if (peekString("<=")) {
      return "<=";
    }
    if (peekString(">=")) {
      return ">=";
    }
    if (peekChar('<')) {
      return "<";
    }
    return peekChar('>') ? ">" : null;
  }

  private static String comparisonName(String operator) {
    return switch (operator) {
      case "<" -> "lt";
      case "<=" -> "lte";
      case ">" -> "gt";
      case ">=" -> "gte";
      default -> throw new IllegalArgumentException("Unknown comparison operator " + operator);
    };
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

    char current = peekChar();
    if (current == '.') {
      return parsePropertyChain();
    }
    if (current == '(') {
      return parseParenthesized();
    }
    if (current == '[') {
      return parseArray();
    }
    if (current == '{') {
      return parseObject();
    }
    if (current == '"') {
      return new JsonPrimitive(parseStringValueOrThrowValueContext());
    }
    if (current == '-' || isDigit(current)) {
      return parseNumberValue();
    }

    JsonElement keyword = parseKeywordLiteral();
    if (keyword != null) {
      return keyword;
    }
    if (isIdentStart(current)) {
      return parseIdentifierPrimary();
    }

    throw JsonQueryParseException.valueExpected(index);
  }

  private JsonElement parsePropertyChain() throws JsonQueryParseException {
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

  private JsonElement parseParenthesized() throws JsonQueryParseException {
    consumeChar('(');
    JsonElement inside = parsePipe();
    skipWhitespace();
    if (!consumeChar(')')) {
      throw JsonQueryParseException.characterExpected(')', index);
    }
    return inside;
  }

  private JsonElement parseKeywordLiteral() {
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
    return null;
  }

  private JsonElement parseIdentifierPrimary() throws JsonQueryParseException {
    String name = parseIdentifier();
    skipWhitespace();
    if (peekChar('(')) {
      return parseFunctionCall(name);
    }
    throw JsonQueryParseException.valueExpected(index - name.length());
  }

  private JsonElement parseFunctionCall(String name) throws JsonQueryParseException {
    consumeCharOrThrow('(');
    skipWhitespace();

    if (consumeChar(')')) {
      return makeFunctionCall(name, List.of());
    }

    List<JsonElement> arguments = new ArrayList<>();
    arguments.add(parsePipe());
    parseRemainingArguments(arguments);
    return makeFunctionCall(name, arguments);
  }

  private void parseRemainingArguments(List<JsonElement> arguments) {
    while (true) {
      skipWhitespace();
      if (consumeChar(')')) {
        return;
      }
      if (!consumeChar(',')) {
        if (isEof()) {
          throw JsonQueryParseException.characterExpected(')', index);
        }
        throw JsonQueryParseException.characterExpected(',', index);
      }

      skipWhitespace();
      if (isEof()) {
        throw JsonQueryParseException.valueExpected(index);
      }
      arguments.add(parsePipe());
    }
  }

  private static JsonArray makeFunctionCall(String name, List<JsonElement> arguments) {
    JsonArray call = new JsonArray();
    call.add(name);
    arguments.forEach(call::add);
    return call;
  }


  private JsonElement parseArray() throws JsonQueryParseException {
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
    int position = dotPos + 1;
    if (position >= input.length() || Character.isWhitespace(input.charAt(position))) {
      throw JsonQueryParseException.propertyExpected(position);
    }

    char current = input.charAt(position);
    if (current == '"') {
      index = position;
      return parseStringValueOrThrowPropertyContext(position);
    }
    if (isDigit(current)) {
      return parseNumericProperty(position);
    }
    if (isIdentStart(current)) {
      return parseNamedProperty(position);
    }
    throw JsonQueryParseException.propertyExpected(position);
  }

  private int parseNumericProperty(int position) throws JsonQueryParseException {
    if (input.charAt(position) == '0') {
      validateNoLeadingZeroProperty(position);
      index = position + 1;
      return 0;
    }

    int value = 0;
    int cursor = position;
    while (cursor < input.length() && isDigit(input.charAt(cursor))) {
      value = (value * 10) + (input.charAt(cursor) - '0');
      cursor++;
    }
    rejectIdentifierSuffix(cursor);
    index = cursor;
    return value;
  }

  private void validateNoLeadingZeroProperty(int position) throws JsonQueryParseException {
    if (position + 1 < input.length() && isDigit(input.charAt(position + 1))) {
      throw JsonQueryParseException.unexpectedPart(
          String.valueOf(input.charAt(position + 1)));
    }
  }

  private String parseNamedProperty(int position) throws JsonQueryParseException {
    int cursor = position + 1;
    while (cursor < input.length() && isIdentPart(input.charAt(cursor))) {
      cursor++;
    }

    if (cursor < input.length() && input.charAt(cursor) == '#') {
      int suffixEnd = cursor + 1;
      while (suffixEnd < input.length() && isIdentPart(input.charAt(suffixEnd))) {
        suffixEnd++;
      }
      throw JsonQueryParseException.unexpectedPart(input.substring(cursor, suffixEnd));
    }

    String name = input.substring(position, cursor);
    index = cursor;
    return name;
  }

  private void rejectIdentifierSuffix(int position) throws JsonQueryParseException {
    if (position >= input.length() || !isIdentStart(input.charAt(position))) {
      return;
    }
    int suffixEnd = position + 1;
    while (suffixEnd < input.length() && isIdentPart(input.charAt(suffixEnd))) {
      suffixEnd++;
    }
    throw JsonQueryParseException.unexpectedPart(input.substring(position, suffixEnd));
  }

  private JsonElement makeGetNode(Object property) {
    JsonArray get = new JsonArray();
    get.add("get");
    addProperty(get, property);
    return get;
  }

  private JsonElement makeGetChain(List<Object> segments) {
    JsonArray get = new JsonArray();
    get.add("get");
    segments.forEach(segment -> addProperty(get, segment));
    return get;
  }

  private static void addProperty(JsonArray get, Object property) {
    if (property instanceof String value) {
      get.add(value);
    } else if (property instanceof Integer value) {
      get.add(value);
    } else {
      get.add(String.valueOf(property));
    }
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
    if (arr.isEmpty()) {
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
    skipWhitespace();
    int operatorStart = index;
    int operatorEnd = scanUntilWhitespace(operatorStart);
    int rhsStart = skipWhitespace(operatorEnd);

    if (rhsStart >= input.length()) {
      return input.substring(operatorStart);
    }

    int rhsEnd = scanValue(rhsStart);
    return input.substring(operatorStart, operatorEnd)
        + " "
        + input.substring(rhsStart, rhsEnd);
  }

  private int scanValue(int start) {
    char first = input.charAt(start);
    if (first == '"') {
      return scanQuotedValue(start);
    }
    if (isIdentStart(first)) {
      return scanIdentifier(start);
    }
    if (isDigit(first) || first == '-') {
      return scanNumber(start);
    }
    return start + 1;
  }

  private int scanQuotedValue(int start) {
    int cursor = start + 1;
    while (cursor < input.length()) {
      char current = input.charAt(cursor);
      if (current == '\\' && cursor + 1 < input.length()) {
        cursor += 2;
      } else if (current == '"') {
        return cursor + 1;
      } else {
        cursor++;
      }
    }
    return cursor;
  }

  private int scanIdentifier(int start) {
    int cursor = start + 1;
    while (cursor < input.length() && isIdentPart(input.charAt(cursor))) {
      cursor++;
    }
    return cursor;
  }

  private int scanNumber(int start) {
    int cursor = start + 1;
    while (cursor < input.length() && isNumberCharacter(input.charAt(cursor))) {
      cursor++;
    }
    return cursor;
  }

  private int scanUntilWhitespace(int start) {
    int cursor = start;
    while (cursor < input.length() && !Character.isWhitespace(input.charAt(cursor))) {
      cursor++;
    }
    return cursor;
  }

  private int skipWhitespace(int start) {
    int cursor = start;
    while (cursor < input.length() && Character.isWhitespace(input.charAt(cursor))) {
      cursor++;
    }
    return cursor;
  }

  private static boolean isNumberCharacter(char value) {
    return isDigit(value)
        || value == '.'
        || value == 'e'
        || value == 'E'
        || value == '+'
        || value == '-';
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
    parseNumberSign(start);
    parseDigits(start);
    parseFraction();
    parseExponent();

    double value = Double.parseDouble(input.substring(start, index));
    return numberPrimitive(value);
  }

  private void parseNumberSign(int start) {
    if (consumeChar('-') && (isEof() || !isDigit(peekChar()))) {
      throw JsonQueryParseException.valueExpected(start);
    }
  }

  private void parseDigits(int start) {
    int digitsStart = index;
    while (!isEof() && isDigit(peekChar())) {
      index++;
    }
    if (index == digitsStart) {
      throw JsonQueryParseException.valueExpected(start);
    }
  }

  private void parseFraction() {
    if (!peekChar('.')) {
      return;
    }

    int dotPosition = index++;
    if (isEof() || !isDigit(peekChar())) {
      throw JsonQueryParseException.propertyExpected(dotPosition + 1);
    }
    while (!isEof() && isDigit(peekChar())) {
      index++;
    }
  }

  private void parseExponent() {
    if (isEof() || (!peekChar('e') && !peekChar('E'))) {
      return;
    }

    int exponentPosition = index++;
    if (!isEof() && (peekChar('+') || peekChar('-'))) {
      index++;
    }
    if (isEof() || !isDigit(peekChar())) {
      throw JsonQueryParseException.unexpectedPart(
          input.substring(exponentPosition, index),
          exponentPosition);
    }
    while (!isEof() && isDigit(peekChar())) {
      index++;
    }
  }

  private static JsonPrimitive numberPrimitive(double value) {
    if (value == Math.rint(value)) {
      long integral = (long) value;
      if (integral >= Integer.MIN_VALUE && integral <= Integer.MAX_VALUE) {
        return new JsonPrimitive((int) integral);
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

    StringBuilder value = new StringBuilder();
    while (!isEof()) {
      char current = input.charAt(index++);
      if (current == '"') {
        return value.toString();
      }
      if (current == '\\') {
        if (isEof()) {
          throw onError;
        }
        value.append(unescape(input.charAt(index++)));
      } else {
        value.append(current);
      }
    }
    throw onError;
  }

  private static char unescape(char escaped) {
    return switch (escaped) {
      case 'n' -> '\n';
      case 'r' -> '\r';
      case 't' -> '\t';
      case '"' -> '"';
      case '\\' -> '\\';
      default -> escaped;
    };
  }

  private void skipWhitespace() {
    while (!isEof() && isWhitespace(input.charAt(index))) {
      index++;
    }
  }

  private static boolean isWhitespace(char value) {
    return value == ' ' || value == '\t' || value == '\n' || value == '\r';
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

}

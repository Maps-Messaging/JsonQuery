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

package io.mapsmessaging.jsonquery.parser;

import lombok.Getter;


public final class JsonQueryParseException extends RuntimeException {
  @Getter
  private final int position;
  private final String message;

  public JsonQueryParseException(String message, int position) {
    super(message);
    this.message = message;
    this.position = position;
  }


  @Override
  public String getMessage(){
    return message;
  }

  public static JsonQueryParseException keyExpected(int pos) {
    return new JsonQueryParseException("Key expected (pos: " + pos + ")", pos);
  }


  public static JsonQueryParseException propertyExpected(int pos) {
    return new JsonQueryParseException("Property expected (pos: " + pos + ")", pos);
  }

  public static JsonQueryParseException valueExpected(int pos) {
    return new JsonQueryParseException("Value expected (pos: " + pos + ")", pos);
  }

  public static JsonQueryParseException characterExpected(char ch, int pos) {
    return new JsonQueryParseException("Character '" + ch + "' expected (pos: " + pos + ")", pos);
  }

  public static JsonQueryParseException unexpectedPart(int pos) {
    return new JsonQueryParseException("Unexpected part '"+pos+"'", pos);
  }


  public static JsonQueryParseException unexpectedPart(String part) {
    return new JsonQueryParseException("Unexpected part '" + part + "'", -1);
  }

  public static JsonQueryParseException unexpectedPart(String part, int pos) {
    //return new JsonQueryParseException("Unexpected part '" + part + "'", pos);
    return new JsonQueryParseException("Unexpected part '" + part + "' (pos: " + pos + ")", pos);
  }
}

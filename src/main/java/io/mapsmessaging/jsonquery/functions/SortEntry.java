package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;

public final class SortEntry {

  private final int originalIndex;
  private final JsonElement element;
  private final JsonElement key;

  public SortEntry(int originalIndex, JsonElement element, JsonElement key) {
    this.originalIndex = originalIndex;
    this.element = element;
    this.key = key;
  }

  public int getOriginalIndex() {
    return originalIndex;
  }

  public JsonElement getElement() {
    return element;
  }

  public JsonElement getKey() {
    return key;
  }
}

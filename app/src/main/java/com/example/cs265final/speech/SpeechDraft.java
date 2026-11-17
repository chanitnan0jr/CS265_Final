package com.example.cs265final.speech;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;

/** Keeps recognition results scoped to the session that produced them. */
public final class SpeechDraft {
  private int generation = 0;
  private String prefix = "";

  public int start(@NonNull String existingText) {
    Objects.requireNonNull(existingText, "existingText");
    int end = existingText.length();
    while (end > 0 && isWhitespace(existingText.charAt(end - 1))) {
      end--;
    }
    prefix = existingText.substring(0, end);
    return ++generation;
  }

  public boolean isCurrent(int session) {
    return session == generation;
  }

  @Nullable
  public String result(int session, @NonNull String recognized) {
    Objects.requireNonNull(recognized, "recognized");
    if (!isCurrent(session)) {
      return null;
    }
    boolean hasPrefix = !isBlank(prefix);
    boolean hasRecognition = !isBlank(recognized);
    if (hasPrefix && hasRecognition) {
      return prefix + " " + recognized;
    }
    if (hasPrefix) {
      return prefix;
    }
    return hasRecognition ? recognized : "";
  }

  public void cancel() {
    generation++;
  }

  private static boolean isBlank(String text) {
    for (int i = 0; i < text.length(); i++) {
      if (!isWhitespace(text.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  // Kotlin's whitespace check includes non-breaking spaces as well as Java whitespace.
  private static boolean isWhitespace(char character) {
    return Character.isWhitespace(character) || Character.isSpaceChar(character);
  }
}

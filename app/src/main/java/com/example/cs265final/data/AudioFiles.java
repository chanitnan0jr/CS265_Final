package com.example.cs265final.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Helpers for resolving metadata from Android content URIs. */
public final class AudioFiles {
  private AudioFiles() {}

  /** Returns the display name for a document URI, or null if unavailable. */
  @Nullable
  public static String contentDisplayName(@NonNull ContentResolver resolver, @NonNull Uri uri) {
    try (Cursor cursor =
        resolver.query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
      if (cursor != null && cursor.moveToFirst()) {
        String name = cursor.getString(0);
        if (name != null && !name.trim().isEmpty()) {
          return name;
        }
      }
    } catch (Exception ignored) {
      // Some document providers throw on metadata queries; callers fall back to a default name.
    }
    return null;
  }
}

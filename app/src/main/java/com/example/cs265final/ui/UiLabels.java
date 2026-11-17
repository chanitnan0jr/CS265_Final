package com.example.cs265final.ui;

import android.content.Context;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import com.example.cs265final.R;
import com.example.cs265final.data.FolderStorage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Maps persisted mood and default folder keys to localized display labels and semantic colors.
 *
 * <p>Persisted data keeps canonical keys so switching Thai/English locales never corrupts stored
 * notes or folders.
 */
public final class UiLabels {
  public static final String MOOD_VERY_GOOD = "ดีมาก";
  public static final String MOOD_GOOD = "ดี";
  public static final String MOOD_NORMAL = "ปกติ";
  public static final String MOOD_STRESSED = "เครียด";
  public static final String MOOD_SAD = "เศร้า";

  public static final List<String> DEFAULT_MOODS =
      Collections.unmodifiableList(
          Arrays.asList(MOOD_VERY_GOOD, MOOD_GOOD, MOOD_NORMAL, MOOD_STRESSED, MOOD_SAD));

  private UiLabels() {}

  @NonNull
  public static String folderLabel(@NonNull Context context, @NonNull String storedFolder) {
    switch (storedFolder) {
      case "การเรียน":
        return context.getString(R.string.folder_study);
      case "ระบายความรู้สึก":
        return context.getString(R.string.folder_feelings);
      case "งาน / โปรเจกต์":
        return context.getString(R.string.folder_work);
      case "ไอเดีย":
        return context.getString(R.string.folder_ideas);
      case FolderStorage.DEFAULT_GENERAL_FOLDER:
        return context.getString(R.string.folder_general);
      case FolderStorage.DEFAULT_UPLOAD_FOLDER:
        return context.getString(R.string.folder_uploaded);
      default:
        return storedFolder;
    }
  }

  @ColorRes
  public static int folderTintRes(@NonNull String storedFolder) {
    switch (storedFolder) {
      case "การเรียน":
        return R.color.folder_tint_study;
      case "ระบายความรู้สึก":
        return R.color.folder_tint_feelings;
      case "งาน / โปรเจกต์":
        return R.color.folder_tint_work;
      case "ไอเดีย":
        return R.color.folder_tint_ideas;
      default:
        return R.color.folder_tint_default;
    }
  }

  @NonNull
  public static String moodLabel(@NonNull Context context, @NonNull String storedMood) {
    switch (storedMood) {
      case MOOD_VERY_GOOD:
        return context.getString(R.string.mood_very_good);
      case MOOD_GOOD:
        return context.getString(R.string.mood_good);
      case MOOD_NORMAL:
        return context.getString(R.string.mood_normal);
      case MOOD_STRESSED:
        return context.getString(R.string.mood_stressed);
      case MOOD_SAD:
        return context.getString(R.string.mood_sad);
      default:
        return storedMood;
    }
  }
}

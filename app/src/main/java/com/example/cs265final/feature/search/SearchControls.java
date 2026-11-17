package com.example.cs265final.feature.search;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.example.cs265final.R;
import java.util.function.Consumer;

/** Search input hosted by the search screen. */
public final class SearchControls extends LinearLayout {
  private final EditText queryInput;
  private Consumer<String> onQueryChanged;
  private boolean updatingQuery;

  public SearchControls(Context context) {
    this(context, null);
  }

  public SearchControls(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public SearchControls(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setOrientation(VERTICAL);
    LayoutInflater.from(context).inflate(R.layout.view_search_controls, this, true);
    queryInput = findViewById(R.id.input_search_query);
    queryInput.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence text, int start, int before, int count) {
        if (!updatingQuery && onQueryChanged != null) {
          onQueryChanged.accept(text.toString());
        }
      }

      @Override
      public void afterTextChanged(Editable editable) {}
    });
  }

  public void setOnQueryChanged(Consumer<String> listener) {
    onQueryChanged = listener;
  }

  /** Synchronizes external state without reporting it as a user edit. */
  public void setQuery(String query) {
    if (query.contentEquals(queryInput.getText())) {
      return;
    }
    updatingQuery = true;
    try {
      queryInput.setText(query);
      queryInput.setSelection(queryInput.length());
    } finally {
      updatingQuery = false;
    }
  }
}

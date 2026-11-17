package com.example.cs265final.feature.search;

import static org.junit.Assert.assertEquals;

import android.widget.EditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.cs265final.R;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SearchControlsTest {
  @Test
  public void reportsUserEditsButNotStateSynchronization() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      SearchControls controls = new SearchControls(
          InstrumentationRegistry.getInstrumentation().getTargetContext());
      List<String> changes = new ArrayList<>();
      controls.setOnQueryChanged(changes::add);

      controls.setQuery("บันทึก");
      EditText input = controls.findViewById(R.id.input_search_query);
      assertEquals("บันทึก", input.getText().toString());
      assertEquals(0, changes.size());

      input.setText("ภาษาไทย");
      assertEquals(1, changes.size());
      assertEquals("ภาษาไทย", changes.get(0));

      input.setSelection(2);
      controls.setQuery("ภาษาไทย");
      assertEquals(2, input.getSelectionStart());
      assertEquals(1, changes.size());

      controls.setQuery("");
      assertEquals("", input.getText().toString());
      assertEquals(1, changes.size());
    });
  }
}

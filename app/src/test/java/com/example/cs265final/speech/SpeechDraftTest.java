package com.example.cs265final.speech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SpeechDraftTest {
  @Test
  public void resultsReplacePartialsAndIgnoreCancelledSessions() {
    SpeechDraft draft = new SpeechDraft();
    int first = draft.start("Existing note  ");
    assertEquals("Existing note hello", draft.result(first, "hello"));
    assertEquals("Existing note hello again", draft.result(first, "hello again"));
    draft.cancel();
    assertNull(draft.result(first, "late result"));

    int second = draft.start("Existing note hello again");
    assertTrue(draft.isCurrent(second));
    assertNull(draft.result(first, "older result"));
    assertEquals("Existing note hello again next", draft.result(second, "next"));
    assertEquals("Existing note hello again", draft.result(second, ""));
  }

  @Test
  public void whitespaceAndEmptyInputsMatchDraftJoiningRules() {
    SpeechDraft draft = new SpeechDraft();
    int empty = draft.start(" \u00a0");
    assertEquals("", draft.result(empty, "\t\u00a0"));
    assertEquals("  spoken ", draft.result(empty, "  spoken "));
    int existing = draft.start("note\u00a0\t");
    assertEquals("note", draft.result(existing, "\u00a0"));
    assertEquals("note  spoken ", draft.result(existing, " spoken "));
    assertNull(draft.result(empty, "late"));
  }
}

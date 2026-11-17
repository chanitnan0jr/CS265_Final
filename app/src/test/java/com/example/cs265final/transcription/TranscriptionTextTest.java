package com.example.cs265final.transcription;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TranscriptionTextTest {
  @Test
  public void aFreshUploadGetsTheTranscript() {
    assertEquals("ข้อความ", TranscriptionClient.mergeTranscript("", "ข้อความ"));
  }

  @Test
  public void aTypedBodyIsKeptAndTheTranscriptIsAppended() {
    assertEquals(
        "โน้ตที่พิมพ์ไว้\n\nข้อความ",
        TranscriptionClient.mergeTranscript("โน้ตที่พิมพ์ไว้\n", "ข้อความ"));
  }
}

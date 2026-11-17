package com.example.cs265final;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.cs265final.data.AudioUtils;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Test;

public class WavFileTest {
  @Test
  public void writesPcmHeaderAndPayload() throws Exception {
    File file = Files.createTempFile("voice2note", ".wav").toFile();
    try {
      byte[] pcm = new byte[] {1, 2, 3, 4};
      byte[] bytes = Files.readAllBytes(AudioUtils.writeWavFile(file, pcm).toPath());
      ByteBuffer header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
      assertEquals("RIFF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
      assertEquals(36 + pcm.length, header.getInt(4));
      assertEquals(16_000, header.getInt(24));
      assertEquals(pcm.length, header.getInt(40));
      assertArrayEquals(pcm, Arrays.copyOfRange(bytes, 44, bytes.length));
    } finally {
      file.delete();
    }
  }

  @Test
  public void writesMp3FrameStreamFromPcm() throws Exception {
    File file = Files.createTempFile("voice2note", ".mp3").toFile();
    try {
      int samples = 16_000;
      ByteBuffer pcm = ByteBuffer.allocate(samples * 2).order(ByteOrder.LITTLE_ENDIAN);
      for (int i = 0; i < samples; i++) {
        short sample = (short) (Math.sin(2.0 * Math.PI * 440.0 * i / 16_000.0) * 16_000.0);
        pcm.putShort(sample);
      }
      byte[] bytes = Files.readAllBytes(AudioUtils.writeMp3File(file, pcm.array()).toPath());
      assertTrue("MP3 output should not be empty", bytes.length > 1_000);
      assertEquals(0xFF, bytes[0] & 0xFF);
      assertEquals(0xE0, bytes[1] & 0xE0);
    } finally {
      file.delete();
    }
  }

  @Test
  public void onlyManagedRecordingsAreEligibleForDeletion() throws Exception {
    File directory = Files.createTempDirectory("voice2note").toFile();
    try {
      assertTrue(AudioUtils.isManagedRecording(new File(directory, "note_123.mp3"), directory));
      assertTrue(AudioUtils.isManagedRecording(new File(directory, "note_123.wav"), directory));
      assertFalse(AudioUtils.isManagedRecording(new File(directory, "other.mp3"), directory));
      assertFalse(AudioUtils.isManagedRecording(new File(directory, "other.wav"), directory));
      assertFalse(
          AudioUtils.isManagedRecording(new File(directory, "../note_outside.mp3"), directory));
      assertFalse(
          AudioUtils.isManagedRecording(new File(directory, "../note_outside.wav"), directory));
    } finally {
      directory.delete();
    }
  }

  @Test
  public void detectsAudibleNonZeroPcm() {
    assertFalse(AudioUtils.hasNonZeroPcm(new byte[32]));
    assertTrue(AudioUtils.hasNonZeroPcm(new byte[] {0, 1}));
  }

  @Test
  public void resamplesMonoPcmTo16kHz() {
    ByteBuffer pcm = ByteBuffer.allocate(800 * 2).order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < 800; i++) {
      pcm.putShort((short) (10_000 * Math.sin(2 * Math.PI * 1_000 * i / 8_000)));
    }
    byte[] result = AudioUtils.resampleMonoPcm16(pcm.array(), 8_000, 16_000);
    assertEquals(1_600 * 2, result.length);
    ShortBuffer samples = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
    for (int i = 40; i < 1_560; i++) {
      double expected = 10_000 * Math.sin(2 * Math.PI * 1_000 * i / 16_000);
      assertTrue(
          "Sample " + i + " deviates from a smooth tone",
          Math.abs(samples.get(i) - expected) < 100);
    }
  }
}

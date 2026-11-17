package com.example.cs265final.data;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.sciss.jump3r.mp3.BitStream;
import de.sciss.jump3r.mp3.GainAnalysis;
import de.sciss.jump3r.mp3.GetAudio;
import de.sciss.jump3r.mp3.ID3Tag;
import de.sciss.jump3r.mp3.Lame;
import de.sciss.jump3r.mp3.MPEGMode;
import de.sciss.jump3r.mp3.Parse;
import de.sciss.jump3r.mp3.Presets;
import de.sciss.jump3r.mp3.PsyModel;
import de.sciss.jump3r.mp3.Quantize;
import de.sciss.jump3r.mp3.QuantizePVT;
import de.sciss.jump3r.mp3.Reservoir;
import de.sciss.jump3r.mp3.Takehiro;
import de.sciss.jump3r.mp3.VBRTag;
import de.sciss.jump3r.mp3.Version;
import de.sciss.jump3r.mp3.LameGlobalFlags;
import de.sciss.jump3r.mpg.Common;
import de.sciss.jump3r.mpg.Interface;
import de.sciss.jump3r.mpg.MPGLib;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** PCM encoding, resampling, WAV/MP3 writing, and managed recording file checks. */
public final class AudioUtils {
  public static final int DEFAULT_SAMPLE_RATE = 16_000;

  private AudioUtils() {}

  @NonNull
  public static File writeWavFile(@NonNull File target, @NonNull byte[] pcm) throws IOException {
    return writeWavFile(target, pcm, DEFAULT_SAMPLE_RATE);
  }

  @NonNull
  public static File writeWavFile(@NonNull File target, @NonNull byte[] pcm, int sampleRate)
      throws IOException {
    byte[] header =
        ByteBuffer.allocate(44)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".getBytes(StandardCharsets.US_ASCII))
            .putInt(36 + pcm.length)
            // "WAVEfmt " is the standard RIFF chunk ID.
            .put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII))
            .putInt(16)
            .putShort((short) 1)
            .putShort((short) 1)
            .putInt(sampleRate)
            .putInt(sampleRate * 2)
            .putShort((short) 2)
            .putShort((short) 16)
            .put("data".getBytes(StandardCharsets.US_ASCII))
            .putInt(pcm.length)
            .array();
    try (FileOutputStream out = new FileOutputStream(target)) {
      out.write(header);
      out.write(pcm);
    }
    return target;
  }

  @NonNull
  public static File writeMp3File(@NonNull File target, @NonNull byte[] pcm) throws IOException {
    return writeMp3File(target, pcm, DEFAULT_SAMPLE_RATE);
  }

  @NonNull
  public static File writeMp3File(@NonNull File target, @NonNull byte[] pcm, int sampleRate)
      throws IOException {
    Lame lame = new Lame();
    GainAnalysis ga = new GainAnalysis();
    BitStream bs = new BitStream();
    Presets presets = new Presets();
    QuantizePVT quantizePvt = new QuantizePVT();
    Quantize quantize = new Quantize();
    VBRTag vbr = new VBRTag();
    Version version = new Version();
    ID3Tag id3 = new ID3Tag();
    Reservoir reservoir = new Reservoir();
    Takehiro takehiro = new Takehiro();
    PsyModel psyModel = new PsyModel();
    GetAudio getAudio = new GetAudio();
    Parse parse = new Parse();
    MPGLib mpg = new MPGLib();
    Interface mpgInterface = new Interface();
    Common common = new Common();

    lame.setModules(ga, bs, presets, quantizePvt, quantize, vbr, version, id3, mpg);
    bs.setModules(ga, mpg, version, vbr);
    id3.setModules(bs, version);
    presets.setModules(lame);
    quantize.setModules(bs, reservoir, quantizePvt, takehiro);
    quantizePvt.setModules(takehiro, reservoir, psyModel);
    reservoir.setModules(bs);
    takehiro.setModules(quantizePvt);
    vbr.setModules(lame, bs, version);
    getAudio.setModules(parse, mpg);
    parse.setModules(version, id3, presets);
    mpg.setModules(mpgInterface, common);
    mpgInterface.setModules(vbr, common);

    LameGlobalFlags gfp = lame.lame_init();
    try {
      gfp.num_channels = 1;
      gfp.in_samplerate = sampleRate;
      gfp.out_samplerate = sampleRate;
      gfp.brate = 64;
      gfp.mode = MPEGMode.MONO;
      gfp.quality = 5;
      gfp.bWriteVbrTag = false;
      gfp.write_id3tag_automatic = false;
      if (lame.lame_init_params(gfp) < 0) {
        throw new IllegalStateException("MP3 init failed");
      }
      int[] chunk = new int[16_384];
      byte[] mp3Buf = new byte[32_768];
      int totalSamples = pcm.length / 2;
      try (FileOutputStream out = new FileOutputStream(target)) {
        int pos = 0;
        while (pos < totalSamples) {
          int count = Math.min(chunk.length, totalSamples - pos);
          for (int i = 0; i < count; i++) {
            int offset = (pos + i) * 2;
            int lo = pcm[offset] & 0xFF;
            int hi = pcm[offset + 1];
            chunk[i] = ((hi << 8) | lo) << 16;
          }
          int encoded =
              lame.lame_encode_buffer_int(gfp, chunk, chunk, count, mp3Buf, 0, mp3Buf.length);
          if (encoded < 0) {
            throw new IllegalStateException("MP3 encode failed: " + encoded);
          }
          if (encoded > 0) {
            out.write(mp3Buf, 0, encoded);
          }
          pos += count;
        }
        int flushed = lame.lame_encode_flush(gfp, mp3Buf, 0, mp3Buf.length);
        if (flushed < 0) {
          throw new IllegalStateException("MP3 flush failed: " + flushed);
        }
        if (flushed > 0) {
          out.write(mp3Buf, 0, flushed);
        }
      }
    } finally {
      lame.lame_close(gfp);
    }
    return target;
  }

  /** Restricts automatic file deletion to app-managed recording files inside [directory]. */
  public static boolean isManagedRecording(@NonNull File file, @NonNull File directory) {
    try {
      File canonical = file.getCanonicalFile();
      File parent = canonical.getParentFile();
      String name = canonical.getName();
      return parent != null
          && parent.equals(directory.getCanonicalFile())
          && name.startsWith("note_")
          && (name.endsWith(".mp3") || name.endsWith(".wav"));
    } catch (IOException e) {
      return false;
    }
  }

  public static boolean hasNonZeroPcm(@NonNull byte[] pcm) {
    for (byte b : pcm) {
      if (b != 0) {
        return true;
      }
    }
    return false;
  }

  @NonNull
  public static byte[] resampleMonoPcm16(@NonNull byte[] pcm, int srcRate, int dstRate) {
    int srcCount = pcm.length / 2;
    if (srcCount == 0 || srcRate <= 0 || srcRate == dstRate) {
      return pcm;
    }
    int dstCount = Math.max(1, (int) (((long) srcCount * dstRate) / srcRate));
    byte[] out = new byte[dstCount * 2];
    int radius = 16;
    double cutoff = Math.min(1.0, (double) dstRate / srcRate);
    Map<Integer, double[]> filters = new HashMap<>();
    for (int i = 0; i < dstCount; i++) {
      long position = (long) i * srcRate;
      int center = (int) (position / dstRate);
      int phase = (int) (position % dstRate);
      double[] filter = filters.get(phase);
      if (filter == null) {
        filter = new double[radius * 2];
        double fraction = (double) phase / dstRate;
        for (int tap = 0; tap < filter.length; tap++) {
          double distance = fraction - (tap - radius + 1);
          if (Math.abs(distance) >= radius) {
            filter[tap] = 0.0;
          } else {
            double angle = Math.PI * distance * cutoff;
            double sinc = Math.abs(angle) < 1e-12 ? 1.0 : Math.sin(angle) / angle;
            filter[tap] = sinc * (0.5 + 0.5 * Math.cos(Math.PI * distance / radius));
          }
        }
        filters.put(phase, filter);
      }
      double sum = 0.0;
      double weights = 0.0;
      for (int tap = 0; tap < filter.length; tap++) {
        int j = center - radius + 1 + tap;
        double weight = filter[tap];
        int clamped = Math.max(0, Math.min(srcCount - 1, j));
        int index = clamped * 2;
        short sample = (short) ((pcm[index] & 0xFF) | (pcm[index + 1] << 8));
        sum += sample * weight;
        weights += weight;
      }
      int rounded = (int) Math.round(sum / weights);
      int sample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, rounded));
      out[i * 2] = (byte) sample;
      out[i * 2 + 1] = (byte) (sample >> 8);
    }
    return out;
  }

  @Nullable
  public static byte[] decodeAudioUriToPcm16k(@NonNull Context context, @NonNull Uri uri) {
    MediaExtractor extractor = new MediaExtractor();
    MediaCodec codec = null;
    try {
      extractor.setDataSource(context, uri, null);
      int track = -1;
      for (int i = 0; i < extractor.getTrackCount(); i++) {
        String trackMime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
        if (trackMime != null && trackMime.startsWith("audio/")) {
          track = i;
          break;
        }
      }
      if (track < 0) {
        return null;
      }
      extractor.selectTrack(track);
      MediaFormat trackFormat = extractor.getTrackFormat(track);
      String mime = trackFormat.getString(MediaFormat.KEY_MIME);
      if (mime == null) {
        return null;
      }
      int sampleRate =
          trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)
              ? trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
              : DEFAULT_SAMPLE_RATE;
      int channels =
          trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
              ? trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
              : 1;
      codec = MediaCodec.createDecoderByType(mime);
      codec.configure(trackFormat, null, null, 0);
      codec.start();

      MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
      ByteArrayOutputStream monoOut = new ByteArrayOutputStream();
      boolean inputDone = false;
      boolean outputDone = false;
      while (!outputDone) {
        if (!inputDone) {
          int inIdx = codec.dequeueInputBuffer(10_000L);
          if (inIdx >= 0) {
            ByteBuffer buf = codec.getInputBuffer(inIdx);
            if (buf != null) {
              int size = extractor.readSampleData(buf, 0);
              if (size < 0) {
                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                inputDone = true;
              } else {
                codec.queueInputBuffer(inIdx, 0, size, extractor.getSampleTime(), 0);
                extractor.advance();
              }
            }
          }
        }
        int outIdx = codec.dequeueOutputBuffer(info, 10_000L);
        if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          MediaFormat outFmt = codec.getOutputFormat();
          if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE);
          }
          if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            channels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
          }
        } else if (outIdx >= 0) {
          if (info.size > 0) {
            ByteBuffer buf = codec.getOutputBuffer(outIdx);
            if (buf != null) {
              buf.position(info.offset);
              buf.limit(info.offset + info.size);
              byte[] chunk = new byte[info.size];
              buf.get(chunk);
              if (channels <= 1) {
                monoOut.write(chunk, 0, chunk.length);
              } else {
                int stride = channels * 2;
                for (int pos = 0; pos + stride <= chunk.length; pos += stride) {
                  monoOut.write(chunk[pos]);
                  monoOut.write(chunk[pos + 1]);
                }
              }
            }
          }
          codec.releaseOutputBuffer(outIdx, false);
          if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            outputDone = true;
          }
        }
      }
      return resampleMonoPcm16(monoOut.toByteArray(), sampleRate, DEFAULT_SAMPLE_RATE);
    } catch (Exception e) {
      return null;
    } finally {
      if (codec != null) {
        try {
          codec.stop();
        } catch (Exception ignored) {
          // Best-effort cleanup.
        }
        try {
          codec.release();
        } catch (Exception ignored) {
          // Best-effort cleanup.
        }
      }
      try {
        extractor.release();
      } catch (Exception ignored) {
        // Best-effort cleanup.
      }
    }
  }
}

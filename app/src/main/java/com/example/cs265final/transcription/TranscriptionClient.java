package com.example.cs265final.transcription;

import androidx.annotation.NonNull;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONObject;

/** Talks to the transcription API defined in Voice2Note_Pack/Roadmap-Plan.md (section 4.4). */
public final class TranscriptionClient {
  private static final int CONNECT_TIMEOUT_MS = 15_000;
  private static final int READ_TIMEOUT_MS = 60_000;
  private static final int MAX_RESPONSE_BYTES = 1_000_000;

  private TranscriptionClient() {}

  @NonNull
  public static String submit(
      @NonNull String baseUrl,
      @NonNull String fileName,
      @NonNull String mimeType,
      @NonNull String language,
      @NonNull InputStream audio)
      throws IOException, TranscriptionException {
    String boundary = "Voice2Note-" + UUID.randomUUID();
    HttpURLConnection connection =
        open(
            normalizeBaseUrl(baseUrl) + "/api/v1/transcriptions",
            "POST",
            boundary);
    try {
      try (OutputStream out = new BufferedOutputStream(connection.getOutputStream())) {
        writeField(out, boundary, language);
        writeFile(out, boundary, fileName, mimeType, audio);
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
      }
      int code = connection.getResponseCode();
      String body = readBody(connection, code);
      if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_ACCEPTED) {
        String jobId = optText(new JSONObject(body), "jobId");
        if (jobId.trim().isEmpty()) {
          throw new TranscriptionException("Server did not return a job ID", false);
        }
        return jobId;
      }
      throw httpError(code);
    } catch (TranscriptionException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new TranscriptionException("Invalid response format from server", false);
    } finally {
      connection.disconnect();
    }
  }

  @NonNull
  public static TranscriptionResponse poll(@NonNull String baseUrl, @NonNull String jobId)
      throws IOException, TranscriptionException {
    String encodedJob =
        URLEncoder.encode(jobId, StandardCharsets.UTF_8.name()).replace("+", "%20");
    HttpURLConnection connection =
        open(
            normalizeBaseUrl(baseUrl) + "/api/v1/transcriptions/" + encodedJob,
            "GET",
            null);
    try {
      int code = connection.getResponseCode();
      String body = readBody(connection, code);
      if (code == HttpURLConnection.HTTP_OK) {
        JSONObject json = new JSONObject(body);
        return new TranscriptionResponse(
            optText(json, "status").toLowerCase(Locale.ROOT),
            optText(json, "text"),
            optText(json, "error"));
      }
      throw httpError(code);
    } catch (TranscriptionException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new TranscriptionException("Invalid response format from server", false);
    } finally {
      connection.disconnect();
    }
  }

  @NonNull
  public static String mimeTypeFor(@NonNull String fileName) {
    int dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot == fileName.length() - 1) {
      return "application/octet-stream";
    }
    String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    switch (ext) {
      case "mp3":
        return "audio/mpeg";
      case "m4a":
      case "mp4":
        return "audio/mp4";
      case "wav":
        return "audio/wav";
      case "aac":
        return "audio/aac";
      case "ogg":
      case "opus":
        return "audio/ogg";
      default:
        return "application/octet-stream";
    }
  }

  /** Appends an async transcript to an existing typed body, or replaces an empty body. */
  @NonNull
  public static String mergeTranscript(@NonNull String body, @NonNull String transcript) {
    if (body.trim().isEmpty()) {
      return transcript;
    }
    int end = body.length();
    while (end > 0 && Character.isWhitespace(body.charAt(end - 1))) {
      end--;
    }
    return body.substring(0, end) + "\n\n" + transcript;
  }

  private static String normalizeBaseUrl(String value) throws TranscriptionException {
    String base = value.trim();
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    if (!base.startsWith("https://") && !base.startsWith("http://")) {
      throw new TranscriptionException("Transcription server URL is not configured", false);
    }
    return base;
  }

  private static HttpURLConnection open(String url, String method, String multipartBoundary)
      throws IOException, TranscriptionException {
    HttpURLConnection connection;
    try {
      connection = (HttpURLConnection) new URL(url).openConnection();
    } catch (Exception e) {
      throw new TranscriptionException("Invalid transcription server URL", false);
    }
    connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
    connection.setReadTimeout(READ_TIMEOUT_MS);
    connection.setRequestProperty("Accept", "application/json");
    connection.setRequestMethod(method);
    if (multipartBoundary != null) {
      connection.setDoOutput(true);
      connection.setRequestProperty(
          "Content-Type", "multipart/form-data; boundary=" + multipartBoundary);
      connection.setChunkedStreamingMode(16_384);
    }
    return connection;
  }

  private static void writeField(OutputStream out, String boundary, String value)
      throws IOException {
    String header =
        "--"
            + boundary
            + "\r\nContent-Disposition: form-data; name=\"language\"\r\n\r\n"
            + value
            + "\r\n";
    out.write(header.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeFile(
      OutputStream out, String boundary, String fileName, String mimeType, InputStream audio)
      throws IOException {
    String safeName =
        fileName.replace("\\", "_").replace("\"", "_").replace("\r", " ").replace("\n", " ");
    if (safeName.trim().isEmpty()) {
      safeName = "audio";
    }
    String safeMime = mimeType.replace("\r", "").replace("\n", "");
    if (safeMime.trim().isEmpty()) {
      safeMime = "application/octet-stream";
    }
    String header =
        "--"
            + boundary
            + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
            + safeName
            + "\"\r\nContent-Type: "
            + safeMime
            + "\r\n\r\n";
    out.write(header.getBytes(StandardCharsets.UTF_8));
    byte[] buffer = new byte[8_192];
    int read;
    while ((read = audio.read(buffer)) >= 0) {
      if (read > 0) {
        out.write(buffer, 0, read);
      }
    }
    out.write("\r\n".getBytes(StandardCharsets.UTF_8));
  }

  private static String readBody(HttpURLConnection connection, int code)
      throws IOException, TranscriptionException {
    InputStream stream =
        (code >= 200 && code <= 299) ? connection.getInputStream() : connection.getErrorStream();
    if (stream == null) {
      return "";
    }
    try (InputStream input = stream) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[8_192];
      int total = 0;
      int read;
      while ((read = input.read(buffer)) >= 0) {
        total += read;
        if (total > MAX_RESPONSE_BYTES) {
          throw new TranscriptionException("Server response exceeded size limit", false);
        }
        out.write(buffer, 0, read);
      }
      return out.toString(StandardCharsets.UTF_8.name());
    }
  }

  private static TranscriptionException httpError(int code) {
    String message;
    if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
      message = "Server denied access (" + code + ")";
    } else if (code == 413) {
      message = "Audio file is too large (" + code + ")";
    } else if (code >= 400 && code <= 499) {
      message = "Server rejected request (" + code + ")";
    } else {
      message = "Server error (" + code + ")";
    }
    boolean retryable =
        code >= 500 || code == HttpURLConnection.HTTP_CLIENT_TIMEOUT || code == 429;
    return new TranscriptionException(message, retryable);
  }

  private static String optText(JSONObject json, String key) {
    return json.isNull(key) ? "" : json.optString(key, "");
  }
}

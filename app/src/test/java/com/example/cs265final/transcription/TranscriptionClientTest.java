package com.example.cs265final.transcription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TranscriptionClientTest {
  private static final class MockResponse {
    final int status;
    final String body;

    MockResponse(int status, String body) {
      this.status = status;
      this.body = body;
    }
  }

  private ServerSocket server;
  private Thread serverThread;
  private String baseUrl;
  private final Map<String, MockResponse> routes = new ConcurrentHashMap<>();
  private final AtomicReference<String> requestBody = new AtomicReference<>("");
  private final AtomicReference<String> contentType = new AtomicReference<>("");

  @Before
  public void startServer() throws IOException {
    server = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
    baseUrl = "http://127.0.0.1:" + server.getLocalPort();
    serverThread =
        new Thread(
            () -> {
              while (!server.isClosed()) {
                try (Socket client = server.accept()) {
                  handleClient(client);
                } catch (IOException ignored) {
                  if (server.isClosed()) {
                    break;
                  }
                }
              }
            },
            "MockHttpServer");
    serverThread.setDaemon(true);
    serverThread.start();
  }

  @After
  public void stopServer() throws IOException {
    server.close();
  }

  private void respond(String path, int status, String response) {
    routes.put(path, new MockResponse(status, response));
  }

  private void handleClient(Socket client) throws IOException {
    BufferedInputStream input = new BufferedInputStream(client.getInputStream());
    String requestLine = readLine(input);
    if (requestLine.isEmpty()) {
      return;
    }
    String[] parts = requestLine.split(" ");
    String path = parts.length > 1 ? parts[1] : "/";

    String headerContentType = "";
    boolean chunked = false;
    int contentLength = 0;
    String line;
    while (!(line = readLine(input)).isEmpty()) {
      int colon = line.indexOf(':');
      if (colon > 0) {
        String name = line.substring(0, colon).trim().toLowerCase();
        String value = line.substring(colon + 1).trim();
        if ("content-type".equals(name)) {
          headerContentType = value;
        } else if ("transfer-encoding".equals(name) && value.toLowerCase().contains("chunked")) {
          chunked = true;
        } else if ("content-length".equals(name)) {
          contentLength = Integer.parseInt(value);
        }
      }
    }
    contentType.set(headerContentType);

    if (chunked) {
      requestBody.set(readChunkedUtf8(input));
    } else if (contentLength > 0) {
      requestBody.set(readFixedUtf8(input, contentLength));
    } else {
      requestBody.set("");
    }

    MockResponse mock = routes.getOrDefault(path, new MockResponse(404, "{}"));
    byte[] bodyBytes = mock.body.getBytes(StandardCharsets.UTF_8);
    String headers =
        "HTTP/1.1 "
            + mock.status
            + " OK\r\nContent-Type: application/json\r\nContent-Length: "
            + bodyBytes.length
            + "\r\nConnection: close\r\n\r\n";
    OutputStream out = client.getOutputStream();
    out.write(headers.getBytes(StandardCharsets.US_ASCII));
    out.write(bodyBytes);
    out.flush();
  }

  private static String readLine(InputStream input) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int b;
    while ((b = input.read()) >= 0) {
      if (b == '\n') {
        break;
      }
      if (b != '\r') {
        out.write(b);
      }
    }
    return new String(out.toByteArray(), StandardCharsets.US_ASCII);
  }

  private static String readFixedUtf8(InputStream input, int length) throws IOException {
    byte[] buf = new byte[length];
    int offset = 0;
    while (offset < length) {
      int n = input.read(buf, offset, length - offset);
      if (n < 0) {
        break;
      }
      offset += n;
    }
    return new String(buf, 0, offset, StandardCharsets.UTF_8);
  }

  private static String readChunkedUtf8(InputStream input) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    while (true) {
      String sizeLine = readLine(input).trim();
      int semi = sizeLine.indexOf(';');
      if (semi >= 0) {
        sizeLine = sizeLine.substring(0, semi);
      }
      int chunkSize = sizeLine.isEmpty() ? 0 : Integer.parseInt(sizeLine, 16);
      if (chunkSize == 0) {
        readLine(input);
        break;
      }
      byte[] chunk = new byte[chunkSize];
      int offset = 0;
      while (offset < chunkSize) {
        int n = input.read(chunk, offset, chunkSize - offset);
        if (n < 0) {
          break;
        }
        offset += n;
      }
      out.write(chunk, 0, offset);
      readLine(input);
    }
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }

  @Test
  public void submitsMultipartAudioAndReturnsJobId() throws Exception {
    respond("/api/v1/transcriptions", 202, "{\"jobId\":\"job_1\",\"status\":\"queued\"}");
    byte[] audio = "ID3-audio-bytes".getBytes(StandardCharsets.UTF_8);
    String jobId =
        TranscriptionClient.submit(
            baseUrl, "note.mp3", "audio/mpeg", "th", new ByteArrayInputStream(audio));

    assertEquals("job_1", jobId);
    assertTrue(contentType.get().startsWith("multipart/form-data; boundary="));
    String body = requestBody.get();
    assertTrue(body.contains("name=\"language\""));
    assertTrue(body.contains("\r\n\r\nth\r\n"));
    assertTrue(body.contains("name=\"file\"; filename=\"note.mp3\""));
    assertTrue(body.contains("Content-Type: audio/mpeg"));
    assertTrue(body.contains("ID3-audio-bytes"));
  }

  @Test
  public void pollParsesCompletedTranscriptAndNullFields() throws Exception {
    respond(
        "/api/v1/transcriptions/job_1",
        200,
        "{\"jobId\":\"job_1\",\"status\":\"completed\",\"text\":\"สวัสดี\",\"durationSec\":4,\"segments\":[],\"error\":null}");
    TranscriptionResponse response = TranscriptionClient.poll(baseUrl, "job_1");

    assertEquals("completed", response.getStatus());
    assertEquals("สวัสดี", response.getText());
    assertEquals("", response.getError());
  }

  @Test
  public void pollParsesFailedJob() throws Exception {
    respond(
        "/api/v1/transcriptions/job_1",
        200,
        "{\"status\":\"failed\",\"error\":\"bad audio\"}");
    TranscriptionResponse response = TranscriptionClient.poll(baseUrl, "job_1");

    assertEquals("failed", response.getStatus());
    assertEquals("bad audio", response.getError());
  }

  @Test
  public void serverErrorsAreRetryable() {
    respond("/api/v1/transcriptions/job_1", 503, "{\"error\":\"down\"}");
    TranscriptionException error =
        assertThrows(
            TranscriptionException.class, () -> TranscriptionClient.poll(baseUrl, "job_1"));
    assertTrue(error.getRetryable());
  }

  @Test
  public void clientErrorsAreNotRetried() {
    respond("/api/v1/transcriptions/job_1", 400, "{\"error\":\"bad request\"}");
    TranscriptionException error =
        assertThrows(
            TranscriptionException.class, () -> TranscriptionClient.poll(baseUrl, "job_1"));
    assertFalse(error.getRetryable());
  }

  @Test
  public void missingServerConfigurationIsNotRetried() {
    TranscriptionException error =
        assertThrows(
            TranscriptionException.class, () -> TranscriptionClient.poll("", "job_1"));
    assertFalse(error.getRetryable());
  }

  @Test
  public void mimeTypeFallsBackToTheFileExtension() {
    assertEquals("audio/mpeg", TranscriptionClient.mimeTypeFor("A.MP3"));
    assertEquals("audio/mp4", TranscriptionClient.mimeTypeFor("voice.m4a"));
    assertEquals("audio/wav", TranscriptionClient.mimeTypeFor("voice.wav"));
    assertEquals("application/octet-stream", TranscriptionClient.mimeTypeFor("voice.flac"));
  }
}

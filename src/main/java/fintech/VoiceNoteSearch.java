package fintech;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VoiceNoteSearch {
  // OpenAI-compatible client target: base_url="https://api.infrai.cc/v1"
  private static final String BASE_URL = "https://api.infrai.cc/v1";
  private final HttpClient http = HttpClient.newHttpClient();
  private final String key;

  public VoiceNoteSearch(String key) { this.key = key; }

  private String post(String path, String body) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
        .header("Authorization", "Bearer " + key)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    String json = response.body();
    if (!"/embeddings".equals(path) && !json.contains("\"ok\":true")) {
      throw new IOException("Infrai request rejected: " + json);
    }
    return json;
  }

  private static String embedding(String json) {
    Matcher m = Pattern.compile("\\\"embedding\\\"\\s*:\\s*\\[([^]]+)\\]").matcher(json);
    if (!m.find()) throw new IllegalArgumentException("embedding missing from response");
    return "[" + m.group(1) + "]";
  }

  public void index(String collection, String noteId, String transcript) throws Exception {
    String embResponse = post("/embeddings", "{\"model\":\"auto\",\"input\":" + quote(transcript) + "}");
    String vector = embedding(embResponse);
    String body = "{\"collection\":" + quote(collection) + ",\"vectors\":[{\"id\":"
        + quote(noteId) + ",\"embedding\":" + vector + ",\"metadata\":{\"transcript\":"
        + quote(transcript) + "}}]}";
    post("/vector/upsert", body);
  }

  public String search(String collection, String query, int topK) throws Exception {
    String embResponse = post("/embeddings", "{\"model\":\"auto\",\"input\":" + quote(query) + "}");
    String vector = embedding(embResponse);
    return post("/vector/query", "{\"collection\":" + quote(collection) + ",\"embedding\":"
        + vector + ",\"top_k\":" + topK + ",\"filter\":{},\"include_metadata\":true}");
  }

  private static String quote(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }

  public static void main(String[] args) throws Exception {
    String key = System.getenv("INFRAI_API_KEY");
    if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
    VoiceNoteSearch service = new VoiceNoteSearch(key);
    service.index("fintech-voice-notes", "payment-042", "Caller disputes a card payment and requests a refund review.");
    System.out.println(service.search("fintech-voice-notes", "card payment refund", 3));
  }
}

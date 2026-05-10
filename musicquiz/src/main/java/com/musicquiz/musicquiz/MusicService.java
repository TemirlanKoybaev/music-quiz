package com.musicquiz.musicquiz;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

@Service
public class MusicService {

    private final QueryHistoryRepository repository;
    private static final String AUDD_API_TOKEN = "test";

    public MusicService(QueryHistoryRepository repository) {
        this.repository = repository;
    }

    public QueryHistory recognize(MultipartFile multipartFile) throws Exception {
        File tempFile = File.createTempFile("audio", "_" + multipartFile.getOriginalFilename());
        multipartFile.transferTo(tempFile);

        String fileInfo = "";
        String tagGenre = "Не определено";
        try {
            AudioFile audioFile = AudioFileIO.read(tempFile);
            AudioHeader header = audioFile.getAudioHeader();
            fileInfo = header.getFormat() + ", " + header.getBitRate() + " kbps, " + header.getSampleRate() + " Hz";
            String g = audioFile.getTag().getFirst(FieldKey.GENRE);
            if (g != null && !g.isEmpty()) tagGenre = g;
        } catch (Exception e) {
            fileInfo = "Не удалось прочитать метаданные";
        }

        String responseBody = sendToAudd(tempFile);

        String artist = extractField(responseBody, "artist");
        String title = extractField(responseBody, "title");
        String genre = extractSpotifyGenre(responseBody);
        if (genre.equals("Не определено")) genre = tagGenre;

        QueryHistory record = new QueryHistory();
        record.setFileName(multipartFile.getOriginalFilename() + " | " + fileInfo);
        record.setArtist(artist);
        record.setTitle(title);
        record.setGenre(genre);
        repository.save(record);

        tempFile.delete();
        return record;
    }

    private String sendToAudd(File file) throws IOException, InterruptedException {
        String boundary = "----Boundary" + System.currentTimeMillis();
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        String header = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"api_token\"\r\n\r\n" +
                AUDD_API_TOKEN + "\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"return\"\r\n\r\n" +
                "spotify" + "\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                "Content-Type: audio/mpeg\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";

        byte[] headerBytes = header.getBytes();
        byte[] footerBytes = footer.getBytes();
        byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.audd.io/"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start == -1) return "Не определено";
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "Не определено";
        return json.substring(start, end);
    }

    private String extractSpotifyGenre(String json) {
        String key = "\"genres\":[\"";
        int start = json.indexOf(key);
        if (start == -1) return "Не определено";
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "Не определено";
        return json.substring(start, end);
    }
}
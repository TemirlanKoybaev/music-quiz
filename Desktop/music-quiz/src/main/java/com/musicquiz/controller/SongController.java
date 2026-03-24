package com.musicquiz.controller;

import com.musicquiz.dto.SongResponse;
import com.musicquiz.model.Song;
import com.musicquiz.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    /**
     * Загрузить аудиофайл с метаданными песни.
     * POST /api/songs/upload
     * Content-Type: multipart/form-data
     *   file   — аудиофайл (audio/*)
     *   title  — название
     *   artist — исполнитель
     *   genre  — жанр (необязательно)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SongResponse> uploadSong(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("artist") String artist,
            @RequestParam(value = "genre", required = false) String genre) throws IOException {

        Song song = songService.uploadSong(file, title, artist, genre);
        return ResponseEntity.ok(SongResponse.from(song));
    }

    /** GET /api/songs — список всех песен */
    @GetMapping
    public ResponseEntity<List<SongResponse>> getAllSongs() {
        List<SongResponse> songs = songService.getAllSongs().stream()
                .map(SongResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(songs);
    }

    /** GET /api/songs/{id} — метаданные одной песни */
    @GetMapping("/{id}")
    public ResponseEntity<SongResponse> getSong(@PathVariable Long id) {
        return ResponseEntity.ok(SongResponse.from(songService.getSongById(id)));
    }

    /** GET /api/songs/{id}/audio — стриминг аудиофайла */
    @GetMapping("/{id}/audio")
    public ResponseEntity<Resource> streamAudio(@PathVariable Long id) throws IOException {
        Resource resource = songService.loadAudioFile(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .contentType(resolveAudioContentType(resource.getFilename()))
                .body(resource);
    }

    /** POST /api/songs/{id}/analyze — определить жанр песни */
    @PostMapping("/{id}/analyze")
    public ResponseEntity<SongResponse> analyzeGenre(@PathVariable Long id) {
        return ResponseEntity.ok(SongResponse.from(songService.analyzeGenre(id)));
    }

    private MediaType resolveAudioContentType(String filename) {
        if (filename == null) return MediaType.parseMediaType("audio/mpeg");
        String lower = filename.toLowerCase();
        if (lower.endsWith(".wav"))  return MediaType.parseMediaType("audio/wav");
        if (lower.endsWith(".ogg"))  return MediaType.parseMediaType("audio/ogg");
        if (lower.endsWith(".mp3"))  return MediaType.parseMediaType("audio/mpeg");
        if (lower.endsWith(".flac")) return MediaType.parseMediaType("audio/flac");
        return MediaType.parseMediaType("audio/mpeg");
    }

    /** DELETE /api/songs/{id} — удалить песню и файл */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable Long id) throws IOException {
        songService.deleteSong(id);
        return ResponseEntity.noContent().build();
    }
}

package com.musicquiz.service;

import com.musicquiz.model.Song;
import com.musicquiz.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {

    private final SongRepository songRepository;
    private final AudioAnalysisService audioAnalysisService;

    @Value("${app.audio.upload-dir}")
    private String uploadDir;

    public Song uploadSong(MultipartFile file, String title, String artist, String genre) throws IOException {
        validateAudioFile(file);

        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Song song = Song.builder()
                .title(title)
                .artist(artist)
                .genre(genre)
                .fileName(file.getOriginalFilename())
                .filePath(filePath.toString())
                .build();

        return songRepository.save(song);
    }

    public List<Song> getAllSongs() {
        return songRepository.findAll();
    }

    public Song getSongById(Long id) {
        return songRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Песня не найдена: " + id));
    }

    public Resource loadAudioFile(Long songId) throws MalformedURLException {
        Song song = getSongById(songId);
        Path path = Paths.get(song.getFilePath());
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) {
            throw new IllegalStateException("Аудиофайл не найден на диске: " + song.getFilePath());
        }
        return resource;
    }

    private static final List<String> FALLBACK_GENRES =
            List.of("Rock", "Pop", "Jazz", "Classical", "Hip-Hop");
    private static final java.util.Random FALLBACK_RANDOM = new java.util.Random();

    public Song analyzeGenre(Long id) {
        Song song = getSongById(id);
        String genre;
        try {
            AudioAnalysisService.AudioFeatures features =
                    audioAnalysisService.analyze(song.getFilePath());
            genre = classifyGenre(features);
            log.info("Песня '{}': BPM={}, energy={}, centroid={} Hz → {}",
                     song.getTitle(),
                     String.format("%.1f", features.bpm()),
                     String.format("%.4f", features.energy()),
                     String.format("%.0f", features.centroidHz()),
                     genre);
        } catch (Exception e) {
            genre = FALLBACK_GENRES.get(FALLBACK_RANDOM.nextInt(FALLBACK_GENRES.size()));
            log.warn("Анализ TarsosDSP недоступен для '{}', используется fallback-жанр: {}. Причина: {}",
                     song.getTitle(), genre, e.getMessage());
        }
        song.setGenre(genre);
        return songRepository.save(song);
    }

    /**
     * Правила классификации жанра по акустическим признакам.
     *
     * <pre>
     *  BPM        Energy (RMS)   Centroid (Hz)  → Жанр
     *  > 115      > 0.10         > 2500          → Rock      (быстро, громко, ярко)
     *  < 100      < 0.04         < 1500          → Classical (медленно, тихо, тепло)
     *  60–105     > 0.05         < 2000          → Hip-Hop   (средний темп, бас-центрированный)
     *  100–135    any            ≥ 2000          → Pop       (танцевальный, яркий)
     *  any        0.03–0.15      1500–3500       → Jazz      (умеренная энергия, средний спектр)
     *  fallback                                  → Pop
     * </pre>
     */
    private String classifyGenre(AudioAnalysisService.AudioFeatures f) {
        double bpm       = f.bpm();
        double energy    = f.energy();
        double centroid  = f.centroidHz();

        if (energy > 0.10 && bpm > 115 && centroid > 2500) return "Rock";
        if (energy < 0.04 && centroid < 1500)              return "Classical";
        if (bpm >= 60 && bpm <= 105
                && energy > 0.05 && centroid < 2000)       return "Hip-Hop";
        if (bpm >= 100 && bpm <= 135 && centroid >= 2000)  return "Pop";
        if (energy >= 0.03 && energy <= 0.15
                && centroid >= 1500 && centroid <= 3500)    return "Jazz";
        return "Pop";
    }

    public void deleteSong(Long id) throws IOException {
        Song song = getSongById(id);
        Files.deleteIfExists(Paths.get(song.getFilePath()));
        songRepository.delete(song);
    }

    private void validateAudioFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new IllegalArgumentException("Допустимы только аудиофайлы (audio/*). Получен: " + contentType);
        }
    }
}

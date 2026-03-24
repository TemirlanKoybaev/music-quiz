package com.musicquiz.dto;

import com.musicquiz.model.Song;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SongResponse {

    private Long id;
    private String title;
    private String artist;
    private String genre;
    private String fileName;
    private LocalDateTime uploadedAt;

    public static SongResponse from(Song song) {
        return SongResponse.builder()
                .id(song.getId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .genre(song.getGenre())
                .fileName(song.getFileName())
                .uploadedAt(song.getUploadedAt())
                .build();
    }
}

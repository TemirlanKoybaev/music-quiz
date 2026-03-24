package com.musicquiz.repository;

import com.musicquiz.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {

    List<Song> findByArtistIgnoreCase(String artist);

    List<Song> findByGenreIgnoreCase(String genre);

    boolean existsByTitleAndArtistIgnoreCase(String title, String artist);
}

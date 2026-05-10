package com.musicquiz.musicquiz;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class MusicController {

    private final MusicService musicService;
    private final QueryHistoryRepository repository;

    public MusicController(MusicService musicService, QueryHistoryRepository repository) {
        this.musicService = musicService;
        this.repository = repository;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/recognize")
    public String recognize(@RequestParam("file") MultipartFile file, Model model) {
        try {
            QueryHistory result = musicService.recognize(file);
            model.addAttribute("result", result);
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при распознавании: " + e.getMessage());
        }
        return "result";
    }

    @GetMapping("/history")
    public String history(Model model) {
        List<QueryHistory> history = repository.findAll();
        model.addAttribute("history", history);
        return "history";
    }
}
package com.musicquiz.service;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.util.fft.FFT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Извлекает акустические признаки из аудиофайла с помощью TarsosDSP:
 * <ul>
 *   <li>BPM — через детектор ударов (onset detection + медианный IOI)</li>
 *   <li>Energy — среднеквадратичная амплитуда (RMS) кадров</li>
 *   <li>Spectral centroid — центроид спектра (в Гц), характеризует «яркость» звука</li>
 * </ul>
 *
 * Поддерживаемые форматы: WAV (нативно), MP3 (через mp3spi SPI).
 */
@Slf4j
@Service
public class AudioAnalysisService {

    private static final int BUFFER_SIZE = 2048;
    private static final int OVERLAP     = 0;

    // ── Public API ───────────────────────────────────────────────────────────

    public AudioFeatures analyze(String filePath) {
        File file = new File(filePath);

        List<Double> onsetTimes  = Collections.synchronizedList(new ArrayList<>());
        double[]     totalRMS    = {0};
        double[]     totalCentroid = {0};
        int[]        frameCount  = {0};
        float[]      sampleRateHolder = {44100f};

        try {
            AudioDispatcher dispatcher =
                    AudioDispatcherFactory.fromFile(file, BUFFER_SIZE, OVERLAP);

            // 1. Определение ударов (BPM)
            ComplexOnsetDetector onsetDetector = new ComplexOnsetDetector(BUFFER_SIZE);
            onsetDetector.setHandler((time, salience) -> onsetTimes.add(time));
            dispatcher.addAudioProcessor(onsetDetector);

            // 2. Энергия + спектральный центроид
            dispatcher.addAudioProcessor(new AudioProcessor() {
                private final FFT fft = new FFT(BUFFER_SIZE);

                @Override
                public boolean process(AudioEvent event) {
                    if (frameCount[0] == 0) {
                        sampleRateHolder[0] = event.getSampleRate();
                    }

                    float[] buffer = event.getFloatBuffer();

                    // RMS (энергия)
                    double sumSq = 0;
                    for (float s : buffer) sumSq += (double) s * s;
                    totalRMS[0] += Math.sqrt(sumSq / buffer.length);

                    // Спектральный центроид через FFT
                    float[] copy = Arrays.copyOf(buffer, buffer.length);
                    fft.forwardTransform(copy);
                    float[] amplitudes = new float[BUFFER_SIZE / 2];
                    fft.modulus(copy, amplitudes);

                    double weightedSum = 0, totalAmp = 0;
                    for (int i = 1; i < amplitudes.length; i++) {
                        weightedSum += (double) i * amplitudes[i];
                        totalAmp    += amplitudes[i];
                    }
                    if (totalAmp > 0) {
                        totalCentroid[0] += weightedSum / totalAmp;
                    }

                    frameCount[0]++;
                    return true;
                }

                @Override
                public void processingFinished() {}
            });

            dispatcher.run();

        } catch (Exception e) {
            log.warn("Ошибка TarsosDSP при анализе '{}': {}", filePath, e.getMessage());
            throw new RuntimeException(
                "Не удалось проанализировать аудиофайл. " +
                "Убедитесь, что формат поддерживается (WAV, MP3): " + e.getMessage(), e);
        }

        double bpm      = estimateBpm(onsetTimes);
        double energy   = frameCount[0] > 0 ? totalRMS[0]     / frameCount[0] : 0;
        double centroid = frameCount[0] > 0 ? totalCentroid[0] / frameCount[0] : 0;

        // Перевод центроида из индекса бина в Гц
        double centroidHz = centroid * sampleRateHolder[0] / BUFFER_SIZE;

        log.debug("Анализ '{}': BPM={}, energy={}, centroid={} Hz",
                  filePath,
                  String.format("%.1f", bpm),
                  String.format("%.4f", energy),
                  String.format("%.0f", centroidHz));

        return new AudioFeatures(bpm, energy, centroidHz);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Оценивает BPM как 60 / (медианный интервал между ударами).
     * Фильтрует интервалы вне диапазона 0.25–2.0 с (30–240 BPM).
     */
    private double estimateBpm(List<Double> onsetTimes) {
        if (onsetTimes.size() < 4) return 0;

        List<Double> sorted = new ArrayList<>(onsetTimes);
        Collections.sort(sorted);

        List<Double> intervals = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            double ioi = sorted.get(i) - sorted.get(i - 1);
            if (ioi >= 0.25 && ioi <= 2.0) {
                intervals.add(ioi);
            }
        }
        if (intervals.isEmpty()) return 0;

        Collections.sort(intervals);
        double median = intervals.get(intervals.size() / 2);
        return 60.0 / median;
    }

    // ── Result record ────────────────────────────────────────────────────────

    /**
     * Акустические признаки, извлечённые из аудиофайла.
     *
     * @param bpm             Удары в минуту (0 если не определено)
     * @param energy          Средний RMS (диапазон примерно 0.0–0.5)
     * @param centroidHz      Спектральный центроид в Гц (типично 500–5000 Гц для музыки)
     */
    public record AudioFeatures(double bpm, double energy, double centroidHz) {}
}

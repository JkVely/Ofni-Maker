package com.ofni.service;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.KMEANS_RANDOM_CENTERS;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Service
public class ColorExtractionService {

    private static final int K = 5;

    public List<String> extractPalette(String imagePath) {
        var src = imread(imagePath);
        if (src.empty()) {
            throw new IllegalArgumentException("Cannot read image: " + imagePath);
        }

        var rgb = new Mat();
        cvtColor(src, rgb, COLOR_BGR2RGB);

        var reshaped = rgb.reshape(1, rgb.rows() * rgb.cols());
        var pixels = new Mat();
        reshaped.convertTo(pixels, opencv_core.CV_32F);

        var labels = new Mat();
        var centers = new Mat();
        var criteria = new TermCriteria(TermCriteria.COUNT | TermCriteria.EPS, 10, 1.0);

        opencv_core.kmeans(pixels, K, labels, criteria, 3, KMEANS_RANDOM_CENTERS, centers);

        var colorCounts = new ArrayList<ColorCount>();
        var centerIdx = (FloatIndexer) centers.createIndexer();
        for (int i = 0; i < K; i++) {
            int r = Math.round(centerIdx.get(i, 0));
            int g = Math.round(centerIdx.get(i, 1));
            int b = Math.round(centerIdx.get(i, 2));
            colorCounts.add(new ColorCount("#%02X%02X%02X".formatted(r, g, b), 0));
        }
        centerIdx.release();

        var labelIdx = (FloatIndexer) labels.createIndexer();
        for (int i = 0; i < labels.rows(); i++) {
            int cluster = (int) labelIdx.get(i, 0);
            colorCounts.get(cluster).increment();
        }
        labelIdx.release();

        return colorCounts.stream()
            .sorted(Comparator.comparingInt(ColorCount::count).reversed())
            .map(ColorCount::hex)
            .toList();
    }

    private static final class ColorCount {
        private final String hex;
        private int count;

        ColorCount(String hex, int count) {
            this.hex = hex;
            this.count = count;
        }

        String hex() { return hex; }
        int count() { return count; }
        void increment() { count++; }
    }
}

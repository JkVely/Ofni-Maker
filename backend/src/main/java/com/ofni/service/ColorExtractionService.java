package com.ofni.service;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.KMEANS_RANDOM_CENTERS;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_UNCHANGED;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Service
public class ColorExtractionService {

    private static final int K = 5;

    public List<String> extractPalette(String imagePath) {
        var src = imread(imagePath, IMREAD_UNCHANGED);
        if (src.empty()) {
            throw new IllegalArgumentException("Cannot read image: " + imagePath);
        }

        var bgr = new Mat();
        if (src.channels() == 4) {
            var channels = new MatVector();
            opencv_core.split(src, channels);
            var bgrChannels = new MatVector(channels.get(0), channels.get(1), channels.get(2));
            opencv_core.merge(bgrChannels, bgr);

            var white = new Mat(src.size(), opencv_core.CV_8UC3, new Scalar(255.0, 255.0, 255.0, 0.0));
            bgr.copyTo(white, channels.get(3));
            bgr = white;
        } else {
            bgr = src;
        }

        var rgb = new Mat();
        cvtColor(bgr, rgb, COLOR_BGR2RGB);

        var reshaped = rgb.reshape(1, rgb.rows() * rgb.cols());
        var pixels = new Mat();
        reshaped.convertTo(pixels, opencv_core.CV_32F);

        var labels = new Mat();
        var centers = new Mat(K, 3, opencv_core.CV_32F);
        var criteria = new TermCriteria(TermCriteria.COUNT | TermCriteria.EPS, 10, 1.0);

        opencv_core.kmeans(pixels, K, labels, criteria, 3, KMEANS_RANDOM_CENTERS, centers);

        var colorCounts = new ArrayList<ColorCount>();
        var centerIdx = (FloatIndexer) centers.createIndexer();
        for (int i = 0; i < K; i++) {
            int r = Math.min(255, Math.max(0, Math.round(centerIdx.get(i, 0))));
            int g = Math.min(255, Math.max(0, Math.round(centerIdx.get(i, 1))));
            int b = Math.min(255, Math.max(0, Math.round(centerIdx.get(i, 2))));
            colorCounts.add(new ColorCount("#%02X%02X%02X".formatted(r, g, b), 0));
        }
        centerIdx.release();

        var labelIdx = (IntRawIndexer) labels.createIndexer();
        for (int i = 0; i < labels.rows(); i++) {
            int cluster = labelIdx.get(i, 0);
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

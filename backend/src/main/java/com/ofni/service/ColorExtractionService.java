package com.ofni.service;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
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
        return extractPalette(imagePath, null);
    }

    public List<String> extractPalette(String imagePath, Rect cropBox) {
        var src = imread(imagePath, IMREAD_UNCHANGED);
        if (src.empty()) {
            throw new IllegalArgumentException("Cannot read image: " + imagePath);
        }

        var roi = (cropBox != null) ? new Mat(src, cropBox) : src;

        var rgb = new Mat();
        Mat alpha;

        if (roi.channels() == 4) {
            var channels = new org.bytedeco.opencv.opencv_core.MatVector();
            opencv_core.split(roi, channels);
            var bgrChannels = new org.bytedeco.opencv.opencv_core.MatVector(
                channels.get(0), channels.get(1), channels.get(2));
            opencv_core.merge(bgrChannels, rgb);
            alpha = channels.get(3);
        } else {
            rgb = roi.clone();
            alpha = new Mat();
        }

        var rgbF = new Mat();
        cvtColor(rgb, rgbF, COLOR_BGR2RGB);
        rgbF.convertTo(rgbF, opencv_core.CV_32F);

        var rgbIdx = (FloatIndexer) rgbF.createIndexer();
        var alphaIdx = alpha.empty() ? null : (UByteIndexer) alpha.createIndexer();
        var height = rgbF.rows();
        var width = rgbF.cols();

        var fgPixels = new ArrayList<float[]>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                var isFg = alphaIdx == null || alphaIdx.get(y, x) > 128;
                if (isFg) {
                    fgPixels.add(new float[]{
                        rgbIdx.get(y, x, 0),
                        rgbIdx.get(y, x, 1),
                        rgbIdx.get(y, x, 2)
                    });
                }
            }
        }
        rgbIdx.release();

        if (fgPixels.isEmpty()) {
            return List.of("#808080");
        }

        var pixelsMat = new Mat(fgPixels.size(), 3, opencv_core.CV_32F);
        var pxIdx = (FloatIndexer) pixelsMat.createIndexer();
        for (int i = 0; i < fgPixels.size(); i++) {
            pxIdx.put(i, 0, fgPixels.get(i)[0]);
            pxIdx.put(i, 1, fgPixels.get(i)[1]);
            pxIdx.put(i, 2, fgPixels.get(i)[2]);
        }
        pxIdx.release();

        var labels = new Mat();
        var centers = new Mat(K, 3, opencv_core.CV_32F);
        var criteria = new TermCriteria(TermCriteria.COUNT | TermCriteria.EPS, 10, 1.0);

        opencv_core.kmeans(pixelsMat, K, labels, criteria, 3, KMEANS_RANDOM_CENTERS, centers);

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
            colorCounts.get(labelIdx.get(i, 0)).increment();
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

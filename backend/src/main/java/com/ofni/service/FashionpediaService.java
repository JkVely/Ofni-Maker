package com.ofni.service;

import com.ofni.model.Category;
import com.ofni.model.Slot;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import ai.onnxruntime.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Service
public class FashionpediaService {

    private final Path modelPath;
    private OrtEnvironment env;
    private OrtSession session;

    private static final int IMG_SIZE = 320;
    private static final int NUM_MAIN_CLASSES = 27;
    private static final int NUM_QUERIES = 100;

    public FashionpediaService(@Value("${app.onnx.model-path}") String modelPath) {
        this.modelPath = Path.of(modelPath);
    }

    @PostConstruct
    void init() {
        if (!Files.exists(modelPath)) {
            throw new RuntimeException("Fashionpedia ONNX model not found: " + modelPath);
        }
        try {
            env = OrtEnvironment.getEnvironment();
            var opts = new OrtSession.SessionOptions();
            session = env.createSession(modelPath.toString(), opts);
        } catch (OrtException e) {
            throw new RuntimeException("Failed to load Fashionpedia ONNX model", e);
        }
    }

    @PreDestroy
    void cleanup() {
        try { if (session != null) session.close(); } catch (OrtException ignored) {}
    }

    public record FashionpediaResult(
        int classIndex, float confidence, String label,
        Category category, Slot slot,
        float boxCx, float boxCy, float boxW, float boxH,
        int origW, int origH
    ) {}

    public FashionpediaResult classify(String imagePath) {
        var img = imread(imagePath);
        if (img.empty()) {
            throw new IllegalArgumentException("Cannot read image: " + imagePath);
        }
        int origW = img.cols();
        int origH = img.rows();

        try (var tensor = preprocess(img);
             var results = session.run(Map.of("pixel_values", tensor))) {

            var logitsTensor = (OnnxTensor) results.get("logits").orElseThrow();
            var boxesTensor = (OnnxTensor) results.get("pred_boxes").orElseThrow();
            var logits = (float[][][]) logitsTensor.getValue();
            var boxes = (float[][][]) boxesTensor.getValue();

            var classScores = new float[NUM_MAIN_CLASSES];
            var bestQueryForClass = new int[NUM_MAIN_CLASSES];
            var bestProbForClass = new float[NUM_MAIN_CLASSES];

            for (int q = 0; q < NUM_QUERIES; q++) {
                var probs = softmax(logits[0][q]);
                for (int c = 0; c < NUM_MAIN_CLASSES; c++) {
                    classScores[c] += probs[c];
                    if (probs[c] > bestProbForClass[c]) {
                        bestProbForClass[c] = probs[c];
                        bestQueryForClass[c] = q;
                    }
                }
            }

            var bestClass = 0;
            for (int c = 1; c < NUM_MAIN_CLASSES; c++) {
                if (classScores[c] > classScores[bestClass]) {
                    bestClass = c;
                }
            }

            var bestQ = bestQueryForClass[bestClass];
            var originalBox = boxes[0][bestQ];
            var cx = originalBox[0];
            var cy = originalBox[1];
            var w = originalBox[2];
            var h = originalBox[3];

            var label = FashionpediaMapper.labelAt(bestClass);
            var category = FashionpediaMapper.toCategory(bestClass);
            var slot = FashionpediaMapper.toSlot(category);
            var confidence = classScores[bestClass];

            return new FashionpediaResult(
                bestClass, confidence, label, category, slot,
                cx, cy, w, h, origW, origH
            );
        } catch (OrtException e) {
            throw new RuntimeException("Fashionpedia ONNX inference failed", e);
        }
    }

    private OnnxTensor preprocess(Mat img) throws OrtException {
        var resized = new Mat();
        resize(img, resized, new Size(IMG_SIZE, IMG_SIZE));

        var rgb = new Mat();
        cvtColor(resized, rgb, COLOR_BGR2RGB);

        var normalized = new Mat();
        rgb.convertTo(normalized, opencv_core.CV_32F, 1.0 / 255.0, 0.0);

        var data = new float[1][3][IMG_SIZE][IMG_SIZE];
        var idx = (FloatIndexer) normalized.createIndexer();
        for (int y = 0; y < IMG_SIZE; y++) {
            for (int x = 0; x < IMG_SIZE; x++) {
                for (int c = 0; c < 3; c++) {
                    data[0][c][y][x] = (idx.get(y, x, c) - mean(c)) / std(c);
                }
            }
        }
        idx.release();

        return OnnxTensor.createTensor(env, data);
    }

    private static float mean(int c) { return switch (c) { case 0 -> 0.485f; case 1 -> 0.456f; default -> 0.406f; }; }
    private static float std(int c) { return switch (c) { case 0 -> 0.229f; case 1 -> 0.224f; default -> 0.225f; }; }

    private static float[] softmax(float[] logits) {
        var exp = new float[logits.length];
        float sum = 0;
        float max = logits[0];
        for (var v : logits) if (v > max) max = v;
        for (int i = 0; i < logits.length; i++) {
            exp[i] = (float) Math.exp(logits[i] - max);
            sum += exp[i];
        }
        for (int i = 0; i < exp.length; i++) {
            exp[i] /= sum;
        }
        return exp;
    }
}

package com.ofni.service;

import com.ofni.exception.ResourceNotFoundException;
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
public class OnnxClassificationService {

    private final Path modelPath;
    private OrtEnvironment env;
    private OrtSession session;

    private static final int IMG_SIZE = 224;

    private static final String[] DF2_LABELS = {
        "short sleeve top", "long sleeve top", "short sleeve outwear",
        "long sleeve outwear", "vest", "sling", "shorts", "trousers",
        "skirt", "short sleeve dress", "long sleeve dress",
        "vest dress", "sling dress"
    };

    public OnnxClassificationService(@Value("${app.onnx.model-path}") String modelPath) {
        this.modelPath = Path.of(modelPath);
    }

    @PostConstruct
    void init() {
        if (!Files.exists(modelPath)) {
            throw new ResourceNotFoundException("ONNX model", 0L);
        }
        try {
            env = OrtEnvironment.getEnvironment();
            var opts = new OrtSession.SessionOptions();
            session = env.createSession(modelPath.toString(), opts);
        } catch (OrtException e) {
            throw new RuntimeException("Failed to load ONNX model", e);
        }
    }

    @PreDestroy
    void cleanup() {
        try { if (session != null) session.close(); } catch (OrtException ignored) {}
    }

    public record ClassificationResult(int predictedIndex, float[] probabilities, String label) {}

    public ClassificationResult classify(String imagePath) {
        try (var tensor = preprocess(imagePath);
             var results = session.run(Map.of("input", tensor))) {

            var output = (OnnxTensor) results.get("output").orElseThrow();
            var logits = (float[][]) output.getValue();

            var probs = softmax(logits[0]);
            var maxIdx = argmax(probs);

            return new ClassificationResult(maxIdx, probs, DF2_LABELS[maxIdx]);
        } catch (OrtException e) {
            throw new RuntimeException("ONNX inference failed", e);
        }
    }

    private OnnxTensor preprocess(String imagePath) throws OrtException {
        var img = imread(imagePath);
        if (img.empty()) {
            throw new IllegalArgumentException("Cannot read image: " + imagePath);
        }

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
    private static float std(int c)  { return switch (c) { case 0 -> 0.229f; case 1 -> 0.224f; default -> 0.225f; }; }

    private static float[] softmax(float[] logits) {
        var exp = new float[logits.length];
        float sum = 0;
        for (int i = 0; i < logits.length; i++) {
            exp[i] = (float) Math.exp(logits[i]);
            sum += exp[i];
        }
        for (int i = 0; i < exp.length; i++) {
            exp[i] /= sum;
        }
        return exp;
    }

    private static int argmax(float[] arr) {
        int idx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[idx]) idx = i;
        }
        return idx;
    }
}

package com.superstore.app.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.FloatBuffer;
import java.util.Map;

@Service
public class XGBoostForecastService {

    private OrtEnvironment env;
    private OrtSession session;

    @PostConstruct
    public void init() {
        try {
            // Load environment and ONNX model binary
            env = OrtEnvironment.getEnvironment();
            byte[] modelBytes = new ClassPathResource("models/superstore_xgboost_model.onnx")
                    .getInputStream()
                    .readAllBytes();
            session = env.createSession(modelBytes, new OrtSession.SessionOptions());
        } catch (Exception e) {
            throw new IllegalStateException("FAILED to load XGBoost ONNX model.", e);
        }
    }

    public float predict30DayDemand(float currentStock, float avgDailySales, float reorderPoint, float leadTimeDays, float month) {
        try {
            // Match input shape [1 batch, 5 features]
            float[] inputValues = new float[]{ currentStock, avgDailySales, reorderPoint, leadTimeDays, month };
            long[] shape = new long[]{ 1, 5 };

            FloatBuffer buffer = FloatBuffer.wrap(inputValues);

            try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape);
                 OrtSession.Result result = session.run(Map.of("float_input", inputTensor))) {

                float[][] output = (float[][]) result.get(0).getValue();
                return Math.max(0.0f, output[0][0]); // Return non-negative predicted units
            }
        } catch (Exception e) {
            throw new RuntimeException("XGBoost prediction failure", e);
        }
    }

    @PreDestroy
    public void close() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (Exception ignored) {}
    }
}
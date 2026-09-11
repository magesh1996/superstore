package com.superstore.app.pojo;

public record ProductForecastResult(
    String productId,
    String productName,
    int currentStock,
    int predictedDemand30Days,
    int recommendedRestock,
    String statusAlert
) {}
package org.production.models;

public class SimulationResult {
    private final Double time;
    private final String productionCenter;
    private final int workersCount;
    private final int bufferCount;

    public SimulationResult(Double time, String productionCenter, int workersCount, int bufferCount) {
        this.time = time;
        this.productionCenter = productionCenter;
        this.workersCount = workersCount;
        this.bufferCount = bufferCount;
    }

    @Override
    public String toString() {
        return String.format("%.2f, %s, %d, %d", time, productionCenter, workersCount, bufferCount);
    }
}

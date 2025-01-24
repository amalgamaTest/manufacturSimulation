package org.production.models;

public class ProductionCenter {
    private final String id;
    private final String name;
    private final int maxWorkers;
    private final double performance;
    private int currentWorkers;
    private int buffer;

    public ProductionCenter(String id, String name, int maxWorkers, double performance) {
        this.id = id;
        this.name = name;
        this.maxWorkers = maxWorkers;
        this.performance = performance;
        this.currentWorkers = 0;
        this.buffer = 0;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMaxWorkers() {
        return maxWorkers;
    }

    public double getPerformance() {
        return performance;
    }

    public int getCurrentWorkers() {
        return currentWorkers;
    }

    @Override
    public String toString() {
        return "ProductionCenter{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", maxWorkers=" + maxWorkers +
                ", performance=" + performance +
                ", currentWorkers=" + currentWorkers +
                ", buffer=" + buffer +
                '}';
    }
}

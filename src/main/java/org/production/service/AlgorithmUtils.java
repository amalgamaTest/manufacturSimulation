package org.production.service;

import org.production.models.Connection;
import org.production.models.ProductionCenter;
import org.production.models.ScenarioData;

import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * Utility class for managing algorithms used in production center simulations.
 * This includes worker redistribution, connection selection, and buffer handling.
 */
public class AlgorithmUtils {

    /**
     * Selects the next connection for a production center based on weights.
     *
     * @param productionCenter the current production center
     * @param fromCenters list of connections originating from the current center
     * @param centerBuffer map of production center IDs to their respective buffers
     * @param centerWorkers map of production center IDs to the number of assigned workers
     * @param visitedCenters set of visited center IDs to avoid loops
     * @param scenarioData the scenario data containing configuration and details
     * @return the connection with the minimum weight
     * @throws RuntimeException if no valid connections are available
     */
    public static Connection selectNextConnection(ProductionCenter productionCenter,
                                                  List<Connection> fromCenters,
                                                  Map<String, BlockingQueue<String>> centerBuffer,
                                                  Map<String, Integer> centerWorkers,
                                                  Set<String> visitedCenters,
                                                  ScenarioData scenarioData) {

        if (fromCenters.isEmpty()) {
            if (productionCenter.getId().equals(scenarioData.startCenterId())) {
                return null;
            }
            throw new RuntimeException("No outgoing connections available for center: " + productionCenter.getId());
        }

        Map<String, Double> weights = new HashMap<>();

        for (Connection connection : fromCenters) {
            ProductionCenter destinationCenter = connection.toCenter();

            if (visitedCenters.contains(destinationCenter.getId())) {
                continue;
            }

            BlockingQueue<String> buffer = centerBuffer.get(destinationCenter.getId());
            if (buffer == null) {
                throw new RuntimeException("Buffer not found for center: " + destinationCenter.getId());
            }

            int currentWorkers = destinationCenter.getCurrentWorkers();
            if (currentWorkers == 0) {
                AlgorithmUtils.redistributeWorkers(centerWorkers, centerBuffer, scenarioData);
                currentWorkers = destinationCenter.getCurrentWorkers();
            }

            double weight = (destinationCenter.getPerformance() * buffer.size()) /
                    (destinationCenter.getMaxWorkers() - currentWorkers + 1);
            weights.put(destinationCenter.getId(), weight);
        }

        if (weights.isEmpty()) {
            throw new RuntimeException("No valid connections available for center: " + productionCenter.getId());
        }

        return fromCenters.stream()
                .min(Comparator.comparingDouble(conn -> weights.getOrDefault(conn.toCenter().getId(), Double.MAX_VALUE)))
                .orElseThrow(() -> new RuntimeException("No valid connections found for Dijkstra selection."));
    }

    /**
     * Redistributes workers among production centers based on their needs.
     *
     * @param centerWorkers map of production center IDs to the number of assigned workers
     * @param centerBuffers map of production center IDs to their respective buffers
     * @param scenarioData the scenario data containing configuration and details
     */
    public static void redistributeWorkers(
            Map<String, Integer> centerWorkers,
            Map<String, BlockingQueue<String>> centerBuffers,
            ScenarioData scenarioData
    ) {
        int totalWorkers = scenarioData.workersCount();
        int centersCount = scenarioData.centers().size();

        if (totalWorkers < centersCount) {
            redistributeWorkersLess(centerWorkers, scenarioData.centers(), centerBuffers, scenarioData);
        } else {
            redistributeWorkersMore(centerWorkers, scenarioData.centers(), centerBuffers, scenarioData.workersCount());
        }
    }

    /**
     * Redistributes workers when the total number of workers is greater than or equal to the number of production centers.
     *
     * @param centerWorkers map of production center IDs to the number of assigned workers
     * @param productionCenters list of all production centers
     * @param centerBuffers map of production center IDs to their respective buffers
     * @param totalWorkers the total number of workers available for redistribution
     */
    public static void redistributeWorkersMore(
            Map<String, Integer> centerWorkers,
            List<ProductionCenter> productionCenters,
            Map<String, BlockingQueue<String>> centerBuffers,
            int totalWorkers
    ) {
        synchronized (productionCenters) {
            for (ProductionCenter center : productionCenters) {
                centerWorkers.put(center.getId(), 0);
            }

            int freeWorkers = totalWorkers;

            productionCenters.sort((c1, c2) -> {
                int buf1 = centerBuffers.get(c1.getId()).size();
                int buf2 = centerBuffers.get(c2.getId()).size();

                double val1 = buf1 * c1.getPerformance();
                double val2 = buf2 * c2.getPerformance();

                return Double.compare(val2, val1);
            });

            for (ProductionCenter center : productionCenters) {
                String centerId = center.getId();
                BlockingQueue<String> buffer = centerBuffers.get(centerId);

                int bufferSize = (buffer != null) ? buffer.size() : 0;
                int needed = Math.min(bufferSize, center.getMaxWorkers());

                int toAssign = Math.min(needed, freeWorkers);
                centerWorkers.put(centerId, toAssign);

                freeWorkers -= toAssign;
                if (freeWorkers <= 0) {
                    break;
                }
            }
        }
    }

    /**
     * Redistributes workers when the total number of workers is less than the number of production centers.
     *
     * @param centerWorkers map of production center IDs to the number of assigned workers
     * @param productionCenters list of all production centers
     * @param centerBuffers map of production center IDs to their respective buffers
     * @param scenarioData the scenario data containing configuration and details
     */
    public static void redistributeWorkersLess(
            Map<String, Integer> centerWorkers,
            List<ProductionCenter> productionCenters,
            Map<String, BlockingQueue<String>> centerBuffers,
            ScenarioData scenarioData
    ) {
        for (ProductionCenter center : productionCenters) {
            String centerId = center.getId();
            BlockingQueue<String> buffer = centerBuffers.get(centerId);
            int assigned = centerWorkers.getOrDefault(centerId, 0);

            if ((buffer == null || buffer.isEmpty()) && assigned > 0) {
                centerWorkers.put(centerId, 0);
            }
        }

        int totalWorkers = scenarioData.workersCount();
        int alreadyAssigned = centerWorkers.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        int freeWorkers = totalWorkers - alreadyAssigned;

        if (freeWorkers > 0) {
            List<ProductionCenter> sortedCenters = new ArrayList<>(productionCenters);
            sortedCenters.sort((c1, c2) -> {
                double p1 = centerBuffers.get(c1.getId()).size() * c1.getPerformance();
                double p2 = centerBuffers.get(c2.getId()).size() * c2.getPerformance();
                return Double.compare(p2, p1);
            });

            for (ProductionCenter center : sortedCenters) {
                String centerId = center.getId();
                BlockingQueue<String> buffer = centerBuffers.get(centerId);
                if (buffer != null && !buffer.isEmpty()) {
                    int currentAssigned = centerWorkers.getOrDefault(centerId, 0);

                    int needed = Math.min(buffer.size(), center.getMaxWorkers());
                    int toAssign = needed - currentAssigned;

                    if (toAssign > 0 && freeWorkers > 0) {
                        int assignNow = Math.min(toAssign, freeWorkers);
                        centerWorkers.put(centerId, currentAssigned + assignNow);
                        freeWorkers -= assignNow;

                        if (freeWorkers <= 0) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Adjusts the number of workers assigned to centers if the total exceeds the available workers.
     *
     * @param centerWorkers map of production center IDs to the number of assigned workers
     * @param excessWorkers the number of workers to remove from the total assignments
     */
    public static void adjustExcessWorkers(Map<String, Integer> centerWorkers, int excessWorkers) {
        int[] excess = {excessWorkers};

        centerWorkers.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> {
                    if (excess[0] <= 0) return;

                    int currentWorkers = entry.getValue();
                    int toRemove = Math.min(currentWorkers, excess[0]);
                    centerWorkers.put(entry.getKey(), currentWorkers - toRemove);
                    excess[0] -= toRemove;
                });
    }

    /**
     * Retrieves the outgoing connections for a given production center.
     *
     * @param center the current production center
     * @param connections list of all connections
     * @return a list of outgoing connections for the given center
     */
    public static List<Connection> getOutgoingConnectionsForCenter(ProductionCenter center, List<Connection> connections) {
        List<Connection> outgoingConnections = new ArrayList<>();
        for (Connection conn : connections) {
            if (conn.fromCenter().getId().equals(center.getId())) {
                outgoingConnections.add(conn);
            }
        }
        return outgoingConnections;
    }
}

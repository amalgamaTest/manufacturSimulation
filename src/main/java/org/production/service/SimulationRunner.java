package org.production.service;

import org.production.models.ScenarioData;
import org.production.models.ProductionCenter;
import org.production.models.Connection;
import org.production.models.SimulationResult;

import java.util.*;
import java.util.concurrent.*;

import static org.production.service.AlgorithmUtils.adjustExcessWorkers;
import static org.production.service.AlgorithmUtils.getOutgoingConnectionsForCenter;

/**
 * Class responsible for running the simulation of production centers.
 * It manages buffers, worker distribution, and processing of details across multiple centers.
 */
public class SimulationRunner {
    private final ScenarioData scenarioData;
    private final ExecutorService executorService;
    private final Map<String, BlockingQueue<String>> centerBuffers;
    private final Map<String, Integer> centerWorkers;
    private final List<SimulationResult> resultList;
    private double currentTime;

    public SimulationRunner(ScenarioData scenarioData) {
        this.scenarioData = scenarioData;
        this.executorService = Executors.newFixedThreadPool(scenarioData.workersCount());
        this.centerBuffers = new ConcurrentHashMap<>();
        this.centerWorkers = new ConcurrentHashMap<>();
        this.resultList = new ArrayList<>();
        this.currentTime = 0.0;
        initializeBuffers();
    }

    /**
     * Initializes the buffers for all production centers, filling the start center with details.
     */
    private void initializeBuffers() {
        for (ProductionCenter center : scenarioData.centers()) {

            centerBuffers.put(center.getId(), new LinkedBlockingQueue<>());
            centerWorkers.put(center.getId(), 0);
        }

        BlockingQueue<String> startBuffer = centerBuffers.get(scenarioData.startCenterId());
        for (int i = 0; i < scenarioData.detailsCount(); i++) {
            startBuffer.offer("Detail-" + (i + 1));
        }
    }

    /**
     * Runs the simulation until all details are processed or all buffers are empty.
     */
    public void runSimulation() {
        try {
            while (!isSimulationComplete()) {
                List<Future<?>> tasks = new ArrayList<>();

                AlgorithmUtils.redistributeWorkers(centerWorkers, centerBuffers, scenarioData);

                int totalAssignedWorkers = centerWorkers.values()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .sum();

                System.out.printf("==> Шаг симуляции %.1f. Назначено работников: %d (из %d)%n",
                        currentTime, totalAssignedWorkers, scenarioData.workersCount());

                if (totalAssignedWorkers > scenarioData.workersCount()) {
                    int excessWorkers = totalAssignedWorkers - scenarioData.workersCount();
                    adjustExcessWorkers(centerWorkers, excessWorkers);
                }

                List<ProductionCenter> centersSnapshot = new ArrayList<>(scenarioData.centers());
                for (ProductionCenter center : centersSnapshot) {
                    tasks.add(executorService.submit(() -> processCenter(center)));
                }

                for (Future<?> task : tasks) {
                    task.get();
                }

                recordResults();
                currentTime += 1.0;
            }
            System.out.println("Simulation complete!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * Checks whether the simulation is complete by ensuring all buffers are empty and no workers are assigned.
     *
     * @return true if the simulation is complete, false otherwise
     */
    private boolean isSimulationComplete() {
        for (ProductionCenter center : scenarioData.centers()) {
            if (!centerBuffers.get(center.getId()).isEmpty()) {
                return false;
            }
        }
        return centerWorkers.values().stream().allMatch(count -> count == 0);
    }

    /**
     * Processes the given production center by simulating the processing of details and moving them to the next buffer.
     *
     * @param center the production center to process
     */
    private void processCenter(ProductionCenter center) {
        String centerId = center.getId();
        BlockingQueue<String> buffer = centerBuffers.get(centerId);
        int currentWorkers = centerWorkers.getOrDefault(centerId, 0);

        List<Future<?>> tasks = new ArrayList<>();
        for (int i = 0; i < currentWorkers; i++) {
            String detail = buffer.poll();
            if (detail == null) {
                break;
            }

            tasks.add(executorService.submit(() -> {
                simulateProcessing(center, detail);
                moveDetailToNextBuffer(center, detail);
            }));
        }

        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    /**
     * Simulates the processing of a detail at a production center.
     *
     * @param center the production center processing the detail
     * @param detail the detail being processed
     */
    private void simulateProcessing(ProductionCenter center, String detail) {
        try {
            double processingTime = center.getPerformance();
            System.out.printf("Processing detail %s at center %s for %.2f seconds.%n", detail, center.getName(), processingTime);
            Thread.sleep((long) (processingTime * 10));
            System.out.printf("Detail %s processed at center %s.%n", detail, center.getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Moves a processed detail to the buffer of the next production center based on selected connections.
     *
     * @param center the current production center
     * @param detail the processed detail
     */
    private void moveDetailToNextBuffer(ProductionCenter center, String detail) {
        List<Connection> outgoingConnections = getOutgoingConnectionsForCenter(center, scenarioData.connections());

        if (outgoingConnections.isEmpty())
            return;

        Set<String> visitedCenters = new HashSet<>();
        visitedCenters.add(center.getId());

        Connection selectedConnection = AlgorithmUtils.selectNextConnection(
                center,
                outgoingConnections,
                centerBuffers,
                centerWorkers,
                visitedCenters,
                scenarioData
        );

        centerBuffers.get(selectedConnection.toCenter().getId()).offer(detail);
    }

    /**
     * Records the current simulation state into the results list.
     */
    public void recordResults() {
        for (ProductionCenter center : scenarioData.centers()) {
            resultList.add(
                    new SimulationResult(
                            currentTime,
                            center.getName(),
                            centerWorkers.get(center.getId()),
                            centerBuffers.get(center.getId()).size()
                    )
            );
        }
    }

    /**
     * Retrieves the list of simulation results.
     *
     * @return the list of results
     */
    public List<SimulationResult> getResults() {
        return resultList;
    }
}

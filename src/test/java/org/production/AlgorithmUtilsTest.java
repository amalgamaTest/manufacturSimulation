package org.production;

import org.junit.jupiter.api.*;
import org.production.models.Connection;
import org.production.models.ProductionCenter;
import org.production.models.ScenarioData;
import org.production.service.AlgorithmUtils;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

class AlgorithmUtilsTest {

    private List<ProductionCenter> productionCenters;
    Map<String, BlockingQueue<String>> centerBuffers;
    Map<String, Integer> centerWorkers;
    ScenarioData scenarioData;
    private List<Connection> connections;

    @BeforeEach
    void setUp() {
        productionCenters = new ArrayList<>();
        productionCenters.add(new ProductionCenter("1", "Center 1", 2, 1.5));
        productionCenters.add(new ProductionCenter("2", "Center 2", 2, 2.1));
        productionCenters.add(new ProductionCenter("3", "Center 3", 2, 1.7));
        productionCenters.add(new ProductionCenter("4", "Center 4", 3, 2.5));


        centerBuffers = new HashMap<>();
        for (ProductionCenter center : productionCenters) {
            BlockingQueue<String> buffer = new LinkedBlockingQueue<>();
            buffer.offer("Detail 1");
            buffer.offer("Detail 2");
            buffer.offer("Detail 3");
            buffer.offer("Detail 4");
            centerBuffers.put(center.getId(), buffer);
        }


        centerWorkers = new HashMap<>();
        for (ProductionCenter center : productionCenters) {
            centerWorkers.put(center.getId(), 0);
        }

        connections = new ArrayList<>();
        connections.add(new Connection(productionCenters.get(0), productionCenters.get(1)));
        connections.add(new Connection(productionCenters.get(0), productionCenters.get(2)));
        connections.add(new Connection(productionCenters.get(1), productionCenters.get(3)));
        connections.add(new Connection(productionCenters.get(2), productionCenters.get(3)));

        scenarioData = new ScenarioData(
                productionCenters,
                connections,
                7,
                14,
                "1",
                "4");
    }

    @Test
    void testRedistributeWorkersMore() {
        AlgorithmUtils.redistributeWorkersMore(centerWorkers, productionCenters, centerBuffers, scenarioData.workersCount());

        int totalWorkers = centerWorkers.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(scenarioData.workersCount(),
                totalWorkers,
                "The total number of assigned workers cannot exceed the number of available workers.");

        for (ProductionCenter center : productionCenters) {
            int assignedWorkers = centerWorkers.get(center.getId());
            int bufferSize = centerBuffers.get(center.getId()).size();
            int maxWorkers = center.getMaxWorkers();

            assertTrue(assignedWorkers <= Math.min(bufferSize, maxWorkers),
                    "Assigned workers should not exceed buffer size or max workers for the center.");
        }
    }


    @Test
    void testRedistributeWorkersLess() {

        AlgorithmUtils.redistributeWorkersLess(centerWorkers, productionCenters, centerBuffers, scenarioData);


        int totalWorkers = centerWorkers.values().stream().mapToInt(Integer::intValue).sum();
        assertTrue(totalWorkers <= scenarioData.workersCount(),
                "The total number of assigned workers cannot exceed the number of available workers.");

        for (String centerId : centerWorkers.keySet()) {
            int assignetWorkers = centerWorkers.get(centerId);
            BlockingQueue<String> buffer = centerBuffers.get(centerId);

            if (buffer.isEmpty()) {
                assertEquals(0, assignetWorkers,
                        "A center without details should not receive workers");
            }
            assertTrue(assignetWorkers <= buffer.size(),
                    "The number of workers should not exceed the buffer size");
        }
    }

    @Test
    void testSelectNextConnection() {
        Set<String> visitedCenters = new HashSet<>();

        Connection selected = AlgorithmUtils.selectNextConnection(
                productionCenters.get(0),
                connections,
                centerBuffers,
                centerWorkers,
                visitedCenters,
                scenarioData
        );

        assertNotNull(selected, "A connection must be selected");

        ProductionCenter toCenter = selected.toCenter();
        assertTrue(centerBuffers.containsKey(toCenter.getId()),
                "The destination must exist in the buffers");
    }


    @Test
    void testAdjustExcessWorkers() {
        centerWorkers.put("1", 5);
        centerWorkers.put("2", 3);
        centerWorkers.put("3", 2);

        AlgorithmUtils.adjustExcessWorkers(centerWorkers, 4); // Убираем 4 работников

        int totalWorkers = centerWorkers.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(6, totalWorkers, "The total number of employees should be reduced to 6");

        assertTrue(centerWorkers.get("1") <= 3,
                "Excess workers should be removed from the busiest centers first.");
    }

    @Test
    void testRedistributeWorkersPerformance() {

        List<ProductionCenter> largeCenters = new ArrayList<>();
        Map<String, BlockingQueue<String>> largeBuffers = new HashMap<>();
        Map<String, Integer> largeWorkers = new HashMap<>();

        for (int i = 1; i <= 100; i++) {
            ProductionCenter center = new ProductionCenter(String.valueOf(i), "Center " + i, 10, i % 5 + 1.0);
            largeCenters.add(center);
            BlockingQueue<String> buffer = new LinkedBlockingQueue<>();
            for (int j = 0; j < 20; j++) {
                buffer.offer("Detail " + j);
            }
            largeBuffers.put(center.getId(), buffer);
            largeWorkers.put(center.getId(), 0);
        }

        ScenarioData largeScenario = new ScenarioData(
                largeCenters,
                connections,
                500,
                2000,
                "1",
                "100");

        AlgorithmUtils.redistributeWorkers(largeWorkers, largeBuffers, largeScenario);

        int totalAssigned = largeWorkers.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(500, totalAssigned, "All 500 workers must be distributed correctly.");

        for (ProductionCenter center : largeCenters) {
            int assignedWorkers = largeWorkers.get(center.getId());
            assertTrue(assignedWorkers <= center.getMaxWorkers(),
                    "The number of workers assigned must not exceed the maximum number for the center.");
        }
    }
}

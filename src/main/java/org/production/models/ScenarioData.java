package org.production.models;

import java.util.List;

public record ScenarioData(List<ProductionCenter> centers, List<Connection> connections, int workersCount,
                           int detailsCount, String startCenterId, String endCenterId) {

}

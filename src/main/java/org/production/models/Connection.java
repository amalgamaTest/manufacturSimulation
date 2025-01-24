package org.production.models;

public record Connection(ProductionCenter fromCenter, ProductionCenter toCenter) {

    @Override
    public String toString() {
        return "Connection{" +
                "fromCenter=" + fromCenter.getName() +
                ", toCenter=" + toCenter.getName() +
                '}';
    }
}
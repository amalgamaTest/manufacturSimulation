package org.production;

import org.production.io.CsvWritter;
import org.production.io.ExcelReader;
import org.production.models.ScenarioData;
import org.production.service.SimulationRunner;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter the path to the input Excel file:");
        String inputFilePath = scanner.nextLine();

        System.out.println("Enter the path for the output CSV file:");
        String outputFilePath = scanner.nextLine();

        try {
            ScenarioData scenarioData = ExcelReader.collectData(inputFilePath);
            SimulationRunner simulationRunner = new SimulationRunner(scenarioData);

            simulationRunner.runSimulation();

            CsvWritter.writeResults(outputFilePath, simulationRunner.getResults());
            System.out.println("The simulation was successfully completed. The results are written to: " + outputFilePath);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

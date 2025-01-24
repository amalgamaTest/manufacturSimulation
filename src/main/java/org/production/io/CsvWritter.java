package org.production.io;

import org.production.models.SimulationResult;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvWritter {

    public static void writeResults(String filePath, List<SimulationResult> results) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))){

            writer.write("Time, ProductionCenter, WorkersCount, BufferCount");
            writer.newLine();


            for(SimulationResult result : results)
            {
                writer.write(result.toString());
                writer.newLine();
            }
        }
    }


}

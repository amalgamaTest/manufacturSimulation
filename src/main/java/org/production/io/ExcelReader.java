package org.production.io;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.production.models.ScenarioData;
import org.production.models.Connection;
import org.production.models.ProductionCenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ExcelReader {

    public static ScenarioData collectData(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet scenarioSheet = workbook.getSheet("Scenario");
            if (scenarioSheet == null) {
                throw new RuntimeException("Sheet 'Scenario' not found in Excel.");
            }
            int workersCount = extractColumnNumericValue(scenarioSheet, "workersCount");
            int detailsCount = extractColumnNumericValue(scenarioSheet, "detailsCount");

            Sheet pcSheet = workbook.getSheet("ProductionCenter");
            if (pcSheet == null) {
                throw new RuntimeException("Sheet 'ProductionCenter' not found in Excel.");
            }
            List<ProductionCenter> centers = readProductionCenters(pcSheet);

            Sheet connSheet = workbook.getSheet("Connection");
            if (connSheet == null) {
                throw new RuntimeException("Sheet 'Connection' not found in Excel.");
            }
            List<Connection> connections = readConnections(connSheet, centers);

            String startCenterId = findStartCenterId(connections);
            System.out.println("Defined Start Center ID: " + startCenterId);
            String endCenterId = findEndCenterId(connections);

            return new ScenarioData(centers, connections, workersCount, detailsCount, startCenterId, endCenterId);
        }
    }


    private static List<ProductionCenter> readProductionCenters(Sheet sheet) {
        System.out.println("=== Reading data from the ProductionCenter sheet ===");
        List<ProductionCenter> result = new ArrayList<>();
        int headerRowIndex = findHeaderRow(sheet, "id");

        for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            String id = getCellValueAsString(row.getCell(0));
            String name = getCellValueAsString(row.getCell(1));
            double performance = getNumericValue(row.getCell(2));
            int maxWorkers = (int) getNumericValue(row.getCell(3));


            ProductionCenter pc = new ProductionCenter(id, name, maxWorkers, performance);
            result.add(pc);
            System.out.println("Read center: " + pc);
        }
        return result;
    }

    private static List<Connection> readConnections(Sheet sheet, List<ProductionCenter> centers) {
        List<Connection> connections = new ArrayList<>();
        int headerRowIndex = findHeaderRow(sheet, "sourceCenter");

        for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            String sourceId = getCellValueAsString(row.getCell(0));
            String destId = getCellValueAsString(row.getCell(1));

            ProductionCenter fromCenter = findCenterById(centers, sourceId);
            ProductionCenter toCenter = findCenterById(centers, destId);

            if (fromCenter != null && toCenter != null) {
                Connection conn = new Connection(fromCenter, toCenter);
                connections.add(conn);
            }
        }
        return connections;
    }

    private static String findStartCenterId(List<Connection> connections) {
        Set<String> sourceCenters = new HashSet<>();
        Set<String> destCenters = new HashSet<>();

        for (Connection conn : connections) {
            sourceCenters.add(conn.fromCenter().getId());
            destCenters.add(conn.toCenter().getId());
        }

        for (String source : sourceCenters) {
            if (!destCenters.contains(source.trim())) {
                return source.trim();
            }
        }
        throw new RuntimeException("Unable to find starting center.");
    }


    private static String findEndCenterId(List<Connection> connections) {
        Set<String> sourceCenters = new HashSet<>();
        Set<String> destCenters = new HashSet<>();

        for (Connection conn : connections) {
            sourceCenters.add(conn.fromCenter().getId());
            destCenters.add(conn.toCenter().getId());
        }

        for (String dest : destCenters) {
            if (!sourceCenters.contains(dest)) {
                return dest;
            }
        }
        throw new RuntimeException("Unable to find final center.");
    }

    private static int findHeaderRow(Sheet sheet, String key) {
        for (Row row : sheet) {
            if (row == null) continue;
            for (Cell cell : row) {
                if (cell != null && getCellValueAsString(cell).equalsIgnoreCase(key)) {
                    return row.getRowNum();
                }
            }
        }
        return -1;
    }

    private static boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private static double getNumericValue(Cell cell) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            return 0;
        }
        return cell.getNumericCellValue();
    }

    private static ProductionCenter findCenterById(List<ProductionCenter> centers, String id) {
        for (ProductionCenter pc : centers) {
            if (pc.getId().equalsIgnoreCase(id.trim())) {
                return pc;
            }
        }
        return null;
    }

    private static int extractColumnNumericValue(Sheet sheet, String key) {
        int headerRowIndex = findHeaderRow(sheet, key);
        if (headerRowIndex < 0) {
            throw new RuntimeException("No header line found with key: " + key + " on sheet " + sheet.getSheetName());
        }

        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            throw new RuntimeException("The header row is empty (row=" + headerRowIndex + "), key=" + key);
        }

        int columnIndex = -1;
        for (Cell cell : headerRow) {
            if (getCellValueAsString(cell).equalsIgnoreCase(key)) {
                columnIndex = cell.getColumnIndex();
                break;
            }
        }

        if (columnIndex < 0) {
            throw new RuntimeException("Column not fund for keys" + key + " on group " + sheet.getSheetName());
        }

        for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) continue;
            Cell cell = row.getCell(columnIndex);
            if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
        }

        throw new RuntimeException("No numeric value found for key " + key + " on sheet" + sheet.getSheetName());
    }

}

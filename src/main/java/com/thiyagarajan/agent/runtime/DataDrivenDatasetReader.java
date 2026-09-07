package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads data-driven rows from JSON or CSV and validates dataset structure. */
public final class DataDrivenDatasetReader {
    private final ObjectMapper mapper;

    public DataDrivenDatasetReader(ObjectMapper mapper) {
        if (mapper == null) throw new IllegalArgumentException("ObjectMapper is required");
        this.mapper = mapper;
    }

    public List<Map<String, String>> read(Path file) throws Exception {
        if (file == null || !Files.isRegularFile(file)) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Data file not found: " + file);
        String name = file.getFileName().toString().toLowerCase();
        List<Map<String, String>> rows = name.endsWith(".csv") ? readCsv(file) : readJson(file);
        validateRows(rows, file);
        return rows;
    }

    private List<Map<String, String>> readJson(Path file) throws Exception {
        JsonNode root;
        try { root = mapper.readTree(Files.readString(file)); }
        catch (Exception e) { throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Invalid JSON dataset: " + file, e); }
        JsonNode node = root.isArray() ? root : root.get("rows");
        if (node == null || !node.isArray()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "JSON dataset must be an array or {\"rows\": [...]}");
        List<Map<String, String>> rows = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isObject()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Every JSON data row must be an object");
            Map<String, String> row = new LinkedHashMap<>();
            item.fields().forEachRemaining(e -> row.put(e.getKey(), e.getValue().isNull() ? "" : e.getValue().asText()));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, String>> readCsv(Path file) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file); CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).setTrim(false).build().parse(reader)) {
            List<String> headers = parser.getHeaderNames();
            if (headers.isEmpty()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "CSV dataset must contain a header row");
            validateHeaders(headers, file);
            for (CSVRecord record : parser) {
                if (record.size() != headers.size()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "CSV row " + record.getRecordNumber() + " has " + record.size() + " columns; expected " + headers.size());
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) row.put(headers.get(i), record.get(i));
                rows.add(row);
            }
        }
        return rows;
    }

    private void validateHeaders(List<String> headers, Path file) {
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (String header : headers) {
            if (header == null || header.isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "CSV contains a blank header: " + file);
            if (!seen.add(header)) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "CSV contains duplicate header: " + header);
        }
    }

    private void validateRows(List<Map<String, String>> rows, Path file) {
        if (rows.isEmpty()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Data file contains no rows: " + file);
        for (Map<String, String> row : rows) {
            if (row.isEmpty()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Data row contains no columns: " + file);
            for (String key : row.keySet()) if (key == null || key.isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Data row contains a blank column name: " + file);
        }
    }
}

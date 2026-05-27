package com.dentistrybot.shared.service;

import com.dentistrybot.shared.model.AnswerOption;
import com.dentistrybot.shared.model.Question;
import com.dentistrybot.shared.repository.TestRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final TestRepository testRepository;

    public ImportService(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    /**
     * Imports questions from an Excel (.xlsx) file.
     * Expected columns: Question | Points | Option A | Option B | Option C | Option D | [Option E] | Correct Answer
     */
    public int importQuestionsFromExcel(String filePath, int testId) throws Exception {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                throw new IllegalArgumentException("File must have at least 2 rows (header + data)");
            }

            Row headerRow = sheet.getRow(0);
            boolean hasFiveOptions = headerRow != null && headerRow.getLastCellNum() >= 8;

            int orderNum = testRepository.countQuestions(testId);
            int imported = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String questionText = cellString(row, 0);
                if (questionText.isEmpty()) continue;

                int points = 1;
                String pointsStr = cellString(row, 1);
                if (!pointsStr.isEmpty()) {
                    try { points = Integer.parseInt(pointsStr); } catch (NumberFormatException ignored) {}
                }

                String optA = cellString(row, 2);
                String optB = cellString(row, 3);
                if (optA.isEmpty() || optB.isEmpty()) continue;

                String optC = cellString(row, 4);
                String optD = cellString(row, 5);
                String optE = "";
                String correct;

                if (hasFiveOptions) {
                    optE = cellString(row, 6);
                    correct = cellString(row, 7).toUpperCase();
                } else {
                    correct = cellString(row, 6).toUpperCase();
                }

                orderNum++;
                Question q = new Question();
                q.setTestId(testId);
                q.setQuestionText(questionText);
                q.setPoints(points);
                q.setOrderNum(orderNum);
                testRepository.createQuestion(q);

                createOptions(q.getId(), optA, optB, optC, optD, optE, correct);
                imported++;
            }

            if (imported > 0) {
                testRepository.updateTestTotalPoints(testId);
            }
            return imported;
        }
    }

    /**
     * Imports questions from a CSV file (comma-separated, same column order as Excel).
     */
    public int importQuestionsFromCsv(String filePath, int testId) throws Exception {
        List<String[]> rows = parseCsv(filePath);
        if (rows.size() < 2) {
            throw new IllegalArgumentException("File must have at least 2 rows (header + data)");
        }

        boolean hasFiveOptions = rows.get(0).length >= 8;
        int orderNum = testRepository.countQuestions(testId);
        int imported = 0;

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < 6) continue;

            String questionText = row[0].trim();
            if (questionText.isEmpty()) continue;

            int points = 1;
            if (row.length > 1 && !row[1].trim().isEmpty()) {
                try { points = Integer.parseInt(row[1].trim()); } catch (NumberFormatException ignored) {}
            }

            String optA = row.length > 2 ? row[2].trim() : "";
            String optB = row.length > 3 ? row[3].trim() : "";
            if (optA.isEmpty() || optB.isEmpty()) continue;

            String optC = row.length > 4 ? row[4].trim() : "";
            String optD = row.length > 5 ? row[5].trim() : "";
            String optE = "";
            String correct;

            if (hasFiveOptions) {
                optE = row.length > 6 ? row[6].trim() : "";
                correct = row.length > 7 ? row[7].trim().toUpperCase() : "";
            } else {
                correct = row.length > 6 ? row[6].trim().toUpperCase() : "";
            }

            orderNum++;
            Question q = new Question();
            q.setTestId(testId);
            q.setQuestionText(questionText);
            q.setPoints(points);
            q.setOrderNum(orderNum);
            testRepository.createQuestion(q);

            createOptions(q.getId(), optA, optB, optC, optD, optE, correct);
            imported++;
        }

        if (imported > 0) {
            testRepository.updateTestTotalPoints(testId);
        }
        return imported;
    }

    /**
     * Generates a sample Excel template for question import and writes it to filePath.
     */
    public void generateQuestionTemplate(String filePath) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Questions");

            Row header = sheet.createRow(0);
            String[] headers = {"Savol", "Ball", "A varianti", "B varianti", "C varianti", "D varianti", "E varianti", "To'g'ri javob"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Object[][] samples = {
                {"Tishning tashqi qavati qanday nomlanadi?", 1, "Dentin", "Emal", "Sement", "Pulpa", "Tsement", "B"},
                {"Odamda nechta doimiy tish bo'ladi?", 1, "28", "30", "32", "34", "36", "C"},
                {"Kariyes asosiy sababi nima?", 2, "Vitamin yetishmovchiligi", "Bakteriyalar", "Genetik omillar", "Suv yetishmovchiligi", "Irsiyat", "B"},
            };
            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < samples[r].length; c++) {
                    Object val = samples[r][c];
                    Cell cell = row.createCell(c);
                    if (val instanceof Number n) cell.setCellValue(n.doubleValue());
                    else cell.setCellValue(val.toString());
                }
            }

            sheet.setColumnWidth(0, 50 * 256);
            sheet.setColumnWidth(1, 8 * 256);
            for (int c = 2; c <= 6; c++) sheet.setColumnWidth(c, 25 * 256);
            sheet.setColumnWidth(7, 15 * 256);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                wb.write(fos);
            }
        }
    }

    /**
     * Downloads a Telegram file by URL to the given destination path.
     */
    public static void downloadTelegramFile(String fileUrl, String destPath) throws Exception {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(fileUrl))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Telegram file download failed: HTTP " + response.statusCode());
        }
        try (InputStream in = response.body()) {
            Files.copy(in, Path.of(destPath));
        }
    }

    private void createOptions(int questionId, String a, String b, String c, String d, String e, String correct) {
        record Opt(String text, String letter, int order) {}
        List<Opt> opts = List.of(
            new Opt(a, "A", 1), new Opt(b, "B", 2), new Opt(c, "C", 3),
            new Opt(d, "D", 4), new Opt(e, "E", 5)
        );
        for (Opt opt : opts) {
            if (opt.text().isEmpty()) continue;
            AnswerOption ao = new AnswerOption();
            ao.setQuestionId(questionId);
            ao.setOptionText(opt.text());
            ao.setCorrect(opt.letter().equals(correct));
            ao.setOrderNum(opt.order());
            testRepository.createAnswerOption(ao);
        }
    }

    private static String cellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield (v == Math.floor(v)) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private static List<String[]> parseCsv(String filePath) throws IOException {
        List<String[]> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.add(line.split(",", -1));
            }
        }
        return result;
    }
}

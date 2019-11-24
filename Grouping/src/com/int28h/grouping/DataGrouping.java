package com.int28h.grouping;

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;

public class DataGrouping {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.##");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMAT.setDecimalFormatSymbols(symbols);
    }

    /**
     * Структура для хранения значений входных параметров
     */
    private static class Parameters {
        public final String outputFilenameDates;
        public final String outputFilenameOffices;
        public final String[] inputFilenames;

        private Parameters(String outputFilenameDates, String outputFilenameOffices, String[] inputFilenames) {
            this.outputFilenameDates = outputFilenameDates;
            this.outputFilenameOffices = outputFilenameOffices;
            this.inputFilenames = inputFilenames;
        }
    }

    /**
     * Парсинг массива входных параметров
     *
     * @param args массив входных параметров
     * @return класс Parameters с заполненными полями
     */
    private static Parameters parseParametersFromCommandLine(String[] args) {
        if (args.length < 3) {
            throw new IllegalStateException("Недостаточное количество входных параметров.\n" +
                    "Использование: stats-dates.txt stats-offices.txt input.txt [input.txt...]\n" +
                    "Где stats-dates.txt - файл для вывода статистики по датам,\n" +
                    "stats-offices.txt - файл для вывода статистики по точкам продаж,\n" +
                    "input.txt [input.txt...] - список входных файлов."
            );
        }
        String outputFilenameDates = args[0];
        String outputFilenameOffices = args[1];

        String[] inputFilenames = Arrays.copyOfRange(args, 2, args.length);

        return new Parameters(outputFilenameDates, outputFilenameOffices, inputFilenames);
    }

    /**
     * Структура для хранения мап с нужными данными
     */
    private static class Data {
        public final HashMap<LocalDate, Double> statsDates;
        public final HashMap<String, Double> statsOffices;

        public Data(HashMap<LocalDate, Double> statsDates, HashMap<String, Double> statsOffices) {
            this.statsDates = statsDates;
            this.statsOffices = statsOffices;
        }
    }

    /**
     * Парсинг входных файлов и создание объекта Data с двумя мапами дата-сумма и точка-сумма
     *
     * @param inputFilenames список имён входных файлов
     * @return объект Data с заполненными мапами дата-сумма и точка-сумма
     */
    private static Data readData(String[] inputFilenames) {
        HashMap<LocalDate, Double> statsDates = new HashMap<>();
        HashMap<String, Double> statsOffices = new HashMap<>();

        for (String inputFilename : inputFilenames) {
            try (BufferedReader br = new BufferedReader(new FileReader(inputFilename))) {
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    String[] substrings = line.split(" ");

                    LocalDate date = LocalDate.parse(substrings[0]);
                    String office = substrings[2];
                    Double sum = Double.parseDouble(substrings[4]);

                    statsDates.put(date, statsDates.getOrDefault(date, 0.0) + sum);
                    statsOffices.put(office, statsOffices.getOrDefault(office, 0.0) + sum);
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return new Data(statsDates, statsOffices);
    }

    public static void main(String[] args) {
        Parameters parameters = parseParametersFromCommandLine(args);

        Data data = readData(parameters.inputFilenames);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(parameters.outputFilenameDates))) {
            data.statsDates
                    .entrySet()
                    .stream()
                    .sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey()))
                    .forEach(entry -> {
                        try {
                            bw.append(entry.getKey().toString())
                                    .append(' ')
                                    .append(DECIMAL_FORMAT.format(entry.getValue()))
                                    .append("\n");
                        } catch (IOException e) {
                            throw new IllegalStateException("Не удалось записать в файл.");
                        }
                    });
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось записать в файл.");
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(parameters.outputFilenameOffices))) {
            data.statsOffices
                    .entrySet()
                    .stream()
                    .sorted((o1, o2) -> o2.getValue().compareTo(o1.getValue()))
                    .forEach(entry -> {
                        try {
                            bw.append(entry.getKey())
                                    .append(' ')
                                    .append(DECIMAL_FORMAT.format(entry.getValue()))
                                    .append("\n");
                        } catch (IOException e) {
                            throw new IllegalStateException("Не удалось записать в файл.");
                        }
                    });
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось записать в файл.");
        }
    }
}

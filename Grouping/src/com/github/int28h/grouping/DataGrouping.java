package com.github.int28h.grouping;

import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataGrouping {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.##");

    private static ExecutorService executorService = Executors.newFixedThreadPool(2);

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMAT.setDecimalFormatSymbols(symbols);
    }

    /**
     * Структура для хранения значений входных параметров
     */
    private static class Parameters {
        final String outputFilenameDates;
        final String outputFilenameOffices;
        final String[] inputFilenames;

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
        final HashMap<LocalDate, BigDecimal> statsDates;
        final HashMap<String, BigDecimal> statsOffices;

        Data(HashMap<LocalDate, BigDecimal> statsDates, HashMap<String, BigDecimal> statsOffices) {
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
        HashMap<LocalDate, BigDecimal> statsDates = new HashMap<>();
        HashMap<String, BigDecimal> statsOffices = new HashMap<>();

        for (String inputFilename : inputFilenames) {
            try (BufferedReader br = new BufferedReader(new FileReader(inputFilename))) {
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    String[] substrings = line.split(" ");

                    try {
                        LocalDate date = LocalDate.parse(substrings[0]);
                        String office = substrings[2];
                        BigDecimal sum = new BigDecimal(substrings[4]);

                        statsDates.merge(date, sum, BigDecimal::add);
                        statsOffices.merge(office, sum, BigDecimal::add);
                    } catch (IndexOutOfBoundsException | NumberFormatException | DateTimeParseException e) {
                        throw new IllegalStateException("Ошибка обработки входных файлов - в файле " + inputFilename + " некорректные данные в строке " + line);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Ошибка обработки входных файлов.");
            }
        }
        return new Data(statsDates, statsOffices);
    }

    /**
     * Задача для исполнения в отдельном потоке - группировка по датам
     */
    private static class DatesTask implements Runnable {
        private Data data;
        private Parameters parameters;

        DatesTask(Data data, Parameters parameters){
            this.data = data;
            this.parameters = parameters;
        }

        @Override
        public void run() {
            System.out.println("DatesTask " + Thread.currentThread().getName());

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(parameters.outputFilenameDates))) {
                data.statsDates
                        .entrySet()
                        .stream()
                        .sorted(Comparator.comparing(Map.Entry::getKey))
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
        }
    }

    /**
     * Задача для исполнения в отдельном потоке - группировка по офисам
     */
    private static class OfficesTask implements Runnable {
        private Data data;
        private Parameters parameters;

        OfficesTask(Data data, Parameters parameters){
            this.data = data;
            this.parameters = parameters;
        }

        @Override
        public void run() {
            System.out.println("OfficesTask " + Thread.currentThread().getName());
            
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

    public static void main(String[] args) {
        Parameters parameters = parseParametersFromCommandLine(args);

        Data data = readData(parameters.inputFilenames);

        executorService.execute(new DatesTask(data, parameters));

        executorService.execute(new OfficesTask(data, parameters));
    }
}

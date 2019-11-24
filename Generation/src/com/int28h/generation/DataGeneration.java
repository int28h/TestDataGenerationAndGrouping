package com.int28h.generation;

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class DataGeneration {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC+00:00"));
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.##");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMAT.setDecimalFormatSymbols(symbols);
    }

    /**
     * Возвращает случайные дату и время в пределах некоторого временного интервала
     *
     * @param startDate начало временного интервала
     * @param endDate   конец временного интервала
     * @return случайная дата в интервале между входными датами
     */
    public static Instant randomDate(Instant startDate, Instant endDate) {
        long startSeconds = startDate.getEpochSecond();
        long endSeconds = endDate.getEpochSecond();
        long random = ThreadLocalRandom.current().nextLong(startSeconds, endSeconds);

        return Instant.ofEpochSecond(random);
    }

    /**
     * Считывание списка торговых точек из файла.
     *
     * @param inputFilename имя файла со списком торговых точек
     * @return список торговых точек
     */
    private static ArrayList<String> readOffices(String inputFilename) {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilename))) {
            return (ArrayList<String>) br.lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Структура для хранения значений входных параметров
     */
    private static class Parameters {
        public final String inputFilename;
        public final int operationsCount;
        public final String[] outputFilenames;

        private Parameters(String inputFilename, int operationsCount, String[] outputFilenames) {
            this.inputFilename = inputFilename;
            this.operationsCount = operationsCount;
            this.outputFilenames = outputFilenames;
        }
    }

    /**
     * Парсинг входных параметров
     *
     * @param args массив входных параметров
     * @return класс Parameters с заполненными полями
     */
    private static Parameters parseParametersFromCommandLine(String[] args) {
        if (args.length < 3) {
            throw new IllegalStateException("Недостаточное количество входных параметров.\n" +
                    "Использование: input.txt count output.txt [output.txt...]\n" +
                    "Где input.txt - заранее подготовленный список точек продаж,\n" +
                    "count - число операций,\n" +
                    "output.txt [output.txt...] - список файлов для вывода данных."
            );
        }
        String inputFilename = args[0];

        int operationsCount = 0;
        try {
            operationsCount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Второй параметр - количество операций - должен быть целочисленного типа.");
        }

        String[] outputFilenames = Arrays.copyOfRange(args, 2, args.length);

        return new Parameters(inputFilename, operationsCount, outputFilenames);
    }

    /**
     * Итоговая запись данных в файл
     * @param offices список точек продаж
     * @param startDate начало временного отрезка для генерации случайных даты-времени
     * @param endDate конец временного отрезка для генерации случайных даты-времени
     * @param countPerFile число операций для записи в файл
     * @param counter счётчик операций
     * @param bw
     * @return
     */
    private static int writeData(List<String> offices, Instant startDate, Instant endDate, int countPerFile, int counter, BufferedWriter bw) {
        Random random = ThreadLocalRandom.current();
        for (int j = 0; j < countPerFile; j++) {
            String randomDate = formatter.format(randomDate(startDate, endDate));
            try {
                bw.append(randomDate)
                        .append(' ')
                        .append(offices.get(random.nextInt(offices.size())))
                        .append(' ')
                        .append(String.valueOf(counter++))
                        .append(' ')
                        .append(DECIMAL_FORMAT.format(randomDoubleInRange(random)))
                        .append('\n');
            } catch (IOException e){
                throw new IllegalStateException("Не удалось записать в файл.");
            }
        }
        return counter;
    }

    /**
     * Возвращает случайное double-число в промежутке от 10000.12 до 100000.50
     * @param random
     * @return
     */
    private static double randomDoubleInRange(Random random){
        return 10000.12 + (100000.50 - 10000.12) * random.nextDouble();
    }

    public static void main(String[] args) {
        Parameters parameters = parseParametersFromCommandLine(args);
        ArrayList<String> offices = readOffices(parameters.inputFilename);

        //todo вынести это куда-то
        //region формирование интервала для генерации рандомных даты/времени
        LocalDateTime endDate = LocalDateTime.now()
                .withDayOfYear(1)
                .with(LocalTime.of(0, 0, 0, 0));
        LocalDateTime startDate = endDate.minusYears(1);
        Instant startInstant = startDate.toInstant(ZoneOffset.UTC);
        Instant endInstant = endDate.toInstant(ZoneOffset.UTC);
        //endregion

        //region разбиение суммарного числа операций на части
        int countPerFile = parameters.operationsCount / parameters.outputFilenames.length;
        int countRemaining = parameters.operationsCount % parameters.outputFilenames.length;
        //endregion

        int counter = 1;

        Random random = new Random();

        for (int i = 0; i < parameters.outputFilenames.length; i++) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(parameters.outputFilenames[i]))) {
                counter = writeData(offices, startInstant, endInstant, countPerFile, counter, bw);

                if (i == parameters.outputFilenames.length - 1) {
                    writeData(offices, startInstant, endInstant, countRemaining, counter, bw);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Не удалось записать операцию в файл.");
            }
        }
    }
}

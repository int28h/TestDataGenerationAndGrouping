[DataGeneration.java](Generation/src/com/int28h/generation/DataGeneration.java)  
  
Программа создает текстовые файлы, в которых содержатся:  
- **дата и время** (в формате `YYYY-MM-DD HH:MM:SS`)  
Дата и время выбираются случайно в диапазоне за предыдущий год — например, если программа запущена 02.06.2018, то время операции будет в интервале от 01.01.2017 00:00 до 01.01.2018 00:00.  
- **номер точки продаж**  
Номер точки продаж случайным образом выбирается из заранее подготовленного [списка](offices.txt).  
- **номер операции**  
Номер по порядку.  
- **сумма операции**  
Случайное значение в диапазоне от 10 000.12 до 100 000.50 рублей.  
  
В качестве входных параметров программе передаются имя файла со списком точек продаж, количество операций и имена файлов, куда будут записаны сгенерированные данные.  
Программа записывает столько случайных операций, сколько указано во втором параметре.  
  
Пример запуска программы:  
`java -jar task1.jar offices.txt 90000 ops1.txt ops2.txt ops3.txt`  
  
После запуска такой команды получаются 3 файла (ops1.txt, ops2.txt и ops3.txt), в которых всего 90 000 платежей.  
  
***
  
[DataGrouping.java](Grouping/src/com/int28h/grouping/DataGrouping.java)  
  
Программа считает статистику по операциям, данные о которых берутся из файлов, сгенерированных в прошлой задаче.  
  
Подсчитываются:  
- сумма всех операций за каждый день (сортировка - по возрастанию даты)  
- сумма всех операций в каждой точке продаж (сортировка - по убыванию суммы)  
  
В качестве входных параметров программе передаются имя файла со статистикой по датам, имя файла со статистикой по точкам продаж и имена файлов с операциями.  
   
Пример запуска программы:  
`java -jar task2.jar stats-dates.txt stats-offices.txt ops1.txt ops2.txt ops3.txt`  
  
После запуска такой команды получаются 2 файла (stats-dates.txt и stats-offices.txt), в которых сохранена подсчитанная статистика по датам и точкам продаж соответственно.  
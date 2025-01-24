# **Проект: Производственная симуляция**

## **Описание проекта**

Проект симулирует производственный процесс с учетом ограниченного количества работников и производственных центров, которые связаны друг с другом взвешенными ребрами. Алгоритм распределяет работников между центрами на основе их загрузки и производительности, оптимизируя производственный процесс. В ключевые классы проекта добавлена документация JavaDoc для упрощения понимания кода.
---

### **Шаги для запуска (после Клонирования репозитория и сборки)**

## 1. Инструкции
- Укажите путь к входному **Excel-файлу**.
- Укажите путь, где будет сохранен выходной **CSV-файл**.

## 2. Результаты симуляции
- После выполнения симуляции результаты будут записаны в указанный вами **CSV-файл**.

---

# Описание структур данных

## 1. Производственные центры (ProductionCenter)
- **Описание**: Моделируют производственные узлы. Каждый центр имеет буфер для деталей.
- **Поля**:
    - `id` — уникальный идентификатор центра.
    - `name` — название центра.
    - `maxWorkers` — максимальное количество работников.
    - `performance` — время обработки одной детали.
- **Используется**: Для хранения состояния и свойств каждого производственного центра.

## 2. Соединения (Connection)
- **Описание**: Представляют собой ориентированные взвешенные ребра между центрами.
- **Поля**:
    - `fromCenter` — исходный центр.
    - `toCenter` — конечный центр.
- **Используется**: Для построения графа производственного процесса.

## 3. Буферы деталей (BlockingQueue)
- **Описание**: Потокобезопасные очереди `LinkedBlockingQueue` используются для хранения деталей, ожидающих обработки.
- **Преимущества**:
    - Поддерживают блокирующие операции `offer()` и `poll()`.
    - Исключают гонки потоков в многопоточном окружении.
- **Используется**: Для хранения деталей в буферах производственных центров.

## 4. Работники (Map<String, Integer>)
- **Описание**: Карта `ConcurrentHashMap` используется для хранения текущего количества работников, прикрепленных к каждому центру.
- **Преимущества**: Обеспечивает потокобезопасный доступ в условиях многопоточности.

---

# Ключевые классы

Работа программы организована через следующие основные модули:

## 1. Main.java
- Точка входа в приложение. Отвечает за:
    - Ввод данных.
    - Запуск симуляции.
    - Запись результатов.

## 2. SimulationRunner.java
- Основная логика симуляции:
    - Управление распределением работников.
    - Параллельная обработка деталей.
    - Запись результатов симуляции.

## 3. AlgorithmUtils.java
- Реализация алгоритмов:
    - Распределение работников между центрами.
    - Выбор следующего соединения.
    - Корректировка избыточных работников.

---

# Используемые алгоритмы

## 1. Алгоритм Дейкстры
- **Используется**: В методе `selectNextConnection` для выбора следующего центра на основе минимального веса соединений.
- **Описание**:
    - Рассчитывается вес соединений на основе:
        - Размер буфера (`buffer.size()`).
        - Производительности центра (`performance`).
        - Количества текущих работников.
    - Центры с меньшим значением веса имеют приоритет для обработки деталей.

## 2. Жадный алгоритм распределения работников
- **Используется**: В методах `redistributeWorkersMore` и `redistributeWorkersLess`.
- **Описание**:
    - Центры сортируются по убыванию значений (`buffer.size() * performance`).
    - Работники назначаются начиная с наиболее загруженных центров.
    - Жадный подход минимизирует простои и гарантирует максимальное использование ресурсов.

## 3. Многопоточность
- **Используется**: В классе `SimulationRunner` для параллельной обработки деталей.
- **Описание**:
    - Применяется `ExecutorService` с фиксированным пулом потоков, равным числу доступных работников.
    - Потокобезопасные структуры данных (`ConcurrentHashMap`, `BlockingQueue`) исключают гонки потоков.
- **Преимущества**:
    - Ускоряет выполнение симуляции за счет распараллеливания задач.
    - Обеспечивает корректную работу программы даже при увеличении количества работников и центров.

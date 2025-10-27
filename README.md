# NEM12 Parser

## 1. Overview

### 1-1. Project Overview

A production-grade Kotlin-based parser for NEM12 format energy meter reading files. The parser reads NEM12 files, validates their contents, and stores meter readings and parsing failures in a database.

**Key Features:**
- **Best-Effort Parsing**: Continues processing even when individual records fail validation
- **Dual Failure Handling**: Saves failures to database AND logs them to console in real-time
- **Batch Processing**: Optimized batch inserts for high performance
- **Timezone Conversion**: Automatically converts AEST/AEDT timestamps to UTC
- **Test code**: Wrote test code for server stability.

**What it does:**
1. Reads NEM12 format files line-by-line
2. Validates each record against NEM12 specifications
3. Converts meter readings to UTC timezone
4. Stores valid readings in `meter_reading` table
5. Stores failed records in `failure_reading` table with detailed error information
6. Logs all failures to console for real-time monitoring

### 1-2. Dependencies

**Runtime Dependencies:**
- **JDK 21**: Java Development Kit
- **Kotlin**: Modern JVM language with null-safety and type inference
- **SQLite**: Embedded database for data storage

**Development Dependencies:**
- **Gradle**: Build automation tool
- **JUnit**: Testing framework
- **Ktlint**: Kotlin code style checker and formatter

### 1-3. How to Run

#### Prerequisites
- JDK 21 or higher installed
- No additional software required (SQLite is embedded)

#### Build the Project

```bash
# Clone the repository
cd nem12-parser

# Build the project (runs tests automatically)
./gradlew clean build
```

#### Run the Parser

**Basic Usage:**
```bash
java -jar build/libs/nem12-parser-1.0.0-standalone.jar <input-file> <output-database>
```

**Examples:**
```bash
# Parse a NEM12 file and store results in output.db
java -jar build/libs/nem12-parser-1.0.0-standalone.jar ./src/test/resources/sample.nem12 output.db

# Custom batch size (default: 50)
java -jar build/libs/nem12-parser-1.0.0-standalone.jar ./src/test/resources/sample.nem12 output.db --batch-size=500
```

#### Using Gradle Run Task (Development)

```bash
# Run with default test file
./gradlew run

# Run with custom arguments
./gradlew run --args="input.csv output.db"
```

#### View Results

**Query the database:**
```bash
# Open SQLite database
sqlite3 output.db

# View successful meter readings
SELECT * FROM meter_reading LIMIT 10;

# View failed records with reasons
SELECT line_number, failure_reason, nmi, raw_value
FROM failure_reading
ORDER BY line_number;
```

#### Run Tests

```bash
# Run all tests
./gradlew test
```

#### Code Quality Checks

```bash

# Auto-format code
./gradlew ktlintFormat
```

#### Output Files

After running the parser, you'll find:
- **`<output-database>.db`**: SQLite database with two tables:
    - `meter_reading`: Successfully parsed meter readings
    - `failure_reading`: Failed records with error details

#### Console Output Example

```
INFO  - Starting to parse file: sample.nem12
WARN  - Parsing failure - Line 15: NEGATIVE_VALUE (NMI: 1234567890, Interval: 5, Time: 2024-01-01T12:00, Raw: '-10.5')
INFO  - Successfully parsed 1523 lines
INFO  - Parsing completed successfully
Database created: output.db
Failed records:
  NEGATIVE_VALUE: 2
  EMPTY_VALUE: 5
  INTERVAL_COUNT_MISMATCH: 1
Failed records database: output.db
```

---

## 2. Architecture

### 2-1. Project Architecture Overview

The NEM12 Parser follows a **Layered Architecture** pattern with clear separation of concerns across three main layers:

```
┌─────────────────────────────────────────────────────────┐
│                      Main (CLI)                         │
│              - Command-line argument parsing            │
│              - Dependency injection setup               │
└────────────────────┬────────────────────────────────────┘
                     │
         ┌───────────┴───────────┐
         ▼                       ▼
┌──────────────────┐    ┌──────────────────┐
│  Failure Handler │    │  Parser Service  │
│    (Composite)   │◄───│   (NEM12Parser)  │
├──────────────────┤    ├──────────────────┤
│ - Database       │    │ - File reading   │
│ - Logging        │    │ - State machine  │
│ - (Extensible)   │    │ - Validation     │
└──────────────────┘    └────────┬─────────┘
                                 │
                    ┌────────────┴────────────┐
                    ▼                         ▼
         ┌──────────────────┐      ┌──────────────────┐
         │ Record Parser    │      │   Repository     │
         │    Service       │      │  (Data Access)   │
         ├──────────────────┤      ├──────────────────┤
         │ - Interval data  │      │ - Meter reading  │
         │ - Validation     │      │ - Failure record │
         │ - Failure notify │      │ - Batch insert   │
         └──────────────────┘      └──────────────────┘
                                            │
                                            ▼
                                   ┌──────────────────┐
                                   │  SQLite Database │
                                   │ - meter_reading  │
                                   │ - failure_reading│
                                   └──────────────────┘
```

### 2-2. Component Descriptions

#### **Layer 1: Handler (Entry Point)**

**Main.kt** - Application entry point
- Parses command-line arguments
- Creates and wires dependencies
- Orchestrates the parsing workflow
- Displays statistics and results

#### **Layer 2: Service (Business Logic)**

**NEM12ParserService** - Main parsing orchestration
- Reads NEM12 file line-by-line
- Maintains parser state
- Delegates interval data parsing to RecordParserService
- Saves valid readings to repository

**RecordParserService** - Interval data parsing and validation
- Parses 300 (interval data) records
- Checks interval count against expected count
- Notifies FailureHandler for invalid records
- Returns list of valid MeterReading objects


#### **Layer 3: Handler (Failure Processing)**

**FailureHandler (Interface)** - Defines failure handling contract

**Implementations:**
1. **DatabaseFailureHandler** - Persists failures to SQLite
2. **LoggingFailureHandler** - Logs failures to console
3. **CompositeFailureHandler** - Combines multiple handlers (Composite Pattern)

**Benefits:**
- Easy to add new handlers (e.g., EmailHandler, MetricsHandler)
- Separation of concerns
- Testability through interfaces

#### **Layer 4: Repository (Data Access)**

**BaseSQLiteRepository<T>**
- Handles batch processing
- **Provides timezone conversion utility (AEST → UTC)**
- Implements common operations

**Concrete Implementations:**

1. **MeterReadingRepositoryImpl**
    - Stores valid meter readings
    - Generates UUID for each record
    - Tracks total inserted count

2. **FailureReadingsRepositoryImpl**
    - Stores failed parsing records
    - Tracks statistics by failure reason
    - Supports nullable fields (timestamp, interval index)

### 2-3. Design Patterns Used

| Pattern | Usage | Benefit |
|---------|-------|---------|
| **Layered Architecture** | Handler-Service-Repository | Separation of concerns, testability |
| **Template Method** | BaseSQLiteRepository | Code reuse, consistent behavior |
| **Composite** | CompositeFailureHandler | Combine multiple handlers flexibly |
| **Strategy** | FailureHandler implementations | Swap handling strategies at runtime |
| **State Machine** | ParserState | Track NEM12 file structure hierarchy |
| **Dependency Injection** | Constructor injection | Loose coupling, testability |

### 2-4. Database Schema

**meter_reading table:**
```sql
CREATE TABLE meter_reading (
    id TEXT PRIMARY KEY,                    -- UUID
    nmi VARCHAR(10) NOT NULL,               -- Meter identifier
    timestamp TIMESTAMP NOT NULL,           -- UTC timestamp
    consumption NUMERIC NOT NULL,           -- Energy consumption (15.4 format)
    UNIQUE(nmi, timestamp)                  -- Prevent duplicates
);
```

**failure_reading table:**
```sql
CREATE TABLE failure_reading (
    id TEXT PRIMARY KEY,                    -- UUID
    line_number INTEGER NOT NULL,           -- Source line in input file
    nmi TEXT,                               -- Meter identifier 
    interval_index INTEGER,                 -- Interval position
    raw_value TEXT NOT NULL,                -- Original invalid value
    failure_reason TEXT NOT NULL,           -- Reason enum
    timestamp TIMESTAMP                     -- Timestamp
);
```

---

## 3. Major Decisions

### 3-1. File Reading Strategy

**Decision: Streaming (line-by-line) approach**

```kotlin
// Using BufferedReader with lineSequence()
cmd.inputPath.bufferedReader().use { reader ->
    reader.lineSequence().forEach { line ->
        parseLine(line.trim())
    }
}
```

**Why:**
- No need to load entire file into memory

### 3-2. Best-Effort Parsing

**Decision: Continue parsing even when individual records fail**

```kotlin
failureHandler.use {
    for (i in 0 until expectedIntervals) {
        if (!isValid(value)) {
            failureHandler.handleFailure(record)  // Log and continue
            continue
        }
        readings.add(validReading)
    }
}
```

**Why:**
- **Maximize data extraction** from partially corrupted files
- Better user experience (get some data vs. nothing)
- Detailed failure tracking for debugging

**Alternative considered:**
- Fail-fast approach (stop on first error)
- Rejected because: Real-world files often have isolated errors

### 3-3. Batch Insert Optimization

**Decision: Buffer records and insert in batches**

```kotlin
fun save(entity: T) {
    insertStatement.addBatch()
    batchCount++

    if (batchCount >= batchSize) {
        executeBatch()  // Execute when batch is full
    }
}
```

**Why: Performance**
- Reduces database I/O operations
- Efficient use of database connection

### 3-4. Timezone Conversion (AEST → UTC)

**Decision: Convert all timestamps to UTC before storage**

```kotlin
fun aestToUtc(timestamp: LocalDateTime): LocalDateTime {
    return timestamp.atZone(AEST)
        .withZoneSameInstant(UTC)
        .toLocalDateTime()
}
```

**Why:**
- **DST handling**: Automatically handles AEST ↔ AEDT transitions
- **International compatibility**: UTC is standard for data storage

**from Shishir**
> Input date timezone is AEST, UTC+10:00, and can be stored in the database as UTC

### 3-5. Composite Handler Pattern

**Decision: Multiple failure handlers combined via CompositeFailureHandler**

```kotlin
val databaseHandler = DatabaseFailureHandler(repository)
val loggingHandler = LoggingFailureHandler()
val compositeHandler = CompositeFailureHandler(databaseHandler, loggingHandler)
```

**Why:**
- **Flexibility**: Enable/disable handlers independently
- **Extensibility**: Easy to add new handlers (email, metrics, etc.)
- **Single Responsibility**: Each handler does one thing


---

## 4. How AI Was Used in This Project (AI Driven Development)

### 4-1. Design Phase: Architecture Planning

**Tool: Claude**

Used AI for architectural decision validation before implementation.

**Example:**
- Validated Repository and Composite patterns

### 4-2. Initial Code Implementation

**Tool: Claude Code**

AI assisted with code generation and Kotlin idioms:
- Generated BaseSQLiteRepository structure
- Implemented timezone conversion logic
- Created test scaffolding

### 4-3. Automated Code Review

**Tool: Claude Bot + GitHub Actions**

Set up AI-powered code review on pull requests.([Sample](https://github.com/ChanHHOO/flo-energy-tech-assessment/pull/16))

**Impact:** Instant feedback and validate code quality

### 4-4. NEM12 Format Analysis

**Tool: Google NotebookLM**

Analyzed NEM12 specification documents to extract requirements.

**Process:**
1. Uploaded NEM12 spec PDFs to NotebookLM
2. Asked questions about record types and validation rules
3. Generated summary of key requirements


---

## 5. Project Management

This project was managed using **GitHub Issues** and **Pull Requests** to track tasks, document decisions, and maintain code quality through systematic review processes.

### Task Management Workflow

**GitHub Issues** - Used for:
- Feature planning and requirements tracking
- Bug tracking and resolution
- Design decision documentation

**Pull Requests** - Used for:
- Code review and quality assurance
- Feature integration

### Key Practices

- **Issue-driven development**: Each feature/fix linked to a specific issue
- **Branch strategy**: Feature branches merged via PR
- **Code review**: All changes reviewed before merging
- **Automated testing**: CI/CD pipeline validates every PR
- **Clear commit messages**: Descriptive commits referencing issues

### Links

- **Closed Issues**: [View all completed tasks](https://github.com/ChanHHOO/flo-energy-tech-assessment/issues?q=is%3Aissue+state%3Aclosed)
- **Closed Pull Requests**: [View all merged changes](https://github.com/ChanHHOO/flo-energy-tech-assessment/pulls?q=is%3Apr+is%3Aclosed)


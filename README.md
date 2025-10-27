# NEM12 Parser

A Kotlin-based parser for NEM12 format energy meter reading files, generating SQL INSERT statements for the `meter_reading` database table.

## Features

- **Streaming Processing**: Handles very large files efficiently with constant memory usage
- **State Machine Pattern**: Robust parsing of hierarchical NEM12 file structure
- **Multiple Output Modes**:
  - PostgreSQL COPY (fastest, recommended)
  - Batch INSERT (standard SQL, compatible)
- **Production-Grade**: Error handling, logging, validation
- **Written in Kotlin**: Concise, type-safe, modern

## Quick Start

### Prerequisites

- JDK 11 or higher
- Gradle (or use included wrapper)

### Build

```bash
./gradlew clean build
```

This generates: `build/libs/nem12-parser-1.0.0-standalone.jar`

### Usage

```bash
# Using PostgreSQL COPY (fastest)
java -jar build/libs/nem12-parser-1.0.0-standalone.jar input.nem12 output.sql

# Using Batch INSERT
java -jar build/libs/nem12-parser-1.0.0-standalone.jar input.nem12 output.sql --mode=batch

# Custom batch size
java -jar build/libs/nem12-parser-1.0.0-standalone.jar input.nem12 output.sql --mode=batch --batch-size=500
```

### Execute SQL

```bash
# PostgreSQL
psql -d your_database -f output.sql
```

## Architecture

### Core Components

```
┌──────────────┐
│ NEM12 File   │
└──────┬───────┘
       │
       ▼
┌──────────────────┐
│  NEM12Parser     │  State Machine
│  - ParserState   │  - Tracks NMI context
│  - RecordParser  │  - Validates structure
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ SQLGenerator     │  Strategy Pattern
│ - CopyCommand    │  - PostgreSQL COPY
│ - BatchInsert    │  - Standard INSERT
└──────┬───────────┘
       │
       ▼
┌──────────────┐
│  SQL Output  │
└──────────────┘
```

### Key Classes

#### 1. Data Model

```kotlin
data class MeterReading(
    val nmi: String,
    val timestamp: LocalDateTime,
    val consumption: BigDecimal
)
```

#### 2. Parser

```kotlin
class NEM12Parser(private val sqlGenerator: SQLGenerator) {
    fun parse(filePath: Path) {
        // Stream-based line-by-line processing
        // State machine handles 100/200/300/500/900 records
    }
}
```

#### 3. SQL Generators

```kotlin
interface SQLGenerator : Closeable {
    fun addReading(reading: MeterReading)
    fun flush()
}

class CopyCommandGenerator(outputPath: Path) : SQLGenerator
class BatchInsertGenerator(outputPath: Path, batchSize: Int) : SQLGenerator
```

## Database Schema

```sql
create table meter_reading (
    id uuid default gen_random_uuid() not null,
    nmi varchar(10) not null,
    timestamp timestamp not null,
    consumption numeric not null,
    constraint meter_reading_pk primary key (id),
    constraint meter_reading_unique_consumption unique (nmi, timestamp)
);
```

## NEM12 Format Overview

| Record Type | Code | Description |
|-------------|------|-------------|
| Header | 100 | File metadata |
| NMI Data | 200 | Meter identifier and interval settings |
| Interval Data | 300 | Actual consumption readings (48 values for 30-min intervals) |
| NMI End | 500 | End of meter data block |
| File End | 900 | End of file |

### Example Transformation

**Input (NEM12):**
```
200,NEM1201009,E1E2,1,E1,N1,01009,kWh,30,20050610
300,20050301,0.461,0.810,...
```

**Output (PostgreSQL COPY):**
```sql
COPY meter_reading (nmi, timestamp, consumption) FROM STDIN WITH (FORMAT CSV);
NEM1201009,2005-03-01 00:00:00,0.461
NEM1201009,2005-03-01 00:30:00,0.810
\.
```

## Design Decisions

### Q1: Technology Rationale

**Kotlin:**
- Modern, concise syntax reduces boilerplate
- Null-safety prevents common runtime errors
- Excellent Java interoperability
- Strong type system with data classes
- Expressive DSL capabilities

**Gradle:**
- Kotlin DSL for type-safe build configuration
- Superior dependency management
- Fast incremental builds

**PostgreSQL COPY:**
- 2-5x faster than batch INSERT
- Industry standard for bulk loading
- Direct database protocol optimization

### Q2: Future Improvements

Given more time, I would add:

1. **Parallel Processing**: Chunk-based parallel parsing for multi-core utilization
2. **Progress Reporting**: Real-time progress bar for large files
3. **Data Quality Reports**: Statistics on skipped values, outliers, validation failures
4. **Direct Database Connection**: JDBC-based direct insert with transaction management
5. **Additional Output Formats**: JSON, Parquet, CSV for data analysis workflows
6. **Comprehensive Testing**: Property-based testing, performance benchmarks
7. **Resume Capability**: Checkpoint system to resume interrupted processing

### Q3: Design Choices Rationale

1. **Streaming vs. Loading Entire File**
   - **Choice**: Streaming with BufferedReader
   - **Why**: Constant memory usage regardless of file size
   - **Tradeoff**: Cannot random access, must process sequentially

2. **State Machine Pattern**
   - **Choice**: Explicit state tracking with ParserState
   - **Why**: NEM12's hierarchical structure (200→300→500)
   - **Tradeoff**: Slightly more complex than linear processing

3. **Strategy Pattern for SQL Generation**
   - **Choice**: Interface with multiple implementations
   - **Why**: Flexibility to choose optimal method per use case
   - **Tradeoff**: Additional abstraction layer

4. **Immutable Data Classes**
   - **Choice**: Kotlin data classes with `val`
   - **Why**: Thread-safety, functional programming style
   - **Tradeoff**: Cannot modify after creation

5. **Fail-Fast Error Handling**
   - **Choice**: Throw ParseException on first error
   - **Why**: Ensures data integrity, prevents bad data in DB
   - **Tradeoff**: Cannot do partial processing (could add best-effort mode)

## Performance

**Expected Performance (1GB file, ~10M records):**
- Parsing: ~20-30 seconds
- SQL Generation (COPY): ~10-15 seconds
- Memory Usage: <100MB (constant)
- **Total Time: ~30-45 seconds**

**Optimization Techniques:**
- Line-by-line streaming (no full file in memory)
- Batch buffering (reduces I/O operations)
- String operations without regex (faster parsing)
- Direct date construction (avoid DateTimeFormatter overhead)

## Testing

Run tests:
```bash
./gradlew test
```

Sample test file included: `src/test/resources/sample.nem12`

## Project Structure

```
nem12-parser/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── kotlin/com/flo/nem12/
│   │   │   ├── Main.kt
│   │   │   ├── model/
│   │   │   │   ├── MeterReading.kt
│   │   │   │   └── RecordType.kt
│   │   │   ├── parser/
│   │   │   │   ├── NEM12Parser.kt
│   │   │   │   ├── RecordParser.kt
│   │   │   │   ├── ParserState.kt
│   │   │   │   └── TimestampCalculator.kt
│   │   │   ├── generator/
│   │   │   │   ├── SQLGenerator.kt
│   │   │   │   ├── CopyCommandGenerator.kt
│   │   │   │   └── BatchInsertGenerator.kt
│   │   │   └── exception/
│   │   │       └── ParseException.kt
│   │   └── resources/
│   │       └── logback.xml
│   └── test/
│       ├── kotlin/com/flo/nem12/
│       └── resources/
│           └── sample.nem12
└── README.md
```

## License

This project is for the Flo Energy Tech Assessment.

## Author

Developed as part of Flo Energy technical assessment, demonstrating production-grade Kotlin development practices.

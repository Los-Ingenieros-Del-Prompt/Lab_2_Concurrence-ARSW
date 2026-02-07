# 🐕 Lab 2: Concurrency Analysis - Dog Race Simulator (ARSW)

> <b>Analysis, testing, and identification of concurrency risks in a multi-threaded greyhound racing simulator</b>

---

## 📑 Table of Contents

1. [🎯 Lab Objective](#-lab-objective)
2. [📋 Assignment Description](#-assignment-description)
3. [⚡ Application Features](#-application-features)
4. [⚠️ Risk Zones Identified](#️-risk-zones-identified)
5. [⚙️ Technologies Used](#️-technologies-used)
6. [🏗️ Architecture](#️-architecture)
7. [🚀 Running the Project](#-running-the-project)
8. [🧪 Testing](#-testing)
9. [📊 Code Coverage](#-code-coverage)

---

## 🎯 Lab Objective

This lab was provided by the instructor as a complete working application. Our objectives were to:

1. **Analyze** the multi-threaded dog racing simulator code
2. **Identify risk zones** (zonas de riesgo) - critical sections and potential concurrency issues
3. **Create comprehensive tests** to validate thread-safety and synchronization
4. **Document** concurrency patterns and thread coordination mechanisms
5. **Understand** Java concurrency concepts in a practical context

---

## 📋 Assignment Description

The application simulates a **greyhound racing track** (canodromo) where multiple dogs race simultaneously. Our work involved:

### What We Analyzed
- **Thread lifecycle management**: Creation, execution, and termination of Galgo threads
- **Synchronization mechanisms**: wait()/notifyAll() implementation in RaceControl
- **Critical sections**: Thread-safe operations in ArrivalRegistry
- **Race conditions**: Potential conflicts in shared resource access
- **Thread coordination**: Use of join() to wait for all threads completion

### What We Added
- ✅ Comprehensive unit tests for all components
- ✅ Tests for concurrent scenarios and race conditions
- ✅ Validation of synchronization mechanisms
- ✅ Documentation of identified risk zones
- ✅ Code coverage reporting (JaCoCo)

---

## ⚡ Application Features

The provided application includes:

### 🔹 Multi-threaded Race Simulation
- **Parallel execution**: Each greyhound runs independently in its own thread
- **Visual feedback**: Real-time updates of dog positions on the track
- **Synchronized finish**: Thread-safe arrival registration

### 🔹 Race Control
- **Pause/Resume**: Control all racing threads simultaneously
- **Wait/Notify pattern**: Proper thread coordination using monitor objects
- **Thread orchestration**: Main thread waits for all dogs to finish before declaring winner

### 🔹 GUI Application
- **Visual race track**: Swing-based interface with multiple lanes
- **Interactive controls**: Start, Stop (Pause), and Continue (Resume) buttons
- **Winner dialog**: Displays results after all dogs finish

---

## ⚠️ Risk Zones Identified

During our analysis, we identified the following **critical sections and risk zones**:

### 🔴 Critical Zone 1: Arrival Registration
**Location**: `ArrivalRegistry.registerArrival()`
```java
public synchronized ArrivalSnapshot registerArrival(String dogName) {
    final int position = nextPosition++;  // CRITICAL: Race condition without synchronization
    if (position == 1) {
        winner = dogName;  // CRITICAL: Must be atomic with position assignment
    }
    return new ArrivalSnapshot(position, winner);
}
```
**Risk**: Without `synchronized`, multiple threads could read the same `nextPosition` value, causing:
- Dogs assigned to the same position
- Wrong winner determination
- Inconsistent race results

**Mitigation**: ✅ Solved with `synchronized` method

---

### 🔴 Critical Zone 2: Pause/Resume Control
**Location**: `RaceControl.awaitIfPaused()`
```java
public void awaitIfPaused() throws InterruptedException {
    synchronized (monitor) {
        while (paused) {
            monitor.wait();  // CRITICAL: Must hold monitor lock
        }
    }
}
```
**Risk**: 
- Lost wake-up: If `notifyAll()` happens before `wait()`, thread could wait forever
- Spurious wakeups: Thread might wake without notification
- Missed state changes without proper synchronization

**Mitigation**: ✅ Solved with:
- `synchronized` block protecting state check and wait
- `while` loop (not `if`) to recheck condition after wake-up
- `notifyAll()` to wake all waiting threads

---

## ⚙️ Technologies Used

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Main programming language with concurrency features |
| **Swing** | - | GUI framework for visual race track |
| **Maven** | - | Dependency management and build |
| **JUnit** | 5.10.2 | Unit testing framework |
| **Mockito** | 5.11.0 | Mocking framework for tests |
| **JaCoCo** | 0.8.12 | Code coverage analysis |

---

## 🏗️ Architecture

The application (provided by instructor) is organized as follows:

```
src/main/java/edu/eci/arsw/dogsrace/
├── app/                           # Application entry point
│   └── MainCanodromo.java         # Main class with GUI initialization
│                                   # ANALYZED: Thread creation, join() coordination
│
├── threads/                       # Thread implementation
│   └── Galgo.java                 # Greyhound thread (runner)
│                                   # ANALYZED: Thread lifecycle, shared resource access
│
├── control/                       # Race control logic
│   └── RaceControl.java           # Pause/resume coordinator
│                                   # ⚠️ RISK ZONE: wait()/notifyAll() synchronization
│
├── domain/                        # Business domain
│   └── ArrivalRegistry.java       # Thread-safe finish line registry
│                                   # ⚠️ RISK ZONE: Critical section for position assignment
│
├── ui/                            # User interface components
│   ├── Canodromo.java             # Main race track window
│   └── Carril.java                # Individual lane panel
│                                   # ⚠️ POTENTIAL RISK: GUI updates from worker threads
│
└── util/                          # Utilities
    └── RandomGenerator.java       # Random number generation
```

### Components Analyzed

- **Galgo (Thread)**: Each dog runs independently, accesses shared RaceControl and ArrivalRegistry
- **RaceControl**: ⚠️ **RISK ZONE** - Manages pause/resume using wait()/notifyAll()
- **ArrivalRegistry**: ⚠️ **RISK ZONE** - Critical section for thread-safe result recording
- **Canodromo (UI)**: Visual track with potential thread-safety issues
- **MainCanodromo**: Orchestration with proper join() usage

---

## 🚀 Running the Project

### Prerequisites
- Java 21 or higher
- Maven 3.6+

### Compile the project
```bash
mvn clean compile
```

### Run our tests
```bash
mvn test
```

### Run with coverage (see our test coverage)
```bash
mvn verify
```

### Start the application (provided by instructor)
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.dogsrace.app.MainCanodromo"
```

Or compile and run directly:
```bash
mvn clean package
java -cp target/dogs-race-java21-1.0.0.jar edu.eci.arsw.dogsrace.app.MainCanodromo
```

### How to use the application

1. **Start**: Click the "Start" button to begin the race - all dogs start running simultaneously
2. **Stop (Pause)**: Click "Stop" to pause all dogs (threads call wait())
3. **Continue (Resume)**: Click "Continue" to resume the race (notifyAll() called)
4. **Finish**: Once all dogs complete the race, a dialog shows the winner

### Observing Risk Zones
While running:
- Notice how all threads pause/resume together (RaceControl synchronization)
- Observe the finish line - positions are always unique (ArrivalRegistry synchronization)
- Try pausing/resuming rapidly to stress-test synchronization

---

## 🧪 Testing

As part of our assignment, we created comprehensive tests to validate:

### Test Coverage Added
- ✅ **GalgoTest**: Thread execution and race progression
- ✅ **RaceControlTest**: Pause/resume synchronization mechanisms
- ✅ **ArrivalRegistryTest**: Thread-safe registration and race condition prevention
- ✅ **Concurrent scenarios**: Multiple threads accessing shared resources
- ✅ **Winner determination**: Correct winner assignment under concurrent access
- ✅ **Thread coordination**: Proper join() and thread lifecycle management

### Run all tests
```bash
mvn test
```

### Run tests for a specific class
```bash
mvn test -Dtest=GalgoTest
mvn test -Dtest=RaceControlTest
mvn test -Dtest=ArrivalRegistryTest
```

### Test Scenarios
Our tests verify:
1. **Race condition prevention**: Multiple threads cannot get the same position
2. **Synchronization correctness**: Pause/resume works for all threads
3. **Thread-safety**: No data corruption under concurrent access
4. **Memory visibility**: State changes are visible across threads
5. **Deadlock freedom**: No threads get stuck waiting forever

---

## 📊 Code Coverage

As part of our testing work, we use **JaCoCo** to measure code coverage.

### Generate coverage report
```bash
mvn verify
```

The HTML report is generated at:
```
target/site/jacoco/index.html
```

### Coverage Requirements
- **Instructions**: 85% minimum coverage
- **Branches**: 75% minimum coverage
- **Exclusions**: UI classes (`ui/**` and `app/**`) are excluded from coverage requirements as they are difficult to test

### View report
```bash
open target/site/jacoco/index.html  # macOS
xdg-open target/site/jacoco/index.html  # Linux
start target/site/jacoco/index.html  # Windows
```

### Our Testing Focus
We prioritized testing:
- ✅ Core concurrency logic (RaceControl, ArrivalRegistry)
- ✅ Thread behavior (Galgo)
- ✅ Race condition scenarios
- ✅ Synchronization correctness

---

## 💡 Learning Objectives

Through this lab analysis, we learned:

1. **Critical Section Identification**: How to recognize code that requires synchronization
2. **Race Condition Detection**: Understanding when concurrent access can cause problems
3. **Synchronization Patterns**: Proper use of synchronized, wait(), notifyAll()
4. **Thread-Safe Design**: Techniques to prevent data corruption in multi-threaded code
5. **Testing Concurrent Code**: Strategies to validate thread-safety and synchronization
6. **Memory Visibility**: Understanding how threads see shared data
7. **Deadlock Prevention**: Designing synchronization to avoid thread starvation

---

## 📝 Key Concurrency Concepts Analyzed

### Synchronization Mechanisms Found
- ✅ **synchronized methods**: Ensures atomic operations (ArrivalRegistry)
- ✅ **synchronized blocks**: Fine-grained locking (RaceControl)
- ✅ **wait()/notifyAll()**: Thread coordination for pause/resume
- ✅ **Thread.join()**: Waiting for thread completion
- ✅ **Monitor pattern**: Dedicated monitor object for cleaner synchronization

### Common Pitfalls Identified
- ⚠️ **Check-then-act**: Reading shared state and acting on it (needs atomicity)
- ⚠️ **Lost wake-ups**: Notification before wait() call
- ⚠️ **Spurious wakeups**: Threads waking without notification (use `while`, not `if`)
- ⚠️ **Memory visibility**: Changes in one thread not visible to others without synchronization
- ⚠️ **GUI thread-safety**: Swing updates from worker threads

---

## 🎓 Conclusions

This lab provided hands-on experience analyzing real concurrent code. Key takeaways:

1. **Concurrency is hard**: Even simple programs have multiple risk zones
2. **Synchronization is essential**: Unprotected shared state leads to bugs
3. **Testing is crucial**: Race conditions may not appear in every execution
4. **Patterns matter**: Using established patterns (monitor, producer-consumer) prevents errors
5. **Documentation helps**: Clear identification of critical sections aids maintenance

The dog race simulator effectively demonstrates why proper synchronization is critical in multi-threaded applications.

---
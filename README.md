# World Clock Application

A multi-timezone world clock application built with Java Swing.

## Project Setup

This is a Maven project configured to build a fat (uber) JAR with all dependencies included.

### Build

To build the project:
```bash
mvn clean package
```

This generates `worldclock.jar` in the `target/` directory.

### Run

To run the application:
```bash
java -jar target/worldclock.jar
```

Or directly via Maven:
```bash
mvn exec:java@run
```

## Project Structure

```
worldclock/
├── src/
│   ├── main/
│   │   └── java/com/worldclock/
│   │       └── WorldClockApp.java
│   └── test/
│       └── java/com/worldclock/
├── pom.xml
└── README.md
```

## Technology Stack

- **Java 11+**
- **Swing** - UI framework
- **Maven** - Build tool

## Features (To Implement)

- [ ] Display current time for multiple time zones
- [ ] Add/remove time zones
- [ ] Digital and analog clock displays
- [ ] Configuration file support
- [ ] Theme customization

## Building Fat JAR

The Maven Shade Plugin is configured to create a fat JAR with:
- All dependencies bundled
- Main class automatically specified in manifest
- Ready to run with `java -jar`

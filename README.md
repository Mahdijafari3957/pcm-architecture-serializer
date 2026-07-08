# pcm-architecture-serializer
A Java tool for converting Palladio Component Model (PCM) software architecture models into text-based artifacts suitable for AI-driven architecture analysis.
The tool currently offers four serialization methods:
1. JSON serialization: Produces structured JSON for PCM models.
2. Detailed text serialization: Produces a human-readable architecture report for PCM models.
3. Compact text serialization: Produces a minimal summary listing the main PCM models.
4. Security-focused serialization: Produces a security-oriented view including component exposure, exposed interfaces, external dependencies, communication paths, deployment topology, and component placement.



## Project Structure

```text
pcm-architecture-serializer/
├── lib/
│   └── Required local Palladio/EMF/Eclipse JAR files
├── src/
│   └── main/
│       ├── java/
│       │   └── edu/
│       │       └── kit/
│       │           └── pcm/
│       │               └── architecture/
│       │                   └── serializer/
│       │                       ├── Main.java
│       │                       └── core/
│       │                           ├── PalladioModelLoader.java
│       │                           ├── PalladioArchitectureSerializer.java
│       │                           └── PalladioArchitectureGeneralSerializer.java
│       └── resources/
│           └── models/
│               └── travelplanner/
│                   ├── travelplanner.repository
│                   ├── travelplanner.system
│                   ├── travelplanner.allocation
│                   └── travelplanner.resourceenvironment
├── pom.xml
└── README.md
```

## Prerequisites
- Java 21
- Maven
- Local Palladio/EMF/Eclipse dependency JARs in the `lib/` folder

The project currently uses Maven for the basic project setup and the Jackson dependency. The Palladio, EMF, and Eclipse dependencies are currently provided as local JAR files in `lib`.

---

## Setup
The tool requires several external dependencies, including Palladio, EMF, and Eclipse-related JARs. These dependencies are currently provided locally through the `lib/` directory and must be added to the project manually.

The following setup instructions are for IntelliJ IDEA.

1. Open the project.

2. Add the local dependency JARs:

```text
File → Project Structure → Libraries
```

3. Click:

```text
+ → Java
```

4. Select all `.jar` files inside:

```text
lib/
```

5. Click:

```text
Apply → OK
```

6. Rebuild the project:

```text
Build → Rebuild Project
```

## Running the Serializer

In IntelliJ:

1. Open `Main.java`.
2. Select the PCM model you want to serialize.
3. Run the application.

By default, the serializer loads the `travelplanner` PCM model and creates the following output file:

```text
src/main/resources/models/travelplanner/travelplanner.json

**Note:** To run the serializer on another PCM model, first add the required PCM model files to the `src/main/resources/models/` directory. Each PCM model should be placed in its own folder and should include the required artifacts, such as the repository model, system model, allocation model, and resource environment model. Then update the corresponding `modelName` and `modelBaseName` values in `Main.java`.

---

## Troubleshooting

### 1. `package org.eclipse.emf... does not exist`

Cause: IntelliJ does not know about the local JAR files.

Fix:

```text
File → Project Structure → Libraries → + → Java → select all JARs inside lib/
```

Then rebuild the project.

---

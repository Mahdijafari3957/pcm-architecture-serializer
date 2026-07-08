package edu.kit.pcm.architecture.serializer;

import java.nio.file.Path;

import java.io.IOException;
import java.nio.file.Files;

import edu.kit.pcm.architecture.serializer.core.PalladioArchitectureSerializer;
import edu.kit.pcm.architecture.serializer.core.PalladioArchitectureGeneralSerializer;
import edu.kit.pcm.architecture.serializer.core.PalladioModelLoader;

public class Main {
    public static void main(String[] args) throws IOException {
        String modelName = "travelplanner";
        String modelFolder = Path.of(
                "src", "main", "resources", "models", modelName
        ).toAbsolutePath().toString();

        String modelBaseName = "travelplanner";

        PalladioModelLoader.PalladioArchitecture architecture =
                PalladioModelLoader.load(modelFolder, modelBaseName);


//        String result = PalladioArchitectureSerializer.serializeAsJson(
//                architecture.repository,
//                architecture.system,
//                architecture.allocation,
//                architecture.resourceEnvironment
//        );


        String result = PalladioArchitectureGeneralSerializer.serializeDetailed(
                architecture.repository,
                architecture.system,
                architecture.allocation,
                architecture.resourceEnvironment
        );


        System.out.println("Serialized Model: " + result);

        Path outputFile = Path.of(modelFolder, modelName + ".json");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, result);
    }
}
package megamek.ai.neuralnetwork;

import megamek.client.bot.caspar.ClassificationScore;
import megamek.client.bot.caspar.MovementClassification;
import megamek.common.Coords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class NeuralNetworkTest {

    private NeuralNetwork neuralNetwork;

    @BeforeEach
    public void setUp() {
        NeuralNetwork.testTensorFlow();
        neuralNetwork = NeuralNetwork.loadBrain(new BrainRegistry("default", 3));
    }

    @AfterEach
    public void tearDown() {
        if (neuralNetwork != null) {
            neuralNetwork.close();
        }
    }

    @TestFactory
    Stream<DynamicTest> testCalculatingMoves() {
        return generateValidationInputs().map(target -> dynamicTest(
              "Testing entry " + target.classification,
              () -> {
                  var prediction = neuralNetwork.predict(target.input());
                  assertEquals(target.classification,
                        ClassificationScore.fromPrediction(prediction).getClassification(),
                        "Expected classification: " + target.classification + " but got: " +
                                    ClassificationScore.fromPrediction(prediction).getClassification() + " with " +
                              "values " + Arrays.toString(prediction));
              }
        ));
    }

    private record TestValue(float[] input, MovementClassification classification) {}

    private Stream<TestValue> generateValidationInputs() {
        List<TestValue> featureEntries = new ArrayList<>();
        Path path = Path.of("data", "ai","brains", "default");
        Path normalizationFilePath = Path.of(path.toString(), "test_inputs.csv");
        try (var reader = new BufferedReader(new FileReader(normalizationFilePath.toFile()))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                float[] entry = new float[values.length-1];
                for (int i = 0; i < values.length-1; i++) {
                    entry[i] = Float.parseFloat(values[i]);
                }
                MovementClassification res = MovementClassification.values()[Integer.parseInt(values[values.length-1])];
                featureEntries.add(new TestValue(entry, res));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load TensorFlow model: " + e.getMessage(), e);
        }

        return featureEntries.stream();
    }

}

package megamek.ai.neuralnetwork;

import java.io.IOException;
import java.util.Map;

import megamek.logging.MMLogger;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.proto.SignatureDef;
import org.tensorflow.proto.TensorInfo;


/**
 * NeuralNetwork class for loading and making predictions with TensorFlow models.
 */
public class NeuralNetwork {
    private static final MMLogger logger = MMLogger.create(NeuralNetwork.class);

    private SavedModelBundle model;
    private Session session;
    private String inputOperationName;
    private String outputOperationName;

    /**
     * Loads a TensorFlow model from the specified path.
     *
     * @param modelPath Path to the saved model directory
     * @throws IOException If the model cannot be loaded
     */
    public void loadModel(String modelPath) throws IOException {
        try {
            model = SavedModelBundle.load(modelPath, "serve");
            session = model.session();

            SignatureDef sigDef = model.metaGraphDef().getSignatureDefMap().get("serving_default");

            // Extract input operation name
            Map.Entry<String, TensorInfo> inputEntry = sigDef.getInputsMap().entrySet().iterator().next();
            inputOperationName = parseOperationName(inputEntry.getValue().getName());

            // Extract output operation name
            Map.Entry<String, TensorInfo> outputEntry = sigDef.getOutputsMap().entrySet().iterator().next();
            outputOperationName = parseOperationName(outputEntry.getValue().getName());

            logger.info("Model loaded successfully from: {}", modelPath);
            logger.info("Input operation: {}", inputOperationName);
            logger.info("Output operation: {}", outputOperationName);
        } catch (Exception e) {
            logger.error("Failed to load model", e);
            throw new IOException("Failed to load TensorFlow model: " + e.getMessage(), e);
        }
    }

    /**
     * Parse the operation name from TensorFlow's tensor name format
     * (e.g., "dense_input:0" -> "dense_input")
     */
    private String parseOperationName(String tensorName) {
        if (tensorName.contains(":")) {
            return tensorName.substring(0, tensorName.lastIndexOf(":"));
        }
        return tensorName;
    }

    /**
     * Makes a prediction using the loaded model.
     *
     * @param inputVector Input features as a double array
     * @return Prediction result as a double
     * @throws IllegalStateException If the model is not loaded
     */
    public float predict(float[] inputVector) {
        if (session == null) {
            throw new IllegalStateException("Model not loaded. Call loadModel() first.");
        }

        FloatNdArray ndArray = NdArrays.ofFloats(Shape.of(1, inputVector.length));

        // Copy the float values into the correct shape
        for (int i = 0; i < inputVector.length; i++) {
            // Assuming a 2D input shape [1][features]
            ndArray.setFloat(inputVector[i], 0, i);
        }

        try (var inputTensor = Tensor.of(org.tensorflow.types.TFloat32.class, ndArray.shape(), ndArray::copyTo)) {
            try(var outputTensors = session.runner().feed(inputOperationName, inputTensor)
                                      .fetch(outputOperationName)
                                      .run()) {
                // Extract the result
                try(var rOutputTensor = outputTensors.get(0).asRawTensor()) {
                    return rOutputTensor.data().asFloats().getFloat(0);
                }
            }
        } catch (Exception e) {
            logger.error("Prediction failed", e);
            throw new RuntimeException("Failed to make prediction: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the model and session to release resources.
     */
    public void close() {
        if (model != null) {
            try {
                model.close();
                model = null;
                session = null;
                logger.info("Neural network model resources released");
            } catch (Exception e) {
                logger.warn("Error closing model", e);
            } finally {
                model = null;
                session = null;
            }
        }
    }

    /**
     * Simple test method to verify TensorFlow is working.
     */
    public static void testTensorFlow() {
        try {
            String version = TensorFlow.version();
            logger.info("TensorFlow version: {}", version);
        } catch (Exception e) {
            logger.error("Error loading TensorFlow", e);
        }
    }
}

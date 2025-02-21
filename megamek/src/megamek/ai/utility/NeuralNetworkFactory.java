package megamek.ai.utility;

public class NeuralNetworkFactory {
    public static NeuralNetwork createNeuralNetworkForConsiderationsAndThreatHeatmap(int considerationsSize, int hiddenLayerSize) {
        return new NeuralNetwork(considerationsSize + QuickBoardRepresentation.NORMALIZED_THREAT_HEATMAP, hiddenLayerSize, 1);
    }
    public static NeuralNetwork createNeuralNetworkForConsiderationsAndThreatHeatmap(int considerationsSize, int firstHiddenLayerSize, int secondHiddenLayerSize) {
        return new NeuralNetwork(considerationsSize + QuickBoardRepresentation.NORMALIZED_THREAT_HEATMAP, firstHiddenLayerSize, secondHiddenLayerSize, 1);
    }
}

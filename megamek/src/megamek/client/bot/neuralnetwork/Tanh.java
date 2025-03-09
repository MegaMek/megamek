package megamek.client.bot.neuralnetwork;

/**
 * Tanh activation function
 */
public class Tanh implements ActivationFunction {

    @Override
    public double activate(double x) {
        return Math.tanh(x);
    }

    @Override
    public double derivative(double x) {
        double tanh = Math.tanh(x);
        return 1 - tanh * tanh;
    }
}

package megamek.client.bot.neuralnetwork;

/**
 * Linear activation function
 */
public class Linear implements ActivationFunction {

    @Override
    public double activate(double x) {
        return x;
    }

    @Override
    public double derivative(double x) {
        return 1.0;
    }

}

package megamek.client.bot.neuralnetwork;

import java.io.Serializable;

/**
 * Interface for activation functions
 */
public interface ActivationFunction extends Serializable {
    double activate(double x);
    double derivative(double x);
}

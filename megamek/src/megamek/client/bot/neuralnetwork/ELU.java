package megamek.client.bot.neuralnetwork;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * ELU (Exponential Linear Unit) activation function
 */
public class ELU implements ActivationFunction {
    private double alpha;

    // Default constructor for Jackson
    public ELU() {
        this.alpha = 1.0;
    }

    public ELU(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public double activate(double x) {
        return x > 0 ? x : alpha * (Math.exp(x) - 1);
    }

    @Override
    public double derivative(double x) {
        return x > 0 ? 1.0 : alpha * Math.exp(x);
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;

        if (!(object instanceof ELU elu)) return false;

        return new EqualsBuilder().append(getAlpha(), elu.getAlpha()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getAlpha()).toHashCode();
    }
}

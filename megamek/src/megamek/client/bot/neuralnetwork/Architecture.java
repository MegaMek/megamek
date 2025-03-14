package megamek.client.bot.neuralnetwork;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Architecture {

    private final int[] layers;
    private final ActivationFunction[] activationFunctions;

    public Architecture(int[] layers, ActivationFunction[] activationFunctions) {
        assert layers.length-1 == activationFunctions.length;
        this.layers = layers;
        this.activationFunctions = activationFunctions;
    }

    public int getLayerCount() {
        return layers.length;
    }

    public int[] getLayerSizes() {
        return layers;
    }

    public ActivationFunction[] getActivationFunctions() {
        return activationFunctions;
    }

    public int getActivationFunctionCount() {
        return activationFunctions.length;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;

        if (!(object instanceof Architecture that)) return false;

        return new EqualsBuilder().append(layers, that.layers).append(getActivationFunctions(), that.getActivationFunctions()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(layers).append(getActivationFunctions()).toHashCode();
    }
}

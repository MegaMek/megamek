package megamek.client.bot.princess;

public record FiringPhysicalDamage(double firingDamage, double physicalDamage, double takenDamage) {
    public FiringPhysicalDamage() {
        this(0, 0, 0);
    }

    public FiringPhysicalDamage withFiringDamage(double firingDamage) {
        return new FiringPhysicalDamage(firingDamage, physicalDamage, takenDamage);
    }

    public FiringPhysicalDamage withPhysicalDamage(double physicalDamage) {
        return new FiringPhysicalDamage(firingDamage, physicalDamage, takenDamage);
    }

    public FiringPhysicalDamage withTakenDamage(double takenDamage) {
        return new FiringPhysicalDamage(firingDamage, physicalDamage, takenDamage);
    }

    public double getMaximumDamageEstimate() {
        return firingDamage + physicalDamage;
    }
}
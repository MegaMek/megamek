package megamek.client.bot.princess;

public class FiringPhysicalDamage {
    public double firingDamage;
    public double physicalDamage;
    public double takenDamage;

    public FiringPhysicalDamage() {
        firingDamage = 0;
        physicalDamage = 0;
        takenDamage = 0;
    }

    public FiringPhysicalDamage(double firingDamage, double physicalDamage, double takenDamage) {
        this.firingDamage = firingDamage;
        this.physicalDamage = physicalDamage;
        this.takenDamage = takenDamage;
    }

    public double getMaximumDamageEstimate() {
        return firingDamage + physicalDamage;
    }
}

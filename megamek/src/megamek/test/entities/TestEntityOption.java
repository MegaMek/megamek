package megamek.test.entities;

public interface TestEntityOption
{
    public final static int CEIL_TARGCOMP_CRITS = 0;
    public final static int ROUND_TARGCOMP_CRITS = 1;
    public final static int FLOOR_TARGCOMP_CRITS = 2; 

    public float getWeightCeilingEngine();
    public float getWeightCeilingStructure();
    public float getWeightCeilingArmor();
    public float getWeightCeilingControls();
    public float getWeightCeilingWeapons();
    public float getWeightCeilingTargComp();
    public float getWeightCeilingGyro();
    public float getWeightCeilingTurret();
    public float getWeightCeilingPowerAmp();

    public float getMaxOverweight();
    public boolean showOverweightedEntity();
    public boolean showUnderweightedEntity();
    public boolean showCorrectArmor();
    public boolean showCorrectCritical();
    public boolean showFailedEquip();
    public float getMinUnderweight();
    public boolean ignoreFailedEquip(String name);
    public boolean skip();

    public int getTargCompCrits();

    public int getPrintSize();
}

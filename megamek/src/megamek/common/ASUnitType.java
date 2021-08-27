package megamek.common;

public enum ASUnitType {

    BM, IM, PM, CV, SV, MS, BA, CI, AF, CF, SC, DS, DA, JS, WS, SS;
    
    public static ASUnitType getUnitType(Entity en) {
        if (en instanceof Mech) {
            return ((Mech)en).isIndustrial() ? IM : BM;
        } else if (en instanceof Protomech) {
            return PM;
        } else if (en instanceof Tank) {
            return en.isSupportVehicle() ? SV : CV;
        } else if (en instanceof BattleArmor) {
            return BA;
        } else if (en instanceof Infantry) {
            return CI;
        } else if (en instanceof SpaceStation) {
            return SS;
        } else if (en instanceof Warship) {
            return WS;
        } else if (en instanceof Jumpship) {
            return JS;
        } else if (en instanceof Dropship) {
            return ((Dropship)en).isSpheroid() ? DS : DA;
        } else if (en instanceof SmallCraft) {
            return SC;
        } else if (en instanceof FixedWingSupport) {
            return SV;
        } else if (en instanceof ConvFighter) {
            return CF;
        } else if (en instanceof Aero) {
            return AF;
        }
        return null;
    }
}

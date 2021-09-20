package megamek.common;

import java.util.Arrays;

/** Represents the AlphaStrike Element Types (ASC, page 91) */
public enum ASUnitType {

    BM, IM, PM, CV, SV, MS, BA, CI, AF, CF, SC, DS, DA, JS, WS, SS;

    /** Returns the AlphaStrike element type for the given entity or null if it has no AS equivalent. */
    //TODO: Add a NONE type to avoid null, maybe for buildings?
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

    /** Returns true if this AS Element Type is equal to any of the given Types. */
    public boolean isAnyOf(ASUnitType type, ASUnitType... furtherTypes) {
        return (this == type) || Arrays.stream(furtherTypes).anyMatch(t -> this == t);
    }

}

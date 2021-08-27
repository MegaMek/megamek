package megamek.common;

public enum SBFElementType {

    BM, AS, MX, PM, V, BA, CI, MS, LA;

    public static SBFElementType getUnitType(Entity en) {
        if (en instanceof Mech) {
            return BM;
        } else if (en instanceof Protomech) {
            return PM;
        } else if (en instanceof Tank) {
            return V;
        } else if (en instanceof BattleArmor) {
            return BA;
        } else if (en instanceof Infantry) {
            return CI;
        } else if (en instanceof SpaceStation) {
            return V;
        } else if (en instanceof Warship) {
            return LA;
        } else if (en instanceof Jumpship) {
            return LA;
        } else if (en instanceof Dropship) {
            return LA;
        } else if (en instanceof SmallCraft) {
            return LA;
        } else if (en instanceof FixedWingSupport) {
            return V;
        } else if (en instanceof ConvFighter) {
            return AS;
        } else if (en instanceof Aero) {
            return AS;
        }
        return null;
    }


    public static SBFElementType getUnitType(AlphaStrikeElement element) {
        return getUnitType(element.getType());
    }

    //TODO does this work? AS doesnt distinguish between SV Aero and SV ground tanks
    public static SBFElementType getUnitType(ASUnitType type) {
        switch (type) {
            case IM:
            case BM:
                return BM;
            case PM:
                return PM;
            case MS:
                return MS;
            case BA:
                return BA;
            case CI:
                return CI;
            case AF:
            case CF:
                return AS;
            case SS:
            case SV:
            case CV:
                return V;
            default:
                return LA;
        }
    }
}

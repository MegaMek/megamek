package megamek.common.miscGear;

import megamek.common.EquipmentTypeLookup;
import megamek.common.MiscType;
import megamek.common.SimpleTechLevel;

public class AntiMekGear extends MiscType {

    public AntiMekGear() {
        name = "Anti-Mek Gear";
        setInternalName(EquipmentTypeLookup.ANTI_MEK_GEAR);
        tonnage = 0.015;
        flags = flags.or(F_INF_EQUIPMENT);
        cost = COST_VARIABLE;
        rulesRefs = "155, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL).setAdvancement(2456, 2460, 2500)
                .setStaticTechLevel(SimpleTechLevel.STANDARD)
                .setApproximate(true, false, false).setTechBase(RATING_D)
                .setPrototypeFactions(F_LC).setProductionFactions(F_LC)
                .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D);
    }
}

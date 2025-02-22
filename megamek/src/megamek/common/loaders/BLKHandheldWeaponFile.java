package megamek.common.loaders;

import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.HandheldWeapon;
import megamek.common.util.BuildingBlock;

public class BLKHandheldWeaponFile extends BLKFile implements IMekLoader {
    public BLKHandheldWeaponFile(BuildingBlock block) {
        dataFile = block;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {
        HandheldWeapon e = new HandheldWeapon();
        setBasicEntityData(e);

        if (!dataFile.exists("year")) {
            throw new EntityLoadingException("Could not find year block.");
        }
        e.setYear(dataFile.getDataAsInt("year")[0]);

        loadEquipment(e, "Gun", HandheldWeapon.LOC_GUN);

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        e.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }
        int armor = dataFile.getDataAsInt("armor")[0];
        e.initializeArmor(armor, HandheldWeapon.LOC_GUN);
        e.setArmorTonnage(e.getArmorWeight());

        e.recalculateTechAdvancement();
        return e;
    }
}

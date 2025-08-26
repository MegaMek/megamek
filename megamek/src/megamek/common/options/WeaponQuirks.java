/*
 * Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.options;

import static java.util.stream.Collectors.toList;

import java.io.Serial;
import java.util.List;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.lasers.EnergyWeapon;

/**
 * This class represents the weapon quirks of an individual weapon. Each weapon on a unit has its own WeaponQuirks
 * object assigned that contains its specific quirks, see {@link Mounted#getQuirks()}. When changing this, note that all
 * options should remain boolean options.
 *
 * @author Taharqa (Jay Lawson)
 */
public class WeaponQuirks extends AbstractOptions {
    @Serial
    private static final long serialVersionUID = -8455685281028804229L;
    public static final String WPN_QUIRKS = "WeaponQuirks";

    public WeaponQuirks() {
        super();
    }

    @Override
    public void initialize() {
        IBasicOptionGroup wpnQuirk = addGroup("wpn_quirks", WPN_QUIRKS);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_POS_ACCURATE, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_NEG_INACCURATE, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_POS_STABLE_WEAPON, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_POS_IMP_COOLING, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_NEG_POOR_COOLING, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_NEG_NO_COOLING, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_NEG_EXPOSED_LINKAGE, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_NEG_AMMO_FEED_PROBLEMS, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_NEG_STATIC_FEED, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_NEG_EM_INTERFERENCE, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_POS_FAST_RELOAD, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_POS_MOD_WEAPONS, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_POS_JETTISON_CAPABLE, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_NEG_NON_FUNCTIONAL, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_NEG_MISREPAIRED, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAPON_NEG_MIS_REPLACED, false);

    }

    //TODO
    //unimplemented
    // ammo feed problem
    //EM Interference
    //jettison-capable Weapon
    //non-functional
    //Directional Torso Mount BMM pg 85
    //Barrel Fist BMM pg 85

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return WeaponQuirksInfo.getInstance();
    }

    /** @return A list of weapon quirks that are active in this Quirks object. */
    public List<IOption> activeQuirks() {
        return getOptionsList().stream().filter(IOption::booleanValue).collect(toList());
    }

    public static boolean isQuirkDisallowed(IOption quirk, Entity en,
          EquipmentType equipmentType) {
        String qName = quirk.getName();
        // There may be some non-WeaponType quirks, specifically melee weapons
        if (!(equipmentType instanceof WeaponType) && !equipmentType.hasFlag(MiscType.F_CLUB)) {
            return true;
        } else if ((equipmentType instanceof MiscType) && equipmentType.hasFlag(MiscType.F_CLUB)) {
            return qName.equals(OptionsConstants.QUIRK_WEAPON_NEG_AMMO_FEED_PROBLEMS)
                  || qName.equals(OptionsConstants.QUIRK_WEAPON_NEG_EM_INTERFERENCE)
                  || qName.equals(OptionsConstants.QUIRK_WEAPON_NEG_NO_COOLING)
                  || qName.equals(OptionsConstants.QUIRK_WEAPON_NEG_POOR_COOLING)
                  || qName.equals(OptionsConstants.QUIRK_WEAPON_NEG_STATIC_FEED)
                  || qName.equals(OptionsConstants.QUIRK_WEAPON_POS_IMP_COOLING)
                  || qName.equals(OptionsConstants.QUIRK_WEAPON_POS_FAST_RELOAD);
        }
        if (!(equipmentType instanceof WeaponType weaponType)) {
            throw new IllegalArgumentException("EquipmentType must be a WeaponType");
        }

        if (!(weaponType instanceof AmmoWeapon)) {
            switch (qName) {
                case OptionsConstants.QUIRK_WEAPON_NEG_AMMO_FEED_PROBLEMS,
                     OptionsConstants.QUIRK_WEAPON_NEG_STATIC_FEED,
                     OptionsConstants.QUIRK_WEAPON_POS_FAST_RELOAD -> {
                    return true;
                }
            }
        }

        if (!(weaponType instanceof EnergyWeapon) && qName.equals(OptionsConstants.QUIRK_WEAPON_NEG_EM_INTERFERENCE)) {
            return true;
        }

        if (en instanceof ProtoMek) {
            if (qName.equals(OptionsConstants.QUIRK_WEAPON_POS_FAST_RELOAD)
                  || qName.equals(OptionsConstants.QUIRK_WEAPON_NEG_STATIC_FEED)) {
                return true;
            }
        }

        boolean hasBadCoolingQuirk = qName.equals(OptionsConstants.QUIRK_WEAPON_POS_IMP_COOLING)
              || qName.equals(OptionsConstants.QUIRK_WEAPON_NEG_POOR_COOLING)
              || qName.equals(OptionsConstants.QUIRK_WEAPON_NEG_NO_COOLING);
        if (en instanceof Tank || en instanceof BattleArmor || en instanceof ProtoMek) {
            if (hasBadCoolingQuirk) {
                return true;
            }
        }

        if (en.isConventionalInfantry()) {
            return true;
        }

        if (weaponType.getHeat() == 0) {
            if (hasBadCoolingQuirk) {
                return true;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAPON_POS_JETTISON_CAPABLE)) {
            if (en instanceof ProtoMek
                  || en instanceof Aero
                  || en instanceof GunEmplacement) {

                return true;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAPON_POS_MOD_WEAPONS)) {
            if ((en instanceof ProtoMek) || (en instanceof Jumpship)) {
                return true;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT)) {
            if ((en instanceof Aero) || (en instanceof BattleArmor) || (en instanceof Tank)) {
                return true;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAPON_POS_STABLE_WEAPON)) {
            if (en instanceof Aero) {
                return true;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAPON_NEG_EXPOSED_LINKAGE)) {
            if (en instanceof Aero) {
                return true;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAPON_NEG_EM_INTERFERENCE)) {
            return en instanceof Jumpship;
        }

        return false;
    }

    private static class WeaponQuirksInfo extends AbstractOptionsInfo {
        private static final AbstractOptionsInfo instance = new WeaponQuirksInfo();

        public static AbstractOptionsInfo getInstance() {
            return instance;
        }

        protected WeaponQuirksInfo() {
            super("WeaponQuirksInfo");
        }
    }
}

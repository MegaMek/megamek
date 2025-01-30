/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.options;

import megamek.common.*;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.lasers.EnergyWeapon;

import java.io.Serial;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * This class represents the weapon quirks of an individual weapon. Each weapon on a unit has its own
 * WeaponQuirks object assigned that contains its specific quirks, see {@link Mounted#getQuirks()}.
 * When changing this, note that all options should remain boolean options.
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
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_POS_ACCURATE, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_INACCURATE, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_POS_STABLE_WEAPON, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_POS_IMP_COOLING, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_POOR_COOLING, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_NO_COOLING, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_EXPOSED_LINKAGE, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_AMMO_FEED_PROBLEMS, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_STATIC_FEED, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_EM_INTERFERENCE, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_POS_FAST_RELOAD, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_POS_DIRECT_TORSO_MOUNT, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_POS_MOD_WEAPONS, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_POS_JETTISON_CAPABLE, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_NON_FUNCTIONAL, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_MISREPAIRED, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_MISREPLACED, false);

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
        if (!(equipmentType instanceof WeaponType) && !equipmentType.hasFlag(EquipmentFlag.F_CLUB)) {
            return true;
        } else if (equipmentType.hasFlag(EquipmentFlag.F_CLUB)) {
            return qName.equals(OptionsConstants.QUIRK_WEAP_NEG_AMMO_FEED_PROBLEMS)
                || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_EM_INTERFERENCE)
                || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_NO_COOLING)
                || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_POOR_COOLING)
                || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_STATIC_FEED)
                || qName.equals(OptionsConstants.QUIRK_WEAP_POS_IMP_COOLING)
                || qName.equals(OptionsConstants.QUIRK_WEAP_POS_FAST_RELOAD);
        }
        if (!(equipmentType instanceof WeaponType weaponType)) {
            throw new IllegalArgumentException("EquipmentType must be a WeaponType");
        }

        if (!(weaponType instanceof AmmoWeapon)) {
            switch (qName) {
                case OptionsConstants.QUIRK_WEAP_NEG_AMMO_FEED_PROBLEMS,
                     OptionsConstants.QUIRK_WEAP_NEG_STATIC_FEED,
                     OptionsConstants.QUIRK_WEAP_POS_FAST_RELOAD -> {
                    return true;
                }
            }
        }

        if (!(weaponType instanceof EnergyWeapon) && qName.equals(OptionsConstants.QUIRK_WEAP_NEG_EM_INTERFERENCE)) {
            return true;
        }

        if (en instanceof ProtoMek) {
            if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_FAST_RELOAD)
                || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_STATIC_FEED)) {
                return true;
            }
        }

        boolean hasBadCoolingQuirk = qName.equals(OptionsConstants.QUIRK_WEAP_POS_IMP_COOLING)
            || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_POOR_COOLING)
            || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_NO_COOLING);
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

        if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_JETTISON_CAPABLE)) {
            if (en instanceof ProtoMek
                || en instanceof Aero
                || en instanceof GunEmplacement)  {

                return true;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_MOD_WEAPONS)) {
            if ((en instanceof ProtoMek) || (en instanceof Jumpship)) {
                return true;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_DIRECT_TORSO_MOUNT)) {
            if ((en instanceof Aero) || (en instanceof BattleArmor) || (en instanceof Tank)) {
                return true;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_STABLE_WEAPON)) {
            if (en instanceof Aero) {
                return true;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAP_NEG_EXPOSED_LINKAGE)) {
            if (en instanceof Aero) {
                return true;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAP_NEG_EM_INTERFERENCE)) {
            if (en instanceof Jumpship) {
                return true;
            }
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

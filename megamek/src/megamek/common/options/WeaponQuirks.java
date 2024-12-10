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
import java.util.Map;

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

    // All quirks you add:
    private static final String[] QUIRKS = {
        OptionsConstants.QUIRK_WEAP_POS_ACCURATE,
        OptionsConstants.QUIRK_WEAP_NEG_INACCURATE,
        OptionsConstants.QUIRK_WEAP_POS_STABLE_WEAPON,
        OptionsConstants.QUIRK_WEAP_POS_IMP_COOLING,
        OptionsConstants.QUIRK_WEAP_NEG_POOR_COOLING,
        OptionsConstants.QUIRK_WEAP_NEG_NO_COOLING,
        OptionsConstants.QUIRK_WEAP_NEG_EXPOSED_LINKAGE,
        OptionsConstants.QUIRK_WEAP_NEG_AMMO_FEED_PROBLEMS,
        OptionsConstants.QUIRK_WEAP_NEG_STATIC_FEED,
        OptionsConstants.QUIRK_WEAP_NEG_EM_INTERFERENCE,
        OptionsConstants.QUIRK_WEAP_POS_FAST_RELOAD,
        OptionsConstants.QUIRK_WEAP_POS_DIRECT_TORSO_MOUNT,
        OptionsConstants.QUIRK_WEAP_POS_MOD_WEAPONS,
        OptionsConstants.QUIRK_WEAP_POS_JETTISON_CAPABLE,
        OptionsConstants.QUIRK_WEAP_NEG_NON_FUNCTIONAL,
        OptionsConstants.QUIRK_WEAP_NEG_MISREPAIRED,
        OptionsConstants.QUIRK_WEAP_NEG_MISREPLACED
    };

    public WeaponQuirks() {
        super();
    }

    @Override
    public void initialize() {
        var wpnQuirk = addGroup("wpn_quirks", WPN_QUIRKS);
        var wpnInfo = getOptionsInfoImp();

        for (String quirkName : QUIRKS) {
            optionsHash.put(quirkName, new Option(this, quirkName, IOption.BOOLEAN, false));
            wpnInfo.addOptionInfo(wpnQuirk, quirkName);
        }
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

    public static boolean isQuirkLegalFor(IOption quirk, Entity en,
            EquipmentType etype) {
        String qName = quirk.getName();
        // There may be some non-WeaponType quirks, specifically melee weapons
        if (!(etype instanceof WeaponType) && !etype.hasFlag(MiscType.F_CLUB)) {
            return false;
        } else if (etype.hasFlag(MiscType.F_CLUB)) {
            if (qName.equals(OptionsConstants.QUIRK_WEAP_NEG_AMMO_FEED_PROBLEMS)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_EM_INTERFERENCE)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_NO_COOLING)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_POOR_COOLING)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_STATIC_FEED)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_POS_IMP_COOLING)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_POS_FAST_RELOAD)) {
                return false;
            }
            return true;
        }

        // Anything else is a WeaponType
        assert etype instanceof WeaponType;
        WeaponType wtype = (WeaponType) etype;

        if (!(wtype instanceof AmmoWeapon)) {
            if (qName.equals(OptionsConstants.QUIRK_WEAP_NEG_AMMO_FEED_PROBLEMS)) {
                return false;
            }
            if (qName.equals(OptionsConstants.QUIRK_WEAP_NEG_STATIC_FEED)) {
                return false;
            }
            if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_FAST_RELOAD)) {
                return false;
            }
        }

        if (!(wtype instanceof EnergyWeapon) && qName.equals(OptionsConstants.QUIRK_WEAP_NEG_EM_INTERFERENCE)) {
            return false;
        }

/*        if ((wtype instanceof EnergyWeapon) && qName.equals(OptionsConstants.QUIRK_WEAP_POS_FAST_RELOAD)) {
            return false;
        }*/

        if (en instanceof ProtoMek) {
            if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_FAST_RELOAD)
                || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_STATIC_FEED)) {
                return false;
            }
        }

        boolean coolingQuirks = qName.equals(OptionsConstants.QUIRK_WEAP_POS_IMP_COOLING)
            || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_POOR_COOLING)
            || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_NO_COOLING);
        if (en instanceof Tank || en instanceof BattleArmor || en instanceof ProtoMek) {
            if (coolingQuirks) {
                return false;
            }
        }

        if (en.isConventionalInfantry()) {
            return false;
        }

        if (wtype.getHeat() == 0) {
            if (coolingQuirks) {
                return false;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_JETTISON_CAPABLE)) {
            if (en instanceof ProtoMek
                || en instanceof Aero
                || en instanceof GunEmplacement)  {

                return false;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_MOD_WEAPONS)) {
            if ((en instanceof ProtoMek) || (en instanceof Jumpship)) {
                return false;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_DIRECT_TORSO_MOUNT)) {
            if ((en instanceof Aero) || (en instanceof BattleArmor) || (en instanceof Tank)) {
                return false;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_STABLE_WEAPON)) {
            if (en instanceof Aero) {
                return false;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAP_NEG_EXPOSED_LINKAGE)) {
            if (en instanceof Aero) {
                return false;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAP_NEG_EM_INTERFERENCE)) {
            return !(en instanceof Jumpship);
        }

        return true;
    }

    @Override
    public Map<String, IOption> getOptionsHash() {
        return Map.of();
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

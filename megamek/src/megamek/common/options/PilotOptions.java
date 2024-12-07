/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common.options;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

/**
 * Contains the options determining abilities of the pilot
 *
 * @author Cord
 */
public class PilotOptions extends AbstractOptions {
    private static final long serialVersionUID = 6628080570425023949L;
    public static final String LVL3_ADVANTAGES = "lvl3Advantages";
    public static final String EDGE_ADVANTAGES = "edgeAdvantages";
    public static final String MD_ADVANTAGES = "MDAdvantages";

    private static final Object[][] ADV_OPTIONS = {
        { OptionsConstants.PILOT_ANIMAL_MIMIC, false },
        { OptionsConstants.PILOT_CROSS_COUNTRY, false },
        { OptionsConstants.PILOT_DODGE_MANEUVER, false },
        { OptionsConstants.PILOT_HVY_LIFTER, false },
        { OptionsConstants.PILOT_HOPPING_JACK, false },
        { OptionsConstants.PILOT_HOT_DOG, false },
        { OptionsConstants.PILOT_JUMPING_JACK, false },
        { OptionsConstants.PILOT_MANEUVERING_ACE, false },
        { OptionsConstants.PILOT_MELEE_MASTER, false },
        { OptionsConstants.PILOT_MELEE_SPECIALIST, false },
        { OptionsConstants.PILOT_APTITUDE_PILOTING, false },
        { OptionsConstants.PILOT_SHAKY_STICK, false },
        { OptionsConstants.PILOT_TM_FOREST_RANGER, false },
        { OptionsConstants.PILOT_TM_FROGMAN, false },
        { OptionsConstants.PILOT_TM_MOUNTAINEER, false },
        { OptionsConstants.PILOT_TM_NIGHTWALKER, false },
        { OptionsConstants.PILOT_TM_SWAMP_BEAST, false },
        { OptionsConstants.PILOT_ZWEIHANDER, false },

        // Gunnery Abilities
        { OptionsConstants.GUNNERY_BLOOD_STALKER, false },
        { OptionsConstants.GUNNERY_CLUSTER_HITTER, false },
        { OptionsConstants.GUNNERY_CLUSTER_MASTER, false },
        { OptionsConstants.GUNNERY_GOLDEN_GOOSE, false },
        { OptionsConstants.GUNNERY_SPECIALIST, new Vector<String>() },
        { OptionsConstants.GUNNERY_MULTI_TASKER, false },
        { OptionsConstants.PILOT_APTITUDE_GUNNERY, false },
        { OptionsConstants.GUNNERY_OBLIQUE_ARTILLERY, false },
        { OptionsConstants.GUNNERY_OBLIQUE_ATTACKER, false },
        { OptionsConstants.GUNNERY_RANGE_MASTER, new Vector<String>() },
        { OptionsConstants.GUNNERY_SANDBLASTER, new Vector<String>() },
        { OptionsConstants.GUNNERY_SNIPER, false },
        { OptionsConstants.GUNNERY_WEAPON_SPECIALIST, new Vector<String>() },

        // Misc Abilities
        { OptionsConstants.MISC_EAGLE_EYES, false },
        { OptionsConstants.MISC_ENV_SPECIALIST, new Vector<String>() },
        { OptionsConstants.MISC_FORWARD_OBSERVER, false },
        { OptionsConstants.MISC_HUMAN_TRO, new Vector<String>() },
        { OptionsConstants.MISC_IRON_MAN, false },
        { OptionsConstants.MISC_PAIN_RESISTANCE, false },
        { OptionsConstants.MISC_TACTICAL_GENIUS, false },

        // Infantry abilities
        { OptionsConstants.INFANTRY_FOOT_CAV, false },
        { OptionsConstants.INFANTRY_URBAN_GUERRILLA, false },

        // Unofficial
        { OptionsConstants.UNOFF_EI_IMPLANT, false },
        { OptionsConstants.UNOFF_GUNNERY_LASER, false },
        { OptionsConstants.UNOFF_GUNNERY_MISSILE, false },
        { OptionsConstants.UNOFF_GUNNERY_BALLISTIC, false },
        { OptionsConstants.UNOFF_CLAN_PILOT_TRAINING, false },
        { OptionsConstants.UNOFF_SOME_LIKE_IT_HOT, false },
        { OptionsConstants.UNOFF_WEATHERED, false },
        { OptionsConstants.UNOFF_ALLWEATHER, false },
        { OptionsConstants.UNOFF_BLIND_FIGHTER, false },
        { OptionsConstants.UNOFF_SENSOR_GEEK, false },
        { OptionsConstants.UNOFF_SMALL_PILOT, false },
    };

    private static final Object[][] EDGE_OPTIONS = {
        { OptionsConstants.EDGE, 0 },
        { OptionsConstants.EDGE_WHEN_HEADHIT, true },
        { OptionsConstants.EDGE_WHEN_TAC, true },
        { OptionsConstants.EDGE_WHEN_KO, true },
        { OptionsConstants.EDGE_WHEN_EXPLOSION, true },
        { OptionsConstants.EDGE_WHEN_MASC_FAILS, true },
        { OptionsConstants.EDGE_WHEN_AERO_ALT_LOSS, true },
        { OptionsConstants.EDGE_WHEN_AERO_EXPLOSION, true },
        { OptionsConstants.EDGE_WHEN_AERO_KO, true },
        { OptionsConstants.EDGE_WHEN_AERO_LUCKY_CRIT, true },
        { OptionsConstants.EDGE_WHEN_AERO_NUKE_CRIT, true },
        { OptionsConstants.EDGE_WHEN_AERO_UNIT_CARGO_LOST, true }
    };

    private static final Object[][] MD_OPTIONS = {
        { OptionsConstants.MD_PAIN_SHUNT, false },
        { OptionsConstants.MD_COMM_IMPLANT, false },
        { OptionsConstants.MD_BOOST_COMM_IMPLANT, false },
        { OptionsConstants.MD_CYBER_IMP_AUDIO, false },
        { OptionsConstants.MD_CYBER_IMP_VISUAL, false },
        { OptionsConstants.MD_CYBER_IMP_LASER, false },
        { OptionsConstants.MD_MM_IMPLANTS, false },
        { OptionsConstants.MD_ENH_MM_IMPLANTS, false },
        { OptionsConstants.MD_FILTRATION, false },
        { OptionsConstants.MD_GAS_EFFUSER_PHERO, false },
        { OptionsConstants.MD_GAS_EFFUSER_TOXIN, false },
        { OptionsConstants.MD_DERMAL_ARMOR, false },
        { OptionsConstants.MD_DERMAL_CAMO_ARMOR, false },
        { OptionsConstants.MD_TSM_IMPLANT, false },
        { OptionsConstants.MD_TRIPLE_CORE_PROCESSOR, false },
        { OptionsConstants.MD_VDNI, false },
        { OptionsConstants.MD_BVDNI, false },
        { OptionsConstants.MD_PROTO_DNI, false },
        { OptionsConstants.MD_PL_ENHANCED, false },
        { OptionsConstants.MD_PL_IENHANCED, false },
        { OptionsConstants.MD_PL_EXTRA_LIMBS, false },
        { OptionsConstants.MD_PL_TAIL, false },
        { OptionsConstants.MD_PL_MASC, false },
        { OptionsConstants.MD_PL_GLIDER, false },
        { OptionsConstants.MD_PL_FLIGHT, false },
        { OptionsConstants.MD_SUICIDE_IMPLANTS, false }
    };

    public PilotOptions() {
        super();
    }

    @Override
    public void initialize() {
        // Pre-size optionsHash (just a guess: total options * 2)
        int totalOptions = ADV_OPTIONS.length + EDGE_OPTIONS.length + MD_OPTIONS.length;
        optionsHash = new Hashtable<>(totalOptions * 2);
        var optionsInfo = PilotOptionsInfo.getInstance();
        IBasicOptionGroup advGroup = optionsInfo.addGroup(OptionsConstants.ADV, LVL3_ADVANTAGES);
        IBasicOptionGroup edgeGroup = optionsInfo.addGroup(OptionsConstants.EDGE, EDGE_ADVANTAGES);
        IBasicOptionGroup mdGroup = optionsInfo.addGroup(OptionsConstants.MD, MD_ADVANTAGES);

        addOptions(optionsInfo, advGroup, ADV_OPTIONS);
        addOptions(optionsInfo, edgeGroup, EDGE_OPTIONS);
        addOptions(optionsInfo, mdGroup, MD_OPTIONS);
    }

    private void addOptions(AbstractOptionsInfo optionsInfo, IBasicOptionGroup group, Object[][] options) {
        for (Object[] entry : options) {
            String name = (String) entry[0];
            Object defaultValue = entry[1];

            if (defaultValue instanceof Boolean) {
                addOption(optionsInfo, group, name, IOption.BOOLEAN, defaultValue);
            } else if (defaultValue instanceof Integer) {
                addOption(optionsInfo, group, name, IOption.INTEGER, defaultValue);
            } else if (defaultValue instanceof Vector) {
                addOption(optionsInfo, group, name, IOption.CHOICE, "");
            } else {
                // fallback to boolean if not recognized, or handle other types as needed
                addOption(optionsInfo, group, name, IOption.BOOLEAN, defaultValue);
            }
        }
    }

    protected void addOption(AbstractOptionsInfo optionsInfo, IBasicOptionGroup group, String name, int type, Object defaultValue) {
        optionsHash.put(name, new Option(this, name, type, defaultValue));
        optionsInfo.addOptionInfo(group, name);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return PilotOptionsInfo.getInstance();
    }

    @Override
    public Map<String, IOption> getOptionsHash() {
        return Map.of();
    }

    private static class PilotOptionsInfo extends AbstractOptionsInfo {
        private static boolean initialized = false;
        private static final AbstractOptionsInfo instance = new PilotOptionsInfo();

        public static AbstractOptionsInfo getInstance() {
            if (!initialized) {
                initialized = true;
                // Create a new dummy PilotOptions; ensures values initialized
                // Otherwise, could have issues when loading saved games
                new PilotOptions();
            }
            return instance;
        }

        protected PilotOptionsInfo() {
            super("PilotOptionsInfo");
        }
    }
}

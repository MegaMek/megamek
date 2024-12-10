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

import java.io.Serial;
import java.util.Map;
import java.util.Vector;

/**
 * Contains the options determining abilities of the pilot
 *
 * @author Cord
 */
public class PilotOptions extends AbstractOptions {

    @Serial
    private static final long serialVersionUID = 6628080570425023949L;
    public static final String LVL3_ADVANTAGES = "lvl3Advantages";
    public static final String EDGE_ADVANTAGES = "edgeAdvantages";
    public static final String MD_ADVANTAGES = "MDAdvantages";

    private static final Option.OptionValue[] ADV_OPTIONS = {
        Option.of(OptionsConstants.PILOT_ANIMAL_MIMIC, false ),
        Option.of(OptionsConstants.PILOT_CROSS_COUNTRY, false ),
        Option.of(OptionsConstants.PILOT_DODGE_MANEUVER, false ),
        Option.of(OptionsConstants.PILOT_HVY_LIFTER, false ),
        Option.of(OptionsConstants.PILOT_HOPPING_JACK, false ),
        Option.of(OptionsConstants.PILOT_HOT_DOG, false ),
        Option.of(OptionsConstants.PILOT_JUMPING_JACK, false ),
        Option.of(OptionsConstants.PILOT_MANEUVERING_ACE, false ),
        Option.of(OptionsConstants.PILOT_MELEE_MASTER, false ),
        Option.of(OptionsConstants.PILOT_MELEE_SPECIALIST, false ),
        Option.of(OptionsConstants.PILOT_APTITUDE_PILOTING, false ),
        Option.of(OptionsConstants.PILOT_SHAKY_STICK, false ),
        Option.of(OptionsConstants.PILOT_TM_FOREST_RANGER, false ),
        Option.of(OptionsConstants.PILOT_TM_FROGMAN, false ),
        Option.of(OptionsConstants.PILOT_TM_MOUNTAINEER, false ),
        Option.of(OptionsConstants.PILOT_TM_NIGHTWALKER, false ),
        Option.of(OptionsConstants.PILOT_TM_SWAMP_BEAST, false ),
        Option.of(OptionsConstants.PILOT_ZWEIHANDER, false ),

        // Gunnery Abilities
        Option.of(OptionsConstants.GUNNERY_BLOOD_STALKER, false ),
        Option.of(OptionsConstants.GUNNERY_CLUSTER_HITTER, false ),
        Option.of(OptionsConstants.GUNNERY_CLUSTER_MASTER, false ),
        Option.of(OptionsConstants.GUNNERY_GOLDEN_GOOSE, false ),
        Option.of(OptionsConstants.GUNNERY_SPECIALIST, new Vector<String>() ),
        Option.of(OptionsConstants.GUNNERY_MULTI_TASKER, false ),
        Option.of(OptionsConstants.PILOT_APTITUDE_GUNNERY, false ),
        Option.of(OptionsConstants.GUNNERY_OBLIQUE_ARTILLERY, false ),
        Option.of(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER, false ),
        Option.of(OptionsConstants.GUNNERY_RANGE_MASTER, new Vector<String>() ),
        Option.of(OptionsConstants.GUNNERY_SANDBLASTER, new Vector<String>() ),
        Option.of(OptionsConstants.GUNNERY_SNIPER, false ),
        Option.of(OptionsConstants.GUNNERY_WEAPON_SPECIALIST, new Vector<String>() ),

        // Misc Abilities
        Option.of(OptionsConstants.MISC_EAGLE_EYES, false ),
        Option.of(OptionsConstants.MISC_ENV_SPECIALIST, new Vector<String>() ),
        Option.of(OptionsConstants.MISC_FORWARD_OBSERVER, false ),
        Option.of(OptionsConstants.MISC_HUMAN_TRO, new Vector<String>() ),
        Option.of(OptionsConstants.MISC_IRON_MAN, false ),
        Option.of(OptionsConstants.MISC_PAIN_RESISTANCE, false ),
        Option.of(OptionsConstants.MISC_TACTICAL_GENIUS, false ),

        // Infantry abilities
        Option.of(OptionsConstants.INFANTRY_FOOT_CAV, false ),
        Option.of(OptionsConstants.INFANTRY_URBAN_GUERRILLA, false ),

        // Unofficial
        Option.of(OptionsConstants.UNOFF_EI_IMPLANT, false ),
        Option.of(OptionsConstants.UNOFF_GUNNERY_LASER, false ),
        Option.of(OptionsConstants.UNOFF_GUNNERY_MISSILE, false ),
        Option.of(OptionsConstants.UNOFF_GUNNERY_BALLISTIC, false ),
        Option.of(OptionsConstants.UNOFF_CLAN_PILOT_TRAINING, false ),
        Option.of(OptionsConstants.UNOFF_SOME_LIKE_IT_HOT, false ),
        Option.of(OptionsConstants.UNOFF_WEATHERED, false ),
        Option.of(OptionsConstants.UNOFF_ALLWEATHER, false ),
        Option.of(OptionsConstants.UNOFF_BLIND_FIGHTER, false ),
        Option.of(OptionsConstants.UNOFF_SENSOR_GEEK, false ),
        Option.of(OptionsConstants.UNOFF_SMALL_PILOT, false )
    };

    private static final Option.OptionValue[] EDGE_OPTIONS = {
        Option.of(OptionsConstants.EDGE, 0 ),
        Option.of(OptionsConstants.EDGE_WHEN_HEADHIT, true ),
        Option.of(OptionsConstants.EDGE_WHEN_TAC, true ),
        Option.of(OptionsConstants.EDGE_WHEN_KO, true ),
        Option.of(OptionsConstants.EDGE_WHEN_EXPLOSION, true ),
        Option.of(OptionsConstants.EDGE_WHEN_MASC_FAILS, true ),
        Option.of(OptionsConstants.EDGE_WHEN_AERO_ALT_LOSS, true ),
        Option.of(OptionsConstants.EDGE_WHEN_AERO_EXPLOSION, true ),
        Option.of(OptionsConstants.EDGE_WHEN_AERO_KO, true ),
        Option.of(OptionsConstants.EDGE_WHEN_AERO_LUCKY_CRIT, true ),
        Option.of(OptionsConstants.EDGE_WHEN_AERO_NUKE_CRIT, true ),
        Option.of(OptionsConstants.EDGE_WHEN_AERO_UNIT_CARGO_LOST, true)
    };

    private static final Option.OptionValue[] MD_OPTIONS = {
        Option.of(OptionsConstants.MD_PAIN_SHUNT, false ),
        Option.of(OptionsConstants.MD_COMM_IMPLANT, false ),
        Option.of(OptionsConstants.MD_BOOST_COMM_IMPLANT, false ),
        Option.of(OptionsConstants.MD_CYBER_IMP_AUDIO, false ),
        Option.of(OptionsConstants.MD_CYBER_IMP_VISUAL, false ),
        Option.of(OptionsConstants.MD_CYBER_IMP_LASER, false ),
        Option.of(OptionsConstants.MD_MM_IMPLANTS, false ),
        Option.of(OptionsConstants.MD_ENH_MM_IMPLANTS, false ),
        Option.of(OptionsConstants.MD_FILTRATION, false ),
        Option.of(OptionsConstants.MD_GAS_EFFUSER_PHERO, false ),
        Option.of(OptionsConstants.MD_GAS_EFFUSER_TOXIN, false ),
        Option.of(OptionsConstants.MD_DERMAL_ARMOR, false ),
        Option.of(OptionsConstants.MD_DERMAL_CAMO_ARMOR, false ),
        Option.of(OptionsConstants.MD_TSM_IMPLANT, false ),
        Option.of(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR, false ),
        Option.of(OptionsConstants.MD_VDNI, false ),
        Option.of(OptionsConstants.MD_BVDNI, false ),
        Option.of(OptionsConstants.MD_PROTO_DNI, false ),
        Option.of(OptionsConstants.MD_PL_ENHANCED, false ),
        Option.of(OptionsConstants.MD_PL_IENHANCED, false ),
        Option.of(OptionsConstants.MD_PL_EXTRA_LIMBS, false ),
        Option.of(OptionsConstants.MD_PL_TAIL, false ),
        Option.of(OptionsConstants.MD_PL_MASC, false ),
        Option.of(OptionsConstants.MD_PL_GLIDER, false ),
        Option.of(OptionsConstants.MD_PL_FLIGHT, false ),
        Option.of(OptionsConstants.MD_SUICIDE_IMPLANTS, false)
    };

    public PilotOptions() {
        super();
    }

    @Override
    public void initialize() {
        var optionsInfo = PilotOptionsInfo.getInstance();
        IBasicOptionGroup advGroup = optionsInfo.addGroup(OptionsConstants.ADV, LVL3_ADVANTAGES);
        IBasicOptionGroup edgeGroup = optionsInfo.addGroup(OptionsConstants.EDGE, EDGE_ADVANTAGES);
        IBasicOptionGroup mdGroup = optionsInfo.addGroup(OptionsConstants.MD, MD_ADVANTAGES);

        addOptions(optionsInfo, advGroup, ADV_OPTIONS);
        addOptions(optionsInfo, edgeGroup, EDGE_OPTIONS);
        addOptions(optionsInfo, mdGroup, MD_OPTIONS);
    }

    private void addOptions(AbstractOptionsInfo optionsInfo, IBasicOptionGroup group, Option.OptionValue[] options) {
        for (var entry : options) {
            optionsHash.put(entry.getName(), new Option(this, entry.getName(), entry.getType(), entry.getValue()));
            optionsInfo.addOptionInfo(group, entry.getName());
        }
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

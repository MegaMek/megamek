/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
    public static final String BTM_FACTION_ABILITIES = "BTM_FactionAbilities";
    public static final String BTM_SPECIAL_PILOT_ABILITIES = "BTM_SpecialPilotAbilities";

    public PilotOptions() {
        super();
    }

    @Override
    public void initialize() {
        IBasicOptionGroup adv = addGroup("adv", LVL3_ADVANTAGES);

        addOption(adv, OptionsConstants.PILOT_ANIMAL_MIMIC, false);
        addOption(adv, OptionsConstants.PILOT_CROSS_COUNTRY, false);
        addOption(adv, OptionsConstants.PILOT_DODGE_MANEUVER, false);
        // addOption(adv, OptionsConstants.PILOT_DUST_OFF, false);
        addOption(adv, OptionsConstants.PILOT_HVY_LIFTER, false);
        // addOption(adv, OptionsConstants.PILOT_HOPPER, false);
        addOption(adv, OptionsConstants.PILOT_HOPPING_JACK, false);
        addOption(adv, OptionsConstants.PILOT_HOT_DOG, false);
        addOption(adv, OptionsConstants.PILOT_JUMPING_JACK, false);
        addOption(adv, OptionsConstants.PILOT_MANEUVERING_ACE, false);
        addOption(adv, OptionsConstants.PILOT_MELEE_MASTER, false);
        addOption(adv, OptionsConstants.PILOT_MELEE_SPECIALIST, false);
        addOption(adv, OptionsConstants.PILOT_APTITUDE_PILOTING, false);
        // addOption(adv, OptionsConstants.PILOT_NATURAL_GRACE, false);
        // addOption(adv, OptionsConstants.PILOT_RIDE_WASH, false);
        addOption(adv, OptionsConstants.PILOT_SHAKY_STICK, false);
        // addOption(adv, OptionsConstants.PILOT_SLUGGER, false);
        // addOption(adv, OptionsConstants.PILOT_SPEED_DEMON, false);
        // addOption(adv, OptionsConstants.PILOT_STAND_ASIDE, false);
        // addOption(adv, OptionsConstants.PILOT_SWORDSMAN, false);
        // addOption(adv, OptionsConstants.PILOT_TM_, false);
        addOption(adv, OptionsConstants.PILOT_TM_FOREST_RANGER, false);
        addOption(adv, OptionsConstants.PILOT_TM_FROGMAN, false);
        addOption(adv, OptionsConstants.PILOT_TM_MOUNTAINEER, false);
        addOption(adv, OptionsConstants.PILOT_TM_NIGHTWALKER, false);
        addOption(adv, OptionsConstants.PILOT_TM_SWAMP_BEAST, false);
        // addOption(adv, OptionsConstants.PILOT_WIND_WALKER, false);
        addOption(adv, OptionsConstants.PILOT_ZWEIHANDER, false);

        // Gunnery Abilities
        addOption(adv, OptionsConstants.GUNNERY_BLOOD_STALKER, false);
        addOption(adv, OptionsConstants.GUNNERY_CLUSTER_HITTER, false);
        addOption(adv, OptionsConstants.GUNNERY_CLUSTER_MASTER, false);
        // addOption(adv, OptionsConstants.GUNNERY_FIST_FIRE, false);
        addOption(adv, OptionsConstants.GUNNERY_GOLDEN_GOOSE, false);
        // addOption(adv, OptionsConstants.GUNNERY_GROUND_HUGGER, false);
        addOption(adv, OptionsConstants.GUNNERY_SPECIALIST, new Vector<>());
        // addOption(adv, OptionsConstants.GUNNERY_MARKSMAN, false);
        addOption(adv, OptionsConstants.GUNNERY_MULTI_TASKER, false);
        addOption(adv, OptionsConstants.PILOT_APTITUDE_GUNNERY, false);
        addOption(adv, OptionsConstants.GUNNERY_OBLIQUE_ARTILLERY, false);
        addOption(adv, OptionsConstants.GUNNERY_OBLIQUE_ATTACKER, false);
        addOption(adv, OptionsConstants.GUNNERY_RANGE_MASTER, new Vector<>());
        addOption(adv, OptionsConstants.GUNNERY_SANDBLASTER, new Vector<>());
        // addOption(adv, OptionsConstants.GUNNERY_SHARPSHOOTER, false);
        addOption(adv, OptionsConstants.GUNNERY_SNIPER, false);
        addOption(adv, OptionsConstants.GUNNERY_WEAPON_SPECIALIST, new Vector<>());

        // Misc Abilities
        // addOption(adv, OptionsConstants.MISC_ANTAGONIZER, false);
        // addOption(adv, OptionsConstants.MISC_COMBAT_INTUITION, false);
        // addOption(adv, OptionsConstants.MISC_DEMORALIZER, false);
        addOption(adv, OptionsConstants.MISC_EAGLE_EYES, false);
        addOption(adv, OptionsConstants.MISC_ENV_SPECIALIST, new Vector<>());
        addOption(adv, OptionsConstants.MISC_FORWARD_OBSERVER, false);
        addOption(adv, OptionsConstants.MISC_HUMAN_TRO, new Vector<>());
        addOption(adv, OptionsConstants.MISC_IRON_MAN, false);
        addOption(adv, OptionsConstants.MISC_PAIN_RESISTANCE, false);
        addOption(adv, OptionsConstants.MISC_TACTICAL_GENIUS, false);
        addOption(adv, OptionsConstants.ATOW_COMBAT_SENSE, false);
        addOption(adv, OptionsConstants.ATOW_COMBAT_PARALYSIS, false);

        // Infantry abilities - Only one until beast mounts are implemented
        addOption(adv, OptionsConstants.INFANTRY_FOOT_CAV, false);
        addOption(adv, OptionsConstants.INFANTRY_URBAN_GUERRILLA, false);

        // Unofficial
        addOption(adv, OptionsConstants.UNOFF_EI_IMPLANT, false);
        addOption(adv, OptionsConstants.UNOFF_GUNNERY_LASER, false);
        addOption(adv, OptionsConstants.UNOFF_GUNNERY_MISSILE, false);
        addOption(adv, OptionsConstants.UNOFF_GUNNERY_BALLISTIC, false);
        addOption(adv, OptionsConstants.UNOFF_CLAN_PILOT_TRAINING, false);
        addOption(adv, OptionsConstants.UNOFF_SOME_LIKE_IT_HOT, false);
        addOption(adv, OptionsConstants.UNOFF_WEATHERED, false);
        addOption(adv, OptionsConstants.UNOFF_ALLWEATHER, false);
        addOption(adv, OptionsConstants.UNOFF_BLIND_FIGHTER, false);
        addOption(adv, OptionsConstants.UNOFF_SENSOR_GEEK, false);
        addOption(adv, OptionsConstants.UNOFF_SMALL_PILOT, false);

        IBasicOptionGroup edge = addGroup("edge", EDGE_ADVANTAGES);
        addOption(edge, "edge", 0);
        /* different edge triggers */
        // Mek Triggers
        addOption(edge, "edge_when_headhit", true);
        addOption(edge, "edge_when_tac", true);
        addOption(edge, "edge_when_ko", true);
        addOption(edge, "edge_when_explosion", true);
        addOption(edge, "edge_when_masc_fails", true);
        //Aero Triggers
        addOption(edge, "edge_when_aero_alt_loss", true);
        addOption(edge, "edge_when_aero_explosion", true);
        addOption(edge, "edge_when_aero_ko", true);
        addOption(edge, "edge_when_aero_lucky_crit", true);
        addOption(edge, "edge_when_aero_nuke_crit", true);
        addOption(edge, "edge_when_aero_unit_cargo_lost", true);

        addOption(edge, OptionsConstants.EDGE, 0);
        // different edge triggers
        //Mek Triggers
        addOption(edge, OptionsConstants.EDGE_WHEN_HEADHIT, true);
        addOption(edge, OptionsConstants.EDGE_WHEN_TAC, true);
        addOption(edge, OptionsConstants.EDGE_WHEN_KO, true);
        addOption(edge, OptionsConstants.EDGE_WHEN_EXPLOSION, true);
        addOption(edge, OptionsConstants.EDGE_WHEN_MASC_FAILS, true);
        //Aero Triggers
        addOption(edge, OptionsConstants.EDGE_WHEN_AERO_ALT_LOSS, true);
        addOption(edge, OptionsConstants.EDGE_WHEN_AERO_EXPLOSION, true);
        addOption(edge, OptionsConstants.EDGE_WHEN_AERO_KO, true);
        addOption(edge, OptionsConstants.EDGE_WHEN_AERO_LUCKY_CRIT, true);
        addOption(edge, OptionsConstants.EDGE_WHEN_AERO_NUKE_CRIT, true);
        addOption(edge, OptionsConstants.EDGE_WHEN_AERO_UNIT_CARGO_LOST, true);

        // manei domini
        IBasicOptionGroup md = addGroup("md", MD_ADVANTAGES);
        addOption(md, OptionsConstants.MD_PAIN_SHUNT, false);
        addOption(md, OptionsConstants.MD_COMM_IMPLANT, false);
        //TODO - -1 bonus when spotting for LRMs and moving through mines.
        addOption(md, OptionsConstants.MD_BOOST_COMM_IMPLANT, false);
        //TODO - -1 bonus when spotting for LRMs and moving through mines.
        addOption(md, OptionsConstants.MD_CYBER_IMP_AUDIO, false);
        addOption(md, OptionsConstants.MD_CYBER_IMP_VISUAL, false);
        addOption(md, OptionsConstants.MD_CYBER_IMP_LASER, false);
        addOption(md, OptionsConstants.MD_MM_IMPLANTS, false);
        addOption(md, OptionsConstants.MD_ENH_MM_IMPLANTS, false);
        addOption(md, OptionsConstants.MD_FILTRATION, false);
        addOption(md, OptionsConstants.MD_GAS_EFFUSER_PHERO, false);
        addOption(md, OptionsConstants.MD_GAS_EFFUSER_TOXIN, false);
        addOption(md, OptionsConstants.MD_DERMAL_ARMOR, false);
        addOption(md, OptionsConstants.MD_DERMAL_CAMO_ARMOR, false);
        addOption(md, OptionsConstants.MD_TSM_IMPLANT, false);
        addOption(md, OptionsConstants.MD_TRIPLE_CORE_PROCESSOR, false);
        addOption(md, OptionsConstants.MD_VDNI, false);
        addOption(md, OptionsConstants.MD_BVDNI, false);
        addOption(md, OptionsConstants.MD_PROTO_DNI, false);
        //Prosthetic Limbs (not MD Exclusive)
        addOption(md, OptionsConstants.MD_PL_ENHANCED, false);
        addOption(md, OptionsConstants.MD_PL_IENHANCED, false);
        addOption(md, OptionsConstants.MD_PL_EXTRA_LIMBS, false);
        addOption(md, OptionsConstants.MD_PL_TAIL, false);
        addOption(md, OptionsConstants.MD_PL_MASC, false);
        addOption(md, OptionsConstants.MD_PL_GLIDER, false);
        addOption(md, OptionsConstants.MD_PL_FLIGHT, false);
        addOption(md, OptionsConstants.MD_SUICIDE_IMPLANTS, false);

        //TODO - Prototype DNI IO pg 83

        // BattleTech: Missions - Should this section be a conditional add based on a setting?
        // BattleTech: Missions Faction Abilities
        IBasicOptionGroup btm_fa = addGroup("btm_fa", BTM_FACTION_ABILITIES);

        addOption(btm_fa, OptionsConstants.MISSIONS_FA_FS_TACTICAL_GENIUS, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_FS_TACTICAL_GENIUS_CURRENT, 0);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_FS_CALL_THEM_OUT, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_FS_CALL_THEM_OUT_CURRENT, 0);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_FS_COMBAT_INTUITION, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_CC_FOR_THE_CHANCELLOR, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_CC_AFTER_YOU_I_INSIST, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_CC_FANATICISM, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_CC_FANATICISM_CURRENT, 0);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_FW_PLAYS_WELL_WITH_OTHERS, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_FW_PLAYS_WELL_WITH_OTHERS_CURRENT, 0);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_FW_TAKE_THE_HIT, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_FW_TAKE_THE_HIT_CURRENT, 0);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_FW_SHARE_THE_WEALTH, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_DC_BUSHIDO, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_DC_BUSHIDO_CURRENT, 0);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_DC_FIGHT_ME, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_DC_FIGHT_ME_CURRENT, 0);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_DC_AGGRESSIVE_CHARGER, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_LC_INTIMIDATE, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_LC_INTIMIDATE_CURRENT, 0);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_LC_BULL_RUSH, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_LC_PANIC, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_ME_PAYDAY, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_ME_DOC_WAGON_CONTRACT, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_ME_DOC_WAGON_CONTRACT_CURRENT, 0);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_ME_SHIFTING_LOYALTIES, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_ME_SHIFTING_LOYALTIES_CURRENT, 0);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_CS_I_DOWNLOADED_SARNA, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_CS_PORTABLE_SCANNER, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_CS_THIS_ITS_JUST_SOMETHING_I_HAD_LYING_AROUND, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_PE_SALVAGE_EXPERT, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_PE_FRONTIER_MEDICINE, false);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_PE_FRONTIER_MEDICINE_CURRENT, 0);
        addOption(btm_fa, OptionsConstants.MISSIONS_FA_PE_HOW_ARE_YOU_STILL_ALIVE, false);

        // BattleTech: Missions Special Pilot Abilities
        IBasicOptionGroup btm_spa = addGroup("btm_spa", BTM_SPECIAL_PILOT_ABILITIES);

        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_EDGE_1, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_EDGE_2, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_EDGE_3, false);
        // number of times Edge is taken (0-3)
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_EDGE_USES, 0);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_EDGE_CURRENT, 0);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_LUCK_1, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_LUCK_2, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_LUCK_3, false);
        // number of times Luck is taken (0-3)
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_LUCK_USES, 0);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_LUCK_CURRENT, 0);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_THE_SABOTEUR_1, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_THE_SABOTEUR_2, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_THE_SABOTEUR_3, false);
        // number of times The Saboteur is taken (0-3)
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_THE_SABOTEUR_USES, 0);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_THE_SABOTEUR_CURRENT, 0);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_RAPID_SHOT, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_DODGE, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_SWORDSMAN, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_HATCHETMAN, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_SWEEP_THE_LEG, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_FIELD_REPAIR_KIT_1, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_FIELD_REPAIR_KIT_2, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_FIELD_REPAIR_KIT_3, false);
        // number of times Field Repair Kit is taken (0-3)
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_FIELD_REPAIR_KIT_USES, 0);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_FIELD_REPAIR_KIT_CURRENT, 0);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_LIGHT_MECH_MASTER, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_MEDIUM_MECH_MASTER, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_HEAVY_MECH_MASTER, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_ASSAULT_MECH_MASTER, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_CLUSTER_HITTER, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_NICE_GROUPING, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_OBLIQUE_ATTACKER, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_RANGE_MASTER, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_SAFETY_WHO_NEEDS_IT_AC, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_SAFETY_WHO_NEEDS_IT_LRM, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_SAFETY_WHO_NEEDS_IT_PPC, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_EXTREME_RANGE, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_BURST_OF_SPEED, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_MANEUVERING_ACE, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_NATURAL_GRACE, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_HOT_DOG, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_THE_SWEAT_HELPS_ME_SEE_BETTER, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_TSM_WHO_NEEDS_IT, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_SNIPER, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_MARKSMAN, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_SHARPSHOOTER, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_MELEE_SPECIALIST, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_MELEE_MASTER, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_FIST_FIRE, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_I_AM_A_LEAF_ON_THE_WIND, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_JUMPING_JACK, false);
        addOption(btm_spa, OptionsConstants.MISSIONS_SPA_DONT_LOOK_UP, false);
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

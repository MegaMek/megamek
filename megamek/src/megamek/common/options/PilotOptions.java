/*
 * Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

    public PilotOptions() {
        super();
    }

    @Override
    public void initialize() {
        IBasicOptionGroup adv = addGroup("adv", LVL3_ADVANTAGES);

        addOption(adv, OptionsConstants.PILOT_ANIMAL_MIMIC, false);
        addOption(adv, OptionsConstants.PILOT_CROSS_COUNTRY, false);
        addOption(adv, OptionsConstants.PILOT_DODGE_MANEUVER, false);
        addOption(adv, OptionsConstants.PILOT_HVY_LIFTER, false);
        addOption(adv, OptionsConstants.PILOT_HOPPING_JACK, false);
        addOption(adv, OptionsConstants.PILOT_HOT_DOG, false);
        addOption(adv, OptionsConstants.PILOT_JUMPING_JACK, false);
        addOption(adv, OptionsConstants.PILOT_MANEUVERING_ACE, false);
        addOption(adv, OptionsConstants.PILOT_MELEE_MASTER, false);
        addOption(adv, OptionsConstants.PILOT_MELEE_SPECIALIST, false);
        addOption(adv, OptionsConstants.PILOT_APTITUDE_PILOTING, false);
        addOption(adv, OptionsConstants.PILOT_SHAKY_STICK, false);
        addOption(adv, OptionsConstants.PILOT_TM_FOREST_RANGER, false);
        addOption(adv, OptionsConstants.PILOT_TM_FROGMAN, false);
        addOption(adv, OptionsConstants.PILOT_TM_MOUNTAINEER, false);
        addOption(adv, OptionsConstants.PILOT_TM_NIGHTWALKER, false);
        addOption(adv, OptionsConstants.PILOT_TM_SWAMP_BEAST, false);
        addOption(adv, OptionsConstants.PILOT_ZWEIHANDER, false);
        addOption(adv, OptionsConstants.PILOT_ATOW_G_TOLERANCE, false);

        // Gunnery Abilities
        addOption(adv, OptionsConstants.GUNNERY_BLOOD_STALKER, false);
        addOption(adv, OptionsConstants.GUNNERY_CLUSTER_HITTER, false);
        addOption(adv, OptionsConstants.GUNNERY_CLUSTER_MASTER, false);
        addOption(adv, OptionsConstants.GUNNERY_GOLDEN_GOOSE, false);
        addOption(adv, OptionsConstants.GUNNERY_SPECIALIST, new Vector<>());
        addOption(adv, OptionsConstants.GUNNERY_MULTI_TASKER, false);
        addOption(adv, OptionsConstants.PILOT_APTITUDE_GUNNERY, false);
        addOption(adv, OptionsConstants.GUNNERY_OBLIQUE_ARTILLERY, false);
        addOption(adv, OptionsConstants.GUNNERY_OBLIQUE_ATTACKER, false);
        addOption(adv, OptionsConstants.GUNNERY_RANGE_MASTER, new Vector<>());
        addOption(adv, OptionsConstants.GUNNERY_SANDBLASTER, new Vector<>());
        addOption(adv, OptionsConstants.GUNNERY_SNIPER, false);
        addOption(adv, OptionsConstants.GUNNERY_WEAPON_SPECIALIST, new Vector<>());

        // Misc Abilities
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
        addOption(adv, OptionsConstants.UNOFFICIAL_EI_IMPLANT, false);
        addOption(adv, OptionsConstants.UNOFFICIAL_GUNNERY_LASER, false);
        addOption(adv, OptionsConstants.UNOFFICIAL_GUNNERY_MISSILE, false);
        addOption(adv, OptionsConstants.UNOFFICIAL_GUNNERY_BALLISTIC, false);
        addOption(adv, OptionsConstants.UNOFFICIAL_CLAN_PILOT_TRAINING, false);
        addOption(adv, OptionsConstants.UNOFFICIAL_SOME_LIKE_IT_HOT, false);
        addOption(adv, OptionsConstants.UNOFFICIAL_WEATHERED, false);
        addOption(adv, OptionsConstants.UNOFFICIAL_ALL_WEATHER, false);
        addOption(adv, OptionsConstants.UNOFFICIAL_BLIND_FIGHTER, false);
        addOption(adv, OptionsConstants.UNOFFICIAL_SENSOR_GEEK, false);
        addOption(adv, OptionsConstants.UNOFFICIAL_SMALL_PILOT, false);

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
        addOption(edge, OptionsConstants.EDGE_WHEN_HEAD_HIT, true);
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
        addOption(md, OptionsConstants.MD_BOOST_COMM_IMPLANT, false);
        addOption(md, OptionsConstants.MD_CYBER_IMP_AUDIO, false);
        addOption(md, OptionsConstants.MD_CYBER_IMP_VISUAL, false);
        addOption(md, OptionsConstants.MD_CYBER_IMP_LASER, false);
        addOption(md, OptionsConstants.MD_CYBER_IMP_TELE, false);
        addOption(md, OptionsConstants.MD_MM_IMPLANTS, false);
        addOption(md, OptionsConstants.MD_ENH_MM_IMPLANTS, false);
        addOption(md, OptionsConstants.MD_FILTRATION, false);
        addOption(md, OptionsConstants.MD_GAS_EFFUSER_PHEROMONE, false);
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
        addOption(md, OptionsConstants.MD_PL_I_ENHANCED, false);
        addOption(md, OptionsConstants.MD_PL_EXTRA_LIMBS, false);
        addOption(md, OptionsConstants.MD_PL_TAIL, false);
        addOption(md, OptionsConstants.MD_PL_MASC, false);
        addOption(md, OptionsConstants.MD_PL_GLIDER, false);
        addOption(md, OptionsConstants.MD_PL_FLIGHT, false);
        addOption(md, OptionsConstants.MD_SUICIDE_IMPLANTS, false);
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
        private static volatile PilotOptionsInfo instance;
        private static final Object lock = new Object();

        public static PilotOptionsInfo getInstance() {
            if (instance == null) {
                synchronized (lock) {
                    if (instance == null) {
                        instance = new PilotOptionsInfo();
                        new PilotOptions();
                    }
                }
            }
            return instance;
        }

        protected PilotOptionsInfo() {
            super("PilotOptionsInfo");
        }
    }
}

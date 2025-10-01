/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.File;
import java.util.Vector;

public class SBFRuleOptions extends BasicGameOptions {

    /** Detection and recon rules aka "double-blind", IO BF p.195 */
    public static final String BASE_RECON = "base_recon";

    /** When using detection and recon rules, players on a team share their vision */
    public static final String BASE_TEAM_VISION = "base_team_vision";

    /** Hidden formations, IO BF p.214 */
    public static final String BASE_HIDDEN = "base_hidden";

    /** Allow changes to formation setup, IO BF p.198 */
    public static final String BASE_ADJUST_FORMATIONS = "base_formation_change";

    /** Allow detaching part of a formation, IO BF p.198 */
    public static final String FORM_ALLOW_DETACH = "form_allow_detach";

    /** Allow completely splitting up formations -UNCLEAR IF SEPARATE OPTION-; requires ALLOW_DETACH, IO BF p.198 */
    public static final String FORM_ALLOW_SPLIT = "form_allow_split";

    /** Allow combining any units -UNCLEAR IF SEPARATE OPTION-, IO BF p.198 */
    public static final String FORM_ALLOW_ADHOC = "form_allow_adhoc";

    /** Evading, IO BF p.198 */
    public static final String MOVE_EVASIVE = "move_evasive";

    /** Hull down, IO BF p.199 */
    public static final String MOVE_HULL_DOWN = "move_hulldown";

    /** Sprinting, IO BF p.199 */
    public static final String MOVE_SPRINT = "move_sprint";

    /** Advanced initiative, IO BF p.194 */
    public static final String INIT_MODIFIERS = "init_modifiers";

    /** Advanced initiative, IO BF p.206 */
    //TODO: is this mutually exclusive with INIT_MODIFIERS?
    public static final String INIT_BATTLEFIELD_INT = "init_battlefield_int";

    /** Banking initiative, IO BF p.223 IS AN SCA */
    public static final String INIT_BANKING = "init_banking";

    /** Forcing initiative, IO BF p.223 */
    public static final String INIT_FORCING = "init_forcing";

    @Override
    public synchronized void initialize() {
        super.initialize();

        IBasicOptionGroup base = addGroup("base");
        addOption(base, BASE_RECON, false);
        addOption(base, BASE_TEAM_VISION, true);
        addOption(base, BASE_ADJUST_FORMATIONS, false);
        addOption(base, BASE_RECON, false);

        IBasicOptionGroup init = addGroup("initiative");
        addOption(init, INIT_MODIFIERS, false);
        addOption(init, INIT_BATTLEFIELD_INT, false);

        IBasicOptionGroup move = addGroup("movement");
        addOption(move, MOVE_HULL_DOWN, false);
        addOption(move, MOVE_EVASIVE, false);
        addOption(move, MOVE_SPRINT, false);
    }

    @Override
    public Vector<IOption> loadOptions() {
        return null;
    }

    @Override
    public Vector<IOption> loadOptions(File file, boolean print) {
        return null;
    }

    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return SBFRuleOptionsInfo.getInstance();
    }

    private static class SBFRuleOptionsInfo extends AbstractOptionsInfo {
        private static volatile SBFRuleOptionsInfo instance;
        private static final Object lock = new Object();

        public static SBFRuleOptionsInfo getInstance() {
            if (instance == null) {
                synchronized (lock) {
                    if (instance == null) {
                        instance = new SBFRuleOptionsInfo();
                    }
                }
            }
            return instance;
        }

        protected SBFRuleOptionsInfo() {
            super("SBFRuleOptionsInfo");
        }

    }
}

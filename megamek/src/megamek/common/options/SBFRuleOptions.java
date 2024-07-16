/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.options;

import java.io.File;
import java.util.Vector;

public class SBFRuleOptions extends BasicGameOptions {

    /** Detection and recon rules aka "double blind", IO BF p.195 */
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

    /** Hulldown, IO BF p.199 */
    public static final String MOVE_HULLDOWN = "move_hulldown";

    /** Sprinting, IO BF p.199 */
    public static final String MOVE_SPRINT = "move_sprint";

    /** Advanced initiative, IO BF p.194 */
    public static final String INIT_MODIFIERS = "init_modifiers";

    /** Advanced initiative, IO BF p.206 */
    //TODO: is this mutually exclusive with INIT_MODIFIERS?
    public static final String INIT_BATTLEFIELD_INT = "init_battlefield_int";

    /** Banking initiative, IO BF p.223 IS AN SCA */
//    public static final String INIT_BANKING = "init_banking";

    /** Forcing initiative, IO BF p.223 */
//    public static final String INIT_FORCING = "init_forcing";

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
        addOption(move, MOVE_HULLDOWN, false);
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
        private static final AbstractOptionsInfo instance = new SBFRuleOptionsInfo();

        protected SBFRuleOptionsInfo() {
            super("SBFRuleOptionsInfo");
        }

        public static AbstractOptionsInfo getInstance() {
            return instance;
        }
    }
}

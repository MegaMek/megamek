package megamek.common.rules.totalwarfare;

/*
 * Copyright (C) 2026 James Magnan
 * Copyright (C) 2004-2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.rules.*;

public class TWRulesManager implements RulesManager {
    private TWRulesTarget twRulesTarget = new TWRulesTarget();
    private TWRulesAmmo twRulesAmmo = new TWRulesAmmo();
    private TWRulesC3 twRulesC3 = new TWRulesC3();
    private TWRulesArtillery twRulesArtillery = new TWRulesArtillery();
    private TWRulesArmor twRulesArmor = new TWRulesArmor();
    private TWRulesCharts twRulesCharts = new TWRulesCharts();
    private TWRulesEnvironment twRulesEnvironment = new TWRulesEnvironment();
    private TWRulesEquipment twRulesEquipment = new TWRulesEquipment();
    private TWRulesExplosions twRulesExplosions = new TWRulesExplosions();
    private TWRulesGame twRulesGame = new TWRulesGame();
    private TWRulesMovement twRulesMovement = new TWRulesMovement();
    private TWRulesHeat twRulesHeat = new TWRulesHeat();
    private TWRulesPhysical twRulesPhysical = new TWRulesPhysical();
    private TWRulesPilot twRulesPilot = new TWRulesPilot();
    private TWRulesPSR twRulesPsr = new TWRulesPSR();
    private TWRulesTerrain twRulesTerrain = new TWRulesTerrain();
    private TWRulesUnderwater twRulesUnderwater = new TWRulesUnderwater();
    private TWRulesUnits twRulesUnits = new TWRulesUnits();
    private TWRulesWeapons twRulesWeapons = new TWRulesWeapons();

    public RulesTarget getRulesTarget() {return twRulesTarget;}

    public RulesAmmo getRulesAmmo() {
        return twRulesAmmo;
    }

    public RulesArtillery getRulesArtillery() {
        return twRulesArtillery;
    }

    public RulesArmor getRulesArmor() {
        return twRulesArmor;
    }

    public RulesCharts getRulesCharts() {
        return twRulesCharts;
    }

    public RulesEnvironment getRulesEnvironment() {return twRulesEnvironment;}

    public RulesEquipment getRulesEquipment() {return twRulesEquipment;}

    public RulesC3 getRulesC3() {
        return twRulesC3;
    }

    public RulesExplosions getRulesExplosions() {
        return twRulesExplosions;
    }

    public RulesGame getRulesGame() {
        return twRulesGame;
    }

    public RulesHeat getRulesHeat() {
        return twRulesHeat;
    }

    public RulesMovement getRulesMovement() {
        return twRulesMovement;
    }

    public RulesPhysical getRulesPhysical() {
        return twRulesPhysical;
    }

    public RulesPilot getRulesPilot() {
        return twRulesPilot;
    }

    public RulesPSR getRulesPsr() {
        return twRulesPsr;
    }

    public RulesTerrain getRulesTerrain() {
        return twRulesTerrain;
    }

    public RulesUnderwater getRulesUnderwater() {
        return twRulesUnderwater;
    }

    public RulesUnits getRulesUnits() {return twRulesUnits;}

    public RulesWeapons getRulesWeapons() {return twRulesWeapons;}

}


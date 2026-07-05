package megamek.common.rules.core;

/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

public class CoreRulesManager implements RulesManager {
    private CoreRulesTarget coreRulesTarget = new CoreRulesTarget();
    private CoreRulesAmmo coreRulesAmmo = new CoreRulesAmmo();
    private CoreRulesC3 coreRulesC3 = new CoreRulesC3();
    private CoreRulesArtillery coreRulesArtillery = new CoreRulesArtillery();
    private CoreRulesArmor coreRulesArmor = new CoreRulesArmor();
    private CoreRulesCharts coreRulesCharts = new CoreRulesCharts();
    private CoreRulesEnvironment coreRulesEnvironment = new CoreRulesEnvironment();
    private CoreRulesEquipment coreRulesEquipment = new CoreRulesEquipment();
    private CoreRulesExplosions coreRulesExplosions = new CoreRulesExplosions();
    private CoreRulesGame coreRulesGame = new CoreRulesGame();
    private CoreRulesMovement coreRulesMovement = new CoreRulesMovement();
    private CoreRulesHeat coreRulesHeat = new CoreRulesHeat();
    private CoreRulesPhysical coreRulesPhysical = new CoreRulesPhysical();
    private CoreRulesPilot coreRulesPilot = new CoreRulesPilot();
    private CoreRulesPSR coreRulesPSR = new CoreRulesPSR();
    private CoreRulesTerrain coreRulesTerrain = new CoreRulesTerrain();
    private CoreRulesUnderwater coreRulesUnderwater = new CoreRulesUnderwater();
    private CoreRulesUnits coreRulesUnits = new CoreRulesUnits();
    private CoreRulesWeapons coreRulesWeapons = new CoreRulesWeapons();

    public RulesTarget getRulesTarget() {return coreRulesTarget;}

    public RulesAmmo getRulesAmmo() {return coreRulesAmmo;}

    public RulesC3 getRulesC3() {return coreRulesC3;}

    public RulesArtillery getRulesArtillery() {
        return coreRulesArtillery;
    }

    public RulesArmor getRulesArmor() {
        return coreRulesArmor;
    }

    public RulesCharts getRulesCharts() {
        return coreRulesCharts;
    }

    public RulesEnvironment getRulesEnvironment() {return coreRulesEnvironment;}

    public RulesEquipment getRulesEquipment() {return coreRulesEquipment;}

    public RulesExplosions getRulesExplosions() {
        return coreRulesExplosions;
    }

    public RulesGame getRulesGame() {
        return coreRulesGame;
    }

    public RulesHeat getRulesHeat() {
        return coreRulesHeat;
    }

    public RulesMovement getRulesMovement() {
        return coreRulesMovement;
    }

    public RulesPhysical getRulesPhysical() {
        return coreRulesPhysical;
    }

    public RulesPilot getRulesPilot() {
        return coreRulesPilot;
    }

    public RulesPSR getRulesPsr() {
        return coreRulesPSR;
    }

    public RulesTerrain getRulesTerrain() {
        return coreRulesTerrain;
    }

    public RulesUnderwater getRulesUnderwater() {
        return coreRulesUnderwater;
    }

    public RulesUnits getRulesUnits() {return coreRulesUnits;}

    public RulesWeapons getRulesWeapons() {return coreRulesWeapons;}
}

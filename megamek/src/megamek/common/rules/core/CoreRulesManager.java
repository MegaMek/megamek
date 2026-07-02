package megamek.common.rules.core;

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

import megamek.common.rules.RulesManager;
import megamek.common.rules.RulesTarget;
import megamek.common.rules.*;
import megamek.common.rules.core.*;

public class CoreRulesManager implements RulesManager {
    private CoreRulesTarget rulesTarget = new CoreRulesTarget();
    private CoreRulesAmmo CoreRulesAmmo = new CoreRulesAmmo();
    private CoreRulesC3 CoreRulesC3 = new CoreRulesC3();
    private CoreRulesArtillery CoreRulesArtillery = new CoreRulesArtillery();
    private CoreRulesArmor CoreRulesArmor = new CoreRulesArmor();
    private CoreRulesCharts CoreRulesCharts = new CoreRulesCharts();
    private CoreRulesEnvironment CoreRulesEnvironment = new CoreRulesEnvironment();
    private CoreRulesEquipment CoreRulesEquipment = new CoreRulesEquipment();
    private CoreRulesExplosions CoreRulesExplosions = new CoreRulesExplosions();
    private CoreRulesGame CoreRulesGame = new CoreRulesGame();
    private CoreRulesMovement CoreRulesMovement = new CoreRulesMovement();
    private CoreRulesHeat CoreRulesHeat = new CoreRulesHeat();
    private CoreRulesPhysical CoreRulesPhysical = new CoreRulesPhysical();
    private CoreRulesPilot CoreRulesPilot = new CoreRulesPilot();
    private CoreRulesPsr CoreRulesPsr = new CoreRulesPsr();
    private CoreRulesTerrain CoreRulesTerrain = new CoreRulesTerrain();
    private CoreRulesUnderwater CoreRulesUnderwater = new CoreRulesUnderwater();
    private CoreRulesUnits CoreRulesUnits = new CoreRulesUnits();
    private CoreRulesWeapons CoreRulesWeapons = new CoreRulesWeapons();

    public RulesTarget getRulesTarget() { return rulesTarget;}
    public RulesAmmo getRulesAmmo() { return CoreRulesAmmo;}
    public RulesC3 getRulesC3() { return CoreRulesC3;}

    public RulesArtillery getRulesArtillery() {
        return CoreRulesArtillery;
    }

    public RulesArmor getRulesArmor() {
        return CoreRulesArmor;
    }

    public RulesCharts getRulesCharts() {
        return CoreRulesCharts;
    }
    public RulesEnvironment getRulesEnvironment() { return CoreRulesEnvironment;}

    public RulesEquipment getRulesEquipment() { return CoreRulesEquipment;}

    public RulesExplosions getRulesExplosions() {
        return CoreRulesExplosions;
    }

    public RulesGame getRulesGame() {
        return CoreRulesGame;
    }

    public RulesHeat getRulesHeat() {
        return CoreRulesHeat;
    }

    public RulesMovement getRulesMovement() {
        return CoreRulesMovement;
    }

    public RulesPhysical getRulesPhysical() {
        return CoreRulesPhysical;
    }

    public RulesPilot getRulesPilot() {
        return CoreRulesPilot;
    }

    public RulesPsr getRulesPsr() {
        return CoreRulesPsr;
    }

    public RulesTerrain getRulesTerrain() {
        return CoreRulesTerrain;
    }
    public RulesUnderwater getRulesUnderwater() {
        return CoreRulesUnderwater;
    }

    public RulesUnits getRulesUnits() {return CoreRulesUnits;}

    public RulesWeapons getRulesWeapons() {return CoreRulesWeapons;}
}

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

import java.util.Set;

import megamek.common.options.OptionsConstants;
import megamek.common.rules.*;

public class CoreRulesManager implements RulesManager {
    /**
     * Game options the Core Rules ignore: the Core rules classes give the same result whether each is on or off, so the
     * Game Options dialog makes them read-only under Core. An option the Core rules classes read, such as Tactical
     * Operations Expanded Backward Movement for vehicles, must not be listed here, or a value set under Total Warfare
     * (or sent by MekHQ) would apply under Core with no way to change it.
     */
    public static final Set<String> IGNORED_GAME_OPTIONS = Set.of(
          OptionsConstants.BASE_FLAMER_HEAT,
          OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AMS,
          OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CHARGE_DAMAGE,
          OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RETRACTABLE_BLADES,
          OptionsConstants.ADVANCED_COMBAT_UNJAM_UAC,
          OptionsConstants.INIT_FRONT_LOAD_INITIATIVE,
          OptionsConstants.ADVANCED_MINEFIELDS,
          OptionsConstants.ADVANCED_ALTERNATE_MASC,
          OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED);

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
    private CoreRulesScanning coreRulesScanning = new CoreRulesScanning();
    private CoreRulesTerrain coreRulesTerrain = new CoreRulesTerrain();
    private CoreRulesUnderwater coreRulesUnderwater = new CoreRulesUnderwater();
    private CoreRulesUnits coreRulesUnits = new CoreRulesUnits();
    private CoreRulesWeapons coreRulesWeapons = new CoreRulesWeapons();

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for targeting.
     */
    public RulesTarget getRulesTarget() { return coreRulesTarget; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for ammunition handling.
     */
    public RulesAmmo getRulesAmmo() { return coreRulesAmmo; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for C3 systems.
     */
    public RulesC3 getRulesC3() { return coreRulesC3; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for artillery.
     */
    public RulesArtillery getRulesArtillery() { return coreRulesArtillery; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for armor interactions.
     */
    public RulesArmor getRulesArmor() { return coreRulesArmor; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for charts and lookup tables.
     */
    public RulesCharts getRulesCharts() { return coreRulesCharts; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for environmental effects.
     */
    public RulesEnvironment getRulesEnvironment() { return coreRulesEnvironment; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for equipment behavior.
     */
    public RulesEquipment getRulesEquipment() { return coreRulesEquipment; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for explosions and blast effects.
     */
    public RulesExplosions getRulesExplosions() { return coreRulesExplosions; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for core game mechanics.
     */
    public RulesGame getRulesGame() { return coreRulesGame; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for heat management.
     */
    public RulesHeat getRulesHeat() { return coreRulesHeat; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for movement.
     */
    public RulesMovement getRulesMovement() { return coreRulesMovement; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for physical attacks.
     */
    public RulesPhysical getRulesPhysical() { return coreRulesPhysical; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for pilot‑related mechanics.
     */
    public RulesPilot getRulesPilot() { return coreRulesPilot; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for Piloting Skill Rolls (PSRs).
     */
    public RulesPSR getRulesPSR() { return coreRulesPSR; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for scanning objectives and units.
     */
    @Override
    public RulesScanning getRulesScanning() { return coreRulesScanning; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for terrain interactions.
     */
    public RulesTerrain getRulesTerrain() { return coreRulesTerrain; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for underwater rules.
     */
    public RulesUnderwater getRulesUnderwater() { return coreRulesUnderwater; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for unit‑specific rules.
     */
    public RulesUnits getRulesUnits() { return coreRulesUnits; }

    /**
     * {@inheritDoc}
     * Returns the Core Rules implementation for weapons.
     */
    public RulesWeapons getRulesWeapons() { return coreRulesWeapons; }

}

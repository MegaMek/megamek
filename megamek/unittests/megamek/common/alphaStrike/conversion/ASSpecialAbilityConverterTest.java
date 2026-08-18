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

package megamek.common.alphaStrike.conversion;

import static megamek.common.alphaStrike.ASUnitType.BM;
import static megamek.common.alphaStrike.ASUnitType.DS;
import static megamek.common.alphaStrike.BattleForceSUA.ENE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.ui.clientGUI.calculationReport.DummyCalculationReport;
import megamek.common.alphaStrike.ASArcs;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.ChargeLevel;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.units.Aero;
import megamek.common.units.BipedMek;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.weapons.gaussRifles.innerSphere.ISGaussRifle;
import megamek.common.weapons.lasers.innerSphere.ISBombastLaser;
import megamek.common.weapons.ppc.innerSphere.ISPPC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ASSpecialAbilityConverterTest {
    
    private RulesManager previousRulesManager;
    @org.junit.jupiter.api.BeforeAll
    static void initializeEquipment() {
        megamek.common.equipment.EquipmentType.initializeTypes();
    }

    @BeforeEach
    void useCoreRules() {
        previousRulesManager = Game.rulesManager;
        Game.rulesManager = new CoreRulesManager();
    }

    @AfterEach
    void restoreRulesManager() {
        Game.rulesManager = previousRulesManager;
    }

    @Test
    void ppcCapacitorDoesNotBlockENE() throws Exception {
        BipedMek mek = new BipedMek();
        addChargedPpcAndCapacitor(mek, Mek.LOC_RIGHT_ARM);
        AlphaStrikeElement element = new AlphaStrikeElement();
        element.setType(BM);

        ASSpecialAbilityConverter.getConverter(mek, element, new DummyCalculationReport()).processENE();

        assertTrue(element.hasSUA(ENE));
    }

    @Test
    void ppcCapacitorDoesNotBlockLargeAerospaceArcENE() throws Exception {
        Dropship dropship = new Dropship();
        addChargedPpcAndCapacitor(dropship, Aero.LOC_NOSE);
        AlphaStrikeElement element = new AlphaStrikeElement();
        element.setType(DS);

        ASSpecialAbilityConverter.getConverter(dropship, element, new DummyCalculationReport()).processENE();

        assertTrue(element.getArc(ASArcs.FRONT).hasSUA(ENE));
    }

    @Test
    void bombastLaserDoesNotBlockENE() throws Exception {
        BipedMek mek = new BipedMek();
        addChargedBombastLaser(mek, Mek.LOC_RIGHT_ARM);
        AlphaStrikeElement element = new AlphaStrikeElement();
        element.setType(BM);

        ASSpecialAbilityConverter.getConverter(mek, element, new DummyCalculationReport()).processENE();

        assertTrue(element.hasSUA(ENE));
    }

    @Test
    void bombastLaserDoesNotBlockLargeAerospaceArcENE() throws Exception {
        Dropship dropship = new Dropship();
        addChargedBombastLaser(dropship, Aero.LOC_NOSE);
        AlphaStrikeElement element = new AlphaStrikeElement();
        element.setType(DS);

        ASSpecialAbilityConverter.getConverter(dropship, element, new DummyCalculationReport()).processENE();

        assertTrue(element.getArc(ASArcs.FRONT).hasSUA(ENE));
    }

    @Test
    void explosiveWeaponStillBlocksENE() throws Exception {
        BipedMek mek = new BipedMek();
        mek.addEquipment(new ISGaussRifle(), Mek.LOC_RIGHT_ARM);
        AlphaStrikeElement element = new AlphaStrikeElement();
        element.setType(BM);

        ASSpecialAbilityConverter.getConverter(mek, element, new DummyCalculationReport()).processENE();

        assertFalse(element.hasSUA(ENE));
    }

    private void addChargedPpcAndCapacitor(Entity entity, int location) throws Exception {
        Mounted<?> ppc = entity.addEquipment(new ISPPC(), location);
        Mounted<?> capacitor = entity.addEquipment(MiscType.createISPPCCapacitor(), location);
        capacitor.setLinked(ppc);
        capacitor.setMode("Charge");
        capacitor.newRound(1);

        assertTrue(ppc.getType().isExplosive(ppc));
        assertTrue(capacitor.getType().isExplosive(capacitor));
    }

    private void addChargedBombastLaser(Entity entity, int location) throws Exception {
        Mounted<?> bombastLaser = entity.addEquipment(new ISBombastLaser(), location);
        bombastLaser.setChargeState(ChargeLevel.CHARGED);

        assertTrue(bombastLaser.getType().isExplosive(bombastLaser));
    }
}

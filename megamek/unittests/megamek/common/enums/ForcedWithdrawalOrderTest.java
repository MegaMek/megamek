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
package megamek.common.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.BipedMek;
import megamek.common.util.SerializationHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link ForcedWithdrawalOrder}, the gamemaster's standing order on whether a bot unit withdraws, and for
 * the order surviving a savegame.
 */
class ForcedWithdrawalOrderTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    /**
     * Every order against every combination of the bot's setting and the unit's condition.
     */
    static Stream<Arguments> orderTable() {
        return Stream.of(
              // no order: the bot's own rule decides, so only a crippled unit of a rule-following bot withdraws
              Arguments.of(ForcedWithdrawalOrder.BOT_RULES, true, true, true),
              Arguments.of(ForcedWithdrawalOrder.BOT_RULES, true, false, false),
              Arguments.of(ForcedWithdrawalOrder.BOT_RULES, false, true, false),
              Arguments.of(ForcedWithdrawalOrder.BOT_RULES, false, false, false),
              // withdraw now: always, even unhurt and even for a bot that ignores the rule
              Arguments.of(ForcedWithdrawalOrder.WITHDRAW, true, true, true),
              Arguments.of(ForcedWithdrawalOrder.WITHDRAW, true, false, true),
              Arguments.of(ForcedWithdrawalOrder.WITHDRAW, false, true, true),
              Arguments.of(ForcedWithdrawalOrder.WITHDRAW, false, false, true),
              // fight to the death: never, even crippled under a rule-following bot
              Arguments.of(ForcedWithdrawalOrder.FIGHT_TO_THE_DEATH, true, true, false),
              Arguments.of(ForcedWithdrawalOrder.FIGHT_TO_THE_DEATH, true, false, false),
              Arguments.of(ForcedWithdrawalOrder.FIGHT_TO_THE_DEATH, false, true, false),
              Arguments.of(ForcedWithdrawalOrder.FIGHT_TO_THE_DEATH, false, false, false));
    }

    @ParameterizedTest
    @MethodSource("orderTable")
    void anOrderDecidesWhetherTheUnitWithdraws(ForcedWithdrawalOrder order, boolean botFollowsForcedWithdrawal,
          boolean isCrippled, boolean expectedToWithdraw) {
        assertEquals(expectedToWithdraw, order.isWithdrawing(botFollowsForcedWithdrawal, isCrippled));
    }

    @Test
    void aNewUnitHasNoOrder() {
        assertEquals(ForcedWithdrawalOrder.BOT_RULES, new BipedMek().getForcedWithdrawalOrder());
    }

    @Test
    void anOrderSurvivesASaveAndLoad() {
        BipedMek mek = new BipedMek();
        mek.setForcedWithdrawalOrder(ForcedWithdrawalOrder.FIGHT_TO_THE_DEATH);

        String savedXml = SerializationHelper.getSaveGameXStream().toXML(mek);
        BipedMek restored = (BipedMek) SerializationHelper.getLoadSaveGameXStream().fromXML(savedXml);

        assertEquals(ForcedWithdrawalOrder.FIGHT_TO_THE_DEATH, restored.getForcedWithdrawalOrder());
    }

    @Test
    void aUnitFromASaveMadeBeforeOrdersExistedHasNoOrder() {
        // Loading skips field initialisers, so a save without the element restores the field as null.
        BipedMek mek = new BipedMek();
        mek.setForcedWithdrawalOrder(ForcedWithdrawalOrder.WITHDRAW);
        String legacyXml = SerializationHelper.getSaveGameXStream().toXML(mek)
              .replaceAll("<forcedWithdrawalOrder>[^<]*</forcedWithdrawalOrder>", "");

        BipedMek restored = (BipedMek) SerializationHelper.getLoadSaveGameXStream().fromXML(legacyXml);

        assertEquals(ForcedWithdrawalOrder.BOT_RULES, restored.getForcedWithdrawalOrder());
    }
}

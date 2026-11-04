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

package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests to ensure failed ammo changes are not broadcast.
 */
public class TWGameManagerAmmoChangeTest {
    private static final int MEK_ID = 1;
    private static final int OWNER_CONNECTION_ID = 0;
    private static final int REASON = 1500;

    private final TWGameManager gameManager = mock(TWGameManager.class);

    private Game game;
    private Entity attacker;

    @BeforeEach
    void setUp() throws Exception {
        var owner = new Player(OWNER_CONNECTION_ID, "Owner");
        owner.setTeam(1);


        game = new Game();
        game.setPhase(GamePhase.FIRING);
        game.addPlayer(OWNER_CONNECTION_ID, owner);

        attacker = createTestMek(MEK_ID, owner);
        game.addEntity(attacker);

        doNothing().when(gameManager).entityUpdate(anyInt());
        when(gameManager.getGame()).thenReturn(game);
        doCallRealMethod().when(gameManager).setGame(any(Game.class));
        doCallRealMethod().when(gameManager).handlePacket(anyInt(), any(Packet.class));
        gameManager.setGame(game);
    }

    @Test
    @DisplayName("Failed ammo swaps do not report")
    void mismatchedAmmoIsRejectedAndNotReported() {
        var srm = attacker.getWeaponList().getFirst();
        var originalAmmo = srm.getLinkedAmmo();
        var lbxClusterAmmo = attacker.getAmmo().get(1);

        sendAmmoChange(attacker, srm, attacker, lbxClusterAmmo);

        assertSame(originalAmmo, srm.getLinkedAmmo());
        verify(gameManager, never()).addReport(any(Report.class));
    }

    private Entity createTestMek(int entityId, Player owner) throws Exception {
        // 1. Instantiate the chassis
        Entity mek = new BipedMek();
        mek.setGame(game);
        mek.setId(entityId);
        mek.setChassis("TestCataphract");
        mek.setModel("TC-1");
        mek.setOwner(owner);

        // 2. Give it a pilot (MegaMek expects a crew to avoid NPEs in status checks)
        mek.setCrew(new Crew(CrewType.SINGLE));

        // 3. Mount the Weapon (SRM-6 in Left Arm)
        var srmType = (WeaponType) EquipmentType.get("ISSRM6");
        mek.addEquipment(srmType, Mek.LOC_LEFT_ARM);

        // 4. Mount the Legal Ammo (SRM-6 Ammo in Left Torso)
        var srmAmmoType = (AmmoType) EquipmentType.get("IS Ammo SRM-6");
        mek.addEquipment(srmAmmoType, Mek.LOC_LEFT_TORSO);

        // 5. Mount the Illegal/Incompatible Ammo (LB 10-X Cluster Ammo in Left Torso)
        AmmoType lbxClusterType = (AmmoType) EquipmentType.get("IS LB 10-X Cluster Ammo");
        mek.addEquipment(lbxClusterType, Mek.LOC_LEFT_TORSO);

        // 6. Automatically link legal weapons to their matching bins
        mek.loadAllWeapons();

        return mek;
    }

    private void sendAmmoChange(Entity shooter, WeaponMounted weapon, Entity ammoCarrier, AmmoMounted ammo) {
        gameManager.handlePacket(
              OWNER_CONNECTION_ID,
              new Packet(PacketCommand.ENTITY_AMMO_CHANGE,
              shooter.getId(),
              shooter.getEquipmentNum(weapon),
              ammoCarrier.getEquipmentNum(ammo),
              ammoCarrier.getId(),
              REASON));
    }
}

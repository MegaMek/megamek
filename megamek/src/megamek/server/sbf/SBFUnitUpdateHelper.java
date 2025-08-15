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

package megamek.server.sbf;

import megamek.common.InGameObject;
import megamek.common.Player;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFIGotSomethingUnitPlaceholder;
import megamek.common.strategicBattleSystems.SBFPartialScanUnitPlaceHolder;
import megamek.common.strategicBattleSystems.SBFSomethingOutThereUnitPlaceHolder;
import megamek.common.strategicBattleSystems.SBFUnitPlaceHolder;
import megamek.common.strategicBattleSystems.SBFVisibilityStatus;

record SBFUnitUpdateHelper(SBFGameManager gameManager) implements SBFGameManagerHelper {

    /**
     * Updates all units to all players, taking into account visibility.
     *
     * @see #sendUnitUpdate(Player, InGameObject)
     */
    void sendAllUnitUpdate() {
        game().getPlayersList().forEach(this::sendAllUnitUpdate);
    }

    /**
     * Updates all units to the given recipient, taking into account visibility.
     *
     * @param player The recipient of the update
     *
     * @see #sendUnitUpdate(Player, InGameObject)
     */
    void sendAllUnitUpdate(Player player) {
        for (InGameObject unit : game().getInGameObjects()) {
            sendUnitUpdate(player, unit);
        }
    }

    /**
     * @return A packet instructing the client to replace any previous unit of the same id with the given unit.
     */
    private Packet createUnitPacket(InGameObject unit) {
        return new Packet(PacketCommand.ENTITY_UPDATE, unit);
    }

    /**
     * @return A packet instructing the client to forget the invisible unit
     */
    private Packet createUnitInvisiblePacket(InGameObject unit) {
        return new Packet(PacketCommand.UNIT_INVISIBLE, unit.getId());
    }

    /**
     * Sends the given unit to the given Player, considering the unit's visibility, if it is a formation. If it is
     * visible, the formation is sent as is. If it is less than visible, a replacement object is sent instead. If the
     * formation is invisible to the player, the client is sent a packet instead that instructs it to remove this id.
     * Units other than formations are not checked for visibility but sent as is.
     *
     * @param player The recipient of the update
     * @param unit   The unit to send
     */
    void sendUnitUpdate(Player player, InGameObject unit) {
        if (unit instanceof SBFFormation formation) {
            SBFVisibilityStatus visibility = game().getVisibility(player, unit.getId());
            InGameObject replacement = getReplacement(formation, visibility);
            if (replacement != null) {
                send(player.getId(), createUnitPacket(replacement));
            } else {
                send(player.getId(), createUnitInvisiblePacket(formation));
            }
        } else {
            send(player.getId(), createUnitPacket(unit));
        }
    }

    /**
     * Sends the given unit to all players, considering the unit's visibility, if it is a formation. If it is visible,
     * the formation is sent as is. If it is less than visible, a replacement object is sent instead. If the formation
     * is invisible to the player, the client is sent a packet instead that instructs it to remove this id. Units other
     * than formations are not checked for visibility but sent as is.
     *
     * @param unit The unit to send
     */
    void sendUnitUpdate(InGameObject unit) {
        for (Player player : game().getPlayersList()) {
            sendUnitUpdate(player, unit);
        }
    }

    private InGameObject getReplacement(SBFFormation formation, SBFVisibilityStatus visibility) {
        return switch (visibility) {
            case VISIBLE, UNKNOWN -> formation;
            case INVISIBLE -> null;
            case BLIP, SENSOR_PING, SENSOR_GHOST -> new SBFUnitPlaceHolder(formation);
            case SOMETHING_OUT_THERE -> new SBFSomethingOutThereUnitPlaceHolder(formation);
            case I_GOT_SOMETHING -> new SBFIGotSomethingUnitPlaceholder(formation);
            case PARTIAL_SCAN_RECON, EYES_ON_TARGET, PARTIAL_SCAN -> new SBFPartialScanUnitPlaceHolder(formation);
        };
    }

}

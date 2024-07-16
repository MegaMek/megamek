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
package megamek.server.sbf;

import megamek.common.InGameObject;
import megamek.common.Player;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.strategicBattleSystems.*;

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
     * Sends the given unit to the given Player, considering the unit's visibility, if it is
     * a formation. If it is visible, the formation is sent as is. If it is less than visible,
     * a replacement object is sent instead. If the formation is invisible to the player, the
     * client is sent a packet instead that instructs it to remove this id.
     * Units other than formations are not checked for visibility but sent as is.
     *
     * @param player The recipient of the update
     * @param unit The unit to send
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
     * Sends the given unit to all players, considering the unit's visibility, if it is
     * a formation. If it is visible, the formation is sent as is. If it is less than visible,
     * a replacement object is sent instead. If the formation is invisible to the player, the
     * client is sent a packet instead that instructs it to remove this id.
     * Units other than formations are not checked for visibility but sent as is.
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

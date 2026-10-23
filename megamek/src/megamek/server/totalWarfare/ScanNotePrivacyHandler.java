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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.logging.MMLogger;

/**
 * Keeps a Scan point's note ("The scan reveals...") between the player who placed the point, their team and a game
 * master until somebody scans it (see {@link ObjectiveMarker#isScanNoteVisibleTo(Player, Player)}). Every player is
 * sent their own copy of the ground objects, with the notes they may not read emptied. And because that copy comes
 * back to the server when the player ends a Victory Setup turn, the notes it lacked are put back before the board is
 * stored, so a player can neither wipe nor rewrite a note they were never shown.
 */
class ScanNotePrivacyHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(ScanNotePrivacyHandler.class);

    ScanNotePrivacyHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /** Sends every player their own copy of the ground objects. */
    void sendGroundObjectsToEveryone() {
        for (Player player : getGame().getPlayersList()) {
            sendGroundObjectsTo(player.getId());
        }
    }

    /**
     * Sends one connection its copy of the ground objects.
     *
     * @param connectionId the connection to send to, which is also its player's id
     */
    void sendGroundObjectsTo(int connectionId) {
        Player recipient = getGame().getPlayer(connectionId);
        gameManager.send(connectionId,
              new Packet(PacketCommand.UPDATE_GROUND_OBJECTS, groundObjectsFor(recipient)));
    }

    /**
     * Builds the copy of the ground objects a player may be sent: every object as it is, except that a point whose
     * scan note the player may not read is replaced by a copy without the note. The server's own objects are never
     * changed.
     *
     * @param recipient the player the copy is for, or {@code null} for a connection with no player, which may read
     *                  no notes at all
     *
     * @return the ground objects for that player
     */
    Map<Coords, List<ICarryable>> groundObjectsFor(@Nullable Player recipient) {
        Map<Coords, List<ICarryable>> groundObjectsCopy = new HashMap<>();
        int withheldNotes = 0;
        for (Map.Entry<Coords, List<ICarryable>> hexObjects : getGame().getGroundObjects().entrySet()) {
            List<ICarryable> hexCopy = new ArrayList<>();
            for (ICarryable groundObject : hexObjects.getValue()) {
                if ((groundObject instanceof ObjectiveMarker marker) && isNoteWithheldFrom(marker, recipient)) {
                    hexCopy.add(marker.copyWithoutScanNote());
                    withheldNotes++;
                } else {
                    hexCopy.add(groundObject);
                }
            }
            groundObjectsCopy.put(hexObjects.getKey(), hexCopy);
        }
        if (withheldNotes > 0) {
            LOGGER.debug("[Scan] Withholding {} scan note(s) from {}", withheldNotes, describe(recipient));
        }
        return groundObjectsCopy;
    }

    /**
     * Puts back, in a board a player has sent at the end of a Victory Setup turn, every scan note that player may
     * not read. Their copy came without those notes, so storing it as sent would wipe them, and a note they could
     * not read is not theirs to rewrite either. A point is matched to the server's own by its hex alone: only one
     * objective sits in a hex, and points placed in this phase carry no id of their own. Everything else about the
     * received point came from the sender, the placer included, so none of it can be trusted to find the match.
     * Whether the note is hidden is decided from the server's point, not the received one.
     *
     * @param receivedGroundObjects the board as the player sent it; its notes are corrected in place
     * @param sender                the player who sent it, or {@code null} when the connection has no player
     */
    void keepNotesTheSenderCannotRead(Map<Coords, List<ICarryable>> receivedGroundObjects,
          @Nullable Player sender) {
        int keptNotes = 0;
        for (Map.Entry<Coords, List<ICarryable>> hexObjects : receivedGroundObjects.entrySet()) {
            for (ICarryable groundObject : hexObjects.getValue()) {
                if (!(groundObject instanceof ObjectiveMarker receivedMarker)) {
                    continue;
                }
                ObjectiveMarker storedMarker = storedMarkerAt(hexObjects.getKey());
                if ((storedMarker == null) || !isNoteHiddenFrom(storedMarker, sender)) {
                    continue;
                }
                receivedMarker.setScanRevealsNote(storedMarker.getScanRevealsNote());
                keptNotes++;
            }
        }
        if (keptNotes > 0) {
            LOGGER.debug("[Scan] Kept {} scan note(s) that {} may not read as they were", keptNotes,
                  describe(sender));
        }
    }

    /**
     * @param hex the hex a received point sits in
     *
     * @return the server's point in that hex, or {@code null} when there is none
     */
    private @Nullable ObjectiveMarker storedMarkerAt(Coords hex) {
        for (ICarryable groundObject : getGame().getGroundObjects(hex)) {
            if (groundObject instanceof ObjectiveMarker storedMarker) {
                return storedMarker;
            }
        }
        return null;
    }

    /**
     * @param marker    the point
     * @param recipient the player a copy of the board is for, or {@code null} for a connection with no player
     *
     * @return {@code true} when the point has a scan note and the recipient may not read it
     */
    private boolean isNoteWithheldFrom(ObjectiveMarker marker, @Nullable Player recipient) {
        boolean hasNote = !marker.getScanRevealsNote().isEmpty();
        return hasNote && isNoteHiddenFrom(marker, recipient);
    }

    /**
     * @param marker the point
     * @param viewer the player who would read its note, or {@code null} for a connection with no player
     *
     * @return {@code true} when the viewer may not read the point's scan note
     */
    private boolean isNoteHiddenFrom(ObjectiveMarker marker, @Nullable Player viewer) {
        if (viewer == null) {
            return true;
        }
        Player placer = getGame().getPlayer(marker.getOwnerId());
        return !ObjectiveMarker.isScanNoteVisibleTo(placer, viewer);
    }

    private static String describe(@Nullable Player player) {
        return (player == null) ? "a connection with no player" : player.getName();
    }
}

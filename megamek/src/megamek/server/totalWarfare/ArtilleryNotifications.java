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

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.Mounted;
import megamek.common.event.GameToastEvent;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Sends artillery call-for-fire "net toasts" (Shot / Splash, counter-battery, homing-inbound reminders) to the
 * appropriate team, keeping that notification logic out of {@link TWGameManager}. Toasts are de-duplicated per round so
 * a multi-tube volley raises one toast per moment, not one per tube.
 *
 * @author HammerGS
 */
public class ArtilleryNotifications {
    private static final MMLogger LOGGER = MMLogger.create(ArtilleryNotifications.class);

    private final TWGameManager gameManager;

    // De-duplicates artillery call-for-fire toasts so a multi-tube volley raises one toast per moment, not per tube.
    // Scoped to a single round: cleared whenever the round changes so it does not grow over a long game.
    private final Set<String> sentNetToasts = new HashSet<>();
    private int sentNetToastsRound = Integer.MIN_VALUE;

    public ArtilleryNotifications(TWGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Sends a single artillery call-for-fire toast (Shot / Splash / Rounds complete) to the firing player and their
     * teammates only - never to enemies, so it does not leak the target. A multi-tube volley is de-duplicated to one
     * toast per moment via the round-scoped key, rather than one per tube.
     *
     * @param momentKey    the call-for-fire moment, used both to look up the localized text
     *                     ({@code Artillery.netToast.<momentKey>}) and to scope de-duplication
     * @param firingEntity the artillery unit (its owner and team define the audience)
     * @param momentRound  the round the moment occurs in, used only to scope de-duplication
     */
    public void sendArtilleryNetToast(String momentKey, Entity firingEntity, int momentRound) {
        sendArtilleryNetToast(momentKey, firingEntity, momentRound, null);
    }

    /**
     * Sends a single artillery call-for-fire toast naming the target grid, to the firing player and their teammates
     * only - never to enemies. The target grid is the team-only channel for the aim point: it travels in this toast
     * (and the team-only map marker), never in the shared phase report, so the other team cannot read it.
     *
     * @param momentKey    the call-for-fire moment ({@code Artillery.netToast.<momentKey>})
     * @param firingEntity the artillery unit (its owner and team define the audience)
     * @param momentRound  the round the moment occurs in, used only to scope de-duplication
     * @param targetGrid   the target grid square to name in the toast (team-only), or {@code null} for a moment with no
     *                     specific target hex
     */
    public void sendArtilleryNetToast(String momentKey, Entity firingEntity, int momentRound,
          @Nullable String targetGrid) {
        Player owner = firingEntity.getOwner();
        if (owner == null) {
            LOGGER.debug("[Artillery] No net toast for {}: firing entity has no owner", firingEntity.getShortName());
            return;
        }
        // Reset the per-round dedupe set when the round advances so it stays bounded over a long game.
        if (momentRound != sentNetToastsRound) {
            sentNetToasts.clear();
            sentNetToastsRound = momentRound;
        }
        String dedupeKey = firingEntity.getId() + ":" + momentKey + ":" + momentRound;
        if (!sentNetToasts.add(dedupeKey)) {
            LOGGER.debug("[Artillery] Net toast '{}' for {} already sent this round - skipping duplicate",
                  momentKey, owner.getName());
            return;
        }
        String message = (targetGrid == null)
              ? Messages.getString("Artillery.netToast." + momentKey, owner.getName())
              : Messages.getString("Artillery.netToast." + momentKey, owner.getName(), targetGrid);
        for (Player player : gameManager.getGame().getPlayersList()) {
            if (!player.isEnemyOf(owner)) {
                gameManager.send(player.getId(), new Packet(PacketCommand.SEND_TOAST,
                      GameToastEvent.Level.INFO, message, firingEntity.getId()));
            }
        }
    }

    /**
     * Sends a radio-flavored counter-battery toast to the team that just spotted an enemy off-board battery's fall of
     * shot, so a player sees the call-for-fire moment alongside the report. Scoped to the observing team only and
     * de-duplicated per enemy battery per team per round (shares the artillery net-toast dedupe set).
     *
     * @param observer     the friendly unit that observed the enemy battery's fall of shot
     * @param enemyBattery the off-board enemy battery that was spotted
     * @param impactHex    the hex the enemy rounds landed on (what the observer saw)
     * @param momentRound  the round the observation happens in, used to scope de-duplication
     */
    public void sendCounterBatteryObservedToast(Entity observer, Entity enemyBattery, Coords impactHex,
          int momentRound) {
        Player observerOwner = observer.getOwner();
        if ((observerOwner == null) || (enemyBattery == null) || (impactHex == null)) {
            LOGGER.debug("[Artillery] No counter-battery toast for {}: owner={}, enemyBattery={}, impactHex={}",
                  observer.getShortName(), observerOwner, enemyBattery, impactHex);
            return;
        }
        if (momentRound != sentNetToastsRound) {
            sentNetToasts.clear();
            sentNetToastsRound = momentRound;
        }
        String dedupeKey = "counterBattery:" + enemyBattery.getId() + ":" + observerOwner.getTeam() + ":" + momentRound;
        if (!sentNetToasts.add(dedupeKey)) {
            LOGGER.debug("[Artillery] Counter-battery toast for battery {} already sent to team {} this round",
                  enemyBattery.getId(), observerOwner.getTeam());
            return;
        }
        String message = Messages.getString("Artillery.counterBatteryToast",
              observer.getShortName(), impactHex.getBoardNum());
        for (Player player : gameManager.getGame().getPlayersList()) {
            if (!player.isEnemyOf(observerOwner)) {
                gameManager.send(player.getId(), new Packet(PacketCommand.SEND_TOAST,
                      GameToastEvent.Level.INFO, message, observer.getId()));
            }
        }
    }

    /**
     * During the movement phase, reminds each team that has a homing artillery round landing next round to put a TAG on
     * the target - a homing round needs a friendly TAG within 8 hexes when it impacts. Fired at
     * {@code turnsTilHit <= 1} (the movement phase before the landing turn) so the team still has its firing phase this
     * turn to TAG the target. De-duplicated per battery per round via {@link #sendArtilleryNetToast}.
     */
    public void remindHomingArtilleryInbound() {
        Game game = gameManager.getGame();
        int remindersSent = 0;
        for (Enumeration<ArtilleryAttackAction> attacks = game.getArtilleryAttacks(); attacks.hasMoreElements(); ) {
            ArtilleryAttackAction artilleryAttack = attacks.nextElement();
            // <= 1 covers both the move phase before the landing turn and the landing turn's own move phase (after the
            // round has already been decremented to 0 in the prior offboard), so the reminder always lands in a move
            // phase the player can act on.
            if (artilleryAttack.getTurnsTilHit() > 1) {
                continue;
            }
            if (!isHomingArtilleryAttack(artilleryAttack)) {
                continue;
            }
            Entity firingEntity = artilleryAttack.getEntity(game);
            if (firingEntity != null) {
                sendArtilleryNetToast("homingInbound", firingEntity, game.getRoundCount());
                remindersSent++;
            }
        }
        if (remindersSent > 0) {
            LOGGER.debug("[Artillery] Reminded teams of {} inbound homing round(s) to TAG this turn", remindersSent);
        }
    }

    /**
     * @param artilleryAttack The in-flight artillery attack
     *
     * @return {@code true} if the attack uses a homing round, checked from the attack's recorded munition type and, as
     *       a fallback, the linked ammo bin
     */
    private boolean isHomingArtilleryAttack(ArtilleryAttackAction artilleryAttack) {
        if (artilleryAttack.getAmmoMunitionType().contains(Munitions.M_HOMING)) {
            return true;
        }
        Entity firingEntity = artilleryAttack.getEntity(gameManager.getGame());
        if (firingEntity != null) {
            Mounted<?> ammo = firingEntity.getEquipment(artilleryAttack.getAmmoId());
            return (ammo instanceof AmmoMounted ammoMounted)
                  && ammoMounted.getType().getMunitionType().contains(Munitions.M_HOMING);
        }
        return false;
    }
}

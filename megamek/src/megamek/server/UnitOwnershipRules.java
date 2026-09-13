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
package megamek.server;

import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Who a client may hand units to, and why.
 *
 * <p>Three packets change who owns a unit, and all three used to take the new owner straight from the payload:
 * adding units, reassigning units, and reassigning a whole force. The client only ever offers legal recipients, but a
 * rule enforced on one side only is a rule the other side cannot rely on - two unrelated client changes once combined
 * to hand every connecting player's units to the host for several days (issue #8860).</p>
 *
 * <p>The rule lives here so all three doors enforce the same one, and so every decision is recorded in the same
 * shape. Every outcome is logged, not only refusals: a permitted hand-off that leaves no trace cannot be audited
 * afterwards or verified in testing.</p>
 */
public final class UnitOwnershipRules {

    private static final MMLogger LOGGER = MMLogger.create(UnitOwnershipRules.class);

    private UnitOwnershipRules() {}

    /**
     * The reason a hand-off was allowed or refused. Kept as distinct values rather than a boolean so the log says
     * which clause applied, which is what makes a permitted path auditable.
     */
    public enum OwnershipVerdict {
        /** The ordinary case: a player acting on their own units. */
        SENDER_OWNS_THEM(true),
        /** A gamemaster is meant to be able to set anybody up. */
        SENDER_IS_GAMEMASTER(true),
        /**
         * The known weak point, kept deliberately. Since #8808 a unit for your own Princess travels over your
         * connection carrying the bot's owner id, and the server holds no record of which human runs which bot, so it
         * cannot tell your bot from anyone else's.
         */
        RECIPIENT_IS_A_BOT(true),
        /** The packet arrived on a connection that belongs to no player. */
        NO_SUCH_SENDER(false),
        /** The claimed recipient is not in the game. */
        NO_SUCH_RECIPIENT(false),
        /** A player trying to hand units to another human who is not theirs to act for. */
        NOT_PERMITTED(false);

        private final boolean allowed;

        OwnershipVerdict(boolean allowed) {
            this.allowed = allowed;
        }

        public boolean isAllowed() {
            return allowed;
        }
    }

    /**
     * Decides whether the sender may give units to the recipient.
     *
     * @param sender    the player on the sending connection, or {@code null} if that connection has no player
     * @param recipient the player the units are claimed for, or {@code null} if no such player is in the game
     *
     * @return the verdict, carrying which clause applied
     */
    public static OwnershipVerdict verdictFor(@Nullable Player sender, @Nullable Player recipient) {
        if (sender == null) {
            return OwnershipVerdict.NO_SUCH_SENDER;
        }
        if (recipient == null) {
            return OwnershipVerdict.NO_SUCH_RECIPIENT;
        }
        if (recipient.getId() == sender.getId()) {
            return OwnershipVerdict.SENDER_OWNS_THEM;
        }
        if (sender.isGameMaster()) {
            return OwnershipVerdict.SENDER_IS_GAMEMASTER;
        }
        if (recipient.isBot()) {
            return OwnershipVerdict.RECIPIENT_IS_A_BOT;
        }
        return OwnershipVerdict.NOT_PERMITTED;
    }

    /**
     * Records one ownership decision. Called for every outcome so the permitted paths leave a trail too.
     *
     * @param action    what was being attempted, for example {@code "add"} or {@code "reassign"}
     * @param verdict   the decision
     * @param sender    the player on the sending connection, or {@code null}
     * @param recipient the claimed recipient, or {@code null}
     * @param subject   what was being handed over, for the log line
     */
    public static void logDecision(String action, OwnershipVerdict verdict, @Nullable Player sender,
          @Nullable Player recipient, String subject) {
        String senderName = (sender == null) ? "an unknown connection" : sender.getName();
        String recipientName = (recipient == null) ? "an unknown player" : recipient.getName();
        if (verdict.isAllowed()) {
            LOGGER.info("[Ownership] {} by {} to {}: {} ({})", action, senderName, recipientName, subject, verdict);
        } else {
            LOGGER.warn("[Ownership] REFUSED {} by {} to {}: {} ({})",
                  action, senderName, recipientName, subject, verdict);
        }
    }

    /**
     * The chat line shown when a hand-off is refused, matching how an illegal design is already reported.
     *
     * @param action    what was being attempted
     * @param sender    the player on the sending connection, or {@code null}
     * @param recipient the claimed recipient, or {@code null}
     * @param subject   what was being handed over
     *
     * @return the message to send to all players
     */
    public static String refusalMessage(String action, @Nullable Player sender, @Nullable Player recipient,
          String subject) {
        return String.format("Player %s attempted to %s %s for %s; it was rejected.",
              (sender == null) ? "on an unknown connection" : sender.getName(),
              action,
              subject,
              (recipient == null) ? "a player who is not in the game" : recipient.getName());
    }
}

/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.game;

import java.io.Serial;

import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.ProtoMek;

/**
 * Represents a single turn within a phase of the game, where a specific player has to declare his/her action. The
 * default game turn allows a player to move any entity.
 *
 * @author Ben
 */
public class GameTurn extends AbstractPlayerTurn {
    @Serial
    private static final long serialVersionUID = -8340385894504735190L;

    /**
     * Various optionals rules force certain unit types to move multiple units for one turn, such as mek and vehicle
     * lance rules; this flag keeps track of whether this turn was generated as one of these multi-turns.
     */
    private boolean isMultiTurn = false;

    /** Creates a new instance of GameTurn */
    public GameTurn(int playerId) {
        super(playerId);
    }

    /**
     * Determine if the specified entity is a valid one to use for this turn.
     *
     * @param entity the <code>Entity</code> that may take this turn.
     * @param game   The {@link Game} the turn belongs to
     *
     * @return <code>true</code> if the specified entity can take this turn.
     *       <code>false</code> if the entity is not valid for this turn.
     */
    public boolean isValidEntity(final @Nullable Entity entity, final Game game) {
        return isValidEntity(entity, game, true);
    }

    /**
     * Determine if the specified entity is a valid one to use for this turn.
     * <p>
     * In addition to the "standard" validity checks, there is also a check for the optional rules "infantry move later"
     * and "ProtoMeks move later." This checks to see if those options are enabled and if there is a valid non-infantry
     * (or proto) unit to move and if so, the entity is invalid.
     * <p>
     * There are certain instances where this check should not be used when the optional rules are enabled (such as
     * loading infantry into a unit). Hence, the use of these additional checks is specified by a boolean input
     * parameter.
     *
     * @param entity                   the <code>Entity</code> that may take this turn.
     * @param game                     The {@link Game} the turn belongs to
     * @param useValidNonInfantryCheck Boolean that determines if we should check to see if infantry can be moved yet
     *
     * @return <code>true</code> if the specified entity can take this turn.
     *       <code>false</code> if the entity is not valid for this turn.
     */
    public boolean isValidEntity(final @Nullable Entity entity, final Game game,
          final boolean useValidNonInfantryCheck) {
        return (entity != null)
              && (entity.getOwnerId() == playerId())
              && entity.isSelectableThisTurn()
              // This next bit enforces the "A players Infantry/ProtoMeks move after that player's other units" options.
              && !(useValidNonInfantryCheck && game.getPhase().isMovement()
              && (((entity instanceof Infantry) && game.getOptions()
              .booleanOption(OptionsConstants.INIT_INF_MOVE_LATER))
              || ((entity instanceof ProtoMek) && game.getOptions()
              .booleanOption(OptionsConstants.INIT_PROTOMEKS_MOVE_LATER)))
              && game.checkForValidNonInfantryAndOrProtoMeks(playerId()));
    }

    @Override
    public boolean isValidEntity(InGameObject unit, IGame game) {
        return (unit instanceof Entity) && (game instanceof Game) && isValidEntity((Entity) unit, (Game) game);
    }

    /**
     * @return true if the player and entity are both valid.
     */
    public boolean isValid(final int playerId, final @Nullable Entity entity, final Game game) {
        return isValid(playerId, game) && isValidEntity(entity, game);
    }

    @Override
    public boolean isMultiTurn() {
        return isMultiTurn;
    }

    public void setMultiTurn(boolean isMultiTurn) {
        this.isMultiTurn = isMultiTurn;
    }
}

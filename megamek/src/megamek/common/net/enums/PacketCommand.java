/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.net.enums;

public enum PacketCommand {
    //region Enum Declarations
    CLOSE_CONNECTION,
    SERVER_VERSION_CHECK,
    SERVER_GREETING,
    ILLEGAL_CLIENT_VERSION,
    CLIENT_NAME,
    CLIENT_VERSIONS,
    LOCAL_PN,
    PLAYER_ADD,
    PLAYER_REMOVE,
    PLAYER_UPDATE,
    PLAYER_TEAM_CHANGE,

    /**
     * A packet replacing a Client's knowledge of all bot settings (S -> C) or updating the Server on a single bot's
     * settings (C -> S). Does not invoke any actual changes to bots.
     */
    PRINCESS_SETTINGS,

    /** A Server to Client packet instructing a Princess Client to replace its settings. */
    CHANGE_PRINCESS_SETTINGS,

    /** A packet setting a Client's ready status (S -> C) or updating the Server on the Client's status (C -> S). */
    PLAYER_READY,

    /** A packet telling the server to pause / unpause packet handling (to interrupt a Princess-only game) */
    PAUSE,
    UNPAUSE,

    CHAT,
    ENTITY_ADD,
    ENTITY_REMOVE,
    ENTITY_MOVE,
    ENTITY_DEPLOY,
    ENTITY_DEPLOY_UNLOAD,

    /**
     * A packet informing the receiver of one or more actions of units (both directions; using different packet
     * builds).
     */
    ENTITY_ATTACK,

    ENTITY_PREPHASE,
    ENTITY_GTA_HEX_SELECT,

    /** A packet informing the receiver of an unspecified change to a unit. */
    ENTITY_UPDATE,

    /** A packet instructing the Client to forget the unit of the given id as it is / has become invisible (SBF). */
    UNIT_INVISIBLE,

    ENTITY_MULTI_UPDATE,
    ENTITY_WORDER_UPDATE,
    ENTITY_ASSIGN,
    ENTITY_MODE_CHANGE,
    ENTITY_AMMO_CHANGE,
    ENTITY_SENSOR_CHANGE,
    ENTITY_SINKS_CHANGE,
    ENTITY_ACTIVATE_HIDDEN,
    ENTITY_SYSTEM_MODE_CHANGE,
    FORCE_UPDATE,
    FORCE_ADD,
    FORCE_DELETE,
    FORCE_PARENT,
    FORCE_ADD_ENTITY,
    FORCE_ASSIGN_FULL,
    ENTITY_VISIBILITY_INDICATOR,
    ADD_SMOKE_CLOUD,
    CHANGE_HEX,
    CHANGE_HEXES,
    BLDG_ADD,
    BLDG_REMOVE,
    BLDG_UPDATE,
    BLDG_COLLAPSE,
    BLDG_EXPLODE,

    /** A Server to Client packet instructing the Client to change the game's phase. */
    PHASE_CHANGE,

    /** A Server to Client packet instructing the Client to update the current player turn index. */
    TURN,

    /** A Server to Client packet instructing the Client to change the game's current round. */
    ROUND_UPDATE,

    /** A Server to Client packet instructing the Client to replace all boards with the newly sent ones. */
    SENDING_BOARD,

    SENDING_ILLUMINATED_HEXES,
    CLEAR_ILLUMINATED_HEXES,
    SENDING_ENTITIES,
    SENDING_PLAYERS,

    /** A Server to Client packet instructing the Client to update the list of player turns. */
    SENDING_TURNS,

    SENDING_REPORTS,
    SENDING_REPORTS_SPECIAL,
    SENDING_REPORTS_TACTICAL_GENIUS,

    /** A packet transmitting the entire game's accumulated reports to the Client, possibly obscured. */
    SENDING_REPORTS_ALL,

    /** A packet having an options to share with other Clients (C -> S) or implement on the receiving Client (S -> C). */
    SENDING_GAME_SETTINGS,

    SENDING_MAP_DIMENSIONS,
    SENDING_MAP_SETTINGS,
    END_OF_GAME,
    DEPLOY_MINEFIELDS,
    REVEAL_MINEFIELD,
    REMOVE_MINEFIELD,
    SENDING_MINEFIELDS,
    UPDATE_MINEFIELDS,
    UPDATE_GROUND_OBJECTS,
    REROLL_INITIATIVE,
    UNLOAD_STRANDED,
    SET_ARTILLERY_AUTO_HIT_HEXES,
    SENDING_ARTILLERY_ATTACKS,
    SENDING_FLARES,
    SERVER_CORRECT_NAME,
    SEND_SAVEGAME,
    LOAD_SAVEGAME,
    LOAD_GAME,

    /** A Server to Client packet transmitting SpecialHexDisplays for a board (filtered for visibility) */
    SENDING_SPECIAL_HEX_DISPLAY,

    /** A Client to Server packet adding a new SpecialHexDisplay */
    SPECIAL_HEX_DISPLAY_APPEND,

    /** A Client to Server packet removing a SpecialHexDisplay */
    SPECIAL_HEX_DISPLAY_DELETE,
    CUSTOM_INITIATIVE,
    FORWARD_INITIATIVE,
    SENDING_PLANETARY_CONDITIONS,
    SQUADRON_ADD,
    ENTITY_CALLED_SHOT_CHANGE,
    ENTITY_MOUNTED_FACING_CHANGE,
    SENDING_AVAILABLE_MAP_SIZES,
    ENTITY_LOAD,
    ENTITY_TOW,
    ENTITY_NOVA_NETWORK_CHANGE,
    ENTITY_VARIABLE_RANGE_MODE_CHANGE,
    ENTITY_ABANDON_ANNOUNCE,
    RESET_ROUND_DEPLOYMENT,
    SENDING_TAG_INFO,
    RESET_TAG_INFO,
    CLIENT_FEEDBACK_REQUEST,
    CFR_DOMINO_EFFECT,
    CFR_AMS_ASSIGN,
    CFR_APDS_ASSIGN,
    CFR_HIDDEN_PBS,
    CFR_TELEGUIDED_TARGET,
    CFR_TAG_TARGET,
    GAME_VICTORY_EVENT,

    /** A Server to Client packet instructing the Client to show a message (e.g. story message) to the player. */
    SCRIPTED_MESSAGE,

    /** An SBF packet instructing the Client to replace the pending actions with the sent actions (possibly none). */
    ACTIONS,

    /** A packet containing other packets to be processed in the order they are stored. */
    MULTI_PACKET,

    /** A packet adding a temporary ECM field (e.g., from EMP mines). */
    ADD_TEMPORARY_ECM_FIELD,

    /** A packet syncing all temporary ECM fields to clients (replaces existing list). */
    SYNC_TEMPORARY_ECM_FIELDS;
    //endregion Enum Declarations

    //region Boolean Comparison Methods

    public boolean isSendingReportsTacticalGenius() {
        return this == SENDING_REPORTS_TACTICAL_GENIUS;
    }

    public boolean isCFRDominoEffect() {
        return this == CFR_DOMINO_EFFECT;
    }

    public boolean isCFRAMSAssign() {
        return this == CFR_AMS_ASSIGN;
    }

    public boolean isCFRAPDSAssign() {
        return this == CFR_APDS_ASSIGN;
    }

    public boolean isCFRHiddenPBS() {
        return this == CFR_HIDDEN_PBS;
    }

    public boolean isCFRTeleguidedTarget() {
        return this == CFR_TELEGUIDED_TARGET;
    }

    public boolean isCFRTagTarget() {
        return this == CFR_TAG_TARGET;
    }

    public boolean isGameVictoryEvent() {
        return this == GAME_VICTORY_EVENT;
    }

    public boolean isCFR() {
        return isCFRDominoEffect() || isCFRAMSAssign() || isCFRAPDSAssign() || isCFRHiddenPBS()
              || isCFRTeleguidedTarget() || isCFRTagTarget();
    }
    //endregion Boolean Comparison Methods
}

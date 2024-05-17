/*
 * Copyright (c) 2022, 2024 - The MegaMek Team. All Rights Reserved.
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
    PRINCESS_SETTINGS,

    /** A packet setting a Client's ready status (S -> C) or updating the Server on the Client's status (C -> S). */
    PLAYER_READY,

    CHAT,
    ENTITY_ADD,
    ENTITY_REMOVE,
    ENTITY_MOVE,
    ENTITY_DEPLOY,
    ENTITY_DEPLOY_UNLOAD,

    /** A packet informing the receiver of one or more actions of units (both directions; using different packet builds). */
    ENTITY_ATTACK,

    ENTITY_PREPHASE,
    ENTITY_GTA_HEX_SELECT,
    ENTITY_UPDATE,
    ENTITY_MULTIUPDATE,
    ENTITY_WORDER_UPDATE,
    ENTITY_ASSIGN,
    ENTITY_MODECHANGE,
    ENTITY_AMMOCHANGE,
    ENTITY_SENSORCHANGE,
    ENTITY_SINKSCHANGE,
    ENTITY_ACTIVATE_HIDDEN,
    ENTITY_SYSTEMMODECHANGE,
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

    TURN,

    /** A Server to Client packet instructing the Client to change the game's current round. */
    ROUND_UPDATE,

    /** A Server to Client packet instructing the Client to replace all boards with the newly sent ones. */
    SENDING_BOARD,

    SENDING_ILLUM_HEXES,
    CLEAR_ILLUM_HEXES,
    SENDING_ENTITIES,
    SENDING_PLAYERS,
    SENDING_TURNS,
    SENDING_REPORTS,
    SENDING_REPORTS_SPECIAL,
    SENDING_REPORTS_TACTICAL_GENIUS,

    /** A packet transmitting the entire game's accumulated reports to the Client, possibly obscured. */
    SENDING_REPORTS_ALL,

    /** A packet having a options to share with other Clients (C -> S) or implement on the receiving Client (S -> C). */
    SENDING_GAME_SETTINGS,

    SENDING_MAP_DIMENSIONS,
    SENDING_MAP_SETTINGS,
    END_OF_GAME,
    DEPLOY_MINEFIELDS,
    REVEAL_MINEFIELD,
    REMOVE_MINEFIELD,
    SENDING_MINEFIELDS,
    UPDATE_MINEFIELDS,
    REROLL_INITIATIVE,
    UNLOAD_STRANDED,
    SET_ARTILLERY_AUTOHIT_HEXES,
    SENDING_ARTILLERY_ATTACKS,
    SENDING_FLARES,
    SERVER_CORRECT_NAME,
    SEND_SAVEGAME,
    LOAD_SAVEGAME,
    LOAD_GAME,
    SENDING_SPECIAL_HEX_DISPLAY,
    SPECIAL_HEX_DISPLAY_APPEND,
    SPECIAL_HEX_DISPLAY_DELETE,
    CUSTOM_INITIATIVE,
    FORWARD_INITIATIVE,
    SENDING_PLANETARY_CONDITIONS,
    SQUADRON_ADD,
    ENTITY_CALLEDSHOTCHANGE,
    ENTITY_MOUNTED_FACING_CHANGE,
    SENDING_AVAILABLE_MAP_SIZES,
    ENTITY_LOAD,
    ENTITY_NOVA_NETWORK_CHANGE,
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
    GAME_VICTORY_EVENT;
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

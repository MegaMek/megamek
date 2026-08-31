/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview.spriteHandler;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import megamek.client.ui.Messages;
import megamek.client.ui.panels.phaseDisplay.VictoryHexPropertiesPane;
import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.client.ui.clientGUI.boardview.sprite.GroundObjectSprite;
import megamek.client.ui.clientGUI.boardview.sprite.HexFlagSprite;
import megamek.client.ui.clientGUI.boardview.sprite.Sprite;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.RangeType;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.equipment.ObjectiveScoringScheme;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.game.Game;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.logging.MMLogger;

public class GroundObjectSpriteHandler extends BoardViewSpriteHandler implements IPreferenceChangeListener {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /** Outline colour for a control zone nobody currently holds. */
    private static final Color UNCONTROLLED_ZONE_COLOR = new Color(190, 190, 190);

    /** Feature logger for the victory hex designation diagnostics; enabled via the log4j2.xml VictoryHex block. */
    private static final MMLogger VICTORY_HEX_LOGGER = MMLogger.create("megamek.feature.VictoryHex");

    // Cache the ground object list as it does not change very often
    private Map<Coords, List<ICarryable>> currentGroundObjectList;
    private final Game game;

    public GroundObjectSpriteHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    public void setGroundObjectSprites(Map<Coords, List<ICarryable>> objectCoordList) {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        currentGroundObjectList = objectCoordList;
        int flagCount = 0;
        if (currentGroundObjectList != null) {
            BoardView boardView = (BoardView) clientGUI.boardViews().getFirst();
            for (Coords coords : currentGroundObjectList.keySet()) {
                boolean showOverlays = GUIP.getShowObjectiveOverlays();
                for (ICarryable groundObject : currentGroundObjectList.get(coords)) {
                    if ((groundObject instanceof ObjectiveMarker marker) && showOverlays) {
                        flagCount++;
                        currentSprites.addAll(zoneOutlineSprites(boardView, game.getBoard(), coords,
                              marker.getControlRadius(), marker));
                    }
                    currentSprites.add(spriteFor(groundObject, coords, boardView));
                }
            }
        }

        clientGUI.boardViews().getFirst().addSprites(currentSprites);
        VICTORY_HEX_LOGGER.debug("[VictoryHex] Board shows {} ground object sprite(s), {} of them objective flag(s)",
              currentSprites.size(), flagCount);
    }

    /**
     * Builds the outline of an objective's control zone: every hex within the given radius of the center whose
     * edges facing hexes outside the zone are highlighted, so the zone's perimeter is drawn on the board without
     * filling it. A radius of 0 draws nothing - the flag itself already marks the single hex.
     *
     * @param boardView the board view the sprites are displayed on
     * @param board     the game board, for the zone's edge at the board border; may be {@code null} pre-board
     * @param center    the objective's hex
     * @param radius    the control radius in hexes
     *
     * @return the perimeter sprites of the zone, empty for radius 0
     */
    private List<FieldOfFireSprite> zoneOutlineSprites(BoardView boardView, @Nullable Board board,
          Coords center, int radius, ObjectiveMarker marker) {
        Color zoneColor = controllerColor(marker);
        List<FieldOfFireSprite> outlineSprites = new ArrayList<>();
        if ((radius <= 0) || (board == null) || (board.getWidth() == 0)) {
            return outlineSprites;
        }
        for (Coords zoneHex : center.allAtDistanceOrLess(radius)) {
            if (!board.contains(zoneHex)) {
                continue;
            }
            int outsideEdges = 0;
            for (int direction = 0; direction < 6; direction++) {
                Coords neighbor = zoneHex.translated(direction);
                boolean neighborOutsideZone = (center.distance(neighbor) > radius) || !board.contains(neighbor);
                if (neighborOutsideZone) {
                    outsideEdges |= (1 << direction);
                }
            }
            if (outsideEdges != 0) {
                outlineSprites.add(new FieldOfFireSprite(boardView, zoneColor, zoneHex, outsideEdges));
            }
        }
        return outlineSprites;
    }

    /**
     * @param groundObject the ground object to create a sprite for
     * @param coords       the hex the ground object is in
     * @param boardView    the board view the sprite is displayed on
     *
     * @return a flag in the owning player's color for an objective marker, otherwise the generic cargo sprite
     */
    private Sprite spriteFor(ICarryable groundObject, Coords coords, BoardView boardView) {
        if (groundObject instanceof ObjectiveMarker marker) {
            boolean showOverlays = GUIP.getShowObjectiveOverlays();
            String schemeWord = showOverlays
                  ? Messages.getString("VictoryHex.word."
                        + marker.getScoringScheme().getPreset().name().toLowerCase(Locale.ROOT))
                  : null;
            String progress = showOverlays ? marker.getScoringScheme().progressLabel() : null;
            HexFlagSprite flagSprite = new HexFlagSprite(boardView, coords, ownerColor(marker), schemeWord,
                  progress);
            flagSprite.setTooltip(flagTooltip(marker));
            return flagSprite;
        }
        return new GroundObjectSprite(boardView, coords);
    }

    /**
     * Describes what a point asks of the players, for the hover tooltip: its owner, what it is worth, how
     * far along it is, and the scheme's own explanation of how it is won - the same wording the properties
     * pane shows when the point is set up.
     *
     * @param marker the objective marker to describe
     *
     * @return the tooltip text
     */
    private String flagTooltip(ObjectiveMarker marker) {
        ObjectiveScoringScheme scheme = marker.getScoringScheme();
        Player owner = game.getPlayer(marker.getOwnerId());
        StringBuilder tooltip = new StringBuilder("<html>");
        tooltip.append("<b>").append(marker.generalName()).append("</b>");
        if (owner != null) {
            tooltip.append(" &mdash; ").append(owner.getName());
        }
        tooltip.append("<br>").append(Messages.getString("VictoryHex.tooltip.worth",
              marker.getVictoryPointValue(), marker.getControlRadius()));
        String progress = scheme.progressLabel();
        if (progress != null) {
            tooltip.append("<br>").append(Messages.getString("VictoryHex.tooltip.progress", progress));
        }
        tooltip.append("<br>").append(VictoryHexPropertiesPane.describeScheme(scheme));
        tooltip.append("</html>");
        return tooltip.toString();
    }

    /**
     * @param marker the objective marker to color
     *
     * @return the owning player's color, or yellow when the owner is unknown to this client
     */
    private Color controllerColor(ObjectiveMarker marker) {
        int controllingTeam = marker.getControllingTeam();
        int controllingPlayerId = marker.getControllingPlayerId();
        if (controllingPlayerId != ObjectiveMarker.NO_CONTROLLER) {
            Player controller = game.getPlayer(controllingPlayerId);
            if (controller != null) {
                return controller.getColour().getColour();
            }
        }
        if (controllingTeam != ObjectiveMarker.NO_CONTROLLER) {
            for (Player player : game.getPlayersList()) {
                if (player.getTeam() == controllingTeam) {
                    return player.getColour().getColour();
                }
            }
        }
        // nobody holds it: a neutral outline, so an uncontested point reads differently from a held one
        return UNCONTROLLED_ZONE_COLOR;
    }

    /**
     * @param marker the objective marker to colour
     *
     * @return the owning player's color, or yellow when the owner is unknown to this client
     */
    private Color ownerColor(ObjectiveMarker marker) {
        Player owner = game.getPlayer(marker.getOwnerId());
        return (owner != null) ? owner.getColour().getColour() : Color.YELLOW;
    }

    @Override
    public void clear() {
        super.clear();
        currentGroundObjectList = null;
    }

    @Override
    public void initialize() {
        game.addGameListener(this);
        GUIP.addPreferenceChangeListener(this);
    }

    @Override
    public void dispose() {
        clear();
        game.removeGameListener(this);
        GUIP.removePreferenceChangeListener(this);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
        if (GUIPreferences.SHOW_OBJECTIVE_OVERLAYS.equals(event.getName())) {
            setGroundObjectSprites(game.getGroundObjects());
        }
    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {
        setGroundObjectSprites(game.getGroundObjects());
    }
}

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
import megamek.common.equipment.ObjectiveScoringScheme;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.game.Game;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.logging.MMLogger;

public class GroundObjectSpriteHandler extends BoardViewSpriteHandler implements IPreferenceChangeListener {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /**
     * The colour of a control point nobody holds: white, as the neutral state a point starts in and
     * returns to. Flag and zone both use it, so a point reads as one thing - whoever holds it, or no one -
     * rather than the flag showing who placed it while the zone shows who holds it.
     */
    private static final Color NEUTRAL_COLOR = Color.WHITE;

    /** Exponent applied to a point's progress fraction before it tints the colour; below 1 front-loads. */
    private static final double TINT_CURVE_EXPONENT = 0.5;

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
        Color zoneColor = tracedControllerColor(marker);
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
            // the banner shows the same colour as the zone: a point reads as one thing
            return new HexFlagSprite(boardView, coords, tracedControllerColor(marker), schemeWord, progress);
        }
        return new GroundObjectSprite(boardView, coords);
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
                return controller.getDisplayColour().getColour();
            }
        }
        if (controllingTeam != ObjectiveMarker.NO_CONTROLLER) {
            // teams have no colour of their own, so the zone borrows one from a member. Where a team's
            // players have chosen different colours this picks the first, which is arbitrary but stable:
            // the same team always reads the same way for the whole game
            for (Player player : game.getPlayersList()) {
                if (player.getTeam() == controllingTeam) {
                    VICTORY_HEX_LOGGER.debug("[VictoryHex] team {} colour taken from {} (id {}): colour field={},"
                                + " camouflage={}/{}, display={}", controllingTeam, player.getName(),
                          player.getId(), player.getColour(), player.getCamouflage().getCategory(),
                          player.getCamouflage().getFilename(), player.getDisplayColour());
                    return player.getDisplayColour().getColour();
                }
            }
        }
        // nobody holds it: a neutral outline, so an uncontested point reads differently from a held one
        return NEUTRAL_COLOR;
    }

    /**
     * @param marker the objective marker whose zone is being drawn
     *
     * @return the colour chosen for its zone, logging what the client believes about who holds it. The
     *       zone colour depends on the controller reaching this client at all, so when it looks wrong the
     *       first question is whether the marker arrived with a controller set or without one
     */
    private Color tracedControllerColor(ObjectiveMarker marker) {
        Color chosen = pointColor(marker);
        VICTORY_HEX_LOGGER.debug("[VictoryHex] zone colour for {}: team={}, player={} -> {}",
              marker.generalName(), marker.getControllingTeam(), marker.getControllingPlayerId(), chosen);
        return chosen;
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

    /**
     * The colour a point is shown in, flag and zone alike. A point is not painted in a side's full colour
     * until that side has actually won it: while a counter is running, the colour moves from where the
     * point started toward where it is going, in proportion to the counter. One turn into a ten-turn
     * hold is a barely tinted white; nine turns in is nearly solid. So the shade itself says how close
     * the point is, and a one-turn hold and a ten-turn hold look different at the same moment.
     *
     * @param marker the point to colour
     *
     * @return the blended colour
     */
    private Color pointColor(ObjectiveMarker marker) {
        ObjectiveScoringScheme scheme = marker.getScoringScheme();
        if (scheme.isDecided()) {
            return sideColor(scheme.getSecuredTeam(), scheme.getSecuredPlayerId());
        }
        double fraction = scheme.progressFraction();
        // a point that is held with nothing counted against it is simply its holder's: it was given to
        // them at setup, or they kept it after walking away. It is theirs until someone challenges it,
        // and paints in their full colour - the tint only starts once a counter does
        boolean isHeldWithNothingCounted = hasController(marker) && (fraction == 0.0);
        if (isHeldWithNothingCounted) {
            return controllerColor(marker);
        }
        return switch (scheme.getPreset()) {
            // the holder's colour arrives as the hold is counted
            case HOLD -> {
                ObjectiveScoringScheme.CountedSide leader = scheme.leadingSide();
                Color target = (leader == null) ? controllerColor(marker) : sideColor(leader.team(), leader.playerId());
                yield blend(NEUTRAL_COLOR, target, fraction);
            }
            // the attacker's colour takes over the owner's as the meter fills
            case CAPTURE -> {
                ObjectiveScoringScheme.CountedSide leader = scheme.leadingSide();
                Color attacker = (leader == null) ? ownerColor(marker) : sideColor(leader.team(), leader.playerId());
                yield blend(ownerColor(marker), attacker, fraction);
            }
            // the owner's colour drains toward white with the grip
            case DEFEND -> blend(ownerColor(marker), NEUTRAL_COLOR, fraction);
            // control is instantaneous and is painted in full
            case STANDARD, RAID -> controllerColor(marker);
        };
    }

    /**
     * @param from     the colour at fraction 0
     * @param to       the colour at fraction 1
     * @param fraction how far from one to the other, 0 to 1
     *
     * @return the colour that fraction of the way between them
     */
    private static Color blend(Color from, Color to, double fraction) {
        // a straight line makes the first steps invisible - one turn of ten is a 10% tint on white,
        // which is nothing. A square root front-loads the change: 10% of the way reads as about a third
        // of the colour, half way as 70%, and the last stretch tightens toward solid. Early progress is
        // seen, and a point on the brink is still visibly not there yet
        double clamped = Math.pow(Math.max(0.0, Math.min(1.0, fraction)), TINT_CURVE_EXPONENT);
        int red = (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int green = (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int blue = (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        return new Color(red, green, blue);
    }

    /**
     * @param team     a team id, or {@link ObjectiveScoringScheme#NO_SIDE}
     * @param playerId a player id, or {@link ObjectiveScoringScheme#NO_SIDE}
     *
     * @return that side's colour: a player's own, or the first member's for a team; white when unknown
     */
    private Color sideColor(int team, int playerId) {
        if (playerId != ObjectiveScoringScheme.NO_SIDE) {
            Player player = game.getPlayer(playerId);
            return (player != null) ? player.getDisplayColour().getColour() : NEUTRAL_COLOR;
        }
        for (Player player : game.getPlayersList()) {
            if (player.getTeam() == team) {
                return player.getDisplayColour().getColour();
            }
        }
        return NEUTRAL_COLOR;
    }

    /**
     * @param marker the point
     *
     * @return the colour of the player who placed it, or white when this client does not know them
     */
    private Color ownerColor(ObjectiveMarker marker) {
        Player owner = game.getPlayer(marker.getOwnerId());
        return (owner != null) ? owner.getDisplayColour().getColour() : NEUTRAL_COLOR;
    }

    /**
     * @param marker the point
     *
     * @return {@code true} when some side currently holds it, by team or as an unteamed player
     */
    private static boolean hasController(ObjectiveMarker marker) {
        return (marker.getControllingTeam() != ObjectiveMarker.NO_CONTROLLER)
              || (marker.getControllingPlayerId() != ObjectiveMarker.NO_CONTROLLER);
    }
}

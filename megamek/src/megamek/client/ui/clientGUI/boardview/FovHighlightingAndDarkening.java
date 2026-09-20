/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.sprite.StepSprite;
import megamek.common.ECMInfo;
import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeECM;
import megamek.common.equipment.MiscType;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.event.entity.GameEntityNewEvent;
import megamek.common.event.entity.GameEntityRemoveEvent;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.IlluminationLevel;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * A helper class for highlighting and darkening hexes.
 */
public class FovHighlightingAndDarkening {
    private static final MMLogger logger = MMLogger.create(FovHighlightingAndDarkening.class);

    private final BoardView boardView;
    private List<Color> ringsColors = new ArrayList<>();
    private List<Integer> ringsRadii = new ArrayList<>();
    GUIPreferences gs = GUIPreferences.getInstance();
    private final IPreferenceChangeListener ringsChangeListener;

    public FovHighlightingAndDarkening(BoardView boardView) {
        this.boardView = boardView;
        updateRingsProperties();
        ringsChangeListener = e -> {
            String eName = e.getName();
            if (eName.equals(GUIPreferences.FOV_HIGHLIGHT_RINGS_RADII) ||
                  eName.equals(GUIPreferences.FOV_HIGHLIGHT_RINGS_COLORS_HSB) ||
                  eName.equals(GUIPreferences.FOV_HIGHLIGHT_ALPHA)) {
                updateRingsProperties();
            }
            // Clear LOS cache when spotting mode changes so FOV recalculates with/without mast mount bonus
            if (eName.equals(GUIPreferences.FOV_SPOTTING_MODE)) {
                clearCache();
            }
        };
        gs.addPreferenceChangeListener(ringsChangeListener);

        cacheGameListener = new GameListenerAdapter() {
            @Override
            public void gameTurnChange(GameTurnChangeEvent e) {
                visibilityChanged();
            }

            @Override
            public void gameEntityChange(GameEntityChangeEvent e) {
                visibilityChanged();
            }

            @Override
            public void gameEntityNew(GameEntityNewEvent e) {
                visibilityChanged();
            }

            @Override
            public void gameEntityRemove(GameEntityRemoveEvent e) {
                visibilityChanged();
            }
        };
        this.boardView.game.addGameListener(cacheGameListener);
    }

    private void visibilityChanged() {
        Runnable refresh = () -> {
            invalidate();
            boardView.checkFoVHexImageCacheClear();
        };
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            refresh.run();
        } else {
            javax.swing.SwingUtilities.invokeLater(refresh);
        }
    }

    public void die() {
        gs.removePreferenceChangeListener(ringsChangeListener);
        boardView.game.removeGameListener(cacheGameListener);
    }

    /**
     * Checks if options for darkening and highlighting are turned on: If there is no LOS from currently selected
     * hex/entity, then darkens hex c. If there is a LOS from the hex c to the selected hex/entity, then hex c is
     * colored according to distance.
     *
     * @param boardGraph The board on which we paint.
     * @param c          Hex that is being processed.
     */
    boolean draw(Graphics2D boardGraph, Coords c) {
        BoardFieldOfView.Hex result = evaluate(c);
        if (result.tint() != 0) {
            Color tint = new Color(result.tint(), true);
            if (result.visibility() == BoardFieldOfView.Visibility.ORIGIN) {
                boardView.drawHexBorder(boardGraph, new Point(0, 0), tint, 0, 7);
            } else {
                boardView.drawHexLayer(boardGraph, tint,
                      result.visibility() == BoardFieldOfView.Visibility.BLOCKED, gs.getFovSpottingMode());
            }
        }
        return result.hasLineOfSight();
    }

    /** Shared rules result. The classic painter and native renderer consume the same classification and colors. */
    public BoardFieldOfView.Hex evaluate(Coords c) {
        boolean highlight = boardView.shouldFovHighlight();
        boolean darken = boardView.shouldFovDarken();
        if (!highlight && !darken) {
            return BoardFieldOfView.Hex.NONE;
        }
        Coords viewerPosition = null;
        Entity viewer = boardView.getSelectedEntity();
        // In the movement phase, calc LOS based on the selected hex, otherwise use the selected Entity.
        if (boardView.game.getPhase().isMovement() && boardView.selected != null) {
            viewerPosition = boardView.selected;
        } else if (viewer != null) {
            if (viewer.isOnBoard(boardView.getBoardId())) {
                viewerPosition = viewer.getSecondaryPositions().values().stream()
                      .min(Comparator.comparingInt(co -> co.distance(c))).orElse(viewer.getPosition());
            }
        }
        if (viewerPosition == null || !boardView.getBoard().contains(viewerPosition)) {
            return BoardFieldOfView.Hex.NONE;
        }
        boolean sensorsOn = boardView.game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS)
              || (boardView.game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADVANCED_SENSORS)
                    && viewer != null && viewer.isSpaceborne());
        boolean doubleBlindOn = boardView.game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
        boolean targetIlluminated = boardView.game.getEntitiesVector(c, boardView.boardId).stream().anyMatch(Entity::isIlluminated)
              || !IlluminationLevel.determineIlluminationLevel(boardView.game, boardView.boardId, c).isNone();
        int maxDistance = viewer != null && doubleBlindOn
              ? boardView.game.getPlanetaryConditions().getVisualRange(viewer, targetIlluminated) : 60;
        int distance = viewerPosition.distance(c);
        int blue = gs.getFovSpottingMode() ? 80 : 0;
        int darkenAlpha = gs.getFovDarkenAlpha();
        int blockedTint = darken ? new Color(0, 0, blue, darkenAlpha).getRGB() : 0;
        if (distance == 0) {
            return new BoardFieldOfView.Hex(BoardFieldOfView.Visibility.ORIGIN, new Color(50, 80, 150, 70).getRGB());
        }
        // Hex previews have no target entity. Use the same range calculation as SensorRangeSpriteHandler;
        // the target-specific sensor calculation returns zero for a null target.
        Compute.SensorRangeHelper ranges = sensorsOn && viewer != null ? Compute.getSensorRanges(boardView.game, viewer) : null;
        boolean inSensorRange = false;
        if (ranges != null) {
            boolean ground = viewer.isAirborne() && boardView.game.isOnGroundMap(viewer);
            int min = ground ? ranges.minGroundSensorRange : ranges.minSensorRange;
            int max = ground ? ranges.maxGroundSensorRange : ranges.maxSensorRange;
            inSensorRange = distance > min && distance <= max;
        }
        boolean outsideSensorRange = ranges != null && !inSensorRange;
        if (distance > maxDistance) {
            return new BoardFieldOfView.Hex(BoardFieldOfView.Visibility.BLOCKED, blockedTint, outsideSensorRange);
        }
        LosEffects los = getCachedLosEffects(viewerPosition, c, boardView.getBoardId());
        int visualRange = 30;
        if (viewer != null) {
            if (los == null) {
                los = LosEffects.calculateLOS(boardView.game, viewer, null);
            }
            if (doubleBlindOn) {
                visualRange = Compute.getVisualRange(boardView.game, viewer, los, targetIlluminated);
            }
        }
        // Visual range only constrains LOS when double blind is enabled.
        if (!doubleBlindOn) {
            visualRange = distance;
        }
        if (los != null && !los.canSee() || distance > visualRange) {
            if (darken && inSensorRange) {
                return new BoardFieldOfView.Hex(BoardFieldOfView.Visibility.SENSOR,
                      new Color(0, 0, blue, darkenAlpha / 2).getRGB());
            }
            return new BoardFieldOfView.Hex(BoardFieldOfView.Visibility.BLOCKED, blockedTint, outsideSensorRange);
        }
        if (highlight) {
            Iterator<Integer> radii = ringsRadii.iterator();
            Iterator<Color> colors = ringsColors.iterator();
            while (radii.hasNext() && colors.hasNext()) {
                int radius = radii.next();
                Color color = colors.next();
                if (distance <= radius) {
                    return new BoardFieldOfView.Hex(BoardFieldOfView.Visibility.VISIBLE, color.getRGB());
                }
            }
        }
        return BoardFieldOfView.Hex.VISIBLE;
    }

    List<ECMInfo> cachedAllECMInfo = null;
    Entity cachedSelectedEntity = null;
    StepSprite cachedStepSprite = null;
    Coords cachedSrc = null;
    boolean cacheGameChanged = true;
    int cacheBoardId = -1;
    Map<Coords, LosEffects> losCache = new HashMap<>();

    /** Terrain, unit state and visibility preferences can change between turns. */
    void invalidate() {
        cacheGameChanged = true;
        clearCache();
    }

    private void clearCache() {
        losCache = new HashMap<>();
    }

    GameListener cacheGameListener;

    /**
     * Returns the cached all ECM info.
     *
     * @return the cached all ECM info, nullable
     */
    public @Nullable List<ECMInfo> getCachedECMInfo() {
        return cachedAllECMInfo;
    }

    /**
     * Checks for los effects, preferably from cache, if not getLosEffects is invoked, and it's return value is cached.
     * If environment has changed between calls to this method the cache is cleared.
     */
    public @Nullable LosEffects getCachedLosEffects(Coords src, Coords dest, int boardId) {
        ArrayList<StepSprite> pathSprites = boardView.pathSprites;
        StepSprite lastStepSprite = pathSprites.isEmpty() ? null : pathSprites.getLast();
        // let's check if cache should be cleared
        if ((cachedSelectedEntity != boardView.getSelectedEntity()) ||
              (cachedStepSprite != lastStepSprite) ||
              (!src.equals(cachedSrc)) ||
              (cacheGameChanged) ||
              (cacheBoardId != boardId)) {
            clearCache();
            cachedSelectedEntity = boardView.getSelectedEntity();
            cachedStepSprite = lastStepSprite;
            cachedSrc = src;
            cacheBoardId = boardId;
            cacheGameChanged = false;
            cachedAllECMInfo = ComputeECM.computeAllEntitiesECMInfo(boardView.game.getEntitiesVector());
        }

        LosEffects los = losCache.get(dest);
        if (los == null) {
            los = getLosEffects(src, dest, boardId);
            if (los == null) {
                return null;
            }
            losCache.put(dest, los);
        }
        return los;
    }

    /**
     * Parses the properties of rings received from GUIPreferences
     */
    private void updateRingsProperties() {
        // prepare the parameters for processing bracket by bracket
        String[] dRingsRadiiRaw = gs.getString(GUIPreferences.FOV_HIGHLIGHT_RINGS_RADII).trim().split("\\s+");
        String[] dRingsColorsRaw = gs.getString(GUIPreferences.FOV_HIGHLIGHT_RINGS_COLORS_HSB).split(";");
        final int highlight_alpha = gs.getInt(GUIPreferences.FOV_HIGHLIGHT_ALPHA);
        final int max_dist = 60;

        ringsRadii = new ArrayList<>();
        ringsColors = new ArrayList<>();

        for (String rrRaw : dRingsRadiiRaw) {
            try {
                int rr = Integer.parseInt(rrRaw.trim());
                ringsRadii.add(Math.min(rr, max_dist));
            } catch (Exception e) {
                logger.error(e,
                      String.format("Cannot parse %s parameter '%s'", GUIPreferences.FOV_HIGHLIGHT_RINGS_RADII, rrRaw));
                break;
            }
        }

        for (String rcr : dRingsColorsRaw) {
            try {
                String[] hsbr = rcr.trim().split("\\s+");
                float h = Float.parseFloat(hsbr[0]);
                float s = Float.parseFloat(hsbr[1]);
                float b = Float.parseFloat(hsbr[2]);
                Color tc = new Color(Color.HSBtoRGB(h, s, b));
                ringsColors.add(new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), highlight_alpha));
            } catch (Exception e) {
                logger.error(e, "Cannot parse {} parameter '{}'", GUIPreferences.FOV_HIGHLIGHT_RINGS_COLORS_HSB, rcr);
                break;
            }
        }
    }

    /**
     * Calculate the LosEffects between the given Coords. Unit height for the source hex is determined by the
     * selectedEntity if present otherwise the GUIPreference 'mekInFirst' is used. If pathSprites are not empty then
     * elevation from last step is used for attacker elevation, also it is assumed that last step's position is equal to
     * src. Unit height for the destination hex is determined by the tallest unit present in that hex. If no units are
     * present, the GUIPreference 'mekInSecond' is used.
     */
    private @Nullable LosEffects getLosEffects(final Coords src, final Coords dest, int boardId) {
        /*
         * The getCachedLos method depends on that this method uses only information from src, dest, game,
         * selectedEntity and the last stepSprite from path Sprites. If this behavior changes, please change the
         * getCachedLos method accordingly.
         */
        GUIPreferences guip = GUIPreferences.getInstance();
        Board board = boardView.getBoard();
        Hex srcHex = board.getHex(src);
        if (srcHex == null) {
            logger.error("Cannot process line of sight effects with a null source hex.");
            return null;
        }
        Hex dstHex = board.getHex(dest);
        if (dstHex == null) {
            logger.error("Cannot process line of sight effects with a null destination hex.");
            return null;
        }

        // Need to re-write this to work with Low Alt maps
        LosEffects.AttackInfo attackInfo = LosEffects.prepLosAttackInfo(
              boardView.game, boardView.getSelectedEntity(), null, src, dest, boardId,
              guip.getMekInFirst(), guip.getMekInSecond());
        // First, we check for a selected unit and use its height. If
        // there's no selected unit we use the mekInFirst GUIPref.
        if (boardView.getSelectedEntity() != null) {
            Entity selectedEntity = boardView.getSelectedEntity();
            // Elevation of entity above the hex surface
            int elevation = getElevation(attackInfo, selectedEntity);
            attackInfo.attackAbsHeight = (attackInfo.lowAltitude) ?
                  elevation :
                  srcHex.getLevel() + elevation + selectedEntity.getHeight();
            // Apply mast mount +1 elevation bonus when spotting mode is enabled
            // Mast mounts only provide benefit for spotting (C3, TAG, indirect fire), not direct fire
            if (!attackInfo.lowAltitude &&
                  guip.getFovSpottingMode() &&
                  selectedEntity.hasWorkingMisc(MiscType.F_MAST_MOUNT)) {
                attackInfo.attackAbsHeight += 1;
            }
        } else {
            // For hexes, getLevel is functionally the same as getAltitude()
            attackInfo.attackAbsHeight = srcHex.getLevel() + attackInfo.attackHeight;
        }
        // First, we take the tallest unit in the destination hex, if no units are
        // present we use
        // the mekInSecond GUIPref.
        attackInfo.targetHeight = attackInfo.targetAbsHeight = Integer.MIN_VALUE;
        for (Entity ent : boardView.game.getEntitiesVector(dest, boardId)) {
            int trAbsHeight = (attackInfo.lowAltitude) ? ent.getAltitude() : dstHex.getLevel() + ent.relHeight();
            if (trAbsHeight > attackInfo.targetAbsHeight) {
                attackInfo.targetHeight = ent.getHeight();
                attackInfo.targetAbsHeight = trAbsHeight;
            }
        }
        if ((attackInfo.targetHeight == Integer.MIN_VALUE) && (attackInfo.targetAbsHeight == Integer.MIN_VALUE)) {
            // Current hack for more-correct shading on low-alt maps
            attackInfo.targetHeight = (attackInfo.lowAltitude) ?
                  1 :
                  (GUIPreferences.getInstance().getMekInSecond()) ? 1 : 0;
            attackInfo.targetAbsHeight = dstHex.getLevel() + attackInfo.targetHeight;
        }
        return LosEffects.calculateLos(boardView.game, attackInfo);
    }

    private int getElevation(LosEffects.AttackInfo attackInfo, Entity ae) {
        int elevation;
        if (!boardView.pathSprites.isEmpty()) {
            // If we've got a step, get the elevation from it
            int lastStepIdx = this.boardView.pathSprites.size() - 1;
            MoveStep lastMS = this.boardView.pathSprites.get(lastStepIdx).getStep();
            elevation = (attackInfo.lowAltitude) ? lastMS.getAltitude() : lastMS.getElevation();
        } else {
            // otherwise we use entity's altitude / elevation
            elevation = (attackInfo.lowAltitude) ? ae.getAltitude() : ae.getElevation();
        }
        return elevation;
    }
}

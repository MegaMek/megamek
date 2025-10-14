/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview.toolTip;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.AttackSprite;
import megamek.client.ui.clientGUI.tooltip.HexTooltip;
import megamek.client.ui.clientGUI.tooltip.UnitToolTip;
import megamek.client.ui.util.UIUtil;
import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.Player;
import megamek.common.SpecialHexDisplay;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.EntityVisibilityUtils;
import megamek.common.units.Targetable;

public class TWBoardViewTooltip implements BoardViewTooltipProvider {

    private final GUIPreferences GUIP = GUIPreferences.getInstance();

    private final ClientGUI clientGui;
    private final Game game;
    private final BoardView bv;

    public TWBoardViewTooltip(Game game, @Nullable ClientGUI clientGui, BoardView boardView) {
        this.clientGui = clientGui;
        this.game = game;
        bv = boardView;
    }

    @Override
    public String getTooltip(Point point, Coords movementTarget) {
        final Coords coords = bv.getCoordsAt(point);
        if (!bv.getBoard().contains(coords)) {
            return null;
        }

        String fontSizeAttr = String.format("class=%s", GUIP.getUnitToolTipFontSizeMod());
        Entity selectedEntity = (clientGui != null) ? clientGui.getDisplayedUnit() : null;
        Player localPlayer = localPlayer();
        Hex mhex = bv.getBoard().getHex(coords);

        StringBuilder result = new StringBuilder();

        // Hex Terrain
        if (GUIP.getShowMapHexPopup() && (mhex != null)) {
            StringBuffer sbTerrain = new StringBuffer();
            // Embedded Board
            if (bv.getBoard().embeddedBoardCoords().contains(coords)) {
                Board embeddedBoard = game.getBoard(bv.getBoard().getEmbeddedBoardAt(coords));
                sbTerrain.append("Embedded Map: ").append(embeddedBoard.getBoardName()).append("<BR>");
            }
            appendTerrainTooltip(sbTerrain, mhex, bv.getBoardId());
            String sTerrain = sbTerrain.toString();

            // Distance from the selected unit and a planned movement end point
            if ((selectedEntity != null) && (selectedEntity.getPosition() != null) && !selectedEntity.isOffBoard()) {
                int distance = selectedEntity.getPosition().distance(coords);

                sTerrain += HexTooltip.getDistanceTip(GUIP, distance);

                int maxSensorRange = 0;
                int minSensorRange = 0;

                if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS)) {
                    LosEffects los = bv.getFovHighlighting().getCachedLosEffects(selectedEntity.getPosition(),
                          coords, bv.getBoardId());
                    int bracket = Compute.getSensorRangeBracket(selectedEntity, null,
                          bv.getFovHighlighting().getCachedECMInfo());
                    int range = Compute.getSensorRangeByBracket(game, selectedEntity, null, los);

                    maxSensorRange = bracket * range;
                    minSensorRange = Math.max((bracket - 1) * range, 0);
                    if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE)) {
                        minSensorRange = 0;
                    }
                }

                boolean isMovement = false;
                int disPM = 0;
                if (game.getPhase().isMovement() && (movementTarget != null)) {

                    disPM = movementTarget.distance(coords);
                    isMovement = true;
                }

                sTerrain += HexTooltip.getSensorRangeTip(GUIP,
                      distance,
                      minSensorRange,
                      maxSensorRange,
                      disPM,
                      isMovement);
            }
            String attr = String.format("FACE=Dialog  COLOR=%s",
                  UIUtil.toColorHexString(GUIP.getUnitToolTipTerrainFGColor()));
            sTerrain = UIUtil.tag("FONT", attr, sTerrain);
            String col = UIUtil.tag("TD", "", sTerrain);
            String row = UIUtil.tag("TR", "", col);
            attr = String.format("BORDER=0 BGCOLOR=%s width=100%%",
                  GUIPreferences.hexColor(GUIP.getUnitToolTipTerrainBGColor()));
            String table = UIUtil.tag("TABLE", attr, row);
            result.append(table);

            StringBuffer sbBuildings = new StringBuffer();
            appendBuildingsTooltip(sbBuildings, mhex, bv.getBoardId());
            result.append(sbBuildings);

            if (bv.displayInvalidFields()) {
                List<String> errors = new ArrayList<>();
                if (!mhex.isValid(errors)) {
                    String sInvalidHex = Messages.getString("BoardView1.invalidHex");
                    sInvalidHex += "<BR>";
                    sInvalidHex += String.join("<BR>", errors);
                    attr = String.format("FACE=Dialog COLOR=%s",
                          UIUtil.toColorHexString((GUIP.getUnitToolTipTerrainFGColor())));
                    sInvalidHex += UIUtil.tag("FONT", attr, sInvalidHex);
                    sInvalidHex = UIUtil.tag("span", fontSizeAttr, sInvalidHex);
                    col = UIUtil.tag("TD", "", sInvalidHex);
                    row = UIUtil.tag("TR", "", col);
                    attr = String.format("BORDER=0 BGCOLOR=%s width=100%%",
                          GUIPreferences.hexColor(GUIP.getUnitToolTipTerrainBGColor()));
                    result.append(UIUtil.tag("TABLE", attr, row));
                }
            }
        }

        // Show the player(s) that may deploy here
        // in the artillery auto hit designation phase
        if (game.getPhase().isSetArtilleryAutoHitHexes() && (mhex != null)) {
            result.append(HexTooltip.getArtilleryHit(game, coords, bv.getBoardId()));
        }

        // check if it's on any flares
        result.append(HexTooltip.getFlares(GUIP, bv, point));

        // Add wreck info
        result.append(HexTooltip.getWrecks(GUIP, bv, coords));

        // Entity tooltips
        int entityCount = 0;
        // Maximum number of entities to show in the tooltip
        int maxShown = 4;
        boolean hidden = false;

        Set<Entity> coordEntities = new HashSet<>(game.getEntitiesVector(coords, bv.getBoardId(), true));
        for (Entity entity : coordEntities) {
            entityCount++;

            // List only the first four units
            if (entityCount <= maxShown) {
                if (EntityVisibilityUtils.detectedOrHasVisual(localPlayer, game, entity)) {
                    StringBuffer sbEntity = new StringBuffer();
                    appendEntityTooltip(sbEntity, entity);
                    result.append(sbEntity);
                } else {
                    hidden = true;
                }
            }
        }
        // Info block if there are more than 4 units in that hex
        if (entityCount > maxShown && !hidden) {
            String sUnitsInfo = "There ";
            if (entityCount - maxShown == 1) {
                sUnitsInfo += "is 1 more<BR>unit";
            } else {
                sUnitsInfo += "are " + (entityCount - maxShown) + " more<BR>units";
            }
            sUnitsInfo += " in this hex...";

            String attr = String.format("FACE=Dialog  COLOR=%s",
                  UIUtil.toColorHexString(GUIP.getUnitToolTipBlockFGColor()));
            sUnitsInfo = UIUtil.tag("FONT", attr, sUnitsInfo);
            sUnitsInfo = UIUtil.tag("span", fontSizeAttr, sUnitsInfo);
            String col = UIUtil.tag("TD", "", sUnitsInfo);
            String row = UIUtil.tag("TR", "", col);
            attr = String.format("BORDER=0 BGCOLOR=%s width=100%%",
                  GUIPreferences.hexColor(GUIP.getUnitToolTipBlockBGColor()));
            String table = UIUtil.tag("TABLE", attr, row);
            result.append(table);
        }

        // check if it's on any attacks
        for (AttackSprite aSprite : bv.getAttackSprites()) {
            if (aSprite.isInside(coords)) {
                String sAttackSprite = aSprite.getTooltip().toString();

                String attr = String.format("FACE=Dialog  COLOR=%s",
                      UIUtil.toColorHexString(GUIP.getUnitToolTipAltFGColor()));
                sAttackSprite = UIUtil.tag("FONT", attr, sAttackSprite);
                sAttackSprite = UIUtil.tag("span", fontSizeAttr, sAttackSprite);
                String col = UIUtil.tag("TD", "", sAttackSprite);
                String row = UIUtil.tag("TR", "", col);
                attr = String.format("BORDER=0 BGCOLOR=%s width=100%%",
                      GUIPreferences.hexColor(GUIP.getUnitToolTipAltBGColor()));
                String table = UIUtil.tag("TABLE", attr, row);
                result.append(table);
            }
        }

        // Artillery attacks
        for (ArtilleryAttackAction aaa : getArtilleryAttacksAtLocation(game, coords)) {
            if (!bv.isOnThisBord(aaa.getTarget(game))) {
                continue;
            }
            // Default texts if no real names can be found
            String wpName = Messages.getString("BoardView1.Artillery");
            String ammoName = "Unknown";

            // Get real weapon and ammo name
            final Entity artyEnt = game.getEntity(aaa.getEntityId());
            if (artyEnt != null) {
                if (aaa.getWeaponId() > -1) {
                    wpName = artyEnt.getEquipment(aaa.getWeaponId()).getName();
                    if (aaa.getAmmoId() > -1) {
                        ammoName = artyEnt.getEquipment(aaa.getAmmoId()).getName();
                    }
                }
            }

            String msgArtilleryAttack;

            if (aaa.getTurnsTilHit() == 1) {
                msgArtilleryAttack = Messages.getString("BoardView1.Tooltip.ArtilleryAttackOne1", wpName);
                msgArtilleryAttack += "<BR>&nbsp;&nbsp;";
                msgArtilleryAttack += Messages.getString("BoardView1.Tooltip.ArtilleryAttackOne2", ammoName);
            } else {
                msgArtilleryAttack = Messages.getString("BoardView1.Tooltip.ArtilleryAttackN1",
                      wpName, aaa.getTurnsTilHit());
                msgArtilleryAttack += "<BR>&nbsp;&nbsp;";
                msgArtilleryAttack += Messages.getString("BoardView1.Tooltip.ArtilleryAttackN2", ammoName);
            }

            String attr = String.format("FACE=Dialog  COLOR=%s",
                  UIUtil.toColorHexString(GUIP.getUnitToolTipBlockFGColor()));
            msgArtilleryAttack = UIUtil.tag("FONT", attr, msgArtilleryAttack);
            msgArtilleryAttack = UIUtil.tag("span", fontSizeAttr, msgArtilleryAttack);
            String col = UIUtil.tag("TD", "", msgArtilleryAttack);
            String row = UIUtil.tag("TR", "", col);
            attr = String.format("BORDER=0 BGCOLOR=%s width=100%%",
                  GUIPreferences.hexColor(GUIP.getUnitToolTipBlockBGColor()));
            String table = UIUtil.tag("TABLE", attr, row);
            result.append(table);
        }

        // Artillery fire adjustment
        final Mounted<?> curWeapon = getSelectedArtilleryWeapon();
        if ((curWeapon != null) && (selectedEntity != null)) {
            // process targeted hexes
            int artilleryModifier;
            // Check the predesignated hexes
            if (selectedEntity.getOwner().getArtyAutoHitHexes().contains(BoardLocation.of(coords, bv.getBoardId()))) {
                artilleryModifier = TargetRoll.AUTOMATIC_SUCCESS;
            } else {
                artilleryModifier = selectedEntity.aTracker.getModifier(curWeapon, coords);
            }

            String msgArtilleryAutoHit;

            if (artilleryModifier == TargetRoll.AUTOMATIC_SUCCESS) {
                msgArtilleryAutoHit = Messages.getString("BoardView1.ArtilleryAutohit");
            } else {
                msgArtilleryAutoHit = Messages.getString("BoardView1.ArtilleryAdjustment", artilleryModifier);
            }

            String attr = String.format("FACE=Dialog  COLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipFGColor()));
            msgArtilleryAutoHit = UIUtil.tag("FONT", attr, msgArtilleryAutoHit);

            msgArtilleryAutoHit = UIUtil.tag("span", fontSizeAttr, msgArtilleryAutoHit);
            String col = UIUtil.tag("TD", "", msgArtilleryAutoHit);
            String row = UIUtil.tag("TR", "", col);
            attr = String.format("BORDER=0 BGCOLOR=%s width=100%%",
                  GUIPreferences.hexColor(GUIP.getUnitToolTipBGColor()));
            result.append(UIUtil.tag("TABLE", attr, row));
        }

        final Collection<SpecialHexDisplay> shdList = bv.getBoard().getSpecialHexDisplay(coords);
        int round = game.getRoundCount();
        if (shdList != null) {
            String sSpecialHex = "";
            for (SpecialHexDisplay shd : shdList) {
                boolean isTypeAutoHit = shd.getType() == SpecialHexDisplay.Type.ARTILLERY_AUTO_HIT;
                // Don't draw if this SHD is obscured from this player The SHD list may also contain stale SHDs, so
                // don't show tooltips for SHDs that aren't drawn. The exception is auto hits.  There will be an icon
                // for auto hits, so we need to draw a tooltip
                if (!shd.isObscured(localPlayer)
                      && (shd.drawNow(game.getPhase(), round, localPlayer, GUIP) && isTypeAutoHit)) {
                    if (shd.getType() == SpecialHexDisplay.Type.PLAYER_NOTE) {
                        if (Objects.equals(localPlayer, shd.getOwner())) {
                            sSpecialHex += "Note: ";
                        } else {
                            sSpecialHex += "Note (" + shd.getOwner().getName() + "): ";
                        }
                    }
                    String buf = shd.getInfo();
                    buf = buf.replaceAll("\\n", "<BR>");
                    sSpecialHex += buf;
                    String attr = String.format("FACE=Dialog  COLOR=%s",
                          UIUtil.toColorHexString(GUIP.getUnitToolTipFGColor()));
                    sSpecialHex = UIUtil.tag("FONT", attr, sSpecialHex);
                    sSpecialHex += "<BR>";
                }
            }

            sSpecialHex = UIUtil.tag("span", fontSizeAttr, sSpecialHex);
            String col = UIUtil.tag("TD", "", sSpecialHex);
            String row = UIUtil.tag("TR", "", col);
            String attr = String.format("BORDER=0 BGCOLOR=%s width=100%%",
                  GUIPreferences.hexColor(GUIP.getUnitToolTipBGColor()));
            result.append(UIUtil.tag("TABLE", attr, row));
        }

        StringBuilder txt = new StringBuilder();
        String attr = String.format("WIDTH=%s", UIUtil.scaleForGUI(500));
        String div = UIUtil.tag("DIV", attr, result.toString());
        txt.append(UnitToolTip.wrapWithHTML(div));

        // Check to see if the tool tip is completely empty
        if (result.isEmpty()) {
            return "";
        }

        return txt.toString();
    }

    private List<ArtilleryAttackAction> getArtilleryAttacksAtLocation(Game game, Coords c) {
        List<ArtilleryAttackAction> v = new ArrayList<>();

        for (Enumeration<ArtilleryAttackAction> attacks = game.getArtilleryAttacks(); attacks.hasMoreElements(); ) {
            ArtilleryAttackAction a = attacks.nextElement();
            Targetable target = game.getTarget(a.getTargetType(), a.getTargetId());

            if ((target != null) && c.equals(target.getPosition())) {
                v.add(a);
            }
        }
        return v;
    }

    /**
     * Appends HTML describing the terrain of a given hex
     */
    public void appendTerrainTooltip(StringBuffer txt, @Nullable Hex mhex, int boardId) {
        if (mhex == null) {
            return;
        }

        txt.append(HexTooltip.getTerrainTip(mhex, boardId, game));
    }

    public void appendBuildingsTooltip(StringBuffer txt, @Nullable Hex mhex) {
        // LEGACY replace with board ID version
        appendBuildingsTooltip(txt, mhex, 0);
    }

    /**
     * Appends HTML describing the buildings and minefields in a given hex
     */
    public void appendBuildingsTooltip(StringBuffer txt, @Nullable Hex mhex, int boardId) {
        if ((mhex != null) && (clientGui != null)) {
            String result = HexTooltip.getHexTip(mhex, clientGui.getClient(), boardId);
            txt.append(result);
        }
    }

    /**
     * Appends HTML describing a given Entity aka Unit
     */
    public void appendEntityTooltip(StringBuffer txt, @Nullable Entity entity) {
        if (entity == null) {
            return;
        }

        String result = "<HR STYLE=WIDTH:90% />";
        String entityTip = UnitToolTip.getEntityTipGame(entity, localPlayer()).toString();
        result += entityTip;

        txt.append(result);
    }

    @Nullable
    private Player localPlayer() {
        return (clientGui != null) ? clientGui.getClient().getLocalPlayer() : null;
    }

    /**
     * @return The weapon selected in the unit display if that weapon is an artillery weapon and the unit itself is
     *       owned by the local player - null otherwise
     */
    @Nullable
    private Mounted<?> getSelectedArtilleryWeapon() {
        if ((clientGui != null) && clientGui.getDisplayedWeapon().isPresent()) {
            Entity selectedUnit = clientGui.getDisplayedUnit();
            Mounted<?> selectedWeapon = clientGui.getDisplayedWeapon().get();

            // We don't want to display artillery auto-hit/adjusted fire hexes during
            // the arty auto hit hexes phase. These could be displayed if the player
            // uses the /reset command in some situations
            if ((selectedUnit != null)
                  && !game.getPhase().isSetArtilleryAutoHitHexes()
                  && Objects.equals(localPlayer(), selectedUnit.getOwner())
                  && (selectedWeapon.getType() instanceof WeaponType)
                  && selectedWeapon.getType().hasFlag(WeaponType.F_ARTILLERY)) {
                return selectedWeapon;
            }
        }
        return null;
    }
}

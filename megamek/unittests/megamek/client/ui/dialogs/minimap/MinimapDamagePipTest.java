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
package megamek.client.ui.dialogs.minimap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MinimapDamagePipTest {

    private static final int LOCAL_TEAM = 1;
    private static final int ENEMY_TEAM = 2;

    /** Symbol scale at zoom 4 (UNIT_SCALE 12), where the symbol is drawn at (20, 20) in the test image. */
    private static final double TEST_SCALE = 0.12;
    private static final int TEST_ORIGIN = 20;

    private Game game;
    private Player localPlayer;
    private Player teammate;
    private Player enemy;

    @BeforeEach
    void setUp() {
        game = new Game();
        localPlayer = addPlayer(0, "Local", LOCAL_TEAM);
        teammate = addPlayer(1, "Teammate", LOCAL_TEAM);
        enemy = addPlayer(2, "Enemy", ENEMY_TEAM);
    }

    private Player addPlayer(int id, String name, int team) {
        Player player = new Player(id, name);
        player.setTeam(team);
        game.addPlayer(id, player);
        return player;
    }

    private Entity unit(Player owner, int damageLevel) {
        Entity entity = spy(new BipedMek());
        entity.setGame(game);
        entity.setOwner(owner);
        doReturn(damageLevel).when(entity).getDamageLevel();
        return entity;
    }

    private void setOption(String option, boolean value) {
        game.getOptions().getOption(option).setValue(value);
    }

    @Test
    void damagedUnitGetsPipWhenOptionIsOn() {
        assertTrue(MinimapDamagePip.shouldDraw(unit(localPlayer, Entity.DMG_LIGHT), localPlayer, true, false));
    }

    @Test
    void undamagedUnitGetsNoPip() {
        assertFalse(MinimapDamagePip.shouldDraw(unit(localPlayer, Entity.DMG_NONE), localPlayer, true, false));
    }

    @Test
    void noPipWhenShowDamageLevelIsOff() {
        assertFalse(MinimapDamagePip.shouldDraw(unit(localPlayer, Entity.DMG_HEAVY), localPlayer, false, false));
    }

    @Test
    void destroyedUnitGetsNoPip() {
        assertFalse(MinimapDamagePip.shouldDraw(unit(localPlayer, Entity.DMG_CRIPPLED), localPlayer, true, true));
    }

    @Test
    void enemyDamageShowsWithoutDoubleBlind() {
        setOption(OptionsConstants.ADVANCED_DOUBLE_BLIND, false);
        assertTrue(MinimapDamagePip.shouldDraw(unit(enemy, Entity.DMG_HEAVY), localPlayer, true, false));
    }

    @Test
    void enemyDamageIsHiddenUnderDoubleBlind() {
        setOption(OptionsConstants.ADVANCED_DOUBLE_BLIND, true);
        assertFalse(MinimapDamagePip.shouldDraw(unit(enemy, Entity.DMG_HEAVY), localPlayer, true, false));
    }

    @Test
    void ownDamageShowsUnderDoubleBlind() {
        setOption(OptionsConstants.ADVANCED_DOUBLE_BLIND, true);
        assertTrue(MinimapDamagePip.shouldDraw(unit(localPlayer, Entity.DMG_HEAVY), localPlayer, true, false));
    }

    @Test
    void teammateDamageUnderDoubleBlindFollowsTeamVision() {
        setOption(OptionsConstants.ADVANCED_DOUBLE_BLIND, true);
        Entity teammateUnit = unit(teammate, Entity.DMG_MODERATE);

        setOption(OptionsConstants.ADVANCED_TEAM_VISION, true);
        assertTrue(MinimapDamagePip.shouldDraw(teammateUnit, localPlayer, true, false));

        setOption(OptionsConstants.ADVANCED_TEAM_VISION, false);
        assertFalse(MinimapDamagePip.shouldDraw(teammateUnit, localPlayer, true, false));
    }

    @Test
    void noLocalPlayerHidesDamageUnderDoubleBlind() {
        setOption(OptionsConstants.ADVANCED_DOUBLE_BLIND, true);
        assertFalse(MinimapDamagePip.shouldDraw(unit(localPlayer, Entity.DMG_HEAVY), null, true, false));
    }

    @Test
    void radiusIsNormalAtHighZoom() {
        // Zoom 6: UNIT_SCALE 16, so a 24-unit radius is 3.84 pixels, above the minimum
        assertEquals(MinimapDamagePip.STANDARD_RADIUS, MinimapDamagePip.radius(0.16, false), 0.001);
        assertEquals(MinimapDamagePip.STRAT_OPS_RADIUS, MinimapDamagePip.radius(0.16, true), 0.001);
    }

    @Test
    void radiusIsRaisedToMinimumAtLowZoom() {
        // Zoom 0: UNIT_SCALE 7, so a 24-unit radius would be only 1.68 pixels
        double scale = 0.07;
        double radiusInPixels = MinimapDamagePip.radius(scale, false) * scale;
        assertEquals(MinimapDamagePip.MINIMUM_RADIUS_PIXELS, radiusInPixels, 0.001);
    }

    /** Draws the pip for the unit into a grey 40 x 40 image, as the mini-map would at zoom 4. */
    private BufferedImage drawPip(Entity entity, boolean stratOps) {
        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.GRAY);
        graphics.fillRect(0, 0, 40, 40);
        graphics.translate(TEST_ORIGIN, TEST_ORIGIN);
        graphics.scale(TEST_SCALE, TEST_SCALE);
        MinimapDamagePip.draw(graphics, entity, TEST_SCALE, stratOps);
        graphics.dispose();
        return image;
    }

    /** Returns the pixel under the pip's centre. */
    private Color pipCenterPixel(BufferedImage image, boolean stratOps) {
        double centerX = stratOps ? MinimapDamagePip.STRAT_OPS_CENTER_X : MinimapDamagePip.STANDARD_CENTER_X;
        double centerY = stratOps ? MinimapDamagePip.STRAT_OPS_CENTER_Y : MinimapDamagePip.STANDARD_CENTER_Y;
        int pixelX = (int) Math.floor(TEST_ORIGIN + centerX * TEST_SCALE);
        int pixelY = (int) Math.floor(TEST_ORIGIN + centerY * TEST_SCALE);
        return new Color(image.getRGB(pixelX, pixelY));
    }

    @Test
    void drawsLightDamageInGreen() {
        BufferedImage image = drawPip(unit(localPlayer, Entity.DMG_LIGHT), false);
        assertEquals(Color.GREEN, pipCenterPixel(image, false));
    }

    @Test
    void drawsHeavyDamageInWarningColorOnStratOpsCorner() {
        BufferedImage image = drawPip(unit(localPlayer, Entity.DMG_HEAVY), true);
        Color warning = GUIPreferences.getInstance().getWarningColor();
        assertEquals(new Color(warning.getRGB() & 0xFFFFFF), pipCenterPixel(image, true));
    }

    @Test
    void drawsNothingForUndamagedUnit() {
        BufferedImage image = drawPip(unit(localPlayer, Entity.DMG_NONE), false);
        assertEquals(Color.GRAY, pipCenterPixel(image, false));
    }
}

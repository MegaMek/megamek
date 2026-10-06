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
package megamek.client.ui.tileset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.image.BufferedImage;

import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.battlefieldSupport.OverlayStyle;
import megamek.common.battlefieldSupport.StripeDirection;
import megamek.common.icons.Camouflage;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import org.junit.jupiter.api.Test;

/**
 * Tests the two values {@code TilesetManager.loadImage} uses to deduplicate cached unit images. Both were broken for
 * years, which made every {@code GameEntityChangeEvent} create and retain a fresh image set (roughly 0.5 GB per round
 * in large double-blind games, ending in an OOM):
 * <ul>
 *     <li>{@link EntityImage#getTilesetBase()} must stay identical to the tileset image the icon was created from,
 *     even though {@code loadFacings()} replaces the {@code base} image with a processed version.</li>
 *     <li>{@link EntityImage#calculateDamageLevel(Entity)} is the exact calculation stored at creation time, so the
 *     dedup check must use it too (raw {@code getDamageLevel(false)} disagrees with it, e.g. for units "crippled"
 *     only by ammo depletion).</li>
 * </ul>
 */
class EntityImageTest {

    private static final int ICON_WIDTH = 84;
    private static final int ICON_HEIGHT = 72;

    @Test
    void tilesetBaseIsStableAcrossLoadFacings() {
        // Infantry skips damage decals and smoke, keeping this test independent of decal image files
        Infantry entity = mock(Infantry.class);
        BufferedImage base = new BufferedImage(ICON_WIDTH, ICON_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        EntityImage entityImage = EntityImage.createIcon(base, null, new Camouflage(), entity, -1, true, false);
        entityImage.loadFacings();

        assertSame(base, entityImage.getTilesetBase(),
              "getTilesetBase() must keep returning the original tileset image after loadFacings(), "
                    + "otherwise the TilesetManager cache dedup never matches and images leak");
    }

    @Test
    void iconsFromTheSameTilesetImageShareTheSameTilesetBase() {
        Infantry entity = mock(Infantry.class);
        BufferedImage base = new BufferedImage(ICON_WIDTH, ICON_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        EntityImage first = EntityImage.createIcon(base, null, new Camouflage(), entity, -1, true, false);
        first.loadFacings();
        EntityImage second = EntityImage.createIcon(base, null, new Camouflage(), entity, -1, true, false);

        assertSame(first.getTilesetBase(), second.getTilesetBase(),
              "Icons created from the same tileset image must compare equal on their cache identity");
    }

    @Test
    void destroyedGunEmplacementShowsCrippledDamage() {
        Entity entity = mock(Entity.class);
        when(entity.isBuildingEntityOrGunEmplacement()).thenReturn(true);
        when(entity.isDestroyed()).thenReturn(true);

        assertEquals(Entity.DMG_CRIPPLED, EntityImage.calculateDamageLevel(entity));
    }

    @Test
    void ejectedAirborneUnitShowsAtLeastHeavyDamage() {
        Entity entity = mock(Entity.class);
        Crew crew = mock(Crew.class);
        when(entity.isAirborne()).thenReturn(true);
        when(entity.getCrew()).thenReturn(crew);
        when(crew.isEjected()).thenReturn(true);
        when(entity.getDamageLevel(false)).thenReturn(Entity.DMG_NONE);

        assertEquals(Entity.DMG_HEAVY, EntityImage.calculateDamageLevel(entity));
    }

    @Test
    void unitCrippledWithoutActualDamageShowsNoDamage() {
        // E.g. "crippled" from weapon jams or ammo depletion while armor and structure are untouched
        Entity entity = mock(Entity.class);
        when(entity.getDamageLevel()).thenReturn(Entity.DMG_CRIPPLED);
        when(entity.getArmorRemainingPercent()).thenReturn(1.0);
        when(entity.getInternalRemainingPercent()).thenReturn(1.0);

        assertEquals(Entity.DMG_NONE, EntityImage.calculateDamageLevel(entity));
    }

    @Test
    void unitWithActualDamageShowsItsDamageLevel() {
        Entity entity = mock(Entity.class);
        when(entity.getDamageLevel()).thenReturn(Entity.DMG_MODERATE);
        when(entity.getArmorRemainingPercent()).thenReturn(0.5);
        when(entity.getInternalRemainingPercent()).thenReturn(1.0);

        assertEquals(Entity.DMG_MODERATE, EntityImage.calculateDamageLevel(entity));
    }

    @Test
    void processedImageCacheSeparatesAssetFromBaseUnit() {
        Infantry baseUnit = mock(Infantry.class);
        BattlefieldSupportAsset asset = mock(BattlefieldSupportAsset.class);
        BufferedImage base = new BufferedImage(ICON_WIDTH, ICON_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Camouflage camouflage = new Camouflage();
        EntityImage cachedBase = EntityImage.createIcon(base, null, camouflage, baseUnit, -1, true, false);
        EntityImage cachedAsset = EntityImage.createIcon(base, null, camouflage, asset, -1, true, false);

        assertTrue(TilesetManager.hasSameImageCacheIdentity(cachedBase, base, camouflage, baseUnit));
        assertTrue(TilesetManager.hasSameImageCacheIdentity(cachedAsset, base, camouflage, asset));
        assertFalse(TilesetManager.hasSameImageCacheIdentity(cachedBase, base, camouflage, asset));
        assertFalse(TilesetManager.hasSameImageCacheIdentity(cachedAsset, base, camouflage, baseUnit));
    }

    @Test
    void processedImageCacheIncludesEveryAssetOverlaySetting() {
        BattlefieldSupportAsset asset = mock(BattlefieldSupportAsset.class);
        BufferedImage base = new BufferedImage(ICON_WIDTH, ICON_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Camouflage defaultCamouflage = new Camouflage();
        EntityImage cachedAsset = EntityImage.createIcon(base, null, defaultCamouflage, asset, -1, true, false);

        Camouflage differentStyle = defaultCamouflage.clone();
        differentStyle.setOverlayStyle(OverlayStyle.HAZARD);
        Camouflage differentDirection = defaultCamouflage.clone();
        differentDirection.setOverlayDirection(StripeDirection.HORIZONTAL);
        Camouflage differentColor = defaultCamouflage.clone();
        differentColor.setOverlayColor(Color.MAGENTA);

        assertTrue(TilesetManager.hasSameImageCacheIdentity(cachedAsset, base, defaultCamouflage, asset));
        assertFalse(TilesetManager.hasSameImageCacheIdentity(cachedAsset, base, differentStyle, asset),
              "Overlay style must participate in processed-image cache identity");
        assertFalse(TilesetManager.hasSameImageCacheIdentity(cachedAsset, base, differentDirection, asset),
              "Overlay direction must participate in processed-image cache identity");
        assertFalse(TilesetManager.hasSameImageCacheIdentity(cachedAsset, base, differentColor, asset),
              "Overlay color must participate in processed-image cache identity");
    }
}

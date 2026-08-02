/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 */
package megamek.client.ui.dialogs.iconChooser;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.client.ui.tileset.MekTileset;
import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.common.icons.Camouflage;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class EntityImagePanelTest {

    @Test
    void clearsPreviewWhenTilesetHasNoImageForEntity() {
        Entity entity = mock(Entity.class);
        MekTileset tileset = mock(MekTileset.class);
        when(tileset.imageFor(entity)).thenReturn(null);

        try (MockedStatic<MMStaticDirectoryManager> directories = Mockito.mockStatic(MMStaticDirectoryManager.class)) {
            directories.when(MMStaticDirectoryManager::getMekTileset).thenReturn(tileset);
            EntityImagePanel panel = new EntityImagePanel(null, new Camouflage());

            panel.updateDisplayedEntity(entity, new Camouflage());

            assertNull(panel.getImageLabel().getIcon());
        }
    }
}

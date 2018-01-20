package megamek.client.ui.swing.boardview;

import java.awt.Image;

/**
 *
 * @author arlith
 *
 */
public class HexImageCacheEntry {

    public Image hexImage;

    public boolean needsUpdating;

    HexImageCacheEntry(Image h) {
        hexImage = h;
        needsUpdating = false;
    }

}

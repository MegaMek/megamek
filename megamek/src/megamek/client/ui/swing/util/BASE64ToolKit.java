/**
 *  MegaMek - Copyright (C) 2020 - The MegaMek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.ui.swing.util;

import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;


public class BASE64ToolKit extends HTMLEditorKit {
	
	private static final long serialVersionUID = 8223662026277406659L;
	private static HTMLFactory factory = null;

    /**
     * Returns a unique url for the image. It's created by getting the code location and adding the element to it.
     * This doesn't strictly need to be an actual url, it just needs to be unique and properly formatted.
     *
     * @return the ViewFactory that creates BASE64ImageView objects
     */
    @Override
    public ViewFactory getViewFactory() {
        if (factory == null) {
            factory = new HTMLFactory() {
                @Override
                public View create(Element elem) {
                    AttributeSet attrs = elem.getAttributes();
                    Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);
                    Object o = (elementName != null) ? null : attrs.getAttribute(StyleConstants.NameAttribute);
                    if (o instanceof HTML.Tag) {
                        HTML.Tag kind = (HTML.Tag) o;
                        if (kind == HTML.Tag.IMG) {
                            return new BASE64ImageView(elem);
                        }
                    }
                    return super.create(elem);
                }
            };
        }
        return factory;
    }
}

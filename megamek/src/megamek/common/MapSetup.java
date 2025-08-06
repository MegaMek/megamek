/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import megamek.utilities.xml.MMXMLUtility;

/**
 * A helper class that is used for storing and retrieving map setups in the lobby. MapSetup objects are only used as
 * temporary objects and discarded after loading or saving the setup. The map setup consists of the map size, board size
 * and board names. The board names can include surprise boards with multiple board names and generated boards. No
 * actual hexes are saved, only board names/types. Thus, when loading a map setup with a generated board the resulting
 * board is created again using the random map settings currently in use. And, of course, this generated board is only
 * an example in turn as the actual game board is only created when the game is started.
 *
 * @author Simon (SJuliez)
 */
@XmlRootElement(name = "MAPSETUP")
@XmlAccessorType(value = XmlAccessType.NONE)
public class MapSetup implements Serializable {
    private static final long serialVersionUID = 5219340035488553080L;

    @XmlElement(name = "BOARDWIDTH")
    private int boardWidth = 16;
    @XmlElement(name = "BOARDHEIGHT")
    private int boardHeight = 17;
    @XmlElement(name = "MAPWIDTH")
    private int mapWidth = 1;
    @XmlElement(name = "MAPHEIGHT")
    private int mapHeight = 1;
    @XmlElement(name = "BOARDS")
    private List<String> boards = new ArrayList<>();

    public MapSetup(MapSettings mapSettings) {
        boardWidth = mapSettings.getBoardWidth();
        boardHeight = mapSettings.getBoardHeight();
        mapWidth = mapSettings.getMapWidth();
        mapHeight = mapSettings.getMapHeight();
        boards = mapSettings.getBoardsSelectedVector();
    }

    public MapSetup() {

    }

    /**
     * Returns a MapSetup loaded from the given InputStream. Throws JAXBExceptions.
     */
    public static MapSetup load(final InputStream is) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(MapSetup.class);
        Unmarshaller um = jc.createUnmarshaller();
        return (MapSetup) um.unmarshal(MMXMLUtility.createSafeXmlSource(is));
    }

    /**
     * Retrieves the map setup from the given mapSettings and saves the map setup to the given OutputStream. Throws
     * JAXBExceptions.
     */
    public static void save(final OutputStream os, final MapSettings mapSettings)
          throws JAXBException {
        MapSetup derivedSetup = new MapSetup(mapSettings);

        JAXBContext jc = JAXBContext.newInstance(MapSetup.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        // The default header has the encoding and standalone properties
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.setProperty("org.glassfish.jaxb.xmlHeaders", "<?xml version=\"1.0\"?>");
        JAXBElement<MapSetup> element = new JAXBElement<>(new QName("MAPSETUP"),
              MapSetup.class, derivedSetup);
        marshaller.marshal(element, os);
    }

    public int getBoardWidth() {
        return boardWidth;
    }

    public int getBoardHeight() {
        return boardHeight;
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public List<String> getBoards() {
        return boards;
    }
}

/*  
 * MegaMek - Copyright (C) 2021 - The MegaMek Team  
 *  
 * This program is free software; you can redistribute it and/or modify it under  
 * the terms of the GNU General Public License as published by the Free Software  
 * Foundation; either version 2 of the License, or (at your option) any later  
 * version.  
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT  
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
 * details.  
 */  
package megamek.common;

import jakarta.xml.bind.*;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import megamek.utilities.xml.MMXMLUtility;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;

/** 
 * A helper class that is used for storing and retrieving map setups in the 
 * lobby. MapSetup objects are only used as temporary objects and discarded
 * after loading or saving the setup. 
 * The map setup consists of the map size, board size and board names. The board
 * names can include surprise boards with multiple board names and generated 
 * boards. No actual hexes are saved, only board names/types. 
 * Thus, when loading a map setup with a generated board the resulting board is
 * created again using the random map settings currently in use. And, of course,
 * this generated board is only an example in turn as the actual game board is
 * only created when the game is started.
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
    private ArrayList<String> boards = new ArrayList<>();

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
     * Retrieves the map setup from the given mapSettings and saves the map setup
     * to the given OutputStream. Throws JAXBExceptions. 
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

    public ArrayList<String> getBoards() {
        return boards;
    }
}

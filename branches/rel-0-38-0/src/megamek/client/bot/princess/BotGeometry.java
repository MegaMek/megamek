/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

import java.util.Iterator;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.MovePath;

/**
 * This contains useful classes and functions for geometric questions
 * the bot algorithm might have
 */
public class BotGeometry {
    
    private static Princess owner;

    /**
     * The combination of a coordinate and a facing
     */
    public static class CoordFacingCombo {

        Coords coords;
        int facing;

        CoordFacingCombo(MovePath p) {
            coords=p.getFinalCoords();
            facing=p.getFinalFacing();
        }
        CoordFacingCombo(Coords c,int f) {
            coords=c;
            facing=f;
        }
        CoordFacingCombo(Entity e) {
            if (e == null)
                return;
            coords=e.getPosition();
            facing=e.getFacing();
        }

        @Override
        public boolean equals(Object o) {
            final String METHOD_NAME = "equals(Object)";
            owner.methodBegin(getClass(), METHOD_NAME);

            try {
                if (!(o instanceof CoordFacingCombo)) {
                    return false;
                }
                CoordFacingCombo c=(CoordFacingCombo)o;
                if(coords == null || !coords.equals(c.coords)) {
                    return false;
                }
                return facing == c.facing;
            } finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }

        @Override
        public int hashCode() {
            return (coords.hashCode()*6)+facing;
        }
    }

    /**
     * This describes a line in one of the 6 directions in board space
     * ---copied from Coords---
     * Coords stores x and y values. Since these are hexes, coordinates with odd x
     * values are a half-hex down. Directions work clockwise around the hex,
     * starting with zero at the top.
     *      -y
     *       0
     *     _____
     *  5 /     \ 1
     * -x /       \ +x
     *   \       /
     *  4 \_____/ 2
     *       3
     *      +y
     *    ------------------------------
     *    Direction is stored as above, but the meaning of 'intercept' depends
     *    on the direction.  For directions 0,3, intercept means the y=0 intercept
     *    for directions 1,2,4,5 intercept is the x=0 intercept
     */
    public static class HexLine {
        int intercept;
        int direction;

        /**
         * Create a hexline from a point and direction
         */
        public HexLine(Coords c,int dir) {
            final String METHOD_NAME = "HexLine(Coords, int)";

            direction=dir;
            if((direction==0)||(direction==3)) {
                intercept=c.x;
            } else if((direction==1)||(direction==4)) {
                intercept=c.y+((c.x+1)/2);
            } else {//direction==2||direction==5
                intercept=c.y-((c.x)/2);
            }
        }

        /**
         * returns -1 if the point is to the left of the line
         * +1 if the point is to the right of the line
         * and 0 if the point is on the line
         */
        public int judgePoint(Coords c) {
            final String METHOD_NAME = "judgePoint(Coords)";
            owner.methodBegin(getClass(), METHOD_NAME);

            try {
                HexLine comparor=new HexLine(c,direction);
                if(comparor.intercept<intercept) {
                    return (direction<3)?-1:1;
                } else if(comparor.intercept>intercept) {
                    return (direction<3)?1:-1;
                }
                return 0;
            } finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * returns -1 if the area is entirely to the left of the line
         * returns +1 if the area is entirely to the right of the line
         * returns 0 if the area is divided by the line
         */
        public int judgeArea(ConvexBoardArea a) {
            final String METHOD_NAME = "judgeArea(ConvexBoardArea)";
            owner.methodBegin(getClass(), METHOD_NAME);

            try {
                boolean flip=direction>2;
                if(a.edges[direction].intercept==intercept) {
                    return 0;
                }
                if(a.edges[(direction+3)%6].intercept==intercept) {
                    return 0;
                }
                boolean edgeone=(a.edges[direction].intercept<intercept)^flip;
                boolean edgetwo=(a.edges[(direction+3)%6].intercept<intercept)^flip;
                if(edgeone&&edgetwo) {
                    return 1;
                }
                if((!edgeone)&&(!edgetwo)) {
                    return -1;
                }
                return 0;
            }finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * This function only makes sense for directions 1,2,4,5
         * Note that the function getXfromY would be multvalued
         */
        public int getYfromX(int x) {
            final String METHOD_NAME = "getYfromX(int)";
            owner.methodBegin(getClass(), METHOD_NAME);

            try {
                if((direction==0)||(direction==3)) {
                    return 0;
                }
                if((direction==1)||(direction==4)) {
                    return intercept-((x+1)/2); //halfs round down
                }
                // direction==5||direction==2
                return intercept+((x)/2);     //halfs round down
            } finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * Returns the intersection point with another line
         * if lines are parallel (even if they are coincident) returns null
         */
        public Coords getIntersection(HexLine h) {
            final String METHOD_NAME = "getIntersection(HexLine)";
            owner.methodBegin(getClass(), METHOD_NAME);

            try {
                if((h.direction%3)==(direction%3)) {
                    return null;
                }
                if(h.direction==0) {
                    return h.getIntersection(this);
                }
                if(direction==2) {
                    return h.getIntersection(this);
                }
                if(direction==0) {
                    return new Coords(intercept,h.getYfromX(intercept));
                }
                //direction must be 1 here, and h.direction=2
                return new Coords(intercept-h.intercept,getYfromX(intercept-h.intercept));
            } finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * Returns the (euclidian distance) closest point on this
         * line to another point
         */
        public Coords getClosestPoint(Coords c) {
            final String METHOD_NAME = "getClosestPoint(Coords)";
            owner.methodBegin(getClass(), METHOD_NAME);

            try {
                if((direction==0)||(direction==3)) { //technically two points are equidistant, but who's counting
                    return new Coords(intercept,c.y);
                } else if((direction==1)||(direction==4)) {
                    double myx=(-2.0/3.0)*(intercept-0.5-c.y-(2.0*c.x));
                    return new Coords((int)myx,getYfromX((int)myx));
                }
                double myx=(-5.0/3.0)*(intercept-(double)c.y-(2.0*c.x));
                return new Coords((int)myx,getYfromX((int)myx));
            } finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }
    }

    /**
     * This is a convex area on the board made up of 6 lines lying along one of the
     * 3 primary directions of a hex map
     */
    public static class ConvexBoardArea {

        ConvexBoardArea() {
            final String METHOD_NAME = "ConvexBoardArea()";
            owner.methodBegin(getClass(), METHOD_NAME);

            try {
                for(int i=0;i<6;i++) {
                    edges[i]=null;
                }
            } finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }

        ConvexBoardArea(Iterator<Coords> coord_it) {
            final String METHOD_NAME = "ConvexBoardArea(Iterator<Coords>)";
            owner.methodBegin(getClass(), METHOD_NAME);

            try {
                for(int i=0;i<6;i++) {
                    edges[i]=null;
                }
                while(coord_it.hasNext()) {
                    Coords onc=coord_it.next();
                    expandToInclude(onc);
                }
            } finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }

        void addCoordFacingCombos(Iterator<CoordFacingCombo> cfit) {
            final String METHOD_NAME = "addCoordFacingCombos(Iterator<CoordFacingCombo>)";
            owner.methodBegin(getClass(), METHOD_NAME);

            try {
                while(cfit.hasNext()) {
                    CoordFacingCombo cf=cfit.next();
                    expandToInclude(cf.coords);
                }
            } finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * returns true if a point is inside the area
         * false if it is not
         */
        boolean contains(Coords c) {
            final String METHOD_NAME = "contains(Coords)";
            owner.methodBegin(getClass(), METHOD_NAME);

            try {
                if(edges[0]==null) {
                    return false;
                }
                for(int i=0;i<6;i++) {
                    if(edges[i].judgePoint(c)>0) {
                        return false;
                    }
                }
                return true;
            } finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * expands the board area to include point onc
         */
        void expandToInclude(Coords onc) {
            final String METHOD_NAME = "expandToInclude(Coords)";
            owner.methodBegin(getClass(), METHOD_NAME);

            try {
                for(int i=0;i<6;i++) {
                    if((edges[i]==null)||(edges[i].judgePoint(onc)>0)) {
                        edges[i]=new HexLine(onc,i);
                    }
                }
            } finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * Returns a vertex, with zero starting at the upper left of the hex
         */
        Coords getVertexNum(int i) {
            final String METHOD_NAME = "getVertexNum(int)";
            owner.methodBegin(getClass(), METHOD_NAME);

            try {
                return edges[i].getIntersection(edges[(i+1)%6]);
            } finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }
        /**
         * returns the closest coord in the area to the given coord
         */
        public Coords getClosestCoordsTo(Coords c) {
            final String METHOD_NAME = "getClosestCoordsTo(Coords)";
            owner.methodBegin(getClass(), METHOD_NAME);

            try {
                Coords closest=null;
                int closest_dist=0;
                for(int i=0;i<6;i++) {
                    if(edges[i].judgePoint(c)>0) {
                        Coords vert=getVertexNum(i);
                        int vdist=vert.distance(c);
                        if((closest==null)||(vdist<closest_dist)) {
                            closest=vert;
                            closest_dist=vdist;
                        }
                        Coords online=edges[i].getClosestPoint(c);
                        if(contains(online)) {
                            int ldist=online.distance(c);
                            if(ldist<closest_dist) {
                                closest=online;
                                closest_dist=ldist;
                            }
                        }
                    }
                }
                if(closest==null) {
                    return new Coords(c.x,c.y);
                }
                return closest;
            } finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }

        //left/right indicates whether its the small x
        //or large x line
        //        HexLine[] left=new HexLine[3];
        //        HexLine[] right=new HexLine[3];
        //edge points to the previous lines in the right order
        HexLine[] edges=new HexLine[6];
    }

    /**
     * runs a series of self tests to make sure geometry is done correctly
     */
    static void debugSelfTest(Princess owningPrincess) {
        owner = owningPrincess;
        final String METHOD_NAME = "debugSelfTest()";
        owner.methodBegin(BotGeometry.class, METHOD_NAME);

        try {
            owner.log(BotGeometry.class, METHOD_NAME, "Performing self test of geometry");
            Coords center=new Coords(4,6);
            HexLine []lines=new HexLine[6];
            for(int i=0;i<6;i++) {
                lines[i]=new HexLine(center,i);
            }
            owner.log(BotGeometry.class, METHOD_NAME, "Testing that center lies in lines... ");
            boolean passed=true;
            for(int i=0;i<6;i++) {
                //System.err.println("direction="+i);
                //System.err.println("0="+lines[i].judgePoint(center));
                if(lines[i].judgePoint(center)!=0) {
                    passed=false;
                }
            }
            if(passed) {
                owner.log(BotGeometry.class, METHOD_NAME, "Passed");
            } else {
                owner.log(BotGeometry.class, METHOD_NAME, "Failed");
            }
            owner.log(BotGeometry.class, METHOD_NAME, "Testing more points that should lie on lines... ");
            passed=true;
            for(int i=0;i<6;i++) {
                if((lines[i].judgePoint(center.translated(i))!=0)||(lines[i].judgePoint(center.translated((i+3)%6))!=0))
                 {
                    passed=false;
                //System.err.println("direction="+i);
                //System.err.println("0="+lines[i].judgePoint(center.translated(i)));
                //System.err.println("0="+lines[i].judgePoint(center.translated((i+3)%6)));
                }
            }
            if(passed) {
                owner.log(BotGeometry.class, METHOD_NAME, "Passed");
            } else {
                owner.log(BotGeometry.class, METHOD_NAME, "Failed");
            }
            passed=true;
            owner.log(BotGeometry.class, METHOD_NAME, "Testing points to left and right of lines... ");
            for(int i=0;i<6;i++) {
                //            System.err.println("direction="+i);
                //          System.err.println("-1="+lines[i].judgePoint(center.translated((i+5)%6)));
                if(-1!=lines[i].judgePoint(center.translated((i+5)%6))) {
                    passed=false;
                }
                //        System.err.println("-1="+lines[i].judgePoint(center.translated((i+4)%6)));
                if(-1!=lines[i].judgePoint(center.translated((i+4)%6))) {
                    passed=false;
                }
                //      System.err.println("1="+lines[i].judgePoint(center.translated((i+1)%6)));
                if(1!=lines[i].judgePoint(center.translated((i+1)%6))) {
                    passed=false;
                }
                //    System.err.println("1="+lines[i].judgePoint(center.translated((i+2)%6)));
                if(1!=lines[i].judgePoint(center.translated((i+2)%6))) {
                    passed=false;
                }
            }
            if(passed) {
                owner.log(BotGeometry.class, METHOD_NAME, "Passed");
            } else {
                owner.log(BotGeometry.class, METHOD_NAME, "Failed");
            }
            passed=true;
            Coords areapt1=new Coords(1,1);
            Coords areapt2=new Coords(3,1);
            Coords areapt3=new Coords(2,3);
            ConvexBoardArea area=new ConvexBoardArea();
            area.expandToInclude(areapt1);
            area.expandToInclude(areapt2);
            area.expandToInclude(areapt3);
            owner.log(BotGeometry.class, METHOD_NAME, "Checking area contains proper points... ");
            if(!area.contains(new Coords(1,1))) {
                passed=false;
            }
            if(!area.contains(new Coords(2,1))) {
                passed=false;
            }
            if(!area.contains(new Coords(3,1))) {
                passed=false;
            }
            if(!area.contains(new Coords(1,2))) {
                passed=false;
            }
            if(!area.contains(new Coords(2,2))) {
                passed=false;
            }
            if(!area.contains(new Coords(3,2))) {
                passed=false;
            }
            if(!area.contains(new Coords(2,3))) {
                passed=false;
            }
            if(passed) {
                owner.log(BotGeometry.class, METHOD_NAME, "Passed");
            } else {
                owner.log(BotGeometry.class, METHOD_NAME, "Failed");
            }
            passed=true;
            owner.log(BotGeometry.class, METHOD_NAME, "Checking area doesn't contain extra points... ");
            if(area.contains(new Coords(0,1))) {
                passed=false;
            }
            if(area.contains(new Coords(1,0))) {
                passed=false;
            }
            if(area.contains(new Coords(2,0))) {
                passed=false;
            }
            if(area.contains(new Coords(3,0))) {
                passed=false;
            }
            if(area.contains(new Coords(4,1))) {
                passed=false;
            }
            if(area.contains(new Coords(4,2))) {
                passed=false;
            }
            if(area.contains(new Coords(4,3))) {
                passed=false;
            }
            if(area.contains(new Coords(3,3))) {
                passed=false;
            }
            if(area.contains(new Coords(2,4))) {
                passed=false;
            }
            if(area.contains(new Coords(1,3))) {
                passed=false;
            }
            if(area.contains(new Coords(0,3))) {
                passed=false;
            }
            if(area.contains(new Coords(0,2))) {
                passed=false;
            }
            if(passed) {
                owner.log(BotGeometry.class, METHOD_NAME, "Passed");
            } else {
                owner.log(BotGeometry.class, METHOD_NAME, "Failed");
            }
            passed=true;
        } finally {
            owner.methodEnd(BotGeometry.class, METHOD_NAME);
        }
    }
}

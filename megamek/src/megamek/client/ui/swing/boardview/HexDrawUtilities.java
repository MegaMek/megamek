package megamek.client.ui.swing.boardview;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * Provides utility methods and constants for drawing hex-related shapes
 * Internally all methods work as if the game hex was
 * <BR>- as wide as BoardView1.HEX_W (usually == 84) 
 * <BR>- perfectly hex-shaped, i.e. slightly higher than BoardView1.HEX_H.
 * 
 * <BR>The methods named ...Full...Line() return closed shapes for
 * all 6 faces. They can be used to both graph.draw() and graph.fill().
 * <BR>When a border line is moved inside the hex (inset &gt; 0) and
 * for all border areas, the cuttype parameter controls how the line/area is
 * delimited. <BR>
 * CUT_BORDER extends the line/area out to the hex borders<BR>  
 * CUT_VERT leaves the line/area with the length of the hex border<BR>
 * CUT_INSIDE shrinks the line/area to be inside the triangle between the hex face and 
 * the center point.
 * CUT_LBORDER, CUT_RBORDER, CUT_LVERT, CUT_RVERT, CUT_LINSIDE, CUT_RINSIDE
 * can be ORed to achieve different cuts left and right.
 *

 * 
 * @author Simon
 */
public class HexDrawUtilities {

    public static final double HEX_WID = BoardView.HEX_W;
    public static final double HEX_HGT = HEX_WID * Math.sqrt(3) / 2;

    public static final Point2D.Double HEX_CTR = new Point2D.Double(HEX_WID / 2,HEX_HGT / 2);
    public static final Point2D.Double HEX_UL = new Point2D.Double(HEX_WID * 0.25, 0);
    public static final Point2D.Double HEX_UR = new Point2D.Double(HEX_WID * 0.75, 0);
    public static final Point2D.Double HEX_ML = new Point2D.Double(0,HEX_HGT / 2);
    public static final Point2D.Double HEX_MR = new Point2D.Double(HEX_WID,HEX_HGT / 2);
    public static final Point2D.Double HEX_LL = new Point2D.Double(HEX_WID * 0.25, HEX_HGT);
    public static final Point2D.Double HEX_LR = new Point2D.Double(HEX_WID * 0.75, HEX_HGT);
    
    public static final double PerfectHextoHexY = ((double) BoardView.HEX_H) / HEX_HGT;
    
    public static final AffineTransform PerfectHextoHex = 
            AffineTransform.getScaleInstance(1, PerfectHextoHexY);
    
    public static final int CUT_LBORDER = 1;
    public static final int CUT_RBORDER = 2;
    public static final int CUT_LINSIDE = 4;
    public static final int CUT_RINSIDE = 8;
    public static final int CUT_LVERT = 16;
    public static final int CUT_RVERT = 32;
    public static final int CUT_VERT = CUT_LVERT | CUT_RVERT;
    public static final int CUT_BORDER = CUT_LBORDER | CUT_RBORDER;
    public static final int CUT_INSIDE = CUT_LINSIDE | CUT_RINSIDE;
    
    // the slope of the side hex faces, multiplying by x gives y distance
    private static final double slopeYoverX = 2 * HEX_HGT / HEX_WID;
    
    // the slope of the side hex faces, multiplying by y gives x distance
    private static final double slopeXoverY = 1 / slopeYoverX;
    
    // the weaker slope from the upper left corner to the right mid-corner
    private static final double slopeWYoverX = slopeYoverX / 3;

    
    // Internal functions that will return perfect hex shapes

    private static Shape getHBLU() {
        Path2D.Double border = new Path2D.Double();
        border.moveTo(HEX_UL.x, HEX_UL.y);
        border.lineTo(HEX_UR.x, HEX_UR.y);
        return border;
    }
    
    private static Shape getHBLU(int hexFace) {
        return getHRU(hexFace).createTransformedShape(getHBLU());
    }
    
    
    private static Shape getHBLU(int cuttype, double inset) {
        Path2D.Double border = new Path2D.Double();
        border.moveTo(HEX_UL.x + getDeltaL(cuttype, inset), HEX_UL.y + inset);
        border.lineTo(HEX_UR.x + getDeltaR(cuttype, inset), HEX_UR.y + inset);
        return border;
    }
    
    private static Shape getHBLU(int hexFace, int cuttype, double inset) {
        return getHRU(hexFace).createTransformedShape(getHBLU(cuttype, inset));
    }
    
    private static Shape getHFBLU() {
        Path2D.Double area = new Path2D.Double();
        area.append(getHBLU(), false);
        for (int dir = 1; dir < 6; dir++) {
            area.append(getHBLU(dir), true);
        }
        area.closePath();
        return area; 
    }
    
    private static AffineTransform getHRU(int facing) {
        return AffineTransform.getRotateInstance(Math.toRadians(facing*60), HEX_CTR.x, HEX_CTR.y);
    }
    
    private static Shape getHFBLU(double inset) {
        Path2D.Double area = new Path2D.Double();
        area.append(getHBLU(CUT_INSIDE, inset), false);
        for (int dir = 1; dir < 6; dir++) {
            area.append(getHBLU(dir, CUT_INSIDE, inset), true);
        }
        area.closePath();
        return area; 
    }
    
    
    private static double getDeltaL(int cuttype, double thickness) {
        double DX = slopeXoverY * thickness;
        double LX;
        if ((cuttype & CUT_LBORDER) == CUT_LBORDER) {
            LX = -DX;
        } else if ((cuttype & CUT_LVERT) == CUT_LVERT) {
            LX = 0;
        } else {
            // CUT_LINSIDE
            LX = DX;
        }
        return LX;
    }
    
    private static double getDeltaR(int cuttype, double thickness) {
        double DX = slopeXoverY * thickness;
        double RX;
        if ((cuttype & CUT_RBORDER) == CUT_RBORDER) {
            RX = DX;
        } else if ((cuttype & CUT_RVERT) == CUT_RVERT) {
            RX = 0;
        } else {
            // CUT_RINSIDE
            RX = -DX;
        }
        return RX;
    }
    
    // a border for the facings 0, 1, 2; crossing the hex
    private static Shape getHCLU012() {
        Path2D.Double border = new Path2D.Double();
        border.moveTo(HEX_UL.x, HEX_UL.y);
        border.lineTo(HEX_LR.x, (HEX_LR.x - HEX_UL.x) * slopeWYoverX);
        border.lineTo(HEX_LR.x, HEX_LR.y);
        return border;
    }

    // the border will match with
    // normal hex borders of the same thickness
    private static Shape getHCAU012(double thickness) {
        double ft = Math.sqrt(getDeltaL(CUT_BORDER, thickness)*getDeltaL(CUT_BORDER, thickness)+
                thickness*thickness);
        double my = (HEX_LR.x - ft - HEX_UL.x - getDeltaL(CUT_BORDER, thickness))
                * slopeWYoverX + HEX_UL.y + thickness;
        Path2D.Double area = new Path2D.Double();
        area.append(getHCLU012(), false);
        area.lineTo(HEX_LR.x - ft, HEX_LR.y);
        area.lineTo(HEX_LR.x - ft, my);
        area.lineTo(HEX_UL.x + getDeltaL(CUT_BORDER, thickness), HEX_UL.y + thickness);
        area.closePath();
        return area;        
    }    
    
    // a border for the facings 0, 1; crossing the hex
    private static Shape getHCLU01() {
        Path2D.Double border = new Path2D.Double();
        border.moveTo(HEX_UL.x, HEX_UL.y);
        border.lineTo(HEX_MR.x, HEX_MR.y);
        return border;
    }

    // the border will match with
    // normal hex borders of the same thickness
    private static Shape getHCAU01(double thickness) {
        Path2D.Double area = new Path2D.Double();
        area.append(getHCLU01(), false);
        area.lineTo(HEX_MR.x + getDeltaL(CUT_BORDER, thickness), HEX_MR.y + thickness);
        area.lineTo(HEX_UL.x + getDeltaL(CUT_BORDER, thickness), HEX_UL.y + thickness);
        area.closePath();
        return area;        
    }    
    
    // a border for the facings 0, 1, 2, 3; crossing the hex
    private static Shape getHCLU0123() {
        Path2D.Double border = new Path2D.Double();
        // the 5/8 is chosen arbitrarily
        double mh = (HEX_WID * 5 / 8 - HEX_UL.x) * slopeWYoverX;
        border.moveTo(HEX_UL.x, HEX_UL.y);
        border.lineTo(HEX_WID * 5 / 8, mh);
        border.lineTo(HEX_WID * 5 / 8, HEX_LL.y - mh);
        border.lineTo(HEX_LL.x, HEX_LL.y);
        return border;
    }

    // the border will match with
    // normal hex borders of the same thickness
    private static Shape getHCAU0123(double thickness) {
        double ft = Math.sqrt(getDeltaL(CUT_BORDER, thickness)*getDeltaL(CUT_BORDER, thickness)+
                thickness*thickness);
        double my = (HEX_WID * 5 / 8 - ft - HEX_UL.x - getDeltaL(CUT_BORDER, thickness))
                * slopeWYoverX + HEX_UL.y + thickness;
        Path2D.Double area = new Path2D.Double();
        area.append(getHCLU0123(), false);
        area.lineTo(HEX_LL.x + getDeltaL(CUT_BORDER, thickness), HEX_LL.y - thickness);
        area.lineTo(HEX_WID * 5 / 8 - ft, HEX_LL.y - my);
        area.lineTo(HEX_WID * 5 / 8 - ft, HEX_UL.y + my);
        area.lineTo(HEX_UL.x + getDeltaL(CUT_BORDER, thickness), HEX_UL.y + thickness);
        area.closePath();
        return area;        
    }    
    
    // a border for the facings 0, 1, 2, 3, 4; crossing the hex
    private static Shape getHCLU01234() {
        Path2D.Double border = new Path2D.Double();
        border.moveTo(HEX_UL.x, HEX_UL.y);
        border.lineTo(HEX_LL.x, HEX_LL.y);
        border.lineTo(HEX_LR.x, HEX_LR.y);
        border.lineTo(HEX_UR.x, HEX_UR.y);
        return getHRU(5).createTransformedShape(border);
    }

    // the border will match with
    // normal hex borders of the same thickness
    private static Shape getHCAU01234(double thickness) {
        double ft = Math.sqrt(getDeltaL(CUT_BORDER, thickness)*getDeltaL(CUT_BORDER, thickness)+
                thickness*thickness);
        Path2D.Double area = new Path2D.Double();
        area.moveTo(HEX_UL.x, HEX_UL.y);
        area.lineTo(HEX_LL.x, HEX_LL.y);
        area.lineTo(HEX_LR.x, HEX_LR.y);
        area.lineTo(HEX_UR.x, HEX_UR.y);
        area.lineTo(HEX_UR.x - ft, HEX_UR.y);
        area.lineTo(HEX_LR.x - ft, HEX_LR.y - ft);
        area.lineTo(HEX_LL.x + ft, HEX_LL.y - ft);
        area.lineTo(HEX_UL.x + ft, HEX_UL.y);
        area.closePath();
        return getHRU(5).createTransformedShape(area);        
    }    
    
    private HexDrawUtilities() {
    }
    
    // external functions that will return shapes scaled to the real hex
    
    public static Shape getHexBorderLine(int hexFace, int cuttype, double inset) {
        return PerfectHextoHex.createTransformedShape(
                getHBLU(hexFace, cuttype, inset));
    }
    
    public static AffineTransform getHexRotation(int facing) {
        return AffineTransform.getRotateInstance(Math.toRadians(facing * 60), BoardView.HEX_W / 2, BoardView.HEX_H / 2);
    }
    
    public static Shape getHexBorderLine(int hexFace) {
        return PerfectHextoHex.createTransformedShape(getHBLU(hexFace));
    }
    
    public static Shape getHexFullBorderLine(double inset) {
        return PerfectHextoHex.createTransformedShape(getHFBLU(inset));
    }

    public static Shape getHexBorderArea(int cuttype, double thickness) {
        Path2D.Double area = new Path2D.Double();
        area.append(getHBLU(), false);
        area.lineTo(HEX_UR.x + getDeltaR(cuttype, thickness), HEX_UR.y + thickness);
        area.lineTo(HEX_UL.x + getDeltaL(cuttype, thickness), HEX_UL.y + thickness);
        area.closePath();
        return PerfectHextoHex.createTransformedShape(area);        
    }
    
    public static Shape getHexBorderArea(int cuttype, double thickness, double inset) {
        Path2D.Double area = new Path2D.Double();
        area.moveTo(HEX_UR.x + getDeltaR(cuttype, inset), HEX_UR.y + inset);
        area.lineTo(HEX_UL.x + getDeltaL(cuttype, inset), HEX_UL.y + inset);
        area.lineTo(HEX_UL.x + getDeltaL(cuttype, inset + thickness), 
                HEX_UL.y + inset + thickness);
        area.lineTo(HEX_UR.x + getDeltaR(cuttype, inset + thickness), 
                HEX_UR.y + inset + thickness);
        area.closePath();
        return PerfectHextoHex.createTransformedShape(area);        
    }
    
    public static Shape getHexFullBorderArea(double thickness) {
        Area area = new Area(getHFBLU());
        area.subtract(new Area(getHFBLU(thickness)));
        return PerfectHextoHex.createTransformedShape(area);        
    }
    
    public static Shape getHexFullBorderArea(double thickness, double inset) {
        Area area = new Area(getHFBLU(inset));
        area.subtract(new Area(getHFBLU(inset+thickness)));
        return PerfectHextoHex.createTransformedShape(area);        
    }
    
    public static Shape getHexBorderArea(int hexFace, int cuttype, double thickness) {
        return PerfectHextoHex.createTransformedShape(
                getHRU(hexFace).createTransformedShape(getHexBorderArea(cuttype, thickness)));       
    }
    
    public static Shape getHexBorderArea(int hexFace, int cuttype, double thickness, double inset) {
        return PerfectHextoHex.createTransformedShape(
                getHRU(hexFace).createTransformedShape(getHexBorderArea(cuttype, thickness, inset)));       
    }
    
    public static Shape getHexFull() {
        return PerfectHextoHex.createTransformedShape(getHFBLU());       
    }
    
    public static Shape getHexFull(Point2D.Double p) {
        return AffineTransform.getTranslateInstance(p.x, p.y).createTransformedShape(
                PerfectHextoHex.createTransformedShape(getHFBLU()));       
    }
    
    public static Shape getHexFull(Point p) {
        return AffineTransform.getTranslateInstance(p.x, p.y).createTransformedShape(
        getHexFullBorderLine(0));
    }
    
    public static Shape getHexFull(Point p, float scale) {
        return AffineTransform.getTranslateInstance(p.x, p.y).createTransformedShape(
                AffineTransform.getScaleInstance(scale, scale).createTransformedShape(
        getHexFullBorderLine(0)));
    }

    public static Point2D.Double getHexBorderAreaMid(int hexFace, double thickness, double inset) {
        double xN = 0;
        double yN = -HEX_CTR.y + thickness / 2;
        // rotate the point when necessary
        if (hexFace % 6 != 0) {
            double angle = Math.toRadians(hexFace * 60);
            xN = - (-HEX_CTR.y + thickness / 2) * Math.sin(angle);
            yN = + (-HEX_CTR.y + thickness / 2) * Math.cos(angle);
        }
        xN += HEX_CTR.x;
        yN += HEX_CTR.y;
        yN *= PerfectHextoHexY; 
        return new Point2D.Double(xN, yN); 
    }
    
    public static Shape getHexCrossArea01(int hexFace, double thickness) {
        return PerfectHextoHex.createTransformedShape(
                getHRU(hexFace).createTransformedShape(getHCAU01(thickness)));
    }
    
    public static Shape getHexCrossLine01(int hexFace, double thickness) {
        return PerfectHextoHex.createTransformedShape(
                getHRU(hexFace).createTransformedShape(getHCLU01()));
    }
    
    public static Shape getHexCrossArea012(int hexFace, double thickness) {
        return PerfectHextoHex.createTransformedShape(
                getHRU(hexFace).createTransformedShape(getHCAU012(thickness)));
    }
    
    public static Shape getHexCrossLine012(int hexFace, double thickness) {
        return PerfectHextoHex.createTransformedShape(
                getHRU(hexFace).createTransformedShape(getHCLU012()));
    }
    
    public static Shape getHexCrossArea0123(int hexFace, double thickness) {
        return PerfectHextoHex.createTransformedShape(
                getHRU(hexFace).createTransformedShape(getHCAU0123(thickness)));
    }
    
    public static Shape getHexCrossLine0123(int hexFace, double thickness) {
        return PerfectHextoHex.createTransformedShape(
                getHRU(hexFace).createTransformedShape(getHCLU0123()));
    }
    
    public static Shape getHexCrossArea01234(int hexFace, double thickness) {
        return PerfectHextoHex.createTransformedShape(
                getHRU(hexFace).createTransformedShape(getHCAU01234(thickness)));
    }
    
    public static Shape getHexCrossLine01234(int hexFace, double thickness) {
        return PerfectHextoHex.createTransformedShape(
                getHRU(hexFace).createTransformedShape(getHCLU01234()));
    }
}

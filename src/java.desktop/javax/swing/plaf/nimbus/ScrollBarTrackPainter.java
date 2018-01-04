/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package javax.swing.plaf.nimbus;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.Painter;


final class ScrollBarTrackPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of ScrollBarTrackPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED = 1;
    static final int BACKGROUND_ENABLED = 2;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of ScrollBarTrackPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.10016362f, 0.011764705f, 0);
    private Color color2 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.100476064f, 0.035294116f, 0);
    private Color color3 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.10606203f, 0.13333333f, 0);
    private Color color4 = decodeColor("nimbusBlueGrey", -0.6111111f, -0.110526316f, 0.24705881f, 0);
    private Color color5 = decodeColor("nimbusBlueGrey", 0.02222228f, -0.06465475f, -0.31764707f, 0);
    private Color color6 = decodeColor("nimbusBlueGrey", 0.0f, -0.06766917f, -0.19607842f, 0);
    private Color color7 = decodeColor("nimbusBlueGrey", -0.006944418f, -0.0655825f, -0.04705882f, 0);
    private Color color8 = decodeColor("nimbusBlueGrey", 0.0138888955f, -0.071117446f, 0.05098039f, 0);
    private Color color9 = decodeColor("nimbusBlueGrey", 0.0f, -0.07016757f, 0.12941176f, 0);
    private Color color10 = decodeColor("nimbusBlueGrey", 0.0f, -0.05967886f, -0.5137255f, 0);
    private Color color11 = decodeColor("nimbusBlueGrey", 0.0f, -0.05967886f, -0.5137255f, -255);
    private Color color12 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.07826825f, -0.5019608f, -255);
    private Color color13 = decodeColor("nimbusBlueGrey", -0.015872955f, -0.06731644f, -0.109803915f, 0);
    private Color color14 = decodeColor("nimbusBlueGrey", 0.0f, -0.06924191f, 0.109803915f, 0);
    private Color color15 = decodeColor("nimbusBlueGrey", -0.015872955f, -0.06861015f, -0.09019607f, 0);
    private Color color16 = decodeColor("nimbusBlueGrey", 0.0f, -0.06766917f, 0.07843137f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public ScrollBarTrackPainter(PaintContext ctx, int state) {
        super();
        this.state = state;
        this.ctx = ctx;
    }

    @Override
    protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
        //populate componentColors array with colors calculated in getExtendedCacheKeys call
        componentColors = extendedCacheKeys;
        //generate this entire method. Each state/bg/fg/border combo that has
        //been painted gets its own KEY and paint method.
        switch(state) {
            case BACKGROUND_DISABLED: paintBackgroundDisabled(g); break;
            case BACKGROUND_ENABLED: paintBackgroundEnabled(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundDisabled(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(decodeGradient1(rect));
        g.fill(rect);

    }

    private void paintBackgroundEnabled(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(decodeGradient2(rect));
        g.fill(rect);
        path = decodePath1();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient4(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient6(path));
        g.fill(path);

    }



    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(0.0f), //x
                         decodeY(0.0f), //y
                         decodeX(3.0f) - decodeX(0.0f), //width
                         decodeY(3.0f) - decodeY(0.0f)); //height
        return rect;
    }

    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(0.7f), decodeY(0.0f));
        path.lineTo(decodeX(0.0f), decodeY(0.0f));
        path.lineTo(decodeX(0.0f), decodeY(1.2f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(1.2000000476837158f, 0.0f), decodeAnchorX(0.30000001192092896f, -1.0f), decodeAnchorY(2.200000047683716f, -1.0f), decodeX(0.3f), decodeY(2.2f));
        path.curveTo(decodeAnchorX(0.30000001192092896f, 1.0f), decodeAnchorY(2.200000047683716f, 1.0f), decodeAnchorX(0.6785714030265808f, 0.0f), decodeAnchorY(2.799999952316284f, 0.0f), decodeX(0.6785714f), decodeY(2.8f));
        path.lineTo(decodeX(0.7f), decodeY(0.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(3.0f), decodeY(0.0f));
        path.lineTo(decodeX(2.2222223f), decodeY(0.0f));
        path.lineTo(decodeX(2.2222223f), decodeY(2.8f));
        path.curveTo(decodeAnchorX(2.222222328186035f, 0.0f), decodeAnchorY(2.799999952316284f, 0.0f), decodeAnchorX(2.674603223800659f, -1.0f), decodeAnchorY(2.1857142448425293f, 1.0f), decodeX(2.6746032f), decodeY(2.1857142f));
        path.curveTo(decodeAnchorX(2.674603223800659f, 1.0000000000000036f), decodeAnchorY(2.1857142448425293f, -1.0f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(1.2000000476837158f, 0.0f), decodeX(3.0f), decodeY(1.2f));
        path.lineTo(decodeX(3.0f), decodeY(0.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(0.11428572f), decodeY(1.3714286f));
        path.curveTo(decodeAnchorX(0.11428572237491608f, 0.7857142857142856f), decodeAnchorY(1.3714286088943481f, -0.571428571428573f), decodeAnchorX(0.4642857015132904f, -1.3571428571428572f), decodeAnchorY(2.0714285373687744f, -1.5714285714285694f), decodeX(0.4642857f), decodeY(2.0714285f));
        path.curveTo(decodeAnchorX(0.4642857015132904f, 1.3571428571428577f), decodeAnchorY(2.0714285373687744f, 1.5714285714285694f), decodeAnchorX(0.8714286088943481f, 0.21428571428571352f), decodeAnchorY(2.7285714149475098f, -1.0f), decodeX(0.8714286f), decodeY(2.7285714f));
        path.curveTo(decodeAnchorX(0.8714286088943481f, -0.21428571428571352f), decodeAnchorY(2.7285714149475098f, 1.0f), decodeAnchorX(0.3571428656578064f, 1.5000000000000004f), decodeAnchorY(2.3142857551574707f, 1.642857142857146f), decodeX(0.35714287f), decodeY(2.3142858f));
        path.curveTo(decodeAnchorX(0.3571428656578064f, -1.5000000000000004f), decodeAnchorY(2.3142857551574707f, -1.642857142857146f), decodeAnchorX(0.11428572237491608f, -0.7857142857142856f), decodeAnchorY(1.3714286088943481f, 0.571428571428573f), decodeX(0.11428572f), decodeY(1.3714286f));
        path.closePath();
        return path;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(2.1111112f), decodeY(2.7f));
        path.curveTo(decodeAnchorX(2.1111111640930176f, 0.4285714285714306f), decodeAnchorY(2.700000047683716f, 0.6428571428571388f), decodeAnchorX(2.626984119415283f, -1.571428571428573f), decodeAnchorY(2.200000047683716f, 1.6428571428571388f), decodeX(2.6269841f), decodeY(2.2f));
        path.curveTo(decodeAnchorX(2.626984119415283f, 1.571428571428573f), decodeAnchorY(2.200000047683716f, -1.6428571428571388f), decodeAnchorX(2.8412699699401855f, 0.7142857142857224f), decodeAnchorY(1.3857142925262451f, 0.6428571428571459f), decodeX(2.84127f), decodeY(1.3857143f));
        path.curveTo(decodeAnchorX(2.8412699699401855f, -0.7142857142857224f), decodeAnchorY(1.3857142925262451f, -0.6428571428571459f), decodeAnchorX(2.5238094329833984f, 0.7142857142857117f), decodeAnchorY(2.057142734527588f, -0.8571428571428541f), decodeX(2.5238094f), decodeY(2.0571427f));
        path.curveTo(decodeAnchorX(2.5238094329833984f, -0.7142857142857117f), decodeAnchorY(2.057142734527588f, 0.8571428571428541f), decodeAnchorX(2.1111111640930176f, -0.4285714285714306f), decodeAnchorY(2.700000047683716f, -0.6428571428571388f), decodeX(2.1111112f), decodeY(2.7f));
        path.closePath();
        return path;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.016129032f,0.038709678f,0.061290324f,0.16091082f,0.26451612f,0.4378071f,0.88387096f },
                new Color[] { color1,
                            decodeColor(color1,color2,0.5f),
                            color2,
                            decodeColor(color2,color3,0.5f),
                            color3,
                            decodeColor(color3,color4,0.5f),
                            color4});
    }

    private Paint decodeGradient2(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.030645162f,0.061290324f,0.09677419f,0.13225806f,0.22096774f,0.30967742f,0.47434634f,0.82258064f },
                new Color[] { color5,
                            decodeColor(color5,color6,0.5f),
                            color6,
                            decodeColor(color6,color7,0.5f),
                            color7,
                            decodeColor(color7,color8,0.5f),
                            color8,
                            decodeColor(color8,color9,0.5f),
                            color9});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.0f * w) + x, (0.0f * h) + y, (0.9285714f * w) + x, (0.12244898f * h) + y,
                new float[] { 0.0f,0.1f,1.0f },
                new Color[] { color10,
                            decodeColor(color10,color11,0.5f),
                            color11});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((-0.045918368f * w) + x, (0.18336426f * h) + y, (0.872449f * w) + x, (0.04050711f * h) + y,
                new float[] { 0.0f,0.87096775f,1.0f },
                new Color[] { color12,
                            decodeColor(color12,color10,0.5f),
                            color10});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.12719299f * w) + x, (0.13157894f * h) + y, (0.90789473f * w) + x, (0.877193f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color13,
                            decodeColor(color13,color14,0.5f),
                            color14});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.86458343f * w) + x, (0.20952381f * h) + y, (0.020833189f * w) + x, (0.95238096f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color15,
                            decodeColor(color15,color16,0.5f),
                            color16});
    }


}

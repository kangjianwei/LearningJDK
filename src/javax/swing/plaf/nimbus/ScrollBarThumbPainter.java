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


final class ScrollBarThumbPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of ScrollBarThumbPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED = 1;
    static final int BACKGROUND_ENABLED = 2;
    static final int BACKGROUND_FOCUSED = 3;
    static final int BACKGROUND_MOUSEOVER = 4;
    static final int BACKGROUND_PRESSED = 5;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of ScrollBarThumbPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBase", 5.1498413E-4f, 0.18061227f, -0.35686278f, 0);
    private Color color2 = decodeColor("nimbusBase", 5.1498413E-4f, -0.21018237f, -0.18039218f, 0);
    private Color color3 = decodeColor("nimbusBase", 7.13408E-4f, -0.53277314f, 0.25098038f, 0);
    private Color color4 = decodeColor("nimbusBase", -0.07865167f, -0.6317617f, 0.44313723f, 0);
    private Color color5 = decodeColor("nimbusBase", 5.1498413E-4f, -0.44340658f, 0.26666665f, 0);
    private Color color6 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4669379f, 0.38039213f, 0);
    private Color color7 = decodeColor("nimbusBase", -0.07865167f, -0.56512606f, 0.45098037f, 0);
    private Color color8 = decodeColor("nimbusBase", -0.0017285943f, -0.362987f, 0.011764705f, 0);
    private Color color9 = decodeColor("nimbusBase", 5.2034855E-5f, -0.41753247f, 0.09803921f, -222);
    private Color color10 = new Color(255, 200, 0, 255);
    private Color color11 = decodeColor("nimbusBase", -0.0017285943f, -0.362987f, 0.011764705f, -255);
    private Color color12 = decodeColor("nimbusBase", 0.010237217f, -0.5621849f, 0.25098038f, 0);
    private Color color13 = decodeColor("nimbusBase", 0.08801502f, -0.6317773f, 0.4470588f, 0);
    private Color color14 = decodeColor("nimbusBase", 5.1498413E-4f, -0.45950285f, 0.34117645f, 0);
    private Color color15 = decodeColor("nimbusBase", -0.0017285943f, -0.48277313f, 0.45098037f, 0);
    private Color color16 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, 0);
    private Color color17 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.54901963f, 0);
    private Color color18 = decodeColor("nimbusBase", 0.0013483167f, 0.29021162f, -0.33725494f, 0);
    private Color color19 = decodeColor("nimbusBase", 0.002908647f, -0.29012606f, -0.015686274f, 0);
    private Color color20 = decodeColor("nimbusBase", -8.738637E-4f, -0.40612245f, 0.21960783f, 0);
    private Color color21 = decodeColor("nimbusBase", 0.0f, -0.01765871f, 0.015686274f, 0);
    private Color color22 = decodeColor("nimbusBase", 0.0f, -0.12714285f, 0.1372549f, 0);
    private Color color23 = decodeColor("nimbusBase", 0.0018727183f, -0.23116884f, 0.31372547f, 0);
    private Color color24 = decodeColor("nimbusBase", -8.738637E-4f, -0.3579365f, -0.33725494f, 0);
    private Color color25 = decodeColor("nimbusBase", 0.004681647f, -0.3857143f, -0.36078435f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public ScrollBarThumbPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_ENABLED: paintBackgroundEnabled(g); break;
            case BACKGROUND_MOUSEOVER: paintBackgroundMouseOver(g); break;
            case BACKGROUND_PRESSED: paintBackgroundPressed(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundEnabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(decodeGradient1(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient2(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(color10);
        g.fill(path);
        path = decodePath5();
        g.setPaint(decodeGradient4(path));
        g.fill(path);

    }

    private void paintBackgroundMouseOver(Graphics2D g) {
        path = decodePath1();
        g.setPaint(decodeGradient1(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(color10);
        g.fill(path);
        path = decodePath5();
        g.setPaint(decodeGradient4(path));
        g.fill(path);

    }

    private void paintBackgroundPressed(Graphics2D g) {
        path = decodePath1();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient8(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(color10);
        g.fill(path);
        path = decodePath6();
        g.setPaint(decodeGradient9(path));
        g.fill(path);

    }



    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(1.0f));
        path.lineTo(decodeX(0.0f), decodeY(1.0666667f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(1.0666667222976685f, 6.0f), decodeAnchorX(1.0f, -10.0f), decodeAnchorY(2.0f, 0.0f), decodeX(1.0f), decodeY(2.0f));
        path.lineTo(decodeX(2.0f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(2.0f, 10.0f), decodeAnchorY(2.0f, 0.0f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(1.0666667222976685f, 6.0f), decodeX(3.0f), decodeY(1.0666667f));
        path.lineTo(decodeX(3.0f), decodeY(1.0f));
        path.lineTo(decodeX(0.0f), decodeY(1.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(0.06666667f), decodeY(1.0f));
        path.lineTo(decodeX(0.06666667f), decodeY(1.0666667f));
        path.curveTo(decodeAnchorX(0.06666667014360428f, -0.045454545454545414f), decodeAnchorY(1.0666667222976685f, 8.45454545454545f), decodeAnchorX(1.0f, -5.863636363636354f), decodeAnchorY(1.933333396911621f, 0.0f), decodeX(1.0f), decodeY(1.9333334f));
        path.lineTo(decodeX(2.0f), decodeY(1.9333334f));
        path.curveTo(decodeAnchorX(2.0f, 5.909090909090935f), decodeAnchorY(1.933333396911621f, -3.552713678800501E-15f), decodeAnchorX(2.933333396911621f, -0.045454545454546746f), decodeAnchorY(1.0666667222976685f, 8.36363636363636f), decodeX(2.9333334f), decodeY(1.0666667f));
        path.lineTo(decodeX(2.9333334f), decodeY(1.0f));
        path.lineTo(decodeX(0.06666667f), decodeY(1.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(0.4f), decodeY(1.0f));
        path.lineTo(decodeX(0.06666667f), decodeY(1.0f));
        path.lineTo(decodeX(0.16060607f), decodeY(1.5090909f));
        path.curveTo(decodeAnchorX(0.16060607135295868f, 0.0f), decodeAnchorY(1.5090909004211426f, 0.0f), decodeAnchorX(0.20000000298023224f, -0.9545454545454564f), decodeAnchorY(1.1363636255264282f, 1.5454545454545472f), decodeX(0.2f), decodeY(1.1363636f));
        path.curveTo(decodeAnchorX(0.20000000298023224f, 0.9545454545454564f), decodeAnchorY(1.1363636255264282f, -1.5454545454545472f), decodeAnchorX(0.4000000059604645f, 0.0f), decodeAnchorY(1.0f, 0.0f), decodeX(0.4f), decodeY(1.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(2.4242425f), decodeY(1.5121212f));
        path.lineTo(decodeX(2.4242425f), decodeY(1.5121212f));
        path.closePath();
        return path;
    }

    private Path2D decodePath5() {
        path.reset();
        path.moveTo(decodeX(2.9363637f), decodeY(1.0f));
        path.lineTo(decodeX(2.6030304f), decodeY(1.0f));
        path.curveTo(decodeAnchorX(2.6030304431915283f, 0.0f), decodeAnchorY(1.0f, 0.0f), decodeAnchorX(2.7787880897521973f, -0.6818181818181728f), decodeAnchorY(1.1333333253860474f, -1.227272727272727f), decodeX(2.778788f), decodeY(1.1333333f));
        path.curveTo(decodeAnchorX(2.7787880897521973f, 0.6818181818181728f), decodeAnchorY(1.1333333253860474f, 1.227272727272727f), decodeAnchorX(2.8393938541412354f, 0.0f), decodeAnchorY(1.5060606002807617f, 0.0f), decodeX(2.8393939f), decodeY(1.5060606f));
        path.lineTo(decodeX(2.9363637f), decodeY(1.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath6() {
        path.reset();
        path.moveTo(decodeX(2.9363637f), decodeY(1.0f));
        path.lineTo(decodeX(2.5563636f), decodeY(1.0f));
        path.curveTo(decodeAnchorX(2.556363582611084f, 0.0f), decodeAnchorY(1.0f, 0.0f), decodeAnchorX(2.7587878704071045f, -0.6818181818181728f), decodeAnchorY(1.1399999856948853f, -1.2272727272727266f), decodeX(2.7587879f), decodeY(1.14f));
        path.curveTo(decodeAnchorX(2.7587878704071045f, 0.6818181818181728f), decodeAnchorY(1.1399999856948853f, 1.227272727272727f), decodeAnchorX(2.8393938541412354f, 0.0f), decodeAnchorY(1.5060606002807617f, 0.0f), decodeX(2.8393939f), decodeY(1.5060606f));
        path.lineTo(decodeX(2.9363637f), decodeY(1.0f));
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
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color1,
                            decodeColor(color1,color2,0.5f),
                            color2});
    }

    private Paint decodeGradient2(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.038922157f,0.0508982f,0.06287425f,0.19610777f,0.32934132f,0.48952097f,0.6497006f,0.8248503f,1.0f },
                new Color[] { color3,
                            decodeColor(color3,color4,0.5f),
                            color4,
                            decodeColor(color4,color5,0.5f),
                            color5,
                            decodeColor(color5,color6,0.5f),
                            color6,
                            decodeColor(color6,color7,0.5f),
                            color7});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.06818182f * w) + x, (-0.005952381f * h) + y, (0.3689091f * w) + x, (0.23929171f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color8,
                            decodeColor(color8,color9,0.5f),
                            color9});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.9409091f * w) + x, (0.035928145f * h) + y, (0.5954546f * w) + x, (0.26347303f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color8,
                            decodeColor(color8,color11,0.5f),
                            color11});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.038922157f,0.0508982f,0.06287425f,0.19610777f,0.32934132f,0.48952097f,0.6497006f,0.8248503f,1.0f },
                new Color[] { color12,
                            decodeColor(color12,color13,0.5f),
                            color13,
                            decodeColor(color13,color14,0.5f),
                            color14,
                            decodeColor(color14,color15,0.5f),
                            color15,
                            decodeColor(color15,color16,0.5f),
                            color16});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color17,
                            decodeColor(color17,color18,0.5f),
                            color18});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.038922157f,0.0508982f,0.06287425f,0.19610777f,0.32934132f,0.48952097f,0.6497006f,0.8248503f,1.0f },
                new Color[] { color19,
                            decodeColor(color19,color20,0.5f),
                            color20,
                            decodeColor(color20,color21,0.5f),
                            color21,
                            decodeColor(color21,color22,0.5f),
                            color22,
                            decodeColor(color22,color23,0.5f),
                            color23});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.06818182f * w) + x, (-0.005952381f * h) + y, (0.3689091f * w) + x, (0.23929171f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color24,
                            decodeColor(color24,color9,0.5f),
                            color9});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.9409091f * w) + x, (0.035928145f * h) + y, (0.37615633f * w) + x, (0.34910178f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color25,
                            decodeColor(color25,color11,0.5f),
                            color11});
    }


}

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


final class ComboBoxArrowButtonPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of ComboBoxArrowButtonPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED = 1;
    static final int BACKGROUND_ENABLED = 2;
    static final int BACKGROUND_ENABLED_MOUSEOVER = 3;
    static final int BACKGROUND_ENABLED_PRESSED = 4;
    static final int BACKGROUND_DISABLED_EDITABLE = 5;
    static final int BACKGROUND_ENABLED_EDITABLE = 6;
    static final int BACKGROUND_MOUSEOVER_EDITABLE = 7;
    static final int BACKGROUND_PRESSED_EDITABLE = 8;
    static final int BACKGROUND_SELECTED_EDITABLE = 9;
    static final int FOREGROUND_ENABLED = 10;
    static final int FOREGROUND_MOUSEOVER = 11;
    static final int FOREGROUND_DISABLED = 12;
    static final int FOREGROUND_PRESSED = 13;
    static final int FOREGROUND_SELECTED = 14;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of ComboBoxArrowButtonPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", -0.6111111f, -0.110526316f, -0.74509805f, -247);
    private Color color2 = decodeColor("nimbusBase", 0.021348298f, -0.56289876f, 0.2588235f, 0);
    private Color color3 = decodeColor("nimbusBase", 0.010237217f, -0.55799407f, 0.20784312f, 0);
    private Color color4 = new Color(255, 200, 0, 255);
    private Color color5 = decodeColor("nimbusBase", 0.021348298f, -0.59223604f, 0.35294116f, 0);
    private Color color6 = decodeColor("nimbusBase", 0.02391243f, -0.5774183f, 0.32549018f, 0);
    private Color color7 = decodeColor("nimbusBase", 0.021348298f, -0.56722116f, 0.3098039f, 0);
    private Color color8 = decodeColor("nimbusBase", 0.021348298f, -0.567841f, 0.31764704f, 0);
    private Color color9 = decodeColor("nimbusBlueGrey", -0.6111111f, -0.110526316f, -0.74509805f, -191);
    private Color color10 = decodeColor("nimbusBase", 5.1498413E-4f, -0.34585923f, -0.007843137f, 0);
    private Color color11 = decodeColor("nimbusBase", 5.1498413E-4f, -0.095173776f, -0.25882354f, 0);
    private Color color12 = decodeColor("nimbusBase", 0.004681647f, -0.6197143f, 0.43137252f, 0);
    private Color color13 = decodeColor("nimbusBase", 0.0023007393f, -0.46825016f, 0.27058822f, 0);
    private Color color14 = decodeColor("nimbusBase", 5.1498413E-4f, -0.43866998f, 0.24705881f, 0);
    private Color color15 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4625541f, 0.35686272f, 0);
    private Color color16 = decodeColor("nimbusBase", 0.0013483167f, -0.1769987f, -0.12156865f, 0);
    private Color color17 = decodeColor("nimbusBase", 0.059279382f, 0.3642857f, -0.43529415f, 0);
    private Color color18 = decodeColor("nimbusBase", 0.004681647f, -0.6198413f, 0.43921566f, 0);
    private Color color19 = decodeColor("nimbusBase", 0.0023007393f, -0.48084703f, 0.33725488f, 0);
    private Color color20 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4555341f, 0.3215686f, 0);
    private Color color21 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4757143f, 0.43137252f, 0);
    private Color color22 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.54901963f, 0);
    private Color color23 = decodeColor("nimbusBase", -3.528595E-5f, 0.018606722f, -0.23137257f, 0);
    private Color color24 = decodeColor("nimbusBase", -4.2033195E-4f, -0.38050595f, 0.20392156f, 0);
    private Color color25 = decodeColor("nimbusBase", 7.13408E-4f, -0.064285696f, 0.027450979f, 0);
    private Color color26 = decodeColor("nimbusBase", 0.0f, -0.00895375f, 0.007843137f, 0);
    private Color color27 = decodeColor("nimbusBase", 8.9377165E-4f, -0.13853917f, 0.14509803f, 0);
    private Color color28 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.37254906f, 0);
    private Color color29 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.5254902f, 0);
    private Color color30 = decodeColor("nimbusBase", 0.027408898f, -0.57391655f, 0.1490196f, 0);
    private Color color31 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public ComboBoxArrowButtonPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_DISABLED_EDITABLE: paintBackgroundDisabledAndEditable(g); break;
            case BACKGROUND_ENABLED_EDITABLE: paintBackgroundEnabledAndEditable(g); break;
            case BACKGROUND_MOUSEOVER_EDITABLE: paintBackgroundMouseOverAndEditable(g); break;
            case BACKGROUND_PRESSED_EDITABLE: paintBackgroundPressedAndEditable(g); break;
            case BACKGROUND_SELECTED_EDITABLE: paintBackgroundSelectedAndEditable(g); break;
            case FOREGROUND_ENABLED: paintForegroundEnabled(g); break;
            case FOREGROUND_MOUSEOVER: paintForegroundMouseOver(g); break;
            case FOREGROUND_DISABLED: paintForegroundDisabled(g); break;
            case FOREGROUND_PRESSED: paintForegroundPressed(g); break;
            case FOREGROUND_SELECTED: paintForegroundSelected(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundDisabledAndEditable(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color1);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient1(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(color4);
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient2(path));
        g.fill(path);

    }

    private void paintBackgroundEnabledAndEditable(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color9);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(color4);
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient4(path));
        g.fill(path);

    }

    private void paintBackgroundMouseOverAndEditable(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color9);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(color4);
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient6(path));
        g.fill(path);

    }

    private void paintBackgroundPressedAndEditable(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color9);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(color4);
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient8(path));
        g.fill(path);

    }

    private void paintBackgroundSelectedAndEditable(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color9);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(color4);
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient8(path));
        g.fill(path);

    }

    private void paintForegroundEnabled(Graphics2D g) {
        path = decodePath5();
        g.setPaint(decodeGradient9(path));
        g.fill(path);

    }

    private void paintForegroundMouseOver(Graphics2D g) {
        path = decodePath6();
        g.setPaint(decodeGradient9(path));
        g.fill(path);

    }

    private void paintForegroundDisabled(Graphics2D g) {
        path = decodePath7();
        g.setPaint(color30);
        g.fill(path);

    }

    private void paintForegroundPressed(Graphics2D g) {
        path = decodePath8();
        g.setPaint(color31);
        g.fill(path);

    }

    private void paintForegroundSelected(Graphics2D g) {
        path = decodePath7();
        g.setPaint(color31);
        g.fill(path);

    }



    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(2.0f));
        path.lineTo(decodeX(2.75f), decodeY(2.0f));
        path.lineTo(decodeX(2.75f), decodeY(2.25f));
        path.curveTo(decodeAnchorX(2.75f, 0.0f), decodeAnchorY(2.25f, 4.0f), decodeAnchorX(2.125f, 3.0f), decodeAnchorY(2.875f, 0.0f), decodeX(2.125f), decodeY(2.875f));
        path.lineTo(decodeX(0.0f), decodeY(2.875f));
        path.lineTo(decodeX(0.0f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.25f));
        path.lineTo(decodeX(2.125f), decodeY(0.25f));
        path.curveTo(decodeAnchorX(2.125f, 3.0f), decodeAnchorY(0.25f, 0.0f), decodeAnchorX(2.75f, 0.0f), decodeAnchorY(0.875f, -3.0f), decodeX(2.75f), decodeY(0.875f));
        path.lineTo(decodeX(2.75f), decodeY(2.125f));
        path.curveTo(decodeAnchorX(2.75f, 0.0f), decodeAnchorY(2.125f, 3.0f), decodeAnchorX(2.125f, 3.0f), decodeAnchorY(2.75f, 0.0f), decodeX(2.125f), decodeY(2.75f));
        path.lineTo(decodeX(0.0f), decodeY(2.75f));
        path.lineTo(decodeX(0.0f), decodeY(0.25f));
        path.closePath();
        return path;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(0.85294116f), decodeY(2.639706f));
        path.lineTo(decodeX(0.85294116f), decodeY(2.639706f));
        path.closePath();
        return path;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(0.375f));
        path.lineTo(decodeX(2.0f), decodeY(0.375f));
        path.curveTo(decodeAnchorX(2.0f, 4.0f), decodeAnchorY(0.375f, 0.0f), decodeAnchorX(2.625f, 0.0f), decodeAnchorY(1.0f, -4.0f), decodeX(2.625f), decodeY(1.0f));
        path.lineTo(decodeX(2.625f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(2.625f, 0.0f), decodeAnchorY(2.0f, 4.0f), decodeAnchorX(2.0f, 4.0f), decodeAnchorY(2.625f, 0.0f), decodeX(2.0f), decodeY(2.625f));
        path.lineTo(decodeX(1.0f), decodeY(2.625f));
        path.lineTo(decodeX(1.0f), decodeY(0.375f));
        path.closePath();
        return path;
    }

    private Path2D decodePath5() {
        path.reset();
        path.moveTo(decodeX(0.9995915f), decodeY(1.3616071f));
        path.lineTo(decodeX(2.0f), decodeY(0.8333333f));
        path.lineTo(decodeX(2.0f), decodeY(1.8571429f));
        path.lineTo(decodeX(0.9995915f), decodeY(1.3616071f));
        path.closePath();
        return path;
    }

    private Path2D decodePath6() {
        path.reset();
        path.moveTo(decodeX(1.00625f), decodeY(1.3526785f));
        path.lineTo(decodeX(2.0f), decodeY(0.8333333f));
        path.lineTo(decodeX(2.0f), decodeY(1.8571429f));
        path.lineTo(decodeX(1.00625f), decodeY(1.3526785f));
        path.closePath();
        return path;
    }

    private Path2D decodePath7() {
        path.reset();
        path.moveTo(decodeX(1.0117648f), decodeY(1.3616071f));
        path.lineTo(decodeX(2.0f), decodeY(0.8333333f));
        path.lineTo(decodeX(2.0f), decodeY(1.8571429f));
        path.lineTo(decodeX(1.0117648f), decodeY(1.3616071f));
        path.closePath();
        return path;
    }

    private Path2D decodePath8() {
        path.reset();
        path.moveTo(decodeX(1.0242647f), decodeY(1.3526785f));
        path.lineTo(decodeX(2.0f), decodeY(0.8333333f));
        path.lineTo(decodeX(2.0f), decodeY(1.8571429f));
        path.lineTo(decodeX(1.0242647f), decodeY(1.3526785f));
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
                new Color[] { color2,
                            decodeColor(color2,color3,0.5f),
                            color3});
    }

    private Paint decodeGradient2(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.171875f,0.34375f,0.4815341f,0.6193182f,0.8096591f,1.0f },
                new Color[] { color5,
                            decodeColor(color5,color6,0.5f),
                            color6,
                            decodeColor(color6,color7,0.5f),
                            color7,
                            decodeColor(color7,color8,0.5f),
                            color8});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
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
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.12299465f,0.44652405f,0.5441176f,0.64171124f,0.8208556f,1.0f },
                new Color[] { color12,
                            decodeColor(color12,color13,0.5f),
                            color13,
                            decodeColor(color13,color14,0.5f),
                            color14,
                            decodeColor(color14,color15,0.5f),
                            color15});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color16,
                            decodeColor(color16,color17,0.5f),
                            color17});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.12299465f,0.44652405f,0.5441176f,0.64171124f,0.81283426f,0.98395723f },
                new Color[] { color18,
                            decodeColor(color18,color19,0.5f),
                            color19,
                            decodeColor(color19,color20,0.5f),
                            color20,
                            decodeColor(color20,color21,0.5f),
                            color21});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color22,
                            decodeColor(color22,color23,0.5f),
                            color23});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.12299465f,0.44652405f,0.5441176f,0.64171124f,0.8208556f,1.0f },
                new Color[] { color24,
                            decodeColor(color24,color25,0.5f),
                            color25,
                            decodeColor(color25,color26,0.5f),
                            color26,
                            decodeColor(color26,color27,0.5f),
                            color27});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((1.0f * w) + x, (0.5f * h) + y, (0.0f * w) + x, (0.5f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color28,
                            decodeColor(color28,color29,0.5f),
                            color29});
    }


}

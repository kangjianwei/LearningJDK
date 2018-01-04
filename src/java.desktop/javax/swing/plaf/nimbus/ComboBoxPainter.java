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


final class ComboBoxPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of ComboBoxPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED = 1;
    static final int BACKGROUND_DISABLED_PRESSED = 2;
    static final int BACKGROUND_ENABLED = 3;
    static final int BACKGROUND_FOCUSED = 4;
    static final int BACKGROUND_MOUSEOVER_FOCUSED = 5;
    static final int BACKGROUND_MOUSEOVER = 6;
    static final int BACKGROUND_PRESSED_FOCUSED = 7;
    static final int BACKGROUND_PRESSED = 8;
    static final int BACKGROUND_ENABLED_SELECTED = 9;
    static final int BACKGROUND_DISABLED_EDITABLE = 10;
    static final int BACKGROUND_ENABLED_EDITABLE = 11;
    static final int BACKGROUND_FOCUSED_EDITABLE = 12;
    static final int BACKGROUND_MOUSEOVER_EDITABLE = 13;
    static final int BACKGROUND_PRESSED_EDITABLE = 14;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of ComboBoxPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", -0.6111111f, -0.110526316f, -0.74509805f, -247);
    private Color color2 = decodeColor("nimbusBase", 0.032459438f, -0.5928571f, 0.2745098f, 0);
    private Color color3 = decodeColor("nimbusBase", 0.032459438f, -0.590029f, 0.2235294f, 0);
    private Color color4 = decodeColor("nimbusBase", 0.032459438f, -0.60996324f, 0.36470586f, 0);
    private Color color5 = decodeColor("nimbusBase", 0.040395975f, -0.60474086f, 0.33725488f, 0);
    private Color color6 = decodeColor("nimbusBase", 0.032459438f, -0.5953556f, 0.32549018f, 0);
    private Color color7 = decodeColor("nimbusBase", 0.032459438f, -0.5957143f, 0.3333333f, 0);
    private Color color8 = decodeColor("nimbusBase", 0.021348298f, -0.56289876f, 0.2588235f, 0);
    private Color color9 = decodeColor("nimbusBase", 0.010237217f, -0.55799407f, 0.20784312f, 0);
    private Color color10 = decodeColor("nimbusBase", 0.021348298f, -0.59223604f, 0.35294116f, 0);
    private Color color11 = decodeColor("nimbusBase", 0.02391243f, -0.5774183f, 0.32549018f, 0);
    private Color color12 = decodeColor("nimbusBase", 0.021348298f, -0.56722116f, 0.3098039f, 0);
    private Color color13 = decodeColor("nimbusBase", 0.021348298f, -0.567841f, 0.31764704f, 0);
    private Color color14 = decodeColor("nimbusBlueGrey", 0.0f, 0.0f, -0.22f, -176);
    private Color color15 = decodeColor("nimbusBase", 0.032459438f, -0.5787523f, 0.07058823f, 0);
    private Color color16 = decodeColor("nimbusBase", 0.032459438f, -0.5399696f, -0.18039218f, 0);
    private Color color17 = decodeColor("nimbusBase", 0.08801502f, -0.63174605f, 0.43921566f, 0);
    private Color color18 = decodeColor("nimbusBase", 0.040395975f, -0.6054113f, 0.35686272f, 0);
    private Color color19 = decodeColor("nimbusBase", 0.032459438f, -0.5998577f, 0.4352941f, 0);
    private Color color20 = decodeColor("nimbusBase", 5.1498413E-4f, -0.34585923f, -0.007843137f, 0);
    private Color color21 = decodeColor("nimbusBase", 5.1498413E-4f, -0.095173776f, -0.25882354f, 0);
    private Color color22 = decodeColor("nimbusBase", 0.004681647f, -0.6197143f, 0.43137252f, 0);
    private Color color23 = decodeColor("nimbusBase", -0.0028941035f, -0.4800539f, 0.28235292f, 0);
    private Color color24 = decodeColor("nimbusBase", 5.1498413E-4f, -0.43866998f, 0.24705881f, 0);
    private Color color25 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4625541f, 0.35686272f, 0);
    private Color color26 = decodeColor("nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
    private Color color27 = decodeColor("nimbusBase", 0.032459438f, -0.54616207f, -0.02352941f, 0);
    private Color color28 = decodeColor("nimbusBase", 0.032459438f, -0.41349208f, -0.33725494f, 0);
    private Color color29 = decodeColor("nimbusBase", 0.08801502f, -0.6317773f, 0.4470588f, 0);
    private Color color30 = decodeColor("nimbusBase", 0.032459438f, -0.6113241f, 0.41568625f, 0);
    private Color color31 = decodeColor("nimbusBase", 0.032459438f, -0.5985242f, 0.39999998f, 0);
    private Color color32 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, 0);
    private Color color33 = decodeColor("nimbusBase", 0.0013483167f, -0.1769987f, -0.12156865f, 0);
    private Color color34 = decodeColor("nimbusBase", 0.059279382f, 0.3642857f, -0.43529415f, 0);
    private Color color35 = decodeColor("nimbusBase", 0.004681647f, -0.6198413f, 0.43921566f, 0);
    private Color color36 = decodeColor("nimbusBase", -8.738637E-4f, -0.50527954f, 0.35294116f, 0);
    private Color color37 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4555341f, 0.3215686f, 0);
    private Color color38 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4757143f, 0.43137252f, 0);
    private Color color39 = decodeColor("nimbusBase", 0.08801502f, 0.3642857f, -0.52156866f, 0);
    private Color color40 = decodeColor("nimbusBase", 0.032459438f, -0.5246032f, -0.12549022f, 0);
    private Color color41 = decodeColor("nimbusBase", 0.027408898f, -0.5847884f, 0.2980392f, 0);
    private Color color42 = decodeColor("nimbusBase", 0.026611507f, -0.53623784f, 0.19999999f, 0);
    private Color color43 = decodeColor("nimbusBase", 0.029681683f, -0.52701867f, 0.17254901f, 0);
    private Color color44 = decodeColor("nimbusBase", 0.03801495f, -0.5456242f, 0.3215686f, 0);
    private Color color45 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.54901963f, 0);
    private Color color46 = decodeColor("nimbusBase", -3.528595E-5f, 0.018606722f, -0.23137257f, 0);
    private Color color47 = decodeColor("nimbusBase", -4.2033195E-4f, -0.38050595f, 0.20392156f, 0);
    private Color color48 = decodeColor("nimbusBase", 4.081726E-4f, -0.12922078f, 0.054901958f, 0);
    private Color color49 = decodeColor("nimbusBase", 0.0f, -0.00895375f, 0.007843137f, 0);
    private Color color50 = decodeColor("nimbusBase", -0.0015907288f, -0.1436508f, 0.19215685f, 0);
    private Color color51 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -83);
    private Color color52 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -88);
    private Color color53 = decodeColor("nimbusBlueGrey", 0.0f, -0.005263157f, -0.52156866f, -191);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public ComboBoxPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_DISABLED_PRESSED: paintBackgroundDisabledAndPressed(g); break;
            case BACKGROUND_ENABLED: paintBackgroundEnabled(g); break;
            case BACKGROUND_FOCUSED: paintBackgroundFocused(g); break;
            case BACKGROUND_MOUSEOVER_FOCUSED: paintBackgroundMouseOverAndFocused(g); break;
            case BACKGROUND_MOUSEOVER: paintBackgroundMouseOver(g); break;
            case BACKGROUND_PRESSED_FOCUSED: paintBackgroundPressedAndFocused(g); break;
            case BACKGROUND_PRESSED: paintBackgroundPressed(g); break;
            case BACKGROUND_ENABLED_SELECTED: paintBackgroundEnabledAndSelected(g); break;
            case BACKGROUND_DISABLED_EDITABLE: paintBackgroundDisabledAndEditable(g); break;
            case BACKGROUND_ENABLED_EDITABLE: paintBackgroundEnabledAndEditable(g); break;
            case BACKGROUND_FOCUSED_EDITABLE: paintBackgroundFocusedAndEditable(g); break;
            case BACKGROUND_MOUSEOVER_EDITABLE: paintBackgroundMouseOverAndEditable(g); break;
            case BACKGROUND_PRESSED_EDITABLE: paintBackgroundPressedAndEditable(g); break;

        }
    }
        
    protected Object[] getExtendedCacheKeys(JComponent c) {
        Object[] extendedCacheKeys = null;
        switch(state) {
            case BACKGROUND_ENABLED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color17, -0.63174605f, 0.43921566f, 0),
                     getComponentColor(c, "background", color18, -0.6054113f, 0.35686272f, 0),
                     getComponentColor(c, "background", color6, -0.5953556f, 0.32549018f, 0),
                     getComponentColor(c, "background", color19, -0.5998577f, 0.4352941f, 0),
                     getComponentColor(c, "background", color22, -0.6197143f, 0.43137252f, 0),
                     getComponentColor(c, "background", color23, -0.4800539f, 0.28235292f, 0),
                     getComponentColor(c, "background", color24, -0.43866998f, 0.24705881f, 0),
                     getComponentColor(c, "background", color25, -0.4625541f, 0.35686272f, 0)};
                break;
            case BACKGROUND_FOCUSED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color17, -0.63174605f, 0.43921566f, 0),
                     getComponentColor(c, "background", color18, -0.6054113f, 0.35686272f, 0),
                     getComponentColor(c, "background", color6, -0.5953556f, 0.32549018f, 0),
                     getComponentColor(c, "background", color19, -0.5998577f, 0.4352941f, 0),
                     getComponentColor(c, "background", color22, -0.6197143f, 0.43137252f, 0),
                     getComponentColor(c, "background", color23, -0.4800539f, 0.28235292f, 0),
                     getComponentColor(c, "background", color24, -0.43866998f, 0.24705881f, 0),
                     getComponentColor(c, "background", color25, -0.4625541f, 0.35686272f, 0)};
                break;
            case BACKGROUND_MOUSEOVER_FOCUSED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color29, -0.6317773f, 0.4470588f, 0),
                     getComponentColor(c, "background", color30, -0.6113241f, 0.41568625f, 0),
                     getComponentColor(c, "background", color31, -0.5985242f, 0.39999998f, 0),
                     getComponentColor(c, "background", color32, -0.6357143f, 0.45098037f, 0),
                     getComponentColor(c, "background", color35, -0.6198413f, 0.43921566f, 0),
                     getComponentColor(c, "background", color36, -0.50527954f, 0.35294116f, 0),
                     getComponentColor(c, "background", color37, -0.4555341f, 0.3215686f, 0),
                     getComponentColor(c, "background", color25, -0.4625541f, 0.35686272f, 0),
                     getComponentColor(c, "background", color38, -0.4757143f, 0.43137252f, 0)};
                break;
            case BACKGROUND_MOUSEOVER:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color29, -0.6317773f, 0.4470588f, 0),
                     getComponentColor(c, "background", color30, -0.6113241f, 0.41568625f, 0),
                     getComponentColor(c, "background", color31, -0.5985242f, 0.39999998f, 0),
                     getComponentColor(c, "background", color32, -0.6357143f, 0.45098037f, 0),
                     getComponentColor(c, "background", color35, -0.6198413f, 0.43921566f, 0),
                     getComponentColor(c, "background", color36, -0.50527954f, 0.35294116f, 0),
                     getComponentColor(c, "background", color37, -0.4555341f, 0.3215686f, 0),
                     getComponentColor(c, "background", color25, -0.4625541f, 0.35686272f, 0),
                     getComponentColor(c, "background", color38, -0.4757143f, 0.43137252f, 0)};
                break;
            case BACKGROUND_PRESSED_FOCUSED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color41, -0.5847884f, 0.2980392f, 0),
                     getComponentColor(c, "background", color42, -0.53623784f, 0.19999999f, 0),
                     getComponentColor(c, "background", color43, -0.52701867f, 0.17254901f, 0),
                     getComponentColor(c, "background", color44, -0.5456242f, 0.3215686f, 0),
                     getComponentColor(c, "background", color47, -0.38050595f, 0.20392156f, 0),
                     getComponentColor(c, "background", color48, -0.12922078f, 0.054901958f, 0),
                     getComponentColor(c, "background", color49, -0.00895375f, 0.007843137f, 0),
                     getComponentColor(c, "background", color50, -0.1436508f, 0.19215685f, 0)};
                break;
            case BACKGROUND_PRESSED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color41, -0.5847884f, 0.2980392f, 0),
                     getComponentColor(c, "background", color42, -0.53623784f, 0.19999999f, 0),
                     getComponentColor(c, "background", color43, -0.52701867f, 0.17254901f, 0),
                     getComponentColor(c, "background", color44, -0.5456242f, 0.3215686f, 0),
                     getComponentColor(c, "background", color47, -0.38050595f, 0.20392156f, 0),
                     getComponentColor(c, "background", color48, -0.12922078f, 0.054901958f, 0),
                     getComponentColor(c, "background", color49, -0.00895375f, 0.007843137f, 0),
                     getComponentColor(c, "background", color50, -0.1436508f, 0.19215685f, 0)};
                break;
            case BACKGROUND_ENABLED_SELECTED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color41, -0.5847884f, 0.2980392f, 0),
                     getComponentColor(c, "background", color42, -0.53623784f, 0.19999999f, 0),
                     getComponentColor(c, "background", color43, -0.52701867f, 0.17254901f, 0),
                     getComponentColor(c, "background", color44, -0.5456242f, 0.3215686f, 0),
                     getComponentColor(c, "background", color47, -0.38050595f, 0.20392156f, 0),
                     getComponentColor(c, "background", color48, -0.12922078f, 0.054901958f, 0),
                     getComponentColor(c, "background", color49, -0.00895375f, 0.007843137f, 0),
                     getComponentColor(c, "background", color50, -0.1436508f, 0.19215685f, 0)};
                break;
        }
        return extendedCacheKeys;
    }

    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundDisabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color1);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient1(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient2(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath5();
        g.setPaint(decodeGradient4(path));
        g.fill(path);

    }

    private void paintBackgroundDisabledAndPressed(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color1);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient1(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient2(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath5();
        g.setPaint(decodeGradient4(path));
        g.fill(path);

    }

    private void paintBackgroundEnabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color14);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath5();
        g.setPaint(decodeGradient8(path));
        g.fill(path);

    }

    private void paintBackgroundFocused(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color26);
        g.fill(roundRect);
        path = decodePath2();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath5();
        g.setPaint(decodeGradient8(path));
        g.fill(path);

    }

    private void paintBackgroundMouseOverAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color26);
        g.fill(roundRect);
        path = decodePath2();
        g.setPaint(decodeGradient9(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient10(path));
        g.fill(path);
        path = decodePath5();
        g.setPaint(decodeGradient8(path));
        g.fill(path);

    }

    private void paintBackgroundMouseOver(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color14);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient9(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient10(path));
        g.fill(path);
        path = decodePath5();
        g.setPaint(decodeGradient8(path));
        g.fill(path);

    }

    private void paintBackgroundPressedAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color26);
        g.fill(roundRect);
        path = decodePath2();
        g.setPaint(decodeGradient11(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient12(path));
        g.fill(path);
        path = decodePath5();
        g.setPaint(decodeGradient8(path));
        g.fill(path);

    }

    private void paintBackgroundPressed(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color51);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient11(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient12(path));
        g.fill(path);
        path = decodePath5();
        g.setPaint(decodeGradient8(path));
        g.fill(path);

    }

    private void paintBackgroundEnabledAndSelected(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color52);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient11(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient12(path));
        g.fill(path);
        path = decodePath5();
        g.setPaint(decodeGradient8(path));
        g.fill(path);

    }

    private void paintBackgroundDisabledAndEditable(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color53);
        g.fill(rect);

    }

    private void paintBackgroundEnabledAndEditable(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color53);
        g.fill(rect);

    }

    private void paintBackgroundFocusedAndEditable(Graphics2D g) {
        path = decodePath6();
        g.setPaint(color26);
        g.fill(path);

    }

    private void paintBackgroundMouseOverAndEditable(Graphics2D g) {
        rect = decodeRect2();
        g.setPaint(color53);
        g.fill(rect);

    }

    private void paintBackgroundPressedAndEditable(Graphics2D g) {
        rect = decodeRect2();
        g.setPaint(color53);
        g.fill(rect);

    }



    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(0.22222222f), decodeY(2.0f));
        path.lineTo(decodeX(0.22222222f), decodeY(2.25f));
        path.curveTo(decodeAnchorX(0.2222222238779068f, 0.0f), decodeAnchorY(2.25f, 3.0f), decodeAnchorX(0.7777777910232544f, -3.0f), decodeAnchorY(2.875f, 0.0f), decodeX(0.7777778f), decodeY(2.875f));
        path.lineTo(decodeX(2.631579f), decodeY(2.875f));
        path.curveTo(decodeAnchorX(2.6315789222717285f, 3.0f), decodeAnchorY(2.875f, 0.0f), decodeAnchorX(2.8947367668151855f, 0.0f), decodeAnchorY(2.25f, 3.0f), decodeX(2.8947368f), decodeY(2.25f));
        path.lineTo(decodeX(2.8947368f), decodeY(2.0f));
        path.lineTo(decodeX(0.22222222f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(0.22222222f), decodeY(0.875f));
        path.lineTo(decodeX(0.22222222f), decodeY(2.125f));
        path.curveTo(decodeAnchorX(0.2222222238779068f, 0.0f), decodeAnchorY(2.125f, 3.0f), decodeAnchorX(0.7777777910232544f, -3.0f), decodeAnchorY(2.75f, 0.0f), decodeX(0.7777778f), decodeY(2.75f));
        path.lineTo(decodeX(2.0f), decodeY(2.75f));
        path.lineTo(decodeX(2.0f), decodeY(0.25f));
        path.lineTo(decodeX(0.7777778f), decodeY(0.25f));
        path.curveTo(decodeAnchorX(0.7777777910232544f, -3.0f), decodeAnchorY(0.25f, 0.0f), decodeAnchorX(0.2222222238779068f, 0.0f), decodeAnchorY(0.875f, -3.0f), decodeX(0.22222222f), decodeY(0.875f));
        path.closePath();
        return path;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(0.8888889f), decodeY(0.375f));
        path.lineTo(decodeX(2.0f), decodeY(0.375f));
        path.lineTo(decodeX(2.0f), decodeY(2.625f));
        path.lineTo(decodeX(0.8888889f), decodeY(2.625f));
        path.curveTo(decodeAnchorX(0.8888888955116272f, -4.0f), decodeAnchorY(2.625f, 0.0f), decodeAnchorX(0.3333333432674408f, 0.0f), decodeAnchorY(2.0f, 4.0f), decodeX(0.33333334f), decodeY(2.0f));
        path.lineTo(decodeX(0.33333334f), decodeY(0.875f));
        path.curveTo(decodeAnchorX(0.3333333432674408f, 0.0f), decodeAnchorY(0.875f, -3.0f), decodeAnchorX(0.8888888955116272f, -4.0f), decodeAnchorY(0.375f, 0.0f), decodeX(0.8888889f), decodeY(0.375f));
        path.closePath();
        return path;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(2.0f), decodeY(0.25f));
        path.lineTo(decodeX(2.631579f), decodeY(0.25f));
        path.curveTo(decodeAnchorX(2.6315789222717285f, 3.0f), decodeAnchorY(0.25f, 0.0f), decodeAnchorX(2.8947367668151855f, 0.0f), decodeAnchorY(0.875f, -3.0f), decodeX(2.8947368f), decodeY(0.875f));
        path.lineTo(decodeX(2.8947368f), decodeY(2.125f));
        path.curveTo(decodeAnchorX(2.8947367668151855f, 0.0f), decodeAnchorY(2.125f, 3.0f), decodeAnchorX(2.6315789222717285f, 3.0f), decodeAnchorY(2.75f, 0.0f), decodeX(2.631579f), decodeY(2.75f));
        path.lineTo(decodeX(2.0f), decodeY(2.75f));
        path.lineTo(decodeX(2.0f), decodeY(0.25f));
        path.closePath();
        return path;
    }

    private Path2D decodePath5() {
        path.reset();
        path.moveTo(decodeX(2.0131578f), decodeY(0.375f));
        path.lineTo(decodeX(2.5789473f), decodeY(0.375f));
        path.curveTo(decodeAnchorX(2.5789473056793213f, 4.0f), decodeAnchorY(0.375f, 0.0f), decodeAnchorX(2.8421053886413574f, 0.0f), decodeAnchorY(1.0f, -4.0f), decodeX(2.8421054f), decodeY(1.0f));
        path.lineTo(decodeX(2.8421054f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(2.8421053886413574f, 0.0f), decodeAnchorY(2.0f, 4.0f), decodeAnchorX(2.5789473056793213f, 4.0f), decodeAnchorY(2.625f, 0.0f), decodeX(2.5789473f), decodeY(2.625f));
        path.lineTo(decodeX(2.0131578f), decodeY(2.625f));
        path.lineTo(decodeX(2.0131578f), decodeY(0.375f));
        path.closePath();
        return path;
    }

    private RoundRectangle2D decodeRoundRect1() {
        roundRect.setRoundRect(decodeX(0.06666667f), //x
                               decodeY(0.075f), //y
                               decodeX(2.9684212f) - decodeX(0.06666667f), //width
                               decodeY(2.925f) - decodeY(0.075f), //height
                               13.0f, 13.0f); //rounding
        return roundRect;
    }

    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(1.4385965f), //x
                         decodeY(1.4444444f), //y
                         decodeX(1.4385965f) - decodeX(1.4385965f), //width
                         decodeY(1.4444444f) - decodeY(1.4444444f)); //height
        return rect;
    }

    private Path2D decodePath6() {
        path.reset();
        path.moveTo(decodeX(0.120000005f), decodeY(0.120000005f));
        path.lineTo(decodeX(1.9954545f), decodeY(0.120000005f));
        path.curveTo(decodeAnchorX(1.9954545497894287f, 3.0f), decodeAnchorY(0.12000000476837158f, 0.0f), decodeAnchorX(2.8799986839294434f, 0.0f), decodeAnchorY(1.0941176414489746f, -2.9999999999999996f), decodeX(2.8799987f), decodeY(1.0941176f));
        path.lineTo(decodeX(2.8799987f), decodeY(1.964706f));
        path.curveTo(decodeAnchorX(2.8799986839294434f, 0.0f), decodeAnchorY(1.9647059440612793f, 3.0f), decodeAnchorX(1.9954545497894287f, 3.0f), decodeAnchorY(2.879999876022339f, 0.0f), decodeX(1.9954545f), decodeY(2.8799999f));
        path.lineTo(decodeX(0.120000005f), decodeY(2.8799999f));
        path.lineTo(decodeX(0.120000005f), decodeY(0.120000005f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect2() {
            rect.setRect(decodeX(1.4385965f), //x
                         decodeY(1.5f), //y
                         decodeX(1.4385965f) - decodeX(1.4385965f), //width
                         decodeY(1.5f) - decodeY(1.5f)); //height
        return rect;
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
                new float[] { 0.0f,0.2002841f,0.4005682f,0.5326705f,0.66477275f,0.8323864f,1.0f },
                new Color[] { color4,
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
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
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
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.171875f,0.34375f,0.4815341f,0.6193182f,0.8096591f,1.0f },
                new Color[] { color10,
                            decodeColor(color10,color11,0.5f),
                            color11,
                            decodeColor(color11,color12,0.5f),
                            color12,
                            decodeColor(color12,color13,0.5f),
                            color13});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color15,
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
                new float[] { 0.0f,0.2002841f,0.4005682f,0.5326705f,0.66477275f,0.8323864f,1.0f },
                new Color[] { (Color)componentColors[0],
                            decodeColor((Color)componentColors[0],(Color)componentColors[1],0.5f),
                            (Color)componentColors[1],
                            decodeColor((Color)componentColors[1],(Color)componentColors[2],0.5f),
                            (Color)componentColors[2],
                            decodeColor((Color)componentColors[2],(Color)componentColors[3],0.5f),
                            (Color)componentColors[3]});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color20,
                            decodeColor(color20,color21,0.5f),
                            color21});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.171875f,0.34375f,0.4815341f,0.6193182f,0.8096591f,1.0f },
                new Color[] { (Color)componentColors[4],
                            decodeColor((Color)componentColors[4],(Color)componentColors[5],0.5f),
                            (Color)componentColors[5],
                            decodeColor((Color)componentColors[5],(Color)componentColors[6],0.5f),
                            (Color)componentColors[6],
                            decodeColor((Color)componentColors[6],(Color)componentColors[7],0.5f),
                            (Color)componentColors[7]});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color27,
                            decodeColor(color27,color28,0.5f),
                            color28});
    }

    private Paint decodeGradient10(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color33,
                            decodeColor(color33,color34,0.5f),
                            color34});
    }

    private Paint decodeGradient11(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color39,
                            decodeColor(color39,color40,0.5f),
                            color40});
    }

    private Paint decodeGradient12(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color45,
                            decodeColor(color45,color46,0.5f),
                            color46});
    }


}

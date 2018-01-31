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


final class CheckBoxPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of CheckBoxPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED = 1;
    static final int BACKGROUND_ENABLED = 2;
    static final int ICON_DISABLED = 3;
    static final int ICON_ENABLED = 4;
    static final int ICON_FOCUSED = 5;
    static final int ICON_MOUSEOVER = 6;
    static final int ICON_MOUSEOVER_FOCUSED = 7;
    static final int ICON_PRESSED = 8;
    static final int ICON_PRESSED_FOCUSED = 9;
    static final int ICON_SELECTED = 10;
    static final int ICON_SELECTED_FOCUSED = 11;
    static final int ICON_PRESSED_SELECTED = 12;
    static final int ICON_PRESSED_SELECTED_FOCUSED = 13;
    static final int ICON_MOUSEOVER_SELECTED = 14;
    static final int ICON_MOUSEOVER_SELECTED_FOCUSED = 15;
    static final int ICON_DISABLED_SELECTED = 16;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of CheckBoxPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", 0.0f, -0.06766917f, 0.07843137f, 0);
    private Color color2 = decodeColor("nimbusBlueGrey", 0.0f, -0.06484103f, 0.027450979f, 0);
    private Color color3 = decodeColor("nimbusBase", 0.032459438f, -0.60996324f, 0.36470586f, 0);
    private Color color4 = decodeColor("nimbusBase", 0.02551502f, -0.5996783f, 0.3215686f, 0);
    private Color color5 = decodeColor("nimbusBase", 0.032459438f, -0.59624064f, 0.34509802f, 0);
    private Color color6 = decodeColor("nimbusBlueGrey", 0.0f, 0.0f, 0.0f, -89);
    private Color color7 = decodeColor("nimbusBlueGrey", 0.0f, -0.05356429f, -0.12549019f, 0);
    private Color color8 = decodeColor("nimbusBlueGrey", 0.0f, -0.015789472f, -0.37254903f, 0);
    private Color color9 = decodeColor("nimbusBase", 0.08801502f, -0.63174605f, 0.43921566f, 0);
    private Color color10 = decodeColor("nimbusBase", 0.032459438f, -0.5953556f, 0.32549018f, 0);
    private Color color11 = decodeColor("nimbusBase", 0.032459438f, -0.59942394f, 0.4235294f, 0);
    private Color color12 = decodeColor("nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
    private Color color13 = decodeColor("nimbusBlueGrey", 0.0f, -0.020974077f, -0.21960783f, 0);
    private Color color14 = decodeColor("nimbusBlueGrey", 0.01010108f, 0.08947369f, -0.5294118f, 0);
    private Color color15 = decodeColor("nimbusBase", 0.08801502f, -0.6317773f, 0.4470588f, 0);
    private Color color16 = decodeColor("nimbusBase", 0.032459438f, -0.5985242f, 0.39999998f, 0);
    private Color color17 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, 0);
    private Color color18 = decodeColor("nimbusBlueGrey", 0.055555582f, 0.8894737f, -0.7176471f, 0);
    private Color color19 = decodeColor("nimbusBlueGrey", 0.0f, 0.0016232133f, -0.3254902f, 0);
    private Color color20 = decodeColor("nimbusBase", 0.027408898f, -0.5847884f, 0.2980392f, 0);
    private Color color21 = decodeColor("nimbusBase", 0.029681683f, -0.52701867f, 0.17254901f, 0);
    private Color color22 = decodeColor("nimbusBase", 0.029681683f, -0.5376751f, 0.25098038f, 0);
    private Color color23 = decodeColor("nimbusBase", 5.1498413E-4f, -0.34585923f, -0.007843137f, 0);
    private Color color24 = decodeColor("nimbusBase", 5.1498413E-4f, -0.10238093f, -0.25490198f, 0);
    private Color color25 = decodeColor("nimbusBase", 0.004681647f, -0.6197143f, 0.43137252f, 0);
    private Color color26 = decodeColor("nimbusBase", 5.1498413E-4f, -0.44153953f, 0.2588235f, 0);
    private Color color27 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4602757f, 0.34509802f, 0);
    private Color color28 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.54901963f, 0);
    private Color color29 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color30 = decodeColor("nimbusBase", -3.528595E-5f, 0.026785731f, -0.23529413f, 0);
    private Color color31 = decodeColor("nimbusBase", -4.2033195E-4f, -0.38050595f, 0.20392156f, 0);
    private Color color32 = decodeColor("nimbusBase", -0.0021489263f, -0.2891234f, 0.14117646f, 0);
    private Color color33 = decodeColor("nimbusBase", -0.006362498f, -0.016311288f, -0.02352941f, 0);
    private Color color34 = decodeColor("nimbusBase", 0.0f, -0.17930403f, 0.21568626f, 0);
    private Color color35 = decodeColor("nimbusBase", 0.0013483167f, -0.1769987f, -0.12156865f, 0);
    private Color color36 = decodeColor("nimbusBase", 0.05468172f, 0.3642857f, -0.43137258f, 0);
    private Color color37 = decodeColor("nimbusBase", 0.004681647f, -0.6198413f, 0.43921566f, 0);
    private Color color38 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4555341f, 0.3215686f, 0);
    private Color color39 = decodeColor("nimbusBase", 5.1498413E-4f, -0.47377098f, 0.41960782f, 0);
    private Color color40 = decodeColor("nimbusBlueGrey", -0.01111114f, -0.03771078f, 0.062745094f, 0);
    private Color color41 = decodeColor("nimbusBlueGrey", -0.02222222f, -0.032806106f, 0.011764705f, 0);
    private Color color42 = decodeColor("nimbusBase", 0.021348298f, -0.59223604f, 0.35294116f, 0);
    private Color color43 = decodeColor("nimbusBase", 0.021348298f, -0.56722116f, 0.3098039f, 0);
    private Color color44 = decodeColor("nimbusBase", 0.021348298f, -0.56875f, 0.32941175f, 0);
    private Color color45 = decodeColor("nimbusBase", 0.027408898f, -0.5735674f, 0.14509803f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public CheckBoxPainter(PaintContext ctx, int state) {
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
            case ICON_DISABLED: painticonDisabled(g); break;
            case ICON_ENABLED: painticonEnabled(g); break;
            case ICON_FOCUSED: painticonFocused(g); break;
            case ICON_MOUSEOVER: painticonMouseOver(g); break;
            case ICON_MOUSEOVER_FOCUSED: painticonMouseOverAndFocused(g); break;
            case ICON_PRESSED: painticonPressed(g); break;
            case ICON_PRESSED_FOCUSED: painticonPressedAndFocused(g); break;
            case ICON_SELECTED: painticonSelected(g); break;
            case ICON_SELECTED_FOCUSED: painticonSelectedAndFocused(g); break;
            case ICON_PRESSED_SELECTED: painticonPressedAndSelected(g); break;
            case ICON_PRESSED_SELECTED_FOCUSED: painticonPressedAndSelectedAndFocused(g); break;
            case ICON_MOUSEOVER_SELECTED: painticonMouseOverAndSelected(g); break;
            case ICON_MOUSEOVER_SELECTED_FOCUSED: painticonMouseOverAndSelectedAndFocused(g); break;
            case ICON_DISABLED_SELECTED: painticonDisabledAndSelected(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void painticonDisabled(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient1(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);

    }

    private void painticonEnabled(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color6);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient3(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient4(roundRect));
        g.fill(roundRect);

    }

    private void painticonFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color12);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient3(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient4(roundRect));
        g.fill(roundRect);

    }

    private void painticonMouseOver(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color6);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient5(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient6(roundRect));
        g.fill(roundRect);

    }

    private void painticonMouseOverAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color12);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient5(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient6(roundRect));
        g.fill(roundRect);

    }

    private void painticonPressed(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color6);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient7(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient8(roundRect));
        g.fill(roundRect);

    }

    private void painticonPressedAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color12);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient7(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient8(roundRect));
        g.fill(roundRect);

    }

    private void painticonSelected(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color6);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient9(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient10(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(color28);
        g.fill(path);

    }

    private void painticonSelectedAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color12);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient9(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient10(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(color28);
        g.fill(path);

    }

    private void painticonPressedAndSelected(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color29);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient11(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient12(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(color28);
        g.fill(path);

    }

    private void painticonPressedAndSelectedAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color12);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient11(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient12(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(color28);
        g.fill(path);

    }

    private void painticonMouseOverAndSelected(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color6);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient13(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient14(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(color28);
        g.fill(path);

    }

    private void painticonMouseOverAndSelectedAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color12);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient13(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient14(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(color28);
        g.fill(path);

    }

    private void painticonDisabledAndSelected(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient15(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient16(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(color45);
        g.fill(path);

    }



    private RoundRectangle2D decodeRoundRect1() {
        roundRect.setRoundRect(decodeX(0.4f), //x
                               decodeY(0.4f), //y
                               decodeX(2.6f) - decodeX(0.4f), //width
                               decodeY(2.6f) - decodeY(0.4f), //height
                               3.7058823f, 3.7058823f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect2() {
        roundRect.setRoundRect(decodeX(0.6f), //x
                               decodeY(0.6f), //y
                               decodeX(2.4f) - decodeX(0.6f), //width
                               decodeY(2.4f) - decodeY(0.6f), //height
                               3.764706f, 3.764706f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect3() {
        roundRect.setRoundRect(decodeX(0.4f), //x
                               decodeY(1.75f), //y
                               decodeX(2.6f) - decodeX(0.4f), //width
                               decodeY(2.8f) - decodeY(1.75f), //height
                               5.1764708f, 5.1764708f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect4() {
        roundRect.setRoundRect(decodeX(0.120000005f), //x
                               decodeY(0.120000005f), //y
                               decodeX(2.8799999f) - decodeX(0.120000005f), //width
                               decodeY(2.8799999f) - decodeY(0.120000005f), //height
                               8.0f, 8.0f); //rounding
        return roundRect;
    }

    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(1.0036764f), decodeY(1.382353f));
        path.lineTo(decodeX(1.2536764f), decodeY(1.382353f));
        path.lineTo(decodeX(1.430147f), decodeY(1.757353f));
        path.lineTo(decodeX(1.8235294f), decodeY(0.62352943f));
        path.lineTo(decodeX(2.2f), decodeY(0.61764705f));
        path.lineTo(decodeX(1.492647f), decodeY(2.0058823f));
        path.lineTo(decodeX(1.382353f), decodeY(2.0058823f));
        path.lineTo(decodeX(1.0036764f), decodeY(1.382353f));
        path.closePath();
        return path;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25210086f * w) + x, (0.9957983f * h) + y,
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
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25f * w) + x, (0.997549f * h) + y,
                new float[] { 0.0f,0.32228917f,0.64457834f,0.82228917f,1.0f },
                new Color[] { color3,
                            decodeColor(color3,color4,0.5f),
                            color4,
                            decodeColor(color4,color5,0.5f),
                            color5});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25210086f * w) + x, (0.9957983f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color7,
                            decodeColor(color7,color8,0.5f),
                            color8});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25f * w) + x, (0.997549f * h) + y,
                new float[] { 0.0f,0.32228917f,0.64457834f,0.82228917f,1.0f },
                new Color[] { color9,
                            decodeColor(color9,color10,0.5f),
                            color10,
                            decodeColor(color10,color11,0.5f),
                            color11});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25210086f * w) + x, (0.9957983f * h) + y,
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
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25f * w) + x, (0.997549f * h) + y,
                new float[] { 0.0f,0.32228917f,0.64457834f,0.82228917f,1.0f },
                new Color[] { color15,
                            decodeColor(color15,color16,0.5f),
                            color16,
                            decodeColor(color16,color17,0.5f),
                            color17});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25210086f * w) + x, (0.9957983f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color18,
                            decodeColor(color18,color19,0.5f),
                            color19});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25f * w) + x, (0.997549f * h) + y,
                new float[] { 0.0f,0.32228917f,0.64457834f,0.82228917f,1.0f },
                new Color[] { color20,
                            decodeColor(color20,color21,0.5f),
                            color21,
                            decodeColor(color21,color22,0.5f),
                            color22});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25210086f * w) + x, (0.9957983f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color23,
                            decodeColor(color23,color24,0.5f),
                            color24});
    }

    private Paint decodeGradient10(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25f * w) + x, (0.997549f * h) + y,
                new float[] { 0.0f,0.32228917f,0.64457834f,0.82228917f,1.0f },
                new Color[] { color25,
                            decodeColor(color25,color26,0.5f),
                            color26,
                            decodeColor(color26,color27,0.5f),
                            color27});
    }

    private Paint decodeGradient11(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25210086f * w) + x, (0.9957983f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color28,
                            decodeColor(color28,color30,0.5f),
                            color30});
    }

    private Paint decodeGradient12(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25f * w) + x, (0.997549f * h) + y,
                new float[] { 0.0f,0.05775076f,0.11550152f,0.38003993f,0.64457834f,0.82228917f,1.0f },
                new Color[] { color31,
                            decodeColor(color31,color32,0.5f),
                            color32,
                            decodeColor(color32,color33,0.5f),
                            color33,
                            decodeColor(color33,color34,0.5f),
                            color34});
    }

    private Paint decodeGradient13(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25210086f * w) + x, (0.9957983f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color35,
                            decodeColor(color35,color36,0.5f),
                            color36});
    }

    private Paint decodeGradient14(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25f * w) + x, (0.997549f * h) + y,
                new float[] { 0.0f,0.32228917f,0.64457834f,0.82228917f,1.0f },
                new Color[] { color37,
                            decodeColor(color37,color38,0.5f),
                            color38,
                            decodeColor(color38,color39,0.5f),
                            color39});
    }

    private Paint decodeGradient15(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25210086f * w) + x, (0.9957983f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color40,
                            decodeColor(color40,color41,0.5f),
                            color41});
    }

    private Paint decodeGradient16(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25f * w) + x, (0.997549f * h) + y,
                new float[] { 0.0f,0.32228917f,0.64457834f,0.82228917f,1.0f },
                new Color[] { color42,
                            decodeColor(color42,color43,0.5f),
                            color43,
                            decodeColor(color43,color44,0.5f),
                            color44});
    }


}

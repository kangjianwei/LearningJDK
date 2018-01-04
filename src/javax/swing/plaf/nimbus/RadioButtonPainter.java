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


final class RadioButtonPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of RadioButtonPainter to determine which region/state is being painted
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
    //by a particular instance of RadioButtonPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", 0.0f, -0.06766917f, 0.07843137f, 0);
    private Color color2 = decodeColor("nimbusBlueGrey", 0.0f, -0.06413457f, 0.015686274f, 0);
    private Color color3 = decodeColor("nimbusBlueGrey", 0.0f, -0.08466425f, 0.16470587f, 0);
    private Color color4 = decodeColor("nimbusBlueGrey", 0.0f, -0.07016757f, 0.12941176f, 0);
    private Color color5 = decodeColor("nimbusBlueGrey", 0.0f, -0.070703305f, 0.14117646f, 0);
    private Color color6 = decodeColor("nimbusBlueGrey", 0.0f, -0.07052632f, 0.1372549f, 0);
    private Color color7 = decodeColor("nimbusBlueGrey", 0.0f, 0.0f, 0.0f, -112);
    private Color color8 = decodeColor("nimbusBlueGrey", 0.0f, -0.053201474f, -0.12941176f, 0);
    private Color color9 = decodeColor("nimbusBlueGrey", 0.0f, 0.006356798f, -0.44313726f, 0);
    private Color color10 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.10654225f, 0.23921567f, 0);
    private Color color11 = decodeColor("nimbusBlueGrey", 0.0f, -0.07206477f, 0.17254901f, 0);
    private Color color12 = decodeColor("nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
    private Color color13 = decodeColor("nimbusBlueGrey", -0.00505054f, -0.027819552f, -0.2235294f, 0);
    private Color color14 = decodeColor("nimbusBlueGrey", 0.0f, 0.24241486f, -0.6117647f, 0);
    private Color color15 = decodeColor("nimbusBlueGrey", -0.111111104f, -0.10655806f, 0.24313724f, 0);
    private Color color16 = decodeColor("nimbusBlueGrey", 0.0f, -0.07333623f, 0.20392156f, 0);
    private Color color17 = decodeColor("nimbusBlueGrey", 0.08585858f, -0.067389056f, 0.25490195f, 0);
    private Color color18 = decodeColor("nimbusBlueGrey", -0.111111104f, -0.10628903f, 0.18039215f, 0);
    private Color color19 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color20 = decodeColor("nimbusBlueGrey", 0.055555582f, 0.23947367f, -0.6666667f, 0);
    private Color color21 = decodeColor("nimbusBlueGrey", -0.0777778f, -0.06815343f, -0.28235295f, 0);
    private Color color22 = decodeColor("nimbusBlueGrey", 0.0f, -0.06866585f, 0.09803921f, 0);
    private Color color23 = decodeColor("nimbusBlueGrey", -0.0027777553f, -0.0018306673f, -0.02352941f, 0);
    private Color color24 = decodeColor("nimbusBlueGrey", 0.002924025f, -0.02047892f, 0.082352936f, 0);
    private Color color25 = decodeColor("nimbusBase", 2.9569864E-4f, -0.36035198f, -0.007843137f, 0);
    private Color color26 = decodeColor("nimbusBase", 2.9569864E-4f, 0.019458115f, -0.32156867f, 0);
    private Color color27 = decodeColor("nimbusBase", 0.004681647f, -0.6195853f, 0.4235294f, 0);
    private Color color28 = decodeColor("nimbusBase", 0.004681647f, -0.56704473f, 0.36470586f, 0);
    private Color color29 = decodeColor("nimbusBase", 5.1498413E-4f, -0.43866998f, 0.24705881f, 0);
    private Color color30 = decodeColor("nimbusBase", 5.1498413E-4f, -0.44879842f, 0.29019606f, 0);
    private Color color31 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.07243107f, -0.33333334f, 0);
    private Color color32 = decodeColor("nimbusBlueGrey", -0.6111111f, -0.110526316f, -0.74509805f, 0);
    private Color color33 = decodeColor("nimbusBlueGrey", -0.027777791f, 0.07129187f, -0.6156863f, 0);
    private Color color34 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.49803925f, 0);
    private Color color35 = decodeColor("nimbusBase", 0.0030477047f, -0.1257143f, -0.15686277f, 0);
    private Color color36 = decodeColor("nimbusBase", -0.0017285943f, -0.4367347f, 0.21960783f, 0);
    private Color color37 = decodeColor("nimbusBase", -0.0010654926f, -0.31349206f, 0.15686274f, 0);
    private Color color38 = decodeColor("nimbusBase", 0.0f, 0.0f, 0.0f, 0);
    private Color color39 = decodeColor("nimbusBase", 8.05676E-4f, -0.12380952f, 0.109803915f, 0);
    private Color color40 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.080223285f, -0.4862745f, 0);
    private Color color41 = decodeColor("nimbusBase", -6.374717E-4f, -0.20452163f, -0.12156865f, 0);
    private Color color42 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.5058824f, 0);
    private Color color43 = decodeColor("nimbusBase", -0.011985004f, -0.6157143f, 0.43137252f, 0);
    private Color color44 = decodeColor("nimbusBase", 0.004681647f, -0.56932425f, 0.3960784f, 0);
    private Color color45 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4555341f, 0.3215686f, 0);
    private Color color46 = decodeColor("nimbusBase", 5.1498413E-4f, -0.46550155f, 0.372549f, 0);
    private Color color47 = decodeColor("nimbusBase", 0.0024294257f, -0.47271872f, 0.34117645f, 0);
    private Color color48 = decodeColor("nimbusBase", 0.010237217f, -0.56289876f, 0.2588235f, 0);
    private Color color49 = decodeColor("nimbusBase", 0.016586483f, -0.5620301f, 0.19607842f, 0);
    private Color color50 = decodeColor("nimbusBase", 0.027408898f, -0.5878882f, 0.35294116f, 0);
    private Color color51 = decodeColor("nimbusBase", 0.021348298f, -0.56722116f, 0.3098039f, 0);
    private Color color52 = decodeColor("nimbusBase", 0.021348298f, -0.567841f, 0.31764704f, 0);
    private Color color53 = decodeColor("nimbusBlueGrey", -0.01111114f, -0.058170296f, 0.0039215684f, 0);
    private Color color54 = decodeColor("nimbusBlueGrey", -0.013888836f, -0.04195489f, -0.058823526f, 0);
    private Color color55 = decodeColor("nimbusBlueGrey", 0.009259284f, -0.0147816315f, -0.007843137f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public RadioButtonPainter(PaintContext ctx, int state) {
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
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient1(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient2(ellipse));
        g.fill(ellipse);

    }

    private void painticonEnabled(Graphics2D g) {
        ellipse = decodeEllipse3();
        g.setPaint(color7);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient3(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient4(ellipse));
        g.fill(ellipse);

    }

    private void painticonFocused(Graphics2D g) {
        ellipse = decodeEllipse4();
        g.setPaint(color12);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient3(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient4(ellipse));
        g.fill(ellipse);

    }

    private void painticonMouseOver(Graphics2D g) {
        ellipse = decodeEllipse3();
        g.setPaint(color7);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient5(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient6(ellipse));
        g.fill(ellipse);

    }

    private void painticonMouseOverAndFocused(Graphics2D g) {
        ellipse = decodeEllipse4();
        g.setPaint(color12);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient5(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient6(ellipse));
        g.fill(ellipse);

    }

    private void painticonPressed(Graphics2D g) {
        ellipse = decodeEllipse3();
        g.setPaint(color19);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient7(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient8(ellipse));
        g.fill(ellipse);

    }

    private void painticonPressedAndFocused(Graphics2D g) {
        ellipse = decodeEllipse4();
        g.setPaint(color12);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient7(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient8(ellipse));
        g.fill(ellipse);

    }

    private void painticonSelected(Graphics2D g) {
        ellipse = decodeEllipse3();
        g.setPaint(color7);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient9(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient10(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse5();
        g.setPaint(decodeGradient11(ellipse));
        g.fill(ellipse);

    }

    private void painticonSelectedAndFocused(Graphics2D g) {
        ellipse = decodeEllipse4();
        g.setPaint(color12);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient9(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient10(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse5();
        g.setPaint(decodeGradient11(ellipse));
        g.fill(ellipse);

    }

    private void painticonPressedAndSelected(Graphics2D g) {
        ellipse = decodeEllipse3();
        g.setPaint(color19);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient12(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient13(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse5();
        g.setPaint(decodeGradient14(ellipse));
        g.fill(ellipse);

    }

    private void painticonPressedAndSelectedAndFocused(Graphics2D g) {
        ellipse = decodeEllipse4();
        g.setPaint(color12);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient12(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient13(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse5();
        g.setPaint(decodeGradient14(ellipse));
        g.fill(ellipse);

    }

    private void painticonMouseOverAndSelected(Graphics2D g) {
        ellipse = decodeEllipse3();
        g.setPaint(color7);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient15(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient16(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse5();
        g.setPaint(decodeGradient11(ellipse));
        g.fill(ellipse);

    }

    private void painticonMouseOverAndSelectedAndFocused(Graphics2D g) {
        ellipse = decodeEllipse4();
        g.setPaint(color12);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient15(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient16(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse5();
        g.setPaint(decodeGradient11(ellipse));
        g.fill(ellipse);

    }

    private void painticonDisabledAndSelected(Graphics2D g) {
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient17(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient18(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse5();
        g.setPaint(decodeGradient19(ellipse));
        g.fill(ellipse);

    }



    private Ellipse2D decodeEllipse1() {
        ellipse.setFrame(decodeX(0.4f), //x
                         decodeY(0.4f), //y
                         decodeX(2.6f) - decodeX(0.4f), //width
                         decodeY(2.6f) - decodeY(0.4f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse2() {
        ellipse.setFrame(decodeX(0.6f), //x
                         decodeY(0.6f), //y
                         decodeX(2.4f) - decodeX(0.6f), //width
                         decodeY(2.4f) - decodeY(0.6f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse3() {
        ellipse.setFrame(decodeX(0.4f), //x
                         decodeY(0.6f), //y
                         decodeX(2.6f) - decodeX(0.4f), //width
                         decodeY(2.8f) - decodeY(0.6f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse4() {
        ellipse.setFrame(decodeX(0.120000005f), //x
                         decodeY(0.120000005f), //y
                         decodeX(2.8799999f) - decodeX(0.120000005f), //width
                         decodeY(2.8799999f) - decodeY(0.120000005f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse5() {
        ellipse.setFrame(decodeX(1.125f), //x
                         decodeY(1.125f), //y
                         decodeX(1.875f) - decodeX(1.125f), //width
                         decodeY(1.875f) - decodeY(1.125f)); //height
        return ellipse;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.49789914f * w) + x, (-0.004201681f * h) + y, (0.5f * w) + x, (0.9978992f * h) + y,
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
        return decodeGradient((0.49754903f * w) + x, (0.004901961f * h) + y, (0.50735295f * w) + x, (1.0f * h) + y,
                new float[] { 0.06344411f,0.21601209f,0.36858007f,0.54833835f,0.72809666f,0.77492446f,0.82175225f,0.91087615f,1.0f },
                new Color[] { color3,
                            decodeColor(color3,color4,0.5f),
                            color4,
                            decodeColor(color4,color4,0.5f),
                            color4,
                            decodeColor(color4,color5,0.5f),
                            color5,
                            decodeColor(color5,color6,0.5f),
                            color6});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.49789914f * w) + x, (-0.004201681f * h) + y, (0.5f * w) + x, (0.9978992f * h) + y,
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
        return decodeGradient((0.49754903f * w) + x, (0.004901961f * h) + y, (0.50735295f * w) + x, (1.0f * h) + y,
                new float[] { 0.06344411f,0.25009555f,0.43674698f,0.48042166f,0.52409637f,0.70481926f,0.88554215f },
                new Color[] { color10,
                            decodeColor(color10,color4,0.5f),
                            color4,
                            decodeColor(color4,color4,0.5f),
                            color4,
                            decodeColor(color4,color11,0.5f),
                            color11});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.49789914f * w) + x, (-0.004201681f * h) + y, (0.5f * w) + x, (0.9978992f * h) + y,
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
        return decodeGradient((0.49754903f * w) + x, (0.004901961f * h) + y, (0.50735295f * w) + x, (1.0f * h) + y,
                new float[] { 0.06344411f,0.21601209f,0.36858007f,0.54833835f,0.72809666f,0.77492446f,0.82175225f,0.91087615f,1.0f },
                new Color[] { color15,
                            decodeColor(color15,color16,0.5f),
                            color16,
                            decodeColor(color16,color16,0.5f),
                            color16,
                            decodeColor(color16,color17,0.5f),
                            color17,
                            decodeColor(color17,color18,0.5f),
                            color18});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.49789914f * w) + x, (-0.004201681f * h) + y, (0.5f * w) + x, (0.9978992f * h) + y,
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
        return decodeGradient((0.49754903f * w) + x, (0.004901961f * h) + y, (0.50735295f * w) + x, (1.0f * h) + y,
                new float[] { 0.06344411f,0.20792687f,0.35240963f,0.45030123f,0.5481928f,0.748494f,0.9487952f },
                new Color[] { color22,
                            decodeColor(color22,color23,0.5f),
                            color23,
                            decodeColor(color23,color23,0.5f),
                            color23,
                            decodeColor(color23,color24,0.5f),
                            color24});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.49789914f * w) + x, (-0.004201681f * h) + y, (0.5f * w) + x, (0.9978992f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color25,
                            decodeColor(color25,color26,0.5f),
                            color26});
    }

    private Paint decodeGradient10(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.49754903f * w) + x, (0.004901961f * h) + y, (0.50735295f * w) + x, (1.0f * h) + y,
                new float[] { 0.0813253f,0.100903615f,0.12048193f,0.28915662f,0.45783132f,0.6159638f,0.77409637f,0.82981926f,0.88554215f },
                new Color[] { color27,
                            decodeColor(color27,color28,0.5f),
                            color28,
                            decodeColor(color28,color29,0.5f),
                            color29,
                            decodeColor(color29,color29,0.5f),
                            color29,
                            decodeColor(color29,color30,0.5f),
                            color30});
    }

    private Paint decodeGradient11(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.50490195f * w) + x, (0.0f * h) + y, (0.49509802f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.23192771f,0.46385542f,0.73192775f,1.0f },
                new Color[] { color31,
                            decodeColor(color31,color32,0.5f),
                            color32,
                            decodeColor(color32,color33,0.5f),
                            color33});
    }

    private Paint decodeGradient12(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.49789914f * w) + x, (-0.004201681f * h) + y, (0.5f * w) + x, (0.9978992f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color34,
                            decodeColor(color34,color26,0.5f),
                            color26});
    }

    private Paint decodeGradient13(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.49754903f * w) + x, (0.004901961f * h) + y, (0.50735295f * w) + x, (1.0f * h) + y,
                new float[] { 0.039156627f,0.07831325f,0.11746988f,0.2876506f,0.45783132f,0.56174695f,0.66566265f,0.7756024f,0.88554215f },
                new Color[] { color36,
                            decodeColor(color36,color37,0.5f),
                            color37,
                            decodeColor(color37,color38,0.5f),
                            color38,
                            decodeColor(color38,color38,0.5f),
                            color38,
                            decodeColor(color38,color39,0.5f),
                            color39});
    }

    private Paint decodeGradient14(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.50490195f * w) + x, (0.0f * h) + y, (0.49509802f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.23192771f,0.46385542f,0.73192775f,1.0f },
                new Color[] { color40,
                            decodeColor(color40,color32,0.5f),
                            color32,
                            decodeColor(color32,color33,0.5f),
                            color33});
    }

    private Paint decodeGradient15(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.49789914f * w) + x, (-0.004201681f * h) + y, (0.5f * w) + x, (0.9978992f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color41,
                            decodeColor(color41,color42,0.5f),
                            color42});
    }

    private Paint decodeGradient16(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.49754903f * w) + x, (0.004901961f * h) + y, (0.50735295f * w) + x, (1.0f * h) + y,
                new float[] { 0.0813253f,0.100903615f,0.12048193f,0.20180723f,0.28313252f,0.49246985f,0.7018072f,0.7560241f,0.810241f,0.84789157f,0.88554215f },
                new Color[] { color43,
                            decodeColor(color43,color44,0.5f),
                            color44,
                            decodeColor(color44,color45,0.5f),
                            color45,
                            decodeColor(color45,color45,0.5f),
                            color45,
                            decodeColor(color45,color46,0.5f),
                            color46,
                            decodeColor(color46,color47,0.5f),
                            color47});
    }

    private Paint decodeGradient17(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.49789914f * w) + x, (-0.004201681f * h) + y, (0.5f * w) + x, (0.9978992f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color48,
                            decodeColor(color48,color49,0.5f),
                            color49});
    }

    private Paint decodeGradient18(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.49754903f * w) + x, (0.004901961f * h) + y, (0.50735295f * w) + x, (1.0f * h) + y,
                new float[] { 0.0813253f,0.2695783f,0.45783132f,0.67168677f,0.88554215f },
                new Color[] { color50,
                            decodeColor(color50,color51,0.5f),
                            color51,
                            decodeColor(color51,color52,0.5f),
                            color52});
    }

    private Paint decodeGradient19(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.50490195f * w) + x, (0.0f * h) + y, (0.49509802f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.23192771f,0.46385542f,0.73192775f,1.0f },
                new Color[] { color53,
                            decodeColor(color53,color54,0.5f),
                            color54,
                            decodeColor(color54,color55,0.5f),
                            color55});
    }


}

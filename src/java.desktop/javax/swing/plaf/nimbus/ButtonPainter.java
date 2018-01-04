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


final class ButtonPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of ButtonPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DEFAULT = 1;
    static final int BACKGROUND_DEFAULT_FOCUSED = 2;
    static final int BACKGROUND_MOUSEOVER_DEFAULT = 3;
    static final int BACKGROUND_MOUSEOVER_DEFAULT_FOCUSED = 4;
    static final int BACKGROUND_PRESSED_DEFAULT = 5;
    static final int BACKGROUND_PRESSED_DEFAULT_FOCUSED = 6;
    static final int BACKGROUND_DISABLED = 7;
    static final int BACKGROUND_ENABLED = 8;
    static final int BACKGROUND_FOCUSED = 9;
    static final int BACKGROUND_MOUSEOVER = 10;
    static final int BACKGROUND_MOUSEOVER_FOCUSED = 11;
    static final int BACKGROUND_PRESSED = 12;
    static final int BACKGROUND_PRESSED_FOCUSED = 13;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of ButtonPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.06885965f, -0.36862746f, -190);
    private Color color2 = decodeColor("nimbusBase", 5.1498413E-4f, -0.34585923f, -0.007843137f, 0);
    private Color color3 = decodeColor("nimbusBase", 5.1498413E-4f, -0.095173776f, -0.25882354f, 0);
    private Color color4 = decodeColor("nimbusBase", 0.004681647f, -0.6197143f, 0.43137252f, 0);
    private Color color5 = decodeColor("nimbusBase", 0.004681647f, -0.5766426f, 0.38039213f, 0);
    private Color color6 = decodeColor("nimbusBase", 5.1498413E-4f, -0.43866998f, 0.24705881f, 0);
    private Color color7 = decodeColor("nimbusBase", 5.1498413E-4f, -0.46404046f, 0.36470586f, 0);
    private Color color8 = decodeColor("nimbusBase", 5.1498413E-4f, -0.47761154f, 0.44313723f, 0);
    private Color color9 = decodeColor("nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
    private Color color10 = decodeColor("nimbusBase", 0.0013483167f, -0.1769987f, -0.12156865f, 0);
    private Color color11 = decodeColor("nimbusBase", 0.059279382f, 0.3642857f, -0.43529415f, 0);
    private Color color12 = decodeColor("nimbusBase", 0.004681647f, -0.6198413f, 0.43921566f, 0);
    private Color color13 = decodeColor("nimbusBase", -0.0017285943f, -0.5822163f, 0.40392154f, 0);
    private Color color14 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4555341f, 0.3215686f, 0);
    private Color color15 = decodeColor("nimbusBase", 5.1498413E-4f, -0.47698414f, 0.43921566f, 0);
    private Color color16 = decodeColor("nimbusBase", -0.06415892f, -0.5455182f, 0.45098037f, 0);
    private Color color17 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -95);
    private Color color18 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.54901963f, 0);
    private Color color19 = decodeColor("nimbusBase", -3.528595E-5f, 0.018606722f, -0.23137257f, 0);
    private Color color20 = decodeColor("nimbusBase", -4.2033195E-4f, -0.38050595f, 0.20392156f, 0);
    private Color color21 = decodeColor("nimbusBase", 0.001903832f, -0.29863563f, 0.1490196f, 0);
    private Color color22 = decodeColor("nimbusBase", 0.0f, 0.0f, 0.0f, 0);
    private Color color23 = decodeColor("nimbusBase", 0.0018727183f, -0.14126986f, 0.15686274f, 0);
    private Color color24 = decodeColor("nimbusBase", 8.9377165E-4f, -0.20852983f, 0.2588235f, 0);
    private Color color25 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.06885965f, -0.36862746f, -232);
    private Color color26 = decodeColor("nimbusBlueGrey", 0.0f, -0.06766917f, 0.07843137f, 0);
    private Color color27 = decodeColor("nimbusBlueGrey", 0.0f, -0.06484103f, 0.027450979f, 0);
    private Color color28 = decodeColor("nimbusBlueGrey", 0.0f, -0.08477524f, 0.16862744f, 0);
    private Color color29 = decodeColor("nimbusBlueGrey", -0.015872955f, -0.080091536f, 0.15686274f, 0);
    private Color color30 = decodeColor("nimbusBlueGrey", 0.0f, -0.07016757f, 0.12941176f, 0);
    private Color color31 = decodeColor("nimbusBlueGrey", 0.0f, -0.07052632f, 0.1372549f, 0);
    private Color color32 = decodeColor("nimbusBlueGrey", 0.0f, -0.070878744f, 0.14509803f, 0);
    private Color color33 = decodeColor("nimbusBlueGrey", -0.055555522f, -0.05356429f, -0.12549019f, 0);
    private Color color34 = decodeColor("nimbusBlueGrey", 0.0f, -0.0147816315f, -0.3764706f, 0);
    private Color color35 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.10655806f, 0.24313724f, 0);
    private Color color36 = decodeColor("nimbusBlueGrey", 0.0f, -0.09823123f, 0.2117647f, 0);
    private Color color37 = decodeColor("nimbusBlueGrey", 0.0f, -0.0749532f, 0.24705881f, 0);
    private Color color38 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color39 = decodeColor("nimbusBlueGrey", 0.0f, -0.020974077f, -0.21960783f, 0);
    private Color color40 = decodeColor("nimbusBlueGrey", 0.0f, 0.11169591f, -0.53333336f, 0);
    private Color color41 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.10658931f, 0.25098038f, 0);
    private Color color42 = decodeColor("nimbusBlueGrey", 0.0f, -0.098526314f, 0.2352941f, 0);
    private Color color43 = decodeColor("nimbusBlueGrey", 0.0f, -0.07333623f, 0.20392156f, 0);
    private Color color44 = new Color(245, 250, 255, 160);
    private Color color45 = decodeColor("nimbusBlueGrey", 0.055555582f, 0.8894737f, -0.7176471f, 0);
    private Color color46 = decodeColor("nimbusBlueGrey", 0.0f, 5.847961E-4f, -0.32156864f, 0);
    private Color color47 = decodeColor("nimbusBlueGrey", -0.00505054f, -0.05960039f, 0.10196078f, 0);
    private Color color48 = decodeColor("nimbusBlueGrey", -0.008547008f, -0.04772438f, 0.06666666f, 0);
    private Color color49 = decodeColor("nimbusBlueGrey", -0.0027777553f, -0.0018306673f, -0.02352941f, 0);
    private Color color50 = decodeColor("nimbusBlueGrey", -0.0027777553f, -0.0212406f, 0.13333333f, 0);
    private Color color51 = decodeColor("nimbusBlueGrey", 0.0055555105f, -0.030845039f, 0.23921567f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public ButtonPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_DEFAULT: paintBackgroundDefault(g); break;
            case BACKGROUND_DEFAULT_FOCUSED: paintBackgroundDefaultAndFocused(g); break;
            case BACKGROUND_MOUSEOVER_DEFAULT: paintBackgroundMouseOverAndDefault(g); break;
            case BACKGROUND_MOUSEOVER_DEFAULT_FOCUSED: paintBackgroundMouseOverAndDefaultAndFocused(g); break;
            case BACKGROUND_PRESSED_DEFAULT: paintBackgroundPressedAndDefault(g); break;
            case BACKGROUND_PRESSED_DEFAULT_FOCUSED: paintBackgroundPressedAndDefaultAndFocused(g); break;
            case BACKGROUND_DISABLED: paintBackgroundDisabled(g); break;
            case BACKGROUND_ENABLED: paintBackgroundEnabled(g); break;
            case BACKGROUND_FOCUSED: paintBackgroundFocused(g); break;
            case BACKGROUND_MOUSEOVER: paintBackgroundMouseOver(g); break;
            case BACKGROUND_MOUSEOVER_FOCUSED: paintBackgroundMouseOverAndFocused(g); break;
            case BACKGROUND_PRESSED: paintBackgroundPressed(g); break;
            case BACKGROUND_PRESSED_FOCUSED: paintBackgroundPressedAndFocused(g); break;

        }
    }
        
    protected Object[] getExtendedCacheKeys(JComponent c) {
        Object[] extendedCacheKeys = null;
        switch(state) {
            case BACKGROUND_DEFAULT:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color4, -0.6197143f, 0.43137252f, 0),
                     getComponentColor(c, "background", color5, -0.5766426f, 0.38039213f, 0),
                     getComponentColor(c, "background", color6, -0.43866998f, 0.24705881f, 0),
                     getComponentColor(c, "background", color7, -0.46404046f, 0.36470586f, 0),
                     getComponentColor(c, "background", color8, -0.47761154f, 0.44313723f, 0)};
                break;
            case BACKGROUND_DEFAULT_FOCUSED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color4, -0.6197143f, 0.43137252f, 0),
                     getComponentColor(c, "background", color5, -0.5766426f, 0.38039213f, 0),
                     getComponentColor(c, "background", color6, -0.43866998f, 0.24705881f, 0),
                     getComponentColor(c, "background", color7, -0.46404046f, 0.36470586f, 0),
                     getComponentColor(c, "background", color8, -0.47761154f, 0.44313723f, 0)};
                break;
            case BACKGROUND_MOUSEOVER_DEFAULT:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color12, -0.6198413f, 0.43921566f, 0),
                     getComponentColor(c, "background", color13, -0.5822163f, 0.40392154f, 0),
                     getComponentColor(c, "background", color14, -0.4555341f, 0.3215686f, 0),
                     getComponentColor(c, "background", color15, -0.47698414f, 0.43921566f, 0),
                     getComponentColor(c, "background", color16, -0.5455182f, 0.45098037f, 0)};
                break;
            case BACKGROUND_MOUSEOVER_DEFAULT_FOCUSED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color12, -0.6198413f, 0.43921566f, 0),
                     getComponentColor(c, "background", color13, -0.5822163f, 0.40392154f, 0),
                     getComponentColor(c, "background", color14, -0.4555341f, 0.3215686f, 0),
                     getComponentColor(c, "background", color15, -0.47698414f, 0.43921566f, 0),
                     getComponentColor(c, "background", color16, -0.5455182f, 0.45098037f, 0)};
                break;
            case BACKGROUND_PRESSED_DEFAULT:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color20, -0.38050595f, 0.20392156f, 0),
                     getComponentColor(c, "background", color21, -0.29863563f, 0.1490196f, 0),
                     getComponentColor(c, "background", color22, 0.0f, 0.0f, 0),
                     getComponentColor(c, "background", color23, -0.14126986f, 0.15686274f, 0),
                     getComponentColor(c, "background", color24, -0.20852983f, 0.2588235f, 0)};
                break;
            case BACKGROUND_PRESSED_DEFAULT_FOCUSED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color20, -0.38050595f, 0.20392156f, 0),
                     getComponentColor(c, "background", color21, -0.29863563f, 0.1490196f, 0),
                     getComponentColor(c, "background", color22, 0.0f, 0.0f, 0),
                     getComponentColor(c, "background", color23, -0.14126986f, 0.15686274f, 0),
                     getComponentColor(c, "background", color24, -0.20852983f, 0.2588235f, 0)};
                break;
            case BACKGROUND_ENABLED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color35, -0.10655806f, 0.24313724f, 0),
                     getComponentColor(c, "background", color36, -0.09823123f, 0.2117647f, 0),
                     getComponentColor(c, "background", color30, -0.07016757f, 0.12941176f, 0),
                     getComponentColor(c, "background", color37, -0.0749532f, 0.24705881f, 0),
                     getComponentColor(c, "background", color38, -0.110526316f, 0.25490195f, 0)};
                break;
            case BACKGROUND_FOCUSED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color35, -0.10655806f, 0.24313724f, 0),
                     getComponentColor(c, "background", color36, -0.09823123f, 0.2117647f, 0),
                     getComponentColor(c, "background", color30, -0.07016757f, 0.12941176f, 0),
                     getComponentColor(c, "background", color37, -0.0749532f, 0.24705881f, 0),
                     getComponentColor(c, "background", color38, -0.110526316f, 0.25490195f, 0)};
                break;
            case BACKGROUND_MOUSEOVER:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color41, -0.10658931f, 0.25098038f, 0),
                     getComponentColor(c, "background", color42, -0.098526314f, 0.2352941f, 0),
                     getComponentColor(c, "background", color43, -0.07333623f, 0.20392156f, 0),
                     getComponentColor(c, "background", color38, -0.110526316f, 0.25490195f, 0)};
                break;
            case BACKGROUND_MOUSEOVER_FOCUSED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color41, -0.10658931f, 0.25098038f, 0),
                     getComponentColor(c, "background", color42, -0.098526314f, 0.2352941f, 0),
                     getComponentColor(c, "background", color43, -0.07333623f, 0.20392156f, 0),
                     getComponentColor(c, "background", color38, -0.110526316f, 0.25490195f, 0)};
                break;
            case BACKGROUND_PRESSED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color47, -0.05960039f, 0.10196078f, 0),
                     getComponentColor(c, "background", color48, -0.04772438f, 0.06666666f, 0),
                     getComponentColor(c, "background", color49, -0.0018306673f, -0.02352941f, 0),
                     getComponentColor(c, "background", color50, -0.0212406f, 0.13333333f, 0),
                     getComponentColor(c, "background", color51, -0.030845039f, 0.23921567f, 0)};
                break;
            case BACKGROUND_PRESSED_FOCUSED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color47, -0.05960039f, 0.10196078f, 0),
                     getComponentColor(c, "background", color48, -0.04772438f, 0.06666666f, 0),
                     getComponentColor(c, "background", color49, -0.0018306673f, -0.02352941f, 0),
                     getComponentColor(c, "background", color50, -0.0212406f, 0.13333333f, 0),
                     getComponentColor(c, "background", color51, -0.030845039f, 0.23921567f, 0)};
                break;
        }
        return extendedCacheKeys;
    }

    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundDefault(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient1(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundDefaultAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color9);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient1(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundMouseOverAndDefault(Graphics2D g) {
        roundRect = decodeRoundRect5();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient3(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundMouseOverAndDefaultAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color9);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient3(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundPressedAndDefault(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color17);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient4(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundPressedAndDefaultAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color9);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient4(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundDisabled(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color25);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient5(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient6(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundEnabled(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient7(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color9);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient7(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient8(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundMouseOver(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient9(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient10(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundMouseOverAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color9);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient9(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient10(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundPressed(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color44);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient11(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundPressedAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color9);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient11(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);

    }



    private RoundRectangle2D decodeRoundRect1() {
        roundRect.setRoundRect(decodeX(0.2857143f), //x
                               decodeY(0.42857143f), //y
                               decodeX(2.7142859f) - decodeX(0.2857143f), //width
                               decodeY(2.857143f) - decodeY(0.42857143f), //height
                               12.0f, 12.0f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect2() {
        roundRect.setRoundRect(decodeX(0.2857143f), //x
                               decodeY(0.2857143f), //y
                               decodeX(2.7142859f) - decodeX(0.2857143f), //width
                               decodeY(2.7142859f) - decodeY(0.2857143f), //height
                               9.0f, 9.0f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect3() {
        roundRect.setRoundRect(decodeX(0.42857143f), //x
                               decodeY(0.42857143f), //y
                               decodeX(2.5714285f) - decodeX(0.42857143f), //width
                               decodeY(2.5714285f) - decodeY(0.42857143f), //height
                               7.0f, 7.0f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect4() {
        roundRect.setRoundRect(decodeX(0.08571429f), //x
                               decodeY(0.08571429f), //y
                               decodeX(2.914286f) - decodeX(0.08571429f), //width
                               decodeY(2.914286f) - decodeY(0.08571429f), //height
                               11.0f, 11.0f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect5() {
        roundRect.setRoundRect(decodeX(0.2857143f), //x
                               decodeY(0.42857143f), //y
                               decodeX(2.7142859f) - decodeX(0.2857143f), //width
                               decodeY(2.857143f) - decodeY(0.42857143f), //height
                               9.0f, 9.0f); //rounding
        return roundRect;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.05f,0.5f,0.95f },
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
                new float[] { 0.0f,0.024f,0.06f,0.276f,0.6f,0.65f,0.7f,0.856f,0.96f,0.98399997f,1.0f },
                new Color[] { (Color)componentColors[0],
                            decodeColor((Color)componentColors[0],(Color)componentColors[1],0.5f),
                            (Color)componentColors[1],
                            decodeColor((Color)componentColors[1],(Color)componentColors[2],0.5f),
                            (Color)componentColors[2],
                            decodeColor((Color)componentColors[2],(Color)componentColors[2],0.5f),
                            (Color)componentColors[2],
                            decodeColor((Color)componentColors[2],(Color)componentColors[3],0.5f),
                            (Color)componentColors[3],
                            decodeColor((Color)componentColors[3],(Color)componentColors[4],0.5f),
                            (Color)componentColors[4]});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.05f,0.5f,0.95f },
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
                new float[] { 0.05f,0.5f,0.95f },
                new Color[] { color18,
                            decodeColor(color18,color19,0.5f),
                            color19});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.09f,0.52f,0.95f },
                new Color[] { color26,
                            decodeColor(color26,color27,0.5f),
                            color27});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.03f,0.06f,0.33f,0.6f,0.65f,0.7f,0.825f,0.95f,0.975f,1.0f },
                new Color[] { color28,
                            decodeColor(color28,color29,0.5f),
                            color29,
                            decodeColor(color29,color30,0.5f),
                            color30,
                            decodeColor(color30,color30,0.5f),
                            color30,
                            decodeColor(color30,color31,0.5f),
                            color31,
                            decodeColor(color31,color32,0.5f),
                            color32});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.09f,0.52f,0.95f },
                new Color[] { color33,
                            decodeColor(color33,color34,0.5f),
                            color34});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.03f,0.06f,0.33f,0.6f,0.65f,0.7f,0.825f,0.95f,0.975f,1.0f },
                new Color[] { (Color)componentColors[0],
                            decodeColor((Color)componentColors[0],(Color)componentColors[1],0.5f),
                            (Color)componentColors[1],
                            decodeColor((Color)componentColors[1],(Color)componentColors[2],0.5f),
                            (Color)componentColors[2],
                            decodeColor((Color)componentColors[2],(Color)componentColors[2],0.5f),
                            (Color)componentColors[2],
                            decodeColor((Color)componentColors[2],(Color)componentColors[3],0.5f),
                            (Color)componentColors[3],
                            decodeColor((Color)componentColors[3],(Color)componentColors[4],0.5f),
                            (Color)componentColors[4]});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.09f,0.52f,0.95f },
                new Color[] { color39,
                            decodeColor(color39,color40,0.5f),
                            color40});
    }

    private Paint decodeGradient10(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.024f,0.06f,0.276f,0.6f,0.65f,0.7f,0.856f,0.96f,0.98f,1.0f },
                new Color[] { (Color)componentColors[0],
                            decodeColor((Color)componentColors[0],(Color)componentColors[1],0.5f),
                            (Color)componentColors[1],
                            decodeColor((Color)componentColors[1],(Color)componentColors[2],0.5f),
                            (Color)componentColors[2],
                            decodeColor((Color)componentColors[2],(Color)componentColors[2],0.5f),
                            (Color)componentColors[2],
                            decodeColor((Color)componentColors[2],(Color)componentColors[3],0.5f),
                            (Color)componentColors[3],
                            decodeColor((Color)componentColors[3],(Color)componentColors[3],0.5f),
                            (Color)componentColors[3]});
    }

    private Paint decodeGradient11(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.05f,0.5f,0.95f },
                new Color[] { color45,
                            decodeColor(color45,color46,0.5f),
                            color46});
    }


}

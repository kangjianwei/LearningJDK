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


final class InternalFrameTitlePaneMaximizeButtonPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of InternalFrameTitlePaneMaximizeButtonPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED_WINDOWMAXIMIZED = 1;
    static final int BACKGROUND_ENABLED_WINDOWMAXIMIZED = 2;
    static final int BACKGROUND_MOUSEOVER_WINDOWMAXIMIZED = 3;
    static final int BACKGROUND_PRESSED_WINDOWMAXIMIZED = 4;
    static final int BACKGROUND_ENABLED_WINDOWNOTFOCUSED_WINDOWMAXIMIZED = 5;
    static final int BACKGROUND_MOUSEOVER_WINDOWNOTFOCUSED_WINDOWMAXIMIZED = 6;
    static final int BACKGROUND_PRESSED_WINDOWNOTFOCUSED_WINDOWMAXIMIZED = 7;
    static final int BACKGROUND_DISABLED = 8;
    static final int BACKGROUND_ENABLED = 9;
    static final int BACKGROUND_MOUSEOVER = 10;
    static final int BACKGROUND_PRESSED = 11;
    static final int BACKGROUND_ENABLED_WINDOWNOTFOCUSED = 12;
    static final int BACKGROUND_MOUSEOVER_WINDOWNOTFOCUSED = 13;
    static final int BACKGROUND_PRESSED_WINDOWNOTFOCUSED = 14;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of InternalFrameTitlePaneMaximizeButtonPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusGreen", 0.43362403f, -0.6792196f, 0.054901958f, 0);
    private Color color2 = decodeColor("nimbusGreen", 0.44056845f, -0.631913f, -0.039215684f, 0);
    private Color color3 = decodeColor("nimbusGreen", 0.44056845f, -0.67475206f, 0.06666666f, 0);
    private Color color4 = new Color(255, 200, 0, 255);
    private Color color5 = decodeColor("nimbusGreen", 0.4355179f, -0.6581704f, -0.011764705f, 0);
    private Color color6 = decodeColor("nimbusGreen", 0.44484192f, -0.644647f, -0.031372547f, 0);
    private Color color7 = decodeColor("nimbusGreen", 0.44484192f, -0.6480447f, 0.0f, 0);
    private Color color8 = decodeColor("nimbusGreen", 0.4366002f, -0.6368381f, -0.04705882f, 0);
    private Color color9 = decodeColor("nimbusGreen", 0.44484192f, -0.6423572f, -0.05098039f, 0);
    private Color color10 = decodeColor("nimbusBlueGrey", 0.0055555105f, -0.062449392f, 0.07058823f, 0);
    private Color color11 = decodeColor("nimbusBlueGrey", -0.008547008f, -0.04174325f, -0.0039215684f, -13);
    private Color color12 = decodeColor("nimbusBlueGrey", 0.0f, -0.049920253f, 0.031372547f, 0);
    private Color color13 = decodeColor("nimbusBlueGrey", 0.0055555105f, -0.0029994324f, -0.38039216f, -185);
    private Color color14 = decodeColor("nimbusGreen", 0.1627907f, 0.2793296f, -0.6431373f, 0);
    private Color color15 = decodeColor("nimbusGreen", 0.025363803f, 0.2454313f, -0.2392157f, 0);
    private Color color16 = decodeColor("nimbusGreen", 0.02642706f, -0.3456704f, -0.011764705f, 0);
    private Color color17 = decodeColor("nimbusGreen", 0.025363803f, 0.2373128f, -0.23529413f, 0);
    private Color color18 = decodeColor("nimbusGreen", 0.025363803f, 0.0655365f, -0.13333333f, 0);
    private Color color19 = decodeColor("nimbusGreen", -0.0087068975f, -0.009330213f, -0.32156864f, 0);
    private Color color20 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -13);
    private Color color21 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -33);
    private Color color22 = decodeColor("nimbusGreen", 0.1627907f, 0.2793296f, -0.627451f, 0);
    private Color color23 = decodeColor("nimbusGreen", 0.04572721f, 0.2793296f, -0.37254903f, 0);
    private Color color24 = decodeColor("nimbusGreen", 0.009822637f, -0.34243205f, 0.054901958f, 0);
    private Color color25 = decodeColor("nimbusGreen", 0.010559708f, 0.13167858f, -0.11764705f, 0);
    private Color color26 = decodeColor("nimbusGreen", 0.010559708f, 0.12599629f, -0.11372548f, 0);
    private Color color27 = decodeColor("nimbusGreen", 0.010559708f, 9.2053413E-4f, -0.011764705f, 0);
    private Color color28 = decodeColor("nimbusGreen", 0.015249729f, 0.2793296f, -0.22352943f, -49);
    private Color color29 = decodeColor("nimbusGreen", 0.01279068f, 0.2793296f, -0.19215685f, 0);
    private Color color30 = decodeColor("nimbusGreen", 0.013319805f, 0.2793296f, -0.20784315f, 0);
    private Color color31 = decodeColor("nimbusGreen", 0.009604409f, 0.2793296f, -0.16862744f, 0);
    private Color color32 = decodeColor("nimbusGreen", 0.011600211f, 0.2793296f, -0.15294117f, 0);
    private Color color33 = decodeColor("nimbusGreen", 0.011939123f, 0.2793296f, -0.16470587f, 0);
    private Color color34 = decodeColor("nimbusGreen", 0.009506017f, 0.257901f, -0.15294117f, 0);
    private Color color35 = decodeColor("nimbusGreen", -0.17054264f, -0.7206704f, -0.7019608f, 0);
    private Color color36 = decodeColor("nimbusGreen", 0.07804492f, 0.2793296f, -0.47058827f, 0);
    private Color color37 = decodeColor("nimbusGreen", 0.03592503f, -0.23865601f, -0.15686274f, 0);
    private Color color38 = decodeColor("nimbusGreen", 0.035979107f, 0.23766291f, -0.3254902f, 0);
    private Color color39 = decodeColor("nimbusGreen", 0.03690417f, 0.2793296f, -0.33333334f, 0);
    private Color color40 = decodeColor("nimbusGreen", 0.09681849f, 0.2793296f, -0.5137255f, 0);
    private Color color41 = decodeColor("nimbusGreen", 0.06535478f, 0.2793296f, -0.44705883f, 0);
    private Color color42 = decodeColor("nimbusGreen", 0.0675526f, 0.2793296f, -0.454902f, 0);
    private Color color43 = decodeColor("nimbusGreen", 0.060800627f, 0.2793296f, -0.4392157f, 0);
    private Color color44 = decodeColor("nimbusGreen", 0.06419912f, 0.2793296f, -0.42352942f, 0);
    private Color color45 = decodeColor("nimbusGreen", 0.06375685f, 0.2793296f, -0.43137255f, 0);
    private Color color46 = decodeColor("nimbusGreen", 0.048207358f, 0.2793296f, -0.3882353f, 0);
    private Color color47 = decodeColor("nimbusGreen", 0.057156876f, 0.2793296f, -0.42352942f, 0);
    private Color color48 = decodeColor("nimbusGreen", 0.44056845f, -0.62133265f, -0.109803915f, 0);
    private Color color49 = decodeColor("nimbusGreen", 0.44056845f, -0.5843068f, -0.27058825f, 0);
    private Color color50 = decodeColor("nimbusGreen", 0.4294573f, -0.698349f, 0.17647058f, 0);
    private Color color51 = decodeColor("nimbusGreen", 0.45066953f, -0.665394f, 0.07843137f, 0);
    private Color color52 = decodeColor("nimbusGreen", 0.44056845f, -0.65913194f, 0.062745094f, 0);
    private Color color53 = decodeColor("nimbusGreen", 0.44056845f, -0.6609689f, 0.086274505f, 0);
    private Color color54 = decodeColor("nimbusGreen", 0.44056845f, -0.6578432f, 0.04705882f, 0);
    private Color color55 = decodeColor("nimbusGreen", 0.4355179f, -0.6633787f, 0.05098039f, 0);
    private Color color56 = decodeColor("nimbusGreen", 0.4355179f, -0.664548f, 0.06666666f, 0);
    private Color color57 = decodeColor("nimbusBlueGrey", 0.0f, -0.029445238f, -0.30980393f, -13);
    private Color color58 = decodeColor("nimbusBlueGrey", 0.0f, -0.027957506f, -0.31764707f, -33);
    private Color color59 = decodeColor("nimbusGreen", 0.43202144f, -0.64722407f, -0.007843137f, 0);
    private Color color60 = decodeColor("nimbusGreen", 0.44056845f, -0.6339652f, -0.02352941f, 0);
    private Color color61 = new Color(165, 169, 176, 255);
    private Color color62 = decodeColor("nimbusBlueGrey", -0.00505054f, -0.057128258f, 0.062745094f, 0);
    private Color color63 = decodeColor("nimbusBlueGrey", -0.003968239f, -0.035257496f, -0.015686274f, 0);
    private Color color64 = new Color(64, 88, 0, 255);
    private Color color65 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color66 = decodeColor("nimbusBlueGrey", 0.004830897f, -0.00920473f, 0.14509803f, -101);
    private Color color67 = decodeColor("nimbusGreen", 0.009564877f, 0.100521624f, -0.109803915f, 0);
    private Color color68 = new Color(113, 125, 0, 255);
    private Color color69 = decodeColor("nimbusBlueGrey", 0.0025252104f, -0.0067527294f, 0.086274505f, -65);
    private Color color70 = decodeColor("nimbusGreen", 0.03129223f, 0.2793296f, -0.27450982f, 0);
    private Color color71 = new Color(19, 48, 0, 255);
    private Color color72 = decodeColor("nimbusBlueGrey", 0.0f, -0.029445238f, -0.30980393f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public InternalFrameTitlePaneMaximizeButtonPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_DISABLED_WINDOWMAXIMIZED: paintBackgroundDisabledAndWindowMaximized(g); break;
            case BACKGROUND_ENABLED_WINDOWMAXIMIZED: paintBackgroundEnabledAndWindowMaximized(g); break;
            case BACKGROUND_MOUSEOVER_WINDOWMAXIMIZED: paintBackgroundMouseOverAndWindowMaximized(g); break;
            case BACKGROUND_PRESSED_WINDOWMAXIMIZED: paintBackgroundPressedAndWindowMaximized(g); break;
            case BACKGROUND_ENABLED_WINDOWNOTFOCUSED_WINDOWMAXIMIZED: paintBackgroundEnabledAndWindowNotFocusedAndWindowMaximized(g); break;
            case BACKGROUND_MOUSEOVER_WINDOWNOTFOCUSED_WINDOWMAXIMIZED: paintBackgroundMouseOverAndWindowNotFocusedAndWindowMaximized(g); break;
            case BACKGROUND_PRESSED_WINDOWNOTFOCUSED_WINDOWMAXIMIZED: paintBackgroundPressedAndWindowNotFocusedAndWindowMaximized(g); break;
            case BACKGROUND_DISABLED: paintBackgroundDisabled(g); break;
            case BACKGROUND_ENABLED: paintBackgroundEnabled(g); break;
            case BACKGROUND_MOUSEOVER: paintBackgroundMouseOver(g); break;
            case BACKGROUND_PRESSED: paintBackgroundPressed(g); break;
            case BACKGROUND_ENABLED_WINDOWNOTFOCUSED: paintBackgroundEnabledAndWindowNotFocused(g); break;
            case BACKGROUND_MOUSEOVER_WINDOWNOTFOCUSED: paintBackgroundMouseOverAndWindowNotFocused(g); break;
            case BACKGROUND_PRESSED_WINDOWNOTFOCUSED: paintBackgroundPressedAndWindowNotFocused(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundDisabledAndWindowMaximized(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient1(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color5);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color6);
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color6);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color7);
        g.fill(rect);
        rect = decodeRect6();
        g.setPaint(color8);
        g.fill(rect);
        rect = decodeRect7();
        g.setPaint(color9);
        g.fill(rect);
        rect = decodeRect8();
        g.setPaint(color7);
        g.fill(rect);
        path = decodePath1();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(color12);
        g.fill(path);

    }

    private void paintBackgroundEnabledAndWindowMaximized(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color13);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient4(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient5(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color19);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color19);
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color19);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color19);
        g.fill(rect);
        rect = decodeRect9();
        g.setPaint(color19);
        g.fill(rect);
        rect = decodeRect7();
        g.setPaint(color19);
        g.fill(rect);
        rect = decodeRect10();
        g.setPaint(color19);
        g.fill(rect);
        rect = decodeRect8();
        g.setPaint(color19);
        g.fill(rect);
        path = decodePath1();
        g.setPaint(color20);
        g.fill(path);
        path = decodePath2();
        g.setPaint(color21);
        g.fill(path);

    }

    private void paintBackgroundMouseOverAndWindowMaximized(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color13);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient6(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient7(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color28);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color29);
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color30);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color31);
        g.fill(rect);
        rect = decodeRect9();
        g.setPaint(color32);
        g.fill(rect);
        rect = decodeRect7();
        g.setPaint(color33);
        g.fill(rect);
        rect = decodeRect10();
        g.setPaint(color34);
        g.fill(rect);
        rect = decodeRect8();
        g.setPaint(color31);
        g.fill(rect);
        path = decodePath1();
        g.setPaint(color20);
        g.fill(path);
        path = decodePath2();
        g.setPaint(color21);
        g.fill(path);

    }

    private void paintBackgroundPressedAndWindowMaximized(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color13);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient8(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient9(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color40);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color41);
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color42);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color43);
        g.fill(rect);
        rect = decodeRect6();
        g.setPaint(color44);
        g.fill(rect);
        rect = decodeRect7();
        g.setPaint(color45);
        g.fill(rect);
        rect = decodeRect10();
        g.setPaint(color46);
        g.fill(rect);
        rect = decodeRect8();
        g.setPaint(color47);
        g.fill(rect);
        path = decodePath1();
        g.setPaint(color20);
        g.fill(path);
        path = decodePath2();
        g.setPaint(color21);
        g.fill(path);

    }

    private void paintBackgroundEnabledAndWindowNotFocusedAndWindowMaximized(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient10(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient11(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color54);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color55);
        g.fill(rect);
        rect = decodeRect8();
        g.setPaint(color56);
        g.fill(rect);
        path = decodePath1();
        g.setPaint(color57);
        g.fill(path);
        path = decodePath2();
        g.setPaint(color58);
        g.fill(path);

    }

    private void paintBackgroundMouseOverAndWindowNotFocusedAndWindowMaximized(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color13);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient6(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient7(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color28);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color29);
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color30);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color31);
        g.fill(rect);
        rect = decodeRect9();
        g.setPaint(color32);
        g.fill(rect);
        rect = decodeRect7();
        g.setPaint(color33);
        g.fill(rect);
        rect = decodeRect10();
        g.setPaint(color34);
        g.fill(rect);
        rect = decodeRect8();
        g.setPaint(color31);
        g.fill(rect);
        path = decodePath1();
        g.setPaint(color20);
        g.fill(path);
        path = decodePath2();
        g.setPaint(color21);
        g.fill(path);

    }

    private void paintBackgroundPressedAndWindowNotFocusedAndWindowMaximized(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color13);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient8(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient9(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color40);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color41);
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color42);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color43);
        g.fill(rect);
        rect = decodeRect6();
        g.setPaint(color44);
        g.fill(rect);
        rect = decodeRect7();
        g.setPaint(color45);
        g.fill(rect);
        rect = decodeRect10();
        g.setPaint(color46);
        g.fill(rect);
        rect = decodeRect8();
        g.setPaint(color47);
        g.fill(rect);
        path = decodePath1();
        g.setPaint(color20);
        g.fill(path);
        path = decodePath2();
        g.setPaint(color21);
        g.fill(path);

    }

    private void paintBackgroundDisabled(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient1(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient12(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        path = decodePath3();
        g.setPaint(color61);
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient13(path));
        g.fill(path);

    }

    private void paintBackgroundEnabled(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color13);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient4(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient5(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        path = decodePath3();
        g.setPaint(color64);
        g.fill(path);
        path = decodePath4();
        g.setPaint(color65);
        g.fill(path);

    }

    private void paintBackgroundMouseOver(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color66);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient6(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient14(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        path = decodePath3();
        g.setPaint(color68);
        g.fill(path);
        path = decodePath4();
        g.setPaint(color65);
        g.fill(path);

    }

    private void paintBackgroundPressed(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color69);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient8(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient15(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        path = decodePath3();
        g.setPaint(color71);
        g.fill(path);
        path = decodePath4();
        g.setPaint(color65);
        g.fill(path);

    }

    private void paintBackgroundEnabledAndWindowNotFocused(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient10(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient16(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        path = decodePath4();
        g.setPaint(color72);
        g.fill(path);

    }

    private void paintBackgroundMouseOverAndWindowNotFocused(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color66);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient6(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient14(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        path = decodePath3();
        g.setPaint(color68);
        g.fill(path);
        path = decodePath4();
        g.setPaint(color65);
        g.fill(path);

    }

    private void paintBackgroundPressedAndWindowNotFocused(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(color69);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient8(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient15(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color4);
        g.fill(rect);
        path = decodePath3();
        g.setPaint(color71);
        g.fill(path);
        path = decodePath4();
        g.setPaint(color65);
        g.fill(path);

    }



    private RoundRectangle2D decodeRoundRect1() {
        roundRect.setRoundRect(decodeX(1.0f), //x
                               decodeY(1.0f), //y
                               decodeX(2.0f) - decodeX(1.0f), //width
                               decodeY(1.9444444f) - decodeY(1.0f), //height
                               8.6f, 8.6f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect2() {
        roundRect.setRoundRect(decodeX(1.0526316f), //x
                               decodeY(1.0555556f), //y
                               decodeX(1.9473684f) - decodeX(1.0526316f), //width
                               decodeY(1.8888888f) - decodeY(1.0555556f), //height
                               6.75f, 6.75f); //rounding
        return roundRect;
    }

    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(1.0f), //y
                         decodeX(1.0f) - decodeX(1.0f), //width
                         decodeY(1.0f) - decodeY(1.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect2() {
            rect.setRect(decodeX(1.2165072f), //x
                         decodeY(1.2790405f), //y
                         decodeX(1.6746411f) - decodeX(1.2165072f), //width
                         decodeY(1.3876263f) - decodeY(1.2790405f)); //height
        return rect;
    }

    private Rectangle2D decodeRect3() {
            rect.setRect(decodeX(1.2212919f), //x
                         decodeY(1.6047981f), //y
                         decodeX(1.270335f) - decodeX(1.2212919f), //width
                         decodeY(1.3876263f) - decodeY(1.6047981f)); //height
        return rect;
    }

    private Rectangle2D decodeRect4() {
            rect.setRect(decodeX(1.2643541f), //x
                         decodeY(1.5542929f), //y
                         decodeX(1.6315789f) - decodeX(1.2643541f), //width
                         decodeY(1.5997474f) - decodeY(1.5542929f)); //height
        return rect;
    }

    private Rectangle2D decodeRect5() {
            rect.setRect(decodeX(1.6267943f), //x
                         decodeY(1.3888888f), //y
                         decodeX(1.673445f) - decodeX(1.6267943f), //width
                         decodeY(1.6085858f) - decodeY(1.3888888f)); //height
        return rect;
    }

    private Rectangle2D decodeRect6() {
            rect.setRect(decodeX(1.3684211f), //x
                         decodeY(1.6111112f), //y
                         decodeX(1.4210527f) - decodeX(1.3684211f), //width
                         decodeY(1.7777778f) - decodeY(1.6111112f)); //height
        return rect;
    }

    private Rectangle2D decodeRect7() {
            rect.setRect(decodeX(1.4389952f), //x
                         decodeY(1.7209597f), //y
                         decodeX(1.7882775f) - decodeX(1.4389952f), //width
                         decodeY(1.7765152f) - decodeY(1.7209597f)); //height
        return rect;
    }

    private Rectangle2D decodeRect8() {
            rect.setRect(decodeX(1.5645933f), //x
                         decodeY(1.4078283f), //y
                         decodeX(1.7870812f) - decodeX(1.5645933f), //width
                         decodeY(1.5239899f) - decodeY(1.4078283f)); //height
        return rect;
    }

    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(1.2105263f), decodeY(1.2222222f));
        path.lineTo(decodeX(1.6315789f), decodeY(1.2222222f));
        path.lineTo(decodeX(1.6315789f), decodeY(1.5555556f));
        path.lineTo(decodeX(1.2105263f), decodeY(1.5555556f));
        path.lineTo(decodeX(1.2105263f), decodeY(1.3333334f));
        path.lineTo(decodeX(1.2631578f), decodeY(1.3333334f));
        path.lineTo(decodeX(1.2631578f), decodeY(1.5f));
        path.lineTo(decodeX(1.5789473f), decodeY(1.5f));
        path.lineTo(decodeX(1.5789473f), decodeY(1.3333334f));
        path.lineTo(decodeX(1.2105263f), decodeY(1.3333334f));
        path.lineTo(decodeX(1.2105263f), decodeY(1.2222222f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(1.6842105f), decodeY(1.3888888f));
        path.lineTo(decodeX(1.6842105f), decodeY(1.5f));
        path.lineTo(decodeX(1.7368422f), decodeY(1.5f));
        path.lineTo(decodeX(1.7368422f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.4210527f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.4210527f), decodeY(1.6111112f));
        path.lineTo(decodeX(1.3684211f), decodeY(1.6111112f));
        path.lineTo(decodeX(1.3684211f), decodeY(1.7222222f));
        path.lineTo(decodeX(1.7894738f), decodeY(1.7222222f));
        path.lineTo(decodeX(1.7894738f), decodeY(1.3888888f));
        path.lineTo(decodeX(1.6842105f), decodeY(1.3888888f));
        path.closePath();
        return path;
    }

    private RoundRectangle2D decodeRoundRect3() {
        roundRect.setRoundRect(decodeX(1.0f), //x
                               decodeY(1.6111112f), //y
                               decodeX(2.0f) - decodeX(1.0f), //width
                               decodeY(2.0f) - decodeY(1.6111112f), //height
                               6.0f, 6.0f); //rounding
        return roundRect;
    }

    private Rectangle2D decodeRect9() {
            rect.setRect(decodeX(1.3815789f), //x
                         decodeY(1.6111112f), //y
                         decodeX(1.4366028f) - decodeX(1.3815789f), //width
                         decodeY(1.7739899f) - decodeY(1.6111112f)); //height
        return rect;
    }

    private Rectangle2D decodeRect10() {
            rect.setRect(decodeX(1.7918661f), //x
                         decodeY(1.7752526f), //y
                         decodeX(1.8349283f) - decodeX(1.7918661f), //width
                         decodeY(1.4217172f) - decodeY(1.7752526f)); //height
        return rect;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(1.1913875f), decodeY(1.2916666f));
        path.lineTo(decodeX(1.1925838f), decodeY(1.7462121f));
        path.lineTo(decodeX(1.8157895f), decodeY(1.7449496f));
        path.lineTo(decodeX(1.819378f), decodeY(1.2916666f));
        path.lineTo(decodeX(1.722488f), decodeY(1.2916666f));
        path.lineTo(decodeX(1.7320573f), decodeY(1.669192f));
        path.lineTo(decodeX(1.2799044f), decodeY(1.6565657f));
        path.lineTo(decodeX(1.284689f), decodeY(1.3863636f));
        path.lineTo(decodeX(1.7260766f), decodeY(1.385101f));
        path.lineTo(decodeX(1.722488f), decodeY(1.2904041f));
        path.lineTo(decodeX(1.1913875f), decodeY(1.2916666f));
        path.closePath();
        return path;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(1.2105263f), decodeY(1.2222222f));
        path.lineTo(decodeX(1.2105263f), decodeY(1.7222222f));
        path.lineTo(decodeX(1.7894738f), decodeY(1.7222222f));
        path.lineTo(decodeX(1.7894738f), decodeY(1.3333334f));
        path.lineTo(decodeX(1.7368422f), decodeY(1.3333334f));
        path.lineTo(decodeX(1.7368422f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.2631578f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.2631578f), decodeY(1.3333334f));
        path.lineTo(decodeX(1.7894738f), decodeY(1.3333334f));
        path.lineTo(decodeX(1.7894738f), decodeY(1.2222222f));
        path.lineTo(decodeX(1.2105263f), decodeY(1.2222222f));
        path.closePath();
        return path;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
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
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color3,
                            decodeColor(color3,color2,0.5f),
                            color2});
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
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color14,
                            decodeColor(color14,color15,0.5f),
                            color15});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.8252841f,1.0f },
                new Color[] { color16,
                            decodeColor(color16,color15,0.5f),
                            color15,
                            decodeColor(color15,color17,0.5f),
                            color17,
                            decodeColor(color17,color18,0.5f),
                            color18});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color22,
                            decodeColor(color22,color23,0.5f),
                            color23});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.8252841f,1.0f },
                new Color[] { color24,
                            decodeColor(color24,color25,0.5f),
                            color25,
                            decodeColor(color25,color26,0.5f),
                            color26,
                            decodeColor(color26,color27,0.5f),
                            color27});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color35,
                            decodeColor(color35,color36,0.5f),
                            color36});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.8252841f,1.0f },
                new Color[] { color37,
                            decodeColor(color37,color38,0.5f),
                            color38,
                            decodeColor(color38,color39,0.5f),
                            color39,
                            decodeColor(color39,color18,0.5f),
                            color18});
    }

    private Paint decodeGradient10(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color48,
                            decodeColor(color48,color49,0.5f),
                            color49});
    }

    private Paint decodeGradient11(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.8252841f,1.0f },
                new Color[] { color50,
                            decodeColor(color50,color51,0.5f),
                            color51,
                            decodeColor(color51,color52,0.5f),
                            color52,
                            decodeColor(color52,color53,0.5f),
                            color53});
    }

    private Paint decodeGradient12(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.6082097f,0.6766467f,0.83832335f,1.0f },
                new Color[] { color3,
                            decodeColor(color3,color59,0.5f),
                            color59,
                            decodeColor(color59,color60,0.5f),
                            color60,
                            decodeColor(color60,color2,0.5f),
                            color2});
    }

    private Paint decodeGradient13(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.26047903f,0.6302395f,1.0f },
                new Color[] { color62,
                            decodeColor(color62,color63,0.5f),
                            color63});
    }

    private Paint decodeGradient14(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.8252841f,1.0f },
                new Color[] { color24,
                            decodeColor(color24,color67,0.5f),
                            color67,
                            decodeColor(color67,color25,0.5f),
                            color25,
                            decodeColor(color25,color27,0.5f),
                            color27});
    }

    private Paint decodeGradient15(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.66659296f,0.79341316f,0.8967066f,1.0f },
                new Color[] { color37,
                            decodeColor(color37,color38,0.5f),
                            color38,
                            decodeColor(color38,color39,0.5f),
                            color39,
                            decodeColor(color39,color70,0.5f),
                            color70});
    }

    private Paint decodeGradient16(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.6291678f,0.7185629f,0.8592814f,1.0f },
                new Color[] { color50,
                            decodeColor(color50,color52,0.5f),
                            color52,
                            decodeColor(color52,color52,0.5f),
                            color52,
                            decodeColor(color52,color53,0.5f),
                            color53});
    }


}

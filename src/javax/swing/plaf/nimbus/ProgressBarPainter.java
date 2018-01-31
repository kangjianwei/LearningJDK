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


final class ProgressBarPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of ProgressBarPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_ENABLED = 1;
    static final int BACKGROUND_DISABLED = 2;
    static final int FOREGROUND_ENABLED = 3;
    static final int FOREGROUND_ENABLED_FINISHED = 4;
    static final int FOREGROUND_ENABLED_INDETERMINATE = 5;
    static final int FOREGROUND_DISABLED = 6;
    static final int FOREGROUND_DISABLED_FINISHED = 7;
    static final int FOREGROUND_DISABLED_INDETERMINATE = 8;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of ProgressBarPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", 0.0f, -0.04845735f, -0.17647058f, 0);
    private Color color2 = decodeColor("nimbusBlueGrey", 0.0f, -0.061345987f, -0.027450979f, 0);
    private Color color3 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color4 = decodeColor("nimbusBlueGrey", 0.0f, -0.097921275f, 0.18823528f, 0);
    private Color color5 = decodeColor("nimbusBlueGrey", 0.0138888955f, -0.0925083f, 0.12549019f, 0);
    private Color color6 = decodeColor("nimbusBlueGrey", 0.0f, -0.08222443f, 0.086274505f, 0);
    private Color color7 = decodeColor("nimbusBlueGrey", 0.0f, -0.08477524f, 0.16862744f, 0);
    private Color color8 = decodeColor("nimbusBlueGrey", 0.0f, -0.086996906f, 0.25490195f, 0);
    private Color color9 = decodeColor("nimbusBlueGrey", 0.0f, -0.061613273f, -0.02352941f, 0);
    private Color color10 = decodeColor("nimbusBlueGrey", -0.01111114f, -0.061265234f, 0.05098039f, 0);
    private Color color11 = decodeColor("nimbusBlueGrey", 0.0138888955f, -0.09378991f, 0.19215685f, 0);
    private Color color12 = decodeColor("nimbusBlueGrey", 0.0f, -0.08455229f, 0.1607843f, 0);
    private Color color13 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.08362049f, 0.12941176f, 0);
    private Color color14 = decodeColor("nimbusBlueGrey", 0.007936537f, -0.07826825f, 0.10588235f, 0);
    private Color color15 = decodeColor("nimbusBlueGrey", 0.007936537f, -0.07982456f, 0.1490196f, 0);
    private Color color16 = decodeColor("nimbusBlueGrey", 0.007936537f, -0.08099045f, 0.18431371f, 0);
    private Color color17 = decodeColor("nimbusOrange", 0.0f, 0.0f, 0.0f, -156);
    private Color color18 = decodeColor("nimbusOrange", -0.015796512f, 0.02094239f, -0.15294117f, 0);
    private Color color19 = decodeColor("nimbusOrange", -0.004321605f, 0.02094239f, -0.0745098f, 0);
    private Color color20 = decodeColor("nimbusOrange", -0.008021399f, 0.02094239f, -0.10196078f, 0);
    private Color color21 = decodeColor("nimbusOrange", -0.011706904f, -0.1790576f, -0.02352941f, 0);
    private Color color22 = decodeColor("nimbusOrange", -0.048691254f, 0.02094239f, -0.3019608f, 0);
    private Color color23 = decodeColor("nimbusOrange", 0.003940329f, -0.7375322f, 0.17647058f, 0);
    private Color color24 = decodeColor("nimbusOrange", 0.005506739f, -0.46764207f, 0.109803915f, 0);
    private Color color25 = decodeColor("nimbusOrange", 0.0042127445f, -0.18595415f, 0.04705882f, 0);
    private Color color26 = decodeColor("nimbusOrange", 0.0047626942f, 0.02094239f, 0.0039215684f, 0);
    private Color color27 = decodeColor("nimbusOrange", 0.0047626942f, -0.15147138f, 0.1607843f, 0);
    private Color color28 = decodeColor("nimbusOrange", 0.010665476f, -0.27317524f, 0.25098038f, 0);
    private Color color29 = decodeColor("nimbusBlueGrey", -0.54444444f, -0.08748484f, 0.10588235f, 0);
    private Color color30 = decodeColor("nimbusOrange", 0.0047626942f, -0.21715283f, 0.23921567f, 0);
    private Color color31 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -173);
    private Color color32 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -170);
    private Color color33 = decodeColor("nimbusOrange", 0.024554357f, -0.8873145f, 0.10588235f, -156);
    private Color color34 = decodeColor("nimbusOrange", -0.023593787f, -0.7963165f, 0.02352941f, 0);
    private Color color35 = decodeColor("nimbusOrange", -0.010608241f, -0.7760873f, 0.043137252f, 0);
    private Color color36 = decodeColor("nimbusOrange", -0.015402906f, -0.7840576f, 0.035294116f, 0);
    private Color color37 = decodeColor("nimbusOrange", -0.017112307f, -0.8091547f, 0.058823526f, 0);
    private Color color38 = decodeColor("nimbusOrange", -0.07044564f, -0.844649f, -0.019607842f, 0);
    private Color color39 = decodeColor("nimbusOrange", -0.009704903f, -0.9381485f, 0.11372548f, 0);
    private Color color40 = decodeColor("nimbusOrange", -4.4563413E-4f, -0.86742973f, 0.09411764f, 0);
    private Color color41 = decodeColor("nimbusOrange", -4.4563413E-4f, -0.79896283f, 0.07843137f, 0);
    private Color color42 = decodeColor("nimbusOrange", 0.0013274103f, -0.7530961f, 0.06666666f, 0);
    private Color color43 = decodeColor("nimbusOrange", 0.0013274103f, -0.7644457f, 0.109803915f, 0);
    private Color color44 = decodeColor("nimbusOrange", 0.009244293f, -0.78794646f, 0.13333333f, 0);
    private Color color45 = decodeColor("nimbusBlueGrey", -0.015872955f, -0.0803539f, 0.16470587f, 0);
    private Color color46 = decodeColor("nimbusBlueGrey", 0.007936537f, -0.07968931f, 0.14509803f, 0);
    private Color color47 = decodeColor("nimbusBlueGrey", 0.02222228f, -0.08779904f, 0.11764705f, 0);
    private Color color48 = decodeColor("nimbusBlueGrey", 0.0138888955f, -0.075128086f, 0.14117646f, 0);
    private Color color49 = decodeColor("nimbusBlueGrey", 0.0138888955f, -0.07604356f, 0.16470587f, 0);
    private Color color50 = decodeColor("nimbusOrange", 0.0014062226f, -0.77816474f, 0.12941176f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public ProgressBarPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_DISABLED: paintBackgroundDisabled(g); break;
            case FOREGROUND_ENABLED: paintForegroundEnabled(g); break;
            case FOREGROUND_ENABLED_FINISHED: paintForegroundEnabledAndFinished(g); break;
            case FOREGROUND_ENABLED_INDETERMINATE: paintForegroundEnabledAndIndeterminate(g); break;
            case FOREGROUND_DISABLED: paintForegroundDisabled(g); break;
            case FOREGROUND_DISABLED_FINISHED: paintForegroundDisabledAndFinished(g); break;
            case FOREGROUND_DISABLED_INDETERMINATE: paintForegroundDisabledAndIndeterminate(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundEnabled(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(decodeGradient1(rect));
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient2(rect));
        g.fill(rect);

    }

    private void paintBackgroundDisabled(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(decodeGradient3(rect));
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient4(rect));
        g.fill(rect);

    }

    private void paintForegroundEnabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color17);
        g.fill(path);
        rect = decodeRect3();
        g.setPaint(decodeGradient5(rect));
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(decodeGradient6(rect));
        g.fill(rect);

    }

    private void paintForegroundEnabledAndFinished(Graphics2D g) {
        path = decodePath2();
        g.setPaint(color17);
        g.fill(path);
        rect = decodeRect5();
        g.setPaint(decodeGradient5(rect));
        g.fill(rect);
        rect = decodeRect6();
        g.setPaint(decodeGradient6(rect));
        g.fill(rect);

    }

    private void paintForegroundEnabledAndIndeterminate(Graphics2D g) {
        rect = decodeRect7();
        g.setPaint(decodeGradient7(rect));
        g.fill(rect);
        path = decodePath3();
        g.setPaint(decodeGradient8(path));
        g.fill(path);
        rect = decodeRect8();
        g.setPaint(color31);
        g.fill(rect);
        rect = decodeRect9();
        g.setPaint(color32);
        g.fill(rect);

    }

    private void paintForegroundDisabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color33);
        g.fill(path);
        rect = decodeRect3();
        g.setPaint(decodeGradient9(rect));
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(decodeGradient10(rect));
        g.fill(rect);

    }

    private void paintForegroundDisabledAndFinished(Graphics2D g) {
        path = decodePath4();
        g.setPaint(color33);
        g.fill(path);
        rect = decodeRect5();
        g.setPaint(decodeGradient9(rect));
        g.fill(rect);
        rect = decodeRect6();
        g.setPaint(decodeGradient10(rect));
        g.fill(rect);

    }

    private void paintForegroundDisabledAndIndeterminate(Graphics2D g) {
        rect = decodeRect7();
        g.setPaint(decodeGradient11(rect));
        g.fill(rect);
        path = decodePath5();
        g.setPaint(decodeGradient12(path));
        g.fill(path);

    }



    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(0.4f), //x
                         decodeY(0.4f), //y
                         decodeX(2.6f) - decodeX(0.4f), //width
                         decodeY(2.6f) - decodeY(0.4f)); //height
        return rect;
    }

    private Rectangle2D decodeRect2() {
            rect.setRect(decodeX(0.6f), //x
                         decodeY(0.6f), //y
                         decodeX(2.4f) - decodeX(0.6f), //width
                         decodeY(2.4f) - decodeY(0.6f)); //height
        return rect;
    }

    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(0.21111111f));
        path.curveTo(decodeAnchorX(1.0f, -2.0f), decodeAnchorY(0.21111111342906952f, 0.0f), decodeAnchorX(0.21111111342906952f, 0.0f), decodeAnchorY(1.0f, -2.0f), decodeX(0.21111111f), decodeY(1.0f));
        path.curveTo(decodeAnchorX(0.21111111342906952f, 0.0f), decodeAnchorY(1.0f, 2.0f), decodeAnchorX(0.21111111342906952f, 0.0f), decodeAnchorY(2.0f, -2.0f), decodeX(0.21111111f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(0.21111111342906952f, 0.0f), decodeAnchorY(2.0f, 2.0f), decodeAnchorX(1.0f, -2.0f), decodeAnchorY(2.8222224712371826f, 0.0f), decodeX(1.0f), decodeY(2.8222225f));
        path.curveTo(decodeAnchorX(1.0f, 2.0f), decodeAnchorY(2.8222224712371826f, 0.0f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(2.8222224712371826f, 0.0f), decodeX(3.0f), decodeY(2.8222225f));
        path.lineTo(decodeX(3.0f), decodeY(2.3333333f));
        path.lineTo(decodeX(0.6666667f), decodeY(2.3333333f));
        path.lineTo(decodeX(0.6666667f), decodeY(0.6666667f));
        path.lineTo(decodeX(3.0f), decodeY(0.6666667f));
        path.lineTo(decodeX(3.0f), decodeY(0.2f));
        path.curveTo(decodeAnchorX(3.0f, 0.0f), decodeAnchorY(0.20000000298023224f, 0.0f), decodeAnchorX(1.0f, 2.0f), decodeAnchorY(0.21111111342906952f, 0.0f), decodeX(1.0f), decodeY(0.21111111f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect3() {
            rect.setRect(decodeX(0.6666667f), //x
                         decodeY(0.6666667f), //y
                         decodeX(3.0f) - decodeX(0.6666667f), //width
                         decodeY(2.3333333f) - decodeY(0.6666667f)); //height
        return rect;
    }

    private Rectangle2D decodeRect4() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(1.0f), //y
                         decodeX(2.6666667f) - decodeX(1.0f), //width
                         decodeY(2.0f) - decodeY(1.0f)); //height
        return rect;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(0.9111111f), decodeY(0.21111111f));
        path.curveTo(decodeAnchorX(0.9111111164093018f, -2.000000000000001f), decodeAnchorY(0.21111111342906952f, 0.0f), decodeAnchorX(0.20000000298023224f, 0.0f), decodeAnchorY(1.0025640726089478f, -1.9999999999999998f), decodeX(0.2f), decodeY(1.0025641f));
        path.lineTo(decodeX(0.2f), decodeY(2.0444443f));
        path.curveTo(decodeAnchorX(0.20000000298023224f, 0.0f), decodeAnchorY(2.0444443225860596f, 2.0f), decodeAnchorX(0.9666666984558105f, -2.0f), decodeAnchorY(2.799999952316284f, 0.0f), decodeX(0.9666667f), decodeY(2.8f));
        path.lineTo(decodeX(2.0f), decodeY(2.788889f));
        path.curveTo(decodeAnchorX(2.0f, 1.9709292441265305f), decodeAnchorY(2.788888931274414f, 0.019857039365145823f), decodeAnchorX(2.777777910232544f, -0.03333333333333499f), decodeAnchorY(2.0555553436279297f, 1.9333333333333869f), decodeX(2.777778f), decodeY(2.0555553f));
        path.lineTo(decodeX(2.788889f), decodeY(1.8051281f));
        path.lineTo(decodeX(2.777778f), decodeY(1.2794871f));
        path.lineTo(decodeX(2.777778f), decodeY(1.0025641f));
        path.curveTo(decodeAnchorX(2.777777910232544f, 0.0042173304174148996f), decodeAnchorY(1.0025640726089478f, -1.9503377583381705f), decodeAnchorX(2.0999996662139893f, 1.9659460194139413f), decodeAnchorY(0.2222222238779068f, 0.017122267221350018f), decodeX(2.0999997f), decodeY(0.22222222f));
        path.lineTo(decodeX(0.9111111f), decodeY(0.21111111f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect5() {
            rect.setRect(decodeX(0.6666667f), //x
                         decodeY(0.6666667f), //y
                         decodeX(2.3333333f) - decodeX(0.6666667f), //width
                         decodeY(2.3333333f) - decodeY(0.6666667f)); //height
        return rect;
    }

    private Rectangle2D decodeRect6() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(1.0f), //y
                         decodeX(2.0f) - decodeX(1.0f), //width
                         decodeY(2.0f) - decodeY(1.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect7() {
            rect.setRect(decodeX(0.0f), //x
                         decodeY(0.0f), //y
                         decodeX(3.0f) - decodeX(0.0f), //width
                         decodeY(3.0f) - decodeY(0.0f)); //height
        return rect;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(1.4285715f));
        path.curveTo(decodeAnchorX(0.0f, 2.678571428571433f), decodeAnchorY(1.4285714626312256f, 8.881784197001252E-16f), decodeAnchorX(1.389880895614624f, -6.214285714285715f), decodeAnchorY(0.3452380895614624f, -0.03571428571428292f), decodeX(1.3898809f), decodeY(0.3452381f));
        path.lineTo(decodeX(1.5535715f), decodeY(0.3452381f));
        path.curveTo(decodeAnchorX(1.5535714626312256f, 8.329670329670357f), decodeAnchorY(0.3452380895614624f, 0.002747252747249629f), decodeAnchorX(2.3333332538604736f, -5.2857142857142705f), decodeAnchorY(1.4285714626312256f, 0.03571428571428559f), decodeX(2.3333333f), decodeY(1.4285715f));
        path.lineTo(decodeX(3.0f), decodeY(1.4285715f));
        path.lineTo(decodeX(3.0f), decodeY(1.5714285f));
        path.lineTo(decodeX(2.3333333f), decodeY(1.5714285f));
        path.curveTo(decodeAnchorX(2.3333332538604736f, -5.321428571428569f), decodeAnchorY(1.5714285373687744f, 0.0357142857142847f), decodeAnchorX(1.5535714626312256f, 8.983516483516496f), decodeAnchorY(2.6666667461395264f, 0.03846153846153122f), decodeX(1.5535715f), decodeY(2.6666667f));
        path.lineTo(decodeX(1.4077381f), decodeY(2.6666667f));
        path.curveTo(decodeAnchorX(1.4077380895614624f, -6.714285714285704f), decodeAnchorY(2.6666667461395264f, 0.0f), decodeAnchorX(0.0f, 2.6071428571428568f), decodeAnchorY(1.5714285373687744f, 0.03571428571428559f), decodeX(0.0f), decodeY(1.5714285f));
        path.lineTo(decodeX(0.0f), decodeY(1.4285715f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect8() {
            rect.setRect(decodeX(1.2916666f), //x
                         decodeY(0.0f), //y
                         decodeX(1.3333334f) - decodeX(1.2916666f), //width
                         decodeY(3.0f) - decodeY(0.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect9() {
            rect.setRect(decodeX(1.7083333f), //x
                         decodeY(0.0f), //y
                         decodeX(1.75f) - decodeX(1.7083333f), //width
                         decodeY(3.0f) - decodeY(0.0f)); //height
        return rect;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(0.9888889f), decodeY(0.2f));
        path.curveTo(decodeAnchorX(0.9888889193534851f, -1.9999999999999993f), decodeAnchorY(0.20000000298023224f, 0.0f), decodeAnchorX(0.20000000298023224f, 0.0f), decodeAnchorY(0.9888889193534851f, -2.000000000000001f), decodeX(0.2f), decodeY(0.9888889f));
        path.curveTo(decodeAnchorX(0.20000000298023224f, 0.0f), decodeAnchorY(0.9888889193534851f, 1.9999999999999991f), decodeAnchorX(0.20000000298023224f, 0.0f), decodeAnchorY(1.9974358081817627f, -2.0000000000000053f), decodeX(0.2f), decodeY(1.9974358f));
        path.curveTo(decodeAnchorX(0.20000000298023224f, 0.0f), decodeAnchorY(1.9974358081817627f, 2.000000000000007f), decodeAnchorX(0.9888889193534851f, -1.9999999999999993f), decodeAnchorY(2.811110734939575f, 0.0f), decodeX(0.9888889f), decodeY(2.8111107f));
        path.curveTo(decodeAnchorX(0.9888889193534851f, 2.000000000000003f), decodeAnchorY(2.811110734939575f, 0.0f), decodeAnchorX(2.5f, 0.0f), decodeAnchorY(2.799999952316284f, 0.0f), decodeX(2.5f), decodeY(2.8f));
        path.lineTo(decodeX(2.7444446f), decodeY(2.488889f));
        path.lineTo(decodeX(2.7555554f), decodeY(1.5794872f));
        path.lineTo(decodeX(2.7666664f), decodeY(1.4358975f));
        path.lineTo(decodeX(2.7666664f), decodeY(0.62222224f));
        path.lineTo(decodeX(2.5999997f), decodeY(0.22222222f));
        path.curveTo(decodeAnchorX(2.5999996662139893f, 0.0f), decodeAnchorY(0.2222222238779068f, 0.0f), decodeAnchorX(0.9888889193534851f, 2.000000000000003f), decodeAnchorY(0.20000000298023224f, 0.0f), decodeX(0.9888889f), decodeY(0.2f));
        path.closePath();
        return path;
    }

    private Path2D decodePath5() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(1.4285715f));
        path.curveTo(decodeAnchorX(0.0f, 2.678571428571433f), decodeAnchorY(1.4285714626312256f, 8.881784197001252E-16f), decodeAnchorX(1.389880895614624f, -6.357142857142872f), decodeAnchorY(0.3452380895614624f, -0.03571428571428337f), decodeX(1.3898809f), decodeY(0.3452381f));
        path.lineTo(decodeX(1.5535715f), decodeY(0.3452381f));
        path.curveTo(decodeAnchorX(1.5535714626312256f, 3.9999999999999964f), decodeAnchorY(0.3452380895614624f, 0.0f), decodeAnchorX(2.3333332538604736f, -5.2857142857142705f), decodeAnchorY(1.4285714626312256f, 0.03571428571428559f), decodeX(2.3333333f), decodeY(1.4285715f));
        path.lineTo(decodeX(3.0f), decodeY(1.4285715f));
        path.lineTo(decodeX(3.0f), decodeY(1.5714285f));
        path.lineTo(decodeX(2.3333333f), decodeY(1.5714285f));
        path.curveTo(decodeAnchorX(2.3333332538604736f, -5.321428571428569f), decodeAnchorY(1.5714285373687744f, 0.0357142857142847f), decodeAnchorX(1.5535714626312256f, 3.999999999999986f), decodeAnchorY(2.6666667461395264f, 0.0f), decodeX(1.5535715f), decodeY(2.6666667f));
        path.lineTo(decodeX(1.4077381f), decodeY(2.6666667f));
        path.curveTo(decodeAnchorX(1.4077380895614624f, -6.571428571428584f), decodeAnchorY(2.6666667461395264f, -0.035714285714286476f), decodeAnchorX(0.0f, 2.6071428571428568f), decodeAnchorY(1.5714285373687744f, 0.03571428571428559f), decodeX(0.0f), decodeY(1.5714285f));
        path.lineTo(decodeX(0.0f), decodeY(1.4285715f));
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
                new float[] { 0.038709678f,0.05967742f,0.08064516f,0.23709677f,0.3935484f,0.41612905f,0.43870968f,0.67419356f,0.90967745f,0.91451615f,0.91935486f },
                new Color[] { color3,
                            decodeColor(color3,color4,0.5f),
                            color4,
                            decodeColor(color4,color5,0.5f),
                            color5,
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
                new float[] { 0.05483871f,0.5032258f,0.9516129f },
                new Color[] { color9,
                            decodeColor(color9,color10,0.5f),
                            color10});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.038709678f,0.05967742f,0.08064516f,0.23709677f,0.3935484f,0.41612905f,0.43870968f,0.67419356f,0.90967745f,0.91612905f,0.92258066f },
                new Color[] { color11,
                            decodeColor(color11,color12,0.5f),
                            color12,
                            decodeColor(color12,color13,0.5f),
                            color13,
                            decodeColor(color13,color14,0.5f),
                            color14,
                            decodeColor(color14,color15,0.5f),
                            color15,
                            decodeColor(color15,color16,0.5f),
                            color16});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.038709678f,0.05483871f,0.07096774f,0.28064516f,0.4903226f,0.6967742f,0.9032258f,0.9241935f,0.9451613f },
                new Color[] { color18,
                            decodeColor(color18,color19,0.5f),
                            color19,
                            decodeColor(color19,color20,0.5f),
                            color20,
                            decodeColor(color20,color21,0.5f),
                            color21,
                            decodeColor(color21,color22,0.5f),
                            color22});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.038709678f,0.061290324f,0.08387097f,0.27258065f,0.46129033f,0.4903226f,0.5193548f,0.71774197f,0.91612905f,0.92419356f,0.93225807f },
                new Color[] { color23,
                            decodeColor(color23,color24,0.5f),
                            color24,
                            decodeColor(color24,color25,0.5f),
                            color25,
                            decodeColor(color25,color26,0.5f),
                            color26,
                            decodeColor(color26,color27,0.5f),
                            color27,
                            decodeColor(color27,color28,0.5f),
                            color28});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.051612902f,0.06612903f,0.08064516f,0.2935484f,0.5064516f,0.6903226f,0.87419355f,0.88870966f,0.9032258f },
                new Color[] { color3,
                            decodeColor(color3,color4,0.5f),
                            color4,
                            decodeColor(color4,color29,0.5f),
                            color29,
                            decodeColor(color29,color7,0.5f),
                            color7,
                            decodeColor(color7,color8,0.5f),
                            color8});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.20645161f,0.41290322f,0.44193548f,0.47096774f,0.7354839f,1.0f },
                new Color[] { color24,
                            decodeColor(color24,color25,0.5f),
                            color25,
                            decodeColor(color25,color26,0.5f),
                            color26,
                            decodeColor(color26,color30,0.5f),
                            color30});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.038709678f,0.05483871f,0.07096774f,0.28064516f,0.4903226f,0.6967742f,0.9032258f,0.9241935f,0.9451613f },
                new Color[] { color34,
                            decodeColor(color34,color35,0.5f),
                            color35,
                            decodeColor(color35,color36,0.5f),
                            color36,
                            decodeColor(color36,color37,0.5f),
                            color37,
                            decodeColor(color37,color38,0.5f),
                            color38});
    }

    private Paint decodeGradient10(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.038709678f,0.061290324f,0.08387097f,0.27258065f,0.46129033f,0.4903226f,0.5193548f,0.71774197f,0.91612905f,0.92419356f,0.93225807f },
                new Color[] { color39,
                            decodeColor(color39,color40,0.5f),
                            color40,
                            decodeColor(color40,color41,0.5f),
                            color41,
                            decodeColor(color41,color42,0.5f),
                            color42,
                            decodeColor(color42,color43,0.5f),
                            color43,
                            decodeColor(color43,color44,0.5f),
                            color44});
    }

    private Paint decodeGradient11(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.051612902f,0.06612903f,0.08064516f,0.2935484f,0.5064516f,0.6903226f,0.87419355f,0.88870966f,0.9032258f },
                new Color[] { color45,
                            decodeColor(color45,color46,0.5f),
                            color46,
                            decodeColor(color46,color47,0.5f),
                            color47,
                            decodeColor(color47,color48,0.5f),
                            color48,
                            decodeColor(color48,color49,0.5f),
                            color49});
    }

    private Paint decodeGradient12(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.20645161f,0.41290322f,0.44193548f,0.47096774f,0.7354839f,1.0f },
                new Color[] { color40,
                            decodeColor(color40,color41,0.5f),
                            color41,
                            decodeColor(color41,color42,0.5f),
                            color42,
                            decodeColor(color42,color50,0.5f),
                            color50});
    }


}

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


final class OptionPanePainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of OptionPanePainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_ENABLED = 1;
    static final int ERRORICON_ENABLED = 2;
    static final int INFORMATIONICON_ENABLED = 3;
    static final int QUESTIONICON_ENABLED = 4;
    static final int WARNINGICON_ENABLED = 5;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of OptionPanePainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusRed", -0.014814814f, 0.18384242f, 0.015686274f, 0);
    private Color color2 = decodeColor("nimbusRed", -0.014814814f, -0.403261f, 0.21960783f, 0);
    private Color color3 = decodeColor("nimbusRed", -0.014814814f, -0.07154381f, 0.11372548f, 0);
    private Color color4 = decodeColor("nimbusRed", -0.014814814f, 0.110274374f, 0.07058823f, 0);
    private Color color5 = decodeColor("nimbusRed", -0.014814814f, -0.05413574f, 0.2588235f, 0);
    private Color color6 = new Color(250, 250, 250, 255);
    private Color color7 = decodeColor("nimbusRed", 0.0f, -0.79881656f, 0.33725488f, -187);
    private Color color8 = new Color(255, 200, 0, 255);
    private Color color9 = decodeColor("nimbusInfoBlue", 0.0f, 0.06231594f, -0.054901958f, 0);
    private Color color10 = decodeColor("nimbusInfoBlue", 3.1620264E-4f, 0.07790506f, -0.19215685f, 0);
    private Color color11 = decodeColor("nimbusInfoBlue", -8.2296133E-4f, -0.44631243f, 0.19215685f, 0);
    private Color color12 = decodeColor("nimbusInfoBlue", 0.0012729168f, -0.0739674f, 0.043137252f, 0);
    private Color color13 = decodeColor("nimbusInfoBlue", 8.354187E-4f, -0.14148629f, 0.19999999f, 0);
    private Color color14 = decodeColor("nimbusInfoBlue", -0.0014793873f, -0.41456455f, 0.16470587f, 0);
    private Color color15 = decodeColor("nimbusInfoBlue", 3.437996E-4f, -0.14726585f, 0.043137252f, 0);
    private Color color16 = decodeColor("nimbusInfoBlue", -4.271865E-4f, -0.0055555105f, 0.0f, 0);
    private Color color17 = decodeColor("nimbusInfoBlue", 0.0f, 0.0f, 0.0f, 0);
    private Color color18 = decodeColor("nimbusInfoBlue", -7.866621E-4f, -0.12728173f, 0.17254901f, 0);
    private Color color19 = new Color(115, 120, 126, 255);
    private Color color20 = new Color(26, 34, 43, 255);
    private Color color21 = new Color(168, 173, 178, 255);
    private Color color22 = new Color(101, 109, 118, 255);
    private Color color23 = new Color(159, 163, 168, 255);
    private Color color24 = new Color(116, 122, 130, 255);
    private Color color25 = new Color(96, 104, 112, 255);
    private Color color26 = new Color(118, 128, 138, 255);
    private Color color27 = new Color(255, 255, 255, 255);
    private Color color28 = decodeColor("nimbusAlertYellow", -4.9102306E-4f, 0.1372549f, -0.15294117f, 0);
    private Color color29 = decodeColor("nimbusAlertYellow", -0.0015973002f, 0.1372549f, -0.3490196f, 0);
    private Color color30 = decodeColor("nimbusAlertYellow", 6.530881E-4f, -0.40784314f, 0.0f, 0);
    private Color color31 = decodeColor("nimbusAlertYellow", -3.9456785E-4f, -0.109803915f, 0.0f, 0);
    private Color color32 = decodeColor("nimbusAlertYellow", 0.0f, 0.0f, 0.0f, 0);
    private Color color33 = decodeColor("nimbusAlertYellow", 0.008085668f, -0.04705882f, 0.0f, 0);
    private Color color34 = decodeColor("nimbusAlertYellow", 0.026515156f, -0.18431371f, 0.0f, 0);
    private Color color35 = new Color(69, 69, 69, 255);
    private Color color36 = new Color(0, 0, 0, 255);
    private Color color37 = new Color(16, 16, 16, 255);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public OptionPanePainter(PaintContext ctx, int state) {
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
            case ERRORICON_ENABLED: painterrorIconEnabled(g); break;
            case INFORMATIONICON_ENABLED: paintinformationIconEnabled(g); break;
            case QUESTIONICON_ENABLED: paintquestionIconEnabled(g); break;
            case WARNINGICON_ENABLED: paintwarningIconEnabled(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void painterrorIconEnabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color1);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient1(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(color6);
        g.fill(path);
        ellipse = decodeEllipse1();
        g.setPaint(color6);
        g.fill(ellipse);
        path = decodePath4();
        g.setPaint(color7);
        g.fill(path);

    }

    private void paintinformationIconEnabled(Graphics2D g) {
        ellipse = decodeEllipse2();
        g.setPaint(color8);
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(color8);
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(color8);
        g.fill(ellipse);
        ellipse = decodeEllipse3();
        g.setPaint(decodeGradient2(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse4();
        g.setPaint(decodeGradient3(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse5();
        g.setPaint(decodeGradient4(ellipse));
        g.fill(ellipse);
        path = decodePath5();
        g.setPaint(color6);
        g.fill(path);
        ellipse = decodeEllipse6();
        g.setPaint(color6);
        g.fill(ellipse);

    }

    private void paintquestionIconEnabled(Graphics2D g) {
        ellipse = decodeEllipse3();
        g.setPaint(decodeGradient5(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse4();
        g.setPaint(decodeGradient6(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse5();
        g.setPaint(decodeGradient7(ellipse));
        g.fill(ellipse);
        path = decodePath6();
        g.setPaint(color27);
        g.fill(path);
        ellipse = decodeEllipse1();
        g.setPaint(color27);
        g.fill(ellipse);

    }

    private void paintwarningIconEnabled(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color8);
        g.fill(rect);
        path = decodePath7();
        g.setPaint(decodeGradient8(path));
        g.fill(path);
        path = decodePath8();
        g.setPaint(decodeGradient9(path));
        g.fill(path);
        path = decodePath9();
        g.setPaint(decodeGradient10(path));
        g.fill(path);
        ellipse = decodeEllipse7();
        g.setPaint(color37);
        g.fill(ellipse);

    }



    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(1.2708334f));
        path.lineTo(decodeX(1.2708334f), decodeY(1.0f));
        path.lineTo(decodeX(1.6875f), decodeY(1.0f));
        path.lineTo(decodeX(1.9583333f), decodeY(1.2708334f));
        path.lineTo(decodeX(1.9583333f), decodeY(1.6875f));
        path.lineTo(decodeX(1.6875f), decodeY(1.9583333f));
        path.lineTo(decodeX(1.2708334f), decodeY(1.9583333f));
        path.lineTo(decodeX(1.0f), decodeY(1.6875f));
        path.lineTo(decodeX(1.0f), decodeY(1.2708334f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(1.0208334f), decodeY(1.2916666f));
        path.lineTo(decodeX(1.2916666f), decodeY(1.0208334f));
        path.lineTo(decodeX(1.6666667f), decodeY(1.0208334f));
        path.lineTo(decodeX(1.9375f), decodeY(1.2916666f));
        path.lineTo(decodeX(1.9375f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.6666667f), decodeY(1.9375f));
        path.lineTo(decodeX(1.2916666f), decodeY(1.9375f));
        path.lineTo(decodeX(1.0208334f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.0208334f), decodeY(1.2916666f));
        path.closePath();
        return path;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(1.4166666f), decodeY(1.2291666f));
        path.curveTo(decodeAnchorX(1.4166666269302368f, 0.0f), decodeAnchorY(1.2291666269302368f, -2.0f), decodeAnchorX(1.4791666269302368f, -2.0f), decodeAnchorY(1.1666666269302368f, 0.0f), decodeX(1.4791666f), decodeY(1.1666666f));
        path.curveTo(decodeAnchorX(1.4791666269302368f, 2.0f), decodeAnchorY(1.1666666269302368f, 0.0f), decodeAnchorX(1.5416667461395264f, 0.0f), decodeAnchorY(1.2291666269302368f, -2.0f), decodeX(1.5416667f), decodeY(1.2291666f));
        path.curveTo(decodeAnchorX(1.5416667461395264f, 0.0f), decodeAnchorY(1.2291666269302368f, 2.0f), decodeAnchorX(1.5f, 0.0f), decodeAnchorY(1.6041667461395264f, 0.0f), decodeX(1.5f), decodeY(1.6041667f));
        path.lineTo(decodeX(1.4583334f), decodeY(1.6041667f));
        path.curveTo(decodeAnchorX(1.4583333730697632f, 0.0f), decodeAnchorY(1.6041667461395264f, 0.0f), decodeAnchorX(1.4166666269302368f, 0.0f), decodeAnchorY(1.2291666269302368f, 2.0f), decodeX(1.4166666f), decodeY(1.2291666f));
        path.closePath();
        return path;
    }

    private Ellipse2D decodeEllipse1() {
        ellipse.setFrame(decodeX(1.4166666f), //x
                         decodeY(1.6666667f), //y
                         decodeX(1.5416667f) - decodeX(1.4166666f), //width
                         decodeY(1.7916667f) - decodeY(1.6666667f)); //height
        return ellipse;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(1.0208334f), decodeY(1.2851562f));
        path.lineTo(decodeX(1.2799479f), decodeY(1.0208334f));
        path.lineTo(decodeX(1.6783855f), decodeY(1.0208334f));
        path.lineTo(decodeX(1.9375f), decodeY(1.28125f));
        path.lineTo(decodeX(1.9375f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.6666667f), decodeY(1.9375f));
        path.lineTo(decodeX(1.2851562f), decodeY(1.936198f));
        path.lineTo(decodeX(1.0221354f), decodeY(1.673177f));
        path.lineTo(decodeX(1.0208334f), decodeY(1.5f));
        path.lineTo(decodeX(1.0416666f), decodeY(1.5f));
        path.lineTo(decodeX(1.0416666f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.2916666f), decodeY(1.9166667f));
        path.lineTo(decodeX(1.6666667f), decodeY(1.9166667f));
        path.lineTo(decodeX(1.9166667f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.9166667f), decodeY(1.2916666f));
        path.lineTo(decodeX(1.6666667f), decodeY(1.0416666f));
        path.lineTo(decodeX(1.2916666f), decodeY(1.0416666f));
        path.lineTo(decodeX(1.0416666f), decodeY(1.2916666f));
        path.lineTo(decodeX(1.0416666f), decodeY(1.5f));
        path.lineTo(decodeX(1.0208334f), decodeY(1.5f));
        path.lineTo(decodeX(1.0208334f), decodeY(1.2851562f));
        path.closePath();
        return path;
    }

    private Ellipse2D decodeEllipse2() {
        ellipse.setFrame(decodeX(1.0f), //x
                         decodeY(1.0f), //y
                         decodeX(1.0f) - decodeX(1.0f), //width
                         decodeY(1.0f) - decodeY(1.0f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse3() {
        ellipse.setFrame(decodeX(1.0f), //x
                         decodeY(1.0f), //y
                         decodeX(1.9583333f) - decodeX(1.0f), //width
                         decodeY(1.9583333f) - decodeY(1.0f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse4() {
        ellipse.setFrame(decodeX(1.0208334f), //x
                         decodeY(1.0208334f), //y
                         decodeX(1.9375f) - decodeX(1.0208334f), //width
                         decodeY(1.9375f) - decodeY(1.0208334f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse5() {
        ellipse.setFrame(decodeX(1.0416666f), //x
                         decodeY(1.0416666f), //y
                         decodeX(1.9166667f) - decodeX(1.0416666f), //width
                         decodeY(1.9166667f) - decodeY(1.0416666f)); //height
        return ellipse;
    }

    private Path2D decodePath5() {
        path.reset();
        path.moveTo(decodeX(1.375f), decodeY(1.375f));
        path.curveTo(decodeAnchorX(1.375f, 2.5f), decodeAnchorY(1.375f, 0.0f), decodeAnchorX(1.5f, -1.1875f), decodeAnchorY(1.375f, 0.0f), decodeX(1.5f), decodeY(1.375f));
        path.curveTo(decodeAnchorX(1.5f, 1.1875f), decodeAnchorY(1.375f, 0.0f), decodeAnchorX(1.5416667461395264f, 0.0f), decodeAnchorY(1.4375f, -2.0f), decodeX(1.5416667f), decodeY(1.4375f));
        path.curveTo(decodeAnchorX(1.5416667461395264f, 0.0f), decodeAnchorY(1.4375f, 2.0f), decodeAnchorX(1.5416667461395264f, 0.0f), decodeAnchorY(1.6875f, 0.0f), decodeX(1.5416667f), decodeY(1.6875f));
        path.curveTo(decodeAnchorX(1.5416667461395264f, 0.0f), decodeAnchorY(1.6875f, 0.0f), decodeAnchorX(1.6028645038604736f, -2.5625f), decodeAnchorY(1.6875f, 0.0625f), decodeX(1.6028645f), decodeY(1.6875f));
        path.curveTo(decodeAnchorX(1.6028645038604736f, 2.5625f), decodeAnchorY(1.6875f, -0.0625f), decodeAnchorX(1.6041667461395264f, 2.5625f), decodeAnchorY(1.7708332538604736f, 0.0f), decodeX(1.6041667f), decodeY(1.7708333f));
        path.curveTo(decodeAnchorX(1.6041667461395264f, -2.5625f), decodeAnchorY(1.7708332538604736f, 0.0f), decodeAnchorX(1.3567708730697632f, 2.5f), decodeAnchorY(1.7708332538604736f, 0.0625f), decodeX(1.3567709f), decodeY(1.7708333f));
        path.curveTo(decodeAnchorX(1.3567708730697632f, -2.5f), decodeAnchorY(1.7708332538604736f, -0.0625f), decodeAnchorX(1.3541666269302368f, -2.4375f), decodeAnchorY(1.6875f, 0.0f), decodeX(1.3541666f), decodeY(1.6875f));
        path.curveTo(decodeAnchorX(1.3541666269302368f, 2.4375f), decodeAnchorY(1.6875f, 0.0f), decodeAnchorX(1.4166666269302368f, 0.0f), decodeAnchorY(1.6875f, 0.0f), decodeX(1.4166666f), decodeY(1.6875f));
        path.lineTo(decodeX(1.4166666f), decodeY(1.4583334f));
        path.curveTo(decodeAnchorX(1.4166666269302368f, 0.0f), decodeAnchorY(1.4583333730697632f, 0.0f), decodeAnchorX(1.375f, 2.75f), decodeAnchorY(1.4583333730697632f, 0.0f), decodeX(1.375f), decodeY(1.4583334f));
        path.curveTo(decodeAnchorX(1.375f, -2.75f), decodeAnchorY(1.4583333730697632f, 0.0f), decodeAnchorX(1.375f, -2.5f), decodeAnchorY(1.375f, 0.0f), decodeX(1.375f), decodeY(1.375f));
        path.closePath();
        return path;
    }

    private Ellipse2D decodeEllipse6() {
        ellipse.setFrame(decodeX(1.4166666f), //x
                         decodeY(1.1666666f), //y
                         decodeX(1.5416667f) - decodeX(1.4166666f), //width
                         decodeY(1.2916666f) - decodeY(1.1666666f)); //height
        return ellipse;
    }

    private Path2D decodePath6() {
        path.reset();
        path.moveTo(decodeX(1.3125f), decodeY(1.3723959f));
        path.curveTo(decodeAnchorX(1.3125f, 1.5f), decodeAnchorY(1.3723958730697632f, 1.375f), decodeAnchorX(1.3997396230697632f, -0.75f), decodeAnchorY(1.3580728769302368f, 1.1875f), decodeX(1.3997396f), decodeY(1.3580729f));
        path.curveTo(decodeAnchorX(1.3997396230697632f, 0.75f), decodeAnchorY(1.3580728769302368f, -1.1875f), decodeAnchorX(1.46875f, -1.8125f), decodeAnchorY(1.2903646230697632f, 0.0f), decodeX(1.46875f), decodeY(1.2903646f));
        path.curveTo(decodeAnchorX(1.46875f, 1.8125f), decodeAnchorY(1.2903646230697632f, 0.0f), decodeAnchorX(1.53515625f, 0.0f), decodeAnchorY(1.3502603769302368f, -1.5625f), decodeX(1.5351562f), decodeY(1.3502604f));
        path.curveTo(decodeAnchorX(1.53515625f, 0.0f), decodeAnchorY(1.3502603769302368f, 1.5625f), decodeAnchorX(1.4700521230697632f, 1.25f), decodeAnchorY(1.4283853769302368f, -1.1875f), decodeX(1.4700521f), decodeY(1.4283854f));
        path.curveTo(decodeAnchorX(1.4700521230697632f, -1.25f), decodeAnchorY(1.4283853769302368f, 1.1875f), decodeAnchorX(1.41796875f, -0.0625f), decodeAnchorY(1.5442707538604736f, -1.5f), decodeX(1.4179688f), decodeY(1.5442708f));
        path.curveTo(decodeAnchorX(1.41796875f, 0.0625f), decodeAnchorY(1.5442707538604736f, 1.5f), decodeAnchorX(1.4765625f, -1.3125f), decodeAnchorY(1.6028645038604736f, 0.0f), decodeX(1.4765625f), decodeY(1.6028645f));
        path.curveTo(decodeAnchorX(1.4765625f, 1.3125f), decodeAnchorY(1.6028645038604736f, 0.0f), decodeAnchorX(1.5403645038604736f, 0.0f), decodeAnchorY(1.546875f, 1.625f), decodeX(1.5403645f), decodeY(1.546875f));
        path.curveTo(decodeAnchorX(1.5403645038604736f, 0.0f), decodeAnchorY(1.546875f, -1.625f), decodeAnchorX(1.61328125f, -1.1875f), decodeAnchorY(1.46484375f, 1.25f), decodeX(1.6132812f), decodeY(1.4648438f));
        path.curveTo(decodeAnchorX(1.61328125f, 1.1875f), decodeAnchorY(1.46484375f, -1.25f), decodeAnchorX(1.6666667461395264f, 0.0625f), decodeAnchorY(1.3463541269302368f, 3.3125f), decodeX(1.6666667f), decodeY(1.3463541f));
        path.curveTo(decodeAnchorX(1.6666667461395264f, -0.0625f), decodeAnchorY(1.3463541269302368f, -3.3125f), decodeAnchorX(1.4830728769302368f, 6.125f), decodeAnchorY(1.16796875f, -0.0625f), decodeX(1.4830729f), decodeY(1.1679688f));
        path.curveTo(decodeAnchorX(1.4830728769302368f, -6.125f), decodeAnchorY(1.16796875f, 0.0625f), decodeAnchorX(1.3046875f, 0.4375f), decodeAnchorY(1.2890625f, -1.25f), decodeX(1.3046875f), decodeY(1.2890625f));
        path.curveTo(decodeAnchorX(1.3046875f, -0.4375f), decodeAnchorY(1.2890625f, 1.25f), decodeAnchorX(1.3125f, -1.5f), decodeAnchorY(1.3723958730697632f, -1.375f), decodeX(1.3125f), decodeY(1.3723959f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(1.0f), //y
                         decodeX(1.0f) - decodeX(1.0f), //width
                         decodeY(1.0f) - decodeY(1.0f)); //height
        return rect;
    }

    private Path2D decodePath7() {
        path.reset();
        path.moveTo(decodeX(1.5f), decodeY(1.0208334f));
        path.curveTo(decodeAnchorX(1.5f, 2.0f), decodeAnchorY(1.0208333730697632f, 0.0f), decodeAnchorX(1.56640625f, 0.0f), decodeAnchorY(1.08203125f, 0.0f), decodeX(1.5664062f), decodeY(1.0820312f));
        path.lineTo(decodeX(1.9427083f), decodeY(1.779948f));
        path.curveTo(decodeAnchorX(1.9427082538604736f, 0.0f), decodeAnchorY(1.7799479961395264f, 0.0f), decodeAnchorX(1.9752604961395264f, 0.0f), decodeAnchorY(1.8802082538604736f, -2.375f), decodeX(1.9752605f), decodeY(1.8802083f));
        path.curveTo(decodeAnchorX(1.9752604961395264f, 0.0f), decodeAnchorY(1.8802082538604736f, 2.375f), decodeAnchorX(1.9166667461395264f, 0.0f), decodeAnchorY(1.9375f, 0.0f), decodeX(1.9166667f), decodeY(1.9375f));
        path.lineTo(decodeX(1.0833334f), decodeY(1.9375f));
        path.curveTo(decodeAnchorX(1.0833333730697632f, 0.0f), decodeAnchorY(1.9375f, 0.0f), decodeAnchorX(1.0247396230697632f, 0.125f), decodeAnchorY(1.8815104961395264f, 2.25f), decodeX(1.0247396f), decodeY(1.8815105f));
        path.curveTo(decodeAnchorX(1.0247396230697632f, -0.125f), decodeAnchorY(1.8815104961395264f, -2.25f), decodeAnchorX(1.0598958730697632f, 0.0f), decodeAnchorY(1.78125f, 0.0f), decodeX(1.0598959f), decodeY(1.78125f));
        path.lineTo(decodeX(1.4375f), decodeY(1.0833334f));
        path.curveTo(decodeAnchorX(1.4375f, 0.0f), decodeAnchorY(1.0833333730697632f, 0.0f), decodeAnchorX(1.5f, -2.0f), decodeAnchorY(1.0208333730697632f, 0.0f), decodeX(1.5f), decodeY(1.0208334f));
        path.closePath();
        return path;
    }

    private Path2D decodePath8() {
        path.reset();
        path.moveTo(decodeX(1.4986979f), decodeY(1.0429688f));
        path.curveTo(decodeAnchorX(1.4986978769302368f, 1.75f), decodeAnchorY(1.04296875f, 0.0f), decodeAnchorX(1.5546875f, 0.0f), decodeAnchorY(1.0950521230697632f, 0.0f), decodeX(1.5546875f), decodeY(1.0950521f));
        path.lineTo(decodeX(1.9322917f), decodeY(1.8007812f));
        path.curveTo(decodeAnchorX(1.9322917461395264f, 0.0f), decodeAnchorY(1.80078125f, 0.0f), decodeAnchorX(1.95703125f, 0.0f), decodeAnchorY(1.875f, -1.4375f), decodeX(1.9570312f), decodeY(1.875f));
        path.curveTo(decodeAnchorX(1.95703125f, 0.0f), decodeAnchorY(1.875f, 1.4375f), decodeAnchorX(1.8841145038604736f, 0.0f), decodeAnchorY(1.9166667461395264f, 0.0f), decodeX(1.8841145f), decodeY(1.9166667f));
        path.lineTo(decodeX(1.1002604f), decodeY(1.9166667f));
        path.curveTo(decodeAnchorX(1.1002603769302368f, 0.0f), decodeAnchorY(1.9166667461395264f, 0.0f), decodeAnchorX(1.0455728769302368f, 0.0625f), decodeAnchorY(1.8723957538604736f, 1.625f), decodeX(1.0455729f), decodeY(1.8723958f));
        path.curveTo(decodeAnchorX(1.0455728769302368f, -0.0625f), decodeAnchorY(1.8723957538604736f, -1.625f), decodeAnchorX(1.0755208730697632f, 0.0f), decodeAnchorY(1.7903645038604736f, 0.0f), decodeX(1.0755209f), decodeY(1.7903645f));
        path.lineTo(decodeX(1.4414062f), decodeY(1.1028646f));
        path.curveTo(decodeAnchorX(1.44140625f, 0.0f), decodeAnchorY(1.1028646230697632f, 0.0f), decodeAnchorX(1.4986978769302368f, -1.75f), decodeAnchorY(1.04296875f, 0.0f), decodeX(1.4986979f), decodeY(1.0429688f));
        path.closePath();
        return path;
    }

    private Path2D decodePath9() {
        path.reset();
        path.moveTo(decodeX(1.5f), decodeY(1.2291666f));
        path.curveTo(decodeAnchorX(1.5f, 2.0f), decodeAnchorY(1.2291666269302368f, 0.0f), decodeAnchorX(1.5625f, 0.0f), decodeAnchorY(1.3125f, -2.0f), decodeX(1.5625f), decodeY(1.3125f));
        path.curveTo(decodeAnchorX(1.5625f, 0.0f), decodeAnchorY(1.3125f, 2.0f), decodeAnchorX(1.5f, 1.3125f), decodeAnchorY(1.6666667461395264f, 0.0f), decodeX(1.5f), decodeY(1.6666667f));
        path.curveTo(decodeAnchorX(1.5f, -1.3125f), decodeAnchorY(1.6666667461395264f, 0.0f), decodeAnchorX(1.4375f, 0.0f), decodeAnchorY(1.3125f, 2.0f), decodeX(1.4375f), decodeY(1.3125f));
        path.curveTo(decodeAnchorX(1.4375f, 0.0f), decodeAnchorY(1.3125f, -2.0f), decodeAnchorX(1.5f, -2.0f), decodeAnchorY(1.2291666269302368f, 0.0f), decodeX(1.5f), decodeY(1.2291666f));
        path.closePath();
        return path;
    }

    private Ellipse2D decodeEllipse7() {
        ellipse.setFrame(decodeX(1.4375f), //x
                         decodeY(1.7291667f), //y
                         decodeX(1.5625f) - decodeX(1.4375f), //width
                         decodeY(1.8541667f) - decodeY(1.7291667f)); //height
        return ellipse;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.17258064f,0.3451613f,0.5145161f,0.683871f,0.9f,1.0f },
                new Color[] { color2,
                            decodeColor(color2,color3,0.5f),
                            color3,
                            decodeColor(color3,color4,0.5f),
                            color4,
                            decodeColor(color4,color5,0.5f),
                            color5});
    }

    private Paint decodeGradient2(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color9,
                            decodeColor(color9,color10,0.5f),
                            color10});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.24143836f,0.48287672f,0.7414384f,1.0f },
                new Color[] { color11,
                            decodeColor(color11,color12,0.5f),
                            color12,
                            decodeColor(color12,color13,0.5f),
                            color13});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.14212328f,0.28424656f,0.39212328f,0.5f,0.60958904f,0.7191781f,0.85958904f,1.0f },
                new Color[] { color14,
                            decodeColor(color14,color15,0.5f),
                            color15,
                            decodeColor(color15,color16,0.5f),
                            color16,
                            decodeColor(color16,color17,0.5f),
                            color17,
                            decodeColor(color17,color18,0.5f),
                            color18});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color19,
                            decodeColor(color19,color20,0.5f),
                            color20});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color21,
                            decodeColor(color21,color22,0.5f),
                            color22});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.15239726f,0.30479452f,0.47945207f,0.6541096f,0.8270548f,1.0f },
                new Color[] { color23,
                            decodeColor(color23,color24,0.5f),
                            color24,
                            decodeColor(color24,color25,0.5f),
                            color25,
                            decodeColor(color25,color26,0.5f),
                            color26});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color28,
                            decodeColor(color28,color29,0.5f),
                            color29});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.1729452f,0.3458904f,0.49315068f,0.64041096f,0.7328767f,0.8253425f,0.9126712f,1.0f },
                new Color[] { color30,
                            decodeColor(color30,color31,0.5f),
                            color31,
                            decodeColor(color31,color32,0.5f),
                            color32,
                            decodeColor(color32,color33,0.5f),
                            color33,
                            decodeColor(color33,color34,0.5f),
                            color34});
    }

    private Paint decodeGradient10(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color35,
                            decodeColor(color35,color36,0.5f),
                            color36});
    }


}

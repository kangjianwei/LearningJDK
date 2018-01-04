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


final class TreePainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of TreePainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED = 1;
    static final int BACKGROUND_ENABLED = 2;
    static final int BACKGROUND_ENABLED_SELECTED = 3;
    static final int LEAFICON_ENABLED = 4;
    static final int CLOSEDICON_ENABLED = 5;
    static final int OPENICON_ENABLED = 6;
    static final int COLLAPSEDICON_ENABLED = 7;
    static final int COLLAPSEDICON_ENABLED_SELECTED = 8;
    static final int EXPANDEDICON_ENABLED = 9;
    static final int EXPANDEDICON_ENABLED_SELECTED = 10;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of TreePainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", 0.007936537f, -0.065654516f, -0.13333333f, 0);
    private Color color2 = new Color(97, 98, 102, 255);
    private Color color3 = decodeColor("nimbusBlueGrey", -0.032679737f, -0.043332636f, 0.24705881f, 0);
    private Color color4 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color5 = decodeColor("nimbusBase", 0.0077680945f, -0.51781034f, 0.3490196f, 0);
    private Color color6 = decodeColor("nimbusBase", 0.013940871f, -0.599277f, 0.41960782f, 0);
    private Color color7 = decodeColor("nimbusBase", 0.004681647f, -0.4198052f, 0.14117646f, 0);
    private Color color8 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, -127);
    private Color color9 = decodeColor("nimbusBlueGrey", 0.0f, 0.0f, -0.21f, -99);
    private Color color10 = decodeColor("nimbusBase", 2.9569864E-4f, -0.45978838f, 0.2980392f, 0);
    private Color color11 = decodeColor("nimbusBase", 0.0015952587f, -0.34848025f, 0.18823528f, 0);
    private Color color12 = decodeColor("nimbusBase", 0.0015952587f, -0.30844158f, 0.09803921f, 0);
    private Color color13 = decodeColor("nimbusBase", 0.0015952587f, -0.27329817f, 0.035294116f, 0);
    private Color color14 = decodeColor("nimbusBase", 0.004681647f, -0.6198413f, 0.43921566f, 0);
    private Color color15 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, -125);
    private Color color16 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, -50);
    private Color color17 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, -100);
    private Color color18 = decodeColor("nimbusBase", 0.0012094378f, -0.23571429f, -0.0784314f, 0);
    private Color color19 = decodeColor("nimbusBase", 2.9569864E-4f, -0.115166366f, -0.2627451f, 0);
    private Color color20 = decodeColor("nimbusBase", 0.0027436614f, -0.335015f, 0.011764705f, 0);
    private Color color21 = decodeColor("nimbusBase", 0.0024294257f, -0.3857143f, 0.031372547f, 0);
    private Color color22 = decodeColor("nimbusBase", 0.0018081069f, -0.3595238f, -0.13725492f, 0);
    private Color color23 = new Color(255, 200, 0, 255);
    private Color color24 = decodeColor("nimbusBase", 0.004681647f, -0.33496243f, -0.027450979f, 0);
    private Color color25 = decodeColor("nimbusBase", 0.0019934773f, -0.361378f, -0.10588238f, 0);
    private Color color26 = decodeColor("nimbusBlueGrey", -0.6111111f, -0.110526316f, -0.34509805f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public TreePainter(PaintContext ctx, int state) {
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
            case LEAFICON_ENABLED: paintleafIconEnabled(g); break;
            case CLOSEDICON_ENABLED: paintclosedIconEnabled(g); break;
            case OPENICON_ENABLED: paintopenIconEnabled(g); break;
            case COLLAPSEDICON_ENABLED: paintcollapsedIconEnabled(g); break;
            case COLLAPSEDICON_ENABLED_SELECTED: paintcollapsedIconEnabledAndSelected(g); break;
            case EXPANDEDICON_ENABLED: paintexpandedIconEnabled(g); break;
            case EXPANDEDICON_ENABLED_SELECTED: paintexpandedIconEnabledAndSelected(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintleafIconEnabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color1);
        g.fill(path);
        rect = decodeRect1();
        g.setPaint(color2);
        g.fill(rect);
        path = decodePath2();
        g.setPaint(decodeGradient1(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient2(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(color7);
        g.fill(path);
        path = decodePath5();
        g.setPaint(color8);
        g.fill(path);

    }

    private void paintclosedIconEnabled(Graphics2D g) {
        path = decodePath6();
        g.setPaint(color9);
        g.fill(path);
        path = decodePath7();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath8();
        g.setPaint(decodeGradient4(path));
        g.fill(path);
        rect = decodeRect2();
        g.setPaint(color15);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color17);
        g.fill(rect);
        path = decodePath9();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath10();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath11();
        g.setPaint(color23);
        g.fill(path);

    }

    private void paintopenIconEnabled(Graphics2D g) {
        path = decodePath6();
        g.setPaint(color9);
        g.fill(path);
        path = decodePath12();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath13();
        g.setPaint(decodeGradient4(path));
        g.fill(path);
        rect = decodeRect2();
        g.setPaint(color15);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color17);
        g.fill(rect);
        path = decodePath14();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath15();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath11();
        g.setPaint(color23);
        g.fill(path);

    }

    private void paintcollapsedIconEnabled(Graphics2D g) {
        path = decodePath16();
        g.setPaint(color26);
        g.fill(path);

    }

    private void paintcollapsedIconEnabledAndSelected(Graphics2D g) {
        path = decodePath16();
        g.setPaint(color4);
        g.fill(path);

    }

    private void paintexpandedIconEnabled(Graphics2D g) {
        path = decodePath17();
        g.setPaint(color26);
        g.fill(path);

    }

    private void paintexpandedIconEnabledAndSelected(Graphics2D g) {
        path = decodePath17();
        g.setPaint(color4);
        g.fill(path);

    }



    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(0.2f), decodeY(0.0f));
        path.lineTo(decodeX(0.2f), decodeY(3.0f));
        path.lineTo(decodeX(0.4f), decodeY(3.0f));
        path.lineTo(decodeX(0.4f), decodeY(0.2f));
        path.lineTo(decodeX(1.9197531f), decodeY(0.2f));
        path.lineTo(decodeX(2.6f), decodeY(0.9f));
        path.lineTo(decodeX(2.6f), decodeY(3.0f));
        path.lineTo(decodeX(2.8f), decodeY(3.0f));
        path.lineTo(decodeX(2.8f), decodeY(0.88888896f));
        path.lineTo(decodeX(1.9537036f), decodeY(0.0f));
        path.lineTo(decodeX(0.2f), decodeY(0.0f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(0.4f), //x
                         decodeY(2.8f), //y
                         decodeX(2.6f) - decodeX(0.4f), //width
                         decodeY(3.0f) - decodeY(2.8f)); //height
        return rect;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(1.8333333f), decodeY(0.2f));
        path.lineTo(decodeX(1.8333333f), decodeY(1.0f));
        path.lineTo(decodeX(2.6f), decodeY(1.0f));
        path.lineTo(decodeX(1.8333333f), decodeY(0.2f));
        path.closePath();
        return path;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(1.8333333f), decodeY(0.2f));
        path.lineTo(decodeX(0.4f), decodeY(0.2f));
        path.lineTo(decodeX(0.4f), decodeY(2.8f));
        path.lineTo(decodeX(2.6f), decodeY(2.8f));
        path.lineTo(decodeX(2.6f), decodeY(1.0f));
        path.lineTo(decodeX(1.8333333f), decodeY(1.0f));
        path.lineTo(decodeX(1.8333333f), decodeY(0.2f));
        path.closePath();
        return path;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(1.8333333f), decodeY(0.2f));
        path.lineTo(decodeX(1.6234567f), decodeY(0.2f));
        path.lineTo(decodeX(1.6296296f), decodeY(1.2037038f));
        path.lineTo(decodeX(2.6f), decodeY(1.2006173f));
        path.lineTo(decodeX(2.6f), decodeY(1.0f));
        path.lineTo(decodeX(1.8333333f), decodeY(1.0f));
        path.lineTo(decodeX(1.8333333f), decodeY(0.2f));
        path.closePath();
        return path;
    }

    private Path2D decodePath5() {
        path.reset();
        path.moveTo(decodeX(1.8333333f), decodeY(0.4f));
        path.lineTo(decodeX(1.8333333f), decodeY(0.2f));
        path.lineTo(decodeX(0.4f), decodeY(0.2f));
        path.lineTo(decodeX(0.4f), decodeY(2.8f));
        path.lineTo(decodeX(2.6f), decodeY(2.8f));
        path.lineTo(decodeX(2.6f), decodeY(1.0f));
        path.lineTo(decodeX(2.4f), decodeY(1.0f));
        path.lineTo(decodeX(2.4f), decodeY(2.6f));
        path.lineTo(decodeX(0.6f), decodeY(2.6f));
        path.lineTo(decodeX(0.6f), decodeY(0.4f));
        path.lineTo(decodeX(1.8333333f), decodeY(0.4f));
        path.closePath();
        return path;
    }

    private Path2D decodePath6() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(2.4f));
        path.lineTo(decodeX(0.0f), decodeY(2.6f));
        path.lineTo(decodeX(0.2f), decodeY(3.0f));
        path.lineTo(decodeX(2.6f), decodeY(3.0f));
        path.lineTo(decodeX(2.8f), decodeY(2.6f));
        path.lineTo(decodeX(2.8f), decodeY(2.4f));
        path.lineTo(decodeX(0.0f), decodeY(2.4f));
        path.closePath();
        return path;
    }

    private Path2D decodePath7() {
        path.reset();
        path.moveTo(decodeX(0.6f), decodeY(2.6f));
        path.lineTo(decodeX(0.6037037f), decodeY(1.8425925f));
        path.lineTo(decodeX(0.8f), decodeY(1.0f));
        path.lineTo(decodeX(2.8f), decodeY(1.0f));
        path.lineTo(decodeX(2.8f), decodeY(1.3333334f));
        path.lineTo(decodeX(2.6f), decodeY(2.6f));
        path.lineTo(decodeX(0.6f), decodeY(2.6f));
        path.closePath();
        return path;
    }

    private Path2D decodePath8() {
        path.reset();
        path.moveTo(decodeX(0.2f), decodeY(2.6f));
        path.lineTo(decodeX(0.4f), decodeY(2.6f));
        path.lineTo(decodeX(0.40833336f), decodeY(1.8645833f));
        path.lineTo(decodeX(0.79583335f), decodeY(0.8f));
        path.lineTo(decodeX(2.4f), decodeY(0.8f));
        path.lineTo(decodeX(2.4f), decodeY(0.6f));
        path.lineTo(decodeX(1.5f), decodeY(0.6f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.4f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.2f));
        path.lineTo(decodeX(0.6f), decodeY(0.2f));
        path.lineTo(decodeX(0.6f), decodeY(0.4f));
        path.lineTo(decodeX(0.4f), decodeY(0.6f));
        path.lineTo(decodeX(0.2f), decodeY(0.6f));
        path.lineTo(decodeX(0.2f), decodeY(2.6f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect2() {
            rect.setRect(decodeX(0.2f), //x
                         decodeY(0.6f), //y
                         decodeX(0.4f) - decodeX(0.2f), //width
                         decodeY(0.8f) - decodeY(0.6f)); //height
        return rect;
    }

    private Rectangle2D decodeRect3() {
            rect.setRect(decodeX(0.6f), //x
                         decodeY(0.2f), //y
                         decodeX(1.3333334f) - decodeX(0.6f), //width
                         decodeY(0.4f) - decodeY(0.2f)); //height
        return rect;
    }

    private Rectangle2D decodeRect4() {
            rect.setRect(decodeX(1.5f), //x
                         decodeY(0.6f), //y
                         decodeX(2.4f) - decodeX(1.5f), //width
                         decodeY(0.8f) - decodeY(0.6f)); //height
        return rect;
    }

    private Path2D decodePath9() {
        path.reset();
        path.moveTo(decodeX(3.0f), decodeY(0.8f));
        path.lineTo(decodeX(3.0f), decodeY(1.0f));
        path.lineTo(decodeX(2.4f), decodeY(1.0f));
        path.lineTo(decodeX(2.4f), decodeY(0.6f));
        path.lineTo(decodeX(1.5f), decodeY(0.6f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.4f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.2f));
        path.lineTo(decodeX(0.5888889f), decodeY(0.20370372f));
        path.lineTo(decodeX(0.5962963f), decodeY(0.34814817f));
        path.lineTo(decodeX(0.34814817f), decodeY(0.6f));
        path.lineTo(decodeX(0.2f), decodeY(0.6f));
        path.lineTo(decodeX(0.2f), decodeY(2.6f));
        path.lineTo(decodeX(2.6f), decodeY(2.6f));
        path.lineTo(decodeX(2.6f), decodeY(1.3333334f));
        path.lineTo(decodeX(2.774074f), decodeY(1.1604939f));
        path.lineTo(decodeX(2.8f), decodeY(1.0f));
        path.lineTo(decodeX(3.0f), decodeY(1.0f));
        path.lineTo(decodeX(2.8925927f), decodeY(1.1882716f));
        path.lineTo(decodeX(2.8f), decodeY(1.3333334f));
        path.lineTo(decodeX(2.8f), decodeY(2.6f));
        path.lineTo(decodeX(2.6f), decodeY(2.8f));
        path.lineTo(decodeX(0.2f), decodeY(2.8f));
        path.lineTo(decodeX(0.0f), decodeY(2.6f));
        path.lineTo(decodeX(0.0f), decodeY(0.65185183f));
        path.lineTo(decodeX(0.63703704f), decodeY(0.0f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.0f));
        path.lineTo(decodeX(1.5925925f), decodeY(0.4f));
        path.lineTo(decodeX(2.4f), decodeY(0.4f));
        path.lineTo(decodeX(2.6f), decodeY(0.6f));
        path.lineTo(decodeX(2.6f), decodeY(0.8f));
        path.lineTo(decodeX(3.0f), decodeY(0.8f));
        path.closePath();
        return path;
    }

    private Path2D decodePath10() {
        path.reset();
        path.moveTo(decodeX(2.4f), decodeY(1.0f));
        path.lineTo(decodeX(2.4f), decodeY(0.8f));
        path.lineTo(decodeX(0.74814814f), decodeY(0.8f));
        path.lineTo(decodeX(0.4037037f), decodeY(1.8425925f));
        path.lineTo(decodeX(0.4f), decodeY(2.6f));
        path.lineTo(decodeX(0.6f), decodeY(2.6f));
        path.lineTo(decodeX(0.5925926f), decodeY(2.225926f));
        path.lineTo(decodeX(0.916f), decodeY(0.996f));
        path.lineTo(decodeX(2.4f), decodeY(1.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath11() {
        path.reset();
        path.moveTo(decodeX(2.2f), decodeY(2.2f));
        path.lineTo(decodeX(2.2f), decodeY(2.2f));
        path.closePath();
        return path;
    }

    private Path2D decodePath12() {
        path.reset();
        path.moveTo(decodeX(0.6f), decodeY(2.6f));
        path.lineTo(decodeX(0.6f), decodeY(2.2f));
        path.lineTo(decodeX(0.8f), decodeY(1.3333334f));
        path.lineTo(decodeX(2.8f), decodeY(1.3333334f));
        path.lineTo(decodeX(2.8f), decodeY(1.6666667f));
        path.lineTo(decodeX(2.6f), decodeY(2.6f));
        path.lineTo(decodeX(0.6f), decodeY(2.6f));
        path.closePath();
        return path;
    }

    private Path2D decodePath13() {
        path.reset();
        path.moveTo(decodeX(0.2f), decodeY(2.6f));
        path.lineTo(decodeX(0.4f), decodeY(2.6f));
        path.lineTo(decodeX(0.4f), decodeY(2.0f));
        path.lineTo(decodeX(0.8f), decodeY(1.1666666f));
        path.lineTo(decodeX(2.4f), decodeY(1.1666666f));
        path.lineTo(decodeX(2.4f), decodeY(0.6f));
        path.lineTo(decodeX(1.5f), decodeY(0.6f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.4f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.2f));
        path.lineTo(decodeX(0.6f), decodeY(0.2f));
        path.lineTo(decodeX(0.6f), decodeY(0.4f));
        path.lineTo(decodeX(0.4f), decodeY(0.6f));
        path.lineTo(decodeX(0.2f), decodeY(0.6f));
        path.lineTo(decodeX(0.2f), decodeY(2.6f));
        path.closePath();
        return path;
    }

    private Path2D decodePath14() {
        path.reset();
        path.moveTo(decodeX(3.0f), decodeY(1.1666666f));
        path.lineTo(decodeX(3.0f), decodeY(1.3333334f));
        path.lineTo(decodeX(2.4f), decodeY(1.3333334f));
        path.lineTo(decodeX(2.4f), decodeY(0.6f));
        path.lineTo(decodeX(1.5f), decodeY(0.6f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.4f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.2f));
        path.lineTo(decodeX(0.5888889f), decodeY(0.20370372f));
        path.lineTo(decodeX(0.5962963f), decodeY(0.34814817f));
        path.lineTo(decodeX(0.34814817f), decodeY(0.6f));
        path.lineTo(decodeX(0.2f), decodeY(0.6f));
        path.lineTo(decodeX(0.2f), decodeY(2.6f));
        path.lineTo(decodeX(2.6f), decodeY(2.6f));
        path.lineTo(decodeX(2.6f), decodeY(2.0f));
        path.lineTo(decodeX(2.6f), decodeY(1.8333333f));
        path.lineTo(decodeX(2.916f), decodeY(1.3533334f));
        path.lineTo(decodeX(2.98f), decodeY(1.3766667f));
        path.lineTo(decodeX(2.8f), decodeY(1.8333333f));
        path.lineTo(decodeX(2.8f), decodeY(2.0f));
        path.lineTo(decodeX(2.8f), decodeY(2.6f));
        path.lineTo(decodeX(2.6f), decodeY(2.8f));
        path.lineTo(decodeX(0.2f), decodeY(2.8f));
        path.lineTo(decodeX(0.0f), decodeY(2.6f));
        path.lineTo(decodeX(0.0f), decodeY(0.65185183f));
        path.lineTo(decodeX(0.63703704f), decodeY(0.0f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.0f));
        path.lineTo(decodeX(1.5925925f), decodeY(0.4f));
        path.lineTo(decodeX(2.4f), decodeY(0.4f));
        path.lineTo(decodeX(2.6f), decodeY(0.6f));
        path.lineTo(decodeX(2.6f), decodeY(1.1666666f));
        path.lineTo(decodeX(3.0f), decodeY(1.1666666f));
        path.closePath();
        return path;
    }

    private Path2D decodePath15() {
        path.reset();
        path.moveTo(decodeX(2.4f), decodeY(1.3333334f));
        path.lineTo(decodeX(2.4f), decodeY(1.1666666f));
        path.lineTo(decodeX(0.74f), decodeY(1.1666666f));
        path.lineTo(decodeX(0.4f), decodeY(2.0f));
        path.lineTo(decodeX(0.4f), decodeY(2.6f));
        path.lineTo(decodeX(0.6f), decodeY(2.6f));
        path.lineTo(decodeX(0.5925926f), decodeY(2.225926f));
        path.lineTo(decodeX(0.8f), decodeY(1.3333334f));
        path.lineTo(decodeX(2.4f), decodeY(1.3333334f));
        path.closePath();
        return path;
    }

    private Path2D decodePath16() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.0f));
        path.lineTo(decodeX(1.2397541f), decodeY(0.70163935f));
        path.lineTo(decodeX(0.0f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(0.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath17() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.0f));
        path.lineTo(decodeX(1.25f), decodeY(0.0f));
        path.lineTo(decodeX(0.70819676f), decodeY(2.9901638f));
        path.lineTo(decodeX(0.0f), decodeY(0.0f));
        path.closePath();
        return path;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.046296295f * w) + x, (0.9675926f * h) + y, (0.4861111f * w) + x, (0.5324074f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color3,
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
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color5,
                            decodeColor(color5,color6,0.5f),
                            color6});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.04191617f,0.10329342f,0.16467066f,0.24550897f,0.3263473f,0.6631737f,1.0f },
                new Color[] { color10,
                            decodeColor(color10,color11,0.5f),
                            color11,
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
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color5,
                            decodeColor(color5,color14,0.5f),
                            color14});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color18,
                            decodeColor(color18,color19,0.5f),
                            color19});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.12724552f,0.25449103f,0.62724555f,1.0f },
                new Color[] { color20,
                            decodeColor(color20,color21,0.5f),
                            color21,
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
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color24,
                            decodeColor(color24,color25,0.5f),
                            color25});
    }


}

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


final class SpinnerPreviousButtonPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of SpinnerPreviousButtonPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED = 1;
    static final int BACKGROUND_ENABLED = 2;
    static final int BACKGROUND_FOCUSED = 3;
    static final int BACKGROUND_MOUSEOVER_FOCUSED = 4;
    static final int BACKGROUND_PRESSED_FOCUSED = 5;
    static final int BACKGROUND_MOUSEOVER = 6;
    static final int BACKGROUND_PRESSED = 7;
    static final int FOREGROUND_DISABLED = 8;
    static final int FOREGROUND_ENABLED = 9;
    static final int FOREGROUND_FOCUSED = 10;
    static final int FOREGROUND_MOUSEOVER_FOCUSED = 11;
    static final int FOREGROUND_PRESSED_FOCUSED = 12;
    static final int FOREGROUND_MOUSEOVER = 13;
    static final int FOREGROUND_PRESSED = 14;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of SpinnerPreviousButtonPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBase", 0.015098333f, -0.5557143f, 0.2352941f, 0);
    private Color color2 = decodeColor("nimbusBase", 0.010237217f, -0.55799407f, 0.20784312f, 0);
    private Color color3 = decodeColor("nimbusBase", 0.018570602f, -0.5821429f, 0.32941175f, 0);
    private Color color4 = decodeColor("nimbusBase", 0.021348298f, -0.56722116f, 0.3098039f, 0);
    private Color color5 = decodeColor("nimbusBase", 0.021348298f, -0.567841f, 0.31764704f, 0);
    private Color color6 = decodeColor("nimbusBlueGrey", 0.0f, -0.0033834577f, -0.30588236f, -148);
    private Color color7 = decodeColor("nimbusBase", 5.1498413E-4f, -0.2583558f, -0.13333336f, 0);
    private Color color8 = decodeColor("nimbusBase", 5.1498413E-4f, -0.095173776f, -0.25882354f, 0);
    private Color color9 = decodeColor("nimbusBase", 0.004681647f, -0.5383692f, 0.33725488f, 0);
    private Color color10 = decodeColor("nimbusBase", -0.0017285943f, -0.44453782f, 0.25098038f, 0);
    private Color color11 = decodeColor("nimbusBase", 5.1498413E-4f, -0.43866998f, 0.24705881f, 0);
    private Color color12 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4625541f, 0.35686272f, 0);
    private Color color13 = decodeColor("nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
    private Color color14 = decodeColor("nimbusBase", 0.0013483167f, 0.088923395f, -0.2784314f, 0);
    private Color color15 = decodeColor("nimbusBase", 0.059279382f, 0.3642857f, -0.43529415f, 0);
    private Color color16 = decodeColor("nimbusBase", 0.0010585189f, -0.541452f, 0.4078431f, 0);
    private Color color17 = decodeColor("nimbusBase", 0.00254488f, -0.4608264f, 0.32549018f, 0);
    private Color color18 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4555341f, 0.3215686f, 0);
    private Color color19 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4757143f, 0.43137252f, 0);
    private Color color20 = decodeColor("nimbusBase", 0.061133325f, 0.3642857f, -0.427451f, 0);
    private Color color21 = decodeColor("nimbusBase", -3.528595E-5f, 0.018606722f, -0.23137257f, 0);
    private Color color22 = decodeColor("nimbusBase", 8.354783E-4f, -0.2578073f, 0.12549019f, 0);
    private Color color23 = decodeColor("nimbusBase", 8.9377165E-4f, -0.01599598f, 0.007843137f, 0);
    private Color color24 = decodeColor("nimbusBase", 0.0f, -0.00895375f, 0.007843137f, 0);
    private Color color25 = decodeColor("nimbusBase", 8.9377165E-4f, -0.13853917f, 0.14509803f, 0);
    private Color color26 = decodeColor("nimbusBlueGrey", -0.6111111f, -0.110526316f, -0.63529414f, -179);
    private Color color27 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -186);
    private Color color28 = decodeColor("nimbusBase", 0.018570602f, -0.56714284f, 0.1372549f, 0);
    private Color color29 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.54901963f, 0);
    private Color color30 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public SpinnerPreviousButtonPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_ENABLED: paintBackgroundEnabled(g); break;
            case BACKGROUND_FOCUSED: paintBackgroundFocused(g); break;
            case BACKGROUND_MOUSEOVER_FOCUSED: paintBackgroundMouseOverAndFocused(g); break;
            case BACKGROUND_PRESSED_FOCUSED: paintBackgroundPressedAndFocused(g); break;
            case BACKGROUND_MOUSEOVER: paintBackgroundMouseOver(g); break;
            case BACKGROUND_PRESSED: paintBackgroundPressed(g); break;
            case FOREGROUND_DISABLED: paintForegroundDisabled(g); break;
            case FOREGROUND_ENABLED: paintForegroundEnabled(g); break;
            case FOREGROUND_FOCUSED: paintForegroundFocused(g); break;
            case FOREGROUND_MOUSEOVER_FOCUSED: paintForegroundMouseOverAndFocused(g); break;
            case FOREGROUND_PRESSED_FOCUSED: paintForegroundPressedAndFocused(g); break;
            case FOREGROUND_MOUSEOVER: paintForegroundMouseOver(g); break;
            case FOREGROUND_PRESSED: paintForegroundPressed(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundDisabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(decodeGradient1(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient2(path));
        g.fill(path);

    }

    private void paintBackgroundEnabled(Graphics2D g) {
        path = decodePath3();
        g.setPaint(color6);
        g.fill(path);
        path = decodePath1();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient4(path));
        g.fill(path);

    }

    private void paintBackgroundFocused(Graphics2D g) {
        path = decodePath4();
        g.setPaint(color13);
        g.fill(path);
        path = decodePath1();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient4(path));
        g.fill(path);

    }

    private void paintBackgroundMouseOverAndFocused(Graphics2D g) {
        path = decodePath5();
        g.setPaint(color13);
        g.fill(path);
        path = decodePath6();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath7();
        g.setPaint(decodeGradient6(path));
        g.fill(path);

    }

    private void paintBackgroundPressedAndFocused(Graphics2D g) {
        path = decodePath4();
        g.setPaint(color13);
        g.fill(path);
        path = decodePath1();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient8(path));
        g.fill(path);

    }

    private void paintBackgroundMouseOver(Graphics2D g) {
        path = decodePath3();
        g.setPaint(color26);
        g.fill(path);
        path = decodePath1();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient6(path));
        g.fill(path);

    }

    private void paintBackgroundPressed(Graphics2D g) {
        path = decodePath8();
        g.setPaint(color27);
        g.fill(path);
        path = decodePath1();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient8(path));
        g.fill(path);

    }

    private void paintForegroundDisabled(Graphics2D g) {
        path = decodePath9();
        g.setPaint(color28);
        g.fill(path);

    }

    private void paintForegroundEnabled(Graphics2D g) {
        path = decodePath9();
        g.setPaint(color29);
        g.fill(path);

    }

    private void paintForegroundFocused(Graphics2D g) {
        path = decodePath9();
        g.setPaint(color29);
        g.fill(path);

    }

    private void paintForegroundMouseOverAndFocused(Graphics2D g) {
        path = decodePath9();
        g.setPaint(color29);
        g.fill(path);

    }

    private void paintForegroundPressedAndFocused(Graphics2D g) {
        path = decodePath9();
        g.setPaint(color30);
        g.fill(path);

    }

    private void paintForegroundMouseOver(Graphics2D g) {
        path = decodePath9();
        g.setPaint(color29);
        g.fill(path);

    }

    private void paintForegroundPressed(Graphics2D g) {
        path = decodePath9();
        g.setPaint(color30);
        g.fill(path);

    }



    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(1.0f));
        path.lineTo(decodeX(0.0f), decodeY(2.6666667f));
        path.lineTo(decodeX(2.142857f), decodeY(2.6666667f));
        path.curveTo(decodeAnchorX(2.142857074737549f, 3.0f), decodeAnchorY(2.6666667461395264f, 0.0f), decodeAnchorX(2.7142858505249023f, 0.0f), decodeAnchorY(2.0f, 2.0f), decodeX(2.7142859f), decodeY(2.0f));
        path.lineTo(decodeX(2.7142859f), decodeY(1.0f));
        path.lineTo(decodeX(0.0f), decodeY(1.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(1.0f));
        path.lineTo(decodeX(1.0f), decodeY(2.5f));
        path.lineTo(decodeX(2.142857f), decodeY(2.5f));
        path.curveTo(decodeAnchorX(2.142857074737549f, 2.0f), decodeAnchorY(2.5f, 0.0f), decodeAnchorX(2.5714285373687744f, 0.0f), decodeAnchorY(2.0f, 1.0f), decodeX(2.5714285f), decodeY(2.0f));
        path.lineTo(decodeX(2.5714285f), decodeY(1.0f));
        path.lineTo(decodeX(1.0f), decodeY(1.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(2.6666667f));
        path.lineTo(decodeX(0.0f), decodeY(2.8333333f));
        path.lineTo(decodeX(2.0324676f), decodeY(2.8333333f));
        path.curveTo(decodeAnchorX(2.0324676036834717f, 2.1136363636363793f), decodeAnchorY(2.8333332538604736f, 0.0f), decodeAnchorX(2.7142858505249023f, 0.0f), decodeAnchorY(2.0f, 3.0f), decodeX(2.7142859f), decodeY(2.0f));
        path.lineTo(decodeX(0.0f), decodeY(2.6666667f));
        path.closePath();
        return path;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(1.0f));
        path.lineTo(decodeX(0.0f), decodeY(2.8999999f));
        path.lineTo(decodeX(2.2f), decodeY(2.8999999f));
        path.curveTo(decodeAnchorX(2.200000047683716f, 2.9999999999999982f), decodeAnchorY(2.8999998569488525f, 0.0f), decodeAnchorX(2.914285659790039f, 0.0f), decodeAnchorY(2.2333333492279053f, 3.0f), decodeX(2.9142857f), decodeY(2.2333333f));
        path.lineTo(decodeX(2.9142857f), decodeY(1.0f));
        path.lineTo(decodeX(0.0f), decodeY(1.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath5() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.0f));
        path.lineTo(decodeX(0.0f), decodeY(2.8999999f));
        path.lineTo(decodeX(2.2f), decodeY(2.8999999f));
        path.curveTo(decodeAnchorX(2.200000047683716f, 2.9999999999999982f), decodeAnchorY(2.8999998569488525f, 0.0f), decodeAnchorX(2.914285659790039f, 0.0f), decodeAnchorY(2.2333333492279053f, 3.0f), decodeX(2.9142857f), decodeY(2.2333333f));
        path.lineTo(decodeX(2.9142857f), decodeY(0.0f));
        path.lineTo(decodeX(0.0f), decodeY(0.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath6() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.0f));
        path.lineTo(decodeX(0.0f), decodeY(2.6666667f));
        path.lineTo(decodeX(2.142857f), decodeY(2.6666667f));
        path.curveTo(decodeAnchorX(2.142857074737549f, 3.0f), decodeAnchorY(2.6666667461395264f, 0.0f), decodeAnchorX(2.7142858505249023f, 0.0f), decodeAnchorY(2.0f, 2.0f), decodeX(2.7142859f), decodeY(2.0f));
        path.lineTo(decodeX(2.7142859f), decodeY(0.0f));
        path.lineTo(decodeX(0.0f), decodeY(0.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath7() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(0.0f));
        path.lineTo(decodeX(1.0f), decodeY(2.5f));
        path.lineTo(decodeX(2.142857f), decodeY(2.5f));
        path.curveTo(decodeAnchorX(2.142857074737549f, 2.0f), decodeAnchorY(2.5f, 0.0f), decodeAnchorX(2.5714285373687744f, 0.0f), decodeAnchorY(2.0f, 1.0f), decodeX(2.5714285f), decodeY(2.0f));
        path.lineTo(decodeX(2.5714285f), decodeY(0.0f));
        path.lineTo(decodeX(1.0f), decodeY(0.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath8() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(2.6666667f));
        path.lineTo(decodeX(0.0f), decodeY(2.8333333f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(2.8333332538604736f, 0.0f), decodeAnchorX(2.0324676036834717f, -2.1136363636363793f), decodeAnchorY(2.8333332538604736f, 0.0f), decodeX(2.0324676f), decodeY(2.8333333f));
        path.curveTo(decodeAnchorX(2.0324676036834717f, 2.1136363636363793f), decodeAnchorY(2.8333332538604736f, 0.0f), decodeAnchorX(2.7142858505249023f, 0.0f), decodeAnchorY(2.0f, 3.0f), decodeX(2.7142859f), decodeY(2.0f));
        path.lineTo(decodeX(0.0f), decodeY(2.6666667f));
        path.closePath();
        return path;
    }

    private Path2D decodePath9() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(1.0f));
        path.lineTo(decodeX(1.5045455f), decodeY(1.9943181f));
        path.lineTo(decodeX(2.0f), decodeY(1.0f));
        path.lineTo(decodeX(1.0f), decodeY(1.0f));
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
                new float[] { 0.0f,0.05748663f,0.11497326f,0.55748665f,1.0f },
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
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
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
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.05748663f,0.11497326f,0.2419786f,0.36898395f,0.684492f,1.0f },
                new Color[] { color9,
                            decodeColor(color9,color10,0.5f),
                            color10,
                            decodeColor(color10,color11,0.5f),
                            color11,
                            decodeColor(color11,color12,0.5f),
                            color12});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color14,
                            decodeColor(color14,color15,0.5f),
                            color15});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.05748663f,0.11497326f,0.2419786f,0.36898395f,0.684492f,1.0f },
                new Color[] { color16,
                            decodeColor(color16,color17,0.5f),
                            color17,
                            decodeColor(color17,color18,0.5f),
                            color18,
                            decodeColor(color18,color19,0.5f),
                            color19});
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
                new float[] { 0.0f,0.05748663f,0.11497326f,0.2419786f,0.36898395f,0.684492f,1.0f },
                new Color[] { color22,
                            decodeColor(color22,color23,0.5f),
                            color23,
                            decodeColor(color23,color24,0.5f),
                            color24,
                            decodeColor(color24,color25,0.5f),
                            color25});
    }


}

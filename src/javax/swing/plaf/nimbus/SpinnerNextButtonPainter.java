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


final class SpinnerNextButtonPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of SpinnerNextButtonPainter to determine which region/state is being painted
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
    //by a particular instance of SpinnerNextButtonPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBase", 0.021348298f, -0.56289876f, 0.2588235f, 0);
    private Color color2 = decodeColor("nimbusBase", 0.010237217f, -0.5607143f, 0.2352941f, 0);
    private Color color3 = decodeColor("nimbusBase", 0.021348298f, -0.59223604f, 0.35294116f, 0);
    private Color color4 = decodeColor("nimbusBase", 0.016586483f, -0.5723659f, 0.31764704f, 0);
    private Color color5 = decodeColor("nimbusBase", 0.021348298f, -0.56182265f, 0.24705881f, 0);
    private Color color6 = decodeColor("nimbusBase", 5.1498413E-4f, -0.34585923f, -0.007843137f, 0);
    private Color color7 = decodeColor("nimbusBase", 5.1498413E-4f, -0.27207792f, -0.11764708f, 0);
    private Color color8 = decodeColor("nimbusBase", 0.004681647f, -0.6197143f, 0.43137252f, 0);
    private Color color9 = decodeColor("nimbusBase", -0.0012707114f, -0.5078604f, 0.3098039f, 0);
    private Color color10 = decodeColor("nimbusBase", -0.0028941035f, -0.4800539f, 0.28235292f, 0);
    private Color color11 = decodeColor("nimbusBase", 0.0023007393f, -0.3622768f, -0.04705882f, 0);
    private Color color12 = decodeColor("nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
    private Color color13 = decodeColor("nimbusBase", 0.0013483167f, -0.1769987f, -0.12156865f, 0);
    private Color color14 = decodeColor("nimbusBase", 0.0013483167f, 0.039961398f, -0.25882354f, 0);
    private Color color15 = decodeColor("nimbusBase", 0.004681647f, -0.6198413f, 0.43921566f, 0);
    private Color color16 = decodeColor("nimbusBase", -0.0012707114f, -0.51502466f, 0.3607843f, 0);
    private Color color17 = decodeColor("nimbusBase", 0.0021564364f, -0.49097747f, 0.34509802f, 0);
    private Color color18 = decodeColor("nimbusBase", 5.2034855E-5f, -0.38743842f, 0.019607842f, 0);
    private Color color19 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.54901963f, 0);
    private Color color20 = decodeColor("nimbusBase", 0.08801502f, 0.3642857f, -0.454902f, 0);
    private Color color21 = decodeColor("nimbusBase", -4.2033195E-4f, -0.38050595f, 0.20392156f, 0);
    private Color color22 = decodeColor("nimbusBase", 2.9569864E-4f, -0.15470162f, 0.07058823f, 0);
    private Color color23 = decodeColor("nimbusBase", -4.6235323E-4f, -0.09571427f, 0.039215684f, 0);
    private Color color24 = decodeColor("nimbusBase", 0.018363237f, 0.18135887f, -0.227451f, 0);
    private Color color25 = new Color(255, 200, 0, 255);
    private Color color26 = decodeColor("nimbusBase", 0.021348298f, -0.58106947f, 0.16862744f, 0);
    private Color color27 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.043137252f, 0);
    private Color color28 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.24313727f, 0);
    private Color color29 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public SpinnerNextButtonPainter(PaintContext ctx, int state) {
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
        rect = decodeRect1();
        g.setPaint(color5);
        g.fill(rect);

    }

    private void paintBackgroundEnabled(Graphics2D g) {
        path = decodePath3();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient4(path));
        g.fill(path);
        rect = decodeRect1();
        g.setPaint(color11);
        g.fill(rect);

    }

    private void paintBackgroundFocused(Graphics2D g) {
        path = decodePath5();
        g.setPaint(color12);
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        rect = decodeRect1();
        g.setPaint(color11);
        g.fill(rect);

    }

    private void paintBackgroundMouseOverAndFocused(Graphics2D g) {
        path = decodePath5();
        g.setPaint(color12);
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        rect = decodeRect1();
        g.setPaint(color18);
        g.fill(rect);

    }

    private void paintBackgroundPressedAndFocused(Graphics2D g) {
        path = decodePath5();
        g.setPaint(color12);
        g.fill(path);
        path = decodePath6();
        g.setPaint(decodeGradient8(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient9(path));
        g.fill(path);
        rect = decodeRect1();
        g.setPaint(color24);
        g.fill(rect);

    }

    private void paintBackgroundMouseOver(Graphics2D g) {
        path = decodePath3();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient10(path));
        g.fill(path);
        rect = decodeRect1();
        g.setPaint(color18);
        g.fill(rect);

    }

    private void paintBackgroundPressed(Graphics2D g) {
        path = decodePath6();
        g.setPaint(decodeGradient8(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient11(path));
        g.fill(path);
        rect = decodeRect1();
        g.setPaint(color24);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color25);
        g.fill(rect);

    }

    private void paintForegroundDisabled(Graphics2D g) {
        path = decodePath7();
        g.setPaint(color26);
        g.fill(path);

    }

    private void paintForegroundEnabled(Graphics2D g) {
        path = decodePath7();
        g.setPaint(decodeGradient12(path));
        g.fill(path);

    }

    private void paintForegroundFocused(Graphics2D g) {
        path = decodePath8();
        g.setPaint(decodeGradient12(path));
        g.fill(path);

    }

    private void paintForegroundMouseOverAndFocused(Graphics2D g) {
        path = decodePath8();
        g.setPaint(decodeGradient12(path));
        g.fill(path);

    }

    private void paintForegroundPressedAndFocused(Graphics2D g) {
        path = decodePath9();
        g.setPaint(color29);
        g.fill(path);

    }

    private void paintForegroundMouseOver(Graphics2D g) {
        path = decodePath7();
        g.setPaint(decodeGradient12(path));
        g.fill(path);

    }

    private void paintForegroundPressed(Graphics2D g) {
        path = decodePath9();
        g.setPaint(color29);
        g.fill(path);

    }



    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(0.2857143f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(0.2857142984867096f, 0.0f), decodeAnchorX(2.0f, -3.6363636363636402f), decodeAnchorY(0.2857142984867096f, 0.0f), decodeX(2.0f), decodeY(0.2857143f));
        path.curveTo(decodeAnchorX(2.0f, 3.6363636363636402f), decodeAnchorY(0.2857142984867096f, 0.0f), decodeAnchorX(2.7142858505249023f, -0.022727272727273373f), decodeAnchorY(1.0f, -3.749999999999999f), decodeX(2.7142859f), decodeY(1.0f));
        path.curveTo(decodeAnchorX(2.7142858505249023f, 0.022727272727273373f), decodeAnchorY(1.0f, 3.75f), decodeAnchorX(2.7142858505249023f, 0.0f), decodeAnchorY(3.0f, 0.0f), decodeX(2.7142859f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(3.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(2.0f));
        path.lineTo(decodeX(1.0f), decodeY(0.42857143f));
        path.curveTo(decodeAnchorX(1.0f, 0.0f), decodeAnchorY(0.4285714328289032f, 0.0f), decodeAnchorX(2.0f, -3.0f), decodeAnchorY(0.4285714328289032f, 0.0f), decodeX(2.0f), decodeY(0.42857143f));
        path.curveTo(decodeAnchorX(2.0f, 3.0f), decodeAnchorY(0.4285714328289032f, 0.0f), decodeAnchorX(2.5714285373687744f, 0.0f), decodeAnchorY(1.0f, -2.0f), decodeX(2.5714285f), decodeY(1.0f));
        path.curveTo(decodeAnchorX(2.5714285373687744f, 0.0f), decodeAnchorY(1.0f, 2.0f), decodeAnchorX(2.5714285373687744f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeX(2.5714285f), decodeY(2.0f));
        path.lineTo(decodeX(1.0f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(2.0f), //y
                         decodeX(2.5714285f) - decodeX(1.0f), //width
                         decodeY(3.0f) - decodeY(2.0f)); //height
        return rect;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(0.2857143f));
        path.lineTo(decodeX(2.0f), decodeY(0.2857143f));
        path.curveTo(decodeAnchorX(2.0f, 3.6363636363636402f), decodeAnchorY(0.2857142984867096f, 0.0f), decodeAnchorX(2.7142858505249023f, -0.022727272727273373f), decodeAnchorY(1.0f, -3.749999999999999f), decodeX(2.7142859f), decodeY(1.0f));
        path.lineTo(decodeX(2.7142859f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(3.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(2.0f));
        path.lineTo(decodeX(1.0f), decodeY(0.42857143f));
        path.lineTo(decodeX(2.0f), decodeY(0.42857143f));
        path.curveTo(decodeAnchorX(2.0f, 3.0f), decodeAnchorY(0.4285714328289032f, 0.0f), decodeAnchorX(2.5714285373687744f, 0.0f), decodeAnchorY(1.0f, -2.0f), decodeX(2.5714285f), decodeY(1.0f));
        path.lineTo(decodeX(2.5714285f), decodeY(2.0f));
        path.lineTo(decodeX(1.0f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath5() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(0.08571429f));
        path.lineTo(decodeX(2.142857f), decodeY(0.08571429f));
        path.curveTo(decodeAnchorX(2.142857074737549f, 3.3999999999999986f), decodeAnchorY(0.08571428805589676f, 0.0f), decodeAnchorX(2.914285659790039f, 0.0f), decodeAnchorY(1.0f, -3.4f), decodeX(2.9142857f), decodeY(1.0f));
        path.lineTo(decodeX(2.9142857f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(3.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath6() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(0.2857143f));
        path.lineTo(decodeX(2.0f), decodeY(0.2857143f));
        path.curveTo(decodeAnchorX(2.0f, 3.4545454545454533f), decodeAnchorY(0.2857142984867096f, 0.0f), decodeAnchorX(2.7142858505249023f, -0.022727272727273373f), decodeAnchorY(1.0f, -3.4772727272727266f), decodeX(2.7142859f), decodeY(1.0f));
        path.lineTo(decodeX(2.7142859f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(3.0f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect2() {
            rect.setRect(decodeX(0.0f), //x
                         decodeY(0.0f), //y
                         decodeX(0.0f) - decodeX(0.0f), //width
                         decodeY(0.0f) - decodeY(0.0f)); //height
        return rect;
    }

    private Path2D decodePath7() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(2.0f));
        path.lineTo(decodeX(1.490909f), decodeY(1.0284091f));
        path.lineTo(decodeX(2.0f), decodeY(2.0f));
        path.lineTo(decodeX(1.0f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath8() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(2.0f));
        path.lineTo(decodeX(1.490909f), decodeY(1.3522727f));
        path.lineTo(decodeX(2.0f), decodeY(2.0f));
        path.lineTo(decodeX(1.0f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath9() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(2.0f));
        path.lineTo(decodeX(1.5045455f), decodeY(1.0795455f));
        path.lineTo(decodeX(2.0f), decodeY(2.0f));
        path.lineTo(decodeX(1.0f), decodeY(2.0f));
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
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color3,
                            decodeColor(color3,color4,0.5f),
                            color4});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color6,
                            decodeColor(color6,color7,0.5f),
                            color7});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.36497328f,0.72994655f,0.8649733f,1.0f },
                new Color[] { color8,
                            decodeColor(color8,color9,0.5f),
                            color9,
                            decodeColor(color9,color10,0.5f),
                            color10});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.37566844f,0.7513369f,0.8756684f,1.0f },
                new Color[] { color8,
                            decodeColor(color8,color9,0.5f),
                            color9,
                            decodeColor(color9,color10,0.5f),
                            color10});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color13,
                            decodeColor(color13,color14,0.5f),
                            color14});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.37967914f,0.7593583f,0.87967914f,1.0f },
                new Color[] { color15,
                            decodeColor(color15,color16,0.5f),
                            color16,
                            decodeColor(color16,color17,0.5f),
                            color17});
    }

    private Paint decodeGradient8(Shape s) {
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

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.37165776f,0.7433155f,0.8716577f,1.0f },
                new Color[] { color21,
                            decodeColor(color21,color22,0.5f),
                            color22,
                            decodeColor(color22,color23,0.5f),
                            color23});
    }

    private Paint decodeGradient10(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.3970588f,0.7941176f,0.89705884f,1.0f },
                new Color[] { color15,
                            decodeColor(color15,color16,0.5f),
                            color16,
                            decodeColor(color16,color17,0.5f),
                            color17});
    }

    private Paint decodeGradient11(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.4318182f,0.8636364f,0.9318182f,1.0f },
                new Color[] { color21,
                            decodeColor(color21,color22,0.5f),
                            color22,
                            decodeColor(color22,color23,0.5f),
                            color23});
    }

    private Paint decodeGradient12(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.48636365f * w) + x, (0.0116959065f * h) + y, (0.4909091f * w) + x, (0.8888889f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color27,
                            decodeColor(color27,color28,0.5f),
                            color28});
    }


}

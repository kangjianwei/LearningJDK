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


final class ToolBarButtonPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of ToolBarButtonPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_ENABLED = 1;
    static final int BACKGROUND_FOCUSED = 2;
    static final int BACKGROUND_MOUSEOVER = 3;
    static final int BACKGROUND_MOUSEOVER_FOCUSED = 4;
    static final int BACKGROUND_PRESSED = 5;
    static final int BACKGROUND_PRESSED_FOCUSED = 6;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of ToolBarButtonPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
    private Color color2 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.06885965f, -0.36862746f, -153);
    private Color color3 = decodeColor("nimbusBlueGrey", 0.0f, -0.020974077f, -0.21960783f, 0);
    private Color color4 = decodeColor("nimbusBlueGrey", 0.0f, 0.11169591f, -0.53333336f, 0);
    private Color color5 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.10658931f, 0.25098038f, 0);
    private Color color6 = decodeColor("nimbusBlueGrey", 0.0f, -0.098526314f, 0.2352941f, 0);
    private Color color7 = decodeColor("nimbusBlueGrey", 0.0f, -0.07333623f, 0.20392156f, 0);
    private Color color8 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color9 = decodeColor("nimbusBlueGrey", -0.00505054f, -0.05960039f, 0.10196078f, 0);
    private Color color10 = decodeColor("nimbusBlueGrey", -0.008547008f, -0.04772438f, 0.06666666f, 0);
    private Color color11 = decodeColor("nimbusBlueGrey", -0.0027777553f, -0.0018306673f, -0.02352941f, 0);
    private Color color12 = decodeColor("nimbusBlueGrey", -0.0027777553f, -0.0212406f, 0.13333333f, 0);
    private Color color13 = decodeColor("nimbusBlueGrey", 0.0055555105f, -0.030845039f, 0.23921567f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public ToolBarButtonPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_FOCUSED: paintBackgroundFocused(g); break;
            case BACKGROUND_MOUSEOVER: paintBackgroundMouseOver(g); break;
            case BACKGROUND_MOUSEOVER_FOCUSED: paintBackgroundMouseOverAndFocused(g); break;
            case BACKGROUND_PRESSED: paintBackgroundPressed(g); break;
            case BACKGROUND_PRESSED_FOCUSED: paintBackgroundPressedAndFocused(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundFocused(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color1);
        g.fill(path);

    }

    private void paintBackgroundMouseOver(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color2);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient1(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundMouseOverAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient1(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundPressed(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color2);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient1(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient3(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundPressedAndFocused(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient1(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient3(roundRect));
        g.fill(roundRect);

    }



    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(1.4133738f), decodeY(0.120000005f));
        path.lineTo(decodeX(1.9893618f), decodeY(0.120000005f));
        path.curveTo(decodeAnchorX(1.9893617630004883f, 3.0f), decodeAnchorY(0.12000000476837158f, 0.0f), decodeAnchorX(2.8857147693634033f, 0.0f), decodeAnchorY(1.04347825050354f, -3.0f), decodeX(2.8857148f), decodeY(1.0434783f));
        path.lineTo(decodeX(2.9f), decodeY(1.9565217f));
        path.curveTo(decodeAnchorX(2.9000000953674316f, 0.0f), decodeAnchorY(1.95652174949646f, 3.0f), decodeAnchorX(1.9893617630004883f, 3.0f), decodeAnchorY(2.8714287281036377f, 0.0f), decodeX(1.9893618f), decodeY(2.8714287f));
        path.lineTo(decodeX(1.0106384f), decodeY(2.8714287f));
        path.curveTo(decodeAnchorX(1.0106383562088013f, -3.0f), decodeAnchorY(2.8714287281036377f, 0.0f), decodeAnchorX(0.12000000476837158f, 0.0f), decodeAnchorY(1.95652174949646f, 3.0f), decodeX(0.120000005f), decodeY(1.9565217f));
        path.lineTo(decodeX(0.120000005f), decodeY(1.0465839f));
        path.curveTo(decodeAnchorX(0.12000000476837158f, 0.0f), decodeAnchorY(1.046583890914917f, -3.000000000000001f), decodeAnchorX(1.0106383562088013f, -3.0f), decodeAnchorY(0.12000000476837158f, 0.0f), decodeX(1.0106384f), decodeY(0.120000005f));
        path.lineTo(decodeX(1.4148936f), decodeY(0.120000005f));
        path.lineTo(decodeX(1.4148936f), decodeY(0.4857143f));
        path.lineTo(decodeX(1.0106384f), decodeY(0.4857143f));
        path.curveTo(decodeAnchorX(1.0106383562088013f, -1.928571428571427f), decodeAnchorY(0.48571428656578064f, 0.0f), decodeAnchorX(0.4714285731315613f, -0.04427948362011014f), decodeAnchorY(1.040372610092163f, -2.429218094741624f), decodeX(0.47142857f), decodeY(1.0403726f));
        path.lineTo(decodeX(0.47142857f), decodeY(1.9565217f));
        path.curveTo(decodeAnchorX(0.4714285731315613f, 0.0f), decodeAnchorY(1.95652174949646f, 2.2142857142856975f), decodeAnchorX(1.0106383562088013f, -1.7857142857142847f), decodeAnchorY(2.5142855644226074f, 0.0f), decodeX(1.0106384f), decodeY(2.5142856f));
        path.lineTo(decodeX(1.9893618f), decodeY(2.5142856f));
        path.curveTo(decodeAnchorX(1.9893617630004883f, 2.071428571428598f), decodeAnchorY(2.5142855644226074f, 0.0f), decodeAnchorX(2.5f, 0.0f), decodeAnchorY(1.95652174949646f, 2.2142857142857046f), decodeX(2.5f), decodeY(1.9565217f));
        path.lineTo(decodeX(2.5142853f), decodeY(1.0434783f));
        path.curveTo(decodeAnchorX(2.5142853260040283f, 0.0f), decodeAnchorY(1.04347825050354f, -2.1428571428571406f), decodeAnchorX(1.990121603012085f, 2.142857142857167f), decodeAnchorY(0.4714285731315613f, 0.0f), decodeX(1.9901216f), decodeY(0.47142857f));
        path.lineTo(decodeX(1.4148936f), decodeY(0.4857143f));
        path.lineTo(decodeX(1.4133738f), decodeY(0.120000005f));
        path.closePath();
        return path;
    }

    private RoundRectangle2D decodeRoundRect1() {
        roundRect.setRoundRect(decodeX(0.4f), //x
                               decodeY(0.6f), //y
                               decodeX(2.6f) - decodeX(0.4f), //width
                               decodeY(2.8f) - decodeY(0.6f), //height
                               12.0f, 12.0f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect2() {
        roundRect.setRoundRect(decodeX(0.4f), //x
                               decodeY(0.4f), //y
                               decodeX(2.6f) - decodeX(0.4f), //width
                               decodeY(2.6f) - decodeY(0.4f), //height
                               12.0f, 12.0f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect3() {
        roundRect.setRoundRect(decodeX(0.6f), //x
                               decodeY(0.6f), //y
                               decodeX(2.4f) - decodeX(0.6f), //width
                               decodeY(2.4f) - decodeY(0.6f), //height
                               9.0f, 9.0f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect4() {
        roundRect.setRoundRect(decodeX(0.120000005f), //x
                               decodeY(0.120000005f), //y
                               decodeX(2.8800004f) - decodeX(0.120000005f), //width
                               decodeY(2.8800004f) - decodeY(0.120000005f), //height
                               13.0f, 13.0f); //rounding
        return roundRect;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.09f,0.52f,0.95f },
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
                new float[] { 0.0f,0.03f,0.06f,0.33f,0.6f,0.65f,0.7f,0.825f,0.95f,0.975f,1.0f },
                new Color[] { color5,
                            decodeColor(color5,color6,0.5f),
                            color6,
                            decodeColor(color6,color7,0.5f),
                            color7,
                            decodeColor(color7,color7,0.5f),
                            color7,
                            decodeColor(color7,color8,0.5f),
                            color8,
                            decodeColor(color8,color8,0.5f),
                            color8});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.03f,0.06f,0.33f,0.6f,0.65f,0.7f,0.825f,0.95f,0.975f,1.0f },
                new Color[] { color9,
                            decodeColor(color9,color10,0.5f),
                            color10,
                            decodeColor(color10,color11,0.5f),
                            color11,
                            decodeColor(color11,color11,0.5f),
                            color11,
                            decodeColor(color11,color12,0.5f),
                            color12,
                            decodeColor(color12,color13,0.5f),
                            color13});
    }


}

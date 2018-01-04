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


final class InternalFrameTitlePaneIconifyButtonPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of InternalFrameTitlePaneIconifyButtonPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_ENABLED = 1;
    static final int BACKGROUND_DISABLED = 2;
    static final int BACKGROUND_MOUSEOVER = 3;
    static final int BACKGROUND_PRESSED = 4;
    static final int BACKGROUND_ENABLED_WINDOWNOTFOCUSED = 5;
    static final int BACKGROUND_MOUSEOVER_WINDOWNOTFOCUSED = 6;
    static final int BACKGROUND_PRESSED_WINDOWNOTFOCUSED = 7;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of InternalFrameTitlePaneIconifyButtonPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", 0.0055555105f, -0.0029994324f, -0.38039216f, -185);
    private Color color2 = decodeColor("nimbusOrange", -0.08377897f, 0.02094239f, -0.40392157f, 0);
    private Color color3 = decodeColor("nimbusOrange", 0.0f, 0.0f, 0.0f, 0);
    private Color color4 = decodeColor("nimbusOrange", -4.4563413E-4f, -0.48364475f, 0.10588235f, 0);
    private Color color5 = decodeColor("nimbusOrange", 0.0f, -0.0050992966f, 0.0039215684f, 0);
    private Color color6 = decodeColor("nimbusOrange", 0.0f, -0.12125945f, 0.10588235f, 0);
    private Color color7 = decodeColor("nimbusOrange", -0.08377897f, 0.02094239f, -0.40392157f, -106);
    private Color color8 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color9 = decodeColor("nimbusOrange", 0.5203877f, -0.9376068f, 0.007843137f, 0);
    private Color color10 = decodeColor("nimbusOrange", 0.5273321f, -0.8903002f, -0.086274505f, 0);
    private Color color11 = decodeColor("nimbusOrange", 0.5273321f, -0.93313926f, 0.019607842f, 0);
    private Color color12 = decodeColor("nimbusOrange", 0.53526866f, -0.8995122f, -0.058823526f, 0);
    private Color color13 = decodeColor("nimbusOrange", 0.5233639f, -0.8971863f, -0.07843137f, 0);
    private Color color14 = decodeColor("nimbusBlueGrey", -0.0808081f, 0.015910469f, -0.40392157f, -216);
    private Color color15 = decodeColor("nimbusBlueGrey", -0.003968239f, -0.03760965f, 0.007843137f, 0);
    private Color color16 = new Color(255, 200, 0, 255);
    private Color color17 = decodeColor("nimbusOrange", -0.08377897f, 0.02094239f, -0.31764707f, 0);
    private Color color18 = decodeColor("nimbusOrange", -0.02758849f, 0.02094239f, -0.062745094f, 0);
    private Color color19 = decodeColor("nimbusOrange", -4.4563413E-4f, -0.5074419f, 0.1490196f, 0);
    private Color color20 = decodeColor("nimbusOrange", 9.745359E-6f, -0.11175901f, 0.07843137f, 0);
    private Color color21 = decodeColor("nimbusOrange", 0.0f, -0.09280169f, 0.07843137f, 0);
    private Color color22 = decodeColor("nimbusOrange", 0.0f, -0.19002807f, 0.18039215f, 0);
    private Color color23 = decodeColor("nimbusOrange", -0.025772434f, 0.02094239f, 0.05098039f, 0);
    private Color color24 = decodeColor("nimbusOrange", -0.08377897f, 0.02094239f, -0.4f, 0);
    private Color color25 = decodeColor("nimbusOrange", -0.053104125f, 0.02094239f, -0.109803915f, 0);
    private Color color26 = decodeColor("nimbusOrange", -0.017887495f, -0.33726656f, 0.039215684f, 0);
    private Color color27 = decodeColor("nimbusOrange", -0.018038228f, 0.02094239f, -0.043137252f, 0);
    private Color color28 = decodeColor("nimbusOrange", -0.015844189f, 0.02094239f, -0.027450979f, 0);
    private Color color29 = decodeColor("nimbusOrange", -0.010274701f, 0.02094239f, 0.015686274f, 0);
    private Color color30 = decodeColor("nimbusOrange", -0.08377897f, 0.02094239f, -0.14509803f, -91);
    private Color color31 = decodeColor("nimbusOrange", 0.5273321f, -0.87971985f, -0.15686274f, 0);
    private Color color32 = decodeColor("nimbusOrange", 0.5273321f, -0.842694f, -0.31764707f, 0);
    private Color color33 = decodeColor("nimbusOrange", 0.516221f, -0.9567362f, 0.12941176f, 0);
    private Color color34 = decodeColor("nimbusOrange", 0.5222816f, -0.9229352f, 0.019607842f, 0);
    private Color color35 = decodeColor("nimbusOrange", 0.5273321f, -0.91751915f, 0.015686274f, 0);
    private Color color36 = decodeColor("nimbusOrange", 0.5273321f, -0.9193561f, 0.039215684f, 0);
    private Color color37 = decodeColor("nimbusBlueGrey", -0.01111114f, -0.017933726f, -0.32156864f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public InternalFrameTitlePaneIconifyButtonPainter(PaintContext ctx, int state) {
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

    private void paintBackgroundEnabled(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient1(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color7);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color8);
        g.fill(rect);

    }

    private void paintBackgroundDisabled(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient3(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient4(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color14);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color15);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);

    }

    private void paintBackgroundMouseOver(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient5(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient6(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color23);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color8);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);

    }

    private void paintBackgroundPressed(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient7(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient8(roundRect));
        g.fill(roundRect);
        rect = decodeRect4();
        g.setPaint(color30);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color8);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);

    }

    private void paintBackgroundEnabledAndWindowNotFocused(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient9(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient10(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color14);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color37);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);

    }

    private void paintBackgroundMouseOverAndWindowNotFocused(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient5(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient6(roundRect));
        g.fill(roundRect);
        rect = decodeRect1();
        g.setPaint(color23);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color8);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);

    }

    private void paintBackgroundPressedAndWindowNotFocused(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient7(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient8(roundRect));
        g.fill(roundRect);
        rect = decodeRect4();
        g.setPaint(color30);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(color8);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);

    }



    private RoundRectangle2D decodeRoundRect1() {
        roundRect.setRoundRect(decodeX(1.0f), //x
                               decodeY(1.6111112f), //y
                               decodeX(2.0f) - decodeX(1.0f), //width
                               decodeY(2.0f) - decodeY(1.6111112f), //height
                               6.0f, 6.0f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect2() {
        roundRect.setRoundRect(decodeX(1.0f), //x
                               decodeY(1.0f), //y
                               decodeX(2.0f) - decodeX(1.0f), //width
                               decodeY(1.9444444f) - decodeY(1.0f), //height
                               8.6f, 8.6f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect3() {
        roundRect.setRoundRect(decodeX(1.0526316f), //x
                               decodeY(1.0555556f), //y
                               decodeX(1.9473684f) - decodeX(1.0526316f), //width
                               decodeY(1.8888888f) - decodeY(1.0555556f), //height
                               6.75f, 6.75f); //rounding
        return roundRect;
    }

    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(1.25f), //x
                         decodeY(1.6628788f), //y
                         decodeX(1.75f) - decodeX(1.25f), //width
                         decodeY(1.7487373f) - decodeY(1.6628788f)); //height
        return rect;
    }

    private Rectangle2D decodeRect2() {
            rect.setRect(decodeX(1.2870814f), //x
                         decodeY(1.6123737f), //y
                         decodeX(1.7165072f) - decodeX(1.2870814f), //width
                         decodeY(1.7222222f) - decodeY(1.6123737f)); //height
        return rect;
    }

    private Rectangle2D decodeRect3() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(1.0f), //y
                         decodeX(1.0f) - decodeX(1.0f), //width
                         decodeY(1.0f) - decodeY(1.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect4() {
            rect.setRect(decodeX(1.25f), //x
                         decodeY(1.6527778f), //y
                         decodeX(1.7511961f) - decodeX(1.25f), //width
                         decodeY(1.7828283f) - decodeY(1.6527778f)); //height
        return rect;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
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
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.8252841f,1.0f },
                new Color[] { color4,
                            decodeColor(color4,color3,0.5f),
                            color3,
                            decodeColor(color3,color5,0.5f),
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
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
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
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.8252841f,1.0f },
                new Color[] { color11,
                            decodeColor(color11,color12,0.5f),
                            color12,
                            decodeColor(color12,color13,0.5f),
                            color13,
                            decodeColor(color13,color10,0.5f),
                            color10});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color17,
                            decodeColor(color17,color18,0.5f),
                            color18});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.8252841f,1.0f },
                new Color[] { color19,
                            decodeColor(color19,color20,0.5f),
                            color20,
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
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color24,
                            decodeColor(color24,color25,0.5f),
                            color25});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.8252841f,1.0f },
                new Color[] { color26,
                            decodeColor(color26,color27,0.5f),
                            color27,
                            decodeColor(color27,color28,0.5f),
                            color28,
                            decodeColor(color28,color29,0.5f),
                            color29});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color31,
                            decodeColor(color31,color32,0.5f),
                            color32});
    }

    private Paint decodeGradient10(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.78336793f,0.9161677f },
                new Color[] { color33,
                            decodeColor(color33,color34,0.5f),
                            color34,
                            decodeColor(color34,color35,0.5f),
                            color35,
                            decodeColor(color35,color36,0.5f),
                            color36});
    }


}

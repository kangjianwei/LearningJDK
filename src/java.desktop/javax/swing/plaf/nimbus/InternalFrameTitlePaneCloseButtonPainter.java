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


final class InternalFrameTitlePaneCloseButtonPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of InternalFrameTitlePaneCloseButtonPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED = 1;
    static final int BACKGROUND_ENABLED = 2;
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
    //by a particular instance of InternalFrameTitlePaneCloseButtonPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusRed", 0.5893519f, -0.75736576f, 0.09411764f, 0);
    private Color color2 = decodeColor("nimbusRed", 0.5962963f, -0.71005917f, 0.0f, 0);
    private Color color3 = decodeColor("nimbusRed", 0.6005698f, -0.7200287f, -0.015686274f, -122);
    private Color color4 = decodeColor("nimbusBlueGrey", 0.0055555105f, -0.062449392f, 0.07058823f, 0);
    private Color color5 = decodeColor("nimbusBlueGrey", 0.0055555105f, -0.0029994324f, -0.38039216f, -185);
    private Color color6 = decodeColor("nimbusRed", -0.014814814f, 0.20118344f, -0.4431373f, 0);
    private Color color7 = decodeColor("nimbusRed", -2.7342606E-4f, 0.13829035f, -0.039215684f, 0);
    private Color color8 = decodeColor("nimbusRed", 6.890595E-4f, -0.36665577f, 0.11764705f, 0);
    private Color color9 = decodeColor("nimbusRed", -0.001021713f, 0.101804554f, -0.031372547f, 0);
    private Color color10 = decodeColor("nimbusRed", -2.7342606E-4f, 0.13243341f, -0.035294116f, 0);
    private Color color11 = decodeColor("nimbusRed", -2.7342606E-4f, 0.002258718f, 0.06666666f, 0);
    private Color color12 = decodeColor("nimbusRed", 0.0056530247f, 0.0040003657f, -0.38431373f, -122);
    private Color color13 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color14 = decodeColor("nimbusRed", -0.014814814f, 0.20118344f, -0.3882353f, 0);
    private Color color15 = decodeColor("nimbusRed", -0.014814814f, 0.20118344f, -0.13333333f, 0);
    private Color color16 = decodeColor("nimbusRed", 6.890595E-4f, -0.38929275f, 0.1607843f, 0);
    private Color color17 = decodeColor("nimbusRed", 2.537202E-5f, 0.012294531f, 0.043137252f, 0);
    private Color color18 = decodeColor("nimbusRed", -2.7342606E-4f, 0.033585668f, 0.039215684f, 0);
    private Color color19 = decodeColor("nimbusRed", -2.7342606E-4f, -0.07198727f, 0.14117646f, 0);
    private Color color20 = decodeColor("nimbusRed", -0.014814814f, 0.20118344f, 0.0039215684f, -122);
    private Color color21 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -140);
    private Color color22 = decodeColor("nimbusRed", -0.014814814f, 0.20118344f, -0.49411768f, 0);
    private Color color23 = decodeColor("nimbusRed", -0.014814814f, 0.20118344f, -0.20392159f, 0);
    private Color color24 = decodeColor("nimbusRed", -0.014814814f, -0.21260965f, 0.019607842f, 0);
    private Color color25 = decodeColor("nimbusRed", -0.014814814f, 0.17340565f, -0.09803921f, 0);
    private Color color26 = decodeColor("nimbusRed", -0.014814814f, 0.20118344f, -0.10588235f, 0);
    private Color color27 = decodeColor("nimbusRed", -0.014814814f, 0.20118344f, -0.04705882f, 0);
    private Color color28 = decodeColor("nimbusRed", -0.014814814f, 0.20118344f, -0.31764707f, -122);
    private Color color29 = decodeColor("nimbusRed", 0.5962963f, -0.6994788f, -0.07058823f, 0);
    private Color color30 = decodeColor("nimbusRed", 0.5962963f, -0.66245294f, -0.23137257f, 0);
    private Color color31 = decodeColor("nimbusRed", 0.58518517f, -0.77649516f, 0.21568626f, 0);
    private Color color32 = decodeColor("nimbusRed", 0.5962963f, -0.7372781f, 0.10196078f, 0);
    private Color color33 = decodeColor("nimbusRed", 0.5962963f, -0.73911506f, 0.12549019f, 0);
    private Color color34 = decodeColor("nimbusBlueGrey", 0.0f, -0.027957506f, -0.31764707f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public InternalFrameTitlePaneCloseButtonPainter(PaintContext ctx, int state) {
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

    private void paintBackgroundDisabled(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient1(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(color3);
        g.fill(path);
        path = decodePath2();
        g.setPaint(color4);
        g.fill(path);

    }

    private void paintBackgroundEnabled(Graphics2D g) {
        roundRect = decodeRoundRect2();
        g.setPaint(color5);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient3(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(color12);
        g.fill(path);
        path = decodePath2();
        g.setPaint(color13);
        g.fill(path);

    }

    private void paintBackgroundMouseOver(Graphics2D g) {
        roundRect = decodeRoundRect2();
        g.setPaint(color5);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient4(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect4();
        g.setPaint(decodeGradient5(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(color20);
        g.fill(path);
        path = decodePath2();
        g.setPaint(color13);
        g.fill(path);

    }

    private void paintBackgroundPressed(Graphics2D g) {
        roundRect = decodeRoundRect2();
        g.setPaint(color21);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient6(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient7(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(color28);
        g.fill(path);
        path = decodePath2();
        g.setPaint(color13);
        g.fill(path);

    }

    private void paintBackgroundEnabledAndWindowNotFocused(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient8(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient9(roundRect));
        g.fill(roundRect);
        path = decodePath2();
        g.setPaint(color34);
        g.fill(path);

    }

    private void paintBackgroundMouseOverAndWindowNotFocused(Graphics2D g) {
        roundRect = decodeRoundRect2();
        g.setPaint(color5);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient4(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect4();
        g.setPaint(decodeGradient5(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(color20);
        g.fill(path);
        path = decodePath2();
        g.setPaint(color13);
        g.fill(path);

    }

    private void paintBackgroundPressedAndWindowNotFocused(Graphics2D g) {
        roundRect = decodeRoundRect2();
        g.setPaint(color21);
        g.fill(roundRect);
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient6(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient7(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(color28);
        g.fill(path);
        path = decodePath2();
        g.setPaint(color13);
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

    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(1.25f), decodeY(1.7373737f));
        path.lineTo(decodeX(1.3002392f), decodeY(1.794192f));
        path.lineTo(decodeX(1.5047847f), decodeY(1.5909091f));
        path.lineTo(decodeX(1.6842105f), decodeY(1.7954545f));
        path.lineTo(decodeX(1.7595694f), decodeY(1.719697f));
        path.lineTo(decodeX(1.5956938f), decodeY(1.5239899f));
        path.lineTo(decodeX(1.7535884f), decodeY(1.3409091f));
        path.lineTo(decodeX(1.6830144f), decodeY(1.2537879f));
        path.lineTo(decodeX(1.5083733f), decodeY(1.4406565f));
        path.lineTo(decodeX(1.3301436f), decodeY(1.2563131f));
        path.lineTo(decodeX(1.257177f), decodeY(1.3320707f));
        path.lineTo(decodeX(1.4270334f), decodeY(1.5252526f));
        path.lineTo(decodeX(1.25f), decodeY(1.7373737f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(1.257177f), decodeY(1.2828283f));
        path.lineTo(decodeX(1.3217703f), decodeY(1.2133838f));
        path.lineTo(decodeX(1.5f), decodeY(1.4040405f));
        path.lineTo(decodeX(1.673445f), decodeY(1.2108586f));
        path.lineTo(decodeX(1.7440192f), decodeY(1.2853535f));
        path.lineTo(decodeX(1.5669856f), decodeY(1.4709597f));
        path.lineTo(decodeX(1.7488039f), decodeY(1.6527778f));
        path.lineTo(decodeX(1.673445f), decodeY(1.7398989f));
        path.lineTo(decodeX(1.4988039f), decodeY(1.5416667f));
        path.lineTo(decodeX(1.3313397f), decodeY(1.7424242f));
        path.lineTo(decodeX(1.2523923f), decodeY(1.6565657f));
        path.lineTo(decodeX(1.4366028f), decodeY(1.4722222f));
        path.lineTo(decodeX(1.257177f), decodeY(1.2828283f));
        path.closePath();
        return path;
    }

    private RoundRectangle2D decodeRoundRect2() {
        roundRect.setRoundRect(decodeX(1.0f), //x
                               decodeY(1.6111112f), //y
                               decodeX(2.0f) - decodeX(1.0f), //width
                               decodeY(2.0f) - decodeY(1.6111112f), //height
                               6.0f, 6.0f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect3() {
        roundRect.setRoundRect(decodeX(1.0526316f), //x
                               decodeY(1.0530303f), //y
                               decodeX(1.9473684f) - decodeX(1.0526316f), //width
                               decodeY(1.8863636f) - decodeY(1.0530303f), //height
                               6.75f, 6.75f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect4() {
        roundRect.setRoundRect(decodeX(1.0526316f), //x
                               decodeY(1.0517677f), //y
                               decodeX(1.9473684f) - decodeX(1.0526316f), //width
                               decodeY(1.8851011f) - decodeY(1.0517677f), //height
                               6.75f, 6.75f); //rounding
        return roundRect;
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
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color6,
                            decodeColor(color6,color7,0.5f),
                            color7});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.8252841f,1.0f },
                new Color[] { color8,
                            decodeColor(color8,color9,0.5f),
                            color9,
                            decodeColor(color9,color10,0.5f),
                            color10,
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
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.81480503f,0.97904193f },
                new Color[] { color16,
                            decodeColor(color16,color17,0.5f),
                            color17,
                            decodeColor(color17,color18,0.5f),
                            color18,
                            decodeColor(color18,color19,0.5f),
                            color19});
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
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.81630206f,0.98203593f },
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
                new Color[] { color29,
                            decodeColor(color29,color30,0.5f),
                            color30});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.24101797f,0.48203593f,0.5838324f,0.6856288f,0.8428144f,1.0f },
                new Color[] { color31,
                            decodeColor(color31,color32,0.5f),
                            color32,
                            decodeColor(color32,color32,0.5f),
                            color32,
                            decodeColor(color32,color33,0.5f),
                            color33});
    }


}

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


final class InternalFrameTitlePaneMenuButtonPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of InternalFrameTitlePaneMenuButtonPainter to determine which region/state is being painted
    //by that instance.
    static final int ICON_ENABLED = 1;
    static final int ICON_DISABLED = 2;
    static final int ICON_MOUSEOVER = 3;
    static final int ICON_PRESSED = 4;
    static final int ICON_ENABLED_WINDOWNOTFOCUSED = 5;
    static final int ICON_MOUSEOVER_WINDOWNOTFOCUSED = 6;
    static final int ICON_PRESSED_WINDOWNOTFOCUSED = 7;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of InternalFrameTitlePaneMenuButtonPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", 0.0055555105f, -0.0029994324f, -0.38039216f, -185);
    private Color color2 = decodeColor("nimbusBase", 0.08801502f, 0.3642857f, -0.5019608f, 0);
    private Color color3 = decodeColor("nimbusBase", 0.030543745f, -0.3835404f, -0.09803924f, 0);
    private Color color4 = decodeColor("nimbusBase", 0.029191494f, -0.53801316f, 0.13333333f, 0);
    private Color color5 = decodeColor("nimbusBase", 0.030543745f, -0.3857143f, -0.09411767f, 0);
    private Color color6 = decodeColor("nimbusBase", 0.030543745f, -0.43148893f, 0.007843137f, 0);
    private Color color7 = decodeColor("nimbusBase", 0.029191494f, -0.24935067f, -0.20392159f, -132);
    private Color color8 = decodeColor("nimbusBase", 0.029191494f, -0.24935067f, -0.20392159f, 0);
    private Color color9 = decodeColor("nimbusBase", 0.029191494f, -0.24935067f, -0.20392159f, -123);
    private Color color10 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, 0);
    private Color color11 = decodeColor("nimbusBlueGrey", 0.0055555105f, -0.0029994324f, -0.38039216f, -208);
    private Color color12 = decodeColor("nimbusBase", 0.02551502f, -0.5942635f, 0.20784312f, 0);
    private Color color13 = decodeColor("nimbusBase", 0.032459438f, -0.5490091f, 0.12941176f, 0);
    private Color color14 = decodeColor("nimbusBase", 0.032459438f, -0.5469569f, 0.11372548f, 0);
    private Color color15 = decodeColor("nimbusBase", 0.032459438f, -0.5760128f, 0.23921567f, 0);
    private Color color16 = decodeColor("nimbusBase", 0.08801502f, 0.3642857f, -0.4901961f, 0);
    private Color color17 = decodeColor("nimbusBase", 0.032459438f, -0.1857143f, -0.23529413f, 0);
    private Color color18 = decodeColor("nimbusBase", 0.029191494f, -0.5438224f, 0.17647058f, 0);
    private Color color19 = decodeColor("nimbusBase", 0.030543745f, -0.41929638f, -0.02352941f, 0);
    private Color color20 = decodeColor("nimbusBase", 0.030543745f, -0.45559007f, 0.082352936f, 0);
    private Color color21 = decodeColor("nimbusBase", 0.03409344f, -0.329408f, -0.11372551f, -132);
    private Color color22 = decodeColor("nimbusBase", 0.03409344f, -0.329408f, -0.11372551f, 0);
    private Color color23 = decodeColor("nimbusBase", 0.03409344f, -0.329408f, -0.11372551f, -123);
    private Color color24 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.54901963f, 0);
    private Color color25 = decodeColor("nimbusBase", 0.031104386f, 0.12354499f, -0.33725494f, 0);
    private Color color26 = decodeColor("nimbusBase", 0.032459438f, -0.4592437f, -0.015686274f, 0);
    private Color color27 = decodeColor("nimbusBase", 0.029191494f, -0.2579365f, -0.19607845f, 0);
    private Color color28 = decodeColor("nimbusBase", 0.03409344f, -0.3149596f, -0.13333336f, 0);
    private Color color29 = decodeColor("nimbusBase", 0.029681683f, 0.07857144f, -0.3294118f, -132);
    private Color color30 = decodeColor("nimbusBase", 0.029681683f, 0.07857144f, -0.3294118f, 0);
    private Color color31 = decodeColor("nimbusBase", 0.029681683f, 0.07857144f, -0.3294118f, -123);
    private Color color32 = decodeColor("nimbusBase", 0.032459438f, -0.53637654f, 0.043137252f, 0);
    private Color color33 = decodeColor("nimbusBase", 0.032459438f, -0.49935067f, -0.11764708f, 0);
    private Color color34 = decodeColor("nimbusBase", 0.021348298f, -0.6133929f, 0.32941175f, 0);
    private Color color35 = decodeColor("nimbusBase", 0.042560518f, -0.5804379f, 0.23137254f, 0);
    private Color color36 = decodeColor("nimbusBase", 0.032459438f, -0.57417583f, 0.21568626f, 0);
    private Color color37 = decodeColor("nimbusBase", 0.027408898f, -0.5784226f, 0.20392156f, -132);
    private Color color38 = decodeColor("nimbusBase", 0.042560518f, -0.5665319f, 0.0745098f, 0);
    private Color color39 = decodeColor("nimbusBase", 0.036732912f, -0.5642857f, 0.16470587f, -123);
    private Color color40 = decodeColor("nimbusBase", 0.021348298f, -0.54480517f, -0.11764708f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public InternalFrameTitlePaneMenuButtonPainter(PaintContext ctx, int state) {
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
            case ICON_ENABLED: painticonEnabled(g); break;
            case ICON_DISABLED: painticonDisabled(g); break;
            case ICON_MOUSEOVER: painticonMouseOver(g); break;
            case ICON_PRESSED: painticonPressed(g); break;
            case ICON_ENABLED_WINDOWNOTFOCUSED: painticonEnabledAndWindowNotFocused(g); break;
            case ICON_MOUSEOVER_WINDOWNOTFOCUSED: painticonMouseOverAndWindowNotFocused(g); break;
            case ICON_PRESSED_WINDOWNOTFOCUSED: painticonPressedAndWindowNotFocused(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void painticonEnabled(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient1(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(color10);
        g.fill(path);

    }

    private void painticonDisabled(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color11);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient4(roundRect));
        g.fill(roundRect);
        path = decodePath2();
        g.setPaint(color15);
        g.fill(path);

    }

    private void painticonMouseOver(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient5(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient6(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(color10);
        g.fill(path);

    }

    private void painticonPressed(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient8(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient9(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(decodeGradient10(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(color10);
        g.fill(path);

    }

    private void painticonEnabledAndWindowNotFocused(Graphics2D g) {
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient11(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient12(roundRect));
        g.fill(roundRect);
        path = decodePath3();
        g.setPaint(decodeGradient13(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(color40);
        g.fill(path);

    }

    private void painticonMouseOverAndWindowNotFocused(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient5(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient6(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(color10);
        g.fill(path);

    }

    private void painticonPressedAndWindowNotFocused(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient8(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient9(roundRect));
        g.fill(roundRect);
        path = decodePath1();
        g.setPaint(decodeGradient10(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(color10);
        g.fill(path);

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

    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(1.3157895f), decodeY(1.4444444f));
        path.lineTo(decodeX(1.6842105f), decodeY(1.4444444f));
        path.lineTo(decodeX(1.5013158f), decodeY(1.7208333f));
        path.lineTo(decodeX(1.3157895f), decodeY(1.4444444f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(1.3157895f), decodeY(1.3333334f));
        path.lineTo(decodeX(1.6842105f), decodeY(1.3333334f));
        path.lineTo(decodeX(1.5f), decodeY(1.6083333f));
        path.lineTo(decodeX(1.3157895f), decodeY(1.3333334f));
        path.closePath();
        return path;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(1.3157895f), decodeY(1.3888888f));
        path.lineTo(decodeX(1.6842105f), decodeY(1.3888888f));
        path.lineTo(decodeX(1.4952153f), decodeY(1.655303f));
        path.lineTo(decodeX(1.3157895f), decodeY(1.3888888f));
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
                            decodeColor(color4,color5,0.5f),
                            color5,
                            decodeColor(color5,color3,0.5f),
                            color3,
                            decodeColor(color3,color6,0.5f),
                            color6});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.50714284f * w) + x, (0.095f * h) + y, (0.49285713f * w) + x, (0.91f * h) + y,
                new float[] { 0.0f,0.24289773f,0.48579547f,0.74289775f,1.0f },
                new Color[] { color7,
                            decodeColor(color7,color8,0.5f),
                            color8,
                            decodeColor(color8,color9,0.5f),
                            color9});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.31107953f,0.62215906f,0.8110795f,1.0f },
                new Color[] { color12,
                            decodeColor(color12,color13,0.5f),
                            color13,
                            decodeColor(color13,color14,0.5f),
                            color14});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color16,
                            decodeColor(color16,color17,0.5f),
                            color17});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.8252841f,1.0f },
                new Color[] { color18,
                            decodeColor(color18,color19,0.5f),
                            color19,
                            decodeColor(color19,color19,0.5f),
                            color19,
                            decodeColor(color19,color20,0.5f),
                            color20});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.50714284f * w) + x, (0.095f * h) + y, (0.49285713f * w) + x, (0.91f * h) + y,
                new float[] { 0.0f,0.24289773f,0.48579547f,0.74289775f,1.0f },
                new Color[] { color21,
                            decodeColor(color21,color22,0.5f),
                            color22,
                            decodeColor(color22,color23,0.5f),
                            color23});
    }

    private Paint decodeGradient8(Shape s) {
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

    private Paint decodeGradient9(Shape s) {
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
                            decodeColor(color27,color27,0.5f),
                            color27,
                            decodeColor(color27,color28,0.5f),
                            color28});
    }

    private Paint decodeGradient10(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.50714284f * w) + x, (0.095f * h) + y, (0.49285713f * w) + x, (0.91f * h) + y,
                new float[] { 0.0f,0.24289773f,0.48579547f,0.74289775f,1.0f },
                new Color[] { color29,
                            decodeColor(color29,color30,0.5f),
                            color30,
                            decodeColor(color30,color31,0.5f),
                            color31});
    }

    private Paint decodeGradient11(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.24868421f * w) + x, (0.0014705883f * h) + y, (0.24868421f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color32,
                            decodeColor(color32,color33,0.5f),
                            color33});
    }

    private Paint decodeGradient12(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25441176f * w) + x, (1.0016667f * h) + y,
                new float[] { 0.0f,0.26988637f,0.53977275f,0.5951705f,0.6505682f,0.8252841f,1.0f },
                new Color[] { color34,
                            decodeColor(color34,color35,0.5f),
                            color35,
                            decodeColor(color35,color36,0.5f),
                            color36,
                            decodeColor(color36,color15,0.5f),
                            color15});
    }

    private Paint decodeGradient13(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.50714284f * w) + x, (0.095f * h) + y, (0.49285713f * w) + x, (0.91f * h) + y,
                new float[] { 0.0f,0.24289773f,0.48579547f,0.74289775f,1.0f },
                new Color[] { color37,
                            decodeColor(color37,color38,0.5f),
                            color38,
                            decodeColor(color38,color39,0.5f),
                            color39});
    }


}

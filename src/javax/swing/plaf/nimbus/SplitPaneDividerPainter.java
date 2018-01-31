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


final class SplitPaneDividerPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of SplitPaneDividerPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_ENABLED = 1;
    static final int BACKGROUND_FOCUSED = 2;
    static final int FOREGROUND_ENABLED = 3;
    static final int FOREGROUND_ENABLED_VERTICAL = 4;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of SplitPaneDividerPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", 0.0f, -0.017358616f, -0.11372548f, 0);
    private Color color2 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.102396235f, 0.21960783f, 0);
    private Color color3 = decodeColor("nimbusBlueGrey", 0.0f, -0.07016757f, 0.12941176f, 0);
    private Color color4 = decodeColor("nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
    private Color color5 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color6 = decodeColor("nimbusBlueGrey", 0.0f, -0.048026316f, 0.007843137f, 0);
    private Color color7 = decodeColor("nimbusBlueGrey", 0.0055555105f, -0.06970999f, 0.21568626f, 0);
    private Color color8 = decodeColor("nimbusBlueGrey", 0.0f, -0.06704806f, 0.06666666f, 0);
    private Color color9 = decodeColor("nimbusBlueGrey", 0.0f, -0.019617222f, -0.09803921f, 0);
    private Color color10 = decodeColor("nimbusBlueGrey", 0.004273474f, -0.03790062f, -0.043137252f, 0);
    private Color color11 = decodeColor("nimbusBlueGrey", -0.111111104f, -0.106573746f, 0.24705881f, 0);
    private Color color12 = decodeColor("nimbusBlueGrey", 0.0f, -0.049301825f, 0.02352941f, 0);
    private Color color13 = decodeColor("nimbusBlueGrey", -0.006944418f, -0.07399663f, 0.11372548f, 0);
    private Color color14 = decodeColor("nimbusBlueGrey", -0.018518567f, -0.06998578f, 0.12549019f, 0);
    private Color color15 = decodeColor("nimbusBlueGrey", 0.0f, -0.050526317f, 0.039215684f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public SplitPaneDividerPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_FOCUSED: paintBackgroundFocused(g); break;
            case FOREGROUND_ENABLED: paintForegroundEnabled(g); break;
            case FOREGROUND_ENABLED_VERTICAL: paintForegroundEnabledAndVertical(g); break;

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

    }

    private void paintBackgroundFocused(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(decodeGradient2(rect));
        g.fill(rect);

    }

    private void paintForegroundEnabled(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(decodeGradient3(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient4(roundRect));
        g.fill(roundRect);

    }

    private void paintForegroundEnabledAndVertical(Graphics2D g) {
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient5(roundRect));
        g.fill(roundRect);
        rect = decodeRect2();
        g.setPaint(decodeGradient6(rect));
        g.fill(rect);

    }



    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(0.0f), //y
                         decodeX(2.0f) - decodeX(1.0f), //width
                         decodeY(3.0f) - decodeY(0.0f)); //height
        return rect;
    }

    private RoundRectangle2D decodeRoundRect1() {
        roundRect.setRoundRect(decodeX(1.05f), //x
                               decodeY(1.3f), //y
                               decodeX(1.95f) - decodeX(1.05f), //width
                               decodeY(1.8f) - decodeY(1.3f), //height
                               3.6666667f, 3.6666667f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect2() {
        roundRect.setRoundRect(decodeX(1.1f), //x
                               decodeY(1.4f), //y
                               decodeX(1.9f) - decodeX(1.1f), //width
                               decodeY(1.7f) - decodeY(1.4f), //height
                               4.0f, 4.0f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect3() {
        roundRect.setRoundRect(decodeX(1.3f), //x
                               decodeY(1.1428572f), //y
                               decodeX(1.7f) - decodeX(1.3f), //width
                               decodeY(1.8214285f) - decodeY(1.1428572f), //height
                               4.0f, 4.0f); //rounding
        return roundRect;
    }

    private Rectangle2D decodeRect2() {
            rect.setRect(decodeX(1.4f), //x
                         decodeY(1.1785715f), //y
                         decodeX(1.6f) - decodeX(1.4f), //width
                         decodeY(1.7678571f) - decodeY(1.1785715f)); //height
        return rect;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.058064517f,0.08064516f,0.103225805f,0.116129026f,0.12903225f,0.43387097f,0.7387097f,0.77903223f,0.81935483f,0.85806453f,0.8967742f },
                new Color[] { color1,
                            decodeColor(color1,color2,0.5f),
                            color2,
                            decodeColor(color2,color3,0.5f),
                            color3,
                            decodeColor(color3,color3,0.5f),
                            color3,
                            decodeColor(color3,color2,0.5f),
                            color2,
                            decodeColor(color2,color1,0.5f),
                            color1});
    }

    private Paint decodeGradient2(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.058064517f,0.08064516f,0.103225805f,0.1166129f,0.13f,0.43f,0.73f,0.7746774f,0.81935483f,0.85806453f,0.8967742f },
                new Color[] { color1,
                            decodeColor(color1,color4,0.5f),
                            color4,
                            decodeColor(color4,color3,0.5f),
                            color3,
                            decodeColor(color3,color3,0.5f),
                            color3,
                            decodeColor(color3,color4,0.5f),
                            color4,
                            decodeColor(color4,color1,0.5f),
                            color1});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.20645161f,0.5f,0.7935484f },
                new Color[] { color1,
                            decodeColor(color1,color5,0.5f),
                            color5});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.090322584f,0.2951613f,0.5f,0.5822581f,0.66451615f },
                new Color[] { color6,
                            decodeColor(color6,color7,0.5f),
                            color7,
                            decodeColor(color7,color8,0.5f),
                            color8});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.75f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.42096773f,0.84193546f,0.8951613f,0.9483871f },
                new Color[] { color9,
                            decodeColor(color9,color10,0.5f),
                            color10,
                            decodeColor(color10,color11,0.5f),
                            color11});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.08064516f,0.16129032f,0.5129032f,0.86451614f,0.88548386f,0.90645164f },
                new Color[] { color12,
                            decodeColor(color12,color13,0.5f),
                            color13,
                            decodeColor(color13,color14,0.5f),
                            color14,
                            decodeColor(color14,color15,0.5f),
                            color15});
    }


}

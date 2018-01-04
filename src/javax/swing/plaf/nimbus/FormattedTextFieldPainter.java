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


final class FormattedTextFieldPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of FormattedTextFieldPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED = 1;
    static final int BACKGROUND_ENABLED = 2;
    static final int BACKGROUND_SELECTED = 3;
    static final int BORDER_DISABLED = 4;
    static final int BORDER_FOCUSED = 5;
    static final int BORDER_ENABLED = 6;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of FormattedTextFieldPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", -0.015872955f, -0.07995863f, 0.15294117f, 0);
    private Color color2 = decodeColor("nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
    private Color color3 = decodeColor("nimbusBlueGrey", -0.006944418f, -0.07187897f, 0.06666666f, 0);
    private Color color4 = decodeColor("nimbusBlueGrey", 0.007936537f, -0.07826825f, 0.10588235f, 0);
    private Color color5 = decodeColor("nimbusBlueGrey", 0.007936537f, -0.07856284f, 0.11372548f, 0);
    private Color color6 = decodeColor("nimbusBlueGrey", 0.007936537f, -0.07796818f, 0.09803921f, 0);
    private Color color7 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.0965403f, -0.18431371f, 0);
    private Color color8 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.1048766f, -0.05098039f, 0);
    private Color color9 = decodeColor("nimbusLightBackground", 0.6666667f, 0.004901961f, -0.19999999f, 0);
    private Color color10 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.10512091f, -0.019607842f, 0);
    private Color color11 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.105344966f, 0.011764705f, 0);
    private Color color12 = decodeColor("nimbusFocus", 0.0f, 0.0f, 0.0f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public FormattedTextFieldPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_SELECTED: paintBackgroundSelected(g); break;
            case BORDER_DISABLED: paintBorderDisabled(g); break;
            case BORDER_FOCUSED: paintBorderFocused(g); break;
            case BORDER_ENABLED: paintBorderEnabled(g); break;

        }
    }
        
    protected Object[] getExtendedCacheKeys(JComponent c) {
        Object[] extendedCacheKeys = null;
        switch(state) {
            case BACKGROUND_ENABLED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color2, 0.0f, 0.0f, 0)};
                break;
            case BORDER_FOCUSED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color9, 0.004901961f, -0.19999999f, 0),
                     getComponentColor(c, "background", color2, 0.0f, 0.0f, 0)};
                break;
            case BORDER_ENABLED:
                extendedCacheKeys = new Object[] {
                     getComponentColor(c, "background", color9, 0.004901961f, -0.19999999f, 0),
                     getComponentColor(c, "background", color2, 0.0f, 0.0f, 0)};
                break;
        }
        return extendedCacheKeys;
    }

    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundDisabled(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color1);
        g.fill(rect);

    }

    private void paintBackgroundEnabled(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint((Color)componentColors[0]);
        g.fill(rect);

    }

    private void paintBackgroundSelected(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color2);
        g.fill(rect);

    }

    private void paintBorderDisabled(Graphics2D g) {
        rect = decodeRect2();
        g.setPaint(decodeGradient1(rect));
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(decodeGradient2(rect));
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color6);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color4);
        g.fill(rect);
        rect = decodeRect6();
        g.setPaint(color4);
        g.fill(rect);

    }

    private void paintBorderFocused(Graphics2D g) {
        rect = decodeRect7();
        g.setPaint(decodeGradient3(rect));
        g.fill(rect);
        rect = decodeRect8();
        g.setPaint(decodeGradient4(rect));
        g.fill(rect);
        rect = decodeRect9();
        g.setPaint(color10);
        g.fill(rect);
        rect = decodeRect10();
        g.setPaint(color10);
        g.fill(rect);
        rect = decodeRect11();
        g.setPaint(color11);
        g.fill(rect);
        path = decodePath1();
        g.setPaint(color12);
        g.fill(path);

    }

    private void paintBorderEnabled(Graphics2D g) {
        rect = decodeRect7();
        g.setPaint(decodeGradient5(rect));
        g.fill(rect);
        rect = decodeRect8();
        g.setPaint(decodeGradient4(rect));
        g.fill(rect);
        rect = decodeRect9();
        g.setPaint(color10);
        g.fill(rect);
        rect = decodeRect10();
        g.setPaint(color10);
        g.fill(rect);
        rect = decodeRect11();
        g.setPaint(color11);
        g.fill(rect);

    }



    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(0.4f), //x
                         decodeY(0.4f), //y
                         decodeX(2.6f) - decodeX(0.4f), //width
                         decodeY(2.6f) - decodeY(0.4f)); //height
        return rect;
    }

    private Rectangle2D decodeRect2() {
            rect.setRect(decodeX(0.6666667f), //x
                         decodeY(0.4f), //y
                         decodeX(2.3333333f) - decodeX(0.6666667f), //width
                         decodeY(1.0f) - decodeY(0.4f)); //height
        return rect;
    }

    private Rectangle2D decodeRect3() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(0.6f), //y
                         decodeX(2.0f) - decodeX(1.0f), //width
                         decodeY(1.0f) - decodeY(0.6f)); //height
        return rect;
    }

    private Rectangle2D decodeRect4() {
            rect.setRect(decodeX(0.6666667f), //x
                         decodeY(1.0f), //y
                         decodeX(1.0f) - decodeX(0.6666667f), //width
                         decodeY(2.0f) - decodeY(1.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect5() {
            rect.setRect(decodeX(0.6666667f), //x
                         decodeY(2.3333333f), //y
                         decodeX(2.3333333f) - decodeX(0.6666667f), //width
                         decodeY(2.0f) - decodeY(2.3333333f)); //height
        return rect;
    }

    private Rectangle2D decodeRect6() {
            rect.setRect(decodeX(2.0f), //x
                         decodeY(1.0f), //y
                         decodeX(2.3333333f) - decodeX(2.0f), //width
                         decodeY(2.0f) - decodeY(1.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect7() {
            rect.setRect(decodeX(0.4f), //x
                         decodeY(0.4f), //y
                         decodeX(2.6f) - decodeX(0.4f), //width
                         decodeY(1.0f) - decodeY(0.4f)); //height
        return rect;
    }

    private Rectangle2D decodeRect8() {
            rect.setRect(decodeX(0.6f), //x
                         decodeY(0.6f), //y
                         decodeX(2.4f) - decodeX(0.6f), //width
                         decodeY(1.0f) - decodeY(0.6f)); //height
        return rect;
    }

    private Rectangle2D decodeRect9() {
            rect.setRect(decodeX(0.4f), //x
                         decodeY(1.0f), //y
                         decodeX(0.6f) - decodeX(0.4f), //width
                         decodeY(2.6f) - decodeY(1.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect10() {
            rect.setRect(decodeX(2.4f), //x
                         decodeY(1.0f), //y
                         decodeX(2.6f) - decodeX(2.4f), //width
                         decodeY(2.6f) - decodeY(1.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect11() {
            rect.setRect(decodeX(0.6f), //x
                         decodeY(2.4f), //y
                         decodeX(2.4f) - decodeX(0.6f), //width
                         decodeY(2.6f) - decodeY(2.4f)); //height
        return rect;
    }

    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(0.4f), decodeY(0.4f));
        path.lineTo(decodeX(0.4f), decodeY(2.6f));
        path.lineTo(decodeX(2.6f), decodeY(2.6f));
        path.lineTo(decodeX(2.6f), decodeY(0.4f));
        path.curveTo(decodeAnchorX(2.5999999046325684f, 0.0f), decodeAnchorY(0.4000000059604645f, 0.0f), decodeAnchorX(2.880000352859497f, 0.09999999999999432f), decodeAnchorY(0.4000000059604645f, 0.0f), decodeX(2.8800004f), decodeY(0.4f));
        path.curveTo(decodeAnchorX(2.880000352859497f, 0.09999999999999432f), decodeAnchorY(0.4000000059604645f, 0.0f), decodeAnchorX(2.880000352859497f, 0.0f), decodeAnchorY(2.879999876022339f, 0.0f), decodeX(2.8800004f), decodeY(2.8799999f));
        path.lineTo(decodeX(0.120000005f), decodeY(2.8799999f));
        path.lineTo(decodeX(0.120000005f), decodeY(0.120000005f));
        path.lineTo(decodeX(2.8800004f), decodeY(0.120000005f));
        path.lineTo(decodeX(2.8800004f), decodeY(0.4f));
        path.lineTo(decodeX(0.4f), decodeY(0.4f));
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
                            decodeColor(color5,color1,0.5f),
                            color1});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25f * w) + x, (0.1625f * h) + y,
                new float[] { 0.1f,0.49999997f,0.9f },
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
                new float[] { 0.1f,0.49999997f,0.9f },
                new Color[] { (Color)componentColors[0],
                            decodeColor((Color)componentColors[0],(Color)componentColors[1],0.5f),
                            (Color)componentColors[1]});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.1f,0.49999997f,0.9f },
                new Color[] { color7,
                            decodeColor(color7,color8,0.5f),
                            color8});
    }


}

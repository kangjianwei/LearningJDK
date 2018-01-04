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


final class TabbedPaneTabAreaPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of TabbedPaneTabAreaPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_ENABLED = 1;
    static final int BACKGROUND_DISABLED = 2;
    static final int BACKGROUND_ENABLED_MOUSEOVER = 3;
    static final int BACKGROUND_ENABLED_PRESSED = 4;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of TabbedPaneTabAreaPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = new Color(255, 200, 0, 255);
    private Color color2 = decodeColor("nimbusBase", 0.08801502f, 0.3642857f, -0.4784314f, 0);
    private Color color3 = decodeColor("nimbusBase", 5.1498413E-4f, -0.45471883f, 0.31764704f, 0);
    private Color color4 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4633005f, 0.3607843f, 0);
    private Color color5 = decodeColor("nimbusBase", 0.05468172f, -0.58308274f, 0.19607842f, 0);
    private Color color6 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.54901963f, 0);
    private Color color7 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4690476f, 0.39215684f, 0);
    private Color color8 = decodeColor("nimbusBase", 5.1498413E-4f, -0.47635174f, 0.4352941f, 0);
    private Color color9 = decodeColor("nimbusBase", 0.0f, -0.05401492f, 0.05098039f, 0);
    private Color color10 = decodeColor("nimbusBase", 0.0f, -0.09303135f, 0.09411764f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public TabbedPaneTabAreaPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_ENABLED_MOUSEOVER: paintBackgroundEnabledAndMouseOver(g); break;
            case BACKGROUND_ENABLED_PRESSED: paintBackgroundEnabledAndPressed(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundEnabled(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color1);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient1(rect));
        g.fill(rect);

    }

    private void paintBackgroundDisabled(Graphics2D g) {
        rect = decodeRect2();
        g.setPaint(decodeGradient2(rect));
        g.fill(rect);

    }

    private void paintBackgroundEnabledAndMouseOver(Graphics2D g) {
        rect = decodeRect2();
        g.setPaint(decodeGradient3(rect));
        g.fill(rect);

    }

    private void paintBackgroundEnabledAndPressed(Graphics2D g) {
        rect = decodeRect2();
        g.setPaint(decodeGradient4(rect));
        g.fill(rect);

    }



    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(0.0f), //x
                         decodeY(1.0f), //y
                         decodeX(0.0f) - decodeX(0.0f), //width
                         decodeY(1.0f) - decodeY(1.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect2() {
            rect.setRect(decodeX(0.0f), //x
                         decodeY(2.1666667f), //y
                         decodeX(3.0f) - decodeX(0.0f), //width
                         decodeY(3.0f) - decodeY(2.1666667f)); //height
        return rect;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.08387097f,0.09677419f,0.10967742f,0.43709677f,0.7645161f,0.7758064f,0.7870968f },
                new Color[] { color2,
                            decodeColor(color2,color3,0.5f),
                            color3,
                            decodeColor(color3,color4,0.5f),
                            color4,
                            decodeColor(color4,color2,0.5f),
                            color2});
    }

    private Paint decodeGradient2(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.08387097f,0.09677419f,0.10967742f,0.43709677f,0.7645161f,0.7758064f,0.7870968f },
                new Color[] { color5,
                            decodeColor(color5,color3,0.5f),
                            color3,
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
                new float[] { 0.08387097f,0.09677419f,0.10967742f,0.43709677f,0.7645161f,0.7758064f,0.7870968f },
                new Color[] { color6,
                            decodeColor(color6,color7,0.5f),
                            color7,
                            decodeColor(color7,color8,0.5f),
                            color8,
                            decodeColor(color8,color2,0.5f),
                            color2});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.08387097f,0.09677419f,0.10967742f,0.43709677f,0.7645161f,0.7758064f,0.7870968f },
                new Color[] { color2,
                            decodeColor(color2,color9,0.5f),
                            color9,
                            decodeColor(color9,color10,0.5f),
                            color10,
                            decodeColor(color10,color2,0.5f),
                            color2});
    }


}

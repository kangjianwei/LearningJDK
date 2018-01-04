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


final class SpinnerPanelSpinnerFormattedTextFieldPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of SpinnerPanelSpinnerFormattedTextFieldPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED = 1;
    static final int BACKGROUND_ENABLED = 2;
    static final int BACKGROUND_FOCUSED = 3;
    static final int BACKGROUND_SELECTED = 4;
    static final int BACKGROUND_SELECTED_FOCUSED = 5;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of SpinnerPanelSpinnerFormattedTextFieldPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", -0.6111111f, -0.110526316f, -0.74509805f, -237);
    private Color color2 = decodeColor("nimbusBlueGrey", -0.006944418f, -0.07187897f, 0.06666666f, 0);
    private Color color3 = decodeColor("nimbusBlueGrey", 0.007936537f, -0.07703349f, 0.0745098f, 0);
    private Color color4 = decodeColor("nimbusBlueGrey", 0.007936537f, -0.07968931f, 0.14509803f, 0);
    private Color color5 = decodeColor("nimbusBlueGrey", 0.007936537f, -0.07856284f, 0.11372548f, 0);
    private Color color6 = decodeColor("nimbusBase", 0.040395975f, -0.60315615f, 0.29411763f, 0);
    private Color color7 = decodeColor("nimbusBase", 0.016586483f, -0.6051466f, 0.3490196f, 0);
    private Color color8 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.0965403f, -0.18431371f, 0);
    private Color color9 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.1048766f, -0.08f, 0);
    private Color color10 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.105624355f, 0.054901958f, 0);
    private Color color11 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color12 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.105344966f, 0.011764705f, 0);
    private Color color13 = decodeColor("nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
    private Color color14 = decodeColor("nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
    private Color color15 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.1048766f, -0.05098039f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public SpinnerPanelSpinnerFormattedTextFieldPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_SELECTED: paintBackgroundSelected(g); break;
            case BACKGROUND_SELECTED_FOCUSED: paintBackgroundSelectedAndFocused(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundDisabled(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color1);
        g.fill(rect);
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
        g.setPaint(color7);
        g.fill(rect);

    }

    private void paintBackgroundEnabled(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color1);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient3(rect));
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(decodeGradient4(rect));
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color12);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color13);
        g.fill(rect);

    }

    private void paintBackgroundFocused(Graphics2D g) {
        rect = decodeRect6();
        g.setPaint(color14);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient5(rect));
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(decodeGradient4(rect));
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color12);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color13);
        g.fill(rect);

    }

    private void paintBackgroundSelected(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color1);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient3(rect));
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(decodeGradient4(rect));
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color12);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color13);
        g.fill(rect);

    }

    private void paintBackgroundSelectedAndFocused(Graphics2D g) {
        rect = decodeRect6();
        g.setPaint(color14);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient5(rect));
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(decodeGradient4(rect));
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color12);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color13);
        g.fill(rect);

    }



    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(0.6666667f), //x
                         decodeY(2.3333333f), //y
                         decodeX(3.0f) - decodeX(0.6666667f), //width
                         decodeY(2.6666667f) - decodeY(2.3333333f)); //height
        return rect;
    }

    private Rectangle2D decodeRect2() {
            rect.setRect(decodeX(0.6666667f), //x
                         decodeY(0.4f), //y
                         decodeX(3.0f) - decodeX(0.6666667f), //width
                         decodeY(1.0f) - decodeY(0.4f)); //height
        return rect;
    }

    private Rectangle2D decodeRect3() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(0.6f), //y
                         decodeX(3.0f) - decodeX(1.0f), //width
                         decodeY(1.0f) - decodeY(0.6f)); //height
        return rect;
    }

    private Rectangle2D decodeRect4() {
            rect.setRect(decodeX(0.6666667f), //x
                         decodeY(1.0f), //y
                         decodeX(3.0f) - decodeX(0.6666667f), //width
                         decodeY(2.3333333f) - decodeY(1.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect5() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(1.0f), //y
                         decodeX(3.0f) - decodeX(1.0f), //width
                         decodeY(2.0f) - decodeY(1.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect6() {
            rect.setRect(decodeX(0.22222222f), //x
                         decodeY(0.13333334f), //y
                         decodeX(2.916668f) - decodeX(0.22222222f), //width
                         decodeY(2.75f) - decodeY(0.13333334f)); //height
        return rect;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
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
        return decodeGradient((0.5f * w) + x, (1.0f * h) + y, (0.5f * w) + x, (0.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color4,
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
                new float[] { 0.0f,0.49573863f,0.99147725f },
                new Color[] { color8,
                            decodeColor(color8,color9,0.5f),
                            color9});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.1684492f,1.0f },
                new Color[] { color10,
                            decodeColor(color10,color11,0.5f),
                            color11});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.49573863f,0.99147725f },
                new Color[] { color8,
                            decodeColor(color8,color15,0.5f),
                            color15});
    }


}

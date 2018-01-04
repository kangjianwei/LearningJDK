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


final class TableHeaderRendererPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of TableHeaderRendererPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED = 1;
    static final int BACKGROUND_ENABLED = 2;
    static final int BACKGROUND_ENABLED_FOCUSED = 3;
    static final int BACKGROUND_MOUSEOVER = 4;
    static final int BACKGROUND_PRESSED = 5;
    static final int BACKGROUND_ENABLED_SORTED = 6;
    static final int BACKGROUND_ENABLED_FOCUSED_SORTED = 7;
    static final int BACKGROUND_DISABLED_SORTED = 8;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of TableHeaderRendererPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBorder", -0.013888836f, 5.823001E-4f, -0.12941176f, 0);
    private Color color2 = decodeColor("nimbusBlueGrey", -0.01111114f, -0.08625447f, 0.062745094f, 0);
    private Color color3 = decodeColor("nimbusBlueGrey", -0.013888836f, -0.028334536f, -0.17254901f, 0);
    private Color color4 = decodeColor("nimbusBlueGrey", -0.013888836f, -0.029445238f, -0.16470587f, 0);
    private Color color5 = decodeColor("nimbusBlueGrey", -0.02020204f, -0.053531498f, 0.011764705f, 0);
    private Color color6 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.10655806f, 0.24313724f, 0);
    private Color color7 = decodeColor("nimbusBlueGrey", 0.0f, -0.08455229f, 0.1607843f, 0);
    private Color color8 = decodeColor("nimbusBlueGrey", 0.0f, -0.07016757f, 0.12941176f, 0);
    private Color color9 = decodeColor("nimbusBlueGrey", 0.0f, -0.07466974f, 0.23921567f, 0);
    private Color color10 = decodeColor("nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
    private Color color11 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.10658931f, 0.25098038f, 0);
    private Color color12 = decodeColor("nimbusBlueGrey", 0.0f, -0.08613607f, 0.21960783f, 0);
    private Color color13 = decodeColor("nimbusBlueGrey", 0.0f, -0.07333623f, 0.20392156f, 0);
    private Color color14 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color15 = decodeColor("nimbusBlueGrey", -0.00505054f, -0.05960039f, 0.10196078f, 0);
    private Color color16 = decodeColor("nimbusBlueGrey", 0.0f, -0.017742813f, 0.015686274f, 0);
    private Color color17 = decodeColor("nimbusBlueGrey", -0.0027777553f, -0.0018306673f, -0.02352941f, 0);
    private Color color18 = decodeColor("nimbusBlueGrey", 0.0055555105f, -0.020436227f, 0.12549019f, 0);
    private Color color19 = decodeColor("nimbusBase", -0.023096085f, -0.62376213f, 0.4352941f, 0);
    private Color color20 = decodeColor("nimbusBase", -0.0012707114f, -0.50901747f, 0.31764704f, 0);
    private Color color21 = decodeColor("nimbusBase", -0.002461195f, -0.47139505f, 0.2862745f, 0);
    private Color color22 = decodeColor("nimbusBase", -0.0051222444f, -0.49103343f, 0.372549f, 0);
    private Color color23 = decodeColor("nimbusBase", -8.738637E-4f, -0.49872798f, 0.3098039f, 0);
    private Color color24 = decodeColor("nimbusBase", -2.2029877E-4f, -0.4916465f, 0.37647057f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public TableHeaderRendererPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_ENABLED_FOCUSED: paintBackgroundEnabledAndFocused(g); break;
            case BACKGROUND_MOUSEOVER: paintBackgroundMouseOver(g); break;
            case BACKGROUND_PRESSED: paintBackgroundPressed(g); break;
            case BACKGROUND_ENABLED_SORTED: paintBackgroundEnabledAndSorted(g); break;
            case BACKGROUND_ENABLED_FOCUSED_SORTED: paintBackgroundEnabledAndFocusedAndSorted(g); break;
            case BACKGROUND_DISABLED_SORTED: paintBackgroundDisabledAndSorted(g); break;

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

    }

    private void paintBackgroundEnabled(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color1);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient1(rect));
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(decodeGradient2(rect));
        g.fill(rect);

    }

    private void paintBackgroundEnabledAndFocused(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color1);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient1(rect));
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(decodeGradient2(rect));
        g.fill(rect);
        path = decodePath1();
        g.setPaint(color10);
        g.fill(path);

    }

    private void paintBackgroundMouseOver(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color1);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient1(rect));
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(decodeGradient3(rect));
        g.fill(rect);

    }

    private void paintBackgroundPressed(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color1);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient1(rect));
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(decodeGradient4(rect));
        g.fill(rect);

    }

    private void paintBackgroundEnabledAndSorted(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color1);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient1(rect));
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(decodeGradient5(rect));
        g.fill(rect);

    }

    private void paintBackgroundEnabledAndFocusedAndSorted(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color1);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient1(rect));
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(decodeGradient6(rect));
        g.fill(rect);
        path = decodePath1();
        g.setPaint(color10);
        g.fill(path);

    }

    private void paintBackgroundDisabledAndSorted(Graphics2D g) {
        rect = decodeRect1();
        g.setPaint(color1);
        g.fill(rect);
        rect = decodeRect2();
        g.setPaint(decodeGradient1(rect));
        g.fill(rect);
        rect = decodeRect3();
        g.setPaint(decodeGradient2(rect));
        g.fill(rect);

    }



    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(0.0f), //x
                         decodeY(2.8f), //y
                         decodeX(3.0f) - decodeX(0.0f), //width
                         decodeY(3.0f) - decodeY(2.8f)); //height
        return rect;
    }

    private Rectangle2D decodeRect2() {
            rect.setRect(decodeX(2.8f), //x
                         decodeY(0.0f), //y
                         decodeX(3.0f) - decodeX(2.8f), //width
                         decodeY(2.8f) - decodeY(0.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect3() {
            rect.setRect(decodeX(0.0f), //x
                         decodeY(0.0f), //y
                         decodeX(2.8f) - decodeX(0.0f), //width
                         decodeY(2.8f) - decodeY(0.0f)); //height
        return rect;
    }

    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.0f));
        path.lineTo(decodeX(0.0f), decodeY(3.0f));
        path.lineTo(decodeX(3.0f), decodeY(3.0f));
        path.lineTo(decodeX(3.0f), decodeY(0.0f));
        path.lineTo(decodeX(0.24000001f), decodeY(0.0f));
        path.lineTo(decodeX(0.24000001f), decodeY(0.24000001f));
        path.lineTo(decodeX(2.7599998f), decodeY(0.24000001f));
        path.lineTo(decodeX(2.7599998f), decodeY(2.7599998f));
        path.lineTo(decodeX(0.24000001f), decodeY(2.7599998f));
        path.lineTo(decodeX(0.24000001f), decodeY(0.0f));
        path.lineTo(decodeX(0.0f), decodeY(0.0f));
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
                new float[] { 0.0f,0.14441223f,0.43703705f,0.59444445f,0.75185186f,0.8759259f,1.0f },
                new Color[] { color2,
                            decodeColor(color2,color3,0.5f),
                            color3,
                            decodeColor(color3,color4,0.5f),
                            color4,
                            decodeColor(color4,color5,0.5f),
                            color5});
    }

    private Paint decodeGradient2(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.07147767f,0.2888889f,0.5490909f,0.7037037f,0.8518518f,1.0f },
                new Color[] { color6,
                            decodeColor(color6,color7,0.5f),
                            color7,
                            decodeColor(color7,color8,0.5f),
                            color8,
                            decodeColor(color8,color9,0.5f),
                            color9});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.07147767f,0.2888889f,0.5490909f,0.7037037f,0.7919203f,0.88013697f },
                new Color[] { color11,
                            decodeColor(color11,color12,0.5f),
                            color12,
                            decodeColor(color12,color13,0.5f),
                            color13,
                            decodeColor(color13,color14,0.5f),
                            color14});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.07147767f,0.2888889f,0.5490909f,0.7037037f,0.8518518f,1.0f },
                new Color[] { color15,
                            decodeColor(color15,color16,0.5f),
                            color16,
                            decodeColor(color16,color17,0.5f),
                            color17,
                            decodeColor(color17,color18,0.5f),
                            color18});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.08049711f,0.32534248f,0.56267816f,0.7037037f,0.83986557f,0.97602737f },
                new Color[] { color19,
                            decodeColor(color19,color20,0.5f),
                            color20,
                            decodeColor(color20,color21,0.5f),
                            color21,
                            decodeColor(color21,color22,0.5f),
                            color22});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.07147767f,0.2888889f,0.5490909f,0.7037037f,0.8518518f,1.0f },
                new Color[] { color19,
                            decodeColor(color19,color23,0.5f),
                            color23,
                            decodeColor(color23,color21,0.5f),
                            color21,
                            decodeColor(color21,color24,0.5f),
                            color24});
    }


}

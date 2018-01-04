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


final class SliderTrackPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of SliderTrackPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED = 1;
    static final int BACKGROUND_ENABLED = 2;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of SliderTrackPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -245);
    private Color color2 = decodeColor("nimbusBlueGrey", 0.0055555105f, -0.061265234f, 0.05098039f, 0);
    private Color color3 = decodeColor("nimbusBlueGrey", 0.01010108f, -0.059835073f, 0.10588235f, 0);
    private Color color4 = decodeColor("nimbusBlueGrey", -0.01111114f, -0.061982628f, 0.062745094f, 0);
    private Color color5 = decodeColor("nimbusBlueGrey", -0.00505054f, -0.058639523f, 0.086274505f, 0);
    private Color color6 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -111);
    private Color color7 = decodeColor("nimbusBlueGrey", 0.0f, -0.034093194f, -0.12941176f, 0);
    private Color color8 = decodeColor("nimbusBlueGrey", 0.01111114f, -0.023821115f, -0.06666666f, 0);
    private Color color9 = decodeColor("nimbusBlueGrey", -0.008547008f, -0.03314536f, -0.086274505f, 0);
    private Color color10 = decodeColor("nimbusBlueGrey", 0.004273474f, -0.040256046f, -0.019607842f, 0);
    private Color color11 = decodeColor("nimbusBlueGrey", 0.0f, -0.03626889f, 0.04705882f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public SliderTrackPainter(PaintContext ctx, int state) {
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

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundDisabled(Graphics2D g) {
        roundRect = decodeRoundRect1();
        g.setPaint(color1);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient1(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect3();
        g.setPaint(decodeGradient2(roundRect));
        g.fill(roundRect);

    }

    private void paintBackgroundEnabled(Graphics2D g) {
        roundRect = decodeRoundRect4();
        g.setPaint(color6);
        g.fill(roundRect);
        roundRect = decodeRoundRect2();
        g.setPaint(decodeGradient3(roundRect));
        g.fill(roundRect);
        roundRect = decodeRoundRect5();
        g.setPaint(decodeGradient4(roundRect));
        g.fill(roundRect);

    }



    private RoundRectangle2D decodeRoundRect1() {
        roundRect.setRoundRect(decodeX(0.2f), //x
                               decodeY(1.6f), //y
                               decodeX(2.8f) - decodeX(0.2f), //width
                               decodeY(2.8333333f) - decodeY(1.6f), //height
                               8.705882f, 8.705882f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect2() {
        roundRect.setRoundRect(decodeX(0.0f), //x
                               decodeY(1.0f), //y
                               decodeX(3.0f) - decodeX(0.0f), //width
                               decodeY(2.0f) - decodeY(1.0f), //height
                               4.9411764f, 4.9411764f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect3() {
        roundRect.setRoundRect(decodeX(0.29411763f), //x
                               decodeY(1.2f), //y
                               decodeX(2.7058823f) - decodeX(0.29411763f), //width
                               decodeY(2.0f) - decodeY(1.2f), //height
                               4.0f, 4.0f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect4() {
        roundRect.setRoundRect(decodeX(0.2f), //x
                               decodeY(1.6f), //y
                               decodeX(2.8f) - decodeX(0.2f), //width
                               decodeY(2.1666667f) - decodeY(1.6f), //height
                               8.705882f, 8.705882f); //rounding
        return roundRect;
    }

    private RoundRectangle2D decodeRoundRect5() {
        roundRect.setRoundRect(decodeX(0.28823528f), //x
                               decodeY(1.2f), //y
                               decodeX(2.7f) - decodeX(0.28823528f), //width
                               decodeY(2.0f) - decodeY(1.2f), //height
                               4.0f, 4.0f); //rounding
        return roundRect;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.07647059f * h) + y, (0.25f * w) + x, (0.9117647f * h) + y,
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
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.13770053f,0.27540106f,0.63770056f,1.0f },
                new Color[] { color4,
                            decodeColor(color4,color5,0.5f),
                            color5,
                            decodeColor(color5,color3,0.5f),
                            color3});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.07647059f * h) + y, (0.25f * w) + x, (0.9117647f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
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
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.25f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.13770053f,0.27540106f,0.4906417f,0.7058824f },
                new Color[] { color9,
                            decodeColor(color9,color10,0.5f),
                            color10,
                            decodeColor(color10,color11,0.5f),
                            color11});
    }


}

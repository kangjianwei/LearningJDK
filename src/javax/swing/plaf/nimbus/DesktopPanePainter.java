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


final class DesktopPanePainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of DesktopPanePainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_ENABLED = 1;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of DesktopPanePainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBase", -0.004577577f, -0.12867206f, 0.007843137f, 0);
    private Color color2 = decodeColor("nimbusBase", -0.0063245893f, -0.08363098f, -0.17254904f, 0);
    private Color color3 = decodeColor("nimbusBase", -3.6883354E-4f, -0.056766927f, -0.10196081f, 0);
    private Color color4 = decodeColor("nimbusBase", -0.008954704f, -0.12645501f, -0.12549022f, 0);
    private Color color5 = new Color(255, 200, 0, 6);
    private Color color6 = decodeColor("nimbusBase", -8.028746E-5f, -0.084533215f, -0.05098042f, 0);
    private Color color7 = decodeColor("nimbusBase", -0.0052053332f, -0.12267083f, -0.09803924f, 0);
    private Color color8 = decodeColor("nimbusBase", -0.012559712f, -0.13136649f, -0.09803924f, 0);
    private Color color9 = decodeColor("nimbusBase", -0.009207249f, -0.13984653f, -0.07450983f, 0);
    private Color color10 = decodeColor("nimbusBase", -0.010750473f, -0.13571429f, -0.12549022f, 0);
    private Color color11 = decodeColor("nimbusBase", -0.008476257f, -0.1267857f, -0.109803945f, 0);
    private Color color12 = decodeColor("nimbusBase", -0.0034883022f, -0.042691052f, -0.21176472f, 0);
    private Color color13 = decodeColor("nimbusBase", -0.012613952f, -0.11610645f, -0.14901963f, 0);
    private Color color14 = decodeColor("nimbusBase", -0.0038217902f, -0.05238098f, -0.21960786f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public DesktopPanePainter(PaintContext ctx, int state) {
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

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundEnabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(decodeGradient1(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient2(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(color5);
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath5();
        g.setPaint(decodeGradient4(path));
        g.fill(path);
        path = decodePath6();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath7();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath8();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath9();
        g.setPaint(decodeGradient8(path));
        g.fill(path);

    }



    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(1.2716666f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(1.2716666460037231f, 0.0f), decodeAnchorY(2.0f, 0.5f), decodeAnchorX(1.128333330154419f, 0.0f), decodeAnchorY(1.0f, 0.0f), decodeX(1.1283333f), decodeY(1.0f));
        path.lineTo(decodeX(1.3516667f), decodeY(1.0f));
        path.lineTo(decodeX(1.5866666f), decodeY(1.5754311f));
        path.lineTo(decodeX(1.5416667f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(1.5416667461395264f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeAnchorX(1.2716666460037231f, 0.0f), decodeAnchorY(2.0f, -0.5f), decodeX(1.2716666f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(1.7883334f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(1.7883334159851074f, 0.0f), decodeAnchorY(2.0f, 0.5f), decodeAnchorX(1.653333306312561f, 0.0f), decodeAnchorY(1.7737069129943848f, 0.0f), decodeX(1.6533333f), decodeY(1.7737069f));
        path.lineTo(decodeX(2.0f), decodeY(1.1465517f));
        path.curveTo(decodeAnchorX(2.0f, 0.0f), decodeAnchorY(1.1465517282485962f, 0.0f), decodeAnchorX(2.0f, 0.0f), decodeAnchorY(2.0f, -0.5f), decodeX(2.0f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(2.0f, 0.5f), decodeAnchorY(2.0f, 0.5f), decodeAnchorX(1.7883334159851074f, 0.0f), decodeAnchorY(2.0f, -0.5f), decodeX(1.7883334f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(1.5666666f), decodeY(1.0f));
        path.lineTo(decodeX(1.5666666f), decodeY(1.5689654f));
        path.lineTo(decodeX(1.675f), decodeY(1.7715517f));
        path.curveTo(decodeAnchorX(1.6749999523162842f, 0.0f), decodeAnchorY(1.7715517282485962f, 0.0f), decodeAnchorX(1.81166672706604f, -23.5f), decodeAnchorY(1.4978448152542114f, 33.5f), decodeX(1.8116667f), decodeY(1.4978448f));
        path.curveTo(decodeAnchorX(1.81166672706604f, 23.5f), decodeAnchorY(1.4978448152542114f, -33.5f), decodeAnchorX(2.0f, 0.0f), decodeAnchorY(1.200430989265442f, 0.0f), decodeX(2.0f), decodeY(1.200431f));
        path.lineTo(decodeX(2.0f), decodeY(1.0f));
        path.lineTo(decodeX(1.5666666f), decodeY(1.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(1.3383334f), decodeY(1.0f));
        path.curveTo(decodeAnchorX(1.3383333683013916f, 0.0f), decodeAnchorY(1.0f, 0.0f), decodeAnchorX(1.441666603088379f, -21.0f), decodeAnchorY(1.3103448152542114f, -37.5f), decodeX(1.4416666f), decodeY(1.3103448f));
        path.curveTo(decodeAnchorX(1.441666603088379f, 21.0f), decodeAnchorY(1.3103448152542114f, 37.5f), decodeAnchorX(1.5733332633972168f, 0.0f), decodeAnchorY(1.5840517282485962f, 0.0f), decodeX(1.5733333f), decodeY(1.5840517f));
        path.curveTo(decodeAnchorX(1.5733332633972168f, 0.0f), decodeAnchorY(1.5840517282485962f, 0.0f), decodeAnchorX(1.6066666841506958f, 1.5f), decodeAnchorY(1.2413792610168457f, 29.5f), decodeX(1.6066667f), decodeY(1.2413793f));
        path.curveTo(decodeAnchorX(1.6066666841506958f, -1.5f), decodeAnchorY(1.2413792610168457f, -29.5f), decodeAnchorX(1.6050000190734863f, 0.0f), decodeAnchorY(1.0f, 0.0f), decodeX(1.605f), decodeY(1.0f));
        path.lineTo(decodeX(1.3383334f), decodeY(1.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath5() {
        path.reset();
        path.moveTo(decodeX(1.5683334f), decodeY(1.5797414f));
        path.curveTo(decodeAnchorX(1.568333387374878f, 0.0f), decodeAnchorY(1.579741358757019f, 0.0f), decodeAnchorX(1.5750000476837158f, 0.0f), decodeAnchorY(1.2392241954803467f, 33.0f), decodeX(1.575f), decodeY(1.2392242f));
        path.curveTo(decodeAnchorX(1.5750000476837158f, 0.0f), decodeAnchorY(1.2392241954803467f, -33.0f), decodeAnchorX(1.56166672706604f, 0.0f), decodeAnchorY(1.0f, 0.0f), decodeX(1.5616667f), decodeY(1.0f));
        path.lineTo(decodeX(2.0f), decodeY(1.0f));
        path.lineTo(decodeX(2.0f), decodeY(1.1982758f));
        path.curveTo(decodeAnchorX(2.0f, 0.0f), decodeAnchorY(1.1982758045196533f, 0.0f), decodeAnchorX(1.806666612625122f, 27.5f), decodeAnchorY(1.5043103694915771f, -38.5f), decodeX(1.8066666f), decodeY(1.5043104f));
        path.curveTo(decodeAnchorX(1.806666612625122f, -27.5f), decodeAnchorY(1.5043103694915771f, 38.5f), decodeAnchorX(1.6766667366027832f, 0.0f), decodeAnchorY(1.778017282485962f, 0.0f), decodeX(1.6766667f), decodeY(1.7780173f));
        path.lineTo(decodeX(1.5683334f), decodeY(1.5797414f));
        path.closePath();
        return path;
    }

    private Path2D decodePath6() {
        path.reset();
        path.moveTo(decodeX(1.5216666f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(1.5216666460037231f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeAnchorX(1.5550000667572021f, -2.0f), decodeAnchorY(1.778017282485962f, 22.5f), decodeX(1.5550001f), decodeY(1.7780173f));
        path.curveTo(decodeAnchorX(1.5550000667572021f, 2.0f), decodeAnchorY(1.778017282485962f, -22.5f), decodeAnchorX(1.568333387374878f, 0.0f), decodeAnchorY(1.576508641242981f, 0.0f), decodeX(1.5683334f), decodeY(1.5765086f));
        path.lineTo(decodeX(1.6775f), decodeY(1.7747846f));
        path.curveTo(decodeAnchorX(1.6775000095367432f, 0.0f), decodeAnchorY(1.7747845649719238f, 0.0f), decodeAnchorX(1.6508333683013916f, 6.0f), decodeAnchorY(1.892241358757019f, -14.0f), decodeX(1.6508334f), decodeY(1.8922414f));
        path.curveTo(decodeAnchorX(1.6508333683013916f, -6.0f), decodeAnchorY(1.892241358757019f, 14.0f), decodeAnchorX(1.6083333492279053f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeX(1.6083333f), decodeY(2.0f));
        path.lineTo(decodeX(1.5216666f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath7() {
        path.reset();
        path.moveTo(decodeX(1.6066667f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(1.6066666841506958f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeAnchorX(1.6399999856948853f, -7.0f), decodeAnchorY(1.8814654350280762f, 17.0f), decodeX(1.64f), decodeY(1.8814654f));
        path.curveTo(decodeAnchorX(1.6399999856948853f, 7.0f), decodeAnchorY(1.8814654350280762f, -17.0f), decodeAnchorX(1.6775000095367432f, 0.0f), decodeAnchorY(1.7747845649719238f, 0.0f), decodeX(1.6775f), decodeY(1.7747846f));
        path.curveTo(decodeAnchorX(1.6775000095367432f, 0.0f), decodeAnchorY(1.7747845649719238f, 0.0f), decodeAnchorX(1.7416666746139526f, -11.0f), decodeAnchorY(1.8836207389831543f, -15.0f), decodeX(1.7416667f), decodeY(1.8836207f));
        path.curveTo(decodeAnchorX(1.7416666746139526f, 11.0f), decodeAnchorY(1.8836207389831543f, 15.0f), decodeAnchorX(1.81333327293396f, 0.0f), decodeAnchorY(2.0f, -0.5f), decodeX(1.8133333f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(1.81333327293396f, 0.0f), decodeAnchorY(2.0f, 0.5f), decodeAnchorX(1.6066666841506958f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeX(1.6066667f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath8() {
        path.reset();
        path.moveTo(decodeX(1.2733333f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(1.2733333110809326f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeAnchorX(1.2633333206176758f, 5.0f), decodeAnchorY(1.659482717514038f, 37.0f), decodeX(1.2633333f), decodeY(1.6594827f));
        path.curveTo(decodeAnchorX(1.2633333206176758f, -5.0f), decodeAnchorY(1.659482717514038f, -37.0f), decodeAnchorX(1.193333387374878f, 9.0f), decodeAnchorY(1.2241379022598267f, 33.5f), decodeX(1.1933334f), decodeY(1.2241379f));
        path.curveTo(decodeAnchorX(1.193333387374878f, -9.0f), decodeAnchorY(1.2241379022598267f, -33.5f), decodeAnchorX(1.1333333253860474f, 0.0f), decodeAnchorY(1.0f, 0.0f), decodeX(1.1333333f), decodeY(1.0f));
        path.lineTo(decodeX(1.0f), decodeY(1.0f));
        path.lineTo(decodeX(1.0f), decodeY(1.6120689f));
        path.curveTo(decodeAnchorX(1.0f, 0.0f), decodeAnchorY(1.6120688915252686f, 0.0f), decodeAnchorX(1.149999976158142f, 0.0f), decodeAnchorY(2.0f, -0.5f), decodeX(1.15f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(1.149999976158142f, 0.0f), decodeAnchorY(2.0f, 0.5f), decodeAnchorX(1.2733333110809326f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeX(1.2733333f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath9() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(2.0f));
        path.lineTo(decodeX(1.0f), decodeY(1.5969827f));
        path.curveTo(decodeAnchorX(1.0f, 0.0f), decodeAnchorY(1.596982717514038f, 0.0f), decodeAnchorX(1.0733333826065063f, -10.0f), decodeAnchorY(1.7974138259887695f, -19.5f), decodeX(1.0733334f), decodeY(1.7974138f));
        path.curveTo(decodeAnchorX(1.0733333826065063f, 10.0f), decodeAnchorY(1.7974138259887695f, 19.5f), decodeAnchorX(1.1666666269302368f, 0.0f), decodeAnchorY(2.0f, -0.5f), decodeX(1.1666666f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(1.1666666269302368f, 0.0f), decodeAnchorY(2.0f, 0.5f), decodeAnchorX(1.0f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeX(1.0f), decodeY(2.0f));
        path.closePath();
        return path;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.75f * w) + x, (1.0f * h) + y,
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
        return decodeGradient((0.9567308f * w) + x, (0.06835443f * h) + y, (0.75f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color3,
                            decodeColor(color3,color4,0.5f),
                            color4});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.83536583f * w) + x, (0.9522059f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color6,
                            decodeColor(color6,color7,0.5f),
                            color7});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.8659696f * w) + x, (0.011049724f * h) + y, (0.24809887f * w) + x, (0.95027626f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color6,
                            decodeColor(color6,color8,0.5f),
                            color8});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.3511236f * w) + x, (0.09326425f * h) + y, (0.33426967f * w) + x, (0.9846154f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color9,
                            decodeColor(color9,color10,0.5f),
                            color10});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.3548387f * w) + x, (0.114285715f * h) + y, (0.48387095f * w) + x, (0.9809524f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color11,
                            decodeColor(color11,color4,0.5f),
                            color4});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.75f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color1,
                            decodeColor(color1,color12,0.5f),
                            color12});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.75f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color13,
                            decodeColor(color13,color14,0.5f),
                            color14});
    }


}

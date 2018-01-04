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


final class SliderThumbPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of SliderThumbPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_DISABLED = 1;
    static final int BACKGROUND_ENABLED = 2;
    static final int BACKGROUND_FOCUSED = 3;
    static final int BACKGROUND_FOCUSED_MOUSEOVER = 4;
    static final int BACKGROUND_FOCUSED_PRESSED = 5;
    static final int BACKGROUND_MOUSEOVER = 6;
    static final int BACKGROUND_PRESSED = 7;
    static final int BACKGROUND_ENABLED_ARROWSHAPE = 8;
    static final int BACKGROUND_DISABLED_ARROWSHAPE = 9;
    static final int BACKGROUND_MOUSEOVER_ARROWSHAPE = 10;
    static final int BACKGROUND_PRESSED_ARROWSHAPE = 11;
    static final int BACKGROUND_FOCUSED_ARROWSHAPE = 12;
    static final int BACKGROUND_FOCUSED_MOUSEOVER_ARROWSHAPE = 13;
    static final int BACKGROUND_FOCUSED_PRESSED_ARROWSHAPE = 14;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of SliderThumbPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBase", 0.021348298f, -0.5625436f, 0.25490195f, 0);
    private Color color2 = decodeColor("nimbusBase", 0.015098333f, -0.55105823f, 0.19215685f, 0);
    private Color color3 = decodeColor("nimbusBase", 0.021348298f, -0.5924243f, 0.35686272f, 0);
    private Color color4 = decodeColor("nimbusBase", 0.021348298f, -0.56722116f, 0.3098039f, 0);
    private Color color5 = decodeColor("nimbusBase", 0.021348298f, -0.56844974f, 0.32549018f, 0);
    private Color color6 = decodeColor("nimbusBlueGrey", -0.003968239f, 0.0014736876f, -0.25490198f, -156);
    private Color color7 = decodeColor("nimbusBase", 5.1498413E-4f, -0.34585923f, -0.007843137f, 0);
    private Color color8 = decodeColor("nimbusBase", -0.0017285943f, -0.11571431f, -0.25490198f, 0);
    private Color color9 = decodeColor("nimbusBase", -0.023096085f, -0.6238095f, 0.43921566f, 0);
    private Color color10 = decodeColor("nimbusBase", 5.1498413E-4f, -0.43866998f, 0.24705881f, 0);
    private Color color11 = decodeColor("nimbusBase", 5.1498413E-4f, -0.45714286f, 0.32941175f, 0);
    private Color color12 = decodeColor("nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
    private Color color13 = decodeColor("nimbusBase", -0.0038217902f, -0.15532213f, -0.14901963f, 0);
    private Color color14 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.54509807f, 0);
    private Color color15 = decodeColor("nimbusBase", 0.004681647f, -0.62780917f, 0.44313723f, 0);
    private Color color16 = decodeColor("nimbusBase", 2.9569864E-4f, -0.4653107f, 0.32549018f, 0);
    private Color color17 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4563421f, 0.32549018f, 0);
    private Color color18 = decodeColor("nimbusBase", -0.0017285943f, -0.4732143f, 0.39215684f, 0);
    private Color color19 = decodeColor("nimbusBase", 0.0015952587f, -0.04875779f, -0.18823531f, 0);
    private Color color20 = decodeColor("nimbusBase", 2.9569864E-4f, -0.44943976f, 0.25098038f, 0);
    private Color color21 = decodeColor("nimbusBase", 0.0f, 0.0f, 0.0f, 0);
    private Color color22 = decodeColor("nimbusBase", 8.9377165E-4f, -0.121094406f, 0.12156862f, 0);
    private Color color23 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -121);
    private Color color24 = new Color(150, 156, 168, 146);
    private Color color25 = decodeColor("nimbusBase", -0.0033828616f, -0.40608466f, -0.019607842f, 0);
    private Color color26 = decodeColor("nimbusBase", 5.1498413E-4f, -0.17594418f, -0.20784315f, 0);
    private Color color27 = decodeColor("nimbusBase", 0.0023007393f, -0.11332625f, -0.28627452f, 0);
    private Color color28 = decodeColor("nimbusBase", -0.023096085f, -0.62376213f, 0.4352941f, 0);
    private Color color29 = decodeColor("nimbusBase", 0.004681647f, -0.594392f, 0.39999998f, 0);
    private Color color30 = decodeColor("nimbusBase", -0.0017285943f, -0.4454704f, 0.25490195f, 0);
    private Color color31 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4625541f, 0.35686272f, 0);
    private Color color32 = decodeColor("nimbusBase", 5.1498413E-4f, -0.47442397f, 0.4235294f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public SliderThumbPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_FOCUSED_MOUSEOVER: paintBackgroundFocusedAndMouseOver(g); break;
            case BACKGROUND_FOCUSED_PRESSED: paintBackgroundFocusedAndPressed(g); break;
            case BACKGROUND_MOUSEOVER: paintBackgroundMouseOver(g); break;
            case BACKGROUND_PRESSED: paintBackgroundPressed(g); break;
            case BACKGROUND_ENABLED_ARROWSHAPE: paintBackgroundEnabledAndArrowShape(g); break;
            case BACKGROUND_DISABLED_ARROWSHAPE: paintBackgroundDisabledAndArrowShape(g); break;
            case BACKGROUND_MOUSEOVER_ARROWSHAPE: paintBackgroundMouseOverAndArrowShape(g); break;
            case BACKGROUND_PRESSED_ARROWSHAPE: paintBackgroundPressedAndArrowShape(g); break;
            case BACKGROUND_FOCUSED_ARROWSHAPE: paintBackgroundFocusedAndArrowShape(g); break;
            case BACKGROUND_FOCUSED_MOUSEOVER_ARROWSHAPE: paintBackgroundFocusedAndMouseOverAndArrowShape(g); break;
            case BACKGROUND_FOCUSED_PRESSED_ARROWSHAPE: paintBackgroundFocusedAndPressedAndArrowShape(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintBackgroundDisabled(Graphics2D g) {
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient1(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient2(ellipse));
        g.fill(ellipse);

    }

    private void paintBackgroundEnabled(Graphics2D g) {
        ellipse = decodeEllipse3();
        g.setPaint(color6);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient3(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient4(ellipse));
        g.fill(ellipse);

    }

    private void paintBackgroundFocused(Graphics2D g) {
        ellipse = decodeEllipse4();
        g.setPaint(color12);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient3(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient4(ellipse));
        g.fill(ellipse);

    }

    private void paintBackgroundFocusedAndMouseOver(Graphics2D g) {
        ellipse = decodeEllipse4();
        g.setPaint(color12);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient5(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient6(ellipse));
        g.fill(ellipse);

    }

    private void paintBackgroundFocusedAndPressed(Graphics2D g) {
        ellipse = decodeEllipse4();
        g.setPaint(color12);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient7(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient8(ellipse));
        g.fill(ellipse);

    }

    private void paintBackgroundMouseOver(Graphics2D g) {
        ellipse = decodeEllipse3();
        g.setPaint(color6);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient5(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient6(ellipse));
        g.fill(ellipse);

    }

    private void paintBackgroundPressed(Graphics2D g) {
        ellipse = decodeEllipse3();
        g.setPaint(color23);
        g.fill(ellipse);
        ellipse = decodeEllipse1();
        g.setPaint(decodeGradient7(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse2();
        g.setPaint(decodeGradient8(ellipse));
        g.fill(ellipse);

    }

    private void paintBackgroundEnabledAndArrowShape(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color24);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient9(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient10(path));
        g.fill(path);

    }

    private void paintBackgroundDisabledAndArrowShape(Graphics2D g) {
        path = decodePath2();
        g.setPaint(decodeGradient11(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient12(path));
        g.fill(path);

    }

    private void paintBackgroundMouseOverAndArrowShape(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color24);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient13(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient14(path));
        g.fill(path);

    }

    private void paintBackgroundPressedAndArrowShape(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color24);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient15(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient16(path));
        g.fill(path);

    }

    private void paintBackgroundFocusedAndArrowShape(Graphics2D g) {
        path = decodePath4();
        g.setPaint(color12);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient9(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient17(path));
        g.fill(path);

    }

    private void paintBackgroundFocusedAndMouseOverAndArrowShape(Graphics2D g) {
        path = decodePath4();
        g.setPaint(color12);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient13(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient14(path));
        g.fill(path);

    }

    private void paintBackgroundFocusedAndPressedAndArrowShape(Graphics2D g) {
        path = decodePath4();
        g.setPaint(color12);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient15(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient16(path));
        g.fill(path);

    }



    private Ellipse2D decodeEllipse1() {
        ellipse.setFrame(decodeX(0.4f), //x
                         decodeY(0.4f), //y
                         decodeX(2.6f) - decodeX(0.4f), //width
                         decodeY(2.6f) - decodeY(0.4f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse2() {
        ellipse.setFrame(decodeX(0.6f), //x
                         decodeY(0.6f), //y
                         decodeX(2.4f) - decodeX(0.6f), //width
                         decodeY(2.4f) - decodeY(0.6f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse3() {
        ellipse.setFrame(decodeX(0.4f), //x
                         decodeY(0.6f), //y
                         decodeX(2.6f) - decodeX(0.4f), //width
                         decodeY(2.8f) - decodeY(0.6f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse4() {
        ellipse.setFrame(decodeX(0.120000005f), //x
                         decodeY(0.120000005f), //y
                         decodeX(2.8799999f) - decodeX(0.120000005f), //width
                         decodeY(2.8799999f) - decodeY(0.120000005f)); //height
        return ellipse;
    }

    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(0.8166667f), decodeY(0.5007576f));
        path.curveTo(decodeAnchorX(0.8166667222976685f, 1.5643268796105616f), decodeAnchorY(0.5007575750350952f, -0.309751314021121f), decodeAnchorX(2.7925455570220947f, 0.058173584548962154f), decodeAnchorY(1.6116883754730225f, -0.46476349119779314f), decodeX(2.7925456f), decodeY(1.6116884f));
        path.curveTo(decodeAnchorX(2.7925455570220947f, -0.34086855855797005f), decodeAnchorY(1.6116883754730225f, 2.723285191092547f), decodeAnchorX(0.7006363868713379f, 4.56812791706229f), decodeAnchorY(2.7693636417388916f, -0.006014915148298883f), decodeX(0.7006364f), decodeY(2.7693636f));
        path.curveTo(decodeAnchorX(0.7006363868713379f, -3.523395559100149f), decodeAnchorY(2.7693636417388916f, 0.004639302074426865f), decodeAnchorX(0.8166667222976685f, -1.8635255186676325f), decodeAnchorY(0.5007575750350952f, 0.3689954354443423f), decodeX(0.8166667f), decodeY(0.5007576f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(0.6155303f), decodeY(2.5954547f));
        path.curveTo(decodeAnchorX(0.6155303120613098f, 0.9098089454358838f), decodeAnchorY(2.595454692840576f, 1.3154241785830862f), decodeAnchorX(2.6151516437530518f, 0.014588808096503314f), decodeAnchorY(1.611201286315918f, 0.9295520709665155f), decodeX(2.6151516f), decodeY(1.6112013f));
        path.curveTo(decodeAnchorX(2.6151516437530518f, -0.013655180248463239f), decodeAnchorY(1.611201286315918f, -0.8700642982905453f), decodeAnchorX(0.6092391610145569f, 0.9729934749047704f), decodeAnchorY(0.4071640372276306f, -1.424864396720248f), decodeX(0.60923916f), decodeY(0.40716404f));
        path.curveTo(decodeAnchorX(0.6092391610145569f, -0.7485208875763871f), decodeAnchorY(0.4071640372276306f, 1.0961437978948614f), decodeAnchorX(0.6155303120613098f, -0.7499879392488253f), decodeAnchorY(2.595454692840576f, -1.0843510320300886f), decodeX(0.6155303f), decodeY(2.5954547f));
        path.closePath();
        return path;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(0.8055606f), decodeY(0.6009697f));
        path.curveTo(decodeAnchorX(0.8055605888366699f, 0.508208945236218f), decodeAnchorY(0.600969672203064f, -0.8490880998025481f), decodeAnchorX(2.3692727088928223f, 0.0031846066137877216f), decodeAnchorY(1.613116979598999f, -0.6066882577419275f), decodeX(2.3692727f), decodeY(1.613117f));
        path.curveTo(decodeAnchorX(2.3692727088928223f, -0.0038901961210928704f), decodeAnchorY(1.613116979598999f, 0.7411076447438294f), decodeAnchorX(0.7945454716682434f, 0.38709738141524763f), decodeAnchorY(2.393272876739502f, 1.240782009971129f), decodeX(0.7945455f), decodeY(2.3932729f));
        path.curveTo(decodeAnchorX(0.7945454716682434f, -0.3863658307342148f), decodeAnchorY(2.393272876739502f, -1.2384371350947134f), decodeAnchorX(0.8055605888366699f, -0.9951540091537732f), decodeAnchorY(0.600969672203064f, 1.6626496533832493f), decodeX(0.8055606f), decodeY(0.6009697f));
        path.closePath();
        return path;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(0.60059524f), decodeY(0.11727543f));
        path.curveTo(decodeAnchorX(0.600595235824585f, 1.5643268796105612f), decodeAnchorY(0.1172754317522049f, -0.3097513140211208f), decodeAnchorX(2.7925455570220947f, 0.004405844009975013f), decodeAnchorY(1.6116883754730225f, -1.1881161542467655f), decodeX(2.7925456f), decodeY(1.6116884f));
        path.curveTo(decodeAnchorX(2.7925455570220947f, -0.007364540661274788f), decodeAnchorY(1.6116883754730225f, 1.9859826422490698f), decodeAnchorX(0.7006363868713379f, 2.7716863466452586f), decodeAnchorY(2.869363784790039f, -0.008974581987587271f), decodeX(0.7006364f), decodeY(2.8693638f));
        path.curveTo(decodeAnchorX(0.7006363868713379f, -3.75489914400509f), decodeAnchorY(2.869363784790039f, 0.012158175929172899f), decodeAnchorX(0.600595235824585f, -1.8635255186676323f), decodeAnchorY(0.1172754317522049f, 0.3689954354443423f), decodeX(0.60059524f), decodeY(0.11727543f));
        path.closePath();
        return path;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5106101f * w) + x, (-4.553649E-18f * h) + y, (0.49933687f * w) + x, (1.0039787f * h) + y,
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
        return decodeGradient((0.5023511f * w) + x, (0.0015673981f * h) + y, (0.5023511f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.21256684f,0.42513368f,0.71256685f,1.0f },
                new Color[] { color3,
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
        return decodeGradient((0.51f * w) + x, (-4.553649E-18f * h) + y, (0.51f * w) + x, (1.0039787f * h) + y,
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
        return decodeGradient((0.5f * w) + x, (0.0015673981f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.21256684f,0.42513368f,0.56149733f,0.69786096f,0.8489305f,1.0f },
                new Color[] { color9,
                            decodeColor(color9,color10,0.5f),
                            color10,
                            decodeColor(color10,color10,0.5f),
                            color10,
                            decodeColor(color10,color11,0.5f),
                            color11});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5106101f * w) + x, (-4.553649E-18f * h) + y, (0.49933687f * w) + x, (1.0039787f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color13,
                            decodeColor(color13,color14,0.5f),
                            color14});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5023511f * w) + x, (0.0015673981f * h) + y, (0.5023511f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.21256684f,0.42513368f,0.56149733f,0.69786096f,0.8489305f,1.0f },
                new Color[] { color15,
                            decodeColor(color15,color16,0.5f),
                            color16,
                            decodeColor(color16,color17,0.5f),
                            color17,
                            decodeColor(color17,color18,0.5f),
                            color18});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5106101f * w) + x, (-4.553649E-18f * h) + y, (0.49933687f * w) + x, (1.0039787f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color14,
                            decodeColor(color14,color19,0.5f),
                            color19});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5023511f * w) + x, (0.0015673981f * h) + y, (0.5023511f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.23796791f,0.47593582f,0.5360962f,0.5962567f,0.79812837f,1.0f },
                new Color[] { color20,
                            decodeColor(color20,color21,0.5f),
                            color21,
                            decodeColor(color21,color21,0.5f),
                            color21,
                            decodeColor(color21,color22,0.5f),
                            color22});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.24032257f,0.48064515f,0.7403226f,1.0f },
                new Color[] { color25,
                            decodeColor(color25,color26,0.5f),
                            color26,
                            decodeColor(color26,color27,0.5f),
                            color27});
    }

    private Paint decodeGradient10(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.061290324f,0.1016129f,0.14193548f,0.3016129f,0.46129033f,0.5983871f,0.7354839f,0.7935484f,0.8516129f },
                new Color[] { color28,
                            decodeColor(color28,color29,0.5f),
                            color29,
                            decodeColor(color29,color30,0.5f),
                            color30,
                            decodeColor(color30,color31,0.5f),
                            color31,
                            decodeColor(color31,color32,0.5f),
                            color32});
    }

    private Paint decodeGradient11(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color1,
                            decodeColor(color1,color2,0.5f),
                            color2});
    }

    private Paint decodeGradient12(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.21256684f,0.42513368f,0.71256685f,1.0f },
                new Color[] { color3,
                            decodeColor(color3,color4,0.5f),
                            color4,
                            decodeColor(color4,color5,0.5f),
                            color5});
    }

    private Paint decodeGradient13(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color13,
                            decodeColor(color13,color14,0.5f),
                            color14});
    }

    private Paint decodeGradient14(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.21256684f,0.42513368f,0.56149733f,0.69786096f,0.8489305f,1.0f },
                new Color[] { color15,
                            decodeColor(color15,color16,0.5f),
                            color16,
                            decodeColor(color16,color17,0.5f),
                            color17,
                            decodeColor(color17,color18,0.5f),
                            color18});
    }

    private Paint decodeGradient15(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color14,
                            decodeColor(color14,color19,0.5f),
                            color19});
    }

    private Paint decodeGradient16(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.23796791f,0.47593582f,0.5360962f,0.5962567f,0.79812837f,1.0f },
                new Color[] { color20,
                            decodeColor(color20,color21,0.5f),
                            color21,
                            decodeColor(color21,color21,0.5f),
                            color21,
                            decodeColor(color21,color22,0.5f),
                            color22});
    }

    private Paint decodeGradient17(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.4925773f * w) + x, (0.082019866f * h) + y, (0.4925773f * w) + x, (0.91798013f * h) + y,
                new float[] { 0.061290324f,0.1016129f,0.14193548f,0.3016129f,0.46129033f,0.5983871f,0.7354839f,0.7935484f,0.8516129f },
                new Color[] { color28,
                            decodeColor(color28,color29,0.5f),
                            color29,
                            decodeColor(color29,color30,0.5f),
                            color30,
                            decodeColor(color30,color31,0.5f),
                            color31,
                            decodeColor(color31,color32,0.5f),
                            color32});
    }


}

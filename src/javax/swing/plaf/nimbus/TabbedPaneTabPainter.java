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


final class TabbedPaneTabPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of TabbedPaneTabPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_ENABLED = 1;
    static final int BACKGROUND_ENABLED_MOUSEOVER = 2;
    static final int BACKGROUND_ENABLED_PRESSED = 3;
    static final int BACKGROUND_DISABLED = 4;
    static final int BACKGROUND_SELECTED_DISABLED = 5;
    static final int BACKGROUND_SELECTED = 6;
    static final int BACKGROUND_SELECTED_MOUSEOVER = 7;
    static final int BACKGROUND_SELECTED_PRESSED = 8;
    static final int BACKGROUND_SELECTED_FOCUSED = 9;
    static final int BACKGROUND_SELECTED_MOUSEOVER_FOCUSED = 10;
    static final int BACKGROUND_SELECTED_PRESSED_FOCUSED = 11;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of TabbedPaneTabPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBase", 0.032459438f, -0.55535716f, -0.109803945f, 0);
    private Color color2 = decodeColor("nimbusBase", 0.08801502f, 0.3642857f, -0.4784314f, 0);
    private Color color3 = decodeColor("nimbusBase", 0.08801502f, -0.63174605f, 0.43921566f, 0);
    private Color color4 = decodeColor("nimbusBase", 0.05468172f, -0.6145278f, 0.37647057f, 0);
    private Color color5 = decodeColor("nimbusBase", 0.032459438f, -0.5953556f, 0.32549018f, 0);
    private Color color6 = decodeColor("nimbusBase", 0.032459438f, -0.54616207f, -0.02352941f, 0);
    private Color color7 = decodeColor("nimbusBase", 0.08801502f, -0.6317773f, 0.4470588f, 0);
    private Color color8 = decodeColor("nimbusBase", 0.021348298f, -0.61547136f, 0.41960782f, 0);
    private Color color9 = decodeColor("nimbusBase", 0.032459438f, -0.5985242f, 0.39999998f, 0);
    private Color color10 = decodeColor("nimbusBase", 0.08801502f, 0.3642857f, -0.52156866f, 0);
    private Color color11 = decodeColor("nimbusBase", 0.027408898f, -0.5847884f, 0.2980392f, 0);
    private Color color12 = decodeColor("nimbusBase", 0.035931647f, -0.5553123f, 0.23137254f, 0);
    private Color color13 = decodeColor("nimbusBase", 0.029681683f, -0.5281874f, 0.18039215f, 0);
    private Color color14 = decodeColor("nimbusBase", 0.03801495f, -0.5456242f, 0.3215686f, 0);
    private Color color15 = decodeColor("nimbusBase", 0.032459438f, -0.59181184f, 0.25490195f, 0);
    private Color color16 = decodeColor("nimbusBase", 0.05468172f, -0.58308274f, 0.19607842f, 0);
    private Color color17 = decodeColor("nimbusBase", 0.046348333f, -0.6006266f, 0.34509802f, 0);
    private Color color18 = decodeColor("nimbusBase", 0.046348333f, -0.60015875f, 0.3333333f, 0);
    private Color color19 = decodeColor("nimbusBase", 0.004681647f, -0.6197143f, 0.43137252f, 0);
    private Color color20 = decodeColor("nimbusBase", 7.13408E-4f, -0.543609f, 0.34509802f, 0);
    private Color color21 = decodeColor("nimbusBase", -0.0020751357f, -0.45610264f, 0.2588235f, 0);
    private Color color22 = decodeColor("nimbusBase", 5.1498413E-4f, -0.43866998f, 0.24705881f, 0);
    private Color color23 = decodeColor("nimbusBase", 5.1498413E-4f, -0.44879842f, 0.29019606f, 0);
    private Color color24 = decodeColor("nimbusBase", 5.1498413E-4f, -0.08776909f, -0.2627451f, 0);
    private Color color25 = decodeColor("nimbusBase", 0.06332368f, 0.3642857f, -0.4431373f, 0);
    private Color color26 = decodeColor("nimbusBase", 0.004681647f, -0.6198413f, 0.43921566f, 0);
    private Color color27 = decodeColor("nimbusBase", -0.0022627711f, -0.5335866f, 0.372549f, 0);
    private Color color28 = decodeColor("nimbusBase", -0.0017285943f, -0.4608264f, 0.32549018f, 0);
    private Color color29 = decodeColor("nimbusBase", 5.1498413E-4f, -0.4555341f, 0.3215686f, 0);
    private Color color30 = decodeColor("nimbusBase", 5.1498413E-4f, -0.46404046f, 0.36470586f, 0);
    private Color color31 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.54901963f, 0);
    private Color color32 = decodeColor("nimbusBase", -4.2033195E-4f, -0.38050595f, 0.20392156f, 0);
    private Color color33 = decodeColor("nimbusBase", 0.0013483167f, -0.16401619f, 0.0745098f, 0);
    private Color color34 = decodeColor("nimbusBase", -0.0010001659f, -0.01599598f, 0.007843137f, 0);
    private Color color35 = decodeColor("nimbusBase", 0.0f, 0.0f, 0.0f, 0);
    private Color color36 = decodeColor("nimbusBase", 0.0018727183f, -0.038398862f, 0.035294116f, 0);
    private Color color37 = decodeColor("nimbusFocus", 0.0f, 0.0f, 0.0f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public TabbedPaneTabPainter(PaintContext ctx, int state) {
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
            case BACKGROUND_ENABLED_MOUSEOVER: paintBackgroundEnabledAndMouseOver(g); break;
            case BACKGROUND_ENABLED_PRESSED: paintBackgroundEnabledAndPressed(g); break;
            case BACKGROUND_DISABLED: paintBackgroundDisabled(g); break;
            case BACKGROUND_SELECTED_DISABLED: paintBackgroundSelectedAndDisabled(g); break;
            case BACKGROUND_SELECTED: paintBackgroundSelected(g); break;
            case BACKGROUND_SELECTED_MOUSEOVER: paintBackgroundSelectedAndMouseOver(g); break;
            case BACKGROUND_SELECTED_PRESSED: paintBackgroundSelectedAndPressed(g); break;
            case BACKGROUND_SELECTED_FOCUSED: paintBackgroundSelectedAndFocused(g); break;
            case BACKGROUND_SELECTED_MOUSEOVER_FOCUSED: paintBackgroundSelectedAndMouseOverAndFocused(g); break;
            case BACKGROUND_SELECTED_PRESSED_FOCUSED: paintBackgroundSelectedAndPressedAndFocused(g); break;

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

    }

    private void paintBackgroundEnabledAndMouseOver(Graphics2D g) {
        path = decodePath1();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient4(path));
        g.fill(path);

    }

    private void paintBackgroundEnabledAndPressed(Graphics2D g) {
        path = decodePath3();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient6(path));
        g.fill(path);

    }

    private void paintBackgroundDisabled(Graphics2D g) {
        path = decodePath5();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath6();
        g.setPaint(decodeGradient8(path));
        g.fill(path);

    }

    private void paintBackgroundSelectedAndDisabled(Graphics2D g) {
        path = decodePath7();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient9(path));
        g.fill(path);

    }

    private void paintBackgroundSelected(Graphics2D g) {
        path = decodePath7();
        g.setPaint(decodeGradient10(path));
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient9(path));
        g.fill(path);

    }

    private void paintBackgroundSelectedAndMouseOver(Graphics2D g) {
        path = decodePath8();
        g.setPaint(decodeGradient11(path));
        g.fill(path);
        path = decodePath9();
        g.setPaint(decodeGradient12(path));
        g.fill(path);

    }

    private void paintBackgroundSelectedAndPressed(Graphics2D g) {
        path = decodePath8();
        g.setPaint(decodeGradient13(path));
        g.fill(path);
        path = decodePath9();
        g.setPaint(decodeGradient14(path));
        g.fill(path);

    }

    private void paintBackgroundSelectedAndFocused(Graphics2D g) {
        path = decodePath1();
        g.setPaint(decodeGradient10(path));
        g.fill(path);
        path = decodePath10();
        g.setPaint(decodeGradient9(path));
        g.fill(path);
        path = decodePath11();
        g.setPaint(color37);
        g.fill(path);

    }

    private void paintBackgroundSelectedAndMouseOverAndFocused(Graphics2D g) {
        path = decodePath12();
        g.setPaint(decodeGradient11(path));
        g.fill(path);
        path = decodePath13();
        g.setPaint(decodeGradient12(path));
        g.fill(path);
        path = decodePath14();
        g.setPaint(color37);
        g.fill(path);

    }

    private void paintBackgroundSelectedAndPressedAndFocused(Graphics2D g) {
        path = decodePath12();
        g.setPaint(decodeGradient13(path));
        g.fill(path);
        path = decodePath13();
        g.setPaint(decodeGradient14(path));
        g.fill(path);
        path = decodePath14();
        g.setPaint(color37);
        g.fill(path);

    }



    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.71428573f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(0.7142857313156128f, -3.0f), decodeAnchorX(0.7142857313156128f, -3.0f), decodeAnchorY(0.0f, 0.0f), decodeX(0.71428573f), decodeY(0.0f));
        path.curveTo(decodeAnchorX(0.7142857313156128f, 3.0f), decodeAnchorY(0.0f, 0.0f), decodeAnchorX(2.2857143878936768f, -3.0f), decodeAnchorY(0.0f, 0.0f), decodeX(2.2857144f), decodeY(0.0f));
        path.curveTo(decodeAnchorX(2.2857143878936768f, 3.0f), decodeAnchorY(0.0f, 0.0f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(0.7142857313156128f, -3.0f), decodeX(3.0f), decodeY(0.71428573f));
        path.curveTo(decodeAnchorX(3.0f, 0.0f), decodeAnchorY(0.7142857313156128f, 3.0f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(3.0f, 0.0f), decodeX(3.0f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(3.0f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(3.0f, 0.0f), decodeAnchorX(0.0f, 0.0f), decodeAnchorY(0.7142857313156128f, 3.0f), decodeX(0.0f), decodeY(0.71428573f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(0.14285715f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(0.1428571492433548f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeAnchorX(0.1428571492433548f, 0.0f), decodeAnchorY(0.8571428656578064f, 3.5555555555555536f), decodeX(0.14285715f), decodeY(0.85714287f));
        path.curveTo(decodeAnchorX(0.1428571492433548f, 0.0f), decodeAnchorY(0.8571428656578064f, -3.5555555555555536f), decodeAnchorX(0.8571428656578064f, -3.444444444444443f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeX(0.85714287f), decodeY(0.14285715f));
        path.curveTo(decodeAnchorX(0.8571428656578064f, 3.444444444444443f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeAnchorX(2.142857074737549f, -3.333333333333343f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeX(2.142857f), decodeY(0.14285715f));
        path.curveTo(decodeAnchorX(2.142857074737549f, 3.333333333333343f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeAnchorX(2.857142925262451f, 0.0f), decodeAnchorY(0.8571428656578064f, -3.277777777777777f), decodeX(2.857143f), decodeY(0.85714287f));
        path.curveTo(decodeAnchorX(2.857142925262451f, 0.0f), decodeAnchorY(0.8571428656578064f, 3.277777777777777f), decodeAnchorX(2.857142925262451f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeX(2.857143f), decodeY(2.0f));
        path.lineTo(decodeX(0.14285715f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.71428573f));
        path.curveTo(decodeAnchorX(0.0f, 0.05555555555555555f), decodeAnchorY(0.7142857313156128f, 2.6111111111111125f), decodeAnchorX(0.8333333134651184f, -2.5000000000000018f), decodeAnchorY(0.0f, 0.0f), decodeX(0.8333333f), decodeY(0.0f));
        path.curveTo(decodeAnchorX(0.8333333134651184f, 2.5000000000000018f), decodeAnchorY(0.0f, 0.0f), decodeAnchorX(2.2857143878936768f, -2.7222222222222143f), decodeAnchorY(0.0f, 0.0f), decodeX(2.2857144f), decodeY(0.0f));
        path.curveTo(decodeAnchorX(2.2857143878936768f, 2.7222222222222143f), decodeAnchorY(0.0f, 0.0f), decodeAnchorX(3.0f, -0.055555555555557135f), decodeAnchorY(0.7142857313156128f, -2.722222222222223f), decodeX(3.0f), decodeY(0.71428573f));
        path.curveTo(decodeAnchorX(3.0f, 0.055555555555557135f), decodeAnchorY(0.7142857313156128f, 2.722222222222223f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(3.0f, 0.0f), decodeX(3.0f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(3.0f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(3.0f, 0.0f), decodeAnchorX(0.0f, -0.05555555555555555f), decodeAnchorY(0.7142857313156128f, -2.6111111111111125f), decodeX(0.0f), decodeY(0.71428573f));
        path.closePath();
        return path;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(0.16666667f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(0.1666666716337204f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeAnchorX(0.1666666716337204f, 0.0f), decodeAnchorY(0.8571428656578064f, 3.6666666666666643f), decodeX(0.16666667f), decodeY(0.85714287f));
        path.curveTo(decodeAnchorX(0.1666666716337204f, 0.0f), decodeAnchorY(0.8571428656578064f, -3.6666666666666643f), decodeAnchorX(1.0f, -3.5555555555555536f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeX(1.0f), decodeY(0.14285715f));
        path.curveTo(decodeAnchorX(1.0f, 3.5555555555555536f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeAnchorX(2.142857074737549f, -3.500000000000014f), decodeAnchorY(0.1428571492433548f, 0.05555555555555558f), decodeX(2.142857f), decodeY(0.14285715f));
        path.curveTo(decodeAnchorX(2.142857074737549f, 3.500000000000014f), decodeAnchorY(0.1428571492433548f, -0.05555555555555558f), decodeAnchorX(2.857142925262451f, 0.055555555555557135f), decodeAnchorY(0.8571428656578064f, -3.6666666666666643f), decodeX(2.857143f), decodeY(0.85714287f));
        path.curveTo(decodeAnchorX(2.857142925262451f, -0.055555555555557135f), decodeAnchorY(0.8571428656578064f, 3.6666666666666643f), decodeAnchorX(2.857142925262451f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeX(2.857143f), decodeY(2.0f));
        path.lineTo(decodeX(0.16666667f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath5() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.8333333f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(0.8333333134651184f, -3.0f), decodeAnchorX(0.7142857313156128f, -3.0f), decodeAnchorY(0.0f, 0.0f), decodeX(0.71428573f), decodeY(0.0f));
        path.curveTo(decodeAnchorX(0.7142857313156128f, 3.0f), decodeAnchorY(0.0f, 0.0f), decodeAnchorX(2.2857143878936768f, -3.0f), decodeAnchorY(0.0f, 0.0f), decodeX(2.2857144f), decodeY(0.0f));
        path.curveTo(decodeAnchorX(2.2857143878936768f, 3.0f), decodeAnchorY(0.0f, 0.0f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(0.8333333134651184f, -3.0f), decodeX(3.0f), decodeY(0.8333333f));
        path.curveTo(decodeAnchorX(3.0f, 0.0f), decodeAnchorY(0.8333333134651184f, 3.0f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(3.0f, 0.0f), decodeX(3.0f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(3.0f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(3.0f, 0.0f), decodeAnchorX(0.0f, 0.0f), decodeAnchorY(0.8333333134651184f, 3.0f), decodeX(0.0f), decodeY(0.8333333f));
        path.closePath();
        return path;
    }

    private Path2D decodePath6() {
        path.reset();
        path.moveTo(decodeX(0.14285715f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(0.1428571492433548f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeAnchorX(0.1428571492433548f, 0.0f), decodeAnchorY(1.0f, 3.5555555555555536f), decodeX(0.14285715f), decodeY(1.0f));
        path.curveTo(decodeAnchorX(0.1428571492433548f, 0.0f), decodeAnchorY(1.0f, -3.5555555555555536f), decodeAnchorX(0.8571428656578064f, -3.444444444444443f), decodeAnchorY(0.1666666716337204f, 0.0f), decodeX(0.85714287f), decodeY(0.16666667f));
        path.curveTo(decodeAnchorX(0.8571428656578064f, 3.444444444444443f), decodeAnchorY(0.1666666716337204f, 0.0f), decodeAnchorX(2.142857074737549f, -3.333333333333343f), decodeAnchorY(0.1666666716337204f, 0.0f), decodeX(2.142857f), decodeY(0.16666667f));
        path.curveTo(decodeAnchorX(2.142857074737549f, 3.333333333333343f), decodeAnchorY(0.1666666716337204f, 0.0f), decodeAnchorX(2.857142925262451f, 0.0f), decodeAnchorY(1.0f, -3.277777777777777f), decodeX(2.857143f), decodeY(1.0f));
        path.curveTo(decodeAnchorX(2.857142925262451f, 0.0f), decodeAnchorY(1.0f, 3.277777777777777f), decodeAnchorX(2.857142925262451f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeX(2.857143f), decodeY(2.0f));
        path.lineTo(decodeX(0.14285715f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath7() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.71428573f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(0.7142857313156128f, -3.0f), decodeAnchorX(0.7142857313156128f, -3.0f), decodeAnchorY(0.0f, 0.0f), decodeX(0.71428573f), decodeY(0.0f));
        path.curveTo(decodeAnchorX(0.7142857313156128f, 3.0f), decodeAnchorY(0.0f, 0.0f), decodeAnchorX(2.2857143878936768f, -3.0f), decodeAnchorY(0.0f, 0.0f), decodeX(2.2857144f), decodeY(0.0f));
        path.curveTo(decodeAnchorX(2.2857143878936768f, 3.0f), decodeAnchorY(0.0f, 0.0f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(0.7142857313156128f, -3.0f), decodeX(3.0f), decodeY(0.71428573f));
        path.curveTo(decodeAnchorX(3.0f, 0.0f), decodeAnchorY(0.7142857313156128f, 3.0f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeX(3.0f), decodeY(2.0f));
        path.lineTo(decodeX(0.0f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeAnchorX(0.0f, 0.0f), decodeAnchorY(0.7142857313156128f, 3.0f), decodeX(0.0f), decodeY(0.71428573f));
        path.closePath();
        return path;
    }

    private Path2D decodePath8() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.71428573f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(0.7142857313156128f, -3.0f), decodeAnchorX(0.5555555820465088f, -3.0f), decodeAnchorY(0.0f, 0.0f), decodeX(0.5555556f), decodeY(0.0f));
        path.curveTo(decodeAnchorX(0.5555555820465088f, 3.0f), decodeAnchorY(0.0f, 0.0f), decodeAnchorX(2.444444417953491f, -3.0f), decodeAnchorY(0.0f, 0.0f), decodeX(2.4444444f), decodeY(0.0f));
        path.curveTo(decodeAnchorX(2.444444417953491f, 3.0f), decodeAnchorY(0.0f, 0.0f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(0.7142857313156128f, -3.0f), decodeX(3.0f), decodeY(0.71428573f));
        path.curveTo(decodeAnchorX(3.0f, 0.0f), decodeAnchorY(0.7142857313156128f, 3.0f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeX(3.0f), decodeY(2.0f));
        path.lineTo(decodeX(0.0f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeAnchorX(0.0f, 0.0f), decodeAnchorY(0.7142857313156128f, 3.0f), decodeX(0.0f), decodeY(0.71428573f));
        path.closePath();
        return path;
    }

    private Path2D decodePath9() {
        path.reset();
        path.moveTo(decodeX(0.11111111f), decodeY(2.0f));
        path.curveTo(decodeAnchorX(0.1111111119389534f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeAnchorX(0.1111111119389534f, 0.0f), decodeAnchorY(0.8571428656578064f, 3.5555555555555536f), decodeX(0.11111111f), decodeY(0.85714287f));
        path.curveTo(decodeAnchorX(0.1111111119389534f, 0.0f), decodeAnchorY(0.8571428656578064f, -3.5555555555555536f), decodeAnchorX(0.6666666865348816f, -3.444444444444443f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeX(0.6666667f), decodeY(0.14285715f));
        path.curveTo(decodeAnchorX(0.6666666865348816f, 3.444444444444443f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeAnchorX(2.3333332538604736f, -3.333333333333343f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeX(2.3333333f), decodeY(0.14285715f));
        path.curveTo(decodeAnchorX(2.3333332538604736f, 3.333333333333343f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeAnchorX(2.8888888359069824f, 0.0f), decodeAnchorY(0.8571428656578064f, -3.277777777777777f), decodeX(2.8888888f), decodeY(0.85714287f));
        path.curveTo(decodeAnchorX(2.8888888359069824f, 0.0f), decodeAnchorY(0.8571428656578064f, 3.277777777777777f), decodeAnchorX(2.8888888359069824f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeX(2.8888888f), decodeY(2.0f));
        path.lineTo(decodeX(0.11111111f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath10() {
        path.reset();
        path.moveTo(decodeX(0.14285715f), decodeY(3.0f));
        path.curveTo(decodeAnchorX(0.1428571492433548f, 0.0f), decodeAnchorY(3.0f, 0.0f), decodeAnchorX(0.1428571492433548f, 0.0f), decodeAnchorY(0.8571428656578064f, 3.5555555555555536f), decodeX(0.14285715f), decodeY(0.85714287f));
        path.curveTo(decodeAnchorX(0.1428571492433548f, 0.0f), decodeAnchorY(0.8571428656578064f, -3.5555555555555536f), decodeAnchorX(0.8571428656578064f, -3.444444444444443f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeX(0.85714287f), decodeY(0.14285715f));
        path.curveTo(decodeAnchorX(0.8571428656578064f, 3.444444444444443f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeAnchorX(2.142857074737549f, -3.333333333333343f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeX(2.142857f), decodeY(0.14285715f));
        path.curveTo(decodeAnchorX(2.142857074737549f, 3.333333333333343f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeAnchorX(2.857142925262451f, 0.0f), decodeAnchorY(0.8571428656578064f, -3.277777777777777f), decodeX(2.857143f), decodeY(0.85714287f));
        path.curveTo(decodeAnchorX(2.857142925262451f, 0.0f), decodeAnchorY(0.8571428656578064f, 3.277777777777777f), decodeAnchorX(2.857142925262451f, 0.0f), decodeAnchorY(3.0f, 0.0f), decodeX(2.857143f), decodeY(3.0f));
        path.lineTo(decodeX(0.14285715f), decodeY(3.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath11() {
        path.reset();
        path.moveTo(decodeX(1.4638889f), decodeY(2.25f));
        path.lineTo(decodeX(1.4652778f), decodeY(2.777778f));
        path.lineTo(decodeX(0.3809524f), decodeY(2.777778f));
        path.lineTo(decodeX(0.375f), decodeY(0.88095236f));
        path.curveTo(decodeAnchorX(0.375f, 0.0f), decodeAnchorY(0.8809523582458496f, -2.2500000000000004f), decodeAnchorX(0.8452380895614624f, -1.9166666666666647f), decodeAnchorY(0.380952388048172f, 0.0f), decodeX(0.8452381f), decodeY(0.3809524f));
        path.lineTo(decodeX(2.1011903f), decodeY(0.3809524f));
        path.curveTo(decodeAnchorX(2.1011903285980225f, 2.124999999999986f), decodeAnchorY(0.380952388048172f, 0.0f), decodeAnchorX(2.6309525966644287f, 0.0f), decodeAnchorY(0.863095223903656f, -2.5833333333333317f), decodeX(2.6309526f), decodeY(0.8630952f));
        path.lineTo(decodeX(2.625f), decodeY(2.7638886f));
        path.lineTo(decodeX(1.4666667f), decodeY(2.777778f));
        path.lineTo(decodeX(1.4638889f), decodeY(2.2361114f));
        path.lineTo(decodeX(2.3869045f), decodeY(2.222222f));
        path.lineTo(decodeX(2.375f), decodeY(0.86904764f));
        path.curveTo(decodeAnchorX(2.375f, -7.105427357601002E-15f), decodeAnchorY(0.8690476417541504f, -0.9166666666666679f), decodeAnchorX(2.095238208770752f, 1.0833333333333357f), decodeAnchorY(0.6071428656578064f, -1.7763568394002505E-15f), decodeX(2.0952382f), decodeY(0.60714287f));
        path.lineTo(decodeX(0.8333334f), decodeY(0.6130952f));
        path.curveTo(decodeAnchorX(0.8333333730697632f, -1.0f), decodeAnchorY(0.613095223903656f, 0.0f), decodeAnchorX(0.625f, 0.04166666666666696f), decodeAnchorY(0.8690476417541504f, -0.9583333333333339f), decodeX(0.625f), decodeY(0.86904764f));
        path.lineTo(decodeX(0.6130952f), decodeY(2.2361114f));
        path.lineTo(decodeX(1.4638889f), decodeY(2.25f));
        path.closePath();
        return path;
    }

    private Path2D decodePath12() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.71428573f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(0.7142857313156128f, -3.0f), decodeAnchorX(0.5555555820465088f, -3.0f), decodeAnchorY(0.0f, 0.0f), decodeX(0.5555556f), decodeY(0.0f));
        path.curveTo(decodeAnchorX(0.5555555820465088f, 3.0f), decodeAnchorY(0.0f, 0.0f), decodeAnchorX(2.444444417953491f, -3.0f), decodeAnchorY(0.0f, 0.0f), decodeX(2.4444444f), decodeY(0.0f));
        path.curveTo(decodeAnchorX(2.444444417953491f, 3.0f), decodeAnchorY(0.0f, 0.0f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(0.7142857313156128f, -3.0f), decodeX(3.0f), decodeY(0.71428573f));
        path.curveTo(decodeAnchorX(3.0f, 0.0f), decodeAnchorY(0.7142857313156128f, 3.0f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(3.0f, 0.0f), decodeX(3.0f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(3.0f));
        path.curveTo(decodeAnchorX(0.0f, 0.0f), decodeAnchorY(3.0f, 0.0f), decodeAnchorX(0.0f, 0.0f), decodeAnchorY(0.7142857313156128f, 3.0f), decodeX(0.0f), decodeY(0.71428573f));
        path.closePath();
        return path;
    }

    private Path2D decodePath13() {
        path.reset();
        path.moveTo(decodeX(0.11111111f), decodeY(3.0f));
        path.curveTo(decodeAnchorX(0.1111111119389534f, 0.0f), decodeAnchorY(3.0f, 0.0f), decodeAnchorX(0.1111111119389534f, 0.0f), decodeAnchorY(0.8571428656578064f, 3.5555555555555536f), decodeX(0.11111111f), decodeY(0.85714287f));
        path.curveTo(decodeAnchorX(0.1111111119389534f, 0.0f), decodeAnchorY(0.8571428656578064f, -3.5555555555555536f), decodeAnchorX(0.6666666865348816f, -3.444444444444443f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeX(0.6666667f), decodeY(0.14285715f));
        path.curveTo(decodeAnchorX(0.6666666865348816f, 3.444444444444443f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeAnchorX(2.3333332538604736f, -3.333333333333343f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeX(2.3333333f), decodeY(0.14285715f));
        path.curveTo(decodeAnchorX(2.3333332538604736f, 3.333333333333343f), decodeAnchorY(0.1428571492433548f, 0.0f), decodeAnchorX(2.8888888359069824f, 0.0f), decodeAnchorY(0.8571428656578064f, -3.277777777777777f), decodeX(2.8888888f), decodeY(0.85714287f));
        path.curveTo(decodeAnchorX(2.8888888359069824f, 0.0f), decodeAnchorY(0.8571428656578064f, 3.277777777777777f), decodeAnchorX(2.8888888359069824f, 0.0f), decodeAnchorY(3.0f, 0.0f), decodeX(2.8888888f), decodeY(3.0f));
        path.lineTo(decodeX(0.11111111f), decodeY(3.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath14() {
        path.reset();
        path.moveTo(decodeX(1.4583333f), decodeY(2.25f));
        path.lineTo(decodeX(1.4599359f), decodeY(2.777778f));
        path.lineTo(decodeX(0.2962963f), decodeY(2.777778f));
        path.lineTo(decodeX(0.29166666f), decodeY(0.88095236f));
        path.curveTo(decodeAnchorX(0.2916666567325592f, 0.0f), decodeAnchorY(0.8809523582458496f, -2.2500000000000004f), decodeAnchorX(0.6574074029922485f, -1.9166666666666647f), decodeAnchorY(0.380952388048172f, 0.0f), decodeX(0.6574074f), decodeY(0.3809524f));
        path.lineTo(decodeX(2.3009257f), decodeY(0.3809524f));
        path.curveTo(decodeAnchorX(2.3009257316589355f, 2.124999999999986f), decodeAnchorY(0.380952388048172f, 0.0f), decodeAnchorX(2.712963104248047f, 0.0f), decodeAnchorY(0.863095223903656f, -2.5833333333333317f), decodeX(2.712963f), decodeY(0.8630952f));
        path.lineTo(decodeX(2.7083333f), decodeY(2.7638886f));
        path.lineTo(decodeX(1.4615384f), decodeY(2.777778f));
        path.lineTo(decodeX(1.4583333f), decodeY(2.2361114f));
        path.lineTo(decodeX(2.523148f), decodeY(2.222222f));
        path.lineTo(decodeX(2.5138888f), decodeY(0.86904764f));
        path.curveTo(decodeAnchorX(2.5138888359069824f, -7.105427357601002E-15f), decodeAnchorY(0.8690476417541504f, -0.9166666666666679f), decodeAnchorX(2.2962963581085205f, 1.0833333333333357f), decodeAnchorY(0.6071428656578064f, -1.7763568394002505E-15f), decodeX(2.2962964f), decodeY(0.60714287f));
        path.lineTo(decodeX(0.6481482f), decodeY(0.6130952f));
        path.curveTo(decodeAnchorX(0.6481481790542603f, -1.0f), decodeAnchorY(0.613095223903656f, 0.0f), decodeAnchorX(0.4861111044883728f, 0.04166666666666696f), decodeAnchorY(0.8690476417541504f, -0.9583333333333339f), decodeX(0.4861111f), decodeY(0.86904764f));
        path.lineTo(decodeX(0.47685182f), decodeY(2.2361114f));
        path.lineTo(decodeX(1.4583333f), decodeY(2.25f));
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
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.1f,0.2f,0.6f,1.0f },
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
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color6,
                            decodeColor(color6,color2,0.5f),
                            color2});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.1f,0.2f,0.6f,1.0f },
                new Color[] { color7,
                            decodeColor(color7,color8,0.5f),
                            color8,
                            decodeColor(color8,color9,0.5f),
                            color9});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color10,
                            decodeColor(color10,color2,0.5f),
                            color2});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.1f,0.2f,0.42096776f,0.64193547f,0.82096773f,1.0f },
                new Color[] { color11,
                            decodeColor(color11,color12,0.5f),
                            color12,
                            decodeColor(color12,color13,0.5f),
                            color13,
                            decodeColor(color13,color14,0.5f),
                            color14});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color15,
                            decodeColor(color15,color16,0.5f),
                            color16});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.1f,0.2f,0.6f,1.0f },
                new Color[] { color17,
                            decodeColor(color17,color18,0.5f),
                            color18,
                            decodeColor(color18,color5,0.5f),
                            color5});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.12419355f,0.2483871f,0.42580646f,0.6032258f,0.6854839f,0.7677419f,0.88387096f,1.0f },
                new Color[] { color19,
                            decodeColor(color19,color20,0.5f),
                            color20,
                            decodeColor(color20,color21,0.5f),
                            color21,
                            decodeColor(color21,color22,0.5f),
                            color22,
                            decodeColor(color22,color23,0.5f),
                            color23});
    }

    private Paint decodeGradient10(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color24,
                            decodeColor(color24,color2,0.5f),
                            color2});
    }

    private Paint decodeGradient11(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color25,
                            decodeColor(color25,color2,0.5f),
                            color2});
    }

    private Paint decodeGradient12(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.12419355f,0.2483871f,0.42580646f,0.6032258f,0.6854839f,0.7677419f,0.86774194f,0.9677419f },
                new Color[] { color26,
                            decodeColor(color26,color27,0.5f),
                            color27,
                            decodeColor(color27,color28,0.5f),
                            color28,
                            decodeColor(color28,color29,0.5f),
                            color29,
                            decodeColor(color29,color30,0.5f),
                            color30});
    }

    private Paint decodeGradient13(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color25,
                            decodeColor(color25,color31,0.5f),
                            color31});
    }

    private Paint decodeGradient14(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.12419355f,0.2483871f,0.42580646f,0.6032258f,0.6854839f,0.7677419f,0.8548387f,0.9419355f },
                new Color[] { color32,
                            decodeColor(color32,color33,0.5f),
                            color33,
                            decodeColor(color33,color34,0.5f),
                            color34,
                            decodeColor(color34,color35,0.5f),
                            color35,
                            decodeColor(color35,color36,0.5f),
                            color36});
    }


}

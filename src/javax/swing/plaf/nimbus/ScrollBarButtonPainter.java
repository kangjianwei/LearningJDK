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


final class ScrollBarButtonPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of ScrollBarButtonPainter to determine which region/state is being painted
    //by that instance.
    static final int FOREGROUND_ENABLED = 1;
    static final int FOREGROUND_DISABLED = 2;
    static final int FOREGROUND_MOUSEOVER = 3;
    static final int FOREGROUND_PRESSED = 4;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of ScrollBarButtonPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = new Color(255, 200, 0, 255);
    private Color color2 = decodeColor("nimbusBlueGrey", -0.01111114f, -0.07763158f, -0.1490196f, 0);
    private Color color3 = decodeColor("nimbusBlueGrey", -0.111111104f, -0.10580933f, 0.086274505f, 0);
    private Color color4 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.102261856f, 0.20392156f, 0);
    private Color color5 = decodeColor("nimbusBlueGrey", -0.039682567f, -0.079276316f, 0.13333333f, 0);
    private Color color6 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.07382907f, 0.109803915f, 0);
    private Color color7 = decodeColor("nimbusBlueGrey", -0.039682567f, -0.08241387f, 0.23137254f, 0);
    private Color color8 = decodeColor("nimbusBlueGrey", -0.055555522f, -0.08443936f, -0.29411766f, -136);
    private Color color9 = decodeColor("nimbusBlueGrey", -0.055555522f, -0.09876161f, 0.25490195f, -178);
    private Color color10 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.08878718f, -0.5647059f, 0);
    private Color color11 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.080223285f, -0.4862745f, 0);
    private Color color12 = decodeColor("nimbusBlueGrey", -0.111111104f, -0.09525914f, -0.23137254f, 0);
    private Color color13 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -165);
    private Color color14 = decodeColor("nimbusBlueGrey", -0.04444444f, -0.080223285f, -0.09803921f, 0);
    private Color color15 = decodeColor("nimbusBlueGrey", -0.6111111f, -0.110526316f, 0.10588235f, 0);
    private Color color16 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color17 = decodeColor("nimbusBlueGrey", -0.039682567f, -0.081719734f, 0.20784312f, 0);
    private Color color18 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.07677104f, 0.18431371f, 0);
    private Color color19 = decodeColor("nimbusBlueGrey", -0.04444444f, -0.080223285f, -0.09803921f, -69);
    private Color color20 = decodeColor("nimbusBlueGrey", -0.055555522f, -0.09876161f, 0.25490195f, -39);
    private Color color21 = decodeColor("nimbusBlueGrey", 0.055555582f, -0.0951417f, -0.49019608f, 0);
    private Color color22 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.086996906f, -0.4117647f, 0);
    private Color color23 = decodeColor("nimbusBlueGrey", -0.111111104f, -0.09719298f, -0.15686274f, 0);
    private Color color24 = decodeColor("nimbusBlueGrey", -0.037037015f, -0.043859646f, -0.21568626f, 0);
    private Color color25 = decodeColor("nimbusBlueGrey", -0.06349206f, -0.07309316f, -0.011764705f, 0);
    private Color color26 = decodeColor("nimbusBlueGrey", -0.048611104f, -0.07296763f, 0.09019607f, 0);
    private Color color27 = decodeColor("nimbusBlueGrey", -0.03535354f, -0.05497076f, 0.031372547f, 0);
    private Color color28 = decodeColor("nimbusBlueGrey", -0.034188032f, -0.043168806f, 0.011764705f, 0);
    private Color color29 = decodeColor("nimbusBlueGrey", -0.03535354f, -0.0600676f, 0.109803915f, 0);
    private Color color30 = decodeColor("nimbusBlueGrey", -0.037037015f, -0.043859646f, -0.21568626f, -44);
    private Color color31 = decodeColor("nimbusBlueGrey", -0.6111111f, -0.110526316f, -0.74509805f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public ScrollBarButtonPainter(PaintContext ctx, int state) {
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
            case FOREGROUND_ENABLED: paintForegroundEnabled(g); break;
            case FOREGROUND_DISABLED: paintForegroundDisabled(g); break;
            case FOREGROUND_MOUSEOVER: paintForegroundMouseOver(g); break;
            case FOREGROUND_PRESSED: paintForegroundPressed(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintForegroundEnabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color1);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient1(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient2(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath5();
        g.setPaint(color13);
        g.fill(path);

    }

    private void paintForegroundDisabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color1);
        g.fill(path);

    }

    private void paintForegroundMouseOver(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color1);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient4(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath5();
        g.setPaint(color13);
        g.fill(path);

    }

    private void paintForegroundPressed(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color1);
        g.fill(path);
        path = decodePath2();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient8(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(color31);
        g.fill(path);
        path = decodePath5();
        g.setPaint(color13);
        g.fill(path);

    }



    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(3.0f), decodeY(3.0f));
        path.lineTo(decodeX(3.0f), decodeY(3.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.0f));
        path.lineTo(decodeX(1.6956522f), decodeY(0.0f));
        path.curveTo(decodeAnchorX(1.6956522464752197f, 0.0f), decodeAnchorY(0.0f, 0.0f), decodeAnchorX(1.6956522464752197f, -0.7058823529411633f), decodeAnchorY(1.307692289352417f, -3.0294117647058822f), decodeX(1.6956522f), decodeY(1.3076923f));
        path.curveTo(decodeAnchorX(1.6956522464752197f, 0.7058823529411633f), decodeAnchorY(1.307692289352417f, 3.0294117647058822f), decodeAnchorX(1.8260869979858398f, -2.0f), decodeAnchorY(1.769230842590332f, -1.9411764705882355f), decodeX(1.826087f), decodeY(1.7692308f));
        path.curveTo(decodeAnchorX(1.8260869979858398f, 2.0f), decodeAnchorY(1.769230842590332f, 1.9411764705882355f), decodeAnchorX(3.0f, 0.0f), decodeAnchorY(2.0f, 0.0f), decodeX(3.0f), decodeY(2.0f));
        path.lineTo(decodeX(3.0f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(0.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(1.0022625f));
        path.lineTo(decodeX(0.9705882f), decodeY(1.0384616f));
        path.lineTo(decodeX(1.0409207f), decodeY(1.0791855f));
        path.lineTo(decodeX(1.0409207f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(1.0022625f));
        path.closePath();
        return path;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(1.4782609f), decodeY(1.2307693f));
        path.lineTo(decodeX(1.4782609f), decodeY(1.7692308f));
        path.lineTo(decodeX(1.1713555f), decodeY(1.5f));
        path.lineTo(decodeX(1.4782609f), decodeY(1.2307693f));
        path.closePath();
        return path;
    }

    private Path2D decodePath5() {
        path.reset();
        path.moveTo(decodeX(1.6713555f), decodeY(1.0769231f));
        path.curveTo(decodeAnchorX(1.6713554859161377f, 0.7352941176470615f), decodeAnchorY(1.076923131942749f, 0.0f), decodeAnchorX(1.718670129776001f, -0.911764705882355f), decodeAnchorY(1.4095022678375244f, -2.2058823529411784f), decodeX(1.7186701f), decodeY(1.4095023f));
        path.curveTo(decodeAnchorX(1.718670129776001f, 0.911764705882355f), decodeAnchorY(1.4095022678375244f, 2.2058823529411784f), decodeAnchorX(1.8439897298812866f, -2.3529411764705905f), decodeAnchorY(1.7941176891326904f, -1.852941176470587f), decodeX(1.8439897f), decodeY(1.7941177f));
        path.curveTo(decodeAnchorX(1.8439897298812866f, 2.3529411764705905f), decodeAnchorY(1.7941176891326904f, 1.852941176470587f), decodeAnchorX(2.5f, 0.0f), decodeAnchorY(2.2352943420410156f, 0.0f), decodeX(2.5f), decodeY(2.2352943f));
        path.lineTo(decodeX(2.3529415f), decodeY(2.8235292f));
        path.curveTo(decodeAnchorX(2.3529415130615234f, 0.0f), decodeAnchorY(2.8235292434692383f, 0.0f), decodeAnchorX(1.818414330482483f, 1.5588235294117645f), decodeAnchorY(1.8438913822174072f, 1.3823529411764675f), decodeX(1.8184143f), decodeY(1.8438914f));
        path.curveTo(decodeAnchorX(1.818414330482483f, -1.5588235294117645f), decodeAnchorY(1.8438913822174072f, -1.3823529411764675f), decodeAnchorX(1.694373369216919f, 0.7941176470588225f), decodeAnchorY(1.4841628074645996f, 1.9999999999999991f), decodeX(1.6943734f), decodeY(1.4841628f));
        path.curveTo(decodeAnchorX(1.694373369216919f, -0.7941176470588225f), decodeAnchorY(1.4841628074645996f, -1.9999999999999991f), decodeAnchorX(1.6713554859161377f, -0.7352941176470598f), decodeAnchorY(1.076923131942749f, 0.0f), decodeX(1.6713555f), decodeY(1.0769231f));
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
                new float[] { 0.0f,0.032934133f,0.065868266f,0.089820355f,0.11377245f,0.23053892f,0.3473054f,0.494012f,0.6407186f,0.78443116f,0.92814374f },
                new Color[] { color2,
                            decodeColor(color2,color3,0.5f),
                            color3,
                            decodeColor(color3,color4,0.5f),
                            color4,
                            decodeColor(color4,color5,0.5f),
                            color5,
                            decodeColor(color5,color6,0.5f),
                            color6,
                            decodeColor(color6,color7,0.5f),
                            color7});
    }

    private Paint decodeGradient2(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.0f * w) + x, (0.5f * h) + y, (0.5735294f * w) + x, (0.5f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color8,
                            decodeColor(color8,color9,0.5f),
                            color9});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.925f * w) + x, (0.9285714f * h) + y, (0.925f * w) + x, (0.004201681f * h) + y,
                new float[] { 0.0f,0.2964072f,0.5928144f,0.79341316f,0.994012f },
                new Color[] { color10,
                            decodeColor(color10,color11,0.5f),
                            color11,
                            decodeColor(color11,color12,0.5f),
                            color12});
    }

    private Paint decodeGradient4(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.032934133f,0.065868266f,0.089820355f,0.11377245f,0.23053892f,0.3473054f,0.494012f,0.6407186f,0.78443116f,0.92814374f },
                new Color[] { color14,
                            decodeColor(color14,color15,0.5f),
                            color15,
                            decodeColor(color15,color16,0.5f),
                            color16,
                            decodeColor(color16,color17,0.5f),
                            color17,
                            decodeColor(color17,color18,0.5f),
                            color18,
                            decodeColor(color18,color16,0.5f),
                            color16});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.0f * w) + x, (0.5f * h) + y, (0.5735294f * w) + x, (0.5f * h) + y,
                new float[] { 0.19518717f,0.5975936f,1.0f },
                new Color[] { color19,
                            decodeColor(color19,color20,0.5f),
                            color20});
    }

    private Paint decodeGradient6(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.925f * w) + x, (0.9285714f * h) + y, (0.925f * w) + x, (0.004201681f * h) + y,
                new float[] { 0.0f,0.2964072f,0.5928144f,0.79341316f,0.994012f },
                new Color[] { color21,
                            decodeColor(color21,color22,0.5f),
                            color22,
                            decodeColor(color22,color23,0.5f),
                            color23});
    }

    private Paint decodeGradient7(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.032934133f,0.065868266f,0.089820355f,0.11377245f,0.23053892f,0.3473054f,0.494012f,0.6407186f,0.78443116f,0.92814374f },
                new Color[] { color24,
                            decodeColor(color24,color25,0.5f),
                            color25,
                            decodeColor(color25,color26,0.5f),
                            color26,
                            decodeColor(color26,color27,0.5f),
                            color27,
                            decodeColor(color27,color28,0.5f),
                            color28,
                            decodeColor(color28,color29,0.5f),
                            color29});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.0f * w) + x, (0.5f * h) + y, (0.5735294f * w) + x, (0.5f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color30,
                            decodeColor(color30,color9,0.5f),
                            color9});
    }


}

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


final class ArrowButtonPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of ArrowButtonPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_ENABLED = 1;
    static final int FOREGROUND_DISABLED = 2;
    static final int FOREGROUND_ENABLED = 3;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of ArrowButtonPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("nimbusBase", 0.027408898f, -0.57391655f, 0.1490196f, 0);
    private Color color2 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.37254906f, 0);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public ArrowButtonPainter(PaintContext ctx, int state) {
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
            case FOREGROUND_DISABLED: paintForegroundDisabled(g); break;
            case FOREGROUND_ENABLED: paintForegroundEnabled(g); break;

        }
    }
        


    @Override
    protected final PaintContext getPaintContext() {
        return ctx;
    }

    private void paintForegroundDisabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color1);
        g.fill(path);

    }

    private void paintForegroundEnabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color2);
        g.fill(path);

    }



    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(1.8f), decodeY(1.2f));
        path.lineTo(decodeX(1.2f), decodeY(1.5f));
        path.lineTo(decodeX(1.8f), decodeY(1.8f));
        path.lineTo(decodeX(1.8f), decodeY(1.2f));
        path.closePath();
        return path;
    }




}

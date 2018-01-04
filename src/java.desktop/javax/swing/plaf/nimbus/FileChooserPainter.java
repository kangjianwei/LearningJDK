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


final class FileChooserPainter extends AbstractRegionPainter {
    //package private integers representing the available states that
    //this painter will paint. These are used when creating a new instance
    //of FileChooserPainter to determine which region/state is being painted
    //by that instance.
    static final int BACKGROUND_ENABLED = 1;
    static final int FILEICON_ENABLED = 2;
    static final int DIRECTORYICON_ENABLED = 3;
    static final int UPFOLDERICON_ENABLED = 4;
    static final int NEWFOLDERICON_ENABLED = 5;
    static final int COMPUTERICON_ENABLED = 6;
    static final int HARDDRIVEICON_ENABLED = 7;
    static final int FLOPPYDRIVEICON_ENABLED = 8;
    static final int HOMEFOLDERICON_ENABLED = 9;
    static final int DETAILSVIEWICON_ENABLED = 10;
    static final int LISTVIEWICON_ENABLED = 11;


    private int state; //refers to one of the static final ints above
    private PaintContext ctx;

    //the following 4 variables are reused during the painting code of the layers
    private Path2D path = new Path2D.Float();
    private Rectangle2D rect = new Rectangle2D.Float(0, 0, 0, 0);
    private RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
    private Ellipse2D ellipse = new Ellipse2D.Float(0, 0, 0, 0);

    //All Colors used for painting are stored here. Ideally, only those colors being used
    //by a particular instance of FileChooserPainter would be created. For the moment at least,
    //however, all are created for each instance.
    private Color color1 = decodeColor("control", 0.0f, 0.0f, 0.0f, 0);
    private Color color2 = decodeColor("nimbusBlueGrey", 0.007936537f, -0.065654516f, -0.13333333f, 0);
    private Color color3 = new Color(97, 98, 102, 255);
    private Color color4 = decodeColor("nimbusBlueGrey", -0.032679737f, -0.043332636f, 0.24705881f, 0);
    private Color color5 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, 0);
    private Color color6 = decodeColor("nimbusBase", 0.0077680945f, -0.51781034f, 0.3490196f, 0);
    private Color color7 = decodeColor("nimbusBase", 0.013940871f, -0.599277f, 0.41960782f, 0);
    private Color color8 = decodeColor("nimbusBase", 0.004681647f, -0.4198052f, 0.14117646f, 0);
    private Color color9 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, -127);
    private Color color10 = decodeColor("nimbusBlueGrey", 0.0f, 0.0f, -0.21f, -99);
    private Color color11 = decodeColor("nimbusBase", 2.9569864E-4f, -0.45978838f, 0.2980392f, 0);
    private Color color12 = decodeColor("nimbusBase", 0.0015952587f, -0.34848025f, 0.18823528f, 0);
    private Color color13 = decodeColor("nimbusBase", 0.0015952587f, -0.30844158f, 0.09803921f, 0);
    private Color color14 = decodeColor("nimbusBase", 0.0015952587f, -0.27329817f, 0.035294116f, 0);
    private Color color15 = decodeColor("nimbusBase", 0.004681647f, -0.6198413f, 0.43921566f, 0);
    private Color color16 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, -125);
    private Color color17 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, -50);
    private Color color18 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, -100);
    private Color color19 = decodeColor("nimbusBase", 0.0012094378f, -0.23571429f, -0.0784314f, 0);
    private Color color20 = decodeColor("nimbusBase", 2.9569864E-4f, -0.115166366f, -0.2627451f, 0);
    private Color color21 = decodeColor("nimbusBase", 0.0027436614f, -0.335015f, 0.011764705f, 0);
    private Color color22 = decodeColor("nimbusBase", 0.0024294257f, -0.3857143f, 0.031372547f, 0);
    private Color color23 = decodeColor("nimbusBase", 0.0018081069f, -0.3595238f, -0.13725492f, 0);
    private Color color24 = new Color(255, 200, 0, 255);
    private Color color25 = decodeColor("nimbusBase", 0.004681647f, -0.44904763f, 0.039215684f, 0);
    private Color color26 = decodeColor("nimbusBase", 0.0015952587f, -0.43718487f, -0.015686274f, 0);
    private Color color27 = decodeColor("nimbusBase", 2.9569864E-4f, -0.39212453f, -0.24313727f, 0);
    private Color color28 = decodeColor("nimbusBase", 0.004681647f, -0.6117143f, 0.43137252f, 0);
    private Color color29 = decodeColor("nimbusBase", 0.0012094378f, -0.28015873f, -0.019607842f, 0);
    private Color color30 = decodeColor("nimbusBase", 0.00254488f, -0.07049692f, -0.2784314f, 0);
    private Color color31 = decodeColor("nimbusBase", 0.0015952587f, -0.28045115f, 0.04705882f, 0);
    private Color color32 = decodeColor("nimbusBlueGrey", 0.0f, 5.847961E-4f, -0.21568626f, 0);
    private Color color33 = decodeColor("nimbusBase", -0.0061469674f, 0.3642857f, 0.14509803f, 0);
    private Color color34 = decodeColor("nimbusBase", 0.0053939223f, 0.3642857f, -0.0901961f, 0);
    private Color color35 = decodeColor("nimbusBase", 0.0f, -0.6357143f, 0.45098037f, 0);
    private Color color36 = decodeColor("nimbusBase", -0.006044388f, -0.23963585f, 0.45098037f, 0);
    private Color color37 = decodeColor("nimbusBase", -0.0063245893f, 0.01592505f, 0.4078431f, 0);
    private Color color38 = decodeColor("nimbusBlueGrey", 0.0f, -0.110526316f, 0.25490195f, -170);
    private Color color39 = decodeColor("nimbusOrange", -0.032758567f, -0.018273294f, 0.25098038f, 0);
    private Color color40 = new Color(255, 255, 255, 255);
    private Color color41 = new Color(252, 255, 92, 255);
    private Color color42 = new Color(253, 191, 4, 255);
    private Color color43 = new Color(160, 161, 163, 255);
    private Color color44 = new Color(0, 0, 0, 255);
    private Color color45 = new Color(239, 241, 243, 255);
    private Color color46 = new Color(197, 201, 205, 255);
    private Color color47 = new Color(105, 110, 118, 255);
    private Color color48 = new Color(63, 67, 72, 255);
    private Color color49 = new Color(56, 51, 25, 255);
    private Color color50 = new Color(144, 255, 0, 255);
    private Color color51 = new Color(243, 245, 246, 255);
    private Color color52 = new Color(208, 212, 216, 255);
    private Color color53 = new Color(191, 193, 194, 255);
    private Color color54 = new Color(170, 172, 175, 255);
    private Color color55 = new Color(152, 155, 158, 255);
    private Color color56 = new Color(59, 62, 66, 255);
    private Color color57 = new Color(46, 46, 46, 255);
    private Color color58 = new Color(64, 64, 64, 255);
    private Color color59 = new Color(43, 43, 43, 255);
    private Color color60 = new Color(164, 179, 206, 255);
    private Color color61 = new Color(97, 123, 170, 255);
    private Color color62 = new Color(53, 86, 146, 255);
    private Color color63 = new Color(48, 82, 144, 255);
    private Color color64 = new Color(71, 99, 150, 255);
    private Color color65 = new Color(224, 224, 224, 255);
    private Color color66 = new Color(232, 232, 232, 255);
    private Color color67 = new Color(231, 234, 237, 255);
    private Color color68 = new Color(205, 211, 215, 255);
    private Color color69 = new Color(149, 153, 156, 54);
    private Color color70 = new Color(255, 122, 101, 255);
    private Color color71 = new Color(54, 78, 122, 255);
    private Color color72 = new Color(51, 60, 70, 255);
    private Color color73 = new Color(228, 232, 237, 255);
    private Color color74 = new Color(27, 57, 87, 255);
    private Color color75 = new Color(75, 109, 137, 255);
    private Color color76 = new Color(77, 133, 185, 255);
    private Color color77 = new Color(81, 59, 7, 255);
    private Color color78 = new Color(97, 74, 18, 255);
    private Color color79 = new Color(137, 115, 60, 255);
    private Color color80 = new Color(174, 151, 91, 255);
    private Color color81 = new Color(114, 92, 13, 255);
    private Color color82 = new Color(64, 48, 0, 255);
    private Color color83 = new Color(244, 222, 143, 255);
    private Color color84 = new Color(160, 161, 162, 255);
    private Color color85 = new Color(226, 230, 233, 255);
    private Color color86 = new Color(221, 225, 230, 255);
    private Color color87 = decodeColor("nimbusBase", 0.004681647f, -0.48756614f, 0.19215685f, 0);
    private Color color88 = decodeColor("nimbusBase", 0.004681647f, -0.48399013f, 0.019607842f, 0);
    private Color color89 = decodeColor("nimbusBase", -0.0028941035f, -0.5906323f, 0.4078431f, 0);
    private Color color90 = decodeColor("nimbusBase", 0.004681647f, -0.51290727f, 0.34509802f, 0);
    private Color color91 = decodeColor("nimbusBase", 0.009583652f, -0.5642857f, 0.3843137f, 0);
    private Color color92 = decodeColor("nimbusBase", -0.0072231293f, -0.6074885f, 0.4235294f, 0);
    private Color color93 = decodeColor("nimbusBase", 7.13408E-4f, -0.52158386f, 0.17254901f, 0);
    private Color color94 = decodeColor("nimbusBase", 0.012257397f, -0.5775132f, 0.19215685f, 0);
    private Color color95 = decodeColor("nimbusBase", 0.08801502f, -0.6164835f, -0.14117649f, 0);
    private Color color96 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.5019608f, 0);
    private Color color97 = decodeColor("nimbusBase", -0.0036516786f, -0.555393f, 0.42745095f, 0);
    private Color color98 = decodeColor("nimbusBase", -0.0010654926f, -0.3634138f, 0.2862745f, 0);
    private Color color99 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.29803923f, 0);
    private Color color100 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, 0.12156862f, 0);
    private Color color101 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.54901963f, 0);
    private Color color102 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.48627454f, 0);
    private Color color103 = decodeColor("nimbusBase", -0.57865167f, -0.6357143f, -0.007843137f, 0);
    private Color color104 = decodeColor("nimbusBase", -0.0028941035f, -0.5408867f, -0.09411767f, 0);
    private Color color105 = decodeColor("nimbusBase", -0.011985004f, -0.54721874f, -0.10588238f, 0);
    private Color color106 = decodeColor("nimbusBase", -0.0022627711f, -0.4305861f, -0.0901961f, 0);
    private Color color107 = decodeColor("nimbusBase", -0.00573498f, -0.447479f, -0.21568629f, 0);
    private Color color108 = decodeColor("nimbusBase", 0.004681647f, -0.53271f, 0.36470586f, 0);
    private Color color109 = decodeColor("nimbusBase", 0.004681647f, -0.5276062f, -0.11372551f, 0);
    private Color color110 = decodeColor("nimbusBase", -8.738637E-4f, -0.5278006f, -0.0039215684f, 0);
    private Color color111 = decodeColor("nimbusBase", -0.0028941035f, -0.5338625f, -0.12549022f, 0);
    private Color color112 = decodeColor("nimbusBlueGrey", -0.03535354f, -0.008674465f, -0.32156864f, 0);
    private Color color113 = decodeColor("nimbusBlueGrey", -0.027777791f, -0.010526314f, -0.3529412f, 0);
    private Color color114 = decodeColor("nimbusBase", -0.0028941035f, -0.5234694f, -0.1647059f, 0);
    private Color color115 = decodeColor("nimbusBase", 0.004681647f, -0.53401935f, -0.086274534f, 0);
    private Color color116 = decodeColor("nimbusBase", 0.004681647f, -0.52077174f, -0.20784315f, 0);
    private Color color117 = new Color(108, 114, 120, 255);
    private Color color118 = new Color(77, 82, 87, 255);
    private Color color119 = decodeColor("nimbusBase", -0.004577577f, -0.52179027f, -0.2392157f, 0);
    private Color color120 = decodeColor("nimbusBase", -0.004577577f, -0.547479f, -0.14901963f, 0);
    private Color color121 = new Color(186, 186, 186, 50);
    private Color color122 = new Color(186, 186, 186, 40);


    //Array of current component colors, updated in each paint call
    private Object[] componentColors;

    public FileChooserPainter(PaintContext ctx, int state) {
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
            case FILEICON_ENABLED: paintfileIconEnabled(g); break;
            case DIRECTORYICON_ENABLED: paintdirectoryIconEnabled(g); break;
            case UPFOLDERICON_ENABLED: paintupFolderIconEnabled(g); break;
            case NEWFOLDERICON_ENABLED: paintnewFolderIconEnabled(g); break;
            case HARDDRIVEICON_ENABLED: painthardDriveIconEnabled(g); break;
            case FLOPPYDRIVEICON_ENABLED: paintfloppyDriveIconEnabled(g); break;
            case HOMEFOLDERICON_ENABLED: painthomeFolderIconEnabled(g); break;
            case DETAILSVIEWICON_ENABLED: paintdetailsViewIconEnabled(g); break;
            case LISTVIEWICON_ENABLED: paintlistViewIconEnabled(g); break;

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

    }

    private void paintfileIconEnabled(Graphics2D g) {
        path = decodePath1();
        g.setPaint(color2);
        g.fill(path);
        rect = decodeRect2();
        g.setPaint(color3);
        g.fill(rect);
        path = decodePath2();
        g.setPaint(decodeGradient1(path));
        g.fill(path);
        path = decodePath3();
        g.setPaint(decodeGradient2(path));
        g.fill(path);
        path = decodePath4();
        g.setPaint(color8);
        g.fill(path);
        path = decodePath5();
        g.setPaint(color9);
        g.fill(path);

    }

    private void paintdirectoryIconEnabled(Graphics2D g) {
        path = decodePath6();
        g.setPaint(color10);
        g.fill(path);
        path = decodePath7();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath8();
        g.setPaint(decodeGradient4(path));
        g.fill(path);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color17);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color18);
        g.fill(rect);
        path = decodePath9();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath10();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath11();
        g.setPaint(color24);
        g.fill(path);

    }

    private void paintupFolderIconEnabled(Graphics2D g) {
        path = decodePath12();
        g.setPaint(decodeGradient7(path));
        g.fill(path);
        path = decodePath13();
        g.setPaint(decodeGradient8(path));
        g.fill(path);
        path = decodePath14();
        g.setPaint(decodeGradient9(path));
        g.fill(path);
        path = decodePath15();
        g.setPaint(decodeGradient10(path));
        g.fill(path);
        path = decodePath16();
        g.setPaint(color32);
        g.fill(path);
        path = decodePath17();
        g.setPaint(decodeGradient11(path));
        g.fill(path);
        path = decodePath18();
        g.setPaint(color35);
        g.fill(path);
        path = decodePath19();
        g.setPaint(decodeGradient12(path));
        g.fill(path);

    }

    private void paintnewFolderIconEnabled(Graphics2D g) {
        path = decodePath6();
        g.setPaint(color10);
        g.fill(path);
        path = decodePath7();
        g.setPaint(decodeGradient3(path));
        g.fill(path);
        path = decodePath8();
        g.setPaint(decodeGradient4(path));
        g.fill(path);
        rect = decodeRect3();
        g.setPaint(color16);
        g.fill(rect);
        rect = decodeRect4();
        g.setPaint(color17);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color18);
        g.fill(rect);
        path = decodePath9();
        g.setPaint(decodeGradient5(path));
        g.fill(path);
        path = decodePath10();
        g.setPaint(decodeGradient6(path));
        g.fill(path);
        path = decodePath11();
        g.setPaint(color24);
        g.fill(path);
        path = decodePath20();
        g.setPaint(color38);
        g.fill(path);
        path = decodePath21();
        g.setPaint(color39);
        g.fill(path);
        path = decodePath22();
        g.setPaint(decodeRadial1(path));
        g.fill(path);

    }

    private void painthardDriveIconEnabled(Graphics2D g) {
        rect = decodeRect6();
        g.setPaint(color43);
        g.fill(rect);
        rect = decodeRect7();
        g.setPaint(color44);
        g.fill(rect);
        rect = decodeRect8();
        g.setPaint(decodeGradient13(rect));
        g.fill(rect);
        path = decodePath23();
        g.setPaint(decodeGradient14(path));
        g.fill(path);
        rect = decodeRect9();
        g.setPaint(color49);
        g.fill(rect);
        rect = decodeRect10();
        g.setPaint(color49);
        g.fill(rect);
        ellipse = decodeEllipse1();
        g.setPaint(color50);
        g.fill(ellipse);
        path = decodePath24();
        g.setPaint(decodeGradient15(path));
        g.fill(path);
        ellipse = decodeEllipse2();
        g.setPaint(color53);
        g.fill(ellipse);
        ellipse = decodeEllipse3();
        g.setPaint(color53);
        g.fill(ellipse);
        ellipse = decodeEllipse4();
        g.setPaint(color54);
        g.fill(ellipse);
        ellipse = decodeEllipse5();
        g.setPaint(color55);
        g.fill(ellipse);
        ellipse = decodeEllipse6();
        g.setPaint(color55);
        g.fill(ellipse);
        ellipse = decodeEllipse7();
        g.setPaint(color55);
        g.fill(ellipse);
        rect = decodeRect11();
        g.setPaint(color56);
        g.fill(rect);
        rect = decodeRect12();
        g.setPaint(color56);
        g.fill(rect);
        rect = decodeRect13();
        g.setPaint(color56);
        g.fill(rect);

    }

    private void paintfloppyDriveIconEnabled(Graphics2D g) {
        path = decodePath25();
        g.setPaint(decodeGradient16(path));
        g.fill(path);
        path = decodePath26();
        g.setPaint(decodeGradient17(path));
        g.fill(path);
        path = decodePath27();
        g.setPaint(decodeGradient18(path));
        g.fill(path);
        path = decodePath28();
        g.setPaint(decodeGradient19(path));
        g.fill(path);
        path = decodePath29();
        g.setPaint(color69);
        g.fill(path);
        rect = decodeRect14();
        g.setPaint(color70);
        g.fill(rect);
        rect = decodeRect15();
        g.setPaint(color40);
        g.fill(rect);
        rect = decodeRect16();
        g.setPaint(color67);
        g.fill(rect);
        rect = decodeRect17();
        g.setPaint(color71);
        g.fill(rect);
        rect = decodeRect18();
        g.setPaint(color44);
        g.fill(rect);

    }

    private void painthomeFolderIconEnabled(Graphics2D g) {
        path = decodePath30();
        g.setPaint(color72);
        g.fill(path);
        path = decodePath31();
        g.setPaint(color73);
        g.fill(path);
        rect = decodeRect19();
        g.setPaint(decodeGradient20(rect));
        g.fill(rect);
        rect = decodeRect20();
        g.setPaint(color76);
        g.fill(rect);
        path = decodePath32();
        g.setPaint(decodeGradient21(path));
        g.fill(path);
        rect = decodeRect21();
        g.setPaint(decodeGradient22(rect));
        g.fill(rect);
        path = decodePath33();
        g.setPaint(decodeGradient23(path));
        g.fill(path);
        path = decodePath34();
        g.setPaint(color83);
        g.fill(path);
        path = decodePath35();
        g.setPaint(decodeGradient24(path));
        g.fill(path);
        path = decodePath36();
        g.setPaint(decodeGradient25(path));
        g.fill(path);

    }

    private void paintdetailsViewIconEnabled(Graphics2D g) {
        rect = decodeRect22();
        g.setPaint(decodeGradient26(rect));
        g.fill(rect);
        rect = decodeRect23();
        g.setPaint(decodeGradient27(rect));
        g.fill(rect);
        rect = decodeRect24();
        g.setPaint(color93);
        g.fill(rect);
        rect = decodeRect5();
        g.setPaint(color93);
        g.fill(rect);
        rect = decodeRect25();
        g.setPaint(color93);
        g.fill(rect);
        rect = decodeRect26();
        g.setPaint(color94);
        g.fill(rect);
        ellipse = decodeEllipse8();
        g.setPaint(decodeGradient28(ellipse));
        g.fill(ellipse);
        ellipse = decodeEllipse9();
        g.setPaint(decodeRadial2(ellipse));
        g.fill(ellipse);
        path = decodePath37();
        g.setPaint(decodeGradient29(path));
        g.fill(path);
        path = decodePath38();
        g.setPaint(decodeGradient30(path));
        g.fill(path);
        rect = decodeRect27();
        g.setPaint(color104);
        g.fill(rect);
        rect = decodeRect28();
        g.setPaint(color105);
        g.fill(rect);
        rect = decodeRect29();
        g.setPaint(color106);
        g.fill(rect);
        rect = decodeRect30();
        g.setPaint(color107);
        g.fill(rect);

    }

    private void paintlistViewIconEnabled(Graphics2D g) {
        rect = decodeRect31();
        g.setPaint(decodeGradient26(rect));
        g.fill(rect);
        rect = decodeRect32();
        g.setPaint(decodeGradient31(rect));
        g.fill(rect);
        rect = decodeRect33();
        g.setPaint(color109);
        g.fill(rect);
        rect = decodeRect34();
        g.setPaint(decodeGradient32(rect));
        g.fill(rect);
        rect = decodeRect35();
        g.setPaint(color111);
        g.fill(rect);
        rect = decodeRect36();
        g.setPaint(color112);
        g.fill(rect);
        rect = decodeRect37();
        g.setPaint(color113);
        g.fill(rect);
        rect = decodeRect38();
        g.setPaint(decodeGradient33(rect));
        g.fill(rect);
        rect = decodeRect39();
        g.setPaint(color116);
        g.fill(rect);
        rect = decodeRect40();
        g.setPaint(decodeGradient34(rect));
        g.fill(rect);
        rect = decodeRect41();
        g.setPaint(decodeGradient35(rect));
        g.fill(rect);
        rect = decodeRect42();
        g.setPaint(color119);
        g.fill(rect);
        rect = decodeRect43();
        g.setPaint(color121);
        g.fill(rect);
        rect = decodeRect44();
        g.setPaint(color121);
        g.fill(rect);
        rect = decodeRect45();
        g.setPaint(color121);
        g.fill(rect);
        rect = decodeRect46();
        g.setPaint(color122);
        g.fill(rect);
        rect = decodeRect47();
        g.setPaint(color121);
        g.fill(rect);
        rect = decodeRect48();
        g.setPaint(color122);
        g.fill(rect);
        rect = decodeRect49();
        g.setPaint(color122);
        g.fill(rect);
        rect = decodeRect50();
        g.setPaint(color121);
        g.fill(rect);
        rect = decodeRect51();
        g.setPaint(color122);
        g.fill(rect);
        rect = decodeRect52();
        g.setPaint(color122);
        g.fill(rect);

    }



    private Rectangle2D decodeRect1() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(1.0f), //y
                         decodeX(2.0f) - decodeX(1.0f), //width
                         decodeY(2.0f) - decodeY(1.0f)); //height
        return rect;
    }

    private Path2D decodePath1() {
        path.reset();
        path.moveTo(decodeX(0.2f), decodeY(0.0f));
        path.lineTo(decodeX(0.2f), decodeY(3.0f));
        path.lineTo(decodeX(0.4f), decodeY(3.0f));
        path.lineTo(decodeX(0.4f), decodeY(0.2f));
        path.lineTo(decodeX(1.9197531f), decodeY(0.2f));
        path.lineTo(decodeX(2.6f), decodeY(0.9f));
        path.lineTo(decodeX(2.6f), decodeY(3.0f));
        path.lineTo(decodeX(2.8f), decodeY(3.0f));
        path.lineTo(decodeX(2.8f), decodeY(0.88888896f));
        path.lineTo(decodeX(1.9537036f), decodeY(0.0f));
        path.lineTo(decodeX(0.2f), decodeY(0.0f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect2() {
            rect.setRect(decodeX(0.4f), //x
                         decodeY(2.8f), //y
                         decodeX(2.6f) - decodeX(0.4f), //width
                         decodeY(3.0f) - decodeY(2.8f)); //height
        return rect;
    }

    private Path2D decodePath2() {
        path.reset();
        path.moveTo(decodeX(1.8333333f), decodeY(0.2f));
        path.lineTo(decodeX(1.8333333f), decodeY(1.0f));
        path.lineTo(decodeX(2.6f), decodeY(1.0f));
        path.lineTo(decodeX(1.8333333f), decodeY(0.2f));
        path.closePath();
        return path;
    }

    private Path2D decodePath3() {
        path.reset();
        path.moveTo(decodeX(1.8333333f), decodeY(0.2f));
        path.lineTo(decodeX(0.4f), decodeY(0.2f));
        path.lineTo(decodeX(0.4f), decodeY(2.8f));
        path.lineTo(decodeX(2.6f), decodeY(2.8f));
        path.lineTo(decodeX(2.6f), decodeY(1.0f));
        path.lineTo(decodeX(1.8333333f), decodeY(1.0f));
        path.lineTo(decodeX(1.8333333f), decodeY(0.2f));
        path.closePath();
        return path;
    }

    private Path2D decodePath4() {
        path.reset();
        path.moveTo(decodeX(1.8333333f), decodeY(0.2f));
        path.lineTo(decodeX(1.6234567f), decodeY(0.2f));
        path.lineTo(decodeX(1.6296296f), decodeY(1.2037038f));
        path.lineTo(decodeX(2.6f), decodeY(1.2006173f));
        path.lineTo(decodeX(2.6f), decodeY(1.0f));
        path.lineTo(decodeX(1.8333333f), decodeY(1.0f));
        path.lineTo(decodeX(1.8333333f), decodeY(0.2f));
        path.closePath();
        return path;
    }

    private Path2D decodePath5() {
        path.reset();
        path.moveTo(decodeX(1.8333333f), decodeY(0.4f));
        path.lineTo(decodeX(1.8333333f), decodeY(0.2f));
        path.lineTo(decodeX(0.4f), decodeY(0.2f));
        path.lineTo(decodeX(0.4f), decodeY(2.8f));
        path.lineTo(decodeX(2.6f), decodeY(2.8f));
        path.lineTo(decodeX(2.6f), decodeY(1.0f));
        path.lineTo(decodeX(2.4f), decodeY(1.0f));
        path.lineTo(decodeX(2.4f), decodeY(2.6f));
        path.lineTo(decodeX(0.6f), decodeY(2.6f));
        path.lineTo(decodeX(0.6f), decodeY(0.4f));
        path.lineTo(decodeX(1.8333333f), decodeY(0.4f));
        path.closePath();
        return path;
    }

    private Path2D decodePath6() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(2.4f));
        path.lineTo(decodeX(0.0f), decodeY(2.6f));
        path.lineTo(decodeX(0.2f), decodeY(3.0f));
        path.lineTo(decodeX(2.6f), decodeY(3.0f));
        path.lineTo(decodeX(2.8f), decodeY(2.6f));
        path.lineTo(decodeX(2.8f), decodeY(2.4f));
        path.lineTo(decodeX(0.0f), decodeY(2.4f));
        path.closePath();
        return path;
    }

    private Path2D decodePath7() {
        path.reset();
        path.moveTo(decodeX(0.6f), decodeY(2.6f));
        path.lineTo(decodeX(0.6037037f), decodeY(1.8425925f));
        path.lineTo(decodeX(0.8f), decodeY(1.0f));
        path.lineTo(decodeX(2.8f), decodeY(1.0f));
        path.lineTo(decodeX(2.8f), decodeY(1.3333334f));
        path.lineTo(decodeX(2.6f), decodeY(2.6f));
        path.lineTo(decodeX(0.6f), decodeY(2.6f));
        path.closePath();
        return path;
    }

    private Path2D decodePath8() {
        path.reset();
        path.moveTo(decodeX(0.2f), decodeY(2.6f));
        path.lineTo(decodeX(0.4f), decodeY(2.6f));
        path.lineTo(decodeX(0.40833336f), decodeY(1.8645833f));
        path.lineTo(decodeX(0.79583335f), decodeY(0.8f));
        path.lineTo(decodeX(2.4f), decodeY(0.8f));
        path.lineTo(decodeX(2.4f), decodeY(0.6f));
        path.lineTo(decodeX(1.5f), decodeY(0.6f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.4f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.2f));
        path.lineTo(decodeX(0.6f), decodeY(0.2f));
        path.lineTo(decodeX(0.6f), decodeY(0.4f));
        path.lineTo(decodeX(0.4f), decodeY(0.6f));
        path.lineTo(decodeX(0.2f), decodeY(0.6f));
        path.lineTo(decodeX(0.2f), decodeY(2.6f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect3() {
            rect.setRect(decodeX(0.2f), //x
                         decodeY(0.6f), //y
                         decodeX(0.4f) - decodeX(0.2f), //width
                         decodeY(0.8f) - decodeY(0.6f)); //height
        return rect;
    }

    private Rectangle2D decodeRect4() {
            rect.setRect(decodeX(0.6f), //x
                         decodeY(0.2f), //y
                         decodeX(1.3333334f) - decodeX(0.6f), //width
                         decodeY(0.4f) - decodeY(0.2f)); //height
        return rect;
    }

    private Rectangle2D decodeRect5() {
            rect.setRect(decodeX(1.5f), //x
                         decodeY(0.6f), //y
                         decodeX(2.4f) - decodeX(1.5f), //width
                         decodeY(0.8f) - decodeY(0.6f)); //height
        return rect;
    }

    private Path2D decodePath9() {
        path.reset();
        path.moveTo(decodeX(3.0f), decodeY(0.8f));
        path.lineTo(decodeX(3.0f), decodeY(1.0f));
        path.lineTo(decodeX(2.4f), decodeY(1.0f));
        path.lineTo(decodeX(2.4f), decodeY(0.6f));
        path.lineTo(decodeX(1.5f), decodeY(0.6f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.4f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.2f));
        path.lineTo(decodeX(0.5888889f), decodeY(0.20370372f));
        path.lineTo(decodeX(0.5962963f), decodeY(0.34814817f));
        path.lineTo(decodeX(0.34814817f), decodeY(0.6f));
        path.lineTo(decodeX(0.2f), decodeY(0.6f));
        path.lineTo(decodeX(0.2f), decodeY(2.6f));
        path.lineTo(decodeX(2.6f), decodeY(2.6f));
        path.lineTo(decodeX(2.6f), decodeY(1.3333334f));
        path.lineTo(decodeX(2.774074f), decodeY(1.1604939f));
        path.lineTo(decodeX(2.8f), decodeY(1.0f));
        path.lineTo(decodeX(3.0f), decodeY(1.0f));
        path.lineTo(decodeX(2.8925927f), decodeY(1.1882716f));
        path.lineTo(decodeX(2.8f), decodeY(1.3333334f));
        path.lineTo(decodeX(2.8f), decodeY(2.6f));
        path.lineTo(decodeX(2.6f), decodeY(2.8f));
        path.lineTo(decodeX(0.2f), decodeY(2.8f));
        path.lineTo(decodeX(0.0f), decodeY(2.6f));
        path.lineTo(decodeX(0.0f), decodeY(0.65185183f));
        path.lineTo(decodeX(0.63703704f), decodeY(0.0f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.0f));
        path.lineTo(decodeX(1.5925925f), decodeY(0.4f));
        path.lineTo(decodeX(2.4f), decodeY(0.4f));
        path.lineTo(decodeX(2.6f), decodeY(0.6f));
        path.lineTo(decodeX(2.6f), decodeY(0.8f));
        path.lineTo(decodeX(3.0f), decodeY(0.8f));
        path.closePath();
        return path;
    }

    private Path2D decodePath10() {
        path.reset();
        path.moveTo(decodeX(2.4f), decodeY(1.0f));
        path.lineTo(decodeX(2.4f), decodeY(0.8f));
        path.lineTo(decodeX(0.74814814f), decodeY(0.8f));
        path.lineTo(decodeX(0.4037037f), decodeY(1.8425925f));
        path.lineTo(decodeX(0.4f), decodeY(2.6f));
        path.lineTo(decodeX(0.6f), decodeY(2.6f));
        path.lineTo(decodeX(0.5925926f), decodeY(2.225926f));
        path.lineTo(decodeX(0.916f), decodeY(0.996f));
        path.lineTo(decodeX(2.4f), decodeY(1.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath11() {
        path.reset();
        path.moveTo(decodeX(2.2f), decodeY(2.2f));
        path.lineTo(decodeX(2.2f), decodeY(2.2f));
        path.closePath();
        return path;
    }

    private Path2D decodePath12() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(2.8f));
        path.lineTo(decodeX(0.2f), decodeY(3.0f));
        path.lineTo(decodeX(2.6f), decodeY(3.0f));
        path.lineTo(decodeX(2.8f), decodeY(2.8f));
        path.lineTo(decodeX(2.8f), decodeY(1.8333333f));
        path.lineTo(decodeX(3.0f), decodeY(1.3333334f));
        path.lineTo(decodeX(3.0f), decodeY(1.0f));
        path.lineTo(decodeX(1.5f), decodeY(1.0f));
        path.lineTo(decodeX(1.5f), decodeY(0.4f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.2f));
        path.lineTo(decodeX(0.6f), decodeY(0.2f));
        path.lineTo(decodeX(0.4f), decodeY(0.4f));
        path.lineTo(decodeX(0.4f), decodeY(0.6f));
        path.lineTo(decodeX(0.2f), decodeY(0.6f));
        path.lineTo(decodeX(0.0f), decodeY(0.8f));
        path.lineTo(decodeX(0.0f), decodeY(2.8f));
        path.closePath();
        return path;
    }

    private Path2D decodePath13() {
        path.reset();
        path.moveTo(decodeX(0.2f), decodeY(2.8f));
        path.lineTo(decodeX(0.2f), decodeY(0.8f));
        path.lineTo(decodeX(0.4f), decodeY(0.8f));
        path.lineTo(decodeX(0.6f), decodeY(0.6f));
        path.lineTo(decodeX(0.6f), decodeY(0.4f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.4f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.6f));
        path.lineTo(decodeX(1.5f), decodeY(0.6f));
        path.lineTo(decodeX(1.5f), decodeY(2.8f));
        path.lineTo(decodeX(0.2f), decodeY(2.8f));
        path.closePath();
        return path;
    }

    private Path2D decodePath14() {
        path.reset();
        path.moveTo(decodeX(0.4f), decodeY(2.0f));
        path.lineTo(decodeX(0.6f), decodeY(1.1666666f));
        path.lineTo(decodeX(0.8f), decodeY(1.0f));
        path.lineTo(decodeX(2.8f), decodeY(1.0f));
        path.lineTo(decodeX(2.8f), decodeY(2.8f));
        path.lineTo(decodeX(2.4f), decodeY(3.0f));
        path.lineTo(decodeX(0.4f), decodeY(3.0f));
        path.lineTo(decodeX(0.4f), decodeY(2.0f));
        path.closePath();
        return path;
    }

    private Path2D decodePath15() {
        path.reset();
        path.moveTo(decodeX(0.6f), decodeY(2.8f));
        path.lineTo(decodeX(0.6f), decodeY(2.0f));
        path.lineTo(decodeX(0.8f), decodeY(1.1666666f));
        path.lineTo(decodeX(2.8f), decodeY(1.1666666f));
        path.lineTo(decodeX(2.6f), decodeY(2.0f));
        path.lineTo(decodeX(2.6f), decodeY(2.8f));
        path.lineTo(decodeX(0.6f), decodeY(2.8f));
        path.closePath();
        return path;
    }

    private Path2D decodePath16() {
        path.reset();
        path.moveTo(decodeX(1.1702899f), decodeY(1.2536231f));
        path.lineTo(decodeX(1.1666666f), decodeY(1.0615941f));
        path.lineTo(decodeX(3.0f), decodeY(1.0978261f));
        path.lineTo(decodeX(2.7782607f), decodeY(1.25f));
        path.lineTo(decodeX(2.3913045f), decodeY(1.3188406f));
        path.lineTo(decodeX(2.3826087f), decodeY(1.7246377f));
        path.lineTo(decodeX(2.173913f), decodeY(1.9347827f));
        path.lineTo(decodeX(1.8695652f), decodeY(1.923913f));
        path.lineTo(decodeX(1.710145f), decodeY(1.7246377f));
        path.lineTo(decodeX(1.710145f), decodeY(1.3115941f));
        path.lineTo(decodeX(1.1702899f), decodeY(1.2536231f));
        path.closePath();
        return path;
    }

    private Path2D decodePath17() {
        path.reset();
        path.moveTo(decodeX(1.1666666f), decodeY(1.1666666f));
        path.lineTo(decodeX(1.1666666f), decodeY(0.9130435f));
        path.lineTo(decodeX(1.9456522f), decodeY(0.0f));
        path.lineTo(decodeX(2.0608697f), decodeY(0.0f));
        path.lineTo(decodeX(2.9956522f), decodeY(0.9130435f));
        path.lineTo(decodeX(3.0f), decodeY(1.1666666f));
        path.lineTo(decodeX(2.4f), decodeY(1.1666666f));
        path.lineTo(decodeX(2.4f), decodeY(1.6666667f));
        path.lineTo(decodeX(2.2f), decodeY(1.8333333f));
        path.lineTo(decodeX(1.8333333f), decodeY(1.8333333f));
        path.lineTo(decodeX(1.6666667f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.6666667f), decodeY(1.1666666f));
        path.lineTo(decodeX(1.1666666f), decodeY(1.1666666f));
        path.closePath();
        return path;
    }

    private Path2D decodePath18() {
        path.reset();
        path.moveTo(decodeX(1.2717391f), decodeY(0.9956522f));
        path.lineTo(decodeX(1.8333333f), decodeY(1.0f));
        path.lineTo(decodeX(1.8333333f), decodeY(1.6666667f));
        path.lineTo(decodeX(2.2f), decodeY(1.6666667f));
        path.lineTo(decodeX(2.2f), decodeY(1.0f));
        path.lineTo(decodeX(2.8652174f), decodeY(1.0f));
        path.lineTo(decodeX(2.0f), decodeY(0.13043478f));
        path.lineTo(decodeX(1.2717391f), decodeY(0.9956522f));
        path.closePath();
        return path;
    }

    private Path2D decodePath19() {
        path.reset();
        path.moveTo(decodeX(1.8333333f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.8333333f), decodeY(1.0f));
        path.lineTo(decodeX(1.3913044f), decodeY(1.0f));
        path.lineTo(decodeX(1.9963768f), decodeY(0.25652176f));
        path.lineTo(decodeX(2.6608696f), decodeY(1.0f));
        path.lineTo(decodeX(2.2f), decodeY(1.0f));
        path.lineTo(decodeX(2.2f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.8333333f), decodeY(1.6666667f));
        path.closePath();
        return path;
    }

    private Path2D decodePath20() {
        path.reset();
        path.moveTo(decodeX(0.22692308f), decodeY(0.061538465f));
        path.lineTo(decodeX(0.75384617f), decodeY(0.37692308f));
        path.lineTo(decodeX(0.91923076f), decodeY(0.01923077f));
        path.lineTo(decodeX(1.2532052f), decodeY(0.40769228f));
        path.lineTo(decodeX(1.7115386f), decodeY(0.13846155f));
        path.lineTo(decodeX(1.6923077f), decodeY(0.85f));
        path.lineTo(decodeX(2.169231f), decodeY(0.9115385f));
        path.lineTo(decodeX(1.7852564f), decodeY(1.3333334f));
        path.lineTo(decodeX(1.9166667f), decodeY(1.9679487f));
        path.lineTo(decodeX(1.3685898f), decodeY(1.8301282f));
        path.lineTo(decodeX(1.1314102f), decodeY(2.2115386f));
        path.lineTo(decodeX(0.63076925f), decodeY(1.8205128f));
        path.lineTo(decodeX(0.22692308f), decodeY(1.9262822f));
        path.lineTo(decodeX(0.31153846f), decodeY(1.4871795f));
        path.lineTo(decodeX(0.0f), decodeY(1.1538461f));
        path.lineTo(decodeX(0.38461536f), decodeY(0.68076926f));
        path.lineTo(decodeX(0.22692308f), decodeY(0.061538465f));
        path.closePath();
        return path;
    }

    private Path2D decodePath21() {
        path.reset();
        path.moveTo(decodeX(0.23461537f), decodeY(0.33076924f));
        path.lineTo(decodeX(0.32692307f), decodeY(0.21538463f));
        path.lineTo(decodeX(0.9653846f), decodeY(0.74615383f));
        path.lineTo(decodeX(1.0160257f), decodeY(0.01923077f));
        path.lineTo(decodeX(1.1506411f), decodeY(0.01923077f));
        path.lineTo(decodeX(1.2275641f), decodeY(0.72307694f));
        path.lineTo(decodeX(1.6987178f), decodeY(0.20769231f));
        path.lineTo(decodeX(1.8237178f), decodeY(0.37692308f));
        path.lineTo(decodeX(1.3878205f), decodeY(0.94230765f));
        path.lineTo(decodeX(1.9775641f), decodeY(1.0256411f));
        path.lineTo(decodeX(1.9839742f), decodeY(1.1474359f));
        path.lineTo(decodeX(1.4070512f), decodeY(1.2083334f));
        path.lineTo(decodeX(1.7980769f), decodeY(1.7307692f));
        path.lineTo(decodeX(1.7532051f), decodeY(1.8269231f));
        path.lineTo(decodeX(1.2211539f), decodeY(1.3365384f));
        path.lineTo(decodeX(1.1506411f), decodeY(1.9839742f));
        path.lineTo(decodeX(1.0288461f), decodeY(1.9775641f));
        path.lineTo(decodeX(0.95384616f), decodeY(1.3429488f));
        path.lineTo(decodeX(0.28846154f), decodeY(1.8012822f));
        path.lineTo(decodeX(0.20769231f), decodeY(1.7371795f));
        path.lineTo(decodeX(0.75f), decodeY(1.173077f));
        path.lineTo(decodeX(0.011538462f), decodeY(1.1634616f));
        path.lineTo(decodeX(0.015384616f), decodeY(1.0224359f));
        path.lineTo(decodeX(0.79615384f), decodeY(0.94230765f));
        path.lineTo(decodeX(0.23461537f), decodeY(0.33076924f));
        path.closePath();
        return path;
    }

    private Path2D decodePath22() {
        path.reset();
        path.moveTo(decodeX(0.58461535f), decodeY(0.6615385f));
        path.lineTo(decodeX(0.68846154f), decodeY(0.56923074f));
        path.lineTo(decodeX(0.9884615f), decodeY(0.80769235f));
        path.lineTo(decodeX(1.0352564f), decodeY(0.43076926f));
        path.lineTo(decodeX(1.1282052f), decodeY(0.43846154f));
        path.lineTo(decodeX(1.1891025f), decodeY(0.80769235f));
        path.lineTo(decodeX(1.4006411f), decodeY(0.59615386f));
        path.lineTo(decodeX(1.4967948f), decodeY(0.70384616f));
        path.lineTo(decodeX(1.3173077f), decodeY(0.9384615f));
        path.lineTo(decodeX(1.625f), decodeY(1.0256411f));
        path.lineTo(decodeX(1.6282051f), decodeY(1.1346154f));
        path.lineTo(decodeX(1.2564102f), decodeY(1.176282f));
        path.lineTo(decodeX(1.4711539f), decodeY(1.3910257f));
        path.lineTo(decodeX(1.4070512f), decodeY(1.4807693f));
        path.lineTo(decodeX(1.1858975f), decodeY(1.2724359f));
        path.lineTo(decodeX(1.1474359f), decodeY(1.6602564f));
        path.lineTo(decodeX(1.0416666f), decodeY(1.6602564f));
        path.lineTo(decodeX(0.9769231f), decodeY(1.2884616f));
        path.lineTo(decodeX(0.6923077f), decodeY(1.5f));
        path.lineTo(decodeX(0.6423077f), decodeY(1.3782052f));
        path.lineTo(decodeX(0.83076924f), decodeY(1.176282f));
        path.lineTo(decodeX(0.46923074f), decodeY(1.1474359f));
        path.lineTo(decodeX(0.48076925f), decodeY(1.0064102f));
        path.lineTo(decodeX(0.8230769f), decodeY(0.98461545f));
        path.lineTo(decodeX(0.58461535f), decodeY(0.6615385f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect6() {
            rect.setRect(decodeX(0.2f), //x
                         decodeY(0.0f), //y
                         decodeX(2.8f) - decodeX(0.2f), //width
                         decodeY(2.2f) - decodeY(0.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect7() {
            rect.setRect(decodeX(0.2f), //x
                         decodeY(2.2f), //y
                         decodeX(2.8f) - decodeX(0.2f), //width
                         decodeY(3.0f) - decodeY(2.2f)); //height
        return rect;
    }

    private Rectangle2D decodeRect8() {
            rect.setRect(decodeX(0.4f), //x
                         decodeY(0.2f), //y
                         decodeX(2.6f) - decodeX(0.4f), //width
                         decodeY(2.2f) - decodeY(0.2f)); //height
        return rect;
    }

    private Path2D decodePath23() {
        path.reset();
        path.moveTo(decodeX(0.4f), decodeY(2.2f));
        path.lineTo(decodeX(0.4f), decodeY(2.8f));
        path.lineTo(decodeX(0.6f), decodeY(2.8f));
        path.lineTo(decodeX(0.6f), decodeY(2.6f));
        path.lineTo(decodeX(2.4f), decodeY(2.6f));
        path.lineTo(decodeX(2.4f), decodeY(2.8f));
        path.lineTo(decodeX(2.6f), decodeY(2.8f));
        path.lineTo(decodeX(2.6f), decodeY(2.2f));
        path.lineTo(decodeX(0.4f), decodeY(2.2f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect9() {
            rect.setRect(decodeX(0.6f), //x
                         decodeY(2.8f), //y
                         decodeX(1.6666667f) - decodeX(0.6f), //width
                         decodeY(3.0f) - decodeY(2.8f)); //height
        return rect;
    }

    private Rectangle2D decodeRect10() {
            rect.setRect(decodeX(1.8333333f), //x
                         decodeY(2.8f), //y
                         decodeX(2.4f) - decodeX(1.8333333f), //width
                         decodeY(3.0f) - decodeY(2.8f)); //height
        return rect;
    }

    private Ellipse2D decodeEllipse1() {
        ellipse.setFrame(decodeX(0.6f), //x
                         decodeY(2.4f), //y
                         decodeX(0.8f) - decodeX(0.6f), //width
                         decodeY(2.6f) - decodeY(2.4f)); //height
        return ellipse;
    }

    private Path2D decodePath24() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(0.4f));
        path.curveTo(decodeAnchorX(1.0f, 1.0f), decodeAnchorY(0.4000000059604645f, -1.0f), decodeAnchorX(2.0f, -1.0f), decodeAnchorY(0.4000000059604645f, -1.0f), decodeX(2.0f), decodeY(0.4f));
        path.curveTo(decodeAnchorX(2.0f, 1.0f), decodeAnchorY(0.4000000059604645f, 1.0f), decodeAnchorX(2.200000047683716f, 0.0f), decodeAnchorY(1.0f, -1.0f), decodeX(2.2f), decodeY(1.0f));
        path.curveTo(decodeAnchorX(2.200000047683716f, 0.0f), decodeAnchorY(1.0f, 1.0f), decodeAnchorX(2.200000047683716f, 0.0f), decodeAnchorY(1.5f, -2.0f), decodeX(2.2f), decodeY(1.5f));
        path.curveTo(decodeAnchorX(2.200000047683716f, 0.0f), decodeAnchorY(1.5f, 2.0f), decodeAnchorX(1.6666667461395264f, 1.0f), decodeAnchorY(1.8333332538604736f, 0.0f), decodeX(1.6666667f), decodeY(1.8333333f));
        path.curveTo(decodeAnchorX(1.6666667461395264f, -1.0f), decodeAnchorY(1.8333332538604736f, 0.0f), decodeAnchorX(1.3333333730697632f, 1.0f), decodeAnchorY(1.8333332538604736f, 0.0f), decodeX(1.3333334f), decodeY(1.8333333f));
        path.curveTo(decodeAnchorX(1.3333333730697632f, -1.0f), decodeAnchorY(1.8333332538604736f, 0.0f), decodeAnchorX(0.800000011920929f, 0.0f), decodeAnchorY(1.5f, 2.0f), decodeX(0.8f), decodeY(1.5f));
        path.curveTo(decodeAnchorX(0.800000011920929f, 0.0f), decodeAnchorY(1.5f, -2.0f), decodeAnchorX(0.800000011920929f, 0.0f), decodeAnchorY(1.0f, 1.0f), decodeX(0.8f), decodeY(1.0f));
        path.curveTo(decodeAnchorX(0.800000011920929f, 0.0f), decodeAnchorY(1.0f, -1.0f), decodeAnchorX(1.0f, -1.0f), decodeAnchorY(0.4000000059604645f, 1.0f), decodeX(1.0f), decodeY(0.4f));
        path.closePath();
        return path;
    }

    private Ellipse2D decodeEllipse2() {
        ellipse.setFrame(decodeX(0.6f), //x
                         decodeY(0.2f), //y
                         decodeX(0.8f) - decodeX(0.6f), //width
                         decodeY(0.4f) - decodeY(0.2f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse3() {
        ellipse.setFrame(decodeX(2.2f), //x
                         decodeY(0.2f), //y
                         decodeX(2.4f) - decodeX(2.2f), //width
                         decodeY(0.4f) - decodeY(0.2f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse4() {
        ellipse.setFrame(decodeX(2.2f), //x
                         decodeY(1.0f), //y
                         decodeX(2.4f) - decodeX(2.2f), //width
                         decodeY(1.1666666f) - decodeY(1.0f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse5() {
        ellipse.setFrame(decodeX(2.2f), //x
                         decodeY(1.6666667f), //y
                         decodeX(2.4f) - decodeX(2.2f), //width
                         decodeY(1.8333333f) - decodeY(1.6666667f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse6() {
        ellipse.setFrame(decodeX(0.6f), //x
                         decodeY(1.6666667f), //y
                         decodeX(0.8f) - decodeX(0.6f), //width
                         decodeY(1.8333333f) - decodeY(1.6666667f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse7() {
        ellipse.setFrame(decodeX(0.6f), //x
                         decodeY(1.0f), //y
                         decodeX(0.8f) - decodeX(0.6f), //width
                         decodeY(1.1666666f) - decodeY(1.0f)); //height
        return ellipse;
    }

    private Rectangle2D decodeRect11() {
            rect.setRect(decodeX(0.8f), //x
                         decodeY(2.2f), //y
                         decodeX(1.0f) - decodeX(0.8f), //width
                         decodeY(2.6f) - decodeY(2.2f)); //height
        return rect;
    }

    private Rectangle2D decodeRect12() {
            rect.setRect(decodeX(1.1666666f), //x
                         decodeY(2.2f), //y
                         decodeX(1.3333334f) - decodeX(1.1666666f), //width
                         decodeY(2.6f) - decodeY(2.2f)); //height
        return rect;
    }

    private Rectangle2D decodeRect13() {
            rect.setRect(decodeX(1.5f), //x
                         decodeY(2.2f), //y
                         decodeX(1.6666667f) - decodeX(1.5f), //width
                         decodeY(2.6f) - decodeY(2.2f)); //height
        return rect;
    }

    private Path2D decodePath25() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(0.2f));
        path.lineTo(decodeX(0.2f), decodeY(0.0f));
        path.lineTo(decodeX(2.6f), decodeY(0.0f));
        path.lineTo(decodeX(3.0f), decodeY(0.4f));
        path.lineTo(decodeX(3.0f), decodeY(2.8f));
        path.lineTo(decodeX(2.8f), decodeY(3.0f));
        path.lineTo(decodeX(0.2f), decodeY(3.0f));
        path.lineTo(decodeX(0.0f), decodeY(2.8f));
        path.lineTo(decodeX(0.0f), decodeY(0.2f));
        path.closePath();
        return path;
    }

    private Path2D decodePath26() {
        path.reset();
        path.moveTo(decodeX(0.2f), decodeY(0.4f));
        path.lineTo(decodeX(0.4f), decodeY(0.2f));
        path.lineTo(decodeX(2.4f), decodeY(0.2f));
        path.lineTo(decodeX(2.8f), decodeY(0.6f));
        path.lineTo(decodeX(2.8f), decodeY(2.8f));
        path.lineTo(decodeX(0.2f), decodeY(2.8f));
        path.lineTo(decodeX(0.2f), decodeY(0.4f));
        path.closePath();
        return path;
    }

    private Path2D decodePath27() {
        path.reset();
        path.moveTo(decodeX(0.8f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.0f), decodeY(1.5f));
        path.lineTo(decodeX(2.0f), decodeY(1.5f));
        path.lineTo(decodeX(2.2f), decodeY(1.6666667f));
        path.lineTo(decodeX(2.2f), decodeY(2.6f));
        path.lineTo(decodeX(0.8f), decodeY(2.6f));
        path.lineTo(decodeX(0.8f), decodeY(1.6666667f));
        path.closePath();
        return path;
    }

    private Path2D decodePath28() {
        path.reset();
        path.moveTo(decodeX(1.1666666f), decodeY(0.2f));
        path.lineTo(decodeX(1.1666666f), decodeY(1.1666666f));
        path.lineTo(decodeX(2.2f), decodeY(1.1666666f));
        path.lineTo(decodeX(2.2f), decodeY(0.4f));
        path.lineTo(decodeX(2.0f), decodeY(0.4f));
        path.lineTo(decodeX(2.0f), decodeY(1.0f));
        path.lineTo(decodeX(1.6666667f), decodeY(1.0f));
        path.lineTo(decodeX(1.6666667f), decodeY(0.4f));
        path.lineTo(decodeX(2.2f), decodeY(0.4f));
        path.lineTo(decodeX(2.2f), decodeY(0.2f));
        path.lineTo(decodeX(1.1666666f), decodeY(0.2f));
        path.closePath();
        return path;
    }

    private Path2D decodePath29() {
        path.reset();
        path.moveTo(decodeX(0.8f), decodeY(0.2f));
        path.lineTo(decodeX(1.0f), decodeY(0.2f));
        path.lineTo(decodeX(1.0f), decodeY(1.0f));
        path.lineTo(decodeX(1.3333334f), decodeY(1.0f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.2f));
        path.lineTo(decodeX(1.5f), decodeY(0.2f));
        path.lineTo(decodeX(1.5f), decodeY(1.0f));
        path.lineTo(decodeX(1.6666667f), decodeY(1.0f));
        path.lineTo(decodeX(1.6666667f), decodeY(1.1666666f));
        path.lineTo(decodeX(0.8f), decodeY(1.1666666f));
        path.lineTo(decodeX(0.8f), decodeY(0.2f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect14() {
            rect.setRect(decodeX(0.8f), //x
                         decodeY(2.6f), //y
                         decodeX(2.2f) - decodeX(0.8f), //width
                         decodeY(2.8f) - decodeY(2.6f)); //height
        return rect;
    }

    private Rectangle2D decodeRect15() {
            rect.setRect(decodeX(0.36153847f), //x
                         decodeY(2.3576922f), //y
                         decodeX(0.63461536f) - decodeX(0.36153847f), //width
                         decodeY(2.6807692f) - decodeY(2.3576922f)); //height
        return rect;
    }

    private Rectangle2D decodeRect16() {
            rect.setRect(decodeX(2.376923f), //x
                         decodeY(2.3807693f), //y
                         decodeX(2.6384616f) - decodeX(2.376923f), //width
                         decodeY(2.6846154f) - decodeY(2.3807693f)); //height
        return rect;
    }

    private Rectangle2D decodeRect17() {
            rect.setRect(decodeX(0.4f), //x
                         decodeY(2.4f), //y
                         decodeX(0.6f) - decodeX(0.4f), //width
                         decodeY(2.6f) - decodeY(2.4f)); //height
        return rect;
    }

    private Rectangle2D decodeRect18() {
            rect.setRect(decodeX(2.4f), //x
                         decodeY(2.4f), //y
                         decodeX(2.6f) - decodeX(2.4f), //width
                         decodeY(2.6f) - decodeY(2.4f)); //height
        return rect;
    }

    private Path2D decodePath30() {
        path.reset();
        path.moveTo(decodeX(0.4f), decodeY(1.5f));
        path.lineTo(decodeX(0.4f), decodeY(2.6f));
        path.lineTo(decodeX(0.6f), decodeY(2.8f));
        path.lineTo(decodeX(2.4f), decodeY(2.8f));
        path.lineTo(decodeX(2.6f), decodeY(2.6f));
        path.lineTo(decodeX(2.6f), decodeY(1.5f));
        path.lineTo(decodeX(0.4f), decodeY(1.5f));
        path.closePath();
        return path;
    }

    private Path2D decodePath31() {
        path.reset();
        path.moveTo(decodeX(0.6f), decodeY(1.5f));
        path.lineTo(decodeX(0.6f), decodeY(2.6f));
        path.lineTo(decodeX(2.4f), decodeY(2.6f));
        path.lineTo(decodeX(2.4f), decodeY(1.5f));
        path.lineTo(decodeX(1.5f), decodeY(0.8f));
        path.lineTo(decodeX(0.6f), decodeY(1.5f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect19() {
            rect.setRect(decodeX(1.6666667f), //x
                         decodeY(1.6666667f), //y
                         decodeX(2.2f) - decodeX(1.6666667f), //width
                         decodeY(2.2f) - decodeY(1.6666667f)); //height
        return rect;
    }

    private Rectangle2D decodeRect20() {
            rect.setRect(decodeX(1.8333333f), //x
                         decodeY(1.8333333f), //y
                         decodeX(2.0f) - decodeX(1.8333333f), //width
                         decodeY(2.0f) - decodeY(1.8333333f)); //height
        return rect;
    }

    private Path2D decodePath32() {
        path.reset();
        path.moveTo(decodeX(1.0f), decodeY(2.8f));
        path.lineTo(decodeX(1.5f), decodeY(2.8f));
        path.lineTo(decodeX(1.5f), decodeY(1.8333333f));
        path.lineTo(decodeX(1.3333334f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.1666666f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.0f), decodeY(1.8333333f));
        path.lineTo(decodeX(1.0f), decodeY(2.8f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect21() {
            rect.setRect(decodeX(1.1666666f), //x
                         decodeY(1.8333333f), //y
                         decodeX(1.3333334f) - decodeX(1.1666666f), //width
                         decodeY(2.6f) - decodeY(1.8333333f)); //height
        return rect;
    }

    private Path2D decodePath33() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(1.3333334f));
        path.lineTo(decodeX(0.0f), decodeY(1.6666667f));
        path.lineTo(decodeX(0.4f), decodeY(1.6666667f));
        path.lineTo(decodeX(1.3974359f), decodeY(0.6f));
        path.lineTo(decodeX(1.596154f), decodeY(0.6f));
        path.lineTo(decodeX(2.6f), decodeY(1.6666667f));
        path.lineTo(decodeX(3.0f), decodeY(1.6666667f));
        path.lineTo(decodeX(3.0f), decodeY(1.3333334f));
        path.lineTo(decodeX(1.6666667f), decodeY(0.0f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.0f));
        path.lineTo(decodeX(0.0f), decodeY(1.3333334f));
        path.closePath();
        return path;
    }

    private Path2D decodePath34() {
        path.reset();
        path.moveTo(decodeX(0.2576923f), decodeY(1.3717948f));
        path.lineTo(decodeX(0.2f), decodeY(1.5f));
        path.lineTo(decodeX(0.3230769f), decodeY(1.4711539f));
        path.lineTo(decodeX(1.4006411f), decodeY(0.40384617f));
        path.lineTo(decodeX(1.5929487f), decodeY(0.4f));
        path.lineTo(decodeX(2.6615386f), decodeY(1.4615384f));
        path.lineTo(decodeX(2.8f), decodeY(1.5f));
        path.lineTo(decodeX(2.7461538f), decodeY(1.3653846f));
        path.lineTo(decodeX(1.6089742f), decodeY(0.19615385f));
        path.lineTo(decodeX(1.4070512f), decodeY(0.2f));
        path.lineTo(decodeX(0.2576923f), decodeY(1.3717948f));
        path.closePath();
        return path;
    }

    private Path2D decodePath35() {
        path.reset();
        path.moveTo(decodeX(0.6f), decodeY(1.5f));
        path.lineTo(decodeX(1.3333334f), decodeY(0.6f));
        path.lineTo(decodeX(1.5f), decodeY(0.6f));
        path.lineTo(decodeX(1.5f), decodeY(1.1666666f));
        path.lineTo(decodeX(1.0f), decodeY(1.6666667f));
        path.lineTo(decodeX(0.6f), decodeY(1.6666667f));
        path.lineTo(decodeX(0.6f), decodeY(1.5f));
        path.closePath();
        return path;
    }

    private Path2D decodePath36() {
        path.reset();
        path.moveTo(decodeX(1.6666667f), decodeY(0.6f));
        path.lineTo(decodeX(1.5f), decodeY(0.6f));
        path.lineTo(decodeX(1.5f), decodeY(1.1666666f));
        path.lineTo(decodeX(2.0f), decodeY(1.6666667f));
        path.lineTo(decodeX(2.4f), decodeY(1.6666667f));
        path.lineTo(decodeX(2.4f), decodeY(1.3333334f));
        path.lineTo(decodeX(1.6666667f), decodeY(0.6f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect22() {
            rect.setRect(decodeX(0.2f), //x
                         decodeY(0.0f), //y
                         decodeX(3.0f) - decodeX(0.2f), //width
                         decodeY(2.8f) - decodeY(0.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect23() {
            rect.setRect(decodeX(0.4f), //x
                         decodeY(0.2f), //y
                         decodeX(2.8f) - decodeX(0.4f), //width
                         decodeY(2.6f) - decodeY(0.2f)); //height
        return rect;
    }

    private Rectangle2D decodeRect24() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(0.6f), //y
                         decodeX(1.3333334f) - decodeX(1.0f), //width
                         decodeY(0.8f) - decodeY(0.6f)); //height
        return rect;
    }

    private Rectangle2D decodeRect25() {
            rect.setRect(decodeX(1.5f), //x
                         decodeY(1.3333334f), //y
                         decodeX(2.4f) - decodeX(1.5f), //width
                         decodeY(1.5f) - decodeY(1.3333334f)); //height
        return rect;
    }

    private Rectangle2D decodeRect26() {
            rect.setRect(decodeX(1.5f), //x
                         decodeY(2.0f), //y
                         decodeX(2.4f) - decodeX(1.5f), //width
                         decodeY(2.2f) - decodeY(2.0f)); //height
        return rect;
    }

    private Ellipse2D decodeEllipse8() {
        ellipse.setFrame(decodeX(0.6f), //x
                         decodeY(0.8f), //y
                         decodeX(2.2f) - decodeX(0.6f), //width
                         decodeY(2.4f) - decodeY(0.8f)); //height
        return ellipse;
    }

    private Ellipse2D decodeEllipse9() {
        ellipse.setFrame(decodeX(0.8f), //x
                         decodeY(1.0f), //y
                         decodeX(2.0f) - decodeX(0.8f), //width
                         decodeY(2.2f) - decodeY(1.0f)); //height
        return ellipse;
    }

    private Path2D decodePath37() {
        path.reset();
        path.moveTo(decodeX(0.0f), decodeY(2.8f));
        path.lineTo(decodeX(0.0f), decodeY(3.0f));
        path.lineTo(decodeX(0.4f), decodeY(3.0f));
        path.lineTo(decodeX(1.0f), decodeY(2.2f));
        path.lineTo(decodeX(0.8f), decodeY(1.8333333f));
        path.lineTo(decodeX(0.0f), decodeY(2.8f));
        path.closePath();
        return path;
    }

    private Path2D decodePath38() {
        path.reset();
        path.moveTo(decodeX(0.1826087f), decodeY(2.7217393f));
        path.lineTo(decodeX(0.2826087f), decodeY(2.8217392f));
        path.lineTo(decodeX(1.0181159f), decodeY(2.095652f));
        path.lineTo(decodeX(0.9130435f), decodeY(1.9891305f));
        path.lineTo(decodeX(0.1826087f), decodeY(2.7217393f));
        path.closePath();
        return path;
    }

    private Rectangle2D decodeRect27() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(1.3333334f), //y
                         decodeX(1.3333334f) - decodeX(1.0f), //width
                         decodeY(1.5f) - decodeY(1.3333334f)); //height
        return rect;
    }

    private Rectangle2D decodeRect28() {
            rect.setRect(decodeX(1.5f), //x
                         decodeY(1.3333334f), //y
                         decodeX(1.8333333f) - decodeX(1.5f), //width
                         decodeY(1.5f) - decodeY(1.3333334f)); //height
        return rect;
    }

    private Rectangle2D decodeRect29() {
            rect.setRect(decodeX(1.5f), //x
                         decodeY(1.6666667f), //y
                         decodeX(1.8333333f) - decodeX(1.5f), //width
                         decodeY(1.8333333f) - decodeY(1.6666667f)); //height
        return rect;
    }

    private Rectangle2D decodeRect30() {
            rect.setRect(decodeX(1.0f), //x
                         decodeY(1.6666667f), //y
                         decodeX(1.3333334f) - decodeX(1.0f), //width
                         decodeY(1.8333333f) - decodeY(1.6666667f)); //height
        return rect;
    }

    private Rectangle2D decodeRect31() {
            rect.setRect(decodeX(0.0f), //x
                         decodeY(0.0f), //y
                         decodeX(3.0f) - decodeX(0.0f), //width
                         decodeY(2.8f) - decodeY(0.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect32() {
            rect.setRect(decodeX(0.2f), //x
                         decodeY(0.2f), //y
                         decodeX(2.8f) - decodeX(0.2f), //width
                         decodeY(2.6f) - decodeY(0.2f)); //height
        return rect;
    }

    private Rectangle2D decodeRect33() {
            rect.setRect(decodeX(0.8f), //x
                         decodeY(0.6f), //y
                         decodeX(1.1666666f) - decodeX(0.8f), //width
                         decodeY(0.8f) - decodeY(0.6f)); //height
        return rect;
    }

    private Rectangle2D decodeRect34() {
            rect.setRect(decodeX(1.3333334f), //x
                         decodeY(0.6f), //y
                         decodeX(2.2f) - decodeX(1.3333334f), //width
                         decodeY(0.8f) - decodeY(0.6f)); //height
        return rect;
    }

    private Rectangle2D decodeRect35() {
            rect.setRect(decodeX(1.3333334f), //x
                         decodeY(1.0f), //y
                         decodeX(2.0f) - decodeX(1.3333334f), //width
                         decodeY(1.1666666f) - decodeY(1.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect36() {
            rect.setRect(decodeX(0.8f), //x
                         decodeY(1.0f), //y
                         decodeX(1.1666666f) - decodeX(0.8f), //width
                         decodeY(1.1666666f) - decodeY(1.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect37() {
            rect.setRect(decodeX(0.8f), //x
                         decodeY(1.3333334f), //y
                         decodeX(1.1666666f) - decodeX(0.8f), //width
                         decodeY(1.5f) - decodeY(1.3333334f)); //height
        return rect;
    }

    private Rectangle2D decodeRect38() {
            rect.setRect(decodeX(1.3333334f), //x
                         decodeY(1.3333334f), //y
                         decodeX(2.2f) - decodeX(1.3333334f), //width
                         decodeY(1.5f) - decodeY(1.3333334f)); //height
        return rect;
    }

    private Rectangle2D decodeRect39() {
            rect.setRect(decodeX(0.8f), //x
                         decodeY(1.6666667f), //y
                         decodeX(1.1666666f) - decodeX(0.8f), //width
                         decodeY(1.8333333f) - decodeY(1.6666667f)); //height
        return rect;
    }

    private Rectangle2D decodeRect40() {
            rect.setRect(decodeX(1.3333334f), //x
                         decodeY(1.6666667f), //y
                         decodeX(2.0f) - decodeX(1.3333334f), //width
                         decodeY(1.8333333f) - decodeY(1.6666667f)); //height
        return rect;
    }

    private Rectangle2D decodeRect41() {
            rect.setRect(decodeX(1.3333334f), //x
                         decodeY(2.0f), //y
                         decodeX(2.2f) - decodeX(1.3333334f), //width
                         decodeY(2.2f) - decodeY(2.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect42() {
            rect.setRect(decodeX(0.8f), //x
                         decodeY(2.0f), //y
                         decodeX(1.1666666f) - decodeX(0.8f), //width
                         decodeY(2.2f) - decodeY(2.0f)); //height
        return rect;
    }

    private Rectangle2D decodeRect43() {
            rect.setRect(decodeX(0.8f), //x
                         decodeY(0.8f), //y
                         decodeX(1.1666666f) - decodeX(0.8f), //width
                         decodeY(1.0f) - decodeY(0.8f)); //height
        return rect;
    }

    private Rectangle2D decodeRect44() {
            rect.setRect(decodeX(1.3333334f), //x
                         decodeY(0.8f), //y
                         decodeX(2.2f) - decodeX(1.3333334f), //width
                         decodeY(1.0f) - decodeY(0.8f)); //height
        return rect;
    }

    private Rectangle2D decodeRect45() {
            rect.setRect(decodeX(0.8f), //x
                         decodeY(1.1666666f), //y
                         decodeX(1.1666666f) - decodeX(0.8f), //width
                         decodeY(1.3333334f) - decodeY(1.1666666f)); //height
        return rect;
    }

    private Rectangle2D decodeRect46() {
            rect.setRect(decodeX(1.3333334f), //x
                         decodeY(1.1666666f), //y
                         decodeX(2.0f) - decodeX(1.3333334f), //width
                         decodeY(1.3333334f) - decodeY(1.1666666f)); //height
        return rect;
    }

    private Rectangle2D decodeRect47() {
            rect.setRect(decodeX(0.8f), //x
                         decodeY(1.5f), //y
                         decodeX(1.1666666f) - decodeX(0.8f), //width
                         decodeY(1.6666667f) - decodeY(1.5f)); //height
        return rect;
    }

    private Rectangle2D decodeRect48() {
            rect.setRect(decodeX(1.3333334f), //x
                         decodeY(1.5f), //y
                         decodeX(2.2f) - decodeX(1.3333334f), //width
                         decodeY(1.6666667f) - decodeY(1.5f)); //height
        return rect;
    }

    private Rectangle2D decodeRect49() {
            rect.setRect(decodeX(1.3333334f), //x
                         decodeY(1.8333333f), //y
                         decodeX(2.0f) - decodeX(1.3333334f), //width
                         decodeY(2.0f) - decodeY(1.8333333f)); //height
        return rect;
    }

    private Rectangle2D decodeRect50() {
            rect.setRect(decodeX(0.8f), //x
                         decodeY(1.8333333f), //y
                         decodeX(1.1666666f) - decodeX(0.8f), //width
                         decodeY(2.0f) - decodeY(1.8333333f)); //height
        return rect;
    }

    private Rectangle2D decodeRect51() {
            rect.setRect(decodeX(0.8f), //x
                         decodeY(2.2f), //y
                         decodeX(1.1666666f) - decodeX(0.8f), //width
                         decodeY(2.4f) - decodeY(2.2f)); //height
        return rect;
    }

    private Rectangle2D decodeRect52() {
            rect.setRect(decodeX(1.3333334f), //x
                         decodeY(2.2f), //y
                         decodeX(2.2f) - decodeX(1.3333334f), //width
                         decodeY(2.4f) - decodeY(2.2f)); //height
        return rect;
    }



    private Paint decodeGradient1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.046296295f * w) + x, (0.9675926f * h) + y, (0.4861111f * w) + x, (0.5324074f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color4,
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
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color6,
                            decodeColor(color6,color7,0.5f),
                            color7});
    }

    private Paint decodeGradient3(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.04191617f,0.10329342f,0.16467066f,0.24550897f,0.3263473f,0.6631737f,1.0f },
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
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color6,
                            decodeColor(color6,color15,0.5f),
                            color15});
    }

    private Paint decodeGradient5(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
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
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.12724552f,0.25449103f,0.62724555f,1.0f },
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
                new float[] { 0.0f,0.06392045f,0.1278409f,0.5213069f,0.91477275f },
                new Color[] { color25,
                            decodeColor(color25,color26,0.5f),
                            color26,
                            decodeColor(color26,color27,0.5f),
                            color27});
    }

    private Paint decodeGradient8(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.048295453f,0.09659091f,0.5482955f,1.0f },
                new Color[] { color28,
                            decodeColor(color28,color6,0.5f),
                            color6,
                            decodeColor(color6,color15,0.5f),
                            color15});
    }

    private Paint decodeGradient9(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color29,
                            decodeColor(color29,color30,0.5f),
                            color30});
    }

    private Paint decodeGradient10(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.06534091f,0.13068181f,0.3096591f,0.48863637f,0.7443182f,1.0f },
                new Color[] { color11,
                            decodeColor(color11,color12,0.5f),
                            color12,
                            decodeColor(color12,color31,0.5f),
                            color31,
                            decodeColor(color31,color14,0.5f),
                            color14});
    }

    private Paint decodeGradient11(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color33,
                            decodeColor(color33,color34,0.5f),
                            color34});
    }

    private Paint decodeGradient12(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color36,
                            decodeColor(color36,color37,0.5f),
                            color37});
    }

    private Paint decodeRadial1(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeRadialGradient((0.5f * w) + x, (1.0f * h) + y, 0.53913116f,
                new float[] { 0.11290322f,0.17419355f,0.23548387f,0.31129032f,0.38709676f,0.47903225f,0.57096773f },
                new Color[] { color40,
                            decodeColor(color40,color41,0.5f),
                            color41,
                            decodeColor(color41,color41,0.5f),
                            color41,
                            decodeColor(color41,color42,0.5f),
                            color42});
    }

    private Paint decodeGradient13(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color45,
                            decodeColor(color45,color46,0.5f),
                            color46});
    }

    private Paint decodeGradient14(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color47,
                            decodeColor(color47,color48,0.5f),
                            color48});
    }

    private Paint decodeGradient15(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.3983871f,0.7967742f,0.8983871f,1.0f },
                new Color[] { color51,
                            decodeColor(color51,color52,0.5f),
                            color52,
                            decodeColor(color52,color51,0.5f),
                            color51});
    }

    private Paint decodeGradient16(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.061290324f,0.12258065f,0.5016129f,0.88064516f,0.9403226f,1.0f },
                new Color[] { color57,
                            decodeColor(color57,color58,0.5f),
                            color58,
                            decodeColor(color58,color59,0.5f),
                            color59,
                            decodeColor(color59,color44,0.5f),
                            color44});
    }

    private Paint decodeGradient17(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.05f,0.1f,0.19193548f,0.28387097f,0.5209677f,0.7580645f,0.87903225f,1.0f },
                new Color[] { color60,
                            decodeColor(color60,color61,0.5f),
                            color61,
                            decodeColor(color61,color62,0.5f),
                            color62,
                            decodeColor(color62,color63,0.5f),
                            color63,
                            decodeColor(color63,color64,0.5f),
                            color64});
    }

    private Paint decodeGradient18(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.058064517f,0.090322584f,0.12258065f,0.15645161f,0.19032258f,0.22741935f,0.26451612f,0.31290323f,0.36129034f,0.38225806f,0.4032258f,0.4596774f,0.516129f,0.54193544f,0.56774193f,0.61451614f,0.66129035f,0.70645165f,0.7516129f },
                new Color[] { color65,
                            decodeColor(color65,color40,0.5f),
                            color40,
                            decodeColor(color40,color40,0.5f),
                            color40,
                            decodeColor(color40,color65,0.5f),
                            color65,
                            decodeColor(color65,color65,0.5f),
                            color65,
                            decodeColor(color65,color40,0.5f),
                            color40,
                            decodeColor(color40,color40,0.5f),
                            color40,
                            decodeColor(color40,color66,0.5f),
                            color66,
                            decodeColor(color66,color66,0.5f),
                            color66,
                            decodeColor(color66,color40,0.5f),
                            color40});
    }

    private Paint decodeGradient19(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color67,
                            decodeColor(color67,color67,0.5f),
                            color67});
    }

    private Paint decodeGradient20(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color74,
                            decodeColor(color74,color75,0.5f),
                            color75});
    }

    private Paint decodeGradient21(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color77,
                            decodeColor(color77,color78,0.5f),
                            color78});
    }

    private Paint decodeGradient22(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color79,
                            decodeColor(color79,color80,0.5f),
                            color80});
    }

    private Paint decodeGradient23(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color81,
                            decodeColor(color81,color82,0.5f),
                            color82});
    }

    private Paint decodeGradient24(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.43076923f * w) + x, (0.37820512f * h) + y, (0.7076923f * w) + x, (0.6730769f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color84,
                            decodeColor(color84,color85,0.5f),
                            color85});
    }

    private Paint decodeGradient25(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.63076925f * w) + x, (0.3621795f * h) + y, (0.28846154f * w) + x, (0.73397434f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color84,
                            decodeColor(color84,color86,0.5f),
                            color86});
    }

    private Paint decodeGradient26(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color87,
                            decodeColor(color87,color88,0.5f),
                            color88});
    }

    private Paint decodeGradient27(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.056818184f,0.11363637f,0.34232956f,0.57102275f,0.7855114f,1.0f },
                new Color[] { color89,
                            decodeColor(color89,color90,0.5f),
                            color90,
                            decodeColor(color90,color91,0.5f),
                            color91,
                            decodeColor(color91,color92,0.5f),
                            color92});
    }

    private Paint decodeGradient28(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.75f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.5f,1.0f },
                new Color[] { color95,
                            decodeColor(color95,color96,0.5f),
                            color96});
    }

    private Paint decodeRadial2(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeRadialGradient((0.49223602f * w) + x, (0.9751553f * h) + y, 0.73615754f,
                new float[] { 0.0f,0.40625f,1.0f },
                new Color[] { color97,
                            decodeColor(color97,color98,0.5f),
                            color98});
    }

    private Paint decodeGradient29(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.0f * w) + x, (0.0f * h) + y, (1.0f * w) + x, (1.0f * h) + y,
                new float[] { 0.38352272f,0.4190341f,0.45454547f,0.484375f,0.51420456f },
                new Color[] { color99,
                            decodeColor(color99,color100,0.5f),
                            color100,
                            decodeColor(color100,color101,0.5f),
                            color101});
    }

    private Paint decodeGradient30(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((1.0f * w) + x, (0.0f * h) + y, (0.0f * w) + x, (1.0f * h) + y,
                new float[] { 0.12215909f,0.16051137f,0.19886364f,0.2627841f,0.32670453f,0.43039775f,0.53409094f },
                new Color[] { color102,
                            decodeColor(color102,color35,0.5f),
                            color35,
                            decodeColor(color35,color35,0.5f),
                            color35,
                            decodeColor(color35,color103,0.5f),
                            color103});
    }

    private Paint decodeGradient31(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.5f * w) + x, (0.0f * h) + y, (0.5f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.038352273f,0.07670455f,0.24289773f,0.4090909f,0.7045455f,1.0f },
                new Color[] { color89,
                            decodeColor(color89,color90,0.5f),
                            color90,
                            decodeColor(color90,color108,0.5f),
                            color108,
                            decodeColor(color108,color92,0.5f),
                            color92});
    }

    private Paint decodeGradient32(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.0f * w) + x, (0.0f * h) + y, (1.0f * w) + x, (1.0f * h) + y,
                new float[] { 0.25f,0.33522725f,0.42045453f,0.50142044f,0.5823864f },
                new Color[] { color109,
                            decodeColor(color109,color110,0.5f),
                            color110,
                            decodeColor(color110,color109,0.5f),
                            color109});
    }

    private Paint decodeGradient33(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.25f * w) + x, (0.0f * h) + y, (0.75f * w) + x, (1.0f * h) + y,
                new float[] { 0.0f,0.24147727f,0.48295453f,0.74147725f,1.0f },
                new Color[] { color114,
                            decodeColor(color114,color115,0.5f),
                            color115,
                            decodeColor(color115,color114,0.5f),
                            color114});
    }

    private Paint decodeGradient34(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.0f * w) + x, (0.0f * h) + y, (1.0f * w) + x, (0.0f * h) + y,
                new float[] { 0.0f,0.21732955f,0.4346591f },
                new Color[] { color117,
                            decodeColor(color117,color118,0.5f),
                            color118});
    }

    private Paint decodeGradient35(Shape s) {
        Rectangle2D bounds = s.getBounds2D();
        float x = (float)bounds.getX();
        float y = (float)bounds.getY();
        float w = (float)bounds.getWidth();
        float h = (float)bounds.getHeight();
        return decodeGradient((0.0f * w) + x, (0.0f * h) + y, (1.0f * w) + x, (0.0f * h) + y,
                new float[] { 0.0f,0.21448864f,0.42897728f,0.7144886f,1.0f },
                new Color[] { color119,
                            decodeColor(color119,color120,0.5f),
                            color120,
                            decodeColor(color120,color119,0.5f),
                            color119});
    }


}

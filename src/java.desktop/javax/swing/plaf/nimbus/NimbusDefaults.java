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

import javax.swing.Painter;
import java.awt.Graphics;
import sun.font.FontUtilities;
import sun.swing.plaf.synth.DefaultSynthStyle;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.synth.Region;
import javax.swing.plaf.synth.SynthStyle;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;

/**
 * This class contains all the implementation details related to
 * Nimbus. It contains all the code for initializing the UIDefaults table,
 * as well as for selecting
 * a SynthStyle based on a JComponent/Region pair.
 *
 * @author Richard Bair
 */
final class NimbusDefaults {
    /**
     * The map of SynthStyles. This map is keyed by Region. Each Region maps
     * to a List of LazyStyles. Each LazyStyle has a reference to the prefix
     * that was registered with it. This reference can then be inspected to see
     * if it is the proper lazy style.
     * <p/>
     * There can be more than one LazyStyle for a single Region if there is more
     * than one prefix defined for a given region. For example, both Button and
     * "MyButton" might be prefixes assigned to the Region.Button region.
     */
    private Map<Region, List<LazyStyle>> m;
    /**
     * A map of regions which have been registered.
     * This mapping is maintained so that the Region can be found based on
     * prefix in a very fast manner. This is used in the "matches" method of
     * LazyStyle.
     */
    private Map<String, Region> registeredRegions =
            new HashMap<String, Region>();

    private Map<JComponent, Map<Region, SynthStyle>> overridesCache =
            new WeakHashMap<JComponent, Map<Region, SynthStyle>>();
    
    /**
     * Our fallback style to avoid NPEs if the proper style cannot be found in
     * this class. Not sure if relying on DefaultSynthStyle is the best choice.
     */
    private DefaultSynthStyle defaultStyle;
    /**
     * The default font that will be used. I store this value so that it can be
     * set in the UIDefaults when requested.
     */
    private FontUIResource defaultFont;

    private ColorTree colorTree = new ColorTree();

    /** Listener for changes to user defaults table */
    private DefaultsListener defaultsListener = new DefaultsListener();

    /** Called by UIManager when this look and feel is installed. */
    void initialize() {
        // add listener for derived colors
        UIManager.addPropertyChangeListener(defaultsListener);
        UIManager.getDefaults().addPropertyChangeListener(colorTree);
    }

    /** Called by UIManager when this look and feel is uninstalled. */
    void uninitialize() {
        // remove listener for derived colors
        UIManager.removePropertyChangeListener(defaultsListener);
        UIManager.getDefaults().removePropertyChangeListener(colorTree);
    }

    /**
     * Create a new NimbusDefaults. This constructor is only called from
     * within NimbusLookAndFeel.
     */
    NimbusDefaults() {
        m = new HashMap<Region, List<LazyStyle>>();

        //Create the default font and default style. Also register all of the
        //regions and their states that this class will use for later lookup.
        //Additional regions can be registered later by 3rd party components.
        //These are simply the default registrations.
        defaultFont = FontUtilities.getFontConfigFUIR("sans", Font.PLAIN, 12);
        defaultStyle = new DefaultSynthStyle();
        defaultStyle.setFont(defaultFont);

        //initialize the map of styles
        register(Region.ARROW_BUTTON, "ArrowButton");
        register(Region.BUTTON, "Button");
        register(Region.TOGGLE_BUTTON, "ToggleButton");
        register(Region.RADIO_BUTTON, "RadioButton");
        register(Region.CHECK_BOX, "CheckBox");
        register(Region.COLOR_CHOOSER, "ColorChooser");
        register(Region.PANEL, "ColorChooser:\"ColorChooser.previewPanelHolder\"");
        register(Region.LABEL, "ColorChooser:\"ColorChooser.previewPanelHolder\":\"OptionPane.label\"");
        register(Region.COMBO_BOX, "ComboBox");
        register(Region.TEXT_FIELD, "ComboBox:\"ComboBox.textField\"");
        register(Region.ARROW_BUTTON, "ComboBox:\"ComboBox.arrowButton\"");
        register(Region.LABEL, "ComboBox:\"ComboBox.listRenderer\"");
        register(Region.LABEL, "ComboBox:\"ComboBox.renderer\"");
        register(Region.SCROLL_PANE, "\"ComboBox.scrollPane\"");
        register(Region.FILE_CHOOSER, "FileChooser");
        register(Region.INTERNAL_FRAME_TITLE_PANE, "InternalFrameTitlePane");
        register(Region.INTERNAL_FRAME, "InternalFrame");
        register(Region.INTERNAL_FRAME_TITLE_PANE, "InternalFrame:InternalFrameTitlePane");
        register(Region.BUTTON, "InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\"");
        register(Region.BUTTON, "InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.iconifyButton\"");
        register(Region.BUTTON, "InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"");
        register(Region.BUTTON, "InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.closeButton\"");
        register(Region.DESKTOP_ICON, "DesktopIcon");
        register(Region.DESKTOP_PANE, "DesktopPane");
        register(Region.LABEL, "Label");
        register(Region.LIST, "List");
        register(Region.LABEL, "List:\"List.cellRenderer\"");
        register(Region.MENU_BAR, "MenuBar");
        register(Region.MENU, "MenuBar:Menu");
        register(Region.MENU_ITEM_ACCELERATOR, "MenuBar:Menu:MenuItemAccelerator");
        register(Region.MENU_ITEM, "MenuItem");
        register(Region.MENU_ITEM_ACCELERATOR, "MenuItem:MenuItemAccelerator");
        register(Region.RADIO_BUTTON_MENU_ITEM, "RadioButtonMenuItem");
        register(Region.MENU_ITEM_ACCELERATOR, "RadioButtonMenuItem:MenuItemAccelerator");
        register(Region.CHECK_BOX_MENU_ITEM, "CheckBoxMenuItem");
        register(Region.MENU_ITEM_ACCELERATOR, "CheckBoxMenuItem:MenuItemAccelerator");
        register(Region.MENU, "Menu");
        register(Region.MENU_ITEM_ACCELERATOR, "Menu:MenuItemAccelerator");
        register(Region.POPUP_MENU, "PopupMenu");
        register(Region.POPUP_MENU_SEPARATOR, "PopupMenuSeparator");
        register(Region.OPTION_PANE, "OptionPane");
        register(Region.SEPARATOR, "OptionPane:\"OptionPane.separator\"");
        register(Region.PANEL, "OptionPane:\"OptionPane.messageArea\"");
        register(Region.LABEL, "OptionPane:\"OptionPane.messageArea\":\"OptionPane.label\"");
        register(Region.PANEL, "Panel");
        register(Region.PROGRESS_BAR, "ProgressBar");
        register(Region.SEPARATOR, "Separator");
        register(Region.SCROLL_BAR, "ScrollBar");
        register(Region.ARROW_BUTTON, "ScrollBar:\"ScrollBar.button\"");
        register(Region.SCROLL_BAR_THUMB, "ScrollBar:ScrollBarThumb");
        register(Region.SCROLL_BAR_TRACK, "ScrollBar:ScrollBarTrack");
        register(Region.SCROLL_PANE, "ScrollPane");
        register(Region.VIEWPORT, "Viewport");
        register(Region.SLIDER, "Slider");
        register(Region.SLIDER_THUMB, "Slider:SliderThumb");
        register(Region.SLIDER_TRACK, "Slider:SliderTrack");
        register(Region.SPINNER, "Spinner");
        register(Region.PANEL, "Spinner:\"Spinner.editor\"");
        register(Region.FORMATTED_TEXT_FIELD, "Spinner:Panel:\"Spinner.formattedTextField\"");
        register(Region.ARROW_BUTTON, "Spinner:\"Spinner.previousButton\"");
        register(Region.ARROW_BUTTON, "Spinner:\"Spinner.nextButton\"");
        register(Region.SPLIT_PANE, "SplitPane");
        register(Region.SPLIT_PANE_DIVIDER, "SplitPane:SplitPaneDivider");
        register(Region.TABBED_PANE, "TabbedPane");
        register(Region.TABBED_PANE_TAB, "TabbedPane:TabbedPaneTab");
        register(Region.TABBED_PANE_TAB_AREA, "TabbedPane:TabbedPaneTabArea");
        register(Region.TABBED_PANE_CONTENT, "TabbedPane:TabbedPaneContent");
        register(Region.TABLE, "Table");
        register(Region.LABEL, "Table:\"Table.cellRenderer\"");
        register(Region.TABLE_HEADER, "TableHeader");
        register(Region.LABEL, "TableHeader:\"TableHeader.renderer\"");
        register(Region.TEXT_FIELD, "\"Table.editor\"");
        register(Region.TEXT_FIELD, "\"Tree.cellEditor\"");
        register(Region.TEXT_FIELD, "TextField");
        register(Region.FORMATTED_TEXT_FIELD, "FormattedTextField");
        register(Region.PASSWORD_FIELD, "PasswordField");
        register(Region.TEXT_AREA, "TextArea");
        register(Region.TEXT_PANE, "TextPane");
        register(Region.EDITOR_PANE, "EditorPane");
        register(Region.TOOL_BAR, "ToolBar");
        register(Region.BUTTON, "ToolBar:Button");
        register(Region.TOGGLE_BUTTON, "ToolBar:ToggleButton");
        register(Region.TOOL_BAR_SEPARATOR, "ToolBarSeparator");
        register(Region.TOOL_TIP, "ToolTip");
        register(Region.TREE, "Tree");
        register(Region.TREE_CELL, "Tree:TreeCell");
        register(Region.LABEL, "Tree:\"Tree.cellRenderer\"");
        register(Region.ROOT_PANE, "RootPane");

    }

    //--------------- Methods called by NimbusLookAndFeel

    /**
     * Called from NimbusLookAndFeel to initialize the UIDefaults.
     *
     * @param d UIDefaults table to initialize. This will never be null.
     *          If listeners are attached to <code>d</code>, then you will
     *          only receive notification of LookAndFeel level defaults, not
     *          all defaults on the UIManager.
     */
    void initializeDefaults(UIDefaults d) {
        //Color palette
        addColor(d, "text", 0, 0, 0, 255);
        addColor(d, "control", 214, 217, 223, 255);
        addColor(d, "nimbusBase", 51, 98, 140, 255);
        addColor(d, "nimbusBlueGrey", "nimbusBase", 0.032459438f, -0.52518797f, 0.19607842f, 0);
        addColor(d, "nimbusOrange", 191, 98, 4, 255);
        addColor(d, "nimbusGreen", 176, 179, 50, 255);
        addColor(d, "nimbusRed", 169, 46, 34, 255);
        addColor(d, "nimbusBorder", "nimbusBlueGrey", 0.0f, -0.017358616f, -0.11372548f, 0);
        addColor(d, "nimbusSelection", "nimbusBase", -0.010750473f, -0.04875779f, -0.007843137f, 0);
        addColor(d, "nimbusInfoBlue", 47, 92, 180, 255);
        addColor(d, "nimbusAlertYellow", 255, 220, 35, 255);
        addColor(d, "nimbusFocus", 115, 164, 209, 255);
        addColor(d, "nimbusSelectedText", 255, 255, 255, 255);
        addColor(d, "nimbusSelectionBackground", 57, 105, 138, 255);
        addColor(d, "nimbusDisabledText", 142, 143, 145, 255);
        addColor(d, "nimbusLightBackground", 255, 255, 255, 255);
        addColor(d, "infoText", "text", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "info", 242, 242, 189, 255);
        addColor(d, "menuText", "text", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "menu", "nimbusBase", 0.021348298f, -0.6150531f, 0.39999998f, 0);
        addColor(d, "scrollbar", "nimbusBlueGrey", -0.006944418f, -0.07296763f, 0.09019607f, 0);
        addColor(d, "controlText", "text", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "controlHighlight", "nimbusBlueGrey", 0.0f, -0.07333623f, 0.20392156f, 0);
        addColor(d, "controlLHighlight", "nimbusBlueGrey", 0.0f, -0.098526314f, 0.2352941f, 0);
        addColor(d, "controlShadow", "nimbusBlueGrey", -0.0027777553f, -0.0212406f, 0.13333333f, 0);
        addColor(d, "controlDkShadow", "nimbusBlueGrey", -0.0027777553f, -0.0018306673f, -0.02352941f, 0);
        addColor(d, "textHighlight", "nimbusSelectionBackground", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "textHighlightText", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "textInactiveText", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "desktop", "nimbusBase", -0.009207249f, -0.13984653f, -0.07450983f, 0);
        addColor(d, "activeCaption", "nimbusBlueGrey", 0.0f, -0.049920253f, 0.031372547f, 0);
        addColor(d, "inactiveCaption", "nimbusBlueGrey", -0.00505054f, -0.055526316f, 0.039215684f, 0);

        //Font palette
        d.put("defaultFont", new FontUIResource(defaultFont));
        d.put("InternalFrame.titleFont", new DerivedFont("defaultFont", 1.0f, true, null));

        //Border palette

        //The global style definition
        addColor(d, "textForeground", "text", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "textBackground", "nimbusSelectionBackground", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "background", "control", 0.0f, 0.0f, 0.0f, 0);
        d.put("TitledBorder.position", "ABOVE_TOP");
        d.put("FileView.fullRowSelection", Boolean.TRUE);

        //Initialize ArrowButton
        d.put("ArrowButton.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("ArrowButton.size", Integer.valueOf(16));
        d.put("ArrowButton[Disabled].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ArrowButtonPainter", ArrowButtonPainter.FOREGROUND_DISABLED, new Insets(0, 0, 0, 0), new Dimension(10, 10), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("ArrowButton[Enabled].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ArrowButtonPainter", ArrowButtonPainter.FOREGROUND_ENABLED, new Insets(0, 0, 0, 0), new Dimension(10, 10), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));

        //Initialize Button
        d.put("Button.contentMargins", new InsetsUIResource(6, 14, 6, 14));
        d.put("Button.defaultButtonFollowsFocus", Boolean.FALSE);
        d.put("Button[Default].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ButtonPainter", ButtonPainter.BACKGROUND_DEFAULT, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("Button[Default+Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ButtonPainter", ButtonPainter.BACKGROUND_DEFAULT_FOCUSED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("Button[Default+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ButtonPainter", ButtonPainter.BACKGROUND_MOUSEOVER_DEFAULT, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("Button[Default+Focused+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ButtonPainter", ButtonPainter.BACKGROUND_MOUSEOVER_DEFAULT_FOCUSED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        addColor(d, "Button[Default+Pressed].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);
        d.put("Button[Default+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ButtonPainter", ButtonPainter.BACKGROUND_PRESSED_DEFAULT, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("Button[Default+Focused+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ButtonPainter", ButtonPainter.BACKGROUND_PRESSED_DEFAULT_FOCUSED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        addColor(d, "Button[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("Button[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ButtonPainter", ButtonPainter.BACKGROUND_DISABLED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("Button[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ButtonPainter", ButtonPainter.BACKGROUND_ENABLED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("Button[Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ButtonPainter", ButtonPainter.BACKGROUND_FOCUSED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("Button[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ButtonPainter", ButtonPainter.BACKGROUND_MOUSEOVER, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("Button[Focused+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ButtonPainter", ButtonPainter.BACKGROUND_MOUSEOVER_FOCUSED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("Button[Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ButtonPainter", ButtonPainter.BACKGROUND_PRESSED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("Button[Focused+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ButtonPainter", ButtonPainter.BACKGROUND_PRESSED_FOCUSED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));

        //Initialize ToggleButton
        d.put("ToggleButton.contentMargins", new InsetsUIResource(6, 14, 6, 14));
        addColor(d, "ToggleButton[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("ToggleButton[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_DISABLED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ToggleButton[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_ENABLED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ToggleButton[Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_FOCUSED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ToggleButton[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_MOUSEOVER, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ToggleButton[Focused+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_MOUSEOVER_FOCUSED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ToggleButton[Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_PRESSED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ToggleButton[Focused+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_PRESSED_FOCUSED, new Insets(7, 7, 7, 7), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ToggleButton[Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_SELECTED, new Insets(7, 7, 7, 7), new Dimension(72, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ToggleButton[Focused+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_SELECTED_FOCUSED, new Insets(7, 7, 7, 7), new Dimension(72, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ToggleButton[Pressed+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_PRESSED_SELECTED, new Insets(7, 7, 7, 7), new Dimension(72, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ToggleButton[Focused+Pressed+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_PRESSED_SELECTED_FOCUSED, new Insets(7, 7, 7, 7), new Dimension(72, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ToggleButton[MouseOver+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_MOUSEOVER_SELECTED, new Insets(7, 7, 7, 7), new Dimension(72, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ToggleButton[Focused+MouseOver+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_MOUSEOVER_SELECTED_FOCUSED, new Insets(7, 7, 7, 7), new Dimension(72, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        addColor(d, "ToggleButton[Disabled+Selected].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("ToggleButton[Disabled+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToggleButtonPainter", ToggleButtonPainter.BACKGROUND_DISABLED_SELECTED, new Insets(7, 7, 7, 7), new Dimension(72, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));

        //Initialize RadioButton
        d.put("RadioButton.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        addColor(d, "RadioButton[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("RadioButton[Disabled].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_DISABLED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton[Enabled].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton[Focused].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton[MouseOver].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_MOUSEOVER, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton[Focused+MouseOver].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_MOUSEOVER_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton[Pressed].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_PRESSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton[Focused+Pressed].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_PRESSED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton[Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_SELECTED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton[Focused+Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_SELECTED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton[Pressed+Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_PRESSED_SELECTED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton[Focused+Pressed+Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_PRESSED_SELECTED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton[MouseOver+Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_MOUSEOVER_SELECTED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton[Focused+MouseOver+Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_MOUSEOVER_SELECTED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton[Disabled+Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonPainter", RadioButtonPainter.ICON_DISABLED_SELECTED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButton.icon", new NimbusIcon("RadioButton", "iconPainter", 18, 18));

        //Initialize CheckBox
        d.put("CheckBox.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        addColor(d, "CheckBox[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("CheckBox[Disabled].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_DISABLED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox[Enabled].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox[Focused].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox[MouseOver].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_MOUSEOVER, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox[Focused+MouseOver].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_MOUSEOVER_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox[Pressed].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_PRESSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox[Focused+Pressed].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_PRESSED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox[Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_SELECTED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox[Focused+Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_SELECTED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox[Pressed+Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_PRESSED_SELECTED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox[Focused+Pressed+Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_PRESSED_SELECTED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox[MouseOver+Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_MOUSEOVER_SELECTED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox[Focused+MouseOver+Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_MOUSEOVER_SELECTED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox[Disabled+Selected].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxPainter", CheckBoxPainter.ICON_DISABLED_SELECTED, new Insets(5, 5, 5, 5), new Dimension(18, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBox.icon", new NimbusIcon("CheckBox", "iconPainter", 18, 18));

        //Initialize ColorChooser
        d.put("ColorChooser.contentMargins", new InsetsUIResource(5, 0, 0, 0));
        addColor(d, "ColorChooser.swatchesDefaultRecentColor", 255, 255, 255, 255);
        d.put("ColorChooser:\"ColorChooser.previewPanelHolder\".contentMargins", new InsetsUIResource(0, 5, 10, 5));
        d.put("ColorChooser:\"ColorChooser.previewPanelHolder\":\"OptionPane.label\".contentMargins", new InsetsUIResource(0, 10, 10, 10));

        //Initialize ComboBox
        d.put("ComboBox.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("ComboBox.States", "Enabled,MouseOver,Pressed,Selected,Disabled,Focused,Editable");
        d.put("ComboBox.Editable", new ComboBoxEditableState());
        d.put("ComboBox.forceOpaque", Boolean.TRUE);
        d.put("ComboBox.buttonWhenNotEditable", Boolean.TRUE);
        d.put("ComboBox.rendererUseListColors", Boolean.FALSE);
        d.put("ComboBox.pressedWhenPopupVisible", Boolean.TRUE);
        d.put("ComboBox.squareButton", Boolean.FALSE);
        d.put("ComboBox.popupInsets", new InsetsUIResource(-2, 2, 0, 2));
        d.put("ComboBox.padding", new InsetsUIResource(3, 3, 3, 3));
        d.put("ComboBox[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_DISABLED, new Insets(8, 9, 8, 19), new Dimension(83, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox[Disabled+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_DISABLED_PRESSED, new Insets(8, 9, 8, 19), new Dimension(83, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_ENABLED, new Insets(8, 9, 8, 19), new Dimension(83, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox[Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_FOCUSED, new Insets(8, 9, 8, 19), new Dimension(83, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox[Focused+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_MOUSEOVER_FOCUSED, new Insets(8, 9, 8, 19), new Dimension(83, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_MOUSEOVER, new Insets(8, 9, 8, 19), new Dimension(83, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox[Focused+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_PRESSED_FOCUSED, new Insets(8, 9, 8, 19), new Dimension(83, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox[Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_PRESSED, new Insets(8, 9, 8, 19), new Dimension(83, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox[Enabled+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_ENABLED_SELECTED, new Insets(8, 9, 8, 19), new Dimension(83, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox[Disabled+Editable].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_DISABLED_EDITABLE, new Insets(6, 5, 6, 17), new Dimension(79, 21), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox[Editable+Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_ENABLED_EDITABLE, new Insets(6, 5, 6, 17), new Dimension(79, 21), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox[Editable+Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_FOCUSED_EDITABLE, new Insets(5, 5, 5, 5), new Dimension(142, 27), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox[Editable+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_MOUSEOVER_EDITABLE, new Insets(4, 5, 5, 17), new Dimension(79, 21), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox[Editable+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxPainter", ComboBoxPainter.BACKGROUND_PRESSED_EDITABLE, new Insets(4, 5, 5, 17), new Dimension(79, 21), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox:\"ComboBox.textField\".contentMargins", new InsetsUIResource(0, 6, 0, 3));
        addColor(d, "ComboBox:\"ComboBox.textField\"[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("ComboBox:\"ComboBox.textField\"[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxTextFieldPainter", ComboBoxTextFieldPainter.BACKGROUND_DISABLED, new Insets(5, 3, 3, 1), new Dimension(64, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox:\"ComboBox.textField\"[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxTextFieldPainter", ComboBoxTextFieldPainter.BACKGROUND_ENABLED, new Insets(5, 3, 3, 1), new Dimension(64, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        addColor(d, "ComboBox:\"ComboBox.textField\"[Selected].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);
        d.put("ComboBox:\"ComboBox.textField\"[Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxTextFieldPainter", ComboBoxTextFieldPainter.BACKGROUND_SELECTED, new Insets(5, 3, 3, 1), new Dimension(64, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox:\"ComboBox.arrowButton\".contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("ComboBox:\"ComboBox.arrowButton\".States", "Enabled,MouseOver,Pressed,Disabled,Editable");
        d.put("ComboBox:\"ComboBox.arrowButton\".Editable", new ComboBoxArrowButtonEditableState());
        d.put("ComboBox:\"ComboBox.arrowButton\".size", Integer.valueOf(19));
        d.put("ComboBox:\"ComboBox.arrowButton\"[Disabled+Editable].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxArrowButtonPainter", ComboBoxArrowButtonPainter.BACKGROUND_DISABLED_EDITABLE, new Insets(8, 1, 8, 8), new Dimension(20, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox:\"ComboBox.arrowButton\"[Editable+Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxArrowButtonPainter", ComboBoxArrowButtonPainter.BACKGROUND_ENABLED_EDITABLE, new Insets(8, 1, 8, 8), new Dimension(20, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox:\"ComboBox.arrowButton\"[Editable+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxArrowButtonPainter", ComboBoxArrowButtonPainter.BACKGROUND_MOUSEOVER_EDITABLE, new Insets(8, 1, 8, 8), new Dimension(20, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox:\"ComboBox.arrowButton\"[Editable+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxArrowButtonPainter", ComboBoxArrowButtonPainter.BACKGROUND_PRESSED_EDITABLE, new Insets(8, 1, 8, 8), new Dimension(20, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox:\"ComboBox.arrowButton\"[Editable+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxArrowButtonPainter", ComboBoxArrowButtonPainter.BACKGROUND_SELECTED_EDITABLE, new Insets(8, 1, 8, 8), new Dimension(20, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox:\"ComboBox.arrowButton\"[Enabled].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxArrowButtonPainter", ComboBoxArrowButtonPainter.FOREGROUND_ENABLED, new Insets(6, 9, 6, 10), new Dimension(24, 19), true, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox:\"ComboBox.arrowButton\"[MouseOver].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxArrowButtonPainter", ComboBoxArrowButtonPainter.FOREGROUND_MOUSEOVER, new Insets(6, 9, 6, 10), new Dimension(24, 19), true, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox:\"ComboBox.arrowButton\"[Disabled].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxArrowButtonPainter", ComboBoxArrowButtonPainter.FOREGROUND_DISABLED, new Insets(6, 9, 6, 10), new Dimension(24, 19), true, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox:\"ComboBox.arrowButton\"[Pressed].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxArrowButtonPainter", ComboBoxArrowButtonPainter.FOREGROUND_PRESSED, new Insets(6, 9, 6, 10), new Dimension(24, 19), true, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox:\"ComboBox.arrowButton\"[Selected].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ComboBoxArrowButtonPainter", ComboBoxArrowButtonPainter.FOREGROUND_SELECTED, new Insets(6, 9, 6, 10), new Dimension(24, 19), true, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ComboBox:\"ComboBox.listRenderer\".contentMargins", new InsetsUIResource(2, 4, 2, 4));
        d.put("ComboBox:\"ComboBox.listRenderer\".opaque", Boolean.TRUE);
        addColor(d, "ComboBox:\"ComboBox.listRenderer\".background", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "ComboBox:\"ComboBox.listRenderer\"[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "ComboBox:\"ComboBox.listRenderer\"[Selected].background", "nimbusSelectionBackground", 0.0f, 0.0f, 0.0f, 0);
        d.put("ComboBox:\"ComboBox.renderer\".contentMargins", new InsetsUIResource(2, 4, 2, 4));
        addColor(d, "ComboBox:\"ComboBox.renderer\"[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "ComboBox:\"ComboBox.renderer\"[Selected].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "ComboBox:\"ComboBox.renderer\"[Selected].background", "nimbusSelectionBackground", 0.0f, 0.0f, 0.0f, 0);

        //Initialize \"ComboBox.scrollPane\"
        d.put("\"ComboBox.scrollPane\".contentMargins", new InsetsUIResource(0, 0, 0, 0));

        //Initialize FileChooser
        d.put("FileChooser.contentMargins", new InsetsUIResource(10, 10, 10, 10));
        d.put("FileChooser.opaque", Boolean.TRUE);
        d.put("FileChooser.usesSingleFilePane", Boolean.TRUE);
        d.put("FileChooser[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.FileChooserPainter", FileChooserPainter.BACKGROUND_ENABLED, new Insets(0, 0, 0, 0), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("FileChooser[Enabled].fileIconPainter", new LazyPainter("javax.swing.plaf.nimbus.FileChooserPainter", FileChooserPainter.FILEICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(16, 16), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("FileChooser.fileIcon", new NimbusIcon("FileChooser", "fileIconPainter", 16, 16));
        d.put("FileChooser[Enabled].directoryIconPainter", new LazyPainter("javax.swing.plaf.nimbus.FileChooserPainter", FileChooserPainter.DIRECTORYICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(16, 16), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("FileChooser.directoryIcon", new NimbusIcon("FileChooser", "directoryIconPainter", 16, 16));
        d.put("FileChooser[Enabled].upFolderIconPainter", new LazyPainter("javax.swing.plaf.nimbus.FileChooserPainter", FileChooserPainter.UPFOLDERICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(16, 16), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("FileChooser.upFolderIcon", new NimbusIcon("FileChooser", "upFolderIconPainter", 16, 16));
        d.put("FileChooser[Enabled].newFolderIconPainter", new LazyPainter("javax.swing.plaf.nimbus.FileChooserPainter", FileChooserPainter.NEWFOLDERICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(16, 16), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("FileChooser.newFolderIcon", new NimbusIcon("FileChooser", "newFolderIconPainter", 16, 16));
        d.put("FileChooser[Enabled].hardDriveIconPainter", new LazyPainter("javax.swing.plaf.nimbus.FileChooserPainter", FileChooserPainter.HARDDRIVEICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(16, 16), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("FileChooser.hardDriveIcon", new NimbusIcon("FileChooser", "hardDriveIconPainter", 16, 16));
        d.put("FileChooser[Enabled].floppyDriveIconPainter", new LazyPainter("javax.swing.plaf.nimbus.FileChooserPainter", FileChooserPainter.FLOPPYDRIVEICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(16, 16), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("FileChooser.floppyDriveIcon", new NimbusIcon("FileChooser", "floppyDriveIconPainter", 16, 16));
        d.put("FileChooser[Enabled].homeFolderIconPainter", new LazyPainter("javax.swing.plaf.nimbus.FileChooserPainter", FileChooserPainter.HOMEFOLDERICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(16, 16), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("FileChooser.homeFolderIcon", new NimbusIcon("FileChooser", "homeFolderIconPainter", 16, 16));
        d.put("FileChooser[Enabled].detailsViewIconPainter", new LazyPainter("javax.swing.plaf.nimbus.FileChooserPainter", FileChooserPainter.DETAILSVIEWICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(16, 16), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("FileChooser.detailsViewIcon", new NimbusIcon("FileChooser", "detailsViewIconPainter", 16, 16));
        d.put("FileChooser[Enabled].listViewIconPainter", new LazyPainter("javax.swing.plaf.nimbus.FileChooserPainter", FileChooserPainter.LISTVIEWICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(16, 16), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("FileChooser.listViewIcon", new NimbusIcon("FileChooser", "listViewIconPainter", 16, 16));

        //Initialize InternalFrameTitlePane
        d.put("InternalFrameTitlePane.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("InternalFrameTitlePane.maxFrameIconSize", new DimensionUIResource(18, 18));

        //Initialize InternalFrame
        d.put("InternalFrame.contentMargins", new InsetsUIResource(1, 6, 6, 6));
        d.put("InternalFrame.States", "Enabled,WindowFocused");
        d.put("InternalFrame.WindowFocused", new InternalFrameWindowFocusedState());
        d.put("InternalFrame[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFramePainter", InternalFramePainter.BACKGROUND_ENABLED, new Insets(25, 6, 6, 6), new Dimension(25, 36), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame[Enabled+WindowFocused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFramePainter", InternalFramePainter.BACKGROUND_ENABLED_WINDOWFOCUSED, new Insets(25, 6, 6, 6), new Dimension(25, 36), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane.contentMargins", new InsetsUIResource(3, 0, 3, 0));
        d.put("InternalFrame:InternalFrameTitlePane.States", "Enabled,WindowFocused");
        d.put("InternalFrame:InternalFrameTitlePane.WindowFocused", new InternalFrameTitlePaneWindowFocusedState());
        d.put("InternalFrame:InternalFrameTitlePane.titleAlignment", "CENTER");
        addColor(d, "InternalFrame:InternalFrameTitlePane[Enabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\".contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\".States", "Enabled,MouseOver,Pressed,Disabled,Focused,Selected,WindowNotFocused");
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\".WindowNotFocused", new InternalFrameTitlePaneMenuButtonWindowNotFocusedState());
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\".test", "am InternalFrameTitlePane.menuButton");
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\"[Enabled].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMenuButtonPainter", InternalFrameTitlePaneMenuButtonPainter.ICON_ENABLED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\"[Disabled].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMenuButtonPainter", InternalFrameTitlePaneMenuButtonPainter.ICON_DISABLED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\"[MouseOver].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMenuButtonPainter", InternalFrameTitlePaneMenuButtonPainter.ICON_MOUSEOVER, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\"[Pressed].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMenuButtonPainter", InternalFrameTitlePaneMenuButtonPainter.ICON_PRESSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\"[Enabled+WindowNotFocused].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMenuButtonPainter", InternalFrameTitlePaneMenuButtonPainter.ICON_ENABLED_WINDOWNOTFOCUSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\"[MouseOver+WindowNotFocused].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMenuButtonPainter", InternalFrameTitlePaneMenuButtonPainter.ICON_MOUSEOVER_WINDOWNOTFOCUSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\"[Pressed+WindowNotFocused].iconPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMenuButtonPainter", InternalFrameTitlePaneMenuButtonPainter.ICON_PRESSED_WINDOWNOTFOCUSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\".icon", new NimbusIcon("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.menuButton\"", "iconPainter", 19, 18));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.iconifyButton\".contentMargins", new InsetsUIResource(9, 9, 9, 9));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.iconifyButton\".States", "Enabled,MouseOver,Pressed,Disabled,Focused,Selected,WindowNotFocused");
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.iconifyButton\".WindowNotFocused", new InternalFrameTitlePaneIconifyButtonWindowNotFocusedState());
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.iconifyButton\"[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneIconifyButtonPainter", InternalFrameTitlePaneIconifyButtonPainter.BACKGROUND_ENABLED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.iconifyButton\"[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneIconifyButtonPainter", InternalFrameTitlePaneIconifyButtonPainter.BACKGROUND_DISABLED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.iconifyButton\"[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneIconifyButtonPainter", InternalFrameTitlePaneIconifyButtonPainter.BACKGROUND_MOUSEOVER, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.iconifyButton\"[Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneIconifyButtonPainter", InternalFrameTitlePaneIconifyButtonPainter.BACKGROUND_PRESSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.iconifyButton\"[Enabled+WindowNotFocused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneIconifyButtonPainter", InternalFrameTitlePaneIconifyButtonPainter.BACKGROUND_ENABLED_WINDOWNOTFOCUSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.iconifyButton\"[MouseOver+WindowNotFocused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneIconifyButtonPainter", InternalFrameTitlePaneIconifyButtonPainter.BACKGROUND_MOUSEOVER_WINDOWNOTFOCUSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.iconifyButton\"[Pressed+WindowNotFocused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneIconifyButtonPainter", InternalFrameTitlePaneIconifyButtonPainter.BACKGROUND_PRESSED_WINDOWNOTFOCUSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\".contentMargins", new InsetsUIResource(9, 9, 9, 9));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\".States", "Enabled,MouseOver,Pressed,Disabled,Focused,Selected,WindowNotFocused,WindowMaximized");
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\".WindowNotFocused", new InternalFrameTitlePaneMaximizeButtonWindowNotFocusedState());
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\".WindowMaximized", new InternalFrameTitlePaneMaximizeButtonWindowMaximizedState());
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[Disabled+WindowMaximized].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_DISABLED_WINDOWMAXIMIZED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[Enabled+WindowMaximized].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_ENABLED_WINDOWMAXIMIZED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[MouseOver+WindowMaximized].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_MOUSEOVER_WINDOWMAXIMIZED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[Pressed+WindowMaximized].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_PRESSED_WINDOWMAXIMIZED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[Enabled+WindowMaximized+WindowNotFocused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_ENABLED_WINDOWNOTFOCUSED_WINDOWMAXIMIZED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[MouseOver+WindowMaximized+WindowNotFocused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_MOUSEOVER_WINDOWNOTFOCUSED_WINDOWMAXIMIZED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[Pressed+WindowMaximized+WindowNotFocused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_PRESSED_WINDOWNOTFOCUSED_WINDOWMAXIMIZED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_DISABLED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_ENABLED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_MOUSEOVER, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_PRESSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[Enabled+WindowNotFocused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_ENABLED_WINDOWNOTFOCUSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[MouseOver+WindowNotFocused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_MOUSEOVER_WINDOWNOTFOCUSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.maximizeButton\"[Pressed+WindowNotFocused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter", InternalFrameTitlePaneMaximizeButtonPainter.BACKGROUND_PRESSED_WINDOWNOTFOCUSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.closeButton\".contentMargins", new InsetsUIResource(9, 9, 9, 9));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.closeButton\".States", "Enabled,MouseOver,Pressed,Disabled,Focused,Selected,WindowNotFocused");
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.closeButton\".WindowNotFocused", new InternalFrameTitlePaneCloseButtonWindowNotFocusedState());
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.closeButton\"[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneCloseButtonPainter", InternalFrameTitlePaneCloseButtonPainter.BACKGROUND_DISABLED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.closeButton\"[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneCloseButtonPainter", InternalFrameTitlePaneCloseButtonPainter.BACKGROUND_ENABLED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.closeButton\"[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneCloseButtonPainter", InternalFrameTitlePaneCloseButtonPainter.BACKGROUND_MOUSEOVER, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.closeButton\"[Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneCloseButtonPainter", InternalFrameTitlePaneCloseButtonPainter.BACKGROUND_PRESSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.closeButton\"[Enabled+WindowNotFocused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneCloseButtonPainter", InternalFrameTitlePaneCloseButtonPainter.BACKGROUND_ENABLED_WINDOWNOTFOCUSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.closeButton\"[MouseOver+WindowNotFocused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneCloseButtonPainter", InternalFrameTitlePaneCloseButtonPainter.BACKGROUND_MOUSEOVER_WINDOWNOTFOCUSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("InternalFrame:InternalFrameTitlePane:\"InternalFrameTitlePane.closeButton\"[Pressed+WindowNotFocused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.InternalFrameTitlePaneCloseButtonPainter", InternalFrameTitlePaneCloseButtonPainter.BACKGROUND_PRESSED_WINDOWNOTFOCUSED, new Insets(0, 0, 0, 0), new Dimension(19, 18), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        //Initialize DesktopIcon
        d.put("DesktopIcon.contentMargins", new InsetsUIResource(4, 6, 5, 4));
        d.put("DesktopIcon[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.DesktopIconPainter", DesktopIconPainter.BACKGROUND_ENABLED, new Insets(5, 5, 5, 5), new Dimension(28, 26), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        //Initialize DesktopPane
        d.put("DesktopPane.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("DesktopPane.opaque", Boolean.TRUE);
        d.put("DesktopPane[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.DesktopPanePainter", DesktopPanePainter.BACKGROUND_ENABLED, new Insets(0, 0, 0, 0), new Dimension(300, 232), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        //Initialize Label
        d.put("Label.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        addColor(d, "Label[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);

        //Initialize List
        d.put("List.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("List.opaque", Boolean.TRUE);
        addColor(d, "List.background", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
        d.put("List.rendererUseListColors", Boolean.FALSE);
        d.put("List.rendererUseUIBorder", Boolean.TRUE);
        d.put("List.cellNoFocusBorder", new BorderUIResource(BorderFactory.createEmptyBorder(2, 5, 2, 5)));
        d.put("List.focusCellHighlightBorder", new BorderUIResource(new PainterBorder("Tree:TreeCell[Enabled+Focused].backgroundPainter", new Insets(2, 5, 2, 5))));
        addColor(d, "List.dropLineColor", "nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "List[Selected].textForeground", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "List[Selected].textBackground", "nimbusSelectionBackground", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "List[Disabled+Selected].textBackground", "nimbusSelectionBackground", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "List[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("List:\"List.cellRenderer\".contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("List:\"List.cellRenderer\".opaque", Boolean.TRUE);
        addColor(d, "List:\"List.cellRenderer\"[Selected].textForeground", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "List:\"List.cellRenderer\"[Selected].background", "nimbusSelectionBackground", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "List:\"List.cellRenderer\"[Disabled+Selected].background", "nimbusSelectionBackground", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "List:\"List.cellRenderer\"[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);

        //Initialize MenuBar
        d.put("MenuBar.contentMargins", new InsetsUIResource(2, 6, 2, 6));
        d.put("MenuBar[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.MenuBarPainter", MenuBarPainter.BACKGROUND_ENABLED, new Insets(1, 0, 0, 0), new Dimension(18, 22), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("MenuBar[Enabled].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.MenuBarPainter", MenuBarPainter.BORDER_ENABLED, new Insets(0, 0, 1, 0), new Dimension(30, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("MenuBar:Menu.contentMargins", new InsetsUIResource(1, 4, 2, 4));
        addColor(d, "MenuBar:Menu[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "MenuBar:Menu[Enabled].textForeground", 35, 35, 36, 255);
        addColor(d, "MenuBar:Menu[Selected].textForeground", 255, 255, 255, 255);
        d.put("MenuBar:Menu[Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.MenuBarMenuPainter", MenuBarMenuPainter.BACKGROUND_SELECTED, new Insets(0, 0, 0, 0), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("MenuBar:Menu:MenuItemAccelerator.contentMargins", new InsetsUIResource(0, 0, 0, 0));

        //Initialize MenuItem
        d.put("MenuItem.contentMargins", new InsetsUIResource(1, 12, 2, 13));
        d.put("MenuItem.textIconGap", Integer.valueOf(5));
        addColor(d, "MenuItem[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "MenuItem[Enabled].textForeground", 35, 35, 36, 255);
        addColor(d, "MenuItem[MouseOver].textForeground", 255, 255, 255, 255);
        d.put("MenuItem[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.MenuItemPainter", MenuItemPainter.BACKGROUND_MOUSEOVER, new Insets(0, 0, 0, 0), new Dimension(100, 3), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("MenuItem:MenuItemAccelerator.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        addColor(d, "MenuItem:MenuItemAccelerator[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "MenuItem:MenuItemAccelerator[MouseOver].textForeground", 255, 255, 255, 255);

        //Initialize RadioButtonMenuItem
        d.put("RadioButtonMenuItem.contentMargins", new InsetsUIResource(1, 12, 2, 13));
        d.put("RadioButtonMenuItem.textIconGap", Integer.valueOf(5));
        addColor(d, "RadioButtonMenuItem[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "RadioButtonMenuItem[Enabled].textForeground", 35, 35, 36, 255);
        addColor(d, "RadioButtonMenuItem[MouseOver].textForeground", 255, 255, 255, 255);
        d.put("RadioButtonMenuItem[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonMenuItemPainter", RadioButtonMenuItemPainter.BACKGROUND_MOUSEOVER, new Insets(0, 0, 0, 0), new Dimension(100, 3), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        addColor(d, "RadioButtonMenuItem[MouseOver+Selected].textForeground", 255, 255, 255, 255);
        d.put("RadioButtonMenuItem[MouseOver+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonMenuItemPainter", RadioButtonMenuItemPainter.BACKGROUND_SELECTED_MOUSEOVER, new Insets(0, 0, 0, 0), new Dimension(100, 3), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("RadioButtonMenuItem[Disabled+Selected].checkIconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonMenuItemPainter", RadioButtonMenuItemPainter.CHECKICON_DISABLED_SELECTED, new Insets(5, 5, 5, 5), new Dimension(9, 10), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButtonMenuItem[Enabled+Selected].checkIconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonMenuItemPainter", RadioButtonMenuItemPainter.CHECKICON_ENABLED_SELECTED, new Insets(5, 5, 5, 5), new Dimension(9, 10), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButtonMenuItem[MouseOver+Selected].checkIconPainter", new LazyPainter("javax.swing.plaf.nimbus.RadioButtonMenuItemPainter", RadioButtonMenuItemPainter.CHECKICON_SELECTED_MOUSEOVER, new Insets(5, 5, 5, 5), new Dimension(9, 10), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("RadioButtonMenuItem.checkIcon", new NimbusIcon("RadioButtonMenuItem", "checkIconPainter", 9, 10));
        d.put("RadioButtonMenuItem:MenuItemAccelerator.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        addColor(d, "RadioButtonMenuItem:MenuItemAccelerator[MouseOver].textForeground", 255, 255, 255, 255);

        //Initialize CheckBoxMenuItem
        d.put("CheckBoxMenuItem.contentMargins", new InsetsUIResource(1, 12, 2, 13));
        d.put("CheckBoxMenuItem.textIconGap", Integer.valueOf(5));
        addColor(d, "CheckBoxMenuItem[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "CheckBoxMenuItem[Enabled].textForeground", 35, 35, 36, 255);
        addColor(d, "CheckBoxMenuItem[MouseOver].textForeground", 255, 255, 255, 255);
        d.put("CheckBoxMenuItem[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxMenuItemPainter", CheckBoxMenuItemPainter.BACKGROUND_MOUSEOVER, new Insets(0, 0, 0, 0), new Dimension(100, 3), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        addColor(d, "CheckBoxMenuItem[MouseOver+Selected].textForeground", 255, 255, 255, 255);
        d.put("CheckBoxMenuItem[MouseOver+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxMenuItemPainter", CheckBoxMenuItemPainter.BACKGROUND_SELECTED_MOUSEOVER, new Insets(0, 0, 0, 0), new Dimension(100, 3), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("CheckBoxMenuItem[Disabled+Selected].checkIconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxMenuItemPainter", CheckBoxMenuItemPainter.CHECKICON_DISABLED_SELECTED, new Insets(5, 5, 5, 5), new Dimension(9, 10), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBoxMenuItem[Enabled+Selected].checkIconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxMenuItemPainter", CheckBoxMenuItemPainter.CHECKICON_ENABLED_SELECTED, new Insets(5, 5, 5, 5), new Dimension(9, 10), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBoxMenuItem[MouseOver+Selected].checkIconPainter", new LazyPainter("javax.swing.plaf.nimbus.CheckBoxMenuItemPainter", CheckBoxMenuItemPainter.CHECKICON_SELECTED_MOUSEOVER, new Insets(5, 5, 5, 5), new Dimension(9, 10), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("CheckBoxMenuItem.checkIcon", new NimbusIcon("CheckBoxMenuItem", "checkIconPainter", 9, 10));
        d.put("CheckBoxMenuItem:MenuItemAccelerator.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        addColor(d, "CheckBoxMenuItem:MenuItemAccelerator[MouseOver].textForeground", 255, 255, 255, 255);

        //Initialize Menu
        d.put("Menu.contentMargins", new InsetsUIResource(1, 12, 2, 5));
        d.put("Menu.textIconGap", Integer.valueOf(5));
        addColor(d, "Menu[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "Menu[Enabled].textForeground", 35, 35, 36, 255);
        addColor(d, "Menu[Enabled+Selected].textForeground", 255, 255, 255, 255);
        d.put("Menu[Enabled+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.MenuPainter", MenuPainter.BACKGROUND_ENABLED_SELECTED, new Insets(0, 0, 0, 0), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("Menu[Disabled].arrowIconPainter", new LazyPainter("javax.swing.plaf.nimbus.MenuPainter", MenuPainter.ARROWICON_DISABLED, new Insets(5, 5, 5, 5), new Dimension(9, 10), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Menu[Enabled].arrowIconPainter", new LazyPainter("javax.swing.plaf.nimbus.MenuPainter", MenuPainter.ARROWICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(9, 10), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Menu[Enabled+Selected].arrowIconPainter", new LazyPainter("javax.swing.plaf.nimbus.MenuPainter", MenuPainter.ARROWICON_ENABLED_SELECTED, new Insets(1, 1, 1, 1), new Dimension(9, 10), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Menu.arrowIcon", new NimbusIcon("Menu", "arrowIconPainter", 9, 10));
        d.put("Menu:MenuItemAccelerator.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        addColor(d, "Menu:MenuItemAccelerator[MouseOver].textForeground", 255, 255, 255, 255);

        //Initialize PopupMenu
        d.put("PopupMenu.contentMargins", new InsetsUIResource(6, 1, 6, 1));
        d.put("PopupMenu.opaque", Boolean.TRUE);
        d.put("PopupMenu.consumeEventOnClose", Boolean.TRUE);
        d.put("PopupMenu[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.PopupMenuPainter", PopupMenuPainter.BACKGROUND_DISABLED, new Insets(9, 0, 11, 0), new Dimension(220, 313), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("PopupMenu[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.PopupMenuPainter", PopupMenuPainter.BACKGROUND_ENABLED, new Insets(11, 2, 11, 2), new Dimension(220, 313), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));

        //Initialize PopupMenuSeparator
        d.put("PopupMenuSeparator.contentMargins", new InsetsUIResource(1, 0, 2, 0));
        d.put("PopupMenuSeparator[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.PopupMenuSeparatorPainter", PopupMenuSeparatorPainter.BACKGROUND_ENABLED, new Insets(1, 1, 1, 1), new Dimension(3, 3), true, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));

        //Initialize OptionPane
        d.put("OptionPane.contentMargins", new InsetsUIResource(15, 15, 15, 15));
        d.put("OptionPane.opaque", Boolean.TRUE);
        d.put("OptionPane.buttonOrientation", Integer.valueOf(4));
        d.put("OptionPane.messageAnchor", Integer.valueOf(17));
        d.put("OptionPane.separatorPadding", Integer.valueOf(0));
        d.put("OptionPane.sameSizeButtons", Boolean.FALSE);
        d.put("OptionPane:\"OptionPane.separator\".contentMargins", new InsetsUIResource(1, 0, 0, 0));
        d.put("OptionPane:\"OptionPane.messageArea\".contentMargins", new InsetsUIResource(0, 0, 10, 0));
        d.put("OptionPane:\"OptionPane.messageArea\":\"OptionPane.label\".contentMargins", new InsetsUIResource(0, 10, 10, 10));
        d.put("OptionPane:\"OptionPane.messageArea\":\"OptionPane.label\"[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.OptionPaneMessageAreaOptionPaneLabelPainter", OptionPaneMessageAreaOptionPaneLabelPainter.BACKGROUND_ENABLED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("OptionPane[Enabled].errorIconPainter", new LazyPainter("javax.swing.plaf.nimbus.OptionPanePainter", OptionPanePainter.ERRORICON_ENABLED, new Insets(0, 0, 0, 0), new Dimension(48, 48), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("OptionPane.errorIcon", new NimbusIcon("OptionPane", "errorIconPainter", 48, 48));
        d.put("OptionPane[Enabled].informationIconPainter", new LazyPainter("javax.swing.plaf.nimbus.OptionPanePainter", OptionPanePainter.INFORMATIONICON_ENABLED, new Insets(0, 0, 0, 0), new Dimension(48, 48), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("OptionPane.informationIcon", new NimbusIcon("OptionPane", "informationIconPainter", 48, 48));
        d.put("OptionPane[Enabled].questionIconPainter", new LazyPainter("javax.swing.plaf.nimbus.OptionPanePainter", OptionPanePainter.QUESTIONICON_ENABLED, new Insets(0, 0, 0, 0), new Dimension(48, 48), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("OptionPane.questionIcon", new NimbusIcon("OptionPane", "questionIconPainter", 48, 48));
        d.put("OptionPane[Enabled].warningIconPainter", new LazyPainter("javax.swing.plaf.nimbus.OptionPanePainter", OptionPanePainter.WARNINGICON_ENABLED, new Insets(0, 0, 0, 0), new Dimension(48, 48), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("OptionPane.warningIcon", new NimbusIcon("OptionPane", "warningIconPainter", 48, 48));

        //Initialize Panel
        d.put("Panel.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("Panel.opaque", Boolean.TRUE);

        //Initialize ProgressBar
        d.put("ProgressBar.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("ProgressBar.States", "Enabled,Disabled,Indeterminate,Finished");
        d.put("ProgressBar.Indeterminate", new ProgressBarIndeterminateState());
        d.put("ProgressBar.Finished", new ProgressBarFinishedState());
        d.put("ProgressBar.tileWhenIndeterminate", Boolean.TRUE);
        d.put("ProgressBar.tileWidth", Integer.valueOf(27));
        d.put("ProgressBar.paintOutsideClip", Boolean.TRUE);
        d.put("ProgressBar.rotateText", Boolean.TRUE);
        d.put("ProgressBar.vertictalSize", new DimensionUIResource(19, 150));
        d.put("ProgressBar.horizontalSize", new DimensionUIResource(150, 19));
        d.put("ProgressBar.cycleTime", Integer.valueOf(250));
        d.put("ProgressBar.minBarSize", new DimensionUIResource(6, 6));
        d.put("ProgressBar.glowWidth", Integer.valueOf(2));
        d.put("ProgressBar[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ProgressBarPainter", ProgressBarPainter.BACKGROUND_ENABLED, new Insets(5, 5, 5, 5), new Dimension(29, 19), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        addColor(d, "ProgressBar[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("ProgressBar[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ProgressBarPainter", ProgressBarPainter.BACKGROUND_DISABLED, new Insets(5, 5, 5, 5), new Dimension(29, 19), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("ProgressBar[Enabled].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ProgressBarPainter", ProgressBarPainter.FOREGROUND_ENABLED, new Insets(3, 3, 3, 3), new Dimension(27, 19), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ProgressBar[Enabled+Finished].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ProgressBarPainter", ProgressBarPainter.FOREGROUND_ENABLED_FINISHED, new Insets(3, 3, 3, 3), new Dimension(27, 19), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ProgressBar[Enabled+Indeterminate].progressPadding", Integer.valueOf(3));
        d.put("ProgressBar[Enabled+Indeterminate].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ProgressBarPainter", ProgressBarPainter.FOREGROUND_ENABLED_INDETERMINATE, new Insets(3, 3, 3, 3), new Dimension(30, 13), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ProgressBar[Disabled].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ProgressBarPainter", ProgressBarPainter.FOREGROUND_DISABLED, new Insets(3, 3, 3, 3), new Dimension(27, 19), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ProgressBar[Disabled+Finished].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ProgressBarPainter", ProgressBarPainter.FOREGROUND_DISABLED_FINISHED, new Insets(3, 3, 3, 3), new Dimension(27, 19), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ProgressBar[Disabled+Indeterminate].progressPadding", Integer.valueOf(3));
        d.put("ProgressBar[Disabled+Indeterminate].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ProgressBarPainter", ProgressBarPainter.FOREGROUND_DISABLED_INDETERMINATE, new Insets(3, 3, 3, 3), new Dimension(30, 13), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));

        //Initialize Separator
        d.put("Separator.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("Separator[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SeparatorPainter", SeparatorPainter.BACKGROUND_ENABLED, new Insets(0, 40, 0, 40), new Dimension(100, 3), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));

        //Initialize ScrollBar
        d.put("ScrollBar.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("ScrollBar.opaque", Boolean.TRUE);
        d.put("ScrollBar.incrementButtonGap", Integer.valueOf(-8));
        d.put("ScrollBar.decrementButtonGap", Integer.valueOf(-8));
        d.put("ScrollBar.thumbHeight", Integer.valueOf(15));
        d.put("ScrollBar.minimumThumbSize", new DimensionUIResource(29, 29));
        d.put("ScrollBar.maximumThumbSize", new DimensionUIResource(1000, 1000));
        d.put("ScrollBar:\"ScrollBar.button\".contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("ScrollBar:\"ScrollBar.button\".size", Integer.valueOf(25));
        d.put("ScrollBar:\"ScrollBar.button\"[Enabled].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ScrollBarButtonPainter", ScrollBarButtonPainter.FOREGROUND_ENABLED, new Insets(1, 1, 1, 1), new Dimension(25, 15), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("ScrollBar:\"ScrollBar.button\"[Disabled].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ScrollBarButtonPainter", ScrollBarButtonPainter.FOREGROUND_DISABLED, new Insets(1, 1, 1, 1), new Dimension(25, 15), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("ScrollBar:\"ScrollBar.button\"[MouseOver].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ScrollBarButtonPainter", ScrollBarButtonPainter.FOREGROUND_MOUSEOVER, new Insets(1, 1, 1, 1), new Dimension(25, 15), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("ScrollBar:\"ScrollBar.button\"[Pressed].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ScrollBarButtonPainter", ScrollBarButtonPainter.FOREGROUND_PRESSED, new Insets(1, 1, 1, 1), new Dimension(25, 15), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("ScrollBar:ScrollBarThumb.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("ScrollBar:ScrollBarThumb[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ScrollBarThumbPainter", ScrollBarThumbPainter.BACKGROUND_ENABLED, new Insets(0, 15, 0, 15), new Dimension(38, 15), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ScrollBar:ScrollBarThumb[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ScrollBarThumbPainter", ScrollBarThumbPainter.BACKGROUND_MOUSEOVER, new Insets(0, 15, 0, 15), new Dimension(38, 15), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ScrollBar:ScrollBarThumb[Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ScrollBarThumbPainter", ScrollBarThumbPainter.BACKGROUND_PRESSED, new Insets(0, 15, 0, 15), new Dimension(38, 15), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ScrollBar:ScrollBarTrack.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("ScrollBar:ScrollBarTrack[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ScrollBarTrackPainter", ScrollBarTrackPainter.BACKGROUND_DISABLED, new Insets(5, 5, 5, 5), new Dimension(18, 15), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("ScrollBar:ScrollBarTrack[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ScrollBarTrackPainter", ScrollBarTrackPainter.BACKGROUND_ENABLED, new Insets(5, 10, 5, 9), new Dimension(34, 15), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));

        //Initialize ScrollPane
        d.put("ScrollPane.contentMargins", new InsetsUIResource(3, 3, 3, 3));
        d.put("ScrollPane.useChildTextComponentFocus", Boolean.TRUE);
        d.put("ScrollPane[Enabled+Focused].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.ScrollPanePainter", ScrollPanePainter.BORDER_ENABLED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("ScrollPane[Enabled].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.ScrollPanePainter", ScrollPanePainter.BORDER_ENABLED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        //Initialize Viewport
        d.put("Viewport.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("Viewport.opaque", Boolean.TRUE);

        //Initialize Slider
        d.put("Slider.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("Slider.States", "Enabled,MouseOver,Pressed,Disabled,Focused,Selected,ArrowShape");
        d.put("Slider.ArrowShape", new SliderArrowShapeState());
        d.put("Slider.thumbWidth", Integer.valueOf(17));
        d.put("Slider.thumbHeight", Integer.valueOf(17));
        d.put("Slider.trackBorder", Integer.valueOf(0));
        d.put("Slider.paintValue", Boolean.FALSE);
        addColor(d, "Slider.tickColor", 35, 40, 48, 255);
        d.put("Slider:SliderThumb.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("Slider:SliderThumb.States", "Enabled,MouseOver,Pressed,Disabled,Focused,Selected,ArrowShape");
        d.put("Slider:SliderThumb.ArrowShape", new SliderThumbArrowShapeState());
        d.put("Slider:SliderThumb[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_DISABLED, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderThumb[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_ENABLED, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderThumb[Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderThumb[Focused+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_FOCUSED_MOUSEOVER, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderThumb[Focused+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_FOCUSED_PRESSED, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderThumb[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_MOUSEOVER, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderThumb[Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_PRESSED, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderThumb[ArrowShape+Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_ENABLED_ARROWSHAPE, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderThumb[ArrowShape+Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_DISABLED_ARROWSHAPE, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderThumb[ArrowShape+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_MOUSEOVER_ARROWSHAPE, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderThumb[ArrowShape+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_PRESSED_ARROWSHAPE, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderThumb[ArrowShape+Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_FOCUSED_ARROWSHAPE, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderThumb[ArrowShape+Focused+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_FOCUSED_MOUSEOVER_ARROWSHAPE, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderThumb[ArrowShape+Focused+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderThumbPainter", SliderThumbPainter.BACKGROUND_FOCUSED_PRESSED_ARROWSHAPE, new Insets(5, 5, 5, 5), new Dimension(17, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Slider:SliderTrack.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("Slider:SliderTrack.States", "Enabled,MouseOver,Pressed,Disabled,Focused,Selected,ArrowShape");
        d.put("Slider:SliderTrack.ArrowShape", new SliderTrackArrowShapeState());
        d.put("Slider:SliderTrack[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderTrackPainter", SliderTrackPainter.BACKGROUND_DISABLED, new Insets(6, 5, 6, 5), new Dimension(23, 17), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, 2.0));
        d.put("Slider:SliderTrack[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SliderTrackPainter", SliderTrackPainter.BACKGROUND_ENABLED, new Insets(6, 5, 6, 5), new Dimension(23, 17), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));

        //Initialize Spinner
        d.put("Spinner.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("Spinner:\"Spinner.editor\".contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("Spinner:Panel:\"Spinner.formattedTextField\".contentMargins", new InsetsUIResource(6, 6, 5, 6));
        addColor(d, "Spinner:Panel:\"Spinner.formattedTextField\"[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("Spinner:Panel:\"Spinner.formattedTextField\"[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPanelSpinnerFormattedTextFieldPainter", SpinnerPanelSpinnerFormattedTextFieldPainter.BACKGROUND_DISABLED, new Insets(5, 3, 3, 1), new Dimension(64, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("Spinner:Panel:\"Spinner.formattedTextField\"[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPanelSpinnerFormattedTextFieldPainter", SpinnerPanelSpinnerFormattedTextFieldPainter.BACKGROUND_ENABLED, new Insets(5, 3, 3, 1), new Dimension(64, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("Spinner:Panel:\"Spinner.formattedTextField\"[Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPanelSpinnerFormattedTextFieldPainter", SpinnerPanelSpinnerFormattedTextFieldPainter.BACKGROUND_FOCUSED, new Insets(5, 3, 3, 1), new Dimension(64, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        addColor(d, "Spinner:Panel:\"Spinner.formattedTextField\"[Selected].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);
        d.put("Spinner:Panel:\"Spinner.formattedTextField\"[Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPanelSpinnerFormattedTextFieldPainter", SpinnerPanelSpinnerFormattedTextFieldPainter.BACKGROUND_SELECTED, new Insets(5, 3, 3, 1), new Dimension(64, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        addColor(d, "Spinner:Panel:\"Spinner.formattedTextField\"[Focused+Selected].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);
        d.put("Spinner:Panel:\"Spinner.formattedTextField\"[Focused+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPanelSpinnerFormattedTextFieldPainter", SpinnerPanelSpinnerFormattedTextFieldPainter.BACKGROUND_SELECTED_FOCUSED, new Insets(5, 3, 3, 1), new Dimension(64, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("Spinner:\"Spinner.previousButton\".contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("Spinner:\"Spinner.previousButton\".size", Integer.valueOf(20));
        d.put("Spinner:\"Spinner.previousButton\"[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.BACKGROUND_DISABLED, new Insets(0, 1, 6, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.previousButton\"[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.BACKGROUND_ENABLED, new Insets(0, 1, 6, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.previousButton\"[Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.BACKGROUND_FOCUSED, new Insets(0, 1, 6, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.previousButton\"[Focused+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.BACKGROUND_MOUSEOVER_FOCUSED, new Insets(3, 1, 6, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.previousButton\"[Focused+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.BACKGROUND_PRESSED_FOCUSED, new Insets(0, 1, 6, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.previousButton\"[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.BACKGROUND_MOUSEOVER, new Insets(0, 1, 6, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.previousButton\"[Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.BACKGROUND_PRESSED, new Insets(0, 1, 6, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.previousButton\"[Disabled].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.FOREGROUND_DISABLED, new Insets(3, 6, 5, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.previousButton\"[Enabled].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.FOREGROUND_ENABLED, new Insets(3, 6, 5, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.previousButton\"[Focused].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.FOREGROUND_FOCUSED, new Insets(3, 6, 5, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.previousButton\"[Focused+MouseOver].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.FOREGROUND_MOUSEOVER_FOCUSED, new Insets(3, 6, 5, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.previousButton\"[Focused+Pressed].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.FOREGROUND_PRESSED_FOCUSED, new Insets(3, 6, 5, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.previousButton\"[MouseOver].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.FOREGROUND_MOUSEOVER, new Insets(3, 6, 5, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.previousButton\"[Pressed].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter", SpinnerPreviousButtonPainter.FOREGROUND_PRESSED, new Insets(3, 6, 5, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\".contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("Spinner:\"Spinner.nextButton\".size", Integer.valueOf(20));
        d.put("Spinner:\"Spinner.nextButton\"[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.BACKGROUND_DISABLED, new Insets(7, 1, 1, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\"[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.BACKGROUND_ENABLED, new Insets(7, 1, 1, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\"[Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.BACKGROUND_FOCUSED, new Insets(7, 1, 1, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\"[Focused+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.BACKGROUND_MOUSEOVER_FOCUSED, new Insets(7, 1, 1, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\"[Focused+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.BACKGROUND_PRESSED_FOCUSED, new Insets(7, 1, 1, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\"[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.BACKGROUND_MOUSEOVER, new Insets(7, 1, 1, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\"[Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.BACKGROUND_PRESSED, new Insets(7, 1, 1, 7), new Dimension(20, 12), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\"[Disabled].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.FOREGROUND_DISABLED, new Insets(5, 6, 3, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\"[Enabled].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.FOREGROUND_ENABLED, new Insets(5, 6, 3, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\"[Focused].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.FOREGROUND_FOCUSED, new Insets(3, 6, 3, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\"[Focused+MouseOver].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.FOREGROUND_MOUSEOVER_FOCUSED, new Insets(3, 6, 3, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\"[Focused+Pressed].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.FOREGROUND_PRESSED_FOCUSED, new Insets(5, 6, 3, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\"[MouseOver].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.FOREGROUND_MOUSEOVER, new Insets(5, 6, 3, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Spinner:\"Spinner.nextButton\"[Pressed].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SpinnerNextButtonPainter", SpinnerNextButtonPainter.FOREGROUND_PRESSED, new Insets(5, 6, 3, 9), new Dimension(20, 12), true, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));

        //Initialize SplitPane
        d.put("SplitPane.contentMargins", new InsetsUIResource(1, 1, 1, 1));
        d.put("SplitPane.States", "Enabled,MouseOver,Pressed,Disabled,Focused,Selected,Vertical");
        d.put("SplitPane.Vertical", new SplitPaneVerticalState());
        d.put("SplitPane.size", Integer.valueOf(10));
        d.put("SplitPane.dividerSize", Integer.valueOf(10));
        d.put("SplitPane.centerOneTouchButtons", Boolean.TRUE);
        d.put("SplitPane.oneTouchButtonOffset", Integer.valueOf(30));
        d.put("SplitPane.oneTouchExpandable", Boolean.FALSE);
        d.put("SplitPane.continuousLayout", Boolean.TRUE);
        d.put("SplitPane:SplitPaneDivider.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("SplitPane:SplitPaneDivider.States", "Enabled,MouseOver,Pressed,Disabled,Focused,Selected,Vertical");
        d.put("SplitPane:SplitPaneDivider.Vertical", new SplitPaneDividerVerticalState());
        d.put("SplitPane:SplitPaneDivider[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SplitPaneDividerPainter", SplitPaneDividerPainter.BACKGROUND_ENABLED, new Insets(3, 0, 3, 0), new Dimension(68, 10), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("SplitPane:SplitPaneDivider[Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SplitPaneDividerPainter", SplitPaneDividerPainter.BACKGROUND_FOCUSED, new Insets(3, 0, 3, 0), new Dimension(68, 10), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("SplitPane:SplitPaneDivider[Enabled].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SplitPaneDividerPainter", SplitPaneDividerPainter.FOREGROUND_ENABLED, new Insets(0, 24, 0, 24), new Dimension(68, 10), true, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("SplitPane:SplitPaneDivider[Enabled+Vertical].foregroundPainter", new LazyPainter("javax.swing.plaf.nimbus.SplitPaneDividerPainter", SplitPaneDividerPainter.FOREGROUND_ENABLED_VERTICAL, new Insets(5, 0, 5, 0), new Dimension(10, 38), true, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        //Initialize TabbedPane
        d.put("TabbedPane.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("TabbedPane.tabAreaStatesMatchSelectedTab", Boolean.TRUE);
        d.put("TabbedPane.nudgeSelectedLabel", Boolean.FALSE);
        d.put("TabbedPane.tabRunOverlay", Integer.valueOf(2));
        d.put("TabbedPane.tabOverlap", Integer.valueOf(-1));
        d.put("TabbedPane.extendTabsToBase", Boolean.TRUE);
        d.put("TabbedPane.useBasicArrows", Boolean.TRUE);
        addColor(d, "TabbedPane.shadow", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "TabbedPane.darkShadow", "text", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "TabbedPane.highlight", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
        d.put("TabbedPane:TabbedPaneTab.contentMargins", new InsetsUIResource(2, 8, 3, 8));
        d.put("TabbedPane:TabbedPaneTab[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabPainter", TabbedPaneTabPainter.BACKGROUND_ENABLED, new Insets(7, 7, 1, 7), new Dimension(44, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TabbedPane:TabbedPaneTab[Enabled+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabPainter", TabbedPaneTabPainter.BACKGROUND_ENABLED_MOUSEOVER, new Insets(7, 7, 1, 7), new Dimension(44, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TabbedPane:TabbedPaneTab[Enabled+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabPainter", TabbedPaneTabPainter.BACKGROUND_ENABLED_PRESSED, new Insets(7, 6, 1, 7), new Dimension(44, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        addColor(d, "TabbedPane:TabbedPaneTab[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("TabbedPane:TabbedPaneTab[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabPainter", TabbedPaneTabPainter.BACKGROUND_DISABLED, new Insets(6, 7, 1, 7), new Dimension(44, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TabbedPane:TabbedPaneTab[Disabled+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabPainter", TabbedPaneTabPainter.BACKGROUND_SELECTED_DISABLED, new Insets(7, 7, 0, 7), new Dimension(44, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TabbedPane:TabbedPaneTab[Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabPainter", TabbedPaneTabPainter.BACKGROUND_SELECTED, new Insets(7, 7, 0, 7), new Dimension(44, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TabbedPane:TabbedPaneTab[MouseOver+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabPainter", TabbedPaneTabPainter.BACKGROUND_SELECTED_MOUSEOVER, new Insets(7, 9, 0, 9), new Dimension(44, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        addColor(d, "TabbedPane:TabbedPaneTab[Pressed+Selected].textForeground", 255, 255, 255, 255);
        d.put("TabbedPane:TabbedPaneTab[Pressed+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabPainter", TabbedPaneTabPainter.BACKGROUND_SELECTED_PRESSED, new Insets(7, 9, 0, 9), new Dimension(44, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TabbedPane:TabbedPaneTab[Focused+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabPainter", TabbedPaneTabPainter.BACKGROUND_SELECTED_FOCUSED, new Insets(7, 7, 3, 7), new Dimension(44, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TabbedPane:TabbedPaneTab[Focused+MouseOver+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabPainter", TabbedPaneTabPainter.BACKGROUND_SELECTED_MOUSEOVER_FOCUSED, new Insets(7, 9, 3, 9), new Dimension(44, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        addColor(d, "TabbedPane:TabbedPaneTab[Focused+Pressed+Selected].textForeground", 255, 255, 255, 255);
        d.put("TabbedPane:TabbedPaneTab[Focused+Pressed+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabPainter", TabbedPaneTabPainter.BACKGROUND_SELECTED_PRESSED_FOCUSED, new Insets(7, 9, 3, 9), new Dimension(44, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TabbedPane:TabbedPaneTabArea.contentMargins", new InsetsUIResource(3, 10, 4, 10));
        d.put("TabbedPane:TabbedPaneTabArea[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabAreaPainter", TabbedPaneTabAreaPainter.BACKGROUND_ENABLED, new Insets(0, 5, 6, 5), new Dimension(5, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TabbedPane:TabbedPaneTabArea[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabAreaPainter", TabbedPaneTabAreaPainter.BACKGROUND_DISABLED, new Insets(0, 5, 6, 5), new Dimension(5, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TabbedPane:TabbedPaneTabArea[Enabled+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabAreaPainter", TabbedPaneTabAreaPainter.BACKGROUND_ENABLED_MOUSEOVER, new Insets(0, 5, 6, 5), new Dimension(5, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TabbedPane:TabbedPaneTabArea[Enabled+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TabbedPaneTabAreaPainter", TabbedPaneTabAreaPainter.BACKGROUND_ENABLED_PRESSED, new Insets(0, 5, 6, 5), new Dimension(5, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TabbedPane:TabbedPaneContent.contentMargins", new InsetsUIResource(0, 0, 0, 0));

        //Initialize Table
        d.put("Table.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("Table.opaque", Boolean.TRUE);
        addColor(d, "Table.textForeground", 35, 35, 36, 255);
        addColor(d, "Table.background", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
        d.put("Table.showGrid", Boolean.FALSE);
        d.put("Table.intercellSpacing", new DimensionUIResource(0, 0));
        addColor(d, "Table.alternateRowColor", "nimbusLightBackground", 0.0f, 0.0f, -0.05098039f, 0, false);
        d.put("Table.rendererUseTableColors", Boolean.TRUE);
        d.put("Table.rendererUseUIBorder", Boolean.TRUE);
        d.put("Table.cellNoFocusBorder", new BorderUIResource(BorderFactory.createEmptyBorder(2, 5, 2, 5)));
        d.put("Table.focusCellHighlightBorder", new BorderUIResource(new PainterBorder("Tree:TreeCell[Enabled+Focused].backgroundPainter", new Insets(2, 5, 2, 5))));
        addColor(d, "Table.dropLineColor", "nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "Table.dropLineShortColor", "nimbusOrange", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "Table[Enabled+Selected].textForeground", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0, false);
        addColor(d, "Table[Enabled+Selected].textBackground", "nimbusSelectionBackground", 0.0f, 0.0f, 0.0f, 0, false);
        addColor(d, "Table[Disabled+Selected].textBackground", "nimbusSelectionBackground", 0.0f, 0.0f, 0.0f, 0, false);
        d.put("Table:\"Table.cellRenderer\".contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("Table:\"Table.cellRenderer\".opaque", Boolean.TRUE);
        addColor(d, "Table:\"Table.cellRenderer\".background", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0, false);

        //Initialize TableHeader
        d.put("TableHeader.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("TableHeader.opaque", Boolean.TRUE);
        d.put("TableHeader.rightAlignSortArrow", Boolean.TRUE);
        d.put("TableHeader[Enabled].ascendingSortIconPainter", new LazyPainter("javax.swing.plaf.nimbus.TableHeaderPainter", TableHeaderPainter.ASCENDINGSORTICON_ENABLED, new Insets(0, 0, 0, 2), new Dimension(7, 7), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Table.ascendingSortIcon", new NimbusIcon("TableHeader", "ascendingSortIconPainter", 7, 7));
        d.put("TableHeader[Enabled].descendingSortIconPainter", new LazyPainter("javax.swing.plaf.nimbus.TableHeaderPainter", TableHeaderPainter.DESCENDINGSORTICON_ENABLED, new Insets(0, 0, 0, 0), new Dimension(7, 7), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Table.descendingSortIcon", new NimbusIcon("TableHeader", "descendingSortIconPainter", 7, 7));
        d.put("TableHeader:\"TableHeader.renderer\".contentMargins", new InsetsUIResource(2, 5, 4, 5));
        d.put("TableHeader:\"TableHeader.renderer\".opaque", Boolean.TRUE);
        d.put("TableHeader:\"TableHeader.renderer\".States", "Enabled,MouseOver,Pressed,Disabled,Focused,Selected,Sorted");
        d.put("TableHeader:\"TableHeader.renderer\".Sorted", new TableHeaderRendererSortedState());
        d.put("TableHeader:\"TableHeader.renderer\"[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TableHeaderRendererPainter", TableHeaderRendererPainter.BACKGROUND_DISABLED, new Insets(5, 5, 5, 5), new Dimension(22, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TableHeader:\"TableHeader.renderer\"[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TableHeaderRendererPainter", TableHeaderRendererPainter.BACKGROUND_ENABLED, new Insets(5, 5, 5, 5), new Dimension(22, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TableHeader:\"TableHeader.renderer\"[Enabled+Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TableHeaderRendererPainter", TableHeaderRendererPainter.BACKGROUND_ENABLED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(22, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TableHeader:\"TableHeader.renderer\"[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TableHeaderRendererPainter", TableHeaderRendererPainter.BACKGROUND_MOUSEOVER, new Insets(5, 5, 5, 5), new Dimension(22, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TableHeader:\"TableHeader.renderer\"[Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TableHeaderRendererPainter", TableHeaderRendererPainter.BACKGROUND_PRESSED, new Insets(5, 5, 5, 5), new Dimension(22, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TableHeader:\"TableHeader.renderer\"[Enabled+Sorted].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TableHeaderRendererPainter", TableHeaderRendererPainter.BACKGROUND_ENABLED_SORTED, new Insets(5, 5, 5, 5), new Dimension(22, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TableHeader:\"TableHeader.renderer\"[Enabled+Focused+Sorted].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TableHeaderRendererPainter", TableHeaderRendererPainter.BACKGROUND_ENABLED_FOCUSED_SORTED, new Insets(5, 5, 5, 5), new Dimension(22, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TableHeader:\"TableHeader.renderer\"[Disabled+Sorted].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TableHeaderRendererPainter", TableHeaderRendererPainter.BACKGROUND_DISABLED_SORTED, new Insets(5, 5, 5, 5), new Dimension(22, 20), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        //Initialize \"Table.editor\"
        d.put("\"Table.editor\".contentMargins", new InsetsUIResource(3, 5, 3, 5));
        d.put("\"Table.editor\".opaque", Boolean.TRUE);
        addColor(d, "\"Table.editor\".background", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "\"Table.editor\"[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("\"Table.editor\"[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TableEditorPainter", TableEditorPainter.BACKGROUND_ENABLED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("\"Table.editor\"[Enabled+Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TableEditorPainter", TableEditorPainter.BACKGROUND_ENABLED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        addColor(d, "\"Table.editor\"[Selected].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);

        //Initialize \"Tree.cellEditor\"
        d.put("\"Tree.cellEditor\".contentMargins", new InsetsUIResource(2, 5, 2, 5));
        d.put("\"Tree.cellEditor\".opaque", Boolean.TRUE);
        addColor(d, "\"Tree.cellEditor\".background", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "\"Tree.cellEditor\"[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("\"Tree.cellEditor\"[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TreeCellEditorPainter", TreeCellEditorPainter.BACKGROUND_ENABLED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("\"Tree.cellEditor\"[Enabled+Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TreeCellEditorPainter", TreeCellEditorPainter.BACKGROUND_ENABLED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        addColor(d, "\"Tree.cellEditor\"[Selected].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);

        //Initialize TextField
        d.put("TextField.contentMargins", new InsetsUIResource(6, 6, 6, 6));
        addColor(d, "TextField.background", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "TextField[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("TextField[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TextFieldPainter", TextFieldPainter.BACKGROUND_DISABLED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TextField[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TextFieldPainter", TextFieldPainter.BACKGROUND_ENABLED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        addColor(d, "TextField[Selected].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);
        d.put("TextField[Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TextFieldPainter", TextFieldPainter.BACKGROUND_SELECTED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        addColor(d, "TextField[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("TextField[Disabled].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.TextFieldPainter", TextFieldPainter.BORDER_DISABLED, new Insets(5, 3, 3, 3), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TextField[Focused].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.TextFieldPainter", TextFieldPainter.BORDER_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TextField[Enabled].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.TextFieldPainter", TextFieldPainter.BORDER_ENABLED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        //Initialize FormattedTextField
        d.put("FormattedTextField.contentMargins", new InsetsUIResource(6, 6, 6, 6));
        addColor(d, "FormattedTextField[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("FormattedTextField[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.FormattedTextFieldPainter", FormattedTextFieldPainter.BACKGROUND_DISABLED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("FormattedTextField[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.FormattedTextFieldPainter", FormattedTextFieldPainter.BACKGROUND_ENABLED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        addColor(d, "FormattedTextField[Selected].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);
        d.put("FormattedTextField[Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.FormattedTextFieldPainter", FormattedTextFieldPainter.BACKGROUND_SELECTED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        addColor(d, "FormattedTextField[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("FormattedTextField[Disabled].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.FormattedTextFieldPainter", FormattedTextFieldPainter.BORDER_DISABLED, new Insets(5, 3, 3, 3), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("FormattedTextField[Focused].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.FormattedTextFieldPainter", FormattedTextFieldPainter.BORDER_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("FormattedTextField[Enabled].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.FormattedTextFieldPainter", FormattedTextFieldPainter.BORDER_ENABLED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        //Initialize PasswordField
        d.put("PasswordField.contentMargins", new InsetsUIResource(6, 6, 6, 6));
        addColor(d, "PasswordField[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("PasswordField[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.PasswordFieldPainter", PasswordFieldPainter.BACKGROUND_DISABLED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("PasswordField[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.PasswordFieldPainter", PasswordFieldPainter.BACKGROUND_ENABLED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        addColor(d, "PasswordField[Selected].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);
        d.put("PasswordField[Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.PasswordFieldPainter", PasswordFieldPainter.BACKGROUND_SELECTED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        addColor(d, "PasswordField[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("PasswordField[Disabled].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.PasswordFieldPainter", PasswordFieldPainter.BORDER_DISABLED, new Insets(5, 3, 3, 3), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("PasswordField[Focused].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.PasswordFieldPainter", PasswordFieldPainter.BORDER_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("PasswordField[Enabled].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.PasswordFieldPainter", PasswordFieldPainter.BORDER_ENABLED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        //Initialize TextArea
        d.put("TextArea.contentMargins", new InsetsUIResource(6, 6, 6, 6));
        d.put("TextArea.States", "Enabled,MouseOver,Pressed,Selected,Disabled,Focused,NotInScrollPane");
        d.put("TextArea.NotInScrollPane", new TextAreaNotInScrollPaneState());
        addColor(d, "TextArea[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("TextArea[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TextAreaPainter", TextAreaPainter.BACKGROUND_DISABLED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("TextArea[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TextAreaPainter", TextAreaPainter.BACKGROUND_ENABLED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        addColor(d, "TextArea[Disabled+NotInScrollPane].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("TextArea[Disabled+NotInScrollPane].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TextAreaPainter", TextAreaPainter.BACKGROUND_DISABLED_NOTINSCROLLPANE, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("TextArea[Enabled+NotInScrollPane].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TextAreaPainter", TextAreaPainter.BACKGROUND_ENABLED_NOTINSCROLLPANE, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        addColor(d, "TextArea[Selected].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);
        d.put("TextArea[Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TextAreaPainter", TextAreaPainter.BACKGROUND_SELECTED, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        addColor(d, "TextArea[Disabled+NotInScrollPane].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("TextArea[Disabled+NotInScrollPane].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.TextAreaPainter", TextAreaPainter.BORDER_DISABLED_NOTINSCROLLPANE, new Insets(5, 3, 3, 3), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TextArea[Focused+NotInScrollPane].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.TextAreaPainter", TextAreaPainter.BORDER_FOCUSED_NOTINSCROLLPANE, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        d.put("TextArea[Enabled+NotInScrollPane].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.TextAreaPainter", TextAreaPainter.BORDER_ENABLED_NOTINSCROLLPANE, new Insets(5, 5, 5, 5), new Dimension(122, 24), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        //Initialize TextPane
        d.put("TextPane.contentMargins", new InsetsUIResource(4, 6, 4, 6));
        d.put("TextPane.opaque", Boolean.TRUE);
        addColor(d, "TextPane[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("TextPane[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TextPanePainter", TextPanePainter.BACKGROUND_DISABLED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("TextPane[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TextPanePainter", TextPanePainter.BACKGROUND_ENABLED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        addColor(d, "TextPane[Selected].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);
        d.put("TextPane[Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TextPanePainter", TextPanePainter.BACKGROUND_SELECTED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));

        //Initialize EditorPane
        d.put("EditorPane.contentMargins", new InsetsUIResource(4, 6, 4, 6));
        d.put("EditorPane.opaque", Boolean.TRUE);
        addColor(d, "EditorPane[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("EditorPane[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.EditorPanePainter", EditorPanePainter.BACKGROUND_DISABLED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("EditorPane[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.EditorPanePainter", EditorPanePainter.BACKGROUND_ENABLED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        addColor(d, "EditorPane[Selected].textForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0);
        d.put("EditorPane[Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.EditorPanePainter", EditorPanePainter.BACKGROUND_SELECTED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));

        //Initialize ToolBar
        d.put("ToolBar.contentMargins", new InsetsUIResource(2, 2, 2, 2));
        d.put("ToolBar.opaque", Boolean.TRUE);
        d.put("ToolBar.States", "North,East,West,South");
        d.put("ToolBar.North", new ToolBarNorthState());
        d.put("ToolBar.East", new ToolBarEastState());
        d.put("ToolBar.West", new ToolBarWestState());
        d.put("ToolBar.South", new ToolBarSouthState());
        d.put("ToolBar[North].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarPainter", ToolBarPainter.BORDER_NORTH, new Insets(0, 0, 1, 0), new Dimension(30, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("ToolBar[South].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarPainter", ToolBarPainter.BORDER_SOUTH, new Insets(1, 0, 0, 0), new Dimension(30, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("ToolBar[East].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarPainter", ToolBarPainter.BORDER_EAST, new Insets(1, 0, 0, 0), new Dimension(30, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("ToolBar[West].borderPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarPainter", ToolBarPainter.BORDER_WEST, new Insets(0, 0, 1, 0), new Dimension(30, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("ToolBar[Enabled].handleIconPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarPainter", ToolBarPainter.HANDLEICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(11, 38), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar.handleIcon", new NimbusIcon("ToolBar", "handleIconPainter", 11, 38));
        d.put("ToolBar:Button.contentMargins", new InsetsUIResource(4, 4, 4, 4));
        d.put("ToolBar:Button[Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarButtonPainter", ToolBarButtonPainter.BACKGROUND_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:Button[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarButtonPainter", ToolBarButtonPainter.BACKGROUND_MOUSEOVER, new Insets(5, 5, 5, 5), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:Button[Focused+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarButtonPainter", ToolBarButtonPainter.BACKGROUND_MOUSEOVER_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:Button[Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarButtonPainter", ToolBarButtonPainter.BACKGROUND_PRESSED, new Insets(5, 5, 5, 5), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:Button[Focused+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarButtonPainter", ToolBarButtonPainter.BACKGROUND_PRESSED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(104, 33), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:ToggleButton.contentMargins", new InsetsUIResource(4, 4, 4, 4));
        d.put("ToolBar:ToggleButton[Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarToggleButtonPainter", ToolBarToggleButtonPainter.BACKGROUND_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(104, 34), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:ToggleButton[MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarToggleButtonPainter", ToolBarToggleButtonPainter.BACKGROUND_MOUSEOVER, new Insets(5, 5, 5, 5), new Dimension(104, 34), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:ToggleButton[Focused+MouseOver].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarToggleButtonPainter", ToolBarToggleButtonPainter.BACKGROUND_MOUSEOVER_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(104, 34), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:ToggleButton[Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarToggleButtonPainter", ToolBarToggleButtonPainter.BACKGROUND_PRESSED, new Insets(5, 5, 5, 5), new Dimension(72, 25), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:ToggleButton[Focused+Pressed].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarToggleButtonPainter", ToolBarToggleButtonPainter.BACKGROUND_PRESSED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(72, 25), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:ToggleButton[Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarToggleButtonPainter", ToolBarToggleButtonPainter.BACKGROUND_SELECTED, new Insets(5, 5, 5, 5), new Dimension(72, 25), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:ToggleButton[Focused+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarToggleButtonPainter", ToolBarToggleButtonPainter.BACKGROUND_SELECTED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(72, 25), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:ToggleButton[Pressed+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarToggleButtonPainter", ToolBarToggleButtonPainter.BACKGROUND_PRESSED_SELECTED, new Insets(5, 5, 5, 5), new Dimension(72, 25), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:ToggleButton[Focused+Pressed+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarToggleButtonPainter", ToolBarToggleButtonPainter.BACKGROUND_PRESSED_SELECTED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(72, 25), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:ToggleButton[MouseOver+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarToggleButtonPainter", ToolBarToggleButtonPainter.BACKGROUND_MOUSEOVER_SELECTED, new Insets(5, 5, 5, 5), new Dimension(72, 25), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        d.put("ToolBar:ToggleButton[Focused+MouseOver+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarToggleButtonPainter", ToolBarToggleButtonPainter.BACKGROUND_MOUSEOVER_SELECTED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(72, 25), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));
        addColor(d, "ToolBar:ToggleButton[Disabled+Selected].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("ToolBar:ToggleButton[Disabled+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolBarToggleButtonPainter", ToolBarToggleButtonPainter.BACKGROUND_DISABLED_SELECTED, new Insets(5, 5, 5, 5), new Dimension(72, 25), false, AbstractRegionPainter.PaintContext.CacheMode.NINE_SQUARE_SCALE, 2.0, Double.POSITIVE_INFINITY));

        //Initialize ToolBarSeparator
        d.put("ToolBarSeparator.contentMargins", new InsetsUIResource(2, 0, 3, 0));
        addColor(d, "ToolBarSeparator.textForeground", "nimbusBorder", 0.0f, 0.0f, 0.0f, 0);

        //Initialize ToolTip
        d.put("ToolTip.contentMargins", new InsetsUIResource(4, 4, 4, 4));
        d.put("ToolTip[Enabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolTipPainter", ToolTipPainter.BACKGROUND_ENABLED, new Insets(1, 1, 1, 1), new Dimension(10, 10), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("ToolTip[Disabled].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.ToolTipPainter", ToolTipPainter.BACKGROUND_DISABLED, new Insets(1, 1, 1, 1), new Dimension(10, 10), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));

        //Initialize Tree
        d.put("Tree.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("Tree.opaque", Boolean.TRUE);
        addColor(d, "Tree.textForeground", "text", 0.0f, 0.0f, 0.0f, 0, false);
        addColor(d, "Tree.textBackground", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0, false);
        addColor(d, "Tree.background", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
        d.put("Tree.rendererFillBackground", Boolean.FALSE);
        d.put("Tree.leftChildIndent", Integer.valueOf(12));
        d.put("Tree.rightChildIndent", Integer.valueOf(4));
        d.put("Tree.drawHorizontalLines", Boolean.FALSE);
        d.put("Tree.drawVerticalLines", Boolean.FALSE);
        d.put("Tree.showRootHandles", Boolean.FALSE);
        d.put("Tree.rendererUseTreeColors", Boolean.TRUE);
        d.put("Tree.repaintWholeRow", Boolean.TRUE);
        d.put("Tree.rowHeight", Integer.valueOf(0));
        d.put("Tree.rendererMargins", new InsetsUIResource(2, 0, 1, 5));
        addColor(d, "Tree.selectionForeground", "nimbusSelectedText", 0.0f, 0.0f, 0.0f, 0, false);
        addColor(d, "Tree.selectionBackground", "nimbusSelectionBackground", 0.0f, 0.0f, 0.0f, 0, false);
        addColor(d, "Tree.dropLineColor", "nimbusFocus", 0.0f, 0.0f, 0.0f, 0);
        d.put("Tree:TreeCell.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        addColor(d, "Tree:TreeCell[Enabled].background", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
        addColor(d, "Tree:TreeCell[Enabled+Focused].background", "nimbusLightBackground", 0.0f, 0.0f, 0.0f, 0);
        d.put("Tree:TreeCell[Enabled+Focused].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TreeCellPainter", TreeCellPainter.BACKGROUND_ENABLED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        addColor(d, "Tree:TreeCell[Enabled+Selected].textForeground", 255, 255, 255, 255);
        d.put("Tree:TreeCell[Enabled+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TreeCellPainter", TreeCellPainter.BACKGROUND_ENABLED_SELECTED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        addColor(d, "Tree:TreeCell[Focused+Selected].textForeground", 255, 255, 255, 255);
        d.put("Tree:TreeCell[Focused+Selected].backgroundPainter", new LazyPainter("javax.swing.plaf.nimbus.TreeCellPainter", TreeCellPainter.BACKGROUND_SELECTED_FOCUSED, new Insets(5, 5, 5, 5), new Dimension(100, 30), false, AbstractRegionPainter.PaintContext.CacheMode.NO_CACHING, 1.0, 1.0));
        d.put("Tree:\"Tree.cellRenderer\".contentMargins", new InsetsUIResource(0, 0, 0, 0));
        addColor(d, "Tree:\"Tree.cellRenderer\"[Disabled].textForeground", "nimbusDisabledText", 0.0f, 0.0f, 0.0f, 0);
        d.put("Tree[Enabled].leafIconPainter", new LazyPainter("javax.swing.plaf.nimbus.TreePainter", TreePainter.LEAFICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(16, 16), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Tree.leafIcon", new NimbusIcon("Tree", "leafIconPainter", 16, 16));
        d.put("Tree[Enabled].closedIconPainter", new LazyPainter("javax.swing.plaf.nimbus.TreePainter", TreePainter.CLOSEDICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(16, 16), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Tree.closedIcon", new NimbusIcon("Tree", "closedIconPainter", 16, 16));
        d.put("Tree[Enabled].openIconPainter", new LazyPainter("javax.swing.plaf.nimbus.TreePainter", TreePainter.OPENICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(16, 16), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Tree.openIcon", new NimbusIcon("Tree", "openIconPainter", 16, 16));
        d.put("Tree[Enabled].collapsedIconPainter", new LazyPainter("javax.swing.plaf.nimbus.TreePainter", TreePainter.COLLAPSEDICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(18, 7), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Tree[Enabled+Selected].collapsedIconPainter", new LazyPainter("javax.swing.plaf.nimbus.TreePainter", TreePainter.COLLAPSEDICON_ENABLED_SELECTED, new Insets(5, 5, 5, 5), new Dimension(18, 7), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Tree.collapsedIcon", new NimbusIcon("Tree", "collapsedIconPainter", 18, 7));
        d.put("Tree[Enabled].expandedIconPainter", new LazyPainter("javax.swing.plaf.nimbus.TreePainter", TreePainter.EXPANDEDICON_ENABLED, new Insets(5, 5, 5, 5), new Dimension(18, 7), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Tree[Enabled+Selected].expandedIconPainter", new LazyPainter("javax.swing.plaf.nimbus.TreePainter", TreePainter.EXPANDEDICON_ENABLED_SELECTED, new Insets(5, 5, 5, 5), new Dimension(18, 7), false, AbstractRegionPainter.PaintContext.CacheMode.FIXED_SIZES, 1.0, 1.0));
        d.put("Tree.expandedIcon", new NimbusIcon("Tree", "expandedIconPainter", 18, 7));

        //Initialize RootPane
        d.put("RootPane.contentMargins", new InsetsUIResource(0, 0, 0, 0));
        d.put("RootPane.opaque", Boolean.TRUE);
        addColor(d, "RootPane.background", "control", 0.0f, 0.0f, 0.0f, 0);


    }

    /**
     * <p>Registers the given region and prefix. The prefix, if it contains
     * quoted sections, refers to certain named components. If there are not
     * quoted sections, then the prefix refers to a generic component type.</p>
     *
     * <p>If the given region/prefix combo has already been registered, then
     * it will not be registered twice. The second registration attempt will
     * fail silently.</p>
     *
     * @param region The Synth Region that is being registered. Such as Button,
     *        or ScrollBarThumb.
     * @param prefix The UIDefault prefix. For example, could be ComboBox, or if
     *        a named components, "MyComboBox", or even something like
     *        ToolBar:"MyComboBox":"ComboBox.arrowButton"
     */
    void register(Region region, String prefix) {
        //validate the method arguments
        if (region == null || prefix == null) {
            throw new IllegalArgumentException(
                    "Neither Region nor Prefix may be null");
        }

        //Add a LazyStyle for this region/prefix to m.
        List<LazyStyle> styles = m.get(region);
        if (styles == null) {
            styles = new LinkedList<LazyStyle>();
            styles.add(new LazyStyle(prefix));
            m.put(region, styles);
        } else {
            //iterate over all the current styles and see if this prefix has
            //already been registered. If not, then register it.
            for (LazyStyle s : styles) {
                if (prefix.equals(s.prefix)) {
                    return;
                }
            }
            styles.add(new LazyStyle(prefix));
        }

        //add this region to the map of registered regions
        registeredRegions.put(region.getName(), region);
    }

    /**
     * <p>Locate the style associated with the given region, and component.
     * This is called from NimbusLookAndFeel in the SynthStyleFactory
     * implementation.</p>
     *
     * <p>Lookup occurs as follows:<br/>
     * Check the map of styles <code>m</code>. If the map contains no styles at
     * all, then simply return the defaultStyle. If the map contains styles,
     * then iterate over all of the styles for the Region <code>r</code> looking
     * for the best match, based on prefix. If a match was made, then return
     * that SynthStyle. Otherwise, return the defaultStyle.</p>
     *
     * @param comp The component associated with this region. For example, if
     *        the Region is Region.Button then the component will be a JButton.
     *        If the Region is a subregion, such as ScrollBarThumb, then the
     *        associated component will be the component that subregion belongs
     *        to, such as JScrollBar. The JComponent may be named. It may not be
     *        null.
     * @param r The region we are looking for a style for. May not be null.
     */
    SynthStyle getStyle(JComponent comp, Region r) {
        //validate method arguments
        if (comp == null || r == null) {
            throw new IllegalArgumentException(
                    "Neither comp nor r may be null");
        }

        //if there are no lazy styles registered for the region r, then return
        //the default style
        List<LazyStyle> styles = m.get(r);
        if (styles == null || styles.size() == 0) {
            return defaultStyle;
        }

        //Look for the best SynthStyle for this component/region pair.
        LazyStyle foundStyle = null;
        for (LazyStyle s : styles) {
            if (s.matches(comp)) {
                //replace the foundStyle if foundStyle is null, or
                //if the new style "s" is more specific (ie, its path was
                //longer), or if the foundStyle was "simple" and the new style
                //was not (ie: the foundStyle was for something like Button and
                //the new style was for something like "MyButton", hence, being
                //more specific.) In all cases, favor the most specific style
                //found.
                if (foundStyle == null ||
                   (foundStyle.parts.length < s.parts.length) ||
                   (foundStyle.parts.length == s.parts.length 
                    && foundStyle.simple && !s.simple)) {
                    foundStyle = s;
                }
            }
        }

        //return the style, if found, or the default style if not found
        return foundStyle == null ? defaultStyle : foundStyle.getStyle(comp, r);
    }

    public void clearOverridesCache(JComponent c) {
        overridesCache.remove(c);
    }

    /*
        Various public helper classes.
        These may be used to register 3rd party values into UIDefaults
    */

    /**
     * <p>Derives its font value based on a parent font and a set of offsets and
     * attributes. This class is an ActiveValue, meaning that it will recompute
     * its value each time it is requested from UIDefaults. It is therefore
     * recommended to read this value once and cache it in the UI delegate class
     * until asked to reinitialize.</p>
     *
     * <p>To use this class, create an instance with the key of the font in the
     * UI defaults table from which to derive this font, along with a size
     * offset (if any), and whether it is to be bold, italic, or left in its
     * default form.</p>
     */
    static final class DerivedFont implements UIDefaults.ActiveValue {
        private float sizeOffset;
        private Boolean bold;
        private Boolean italic;
        private String parentKey;

        /**
         * Create a new DerivedFont.
         *
         * @param key The UIDefault key associated with this derived font's
         *            parent or source. If this key leads to a null value, or a
         *            value that is not a font, then null will be returned as
         *            the derived font. The key must not be null.
         * @param sizeOffset The size offset, as a percentage, to use. For
         *                   example, if the source font was a 12pt font and the
         *                   sizeOffset were specified as .9, then the new font
         *                   will be 90% of what the source font was, or, 10.8
         *                   pts which is rounded to 11pts. This fractional
         *                   based offset allows for proper font scaling in high
         *                   DPI or large system font scenarios.
         * @param bold Whether the new font should be bold. If null, then this
         *             new font will inherit the bold setting of the source
         *             font.
         * @param italic Whether the new font should be italicized. If null,
         *               then this new font will inherit the italic setting of
         *               the source font.
         */
        public DerivedFont(String key, float sizeOffset, Boolean bold,
                           Boolean italic) {
            //validate the constructor arguments
            if (key == null) {
                throw new IllegalArgumentException("You must specify a key");
            }

            //set the values
            this.parentKey = key;
            this.sizeOffset = sizeOffset;
            this.bold = bold;
            this.italic = italic;
        }

        /**
         * @inheritDoc
         */
        @Override
        public Object createValue(UIDefaults defaults) {
            Font f = defaults.getFont(parentKey);
            if (f != null) {
                // always round size for now so we have exact int font size
                // (or we may have lame looking fonts)
                float size = Math.round(f.getSize2D() * sizeOffset);
                int style = f.getStyle();
                if (bold != null) {
                    if (bold.booleanValue()) {
                        style = style | Font.BOLD;
                    } else {
                        style = style & ~Font.BOLD;
                    }
                }
                if (italic != null) {
                    if (italic.booleanValue()) {
                        style = style | Font.ITALIC;
                    } else {
                        style = style & ~Font.ITALIC;
                    }
                }
                return f.deriveFont(style, size);
            } else {
                return null;
            }
        }
    }


    /**
     * This class is private because it relies on the constructor of the
     * auto-generated AbstractRegionPainter subclasses. Hence, it is not
     * generally useful, and is private.
     * <p/>
     * LazyPainter is a LazyValue class. It will create the
     * AbstractRegionPainter lazily, when asked. It uses reflection to load the
     * proper class and invoke its constructor.
     */
    private static final class LazyPainter implements UIDefaults.LazyValue {
        private int which;
        private AbstractRegionPainter.PaintContext ctx;
        private String className;

        LazyPainter(String className, int which, Insets insets,
                    Dimension canvasSize, boolean inverted) {
            if (className == null) {
                throw new IllegalArgumentException(
                        "The className must be specified");
            }

            this.className = className;
            this.which = which;
            this.ctx = new AbstractRegionPainter.PaintContext(
                insets, canvasSize, inverted);
        }

        LazyPainter(String className, int which, Insets insets,
                    Dimension canvasSize, boolean inverted,
                    AbstractRegionPainter.PaintContext.CacheMode cacheMode,
                    double maxH, double maxV) {
            if (className == null) {
                throw new IllegalArgumentException(
                        "The className must be specified");
            }

            this.className = className;
            this.which = which;
            this.ctx = new AbstractRegionPainter.PaintContext(
                    insets, canvasSize, inverted, cacheMode, maxH, maxV);
        }

        @Override
        public Object createValue(UIDefaults table) {
            try {
                Class<?> c;
                Object cl;
                // See if we should use a separate ClassLoader
                if (table == null || !((cl = table.get("ClassLoader"))
                                       instanceof ClassLoader)) {
                    cl = Thread.currentThread().
                                getContextClassLoader();
                    if (cl == null) {
                        // Fallback to the system class loader.
                        cl = ClassLoader.getSystemClassLoader();
                    }
                }

                c = Class.forName(className, true, (ClassLoader)cl);
                Constructor<?> constructor = c.getConstructor(
                        AbstractRegionPainter.PaintContext.class, int.class);
                if (constructor == null) {
                    throw new NullPointerException(
                            "Failed to find the constructor for the class: " +
                            className);
                }
                return constructor.newInstance(ctx, which);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * A class which creates the NimbusStyle associated with it lazily, but also
     * manages a lot more information about the style. It is less of a LazyValue
     * type of class, and more of an Entry or Item type of class, as it
     * represents an entry in the list of LazyStyles in the map m.
     *
     * The primary responsibilities of this class include:
     * <ul>
     *   <li>Determining whether a given component/region pair matches this
     *       style</li>
     *   <li>Splitting the prefix specified in the constructor into its
     *       constituent parts to facilitate quicker matching</li>
     *   <li>Creating and vending a NimbusStyle lazily.</li>
     * </ul>
     */
    private final class LazyStyle {
        /**
         * The prefix this LazyStyle was registered with. Something like
         * Button or ComboBox:"ComboBox.arrowButton"
         */
        private String prefix;
        /**
         * Whether or not this LazyStyle represents an unnamed component
         */
        private boolean simple = true;
        /**
         * The various parts, or sections, of the prefix. For example,
         * the prefix:
         *     ComboBox:"ComboBox.arrowButton"
         *
         * will be broken into two parts,
         *     ComboBox and "ComboBox.arrowButton"
         */
        private Part[] parts;
        /**
         * Cached shared style.
         */
        private NimbusStyle style;

        /**
         * Create a new LazyStyle.
         *
         * @param prefix The prefix associated with this style. Cannot be null.
         */
        private LazyStyle(String prefix) {
            if (prefix == null) {
                throw new IllegalArgumentException(
                        "The prefix must not be null");
            }

            this.prefix = prefix;

            //there is one odd case that needs to be supported here: cell
            //renderers. A cell renderer is defined as a named internal
            //component, so for example:
            // List."List.cellRenderer"
            //The problem is that the component named List.cellRenderer is not a
            //child of a JList. Rather, it is treated more as a direct component
            //Thus, if the prefix ends with "cellRenderer", then remove all the
            //previous dotted parts of the prefix name so that it becomes, for
            //example: "List.cellRenderer"
            //Likewise, we have a hacked work around for cellRenderer, renderer,
            //and listRenderer.
            String temp = prefix;
            if (temp.endsWith("cellRenderer\"")
                    || temp.endsWith("renderer\"")
                    || temp.endsWith("listRenderer\"")) {
                temp = temp.substring(temp.lastIndexOf(":\"") + 1);
            }

            //otherwise, normal code path
            List<String> sparts = split(temp);
            parts = new Part[sparts.size()];
            for (int i = 0; i < parts.length; i++) {
                parts[i] = new Part(sparts.get(i));
                if (parts[i].named) {
                    simple = false;
                }
            }
        }

        /**
         * Gets the style. Creates it if necessary.
         * @return the style
         */
        SynthStyle getStyle(JComponent c, Region r) {
            // if the component has overrides, it gets its own unique style
            // instead of the shared style.
            if (c.getClientProperty("Nimbus.Overrides") != null) {
                Map<Region, SynthStyle> map = overridesCache.get(c);
                SynthStyle s = null;
                if (map == null) {
                    map = new HashMap<Region, SynthStyle>();
                    overridesCache.put(c, map);
                } else {
                    s = map.get(r);
                }
                if (s == null) {
                    s = new NimbusStyle(prefix, c);
                    map.put(r, s);
                }
                return s;
            }
            
            // lazily create the style if necessary
            if (style == null)
                style = new NimbusStyle(prefix, null);
            
            // return the style
            return style;
        }

        /**
         * This LazyStyle is a match for the given component if, and only if,
         * for each part of the prefix the component hierarchy matches exactly.
         * That is, if given "a":something:"b", then:
         * c.getName() must equals "b"
         * c.getParent() can be anything
         * c.getParent().getParent().getName() must equal "a".
         */
        boolean matches(JComponent c) {
            return matches(c, parts.length - 1);
        }

        private boolean matches(Component c, int partIndex) {
            if (partIndex < 0) return true;
            if (c == null) return false;
            //only get here if partIndex > 0 and c == null

            String name = c.getName();
            if (parts[partIndex].named && parts[partIndex].s.equals(name)) {
                //so far so good, recurse
                return matches(c.getParent(), partIndex - 1);
            } else if (!parts[partIndex].named) {
                //if c is not named, and parts[partIndex] has an expected class
                //type registered, then check to make sure c is of the
                //right type;
                Class<?> clazz = parts[partIndex].c;
                if (clazz != null && clazz.isAssignableFrom(c.getClass())) {
                    //so far so good, recurse
                    return matches(c.getParent(), partIndex - 1);
                } else if (clazz == null &&
                           registeredRegions.containsKey(parts[partIndex].s)) {
                    Region r = registeredRegions.get(parts[partIndex].s);
                    Component parent = r.isSubregion() ? c : c.getParent();
                    //special case the JInternalFrameTitlePane, because it
                    //doesn't fit the mold. very, very funky.
                    if (r == Region.INTERNAL_FRAME_TITLE_PANE && parent != null
                        && parent instanceof JInternalFrame.JDesktopIcon) {
                        JInternalFrame.JDesktopIcon icon =
                                (JInternalFrame.JDesktopIcon) parent;
                        parent = icon.getInternalFrame();
                    }
                    //it was the name of a region. So far, so good. Recurse.
                    return matches(parent, partIndex - 1);
                }
            }

            return false;
        }

        /**
         * Given some dot separated prefix, split on the colons that are
         * not within quotes, and not within brackets.
         *
         * @param prefix
         * @return
         */
        private List<String> split(String prefix) {
            List<String> parts = new ArrayList<String>();
            int bracketCount = 0;
            boolean inquotes = false;
            int lastIndex = 0;
            for (int i = 0; i < prefix.length(); i++) {
                char c = prefix.charAt(i);

                if (c == '[') {
                    bracketCount++;
                    continue;
                } else if (c == '"') {
                    inquotes = !inquotes;
                    continue;
                } else if (c == ']') {
                    bracketCount--;
                    if (bracketCount < 0) {
                        throw new RuntimeException(
                                "Malformed prefix: " + prefix);
                    }
                    continue;
                }

                if (c == ':' && !inquotes && bracketCount == 0) {
                    //found a character to split on.
                    parts.add(prefix.substring(lastIndex, i));
                    lastIndex = i + 1;
                }
            }
            if (lastIndex < prefix.length() - 1 && !inquotes
                    && bracketCount == 0) {
                parts.add(prefix.substring(lastIndex));
            }
            return parts;

        }

        private final class Part {
            private String s;
            //true if this part represents a component name
            private boolean named;
            private Class<?> c;

            Part(String s) {
                named = s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"';
                if (named) {
                    this.s = s.substring(1, s.length() - 1);
                } else {
                    this.s = s;
                    //TODO use a map of known regions for Synth and Swing, and
                    //then use [classname] instead of org_class_name style
                    try {
                        c = Class.forName("javax.swing.J" + s);
                    } catch (Exception e) {
                    }
                    try {
                        c = Class.forName(s.replace("_", "."));
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    private void addColor(UIDefaults d, String uin, int r, int g, int b, int a) {
        Color color = new ColorUIResource(new Color(r, g, b, a));
        colorTree.addColor(uin, color);
        d.put(uin, color);
    }

    private void addColor(UIDefaults d, String uin, String parentUin,
            float hOffset, float sOffset, float bOffset, int aOffset) {
        addColor(d, uin, parentUin, hOffset, sOffset, bOffset, aOffset, true);
    }

    private void addColor(UIDefaults d, String uin, String parentUin,
            float hOffset, float sOffset, float bOffset,
            int aOffset, boolean uiResource) {
        Color color = getDerivedColor(uin, parentUin,
                hOffset, sOffset, bOffset, aOffset, uiResource);
        d.put(uin, color);
    }

    /**
     * Get a derived color, derived colors are shared instances and will be
     * updated when its parent UIDefault color changes.
     *
     * @param uiDefaultParentName The parent UIDefault key
     * @param hOffset The hue offset
     * @param sOffset The saturation offset
     * @param bOffset The brightness offset
     * @param aOffset The alpha offset
     * @param uiResource True if the derived color should be a UIResource,
     *        false if it should not be a UIResource
     * @return The stored derived color
     */
    public DerivedColor getDerivedColor(String parentUin,
                                        float hOffset, float sOffset,
                                        float bOffset, int aOffset,
                                        boolean uiResource){
        return getDerivedColor(null, parentUin,
                hOffset, sOffset, bOffset, aOffset, uiResource);
    }

    private DerivedColor getDerivedColor(String uin, String parentUin,
                                        float hOffset, float sOffset,
                                        float bOffset, int aOffset,
                                        boolean uiResource) {
        DerivedColor color;
        if (uiResource) {
            color = new DerivedColor.UIResource(parentUin,
                    hOffset, sOffset, bOffset, aOffset);
        } else {
            color = new DerivedColor(parentUin, hOffset, sOffset,
                bOffset, aOffset);
        }

        if (derivedColors.containsKey(color)) {
            return derivedColors.get(color);
        } else {
            derivedColors.put(color, color);
            color.rederiveColor(); /// move to ARP.decodeColor() ?
            colorTree.addColor(uin, color);
            return color;
        }
    }

    private Map<DerivedColor, DerivedColor> derivedColors =
            new HashMap<DerivedColor, DerivedColor>();

    private class ColorTree implements PropertyChangeListener {
        private Node root = new Node(null, null);
        private Map<String, Node> nodes = new HashMap<String, Node>();

        public Color getColor(String uin) {
            return nodes.get(uin).color;
        }

        public void addColor(String uin, Color color) {
            Node parent = getParentNode(color);
            Node node = new Node(color, parent);
            parent.children.add(node);
            if (uin != null) {
                nodes.put(uin, node);
            }
        }

        private Node getParentNode(Color color) {
            Node parent = root;
            if (color instanceof DerivedColor) {
                String parentUin = ((DerivedColor)color).getUiDefaultParentName();
                Node p = nodes.get(parentUin);
                if (p != null) {
                    parent = p;
                }
            }
            return parent;
        }

        public void update() {
            root.update();
        }

        @Override
        public void propertyChange(PropertyChangeEvent ev) {
            String name = ev.getPropertyName();
            Node node = nodes.get(name);
            if (node != null) {
                // this is a registered color
                node.parent.children.remove(node);
                Color color = (Color) ev.getNewValue();
                Node parent = getParentNode(color);
                node.set(color, parent);
                parent.children.add(node);
                node.update();
            }
        }

        class Node {
            Color color;
            Node parent;
            List<Node> children = new LinkedList<Node>();

            Node(Color color, Node parent) {
                set(color, parent);
            }

            public void set(Color color, Node parent) {
                this.color = color;
                this.parent = parent;
            }

            public void update() {
                if (color instanceof DerivedColor) {
                    ((DerivedColor)color).rederiveColor();
                }
                for (Node child: children) {
                    child.update();
                }
            }
        }
    }

    /**
     * Listener to update derived colors on UIManager Defaults changes
     */
    private class DefaultsListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("lookAndFeel".equals(evt.getPropertyName())) {
                // LAF has been installed, this is the first point at which we
                // can access our defaults table via UIManager so before now
                // all derived colors will be incorrect.
                // First we need to update
                colorTree.update();
            }
        }
    }

    private static final class PainterBorder implements Border, UIResource {
        private Insets insets;
        private Painter<Component> painter;
        private String painterKey;
        
        PainterBorder(String painterKey, Insets insets) {
            this.insets = insets;
            this.painterKey = painterKey;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            if (painter == null) {
                @SuppressWarnings("unchecked")
                Painter<Component> temp = (Painter<Component>)UIManager.get(painterKey);
                painter = temp;
                if (painter == null) return;
            }
            
            g.translate(x, y);
            if (g instanceof Graphics2D)
                painter.paint((Graphics2D)g, c, w, h);
            else {
                BufferedImage img = new BufferedImage(w, h, TYPE_INT_ARGB);
                Graphics2D gfx = img.createGraphics();
                painter.paint(gfx, c, w, h);
                gfx.dispose();
                g.drawImage(img, x, y, null);
                img = null;
            }
            g.translate(-x, -y);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return (Insets)insets.clone();
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}

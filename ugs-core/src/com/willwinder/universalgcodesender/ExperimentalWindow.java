/*
    Copywrite 2012-2016 Will Winder

    This file is part of Universal Gcode Sender (UGS).

    UGS is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    UGS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with UGS.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.willwinder.universalgcodesender;

import com.willwinder.universalgcodesender.i18n.Localization;
import com.willwinder.universalgcodesender.listeners.ControllerListener;
import com.willwinder.universalgcodesender.listeners.UGSEventListener;
import com.willwinder.universalgcodesender.model.BackendAPI;
import com.willwinder.universalgcodesender.model.GUIBackend;
import com.willwinder.universalgcodesender.model.Position;
import com.willwinder.universalgcodesender.model.UGSEvent;
import com.willwinder.universalgcodesender.pendantui.PendantUI;
import com.willwinder.universalgcodesender.pendantui.PendantURLBean;
import com.willwinder.universalgcodesender.types.GcodeCommand;
import com.willwinder.universalgcodesender.uielements.ConnectionSettingsDialog;
import com.willwinder.universalgcodesender.uielements.GrblFirmwareSettingsDialog;
import com.willwinder.universalgcodesender.utils.SettingsFactory;
import com.willwinder.universalgcodesender.utils.Version;
import com.willwinder.universalgcodesender.visualizer.VisualizerWindow;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static com.willwinder.universalgcodesender.utils.GUIHelpers.displayErrorDialog;

/**
 *
 * @author wwinder
 */
public class ExperimentalWindow extends JFrame implements ControllerListener, UGSEventListener {
    private static final Logger logger = Logger.getLogger(ExperimentalWindow.class.getName());

    final private static String VERSION = Version.getVersion() + " / " + Version.getTimestamp();

    private PendantUI pendantUI;

    BackendAPI backend;
    
    // Other windows
    VisualizerWindow vw = null;
    String gcodeFile = null;
    String processedGcodeFile = null;
    
    /** Creates new form ExperimentalWindow */
    public ExperimentalWindow() {
        this.backend = new GUIBackend();
        try {
            backend.applySettings(SettingsFactory.loadSettings());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (backend.getSettings().isShowNightlyWarning() && ExperimentalWindow.VERSION.contains("nightly")) {
            java.awt.EventQueue.invokeLater(new Runnable() { @Override public void run() {
                String message =
                        "This version of Universal Gcode Sender is a nightly build.\n"
                                + "It contains all of the latest features and improvements, \n"
                                + "but may also have bugs that still need to be fixed.\n"
                                + "\n"
                                + "If you encounter any problems, please report them on github.";
                JOptionPane.showMessageDialog(new JFrame(), message,
                        "", JOptionPane.INFORMATION_MESSAGE);
            }});
        }
        initComponents();
        initProgram();
        backend.addControllerListener(this);
        backend.addUGSEventListener(this);

        setSize(backend.getSettings().getMainWindowSettings().width, backend.getSettings().getMainWindowSettings().height);
        setLocation(backend.getSettings().getMainWindowSettings().xLocation, backend.getSettings().getMainWindowSettings().yLocation);

        commandPanel.loadSettings();
        connectionPanel.loadSettings();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                commandPanel.saveSettings();
                connectionPanel.saveSettings();
                SettingsFactory.saveSettings(backend.getSettings());
                
                if(pendantUI!=null){
                    pendantUI.stop();
                }
            }
        });
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ExperimentalWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ExperimentalWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ExperimentalWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ExperimentalWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        // Fix look and feel to use CMD+C/X/V/A instead of CTRL
        if (SystemUtils.IS_OS_MAC)
        {
            Collection<InputMap> ims = new ArrayList<>();
            ims.add((InputMap) UIManager.get("TextField.focusInputMap"));
            ims.add((InputMap) UIManager.get("TextArea.focusInputMap"));
            ims.add((InputMap) UIManager.get("EditorPane.focusInputMap"));
            ims.add((InputMap) UIManager.get("FormattedTextField.focusInputMap"));
            ims.add((InputMap) UIManager.get("PasswordField.focusInputMap"));
            ims.add((InputMap) UIManager.get("TextPane.focusInputMap"));

            int c = KeyEvent.VK_C;
            int v = KeyEvent.VK_V;
            int x = KeyEvent.VK_X;
            int a = KeyEvent.VK_A;
            int meta = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

            for (InputMap im : ims) {
                im.put(KeyStroke.getKeyStroke(c, meta), DefaultEditorKit.copyAction);
                im.put(KeyStroke.getKeyStroke(v, meta), DefaultEditorKit.pasteAction);
                im.put(KeyStroke.getKeyStroke(x, meta), DefaultEditorKit.cutAction);
                im.put(KeyStroke.getKeyStroke(a, meta), DefaultEditorKit.selectAllAction);
            }
        }
        
         /* Create the form */
//        GUIBackend backend = new GUIBackend();
        final ExperimentalWindow mw = new ExperimentalWindow();
        
        /* Apply the settings to the ExperimentalWindow bofore showing it */

        mw.setSize(mw.backend.getSettings().getMainWindowSettings().width, mw.backend.getSettings().getMainWindowSettings().height);
        mw.setLocation(mw.backend.getSettings().getMainWindowSettings().xLocation, mw.backend.getSettings().getMainWindowSettings().yLocation);

        mw.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent ce) {
                mw.backend.getSettings().getMainWindowSettings().height = ce.getComponent().getSize().height;
                mw.backend.getSettings().getMainWindowSettings().width = ce.getComponent().getSize().width;
            }

            @Override
            public void componentMoved(ComponentEvent ce) {
                mw.backend.getSettings().getMainWindowSettings().xLocation = ce.getComponent().getLocation().x;
                mw.backend.getSettings().getMainWindowSettings().yLocation = ce.getComponent().getLocation().y;
            }

            @Override
            public void componentShown(ComponentEvent ce) {}
            @Override
            public void componentHidden(ComponentEvent ce) {}
        });

        /* Display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                mw.setVisible(true);
            }
        });
        

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                mw.connectionPanel.saveSettings();
                mw.commandPanel.saveSettings();

                if(mw.pendantUI!=null){
                    mw.pendantUI.stop();
                }
            }
        });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        controlContextTabbedPane = new javax.swing.JTabbedPane();
        actionPanel = new com.willwinder.universalgcodesender.uielements.action.ActionPanel(backend);
        macroEditPanel = new javax.swing.JScrollPane();
        macroPanel = new com.willwinder.universalgcodesender.uielements.MacroPanel(backend.getSettings(), backend);
        connectionPanel = new com.willwinder.universalgcodesender.uielements.connection.ConnectionPanel(backend);
        commandPanel = new com.willwinder.universalgcodesender.uielements.command.CommandPanel(backend);
        mainMenuBar = new javax.swing.JMenuBar();
        settingsMenu = new javax.swing.JMenu();
        grblConnectionSettingsMenuItem = new javax.swing.JMenuItem();
        firmwareSettingsMenu = new javax.swing.JMenu();
        grblFirmwareSettingsMenuItem = new javax.swing.JMenuItem();
        PendantMenu = new javax.swing.JMenu();
        startPendantServerButton = new javax.swing.JMenuItem();
        stopPendantServerButton = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(640, 520));

        controlContextTabbedPane.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        controlContextTabbedPane.setMinimumSize(new java.awt.Dimension(395, 175));
        controlContextTabbedPane.setPreferredSize(new java.awt.Dimension(2000, 283));
        controlContextTabbedPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                controlContextTabbedPaneComponentShown(evt);
            }
        });
        controlContextTabbedPane.addTab("tab2", actionPanel);

        macroEditPanel.setViewportView(macroPanel);

        controlContextTabbedPane.addTab("Macros", macroEditPanel);

        connectionPanel.setMinimumSize(new java.awt.Dimension(1, 1));
        connectionPanel.setPreferredSize(new java.awt.Dimension(275, 130));

        settingsMenu.setText("Settings");

        grblConnectionSettingsMenuItem.setText("Sender Settings");
        grblConnectionSettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                grblConnectionSettingsMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(grblConnectionSettingsMenuItem);

        firmwareSettingsMenu.setText("Firmware Settings");

        grblFirmwareSettingsMenuItem.setText("GRBL");
        grblFirmwareSettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                grblFirmwareSettingsMenuItemActionPerformed(evt);
            }
        });
        firmwareSettingsMenu.add(grblFirmwareSettingsMenuItem);

        settingsMenu.add(firmwareSettingsMenu);

        mainMenuBar.add(settingsMenu);

        PendantMenu.setText("Pendant");

        startPendantServerButton.setText("Start...");
        startPendantServerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startPendantServerButtonActionPerformed(evt);
            }
        });
        PendantMenu.add(startPendantServerButton);

        stopPendantServerButton.setText("Stop...");
        stopPendantServerButton.setEnabled(false);
        stopPendantServerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopPendantServerButtonActionPerformed(evt);
            }
        });
        PendantMenu.add(stopPendantServerButton);

        mainMenuBar.add(PendantMenu);

        setJMenuBar(mainMenuBar);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(connectionPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(controlContextTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 764, Short.MAX_VALUE)
                    .add(commandPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(controlContextTabbedPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 294, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(commandPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE))
            .add(connectionPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    /** End of generated code.
     */
    


    // TODO: It would be nice to streamline this somehow...
    private void grblConnectionSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_grblConnectionSettingsMenuItemActionPerformed
        ConnectionSettingsDialog gcsd = new ConnectionSettingsDialog(this, true);
        
        // Set initial values.
        gcsd.setSpeedOverrideEnabled(backend.getSettings().isOverrideSpeedSelected());
        gcsd.setSpeedOverridePercent((int) backend.getSettings().getOverrideSpeedValue());
        gcsd.setMaxCommandLength(backend.getSettings().getMaxCommandLength());
        gcsd.setTruncateDecimalLength(backend.getSettings().getTruncateDecimalLength());
        gcsd.setSingleStepModeEnabled(backend.getSettings().isSingleStepMode());
        gcsd.setRemoveAllWhitespace(backend.getSettings().isRemoveAllWhitespace());
        gcsd.setStatusUpdatesEnabled(backend.getSettings().isStatusUpdatesEnabled());
        gcsd.setStatusUpdatesRate(backend.getSettings().getStatusUpdateRate());
        gcsd.setStateColorDisplayEnabled(backend.getSettings().isDisplayStateColor());
        gcsd.setConvertArcsToLines(backend.getSettings().isConvertArcsToLines());
        gcsd.setSmallArcThreshold(backend.getSettings().getSmallArcThreshold());
        gcsd.setSmallArcSegmentLengthSpinner(backend.getSettings().getSmallArcSegmentLength());
        gcsd.setselectedLanguage(backend.getSettings().getLanguage());
        gcsd.setAutoConnectEnabled(backend.getSettings().isAutoConnectEnabled());
        gcsd.setAutoReconnect(backend.getSettings().isAutoReconnect());

        gcsd.setVisible(true);
        
        if (gcsd.saveChanges()) {
            backend.getSettings().setOverrideSpeedSelected(gcsd.getSpeedOverrideEnabled());
            backend.getSettings().setOverrideSpeedValue(gcsd.getSpeedOverridePercent());
            backend.getSettings().setMaxCommandLength(gcsd.getMaxCommandLength());
            backend.getSettings().setTruncateDecimalLength(gcsd.getTruncateDecimalLength());
            backend.getSettings().setSingleStepMode(gcsd.getSingleStepModeEnabled());
            backend.getSettings().setRemoveAllWhitespace(gcsd.getRemoveAllWhitespace());
            backend.getSettings().setStatusUpdatesEnabled(gcsd.getStatusUpdatesEnabled());
            backend.getSettings().setStatusUpdateRate(gcsd.getStatusUpdatesRate());
            backend.getSettings().setDisplayStateColor(gcsd.getDisplayStateColor());
            backend.getSettings().setConvertArcsToLines(gcsd.getConvertArcsToLines());
            backend.getSettings().setSmallArcThreshold(gcsd.getSmallArcThreshold());
            backend.getSettings().setSmallArcSegmentLength(gcsd.getSmallArcSegmentLength());
            backend.getSettings().setLanguage(gcsd.getLanguage());
            backend.getSettings().setAutoConnectEnabled(gcsd.getAutoConnectEnabled());
            backend.getSettings().setAutoReconnect(gcsd.getAutoReconnect());

            if (this.vw != null) {
                vw.setMinArcLength(gcsd.getSmallArcThreshold());
                vw.setArcLength(gcsd.getSmallArcSegmentLength());
            }
        }
    }//GEN-LAST:event_grblConnectionSettingsMenuItemActionPerformed

    private void grblFirmwareSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_grblFirmwareSettingsMenuItemActionPerformed
        try {
            if (!this.backend.isConnected()) {
                displayErrorDialog(Localization.getString("mainWindow.error.noFirmware"));
            } else if (this.backend.getController() instanceof GrblController) {
                    GrblFirmwareSettingsDialog gfsd = new GrblFirmwareSettingsDialog(this, true, this.backend);
                    gfsd.setVisible(true);
            } else {
                displayErrorDialog(Localization.getString("mainWindow.error.notGrbl"));
            }
        } catch (Exception ex) {
                displayErrorDialog(ex.getMessage());
        }
    }//GEN-LAST:event_grblFirmwareSettingsMenuItemActionPerformed

        private void startPendantServerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startPendantServerButtonActionPerformed
            this.pendantUI = new PendantUI(backend);
            Collection<PendantURLBean> results = this.pendantUI.start();
            for (PendantURLBean result : results) {
                this.messageForConsole(MessageType.INFO, "Pendant URL: " + result.getUrlString());
            }
            this.startPendantServerButton.setEnabled(false);
            this.stopPendantServerButton.setEnabled(true);
            this.backend.addControllerListener(pendantUI);
        }//GEN-LAST:event_startPendantServerButtonActionPerformed

        private void stopPendantServerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopPendantServerButtonActionPerformed
            this.pendantUI.stop();
            this.startPendantServerButton.setEnabled(true);
            this.stopPendantServerButton.setEnabled(false);
        }//GEN-LAST:event_stopPendantServerButtonActionPerformed

    private void controlContextTabbedPaneComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_controlContextTabbedPaneComponentShown
        // TODO add your handling code here:
    }//GEN-LAST:event_controlContextTabbedPaneComponentShown


    private void initProgram() {
        Localization.initialize(this.backend.getSettings().getLanguage());
        try {
            backend.applySettings(backend.getSettings());
        } catch (Exception e) {
            displayErrorDialog(e.getMessage());
        }
        
        this.setLocalLabels();
        this.setTitle(Localization.getString("title") + " ("
                + Localization.getString("version") + " " + VERSION + ")");

        // Add keyboard listener for manual controls.

    }






    /**
     * Updates all text labels in the GUI with localized labels.
     */
    private void setLocalLabels() {
        this.controlContextTabbedPane.setTitleAt(0, Localization.getString("mainWindow.swing.controlContextTabbedPane.machineControl"));
        this.controlContextTabbedPane.setTitleAt(1, Localization.getString("mainWindow.swing.controlContextTabbedPane.macros"));
        this.firmwareSettingsMenu.setText(Localization.getString("mainWindow.swing.firmwareSettingsMenu"));
        this.grblConnectionSettingsMenuItem.setText(Localization.getString("mainWindow.swing.grblConnectionSettingsMenuItem"));
        this.grblFirmwareSettingsMenuItem.setText(Localization.getString("mainWindow.swing.grblFirmwareSettingsMenuItem"));
        this.settingsMenu.setText(Localization.getString("mainWindow.swing.settingsMenu"));
        this.macroEditPanel.setToolTipText(Localization.getString("mainWindow.swing.macroInstructions"));
    }

    /**
     * SerialCommunicatorListener implementation.
     */
    
    @Override
    public void fileStreamComplete(String filename, boolean success) {
        final String durationLabelCopy = connectionPanel.getDuration();
        if (success) {
            java.awt.EventQueue.invokeLater(() -> {
                JOptionPane.showMessageDialog(new JFrame(),
                        Localization.getString("mainWindow.ui.jobComplete") + " " + durationLabelCopy,
                        Localization.getString("success"), JOptionPane.INFORMATION_MESSAGE);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {}

            });
        } else {
            displayErrorDialog(Localization.getString("mainWindow.error.jobComplete"));
        }
    }
    
    @Override
    public void commandSkipped(GcodeCommand command) {
        commandSent(command);
    }
     
    @Override
    public void commandSent(final GcodeCommand command) {

    }
    
    @Override
    public void commandComment(String comment) {

    }
    
    @Override
    public void commandComplete(final GcodeCommand command) {

    }

    @Override
    public void messageForConsole(MessageType type, String msg) {

    }

    @Override
    public void statusStringListener(String state, Position machineCoord, Position workCoord) {

    }
    
    @Override
    public void postProcessData(int numRows) {
    }
    
    /**
     * Updates the visualizer with the processed gcode file if it is available,
     * otherwise uses the unprocessed file.
     */
    private void setVisualizerFile() {
        if (vw == null) return;

        if (processedGcodeFile == null) {
            if (gcodeFile == null) {
                return;
            }
            vw.setGcodeFile(gcodeFile);
        } else {
            vw.setProcessedGcodeFile(processedGcodeFile);
        }
    }

    @Override
    public void UGSEvent(UGSEvent evt) {
        if (evt.isFileChangeEvent()) {
            switch(evt.getFileState()) {
                case FILE_LOADING:
                    processedGcodeFile = null;
                    gcodeFile = evt.getFile();
                    break;

                case FILE_LOADED:
                    processedGcodeFile = evt.getFile();
                    break;

                default:
                    break;
            }

            setVisualizerFile();
        }
    }

    // Generated variables.
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu PendantMenu;
    private com.willwinder.universalgcodesender.uielements.action.ActionPanel actionPanel;
    private com.willwinder.universalgcodesender.uielements.command.CommandPanel commandPanel;
    private com.willwinder.universalgcodesender.uielements.connection.ConnectionPanel connectionPanel;
    private javax.swing.JTabbedPane controlContextTabbedPane;
    private javax.swing.JMenu firmwareSettingsMenu;
    private javax.swing.JMenuItem grblConnectionSettingsMenuItem;
    private javax.swing.JMenuItem grblFirmwareSettingsMenuItem;
    private javax.swing.JScrollPane macroEditPanel;
    private com.willwinder.universalgcodesender.uielements.MacroPanel macroPanel;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JMenu settingsMenu;
    private javax.swing.JMenuItem startPendantServerButton;
    private javax.swing.JMenuItem stopPendantServerButton;
    // End of variables declaration//GEN-END:variables

}

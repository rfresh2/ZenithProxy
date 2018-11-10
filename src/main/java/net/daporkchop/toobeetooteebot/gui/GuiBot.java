/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2018 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.toobeetooteebot.gui;

import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;
import net.daporkchop.toobeetooteebot.util.Config;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.TimerTask;

public class GuiBot extends JFrame {
    public static GuiBot INSTANCE;

    public java.util.Timer guiTimer = new java.util.Timer();

    public JPanel contentPane;
    public JTextField chatInput;
    public JTextField usernameIn;
    public JPasswordField passwordIn;
    public JTextField targetIpIn;
    public JTextField bindHostIn;
    public JCheckBox doAuthenticationIn;
    public JSpinner targetPortIn;
    public JCheckBox doAntiAfkIn;
    public JCheckBox doAutoRespawnIn;
    public JCheckBox doAutoRelogIn;
    public JCheckBox doServerIn;
    public JSpinner bindPortIn;
    public JCheckBox statCollectionIn;
    public JCheckBox processChatIn;
    public JLabel usernameLabel;
    public JLabel chatDisplay;
    public JButton connect_disconnectButton;

    /**
     * Create the frame.
     */
    public GuiBot() {
        super();
        INSTANCE = this;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e1) {
            e1.printStackTrace();
            Runtime.getRuntime().exit(0);
        }

        this.setIconImage(Toolkit.getDefaultToolkit()
                .getImage(GuiBot.class.getResource("/DaPorkchop_.png")));
        this.setTitle("Pork2b2tBot v0.1 alpha");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setBounds(100, 100, 350, 500);
        this.contentPane = new JPanel();
        this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.setContentPane(this.contentPane);

        this.usernameLabel = new JLabel(TooBeeTooTeeBot.bot.protocol.getProfile().getName());

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        GroupLayout gl_contentPane = new GroupLayout(this.contentPane);
        gl_contentPane.setHorizontalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
                .addComponent(this.usernameLabel, GroupLayout.DEFAULT_SIZE, 324, Short.MAX_VALUE)
                .addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 324, Short.MAX_VALUE));
        gl_contentPane.setVerticalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_contentPane.createSequentialGroup().addComponent(this.usernameLabel)
                        .addPreferredGap(ComponentPlacement.RELATED).addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE,
                                431, Short.MAX_VALUE)));

        JPanel tabMain = new JPanel();
        tabbedPane.addTab("Main", null, tabMain, null);

        this.connect_disconnectButton = new JButton("Connect");
        this.connect_disconnectButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
                if (TooBeeTooTeeBot.bot.client == null) {
                    GuiBot.this.connect_disconnectButton.setText("Disconnect");
                    GuiBot.this.connect_disconnectButton.setEnabled(false);
                    GuiBot.this.guiTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            TooBeeTooTeeBot.bot.start(new String[]{"firstart"});
                        }
                    }, 0);
                } else if (TooBeeTooTeeBot.bot.client.getSession().isConnected()) {
                    GuiBot.this.connect_disconnectButton.setText("Connect");
                    GuiBot.this.connect_disconnectButton.setEnabled(false);
                    GuiBot.this.guiTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            TooBeeTooTeeBot.bot.client.getSession().disconnect("User disconnected");
                        }
                    }, 0);
                } else {
                    GuiBot.this.connect_disconnectButton.setText("Disconnect");
                    GuiBot.this.connect_disconnectButton.setEnabled(false);
                    GuiBot.this.guiTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            TooBeeTooTeeBot.bot.cleanUp();
                            TooBeeTooTeeBot.bot.start(new String[0]);
                        }
                    }, 0);
                }
            }
        });

        JButton sendButton = new JButton("Send");

        this.chatInput = new JTextField();
        this.chatInput.setColumns(10);

        JScrollPane scrollPane = new JScrollPane();
        GroupLayout gl_tabMain = new GroupLayout(tabMain);
        gl_tabMain.setHorizontalGroup(gl_tabMain.createParallelGroup(Alignment.TRAILING)
                .addComponent(this.connect_disconnectButton, GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)
                .addGroup(gl_tabMain.createSequentialGroup()
                        .addComponent(this.chatInput, GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED).addComponent(sendButton))
                .addComponent(scrollPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE));
        gl_tabMain.setVerticalGroup(gl_tabMain.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_tabMain.createSequentialGroup().addComponent(this.connect_disconnectButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(gl_tabMain.createParallelGroup(Alignment.BASELINE).addComponent(sendButton)
                                .addComponent(this.chatInput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.PREFERRED_SIZE))));

        scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                e.getAdjustable().setValue(e.getAdjustable().getMaximum());
            }
        });

        this.chatDisplay = new JLabel("<html>Welcome to <strong>Pork2b2tBot v0.1a</strong>!</html>");
        this.chatDisplay.setVerticalAlignment(SwingConstants.TOP);
        this.chatDisplay.setHorizontalAlignment(SwingConstants.LEFT);
        scrollPane.setViewportView(this.chatDisplay);
        tabMain.setLayout(gl_tabMain);
        this.chatDisplay.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JPanel tabConfig = new JPanel();
        tabbedPane.addTab("Config", null, tabConfig, null);

        JScrollPane scrollPane_1 = new JScrollPane();
        scrollPane_1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane_1.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JButton btnSave = new JButton("Save");
        btnSave.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
                Config.parser.set("login.username", Config.username = GuiBot.this.usernameIn.getText());
                Config.parser.set("login.password", Config.password = GuiBot.this.passwordIn.getText());
                Config.parser.set("login.doAuthentication", Config.doAuth = GuiBot.this.doAuthenticationIn.isSelected());
                Config.parser.set("server.host", Config.serverHost = GuiBot.this.bindHostIn.getText());
                Config.parser.set("server.port", Config.serverPort = (int) GuiBot.this.bindPortIn.getValue());
                Config.parser.set("server.doServer", Config.doServer = GuiBot.this.doServerIn.isSelected());
                Config.parser.set("misc.antiafk", Config.doAntiAFK = GuiBot.this.doAntiAfkIn.isSelected());
                Config.parser.set("client.hostIP", Config.ip = GuiBot.this.targetIpIn.getText());
                Config.parser.set("client.hostPort", Config.port = (int) GuiBot.this.targetPortIn.getValue());
                Config.parser.set("misc.autorespawn", Config.doAntiAFK = GuiBot.this.doAntiAfkIn.isSelected());
                Config.parser.set("misc.doAutoRelog", Config.doAutoRelog = GuiBot.this.doAutoRelogIn.isSelected());
                Config.parser.set("stats.doStats", Config.doStatCollection = GuiBot.this.statCollectionIn.isSelected());
                Config.parser.set("chat.doProcess", Config.processChat = GuiBot.this.statCollectionIn.isSelected());

                Config.parser.save();
            }
        });
        GroupLayout gl_tabConfig = new GroupLayout(tabConfig);
        gl_tabConfig.setHorizontalGroup(gl_tabConfig.createParallelGroup(Alignment.LEADING)
                .addComponent(btnSave, GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)
                .addComponent(scrollPane_1, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE));
        gl_tabConfig.setVerticalGroup(gl_tabConfig.createParallelGroup(Alignment.LEADING).addGroup(Alignment.TRAILING,
                gl_tabConfig.createSequentialGroup()
                        .addComponent(scrollPane_1, GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED).addComponent(btnSave)));

        JPanel panel = new JPanel();
        scrollPane_1.setViewportView(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JPanel panel1 = new JPanel();
        panel1.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        JPanel panel2 = new JPanel();
        panel2.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel1, panel2);
        splitPane.setEnabled(false);
        splitPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        splitPane.setDividerSize(0);
        panel2.setLayout(new GridLayout(13, 1, 0, 0));

        JLabel labelUsername = new JLabel("Username");
        labelUsername.setToolTipText(
                "<html>Your ingame username.<br>If you're using authentication, this needs to be the same as you would enter into the launcher.<br>If not, we'll generate a cracked account using the username only.</html>");
        panel2.add(labelUsername);

        JLabel labelPassword = new JLabel("Password");
        labelPassword.setToolTipText(
                "<html>The password used for authenticating.<br>Only needed if authentication is enabled.</html>");
        panel2.add(labelPassword);

        JLabel labelDoAuthentication = new JLabel("Do Authentication");
        labelDoAuthentication.setToolTipText("<html>If this is enabled, then we will attempt to authenticate with Mojang using the username and password.<br>If not, we will log in using a cracked account based on the username.</html>");
        panel2.add(labelDoAuthentication);

        JLabel labelTargetIp = new JLabel("Target IP");
        labelTargetIp.setToolTipText("<html>The IP of the server to connect to.<br>If the IP is in the format of example.com:number, do <strong>NOT</strong> add the :number here!<br>Remove the : and put the number in the target port section.</html>");
        panel2.add(labelTargetIp);

        JLabel lblTargetPort = new JLabel("Target Port");
        lblTargetPort.setToolTipText("<html>The port of the server to connect to.<br>Generally can be left as is.</html>");
        panel2.add(lblTargetPort);

        JLabel lblDoAntiafk = new JLabel("Do AntiAFK");
        lblDoAntiafk.setToolTipText("<html>Rotates the player randomly to prevent getting kicked for being AFK.<br>If the server is active as well as this, AntiAFK will only run when no clients are connected.</html>");
        panel2.add(lblDoAntiafk);

        JLabel lblDoAutorespawn = new JLabel("Do AutoRespawn");
        lblDoAutorespawn.setToolTipText("<html>If this is enabled, the bot will automatically respawn after death.<br>If not, it will just sit there and do nothing, there's really no reason to disable it lol</html>");
        panel2.add(lblDoAutorespawn);

        JLabel lblDoAutorelog = new JLabel("Do AutoRelog");
        lblDoAutorelog.setToolTipText("<html>If this is enabled, the bot will try to reconnect after getting disconnected/kicked.<br>There's a 10 second wait before reconnects.</html>");
        panel2.add(lblDoAutorelog);

        JLabel lblDoServer = new JLabel("Do Server");
        lblDoServer.setToolTipText("<html>If this is enabled, a server will be opened inside the bot that you can connect to.<br>This allows you to move the bot around and such without having to shut it down.<br>Essentially just PorkProxy lawl</html>");
        panel2.add(lblDoServer);

        JLabel lblBindHost = new JLabel("Bind Host");
        lblBindHost.setToolTipText("<html>The host to bind the server to.<br>By default this is 0.0.0.0 (all requests will be accepted)</html>");
        panel2.add(lblBindHost);

        JLabel lblBindPort = new JLabel("Bind Port");
        lblBindPort.setToolTipText("<html>The port to bind to.</html>");
        panel2.add(lblBindPort);

        JLabel lblDoStatCollection = new JLabel("Do Stat Collection");
        lblDoStatCollection.setToolTipText("<html>Whether or not to collect statistics about users.<br>Experimental, don't use this lol</html>");
        panel2.add(lblDoStatCollection);

        JLabel lblProcessChat = new JLabel("Process Chat");
        lblProcessChat.setToolTipText("<html>Not going to bother documenting this, it's pretty advanced. Read the code.</html>");
        panel2.add(lblProcessChat);
        panel1.setLayout(new GridLayout(13, 1, 0, 0));

        this.usernameIn = new JTextField();
        panel1.add(this.usernameIn);
        this.usernameIn.setColumns(10);
        this.usernameIn.setText(Config.username);

        this.passwordIn = new JPasswordField();
        panel1.add(this.passwordIn);
        this.passwordIn.setText(Config.password);

        this.doAuthenticationIn = new JCheckBox("");
        this.doAuthenticationIn.setHorizontalAlignment(SwingConstants.CENTER);
        this.doAuthenticationIn.setSelected(Config.doAuth);
        panel1.add(this.doAuthenticationIn);

        this.targetIpIn = new JTextField();
        panel1.add(this.targetIpIn);
        this.targetIpIn.setColumns(10);
        this.targetIpIn.setText(Config.ip);

        this.targetPortIn = new JSpinner();
        this.targetPortIn.setModel(new SpinnerNumberModel(25565, 1, 65535, 1));
        this.targetPortIn.setEditor(new JSpinner.NumberEditor(this.targetPortIn, "#"));
        this.targetPortIn.setValue(Integer.valueOf(Config.port));
        panel1.add(this.targetPortIn);

        this.doAntiAfkIn = new JCheckBox("");
        this.doAntiAfkIn.setHorizontalAlignment(SwingConstants.CENTER);
        this.doAntiAfkIn.setSelected(Config.doAntiAFK);
        panel1.add(this.doAntiAfkIn);

        this.doAutoRespawnIn = new JCheckBox("");
        this.doAutoRespawnIn.setHorizontalAlignment(SwingConstants.CENTER);
        this.doAutoRespawnIn.setSelected(Config.doAutoRespawn);
        panel1.add(this.doAutoRespawnIn);

        this.doAutoRelogIn = new JCheckBox("");
        this.doAutoRelogIn.setHorizontalAlignment(SwingConstants.CENTER);
        this.doAutoRelogIn.setSelected(Config.doAutoRelog);
        panel1.add(this.doAutoRelogIn);

        this.doServerIn = new JCheckBox("");
        this.doServerIn.setHorizontalAlignment(SwingConstants.CENTER);
        this.doServerIn.setSelected(Config.doServer);
        panel1.add(this.doServerIn);

        this.bindHostIn = new JTextField();
        panel1.add(this.bindHostIn);
        this.bindHostIn.setColumns(10);
        this.bindHostIn.setText(Config.serverHost);

        this.bindPortIn = new JSpinner();
        this.bindPortIn.setModel(new SpinnerNumberModel(10293, 1, 65535, 1));
        this.bindPortIn.setEditor(new JSpinner.NumberEditor(this.bindPortIn, "#"));
        this.bindPortIn.setValue(Integer.valueOf(Config.serverPort));
        panel1.add(this.bindPortIn);

        this.statCollectionIn = new JCheckBox("");
        this.statCollectionIn.setHorizontalAlignment(SwingConstants.CENTER);
        this.statCollectionIn.setSelected(Config.doStatCollection);
        panel1.add(this.statCollectionIn);

        this.processChatIn = new JCheckBox("");
        this.processChatIn.setHorizontalAlignment(SwingConstants.CENTER);
        this.processChatIn.setSelected(Config.doStatCollection);
        panel1.add(this.processChatIn);
        splitPane.setResizeWeight(0.5);
        GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addComponent(splitPane,
                GroupLayout.DEFAULT_SIZE, 317, Short.MAX_VALUE));
        gl_panel.setVerticalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addComponent(splitPane,
                GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE));
        panel.setLayout(gl_panel);
        tabConfig.setLayout(gl_tabConfig);

        JPanel tabAbout = new JPanel();
        tabbedPane.addTab("About", null, tabAbout, null);

        JLabel labelAbout = new JLabel("<html><h1>About</h1><br>Made by <strong>DaPorkchop_</strong><br>Powered by <strong>MCProtocolLib</strong></html>");

        JLabel lblsourceCode = new JLabel("<html><a href=\"https://github.com/DaMatrix/Pork2b2tBot\">Source Code</a></html>");
        lblsourceCode.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/DaMatrix/Pork2b2tBot"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });
        GroupLayout gl_tabAbout = new GroupLayout(tabAbout);
        gl_tabAbout.setHorizontalGroup(
                gl_tabAbout.createParallelGroup(Alignment.LEADING)
                        .addGroup(gl_tabAbout.createSequentialGroup()
                                .addGroup(gl_tabAbout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(labelAbout)
                                        .addComponent(lblsourceCode))
                                .addContainerGap(197, Short.MAX_VALUE))
        );
        gl_tabAbout.setVerticalGroup(
                gl_tabAbout.createParallelGroup(Alignment.LEADING)
                        .addGroup(gl_tabAbout.createSequentialGroup()
                                .addComponent(labelAbout)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(lblsourceCode)
                                .addContainerGap(278, Short.MAX_VALUE))
        );
        tabAbout.setLayout(gl_tabAbout);

        sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (TooBeeTooTeeBot.bot.client != null && TooBeeTooTeeBot.bot.client.getSession().isConnected()) {
                    TooBeeTooTeeBot.bot.queueMessage(GuiBot.this.chatInput.getText());
                    GuiBot.this.chatInput.setText("");
                }
            }
        });
        this.contentPane.setLayout(gl_contentPane);

        this.guiTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (TooBeeTooTeeBot.bot.client == null) {
                    GuiBot.this.connect_disconnectButton.setText("Connect");
                } else if (!TooBeeTooTeeBot.bot.client.getSession().isConnected()) {
                    GuiBot.this.connect_disconnectButton.setText("Connect");
                } else {
                    GuiBot.this.connect_disconnectButton.setText("Disconnect");
                }
            }
        }, 0, 500);
    }
}


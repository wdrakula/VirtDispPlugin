package com.company.virtdispplugin;


import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import processing.app.Editor;
import processing.app.tools.Tool;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;

import static java.awt.Color.*;


public class VirtDispPlugin implements Tool {
    Editor editor;

    public void init(Editor editor) {
        this.editor = editor;
    }

    public String getMenuTitle() {
        return "VirtDispPlugin";
    }

    boolean isRunning = false;
    JFrame mainWindow = new JFrame("Virtual Display");
    JPanel topPanel = new JPanel();
    JPanel bottomPanel = new JPanel();
    static JLabel dimensionLabel = new JLabel("dimensions");
    static JLabel virtDim = new JLabel("     Virtual Display");
    DrawArea drawPanel = new DrawArea(dimensionLabel, virtDim);
    JButton startStopButton = new JButton("START");
    JButton rescanButton = new JButton("Rescan");
    DefaultListModel<String> lm = new DefaultListModel<>();
    JList<String> l = new JList<>(lm);
    static Debugger debug = new Debugger(false);



    SerialPort serialPort;
    boolean serialSet = false;

    public void run() {

        scanSerialPorts();
        startStopButton.addActionListener(e -> {
            if (isRunning) {
                isRunning = false;
                if (serialPort != null && serialPort.isOpened()) {
                    try {
                        serialPort.closePort();
                    } catch (SerialPortException ex) {
                        ex.printStackTrace();
                    }
                }
                startStopButton.setText("START");
            }
            else {
                setSerialPort(String.valueOf(l.getSelectedValue()));
                isRunning = true;
                startStopButton.setText("STOP");
            }
        });

        rescanButton.addActionListener(e -> scanSerialPorts());

        dimensionLabel.setBounds(0,0,380,40);

        l.setBounds(0,0,380,100);
        startStopButton.setBounds(0,0,200,50);
        rescanButton.setBounds(0,0,200,50);

        topPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        topPanel.setBounds(0,0,790,190);
        topPanel.setLayout(new BorderLayout());
        topPanel.add(dimensionLabel,BorderLayout.WEST);
        topPanel.add(virtDim,BorderLayout.CENTER);
        topPanel.add(startStopButton,BorderLayout.EAST);

        bottomPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        bottomPanel.setBounds(0,0,790,190);
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(rescanButton,BorderLayout.EAST);
        bottomPanel.add(l,BorderLayout.WEST);

        drawPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        drawPanel.setBounds(0,0,400,200);

        mainWindow.setSize(800,400);
        mainWindow.setLayout(new BorderLayout());
        mainWindow.add(topPanel, BorderLayout.NORTH);
        mainWindow.add(bottomPanel, BorderLayout.SOUTH);
        mainWindow.add(drawPanel, BorderLayout.CENTER);
        mainWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                debug.log("Main window closed");
                if (serialPort != null && serialPort.isOpened()) {
                    try {
                        serialPort.closePort();
                    } catch (SerialPortException ex) {
                        ex.printStackTrace();
                    }
                }
                System.exit(0);
            }
        });

        mainWindow.setVisible(true);

    }
    private static class DrawArea extends JPanel {
        Dimension dimension;
        JLabel dimLabel;
        JLabel virtLabel;
        int width;
        int height;
        Color [] screenPixels;



        public DrawArea (JComponent _dimLabel, JComponent _virtLabel) {
            setPreferredSize(new Dimension(800, 600));
            dimLabel = (JLabel) _dimLabel;
            virtLabel = (JLabel) _virtLabel;
        }

        void init(int w, int h) {
            width = w;
            height = h;
            screenPixels = new Color[width*height];
            virtLabel.setText("   Virtual screen " + width + "x" + height);
            repaint();
        }

        void clear(int x, int y, int w, int h) {
            if (screenPixels != null &&
                    x + w <= width && y + h <= height) {
                for(int j = 0; j < h; j++) {
                    int s = (y + j) * width;
                    for (int i = 0; i < w; i++) {
                        screenPixels[s + x + i] = BLACK;
                    }
                }
                //repaint();
            }
        }

        void set(int x, int y, int w, int h, byte [] pixels) {
            if (screenPixels != null && x + w <= width && y + h <= height) {
                for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        if ((pixels[j * ((w + 7) / 8) + i / 8] & (0x80 >> (i & 7))) != 0)
                            screenPixels[(y + j) * width + x + i] = WHITE;
                    }
            }
            repaint();
        }
        }




        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            dimension = this.getSize();
            dimLabel.setText("Size " + dimension.width + "x" + dimension.height);
            double dx = (double) dimension.width / width;
            double dy = (double) dimension.height / height;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(BLACK);
            g2d.fill(new Rectangle2D.Double(0,0,dimension.width, dimension.height));
            g2d.setColor(WHITE);
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    if (screenPixels[j * width + i] == WHITE) {
                    g2d.fill(new Rectangle2D.Double(i * dx, j * dy, dx, dy));
                    }
                }
            }

        }
    }

    public void scanSerialPorts () {
        lm.removeAllElements();
        for (String pn : SerialPortList.getPortNames()) {
            lm.addElement(pn);
        }
        l.setSelectedIndex(lm.getSize()-1);
        l.ensureIndexIsVisible(lm.getSize()-1);

    }
    public void setSerialPort(String portName) {
        MyBytes myPacket = new MyBytes(0);

        final byte[] Prefix = {(byte)0xff, (byte)0xaa, 0x55};

        if (serialPort != null && serialPort.isOpened()) {
            try {
                serialPort.closePort();
            } catch (SerialPortException ex) {
                ex.printStackTrace();
            }
        }
        serialPort = new SerialPort(portName);
        try {
            serialPort.openPort();
            serialPort.setParams(230400,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            serialPort.addEventListener(serialPortEvent -> {
                int x, y, w, h;
                int cmd;
                if (isRunning && serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() > 0) {
                    try {
                        myPacket.append(serialPort.readBytes(serialPortEvent.getEventValue()));

                        int i = 0;
                        while (i+2 < myPacket.length) {
                            if (myPacket.containsAt(i, Prefix)) break;
                            i++;
                        }

                        myPacket.remove(i);

                        while (myPacket.containsAt(0, Prefix) && myPacket.length >= 12) {
                            boolean uncomlete = false;
                            cmd = myPacket.get(3);
                            x = (myPacket.get(5) & 0xff) << 8 | (myPacket.get(4) & 0xff);
                            y = (myPacket.get(7) & 0xff) << 8 | (myPacket.get(6) & 0xff);
                            w = (myPacket.get(9) & 0xff) << 8 | (myPacket.get(8) & 0xff);
                            h = (myPacket.get(11) & 0xff) << 8 | (myPacket.get(10) & 0xff);
                            switch (cmd) {
                                case 0: drawPanel.init(w,h);
                                    debug.log("get command=" + cmd + " width=" + w + " height=" + h);
                                    myPacket.remove(12);
                                    serialPort.writeString("OK555");
                                    break;
                                case 1: drawPanel.clear(x,y,w,h);
                                    debug.log("get command=" + cmd + " width=" + w + " height=" + h);
                                    myPacket.remove(12);
                                    break;
                                case 2:
                                    if (myPacket.length >= 12 + (w+7)/8 * h) {
                                        drawPanel.set(x, y, w, h, myPacket.get(12, (w + 7) / 8 * h));
                                        myPacket.remove(12 + (w + 7) / 8 * h);
                                        debug.log("get command=" + cmd + " width=" + w + " height=" + h);
                                    }
                                    else uncomlete = true;
                                    break;
                                default:
                                    myPacket.remove(12);
                                    break;
                            }
                            if (uncomlete) break;
                        }
                        debug.log("Packet buffer len = " + myPacket.length);

                    }
                    catch(SerialPortException ex){
                        ex.printStackTrace();
                    }

                }
            }, SerialPort.MASK_RXCHAR);
            serialSet = true;
        } catch (SerialPortException ex) {
            ex.printStackTrace();
        }
    }



    public VirtDispPlugin() {
        run();
    }

    public static void main(String[] args) {
        new VirtDispPlugin();
    }

}


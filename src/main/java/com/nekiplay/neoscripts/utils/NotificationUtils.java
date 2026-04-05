package com.nekiplay.neoscripts.utils;

import org.apache.commons.lang3.SystemUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class NotificationUtils {
    private static NotificationUtils instance;
    private final TrayIcon trayIcon;

    public NotificationUtils() {
        if (!SystemUtils.IS_OS_WINDOWS || GraphicsEnvironment.isHeadless()) {
            trayIcon = null;
            return;
        }
        BufferedImage image;
        try {
            image = ImageIO.read(Objects.requireNonNull(getClass().getResource("/assets/neoscripts/logo.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        trayIcon = new TrayIcon(image, "Farm Helper Failsafe Notification");
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("Farm Helper Failsafe Notification");
        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static NotificationUtils getInstance() {
        if (instance == null) {
            instance = new NotificationUtils();
        }
        return instance;
    }

    public void sendNotification(String title, String text) {
        try {
            // Проверка headless для Windows
            if (SystemUtils.IS_OS_WINDOWS) {
                if (GraphicsEnvironment.isHeadless()) {
                    System.out.println("Headless mode detected, cannot show system tray notification.");
                    return;
                }
                windows(title, text);
            } else if (SystemUtils.IS_OS_MAC_OSX) {
                mac(title, text);
            } else if (SystemUtils.IS_OS_LINUX) {
                linux(title, text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void windows(String title, String text) {
        if (SystemTray.isSupported() && trayIcon != null) {
            try {
                trayIcon.displayMessage("Farm Helper", text, TrayIcon.MessageType.INFO);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("SystemTray is not supported or trayIcon is null");
        }
    }

    private void mac(String title, String text) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("osascript", "-e", "display notification \"" + text + "\" with title \"" + title + "\"");
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void linux(String title, String text) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("notify-send", "-a", title, text);
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

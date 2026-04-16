package com.yourfamily.pdf.secure_pdf_converter.core.update;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
				
import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

    /**
     * Kovert Update System
     * Uses GitHub as a zero-cost backend for version tracking and distribution.
     */
    public final class UpdateChecker {

        // 🔥 Current version of the installed app
        private static final String CURRENT_VERSION = "1.0.0";

        private static final String VERSION_URL =
                "https://raw.githubusercontent.com/Soodkrish/Kovert/main/version.txt";

        private static final String DOWNLOAD_URL =
                "https://github.com/Soodkrish/Kovert";

        private static final File CONFIG_FILE =
                new File(System.getProperty("user.home"), ".kovert/update.properties");

        public static void check() {
            // Run check in a background thread to prevent startup lag
        	Thread t = new Thread(() -> {
        	    try {
        	        Thread.sleep(2000);

        	        URI uri = URI.create(VERSION_URL);
        	        URLConnection conn = uri.toURL().openConnection();
        	        conn.setConnectTimeout(5000);
        	        conn.setReadTimeout(5000);

        	        try (BufferedReader reader = new BufferedReader(
        	                new InputStreamReader(conn.getInputStream()))) {

        	            String line = reader.readLine();
        	            if (line == null) return;

        	            String latestVersion = line.trim();

        	            if (!isNewer(CURRENT_VERSION, latestVersion)) return;

        	            Properties props = loadProps();
        	            String skipped = props.getProperty("skip", "");

        	            if (latestVersion.equals(skipped)) return;

        	            Platform.runLater(() -> showDialog(latestVersion, props));
        	        }

        	    } catch (Exception e) {
        	        System.err.println("Kovert Update Check Failed: " + e.getMessage());
        	    }
        	});

        	// 🔥 ADD THIS LINE
        	t.setDaemon(true);

        	t.start();
        	
        }
        
        private static void showDialog(String latestVersion, Properties props) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Update Available");
            alert.setHeaderText("Update Available 🚀");
            alert.setContentText("Version " + latestVersion + " is now available. Would you like to download it?");

            ButtonType download = new ButtonType("Download Now");
            ButtonType remind = new ButtonType("Later");
            ButtonType skip = new ButtonType("Skip Version");

            alert.getButtonTypes().setAll(download, remind, skip);

            alert.showAndWait().ifPresent(response -> {

                if (response == download) {
                    try {
                        Desktop.getDesktop().browse(new URI(DOWNLOAD_URL));
                    } catch (Exception e) {
                        System.err.println("Failed to open browser");
                    }
                } else if (response == skip) {
                    props.setProperty("skip", latestVersion);
                    saveProps(props);
                }

            });
        }

        private static Properties loadProps() {
            Properties props = new Properties();
            if (CONFIG_FILE.exists()) {
                try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                    props.load(in);
                } catch (IOException ignored) {}
            }
            return props;
        }

        private static void saveProps(Properties props) {
            try {
                if (CONFIG_FILE.getParentFile().exists() || CONFIG_FILE.getParentFile().mkdirs()) {
                    try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
                        props.store(out, "Kovert Update Settings");
                    }
                }
            } catch (IOException ignored) {}
        }

        /**
         * Compares two semantic version strings (e.g., 1.1.2 vs 1.2.0)
         * Returns true if 'latest' is higher than 'current'
         */
        private static boolean isNewer(String current, String latest) {
            try {
                String[] cParts = current.split("\\.");
                String[] lParts = latest.split("\\.");
                int length = Math.max(cParts.length, lParts.length);

                for (int i = 0; i < length; i++) {
                    int cValue = i < cParts.length ? Integer.parseInt(cParts[i].replaceAll("[^0-9]", "")) : 0;
                    int lValue = i < lParts.length ? Integer.parseInt(lParts[i].replaceAll("[^0-9]", "")) : 0;

                    if (lValue > cValue) return true;
                    if (lValue < cValue) return false;
                }
            } catch (NumberFormatException e) {
                return false; // Fail safe
            }
            return false;
        }
    }
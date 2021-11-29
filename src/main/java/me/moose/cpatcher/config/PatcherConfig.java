package me.moose.cpatcher.config;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class PatcherConfig {
    public String ASSET_URL;
    public String AUTH_URL;
    public String TITLE;
    public PatcherConfig() {
        File file = new File(System.getProperty("user.home") + "/.lunarclient/cpatcher.properties");

        if (!file.exists()) {
            try {
                file.createNewFile();

                FileOutputStream output = new FileOutputStream(file);

                output.write("ASSET_URL=ws://localhost:80\n".getBytes());
                output.write("AUTH_URL=default\n".getBytes());
                output.write("TITLE=Broken Client\n".getBytes());

                output.flush();
                output.close();
            } catch (IOException io) {
                io.printStackTrace();
            }
        }

        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(System.getProperty("user.home") + "/.lunarclient/cpatcher.properties");

            prop.load(input);
            ASSET_URL = (String) prop.getOrDefault("ASSET_URL", "ws://localhost:80");
            AUTH_URL = (String) prop.getOrDefault("AUTH_URL", "default");
            TITLE = (String) prop.getOrDefault("TITLE", "Broken Client");

        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

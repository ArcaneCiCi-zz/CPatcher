package me.moose.cpatcher;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.moose.cpatcher.config.PatcherConfig;
import me.moose.cpatcher.patch.PatcherTransformer;

import java.lang.instrument.Instrumentation;

public class PatcherAgent {
    @Getter private static Version currentVersion;
    @Getter private static PatcherConfig config;
    public static void agentmain(String agentArgs, Instrumentation inst) {
        config = new PatcherConfig();
        try {
            inst.addTransformer(new PatcherTransformer());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    @AllArgsConstructor @Getter
    public static enum Version {
        v1_7("1.7"),
        v1_8("1.8"),
        v1_12("1.12"),
        v1_16("1.16"),
        v1_17("1.17"),
        v1_18("1.18");
        private String name;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        config = new PatcherConfig();
        String[] args = System.getProperty("sun.java.command").split(" ");
        boolean nextVersion = false;
        for(String s : args) {
            if(nextVersion) {
                currentVersion = Version.valueOf("v" + s.toLowerCase().replace(".", "_"));
                break;
            }
            if(s.equalsIgnoreCase("--version")) {
                nextVersion= true;
            }
        }
        System.out.println("[CPatcher] Got Version: " + currentVersion.name());
        inst.addTransformer(new PatcherTransformer());

    }

}

package me.moose.cpatcher.patch;


import javassist.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Descriptor;
import javassist.bytecode.LocalVariableAttribute;
import javassist.scopedpool.ScopedClassPoolFactoryImpl;
import javassist.scopedpool.ScopedClassPoolRepositoryImpl;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import me.moose.cpatcher.PatcherAgent;
import me.moose.cpatcher.config.PatcherConfig;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PatcherTransformer implements ClassFileTransformer {
    private final ScopedClassPoolFactoryImpl scopedClassPoolFactory = new ScopedClassPoolFactoryImpl();
    public PatcherTransformer() {
        System.out.println("[CPatcher] Loaded Transformer");
    }
    private boolean gotAuthWS = false;
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] toReturn = classfileBuffer;
        try {
            ClassPool cp = scopedClassPoolFactory.create(loader,  ClassPool.getDefault(), ScopedClassPoolRepositoryImpl.getInstance());

            CtClass cc = cp.makeClass(new ByteArrayInputStream(classfileBuffer));
            if (cc.getSuperclass() != null && cc.getSuperclass().getSimpleName() != null) {
                if (cc.getSuperclass().getSimpleName().equals("WebSocketClient")) {

                    CtConstructor m = cc.getDeclaredConstructors()[0];

                    CtMethod fakeSU = CtNewMethod.make("public void setURI\n(java.net.URI param)\n{" +
                                    " java.lang.reflect.Field uriField = getClass().getSuperclass().getField(\"uri\");" + "\n" +
                                    "uriField.set(this, param);" +
                                    "}",
                            cc);
                    cc.addMethod(fakeSU);
                    String line = null;
                    if (cc.getFields().length == 25) {
                        gotAuthWS = true;
                        if(!PatcherAgent.getConfig().AUTH_URL.equals("default")) {
                            line = "setURI(new java.net.URI(\"" + PatcherAgent.getConfig().AUTH_URL + "\"));";
                            System.out.println("[CPatcher] Updated Auth WS URL to: " + PatcherAgent.getConfig().AUTH_URL);
                        }

                    } else {
                        if(!PatcherAgent.getConfig().ASSET_URL.equals("default")) {
                            line = "setURI(new java.net.URI(\"" + PatcherAgent.getConfig().ASSET_URL + "\"));";
                            System.out.println("[CPatcher] Updated Asset WS URL to: " + PatcherAgent.getConfig().ASSET_URL);
                        }
                    }
                    if(line != null)
                        m.insertAfter(line);
                    toReturn = cc.toBytecode();

                    cc.detach();
                }
            }
            if(className.contains("lunar/") && Arrays.stream(cc.getFields()).filter(new Predicate<CtField>() {
                @Override
                public boolean test(CtField ctField) {
                    try {
                        if(ctField.getType() != null)
                            return ctField.getType().getSimpleName().equals("Gson");
                        else
                            return false;
                    } catch (Exception e) {
                        return false;
                    }
                }
            }).toArray().length == 2) {
                List<CtMethod> returnsMethods = Arrays.stream(cc.getDeclaredMethods()).filter(ctMethod -> {
                    try {
                        if (ctMethod.getReturnType() != null && ctMethod.getReturnType().getSimpleName().equals("String")) {
                            return ((ctMethod.getMethodInfo().getAccessFlags() & AccessFlag.STATIC) != 0)
                                    && !ctMethod.getName().equals("toString");
                        } else {
                            return false;
                        }
                    } catch (NotFoundException e) {
                        return false;
                    }

                }).collect(Collectors.toList());
                CtMethod getTitleMethod = returnsMethods.get(0);
                cc.defrost();
                getTitleMethod.setBody("return \"" + PatcherAgent.getConfig().TITLE.replace("%version%",
                        PatcherAgent.getCurrentVersion().getName()) + "\";");
                toReturn = cc.toBytecode();

                cc.detach();
            }
            if(className.equals("lunar/dF/lIIlIIIlIlIlIlIIIlIIlIlIl")) {
                CtMethod runMethod = cc.getMethod("run", "()V");

                runMethod.insertAt(34, "java.net.URI uRL = new java.net.URI(\"http://localhost:83/metadata.txt\");");
                toReturn = cc.toBytecode();

                cc.detach();
            }

        } catch (Exception exception) {
            if(!(exception instanceof javassist.NotFoundException)) {
                System.out.println("[CPatcher] Error");
                exception.printStackTrace();
            }
        }
      

        return toReturn;
    }
    private void loadReflection(Runnable runnable) {
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runnable.run();
        }).start();
    }
    @AllArgsConstructor
    @Getter
    enum ClassType {
        ENUM("Enum"),
        INTERFACE("Interface"),
        ANNOTATION("Annotaion"),
        CLASS("Class");
        private String name;

    }
}

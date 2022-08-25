package com.beowulfe.hap.sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import io.github.hapjava.server.impl.HomekitRoot;
import io.github.hapjava.server.impl.HomekitServer;
import io.github.hapjava.server.impl.crypto.HAPSetupCodeUtils;
import io.github.hapjava.server.HomekitAccessoryCategories;

public class Main {

    private static final int PORT = 9123;

    public static void main(String[] args) {
        try {
            File authFile = new File("auth-state.bin");
            MockAuthInfo mockAuth;
            if (authFile.exists()) {
                FileInputStream fileInputStream = new FileInputStream(authFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                try {
                    System.out.println("Using persisted auth");
                    AuthState authState = (AuthState) objectInputStream.readObject();
                    mockAuth = new MockAuthInfo(authState);
                } finally {
                    objectInputStream.close();
                }
            } else {
                mockAuth = new MockAuthInfo();
            }

            HomekitServer homekit = new HomekitServer(PORT);
            HomekitRoot bridge = homekit.createBridge(mockAuth, "Test Bridge", HomekitAccessoryCategories.BRIDGES, "TestBridge, Inc.", "G6", "111abe234", "1.1", "1.2");

            String setupURI = HAPSetupCodeUtils.getSetupURI(mockAuth.getPin().replace("-",""), mockAuth.getSetupId(), 2);
            QRtoConsole.printQR(setupURI);


            mockAuth.onChange(state -> {
                try {
                    System.out.println("State has changed! Writing");
                    FileOutputStream fileOutputStream = new FileOutputStream(authFile);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(state);
                    objectOutputStream.flush();
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            bridge.addAccessory(new MockSwitch());
            bridge.addAccessory(new MockSwitch());
            bridge.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Stopping homekit server.");
                homekit.stop();
            }));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

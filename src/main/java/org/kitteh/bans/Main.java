package org.kitteh.bans;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Random;
import sun.misc.BASE64Encoder;

public class Main {

    private static ServerSocket server;
    private static final Random KEY = new Random();
    private static final String SALT = "HEHE IT WOULD BE FUNNY IF OUR REAL SECRET KEY WENT HERE";

    public static void main(String[] args) throws Exception {
        server = new ServerSocket(25565);
        System.out.println("Server started on *:" + server.getLocalPort());
        while (true) {
            try {
                Socket client = server.accept();
                System.out.println(client.getInetAddress() + " has connected to the server");
                DataInputStream in = new DataInputStream(client.getInputStream());
                DataOutputStream out = new DataOutputStream(client.getOutputStream());
                String message;
                String username = null;
                if (in.read() != 0x2) {
                    message = "BanConcept verification server";
                } else {
                    username = readString(in).split(";")[0];
                    String hash = Long.toString(KEY.nextLong(), 16);
                    //
                    out.writeByte(0x2);
                    writeString(out, hash);
                    //
                    in.read();
                    in.readInt();
                    readString(in);
                    readString(in);
                    in.readInt();
                    in.readInt();
                    in.readByte();
                    in.readByte();
                    in.readByte();
                    //
                    BufferedReader mcAuth = new BufferedReader(new InputStreamReader(new URL("http://session.minecraft.net/game/checkserver.jsp?user=" + URLEncoder.encode(username, "UTF-8") + "&serverId=" + URLEncoder.encode(hash, "UTF-8")).openStream()));
                    String reply = mcAuth.readLine();
                    mcAuth.close();
                    //
                    if (reply.equals("YES")) {
                        message = "Your unique code is: " + new BASE64Encoder().encode(MessageDigest.getInstance("SHA-512").digest((username + SALT).getBytes("UTF-8"))).substring(0, 8);
                    } else {
                        message = "Account not premium! mbaxter will eat your face!";
                    }
                }
                out.writeByte(0xFF);
                writeString(out, message);
                System.out.println(client.getInetAddress() + ((username != null) ? " [" + username + "]" : "") + " kicked with: " + message);
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = in.readShort();
        char[] chars = new char[len];
        for (int i = 0; i < len; i++) {
            chars[i] = in.readChar();
        }
        return new String(chars);
    }

    private static void writeString(DataOutputStream out, String str) throws IOException {
        out.writeShort(str.length());
        out.writeChars(str);
    }
}

package org.kitteh.bans;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static ServerSocket server;
    private static final ExecutorService pool = Executors.newFixedThreadPool(15);
    private static final Random KEY = new Random();
    private static final String SALT = "HEHE IT WOULD BE FUNNY IF OUR REAL SECRET KEY WENT HERE";

    public static void main(String[] args) throws Exception {
        server = new ServerSocket(25565);
        System.out.println("Server started on *:" + server.getLocalPort());
        while (true) {
            try {
                Socket connection = server.accept();
                connection.setSoTimeout(2000); // allow only a 2 second delay *PER* read call
                System.out.println(connection.getInetAddress() + " has connected to the server");
                pool.submit(new Connection(connection));
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

    private static class Connection implements Runnable {

        private final Socket client;
        private static final char[] BASE58_SET = "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
        private static final BigInteger BASE58_SET_LEN = BigInteger.valueOf((long)BASE58_SET.length);

        protected Connection(Socket client) {
            this.client = client;
        }

        private String base58Encode(BigInteger toGo) {
            StringBuilder sb = new StringBuilder();

            while (toGo.compareTo(BigInteger.ZERO) == 1) {
                sb.append(BASE58_SET[toGo.mod(BASE58_SET_LEN).intValue()]);
                toGo = toGo.divide(BASE58_SET_LEN);
            }
            return sb.toString();
        }

        public String createSessionKey(String username) throws NoSuchAlgorithmException, UnsupportedEncodingException {
            long epoch = System.currentTimeMillis() / 1000;
            String digest = base58Encode(new BigInteger(1, MessageDigest.getInstance("SHA-512").digest((username + ";" + SALT + ";" + String.valueOf(epoch)).getBytes("UTF-8"))));
            String epochTime = base58Encode(BigInteger.valueOf(epoch));

            System.out.println(epoch);
            System.out.println(epochTime);

            return digest.substring(1, 6) + epochTime;
        }

        @Override
        public void run() {
            try {
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
                        message = "Your unique code is: " + this.createSessionKey(username.toLowerCase()) + " - this key is valid for the next 10 minutes.";
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
}

package com.demo;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * ╔══════════════════════════════════════════════════════╗
 * ║         MULTITHREADED JAVA CHAT SERVER               ║
 * ║  Handles multiple simultaneous clients using threads ║
 * ╚══════════════════════════════════════════════════════╝
 *
 * Architecture:
 *   - One ServerSocket listens for incoming connections
 *   - Each new client gets its own ClientHandler thread
 *   - A shared CopyOnWriteArrayList stores all active handlers
 *   - broadcast() sends messages to every connected client
 */
public class ChatServer {

    // ── Configuration ────────────────────────────────────────────────────────
    private static final int    PORT        = 12345;
    private static final int    MAX_CLIENTS = 50;

    // ── Shared State (thread-safe collections) ───────────────────────────────
    // CopyOnWriteArrayList: safe for concurrent reads and rare writes (join/leave)
    private static final List<ClientHandler> clients =
            new CopyOnWriteArrayList<>();

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("HH:mm:ss");

    // ── Entry Point ──────────────────────────────────────────────────────────
    public static void main(String[] args) throws IOException {
        printBanner();

        // ThreadPool: limits resources while supporting concurrent clients
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);
            log("Server started on port " + PORT);
            log("Waiting for clients to connect...\n");

            // ── Accept Loop ──────────────────────────────────────────────────
            while (true) {
                Socket clientSocket = serverSocket.accept(); // blocks until a client connects
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                threadPool.execute(handler); // assign to a thread from the pool
            }
        } catch (IOException e) {
            log("Server error: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    // ── Broadcast: send a message to ALL connected clients ───────────────────
    static void broadcast(String message, ClientHandler sender) {
        String timestamped = "[" + TIME_FMT.format(new Date()) + "] " + message;

        // Print to server console too
        System.out.println("  BROADCAST → " + timestamped);

        for (ClientHandler client : clients) {
            // Option: send to everyone including sender (standard chat behavior)
            client.sendMessage(timestamped);
        }
    }

    // ── Direct Message: send to one specific client ───────────────────────────
    static boolean directMessage(String targetName, String message, ClientHandler from) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equalsIgnoreCase(targetName)) {
                String dm = "[" + TIME_FMT.format(new Date()) + "] [DM from "
                        + from.getUsername() + "] " + message;
                client.sendMessage(dm);
                from.sendMessage("[" + TIME_FMT.format(new Date()) + "] [DM to "
                        + targetName + "] " + message);
                return true;
            }
        }
        return false;
    }

    // ── List all online users ─────────────────────────────────────────────────
    static String getOnlineUsers() {
        if (clients.isEmpty()) return "No users online.";
        StringBuilder sb = new StringBuilder("── Online Users (" + clients.size() + ") ──\n");
        for (ClientHandler c : clients) {
            sb.append("  • ").append(c.getUsername()).append("\n");
        }
        return sb.toString().trim();
    }

    // ── Remove a client from the shared list ──────────────────────────────────
    static void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    static void log(String msg) {
        System.out.println("[SERVER " + TIME_FMT.format(new Date()) + "] " + msg);
    }

    private static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║       Java Multithreaded Chat Server             ║");
        System.out.println("║       Port: 12345  |  Max Clients: 50            ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();
    }
}


/**
 * ┌──────────────────────────────────────────────┐
 * │  ClientHandler — one per connected client    │
 * │  Runs in its own thread (implements Runnable)│
 * └──────────────────────────────────────────────┘
 *
 * Responsibilities:
 *  1. Greet client and ask for a username
 *  2. Listen for incoming messages in a loop
 *  3. Route messages: broadcast / DM / command
 *  4. Handle disconnect gracefully
 */
class ClientHandler implements Runnable {

    private final Socket         socket;
    private       PrintWriter    out;       // writes TO client
    private       BufferedReader in;        // reads FROM client
    private       String         username;

    ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // ── Thread entry point ────────────────────────────────────────────────────
    @Override
    public void run() {
        try {
            // Set up I/O streams
            out = new PrintWriter(socket.getOutputStream(), true); // auto-flush
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // ── Step 1: Username handshake ────────────────────────────────────
            sendMessage("╔══════════════════════════════════════════╗");
            sendMessage("║   Welcome to Java Multi-User Chat!       ║");
            sendMessage("║   Commands:                              ║");
            sendMessage("║   /users      - List online users        ║");
            sendMessage("║   /dm <user> <msg> - Direct message      ║");
            sendMessage("║   /quit       - Disconnect               ║");
            sendMessage("╚══════════════════════════════════════════╝");
            sendMessage("Enter your username:");

            username = in.readLine();
            if (username == null || username.isBlank()) {
                username = "User" + (int)(Math.random() * 9000 + 1000);
            }
            username = username.trim().replaceAll("\\s+", "_"); // no spaces in names

            ChatServer.log("Client connected: " + username
                    + " | IP: " + socket.getInetAddress().getHostAddress());

            // Announce arrival to everyone
            ChatServer.broadcast("🟢 " + username + " has joined the chat!", this);
            sendMessage("✅ You joined as: " + username + "\n");

            // ── Step 2: Message loop ──────────────────────────────────────────
            String message;
            while ((message = in.readLine()) != null) {
                message = message.trim();
                if (message.isEmpty()) continue;

                ChatServer.log("[" + username + "]: " + message);

                // ── Command routing ───────────────────────────────────────────
                if (message.equalsIgnoreCase("/quit")) {
                    break; // exit loop → triggers disconnect logic

                } else if (message.equalsIgnoreCase("/users")) {
                    sendMessage(ChatServer.getOnlineUsers());

                } else if (message.startsWith("/dm ")) {
                    handleDirectMessage(message);

                } else if (message.startsWith("/")) {
                    sendMessage("⚠ Unknown command. Try /users, /dm <user> <msg>, or /quit");

                } else {
                    // Regular message → broadcast to all
                    ChatServer.broadcast(username + ": " + message, this);
                }
            }

        } catch (IOException e) {
            ChatServer.log("Connection error for " + username + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // ── Parse and dispatch a direct message ───────────────────────────────────
    private void handleDirectMessage(String raw) {
        // Format: /dm <targetUsername> <message text>
        String[] parts = raw.split("\\s+", 3); // ["/dm", "username", "message"]
        if (parts.length < 3) {
            sendMessage("⚠ Usage: /dm <username> <message>");
            return;
        }
        String target  = parts[1];
        String content = parts[2];

        if (target.equalsIgnoreCase(username)) {
            sendMessage("⚠ You cannot DM yourself.");
            return;
        }

        boolean sent = ChatServer.directMessage(target, content, this);
        if (!sent) {
            sendMessage("⚠ User '" + target + "' not found. Use /users to see who's online.");
        }
    }

    // ── Graceful disconnect ────────────────────────────────────────────────────
    private void disconnect() {
        ChatServer.removeClient(this);
        if (username != null) {
            ChatServer.broadcast("🔴 " + username + " has left the chat.", this);
            ChatServer.log(username + " disconnected. Active clients: "
                    + (ChatServer.getOnlineUsers().contains("•") ? "see /users" : "0"));
        }
        try { socket.close(); } catch (IOException ignored) {}
    }

    // ── Send a message to THIS client ─────────────────────────────────────────
    void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    String getUsername() { return username; }
}

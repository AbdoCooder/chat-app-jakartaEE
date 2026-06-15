package ma.uae.mql.final_project;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/chat")
public class Endpoint {

    private static final Logger logger = Logger.getLogger("Endpoint");

    /**
     * Maps each open session to the username that client registered.
     * ConcurrentHashMap does NOT allow null keys or values, so we use the
     * sentinel string PENDING to mark sessions that have not registered yet.
     */
    private static final Map<Session, String> users = new ConcurrentHashMap<>(); // session -> username
    private static final String PENDING = "";

    @OnOpen
    public void openConnection(Session session) {
        users.put(session, PENDING);
        logger.log(Level.INFO, "New connection: {0}", session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session sender) {

        // Registration handshake (The very first message from a client must be "register:<username>").
        if (PENDING.equals(users.get(sender))) {
            if (message.startsWith("register:")) {
                String username = message.substring("register:".length()).trim();

                // Sanitize: reject blank or duplicate names
                if (username.isEmpty()) {
                    send(sender, "error:Username cannot be empty.");
                    return;
                }
                if (users.containsValue(username)) {
                    send(sender, "error:Username already taken.");
                    return;
                }
                // Register the new user
                users.put(sender, username);
                send(sender, "system:Welcome, " + username + "!"); // to the user
                broadcast("system:" + username + " joined the chat.", sender); // to everyone else
                logger.log(Level.INFO, "Registered user: {0}", username);
            } else {
                // Reject chat messages before registration is complete
                send(sender, "error:You must register a username first.");
            }
            return;
        }

        // Normal chat message
        String username = users.get(sender);
        String payload  = "chat:" + username + ":" + message;
        broadcast(payload, sender);
    }

    @OnClose
    public void closedConnection(Session session) {
        String username = users.remove(session);
        if (username != null) {
            broadcast("system:" + username + " left the chat.", null);
        }
        logger.log(Level.INFO, "Connection closed: {0}", session.getId());
    }

    @OnError
    public void error(Session session, Throwable t) {
        String username = users.remove(session);
        if (username != null) {
            broadcast("system:" + username + " disconnected.", null);
        }
        logger.log(Level.WARNING, "Error on {0}: {1}", new Object[]{session.getId(), t.toString()});
    }

    /** Send a message to a single session. */
    private void send(Session session, String message) {
        if (session.isOpen()) {
            session.getAsyncRemote().sendText(message);
        }
    }

    /**
     * Broadcast to all registered users except the excluded session.
     * Pass null as excluded to broadcast to everyone (including the sender).
     */
    private void broadcast(String message, Session sender) {
        for (Map.Entry<Session, String> entry : users.entrySet()) {
            Session s = entry.getKey();
            if (!PENDING.equals(entry.getValue())  // skip unregistered sessions
                    && !s.equals(sender)           // skip the sender session
                    && s.isOpen()) {
                s.getAsyncRemote().sendText(message);
            }
        }
    }
}

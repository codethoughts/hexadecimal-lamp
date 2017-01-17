package sample;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Objects;

/**
 * Created by nrdwnd on 7/15/16.
 */
public class Server {
    public WebServerSocket socket;
    public ServerState state;
    public Thread thread;
    public HashSet<ServerStatusObserver> observers = new HashSet<>();

    private Server() {
        state = new Halt(this);
    }
    private static Server instance;
    public static Server getInstance() {
        if (instance != null) return instance;
        else {
            instance = new Server();
            return instance;
        }
    }
    public void run() {
        state.running();
    }
    public void stop() {
        state.halt();
    }
    public void changeStateToRunning() {
        state = new Running(this);
    }
    public void changeStateToHalt() {
        state = new Halt(this);
    }
}

abstract class ServerState {
    protected Server server;
    ServerState(Server server) {this.server=server;}
    void running() {
        System.out.println("Already running");
    }
    void halt() {
        System.out.println("Already stopped");
    }
}

class Running extends ServerState {
    Running(Server server) {super((server));}
    @Override
    void halt() {
        try {
            server.thread.interrupt();
            server.socket.close();
            server.changeStateToHalt();
            server.observers.forEach(observer -> observer.notifyOnShutdown());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Halt extends ServerState {
    Halt(Server server){super(server);}
    @Override
    void running() {
        try {
            server.socket = new WebServerSocket(new ServerSocket(3000));
            onIncomingConnection();
            server.changeStateToRunning();
            server.observers.forEach(observer -> observer.notifyOnRun());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void onIncomingConnection() {
        Service listenForConnections = new Service() {
            @Override
            protected Task createTask() {
                return new Task() {
                    @Override
                    protected Void call() throws Exception {
                        do {
                            try {
                                WebSocket socket = server.socket.accept();
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        server.observers.forEach(observer -> observer.notifyOnNewClient(socket));
                                    }
                                });
                                Task handleMessages = new Task<Void>() {
                                    @Override
                                    protected Void call() throws Exception {
                                        socket.handle();
                                        return null;
                                    }
                                };
                                new Thread(handleMessages).start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } while (server.state instanceof Running);
                        return null;
                    }
                };
            }
        };
        server.thread.start();
    }
}

class WebSocket {
    HashSet<ClientSocketObserver> socketObservers = new HashSet<>();
    Socket socket;

    public WebSocket(Socket socket) {
        this.socket = socket;
    }

    public void subscribeOnMessage(ClientSocketObserver observer) {
        socketObservers.add(observer);
    }
    public void unsubscribeFromMessage(ClientSocketObserver observer) {
        socketObservers.remove(observer);
    }
    public void handle() {
        while (true) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String command = reader.readLine();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        socketObservers.forEach(socket -> socket.notifyOnMessage(command));
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void close() throws IOException {
        socketObservers.forEach(socket -> socket.notifyOnDisconnect());
        socket.close();
    }
}
class WebServerSocket {
    private ServerSocket serverSocket;
    HashSet<ServerStatusObserver> observers = new HashSet<>();
    HashSet<WebSocket> clients = new HashSet<>();

    public WebServerSocket(ServerSocket s) {
        serverSocket = s;
    }

    public void subscribe(ServerStatusObserver observer) {
        observers.add(observer);
    }
    public void unsubscribe(ServerStatusObserver observer) {
        observers.remove(observer);
    }


    public WebSocket accept() throws IOException {
        WebSocket socket = new WebSocket(serverSocket.accept());
        clients.add(socket);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                observers.forEach(observer -> observer.notifyOnNewClient(socket));
            }
        });
        return socket;
    }

    public void close() throws IOException {
        serverSocket.close();
    }
}
interface ServerStatusObserver {
    void notifyOnShutdown();
    void notifyOnRun();
    void notifyOnNewClient(WebSocket socket);
}
interface ClientSocketObserver {
    void notifyOnMessage(String message);
    void notifyOnDisconnect();
}
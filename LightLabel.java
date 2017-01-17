package sample;

import javafx.scene.control.Label;

public class LightLabel extends Label implements OnLightObserver, OffLightObserver, ServerStatusObserver, ClientSocketObserver {
    @Override
    public void notifyOnLightON() {
        this.setText("Light is ON");
    }
    @Override
    public void notifyOnLightOFF() {
        this.setText("Light is OFF");
    }

    @Override
    public void notifyOnShutdown() {
        setText("Server is shutdown");
    }

    @Override
    public void notifyOnRun() {
        setText("Server is up");
    }

    @Override
    public void notifyOnNewClient(WebSocket wrapper) {
        setText(wrapper.socket.getRemoteSocketAddress().toString()+"::"+wrapper.socket.getPort());
    }

    @Override
    public void notifyOnMessage(String message) {
        setText(message);
    }

    @Override
    public void notifyOnDisconnect() {
        setText("Client was disconnected");
    }
}

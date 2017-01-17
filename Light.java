package sample;

import org.w3c.dom.css.RGBColor;

import java.util.LinkedList;
import java.util.HashSet;

interface OffLightObserver {
    void notifyOnLightOFF();
}

interface OnLightObserver {
    void notifyOnLightON();
}

interface Command {
    void execute();
    void unexecute();
}

class CommandUndoRedoStack {
    private LinkedList<Command> undoList = new LinkedList<>();
    private LinkedList<Command> redoList = new LinkedList<>();

    public void addCommand(Command command) {
        undoList.addFirst(command);
    }

    public void undo() {
        if (undoList.isEmpty()) return;
        Command command = undoList.removeFirst();
        command.unexecute();
        redoList.addFirst(command);
    }

    public void redo() {
        if (redoList.isEmpty()) return;
        Command command = redoList.removeFirst();
        command.execute();
        addCommand(command);
    }
}

class LightDirector {
    private LightBuilder lightBuilder;

    public void setLightBuilder(LightBuilder lightBuilder) {
        this.lightBuilder = lightBuilder;
    }

    public Light getLight() {
        return lightBuilder.getLight();
    }

    public void buildLight() {
        lightBuilder.createBrandNewLight();
        lightBuilder.setColor();
        lightBuilder.setWatt();
        lightBuilder.setManufacturer();
    }
}

abstract class LightBuilder {
    protected Light light;
    public void createBrandNewLight() { light = new Light(); }
    public Light getLight() { return light; }
    abstract void setColor();
    abstract void setWatt();
    abstract void setManufacturer();
}

class PhillipsNeonLightBuilder extends LightBuilder {

    @Override
    void setColor() {
        light.color = "neon";
    }

    @Override
    void setWatt() {
        light.watt = 60.0;
    }

    @Override
    void setManufacturer() {
        light.manufacturer = "Phillips";
    }
}

public class Light {

    String color;
    double watt;
    String manufacturer;

    public LightState state = new OffState(this); // by default light is off
    public CommandUndoRedoStack stack = new CommandUndoRedoStack();
    private HashSet<OnLightObserver> onLightObservers = new HashSet<>();
    private HashSet<OffLightObserver> offLightObservers = new HashSet<>();

    public Light() {}
    public void on() {
        state.on();
    }
    public void off() {
        state.off();
    }
    public void undo() {
        stack.undo();
    }
    public void redo() {
        stack.redo();
    }
    public void setOffState() {
        state = new OffState(this);
    }
    public void setOnState() {
        state = new OnState(this);
    }
    public void subscribeLightON(OnLightObserver o) {
        onLightObservers.add(o);
    }
    public void subscribeLightOFF(OffLightObserver o) {
        offLightObservers.add(o);
    }
    public void unsubscribeLightON(OnLightObserver o) {
        onLightObservers.remove(o);
    }
    public void unsubscribeLightOFF(OffLightObserver o) {
        offLightObservers.remove(o);
    }
    public void notifyOnLightON() { onLightObservers.forEach(o -> o.notifyOnLightON()); }
    public void notifyOnLightOFF() {
        offLightObservers.forEach(o -> o.notifyOnLightOFF());
    }

    @Override
    public String toString() {
        return "{ " + color + "," + watt + "," + manufacturer + " }";
    }
}

class OnLight implements Command {
    Light light;
    public OnLight(Light light) {
        this.light = light;
    }
    @Override
    public void execute() {
        light.on();
    }
    @Override
    public void unexecute() {
        light.off();
    }
}

class OffLight implements Command {
    Light light;
    public OffLight(Light light) {
        this.light = light;
    }
    @Override
    public void execute() {
        light.off();
    }
    @Override
    public void unexecute() {
        light.on();
    }
}

abstract class LightState {
    protected Light light;
    public LightState(Light light) {
        this.light = light;
    }
    void on() {
        System.out.println("Light is already ON");
    }
    void off() {
        System.out.println("Light is already OFF");
    }
}

class OnState extends LightState {
    public OnState(Light light) { super(light); }
    @Override
    public void off() {
        Command offCommand = new OffLight(light);
        light.stack.addCommand(offCommand);
        light.setOffState();

        System.out.println("Sends a signal to hardware device to OFF light");
        light.notifyOnLightOFF();
    }
}

class OffState extends LightState {
    public OffState(Light light) { super(light); }
    @Override
    public void on() {
        Command onCommand = new OnLight(light);
        light.stack.addCommand(onCommand);
        light.setOnState();

        System.out.println("Sends a signal to hardware device to ON light");
        light.notifyOnLightON();
    }
}

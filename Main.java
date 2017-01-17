package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class Main extends Application {

    @FXML
    public Button onButton;

    @FXML
    public Button offButton;

    @FXML
    public LightLabel stateLabel;

    Light light;

    boolean reg = true;

    public void register() {
        if (!reg) {
            light.subscribeLightON(stateLabel);
            light.subscribeLightOFF(stateLabel);
            reg = true;
        } else {
            light.unsubscribeLightON(stateLabel);
            light.unsubscribeLightOFF(stateLabel);
            reg = false;
        }
    }

    public void undo() {
        light.undo();
    }

    public void redo() {
        light.redo();
    }

    public void on() {
        light.on();
    }

    public void off() {
        light.off();
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
        Platform.setImplicitExit(false);
    }

    @FXML
    public void initialize() {
        LightDirector director = new LightDirector();
        director.setLightBuilder(new PhillipsNeonLightBuilder());
        director.buildLight();
        light = director.getLight();
        light.subscribeLightON(stateLabel);
        light.subscribeLightOFF(stateLabel);
        System.out.println(light);

        Server.getInstance().observers.add(stateLabel);
        Server.getInstance().run();

    }

    public static void main(String[] args) {
        launch(args);
    }
}

package com.adobe.summit2016;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends Application {

    private static final int WINDOW_WIDTH = 460;
    private static final int WINDOW_HEIGHT = 820;
    private static final int IMAGE_WIDTH = 460;
    private static final int IMAGE_HEIGHT = 700;
    private static final String IMAGE_PATH = "image/final_large.jpg";
    private static final String MBOX = System.getProperty("mbox");
    private static final long DELAY = 5;

    private SessionMboxCallService sessionMboxCallService;
    private ScheduledExecutorService scheduledExecutorService;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Target Summit 2016");
        primaryStage.setResizable(false);
        primaryStage.setWidth(WINDOW_WIDTH);
        primaryStage.setHeight(WINDOW_HEIGHT);
        Scene scene = new Scene(new Group());
        AnchorPane anchorPane = getAnchorPane();

        Label label = new Label("Enter your TNT authenticated ID");
        label.setLayoutY(WINDOW_HEIGHT - 110);

        final TextField tntAuthenticatedIdField = new TextField();
        tntAuthenticatedIdField.setPrefColumnCount(10);
        tntAuthenticatedIdField.setLayoutY(WINDOW_HEIGHT - 90);

        Button submit = new Button("Submit");
        submit.setLayoutY(WINDOW_HEIGHT - 60);
        submit.setOnAction(event -> {
            String thirdPartyId = tntAuthenticatedIdField.getText();
            if (StringUtils.isBlank(thirdPartyId)) {
                return;
            }

            if (scheduledExecutorService != null) {
                scheduledExecutorService.shutdownNow();
            }
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            sessionMboxCallService = new SessionMboxCallService(thirdPartyId);
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    String content = sessionMboxCallService.getContent(MBOX);
                    replaceLightbulbColor(content, anchorPane);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, DELAY, DELAY, TimeUnit.SECONDS);
        });

        anchorPane.getChildren().add(tntAuthenticatedIdField);
        anchorPane.getChildren().add(label);
        anchorPane.getChildren().add(submit);

        scene.setRoot(anchorPane);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private AnchorPane getAnchorPane() {
        AnchorPane anchorPane = new AnchorPane();

        ImageView selectedImage = new ImageView();
        Image image = new Image(Main.class.getClassLoader().getResourceAsStream(IMAGE_PATH),
                IMAGE_WIDTH, IMAGE_HEIGHT, true, true);
        selectedImage.setImage(image);
        anchorPane.getChildren().addAll(selectedImage);

        anchorPane.getChildren().add(getLightbulbLight("yellow"));

        return anchorPane;
    }

    private void replaceLightbulbColor(String color, AnchorPane anchorPane) {
        if (StringUtils.isNotBlank(color)) {
            Platform.runLater(() -> anchorPane.getChildren().replaceAll(node -> {
                if (node instanceof Circle) {
                    return getLightbulbLight(color);
                }
                return node;
            }));
        }
    }

    private Circle getLightbulbLight(String color) {
        RadialGradient shadePaint = new RadialGradient(0, 0, .5, .5, .35, true, CycleMethod.NO_CYCLE,
                new Stop(1, Color.TRANSPARENT),
                new Stop(0, Color.web(color)));
        return new Circle(225, 215, 250, shadePaint);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

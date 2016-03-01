package com.adobe.summit2016;

import com.google.common.collect.ImmutableMap;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;

import java.io.StringReader;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends Application {

    private static final int WINDOW_WIDTH = 830;
    private static final int WINDOW_HEIGHT = 710;
    private static final int IMAGE_WIDTH = 460;
    private static final int IMAGE_HEIGHT = 700;
    private static final String IMAGE_PATH = "image/final_large.jpg";
    private static final String MBOX = StringUtils.defaultString(System.getProperty("mbox"),
            "wellnessHomeAutomationServer");
    private static final long DELAY = 5;

    private SessionMboxCallService sessionMboxCallService;
    private ScheduledExecutorService scheduledExecutorService;
    private LifxRequestHelper lifxRequestHelper;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Wellness Home Automation Server");
        primaryStage.setResizable(false);
        primaryStage.setWidth(WINDOW_WIDTH);
        primaryStage.setHeight(WINDOW_HEIGHT);
        Scene scene = new Scene(new Group());
        AnchorPane anchorPane = getAnchorPane();

        final Text welcomeText = new Text();
        welcomeText.setFont(Font.font(null, FontWeight.BOLD, 14));
        welcomeText.setLayoutY(20);
        welcomeText.setLayoutX(20);

        Label userNameLabel = new Label("Enter User Name");
        userNameLabel.setLayoutX(470);
        userNameLabel.setLayoutY(10);

        final TextField userNameField = new TextField();
        userNameField.setPrefColumnCount(10);
        userNameField.setLayoutX(600);
        userNameField.setLayoutY(10);

        Label userIdLabel = new Label("Enter Profile UserId");
        userIdLabel.setLayoutX(470);
        userIdLabel.setLayoutY(40);

        final TextField profileId = new TextField();
        profileId.setPrefColumnCount(10);
        profileId.setLayoutX(600);
        profileId.setLayoutY(40);

        final CheckBox checkBox = new CheckBox("Connect to Physical Light Bulb");
        checkBox.setSelected(false);
        checkBox.setLayoutY(70);
        checkBox.setLayoutX(600);

        final TextArea debugArea = new TextArea();
        debugArea.setPrefRowCount(33);
        debugArea.setPrefColumnCount(25);
        debugArea.setWrapText(true);
        debugArea.setLayoutX(470);
        debugArea.setLayoutY(130);
        debugArea.setEditable(false);
        debugArea.setScrollLeft(Double.MAX_VALUE);

        Button submit = new Button("Submit");
        submit.setLayoutY(100);
        submit.setLayoutX(600);
        submit.setOnAction(event -> {
            String thirdPartyId = profileId.getText();
            String userName = userNameField.getText();
            if (StringUtils.isBlank(thirdPartyId) || StringUtils.isBlank(userName)) {
                return;
            }

            if (scheduledExecutorService != null) {
                scheduledExecutorService.shutdownNow();
            }
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            sessionMboxCallService = new SessionMboxCallService(thirdPartyId, debugArea);
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    String content = sessionMboxCallService.getContent(MBOX, ImmutableMap.of("name", userName));
                    Properties properties = new Properties();
                    properties.load(new StringReader(content));

                    String profileName = properties.getProperty("greeting");
                    if (StringUtils.isNotBlank(profileName)) {
                        welcomeText.setText(profileName);
                    }

                    replaceLightbulbColor(properties.getProperty("virtual_light"), anchorPane);

                    if (checkBox.isSelected()) {
                        if (lifxRequestHelper == null) {
                            lifxRequestHelper = new LifxRequestHelper(debugArea);
                        }
                        lifxRequestHelper.setLightBulbStates(properties.getProperty("physical_light"));
                    }
                } catch (Exception e) {
                    debugArea.appendText(e.getMessage());
                }
            }, DELAY, DELAY, TimeUnit.SECONDS);
        });

        Button stopButton = new Button("Stop");
        stopButton.setLayoutY(100);
        stopButton.setLayoutX(670);
        stopButton.setOnAction(event -> {
            if (scheduledExecutorService != null) {
                scheduledExecutorService.shutdown();
            }
            scheduledExecutorService = null;
        });

        anchorPane.getChildren().add(userNameLabel);
        anchorPane.getChildren().add(userNameField);
        anchorPane.getChildren().add(debugArea);
        anchorPane.getChildren().add(welcomeText);
        anchorPane.getChildren().add(userIdLabel);
        anchorPane.getChildren().add(profileId);
        anchorPane.getChildren().add(checkBox);
        anchorPane.getChildren().add(submit);
        anchorPane.getChildren().add(stopButton);

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
}

package com;

import javafx.application.Application;
import javafx.stage.Stage;
import com.utils.ImageSynchronizer;

public class MainApplication extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize the image synchronizer
        ImageSynchronizer.initialize();
        
        // ... existing code ...
    }
    
    @Override
    public void stop() throws Exception {
        // Shutdown the image synchronizer
        ImageSynchronizer.shutdown();
        
        // ... existing code ...
        super.stop();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 
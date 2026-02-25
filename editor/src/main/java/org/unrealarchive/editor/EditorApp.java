package org.unrealarchive.editor;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EditorApp extends Application {
	@Override
	public void start(Stage stage) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(EditorApp.class.getResource("editor.fxml"));
		Scene scene = new Scene(fxmlLoader.load(), 1024, 768);
		scene.getStylesheets().add(EditorApp.class.getResource("style.css").toExternalForm());

		EditorController controller = fxmlLoader.getController();
		controller.setArgs(getParameters().getRaw());

		stage.setTitle("Unreal Archive Editor");
		stage.setScene(scene);
		stage.show();
	}
}

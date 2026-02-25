package org.unrealarchive.editor;

import java.util.Collection;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.unrealarchive.content.Author;
import org.unrealarchive.content.RepositoryManager;

public class AuthorsController {

	private RepositoryManager repositoryManager;

	@FXML private ListView<String> authorsList;
	@FXML private VBox editorContainer;
	@FXML private Label statusLabel;
	@FXML private HBox buttonContainer;

	private GenericEditor<Author> currentEditor;

	public void setRepositoryManager(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
	}

	@FXML
	public void initialize() {
		authorsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null) {
				loadAuthor(newVal);
			}
		});
	}

	public void refresh() {
		if (repositoryManager == null) return;
		Collection<Author> authors = repositoryManager.authors().allDefined();
		authorsList.setItems(FXCollections.observableArrayList(authors.stream().sorted().map(a -> a.name).toList()));
	}

	private void loadAuthor(String name) {
		Author author = repositoryManager.authors().byName(name);
		if (author != null) {
			showEditor(author);
		} else {
			showError("Author not found: " + name);
		}
	}

	private void showEditor(Author author) {
		editorContainer.getChildren().clear();
		statusLabel.setText("");

		currentEditor = new GenericEditor<>(author);
		ScrollPane scrollPane = new ScrollPane(currentEditor);
		scrollPane.setFitToWidth(true);
		scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
		VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
		editorContainer.getChildren().add(scrollPane);

		Button saveBtn = new Button("Save");
		saveBtn.setOnAction(e -> onSave());
		Button cancelBtn = new Button("Cancel");
		cancelBtn.setOnAction(e -> {
			editorContainer.getChildren().clear();
			buttonContainer.getChildren().clear();
			authorsList.getSelectionModel().clearSelection();
		});

		buttonContainer.getChildren().clear();
		buttonContainer.getChildren().addAll(saveBtn, cancelBtn);
	}

	private void showError(String message) {
		editorContainer.getChildren().clear();
		buttonContainer.getChildren().clear();
		Label errorLabel = new Label(message);
		errorLabel.setStyle("-fx-text-fill: red;");
		editorContainer.getChildren().add(errorLabel);
	}

	private void onSave() {
		if (currentEditor == null) return;
		try {
			Author updated = currentEditor.value();
			repositoryManager.authors().put(updated, false);
			statusLabel.setText("Saved successfully!");
			statusLabel.setStyle("-fx-text-fill: green;");
			refresh();
		} catch (Exception e) {
			statusLabel.setText("Failed to save: " + e.getMessage());
			statusLabel.setStyle("-fx-text-fill: red;");
			e.printStackTrace();
		}
	}
}

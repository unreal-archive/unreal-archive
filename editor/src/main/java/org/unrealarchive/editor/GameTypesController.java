package org.unrealarchive.editor;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.unrealarchive.content.RepositoryManager;
import org.unrealarchive.content.addons.GameType;

public class GameTypesController {

	private RepositoryManager repositoryManager;

	@FXML private ComboBox<String> gameFilter;
	@FXML private ListView<GameTypeItem> gameTypesList;
	@FXML private VBox editorContainer;
	@FXML private HBox buttonContainer;
	@FXML private Label statusLabel;

	private GenericEditor<GameType> currentEditor;

	public void setRepositoryManager(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
	}

	@FXML
	public void initialize() {
		gameFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null) {
				refreshGameTypes(newVal);
			}
		});

		gameTypesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null) {
				loadGameType(newVal.id());
			}
		});
	}

	public void refresh() {
		if (repositoryManager == null) return;
		Set<GameType> all = repositoryManager.gameTypes().all();
		Collection<String> games = all.stream()
									  .map(g -> g.game)
									  .distinct()
									  .sorted()
									  .collect(Collectors.toList());
		gameFilter.setItems(FXCollections.observableArrayList(games));
		if (!games.isEmpty()) {
			gameFilter.getSelectionModel().selectFirst();
		}
	}

	private void refreshGameTypes(String game) {
		if (repositoryManager == null) return;
		Collection<GameTypeItem> items = repositoryManager.gameTypes().all().stream()
														  .filter(g -> g.game.equals(game))
														  .sorted(Comparator.comparing(g -> g.name))
														  .map(g -> new GameTypeItem(g.name, g.id().id()))
														  .collect(Collectors.toList());
		gameTypesList.setItems(FXCollections.observableArrayList(items));
	}

	private void loadGameType(String id) {
		GameType gt = repositoryManager.gameTypes().forId(id);
		if (gt != null) {
			showEditor(gt);
		} else {
			showError("GameType not found: " + id);
		}
	}

	private void showEditor(GameType gameType) {
		editorContainer.getChildren().clear();
		statusLabel.setText("");

		currentEditor = new GenericEditor<>(gameType);
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
			gameTypesList.getSelectionModel().clearSelection();
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
			GameType updated = currentEditor.value();
			repositoryManager.gameTypes().put(updated);
			statusLabel.setText("Saved successfully!");
			statusLabel.setStyle("-fx-text-fill: green;");
		} catch (Exception e) {
			statusLabel.setText("Failed to save: " + e.getMessage());
			statusLabel.setStyle("-fx-text-fill: red;");
			e.printStackTrace();
		}
	}

	private record GameTypeItem(String name, String id) {
		@Override
		public String toString() {
			return name;
		}
	}
}

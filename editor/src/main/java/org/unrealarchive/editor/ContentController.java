package org.unrealarchive.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.unrealarchive.content.RepositoryManager;
import org.unrealarchive.content.addons.Addon;

public class ContentController {

	private RepositoryManager repositoryManager;

	@FXML private ComboBox<String> filterKey;
	@FXML private TextField filterValue;
	@FXML private ListView<Filter> activeFiltersList;
	@FXML private ListView<ContentItem> resultsList;
	@FXML private Label resultsCountLabel;

	@FXML private TextField hashSearchField;
	@FXML private VBox editorContainer;
	@FXML private Label statusLabel;
	@FXML private HBox buttonContainer;

	private GenericEditor<Addon> currentEditor;

	private final List<Filter> filters = new ArrayList<>();

	public void setRepositoryManager(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
	}

	@FXML
	public void initialize() {
		filterKey.setItems(FXCollections.observableArrayList(
			"name", "author", "game", "contentType", "releaseDate", "originalFilename", "variationOf"
		));
		filterKey.getSelectionModel().selectFirst();

		resultsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null) {
				loadAddon(newVal.hash());
			}
		});
	}

	@FXML
	public void onAddFilter() {
		String key = filterKey.getEditor().getText().trim();
		if (key.isEmpty() && filterKey.getValue() != null) key = filterKey.getValue();
		String value = filterValue.getText().trim();

		if (!key.isEmpty() && !value.isEmpty()) {
			filters.add(new Filter(key, value));
			filterValue.clear();
			refresh();
		}
	}

	@FXML
	public void onRemoveFilter() {
		Filter selected = activeFiltersList.getSelectionModel().getSelectedItem();
		if (selected != null) {
			filters.remove(selected);
			refresh();
		}
	}

	public void refresh() {
		activeFiltersList.setItems(FXCollections.observableArrayList(filters));
		applyFilters();
	}

	private void applyFilters() {
		if (repositoryManager == null) return;

		String[] keysValues = new String[filters.size() * 2];
		for (int i = 0; i < filters.size(); i++) {
			keysValues[i * 2] = filters.get(i).key();
			keysValues[i * 2 + 1] = filters.get(i).value();
		}

		Collection<Addon> filtered = repositoryManager.addons().filter(keysValues);

		if (filtered.size() > 100) {
			resultsList.setItems(FXCollections.emptyObservableList());
			resultsCountLabel.setText("Found " + filtered.size() + " results. Too many results, please refine your search.");
			resultsCountLabel.setStyle("-fx-text-fill: orange;");
		} else if (filtered.isEmpty()) {
			resultsList.setItems(FXCollections.emptyObservableList());
			resultsCountLabel.setText("No results found.");
			resultsCountLabel.setStyle("-fx-text-fill: red;");
		} else {
			List<ContentItem> items = filtered.stream()
											  .sorted(Comparator.comparing(a -> a.name != null ? a.name : ""))
											  .map(a -> new ContentItem(a.name, a.hash))
											  .collect(Collectors.toList());
			resultsList.setItems(FXCollections.observableArrayList(items));
			resultsCountLabel.setText("Found " + filtered.size() + " results.");
			resultsCountLabel.setStyle("-fx-text-fill: green;");
		}
	}

	private void loadAddon(String hash) {
		Addon addon = repositoryManager.addons().forHash(hash);
		if (addon != null) {
			showEditor(addon);
		} else {
			showError("Addon not found for hash: " + hash);
		}
	}

	@FXML
	public void onSearch() {
		if (repositoryManager == null) return;

		String hash = hashSearchField.getText().trim();
		if (hash.isEmpty()) {
			showError("Please enter a hash.");
			return;
		}

		Addon addon = repositoryManager.addons().forHash(hash);
		if (addon != null) {
			showEditor(addon);
		} else {
			showError("Addon not found for hash: " + hash);
		}
	}

	private void showEditor(Addon addon) {
		editorContainer.getChildren().clear();
		statusLabel.setText("");

		currentEditor = new GenericEditor<>(addon);
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
			Addon updated = currentEditor.value();
			repositoryManager.addons().put(updated);
			statusLabel.setText("Saved successfully!");
			statusLabel.setStyle("-fx-text-fill: green;");
		} catch (Exception e) {
			statusLabel.setText("Failed to save: " + e.getMessage());
			statusLabel.setStyle("-fx-text-fill: red;");
			e.printStackTrace();
		}
	}
	private record Filter(String key, String value) {
		@Override
		public String toString() {
			return key + " = " + value;
		}
	}

	private record ContentItem(String name, String hash) {
		@Override
		public String toString() {
			return (name != null ? name : "Unknown") + " [" + (hash != null ? hash.substring(0, Math.min(hash.length(), 8)) : "????") + "]";
		}
	}
}

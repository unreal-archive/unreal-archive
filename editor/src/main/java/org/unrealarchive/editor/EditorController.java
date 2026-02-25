package org.unrealarchive.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.unrealarchive.common.CLI;
import org.unrealarchive.content.RepositoryManager;

public class EditorController {

	private RepositoryManager repositoryManager;

	@FXML private TabPane tabPane;
	@FXML private Tab homeTab;
	@FXML private Tab authorsTab;
	@FXML private Tab managedTab;
	@FXML private Tab contentTab;
	@FXML private Tab gameTypesTab;
	@FXML private Tab collectionsTab;

	@FXML private HomeController homePaneController;
	@FXML private AuthorsController authorsPaneController;
	@FXML private ContentController contentPaneController;
	@FXML private ManagedController managedPaneController;
	@FXML private GameTypesController gameTypesPaneController;
	@FXML private CollectionsController collectionsPaneController;

	@FXML
	public void initialize() {
		authorsTab.selectedProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal) authorsPaneController.refresh();
		});
		contentTab.selectedProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal) contentPaneController.refresh();
		});
		gameTypesTab.selectedProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal) gameTypesPaneController.refresh();
		});
	}

	public void setArgs(List<String> args) {
		CLI cli = CLI.parse(args.toArray(new String[0]));
		if (cli.option("content-path", null) == null) {
			Path localData = Paths.get("unreal-archive-data");
			if (Files.isDirectory(localData)) {
				cli.putOption("content-path", localData.toAbsolutePath().toString());
			}
		}

		try {
			this.repositoryManager = new RepositoryManager(cli);

			authorsPaneController.setRepositoryManager(repositoryManager);
			contentPaneController.setRepositoryManager(repositoryManager);
			managedPaneController.setRepositoryManager(repositoryManager);
			gameTypesPaneController.setRepositoryManager(repositoryManager);
			collectionsPaneController.setRepositoryManager(repositoryManager);

			// Select home tab by default
			tabPane.getSelectionModel().select(homeTab);

//			// Refresh whichever tab is currently selected
//			Tab selected = tabPane.getSelectionModel().getSelectedItem();
//			if (selected == authorsTab) authorsPaneController.refresh();
//			else if (selected == contentTab) contentPaneController.refresh();
//			else if (selected == gameTypesTab) gameTypesPaneController.refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

package cl.camodev.wosbot.research.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

public class ResearchLayoutController extends AbstractProfileController {

    @FXML
    private CheckBox checkBoxEnableResearch;

    @FXML
    private void initialize() {
        checkBoxMappings.put(checkBoxEnableResearch, EnumConfigurationKey.RESEARCH_ENABLED_BOOL);
        initializeChangeEvents();
    }
}

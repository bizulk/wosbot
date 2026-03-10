package cl.camodev.wosbot.skiptutorial.view;

import cl.camodev.wosbot.common.view.AbstractProfileController;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.profile.model.ProfileAux;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.serv.task.TaskQueue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

public class SkipTutorialLayoutController extends AbstractProfileController {

    @FXML
    private CheckBox checkBoxEnableSkipTutorial;

    private ProfileAux currentProfile;

    @FXML
    private void initialize() {
        // Map the controls to their respective configuration keys
        checkBoxMappings.put(checkBoxEnableSkipTutorial, EnumConfigurationKey.SKIP_TUTORIAL_ENABLED_BOOL);

        // Initialize change events (inherited from AbstractProfileController)
        initializeChangeEvents();

        // Add additional listener for dynamic control
        checkBoxEnableSkipTutorial.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (currentProfile != null) {
                handleDynamicToggle(newVal);
            }
        });
    }

    @Override
    public void onProfileLoad(ProfileAux profile) {
        super.onProfileLoad(profile);
        this.currentProfile = profile;
    }

    private void handleDynamicToggle(boolean enabled) {
        if (currentProfile == null) {
            return;
        }

        TaskQueue queue = ServScheduler.getServices().getQueueManager().getQueue(currentProfile.getId());
        if (queue == null) {
            return;
        }

        if (enabled) {
            queue.executeTaskNow(TpDailyTaskEnum.SKIP_TUTORIAL, true);
        } else {
            queue.removeTask(TpDailyTaskEnum.SKIP_TUTORIAL);
        }
    }
}

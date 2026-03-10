package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.ot.DTORawImage;
import cl.camodev.wosbot.serv.task.DelayedTask;
import java.util.ArrayList;
import java.util.List;

/**
 * Skip Tutorial Task.
 * This task is intended to skip the initial tutorial of the game.
 * It spam clicks a specific area and occasionally searches for a hand pointer to click.
 */
public class SkipTutorialTask extends DelayedTask {

    public SkipTutorialTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
        super(profile, tpTask);
    }

    @Override
    protected void execute() {
        logInfo("Starting Skip Tutorial Task...");

        DTOPoint clickBoxTopLeft = new DTOPoint(588, 73);
        DTOPoint clickBoxBottomRight = new DTOPoint(677, 113);

        while (true) {
            checkPreemption();

            // Check if task is still enabled in configuration
            try {
                Boolean enabled = cl.camodev.wosbot.serv.impl.ServProfiles.getServices().getProfiles().stream()
                        .filter(p -> p.getId().equals(profile.getId()))
                        .findFirst()
                        .map(p -> p.getConfig(EnumConfigurationKey.SKIP_TUTORIAL_ENABLED_BOOL, Boolean.class))
                        .orElse(false);

                if (enabled != null && !enabled) {
                    logInfo("Skip Tutorial Task disabled in settings. Stopping execution.");
                    break;
                }
            } catch (Exception e) {
                logWarning("Failed to check Skip Tutorial task status: " + e.getMessage());
            }

            // Spam click 30 times
            for (int i = 0; i < 30; i++) {
                tapRandomPoint(clickBoxTopLeft, clickBoxBottomRight);
                sleepTask(250); // Small delay between spam clicks to prevent overloading ADB
            }

            // Rapidly take 3 screenshots
            logInfo("Rapidly taking 3 screenshots...");
            List<DTORawImage> screenshots = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                screenshots.add(emuManager.captureScreenshotViaADB(EMULATOR_NUMBER));
            }

            // Search each screenshot sequentially for the hand template
            logInfo("Searching for hand template in screenshots...");
            boolean handFound = false;
            for (DTORawImage rawImage : screenshots) {
                DTOImageSearchResult result = emuManager.searchTemplate(EMULATOR_NUMBER, rawImage, EnumTemplates.SKIP_TUTORIAL_HAND, 80.0);
                if (result != null && result.isFound()) {
                    logInfo("Hand template found! Clicking it.");
                    tapPoint(result.getPoint());
                    sleepTask(500); // Give it some time to process the click
                    handFound = true;
                    break;
                }
            }
        }
        
        logInfo("Finishing Skip Tutorial Task.");
    }
}

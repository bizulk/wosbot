package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.DelayedTask;
import java.time.LocalDateTime;

public class ResearchTask extends DelayedTask {

    public ResearchTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
        super(profile, tpTask);
    }

    @Override
    protected void execute() {
        logInfo("Starting Research task...");
        // Boilerplate logic
        logInfo("Research logic not implemented yet.");
        logInfo("Research task ended. Rescheduling.");
        this.reschedule(LocalDateTime.now().plusHours(1));
    }
}

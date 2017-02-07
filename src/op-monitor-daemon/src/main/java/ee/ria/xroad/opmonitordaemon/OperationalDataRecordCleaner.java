/**
 * The MIT License
 * Copyright (c) 2016 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ee.ria.xroad.opmonitordaemon;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

import lombok.extern.slf4j.Slf4j;

import org.joda.time.DateTime;

import org.quartz.JobDataMap;
import org.quartz.SchedulerException;

import ee.ria.xroad.common.opmonitoring.OpMonitoringSystemProperties;
import ee.ria.xroad.common.util.JobManager;
import ee.ria.xroad.common.util.MessageSendingJob;

import static ee.ria.xroad.opmonitordaemon.OpMonitorDaemonDatabaseCtx.doInTransaction;

/**
 * Deletes outdated operational data records from the database.
 */
@Slf4j
final class OperationalDataRecordCleaner extends UntypedActor {

    public static final String START_CLEANING = "doClean";

    private static final String OPERATIONAL_DATA_RECORD_CLEANER =
            OperationalDataRecordCleaner.class.getSimpleName();

    /**
     * Initializes the operational data recorder cleaner creating an operational
     * data records cleaner actor in the given actor system and scheduling a
     * periodic cleanup with the provided job manager.
     * @param jobManager the job manager
     * @param actorSystem the actor system
     */
    public static void init(JobManager jobManager, ActorSystem actorSystem) {
        actorSystem.actorOf(Props.create(OperationalDataRecordCleaner.class),
                OPERATIONAL_DATA_RECORD_CLEANER);

        registerCronJob(jobManager, actorSystem, START_CLEANING,
                OpMonitoringSystemProperties.getOpMonitorCleanInterval());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        log.trace("onReceive({})", message);

        if (message.equals(START_CLEANING)) {
            try {
                handleCleanup();
            } catch (Exception e) {
                log.error("Failed to clean outdated operational data records"
                        + " from the database", e);
            }
        } else {
            unhandled(message);
        }
    }

    private static void handleCleanup() throws Exception {
        cleanRecords(new DateTime().minusDays(
                OpMonitoringSystemProperties.getOpMonitorKeepRecordsForDays()));
    }

    static int cleanRecords(DateTime before) throws Exception {
        log.trace("cleanRecords({})", before);

        return doInTransaction(session -> {
            String hql =
                    "delete OperationalDataRecord r where r.monitoringDataTs < "
                    + TimeUnit.MILLISECONDS.toSeconds(before.getMillis());

            int removed = session.createQuery(hql).executeUpdate();

            if (removed == 0) {
                log.info("No outdated operational data records to remove from"
                        + " the database");
            } else {
                log.info("Removed {} outdated operational data records from"
                        + " the database", removed);
            }

            return removed;
        });
    }

    private static void registerCronJob(JobManager jobManager,
            ActorSystem actorSystem, Object message, String cronExpression) {
        ActorSelection actor = actorSystem.actorSelection(
                "/user/" + OPERATIONAL_DATA_RECORD_CLEANER);

        JobDataMap jobData = MessageSendingJob.createJobData(actor, message);

        try {
            jobManager.registerJob(MessageSendingJob.class,
                    OPERATIONAL_DATA_RECORD_CLEANER + "Job", cronExpression,
                    jobData);
        } catch (SchedulerException e) {
            log.error("Unable to schedule job", e);
        }
    }
}

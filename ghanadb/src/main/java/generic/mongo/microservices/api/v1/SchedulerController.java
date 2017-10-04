package generic.mongo.microservices.api.v1;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.bson.Document;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;

import generic.mongo.microservices.constant.CommonString;
import generic.mongo.microservices.model.RequestObject;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("api/v1/dbs/{db}/{collection}")
public class SchedulerController {

	@Resource
	MongoClient mongoClient;

	@Resource
	private SearchController searchController;

	@Resource
	private EmailController emailController;

	
	@Resource
	private CollectionObjectController collectionObjectController;

	public static final String COLLECTION_NAME_SCHEDULER = "scheduler";

	@SuppressWarnings("unused")
	@RequestMapping(method = RequestMethod.POST, value = "/addscheduler")
	@ApiImplicitParams({ @ApiImplicitParam(name = CommonString.DB, value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = CommonString.COLLECTION, value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> addSchedular(
			@ApiIgnore RequestObject request,
			@RequestParam(value = "groupName", required = true) String groupName,
			@RequestParam(value = "jobName", required = true) String jobName,
			@RequestParam(value = "triggerName", required = true) String triggerName,
			@RequestParam(value = "taskClass", required = true) String taskClass,
			@RequestParam(value = "startTime", required = true) long startTime,
			@RequestParam(value = "endTime", required = false) Long endTime,
			@RequestParam(value = "repeatIntervalInSeconds", required = false) Integer repeatIntervalInSeconds,
			@RequestParam(value = "repeatIntervalInMins", required = false) Integer repeatIntervalInMins) throws Exception {

		if (repeatIntervalInSeconds != null && repeatIntervalInMins != null)
			throw new Exception("Specify repeatIntervalInSeconds or repeatIntervalInMin");

		startTime += TimeUnit.SECONDS.toMillis(10);

		Document schedularDoc = new Document();
		schedularDoc.append(CommonString.JOB_NAME, jobName);
		schedularDoc = searchController.findOne(request.getDbName(), COLLECTION_NAME_SCHEDULER, schedularDoc);

		if (schedularDoc == null) {
			schedularDoc = new Document();
			schedularDoc.append(CommonString.JOB_NAME, jobName);
			schedularDoc.append("groupName", groupName);
			schedularDoc.append("triggerName", triggerName);
			schedularDoc.append("taskClass", taskClass);
			schedularDoc.append("startTime", startTime);
			if (endTime != null)
				schedularDoc.append("endTime", endTime);
			schedularDoc.append("repeatIntervalInSeconds", repeatIntervalInSeconds);
			schedularDoc.append("repeatIntervalInMins", repeatIntervalInMins);

			String objectId = collectionObjectController.insertNew(request.getDbName(), COLLECTION_NAME_SCHEDULER, schedularDoc);
			schedularDoc.append(CommonString.ID, objectId);

			scheduleJob(request.getDbName(),groupName, jobName, triggerName, taskClass, startTime, endTime, repeatIntervalInSeconds, repeatIntervalInMins);

			return new ResponseEntity<>(schedularDoc, HttpStatus.OK);
		}
		else {
			throw new Exception("Schedular already exist ");
		}
	}

	/**
	 * @param groupName
	 * @param jobName
	 * @param triggerName
	 * @param taskClass
	 * @param startTime
	 * @param repeatIntervalInSeconds
	 * @param repeatIntervalInMin
	 * @throws ClassNotFoundException
	 * @throws SchedulerException
	 */
	public void scheduleJob(String databaseName, String groupName, String jobName, String triggerName, String taskClass,
			Long startTime,
			Long endTime,
			Integer repeatIntervalInSeconds,
			Integer repeatIntervalInMin) throws ClassNotFoundException, SchedulerException {
		Class<Job> cls = (Class<Job>) Class.forName(taskClass);

		SimpleTrigger trigger;
		if (repeatIntervalInSeconds != null) {
			TriggerBuilder<SimpleTrigger> triggerBuilder = TriggerBuilder
					.newTrigger()
					.withIdentity(triggerName, groupName)
					.startAt(new Date(startTime))
					.withSchedule(SimpleScheduleBuilder.simpleSchedule()
							.withIntervalInSeconds(repeatIntervalInSeconds)
							.repeatForever());

			if (endTime != null) {
				triggerBuilder.endAt(new Date(endTime));
			}
			trigger = triggerBuilder.build();
		}
		else {
			TriggerBuilder<SimpleTrigger> triggerBuilder = TriggerBuilder
					.newTrigger()
					.withIdentity(triggerName, groupName)
					.startAt(new Date(startTime))
					.withSchedule(SimpleScheduleBuilder.simpleSchedule()
							.withIntervalInMinutes(repeatIntervalInMin)
							.repeatForever());

			if (endTime != null) {
				triggerBuilder.endAt(new Date(endTime));
			}
			trigger = triggerBuilder.build();
		}

		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("mongoClient", mongoClient);
		jobDataMap.put("databaseName", databaseName);
		jobDataMap.put("emailController",emailController);
		jobDataMap.put("collectionObjectController", collectionObjectController);
		JobDetail job = JobBuilder.newJob(cls)
				.withIdentity(jobName, groupName)
				.usingJobData(jobDataMap)
				.build();

		Scheduler scheduler = new StdSchedulerFactory().getScheduler();
		scheduler.start();
		scheduler.scheduleJob(job, trigger);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/joblist")
	@ApiImplicitParams({ @ApiImplicitParam(name = CommonString.DB, value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = CommonString.COLLECTION, value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> jobList(@ApiIgnore RequestObject request) throws Exception {

		List<Document> documentList = new ArrayList<>();
		Document jobDocument = new Document();

		Scheduler scheduler = new StdSchedulerFactory().getScheduler();

		for (String groupName : scheduler.getJobGroupNames()) {

			for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

				String jobName = jobKey.getName();
				String jobGroup = jobKey.getGroup();

				//get job's trigger
				List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
				Date nextFireTime = triggers.get(0).getNextFireTime();

				jobDocument.append("name", jobName);
				jobDocument.append("group", jobGroup);
				jobDocument.append("nextFireTime", nextFireTime);

				documentList.add(jobDocument);
			}
		}
		return new ResponseEntity<>(documentList, HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/killjob")
	@ApiImplicitParams({ @ApiImplicitParam(name = CommonString.DB, value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = CommonString.COLLECTION, value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> killjob(@ApiIgnore RequestObject request,
			@RequestParam(value = "groupName", required = true) String groupNameParam,
			@RequestParam(value = "jobName", required = true) String jobNameParam,
			@RequestParam(value = "triggerName", required = true) String triggerName) throws Exception {

		Scheduler scheduler = new StdSchedulerFactory().getScheduler();
		Set<JobKey> jobKey = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupNameParam));

		JobKey desirejobKey = new JobKey(jobNameParam, groupNameParam);
		if (jobKey.contains(desirejobKey)) {

			//get job's trigger
			List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(desirejobKey);
			for (Trigger trigger : triggers) {
				// Unschedule a particular trigger from the job (a job may have more than one trigger)
				scheduler.unscheduleJob(new TriggerKey(triggerName, groupNameParam));
			}

			// Schedule the job with the trigger
			scheduler.deleteJob(desirejobKey);

			deleteJobDetailsFromDB(request.getDbName(), jobNameParam);
		}
		else {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private void deleteJobDetailsFromDB(String dbName, String jobNameParam) {
		Document schedularDoc = new Document();
		schedularDoc.append(CommonString.JOB_NAME, jobNameParam);
		schedularDoc = searchController.findOne(dbName, COLLECTION_NAME_SCHEDULER, schedularDoc);

		MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(COLLECTION_NAME_SCHEDULER);
		Long deleteCount = collectionObjectController.doDelete(collection, schedularDoc.getString(CommonString.ID));
	}
}

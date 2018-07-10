package nl.esciencecenter.computeservice.rest.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.model.JobState;
import nl.esciencecenter.computeservice.model.Status;

@CrossOrigin
@Controller
public class StatusApiController implements StatusApi {
	@Autowired
	private JobRepository repository;
	

	@Override
	public ResponseEntity<Status> getStatus() {
		List<Job> jobs = repository.findAll();
		int errored = 0, running = 0, successful = 0, waiting = 0;
		for (Job job : jobs) {
			JobState i = job.getInternalState();
			if (i.isErrorState()) {
				errored++;
			} else if (i.isRunning()) {
				running++;
			} else if (i.isSuccess()) {
				successful++;
			} else if (i.isWaiting()) {
				waiting++;
			}
		}
		
		Status s = new Status(waiting, running, successful, errored);
		
		return new ResponseEntity<Status>(s, HttpStatus.OK);
	}
}

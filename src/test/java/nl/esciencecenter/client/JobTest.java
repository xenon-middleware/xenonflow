package nl.esciencecenter.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.esciencecenter.computeservice.model.WorkflowBinding;


public class JobTest {

	@Test
	public void equalsTest() {
		Job job1 = job1();
		Job job2 = job1();
		
		assertTrue(job1.equals(job1));
		assertFalse(job1.equals(null));
		assertFalse(job1.equals(new Object()));
		assertTrue(job1.equals(job2));
	}
	
	private Job job1() {
		Job job = new Job();
		job.setId("134897");
		job.setName("My Name");
		job.setWorkflow("src/test/resources/cwl/echo.cwl");
		job.getAdditionalInfo().put("test", "test");
		job.setLog("something/log");
		job.setState(CWLState.RUNNING);
		job.setURI("http://localhost/jobs/134897");
		WorkflowBinding binding = new WorkflowBinding();
		binding.put("a", "b");
		job.setInput(binding);
		
		WorkflowBinding out = new WorkflowBinding();
		out.put("c", "d");
		job.setOutput(out);
		
		return job;
	}
}

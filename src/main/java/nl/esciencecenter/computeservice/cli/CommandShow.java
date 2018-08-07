package nl.esciencecenter.computeservice.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Show a specific job")
public class CommandShow {
	@Parameter(names = "--id", required=true) String id;
}

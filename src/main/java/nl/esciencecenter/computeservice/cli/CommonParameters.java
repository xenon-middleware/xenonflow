package nl.esciencecenter.computeservice.cli;

import com.beust.jcommander.Parameter;

public class CommonParameters {
	@Parameter(names = "--help", help = true)
	private boolean help;

	public boolean isHelp() {
		return help;
	}

	public void setHelp(boolean help) {
		this.help = help;
	}
	
	
}

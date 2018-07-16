package org.commonwl.cwl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class ArrayAppender extends AppenderSkeleton implements Appender {

	private final List<LoggingEvent> log = new ArrayList<LoggingEvent>();
	
	public List<LoggingEvent> getLog() {
        return new ArrayList<LoggingEvent>(log);
    }

	@Override
	public void addFilter(Filter newFilter) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Filter getFilter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearFilters() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doAppend(LoggingEvent event) {
		log.add(event);
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "array-appender";
	}

	@Override
	public void setErrorHandler(ErrorHandler errorHandler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ErrorHandler getErrorHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Layout getLayout() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean requiresLayout() {
		// TODO Auto-generated method stub
		return false;
	}

}

package nl.esciencecenter.cwl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.yaml.snakeyaml.Yaml;

public class SimpleParser {

	public static Map<String, Object> load(String filename) throws FileNotFoundException { 
		return (Map<String, Object>) new Yaml().load(new FileInputStream(new File(filename)));
	}

	private static boolean match(String key, String [] options) { 
		
		if (key == null || key.length() == 0) { 
			throw new IllegalArgumentException("Key may not be empty!");
		}
		
		for (int i=0;i<options.length;i++) { 
			if (key.equals(options[i])) { 
				return true;
			}
		}
		
		return false;		
	}
	
	public static List<String> filterTypes(Map<String, Object> map, String ... values) throws FileNotFoundException {

		List<String> result = new ArrayList<>();

		if (map.isEmpty()) { 
			return result;
		}
		
		for (Entry<String, Object> entry : map.entrySet()) {

			Map<String, Object> tmp = (Map<String, Object>) entry.getValue();
				
			if (tmp.containsKey("type") && match((String) tmp.get("type"), values)) { 
				result.add((String)entry.getKey());
			}
		}
		
		
		return result;
	}

	
	private static Map<String, Object> getMap(Map<String, Object> source, String key) { 
		
		if (source.containsKey(key)) {
			try { 
				return (Map<String, Object>) source.get(key);
			} catch (Exception e) {
				return new HashMap<>();
			}
		}
		
		return new HashMap<>();
	}
	
	private static Map<String, String> expandList(List<String> inputs, Map<String, Object> binding) { 

		Map<String, String> result = new HashMap<>();
		
		for (String name : inputs) { 

			if (binding.containsKey(name)) {
				
				Map<String, Object> tmp = (Map<String, Object>) binding.get(name);
				
				if (tmp.containsKey("location")) { 
					result.put(name, (String)tmp.get("location"));
				} else if (tmp.containsKey("path")) { 
					result.put(name, (String)tmp.get("path"));
				} else { 
					result.put(name, "unknown");
				}
			} else { 
				result.put(name, "unknown");
			}
		}
		
		return result;
	} 
	
	public static void main(String [] args) { 

		if (args.length != 3) { 
			System.err.println("Usage: ...");
			System.exit(1);
		}

		try {
			// Load the CWL workflow 
			Map<String, Object> workflow = load(args[0]);
		
			// Extract the in and output descriptions
			List<String> in = filterTypes(getMap(workflow, "inputs"), "File");
			List<String> out = filterTypes(getMap(workflow, "outputs"), "File", "stdout", "stderr");
			
			// Load the inputbinding
			Map<String, Object> inputBinding = load(args[1]);
			
			// Expand the input files to their names 
			Map<String, String> boundInputs = expandList(in, inputBinding);
			
			// Load the outputbinding
			Map<String, Object> outputBinding = load(args[2]);
			
			// Expand the output files to their names 
			Map<String, String> boundOutputs = expandList(out, outputBinding);
			
			System.out.println("Got inputs: " + boundInputs);
			System.out.println("Got outputs: " + boundOutputs);
			
		} catch (Exception e) {
			System.err.println("Failed to parse: " + e);
			System.exit(1);
		}		
	}
}

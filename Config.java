package com.ir.project;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

public class Config extends Properties
{
	private PrintStream log;
	
	private static final long serialVersionUID = 1L;

	public Config(Properties defaults, PrintStream logStream)
	{
		super(defaults);
		
		this.log = System.out;
		if(logStream != null) this.log = logStream;
	}
	
	//
	// argument-loading methods
	//
	
	public void loadConfigFile(String[] args)
	{
		String configFileName = null;
		
		// find the config file-name after the --config command-line option
		for (int i = 0; i < args.length; ++i)
		{
			if (args[i].equals("--config"))
			{
				configFileName = args[++i];
				break;
			}
		}
		
		// when a config-file is specified, load it
		if (configFileName != null)
		{
			try
			{
				FileInputStream configFileInput = new FileInputStream(configFileName);
				this.load(configFileInput);
			}
			catch (IOException e)
			{
				System.err.println("config-file \"" + configFileName + "\" could not be loaded");
				e.printStackTrace(System.err);
				System.exit(-1);
			}
		}
	}
	
	public void parseArgs(String[] args)
	{
		if ( args.length == 0) {
			System.err.printf("warning: try adding --config test.cfg\n");
			// throw new IllegalArgumentException("error");
		}
		for (int i = 0; i < args.length; ++i)
		{
			if (args[i].startsWith("--"))
			{
				// get the option-name starting after the "--"
				String optionName = args[i].substring(2);
				
				// skip the --config argument - by this stage, the config-file should already be loaded
				if (optionName.equals("config"))
					++i;
				
				// store values for recognized arguments
				else
				{
					String optionValue = args[++i];
					this.setProperty(optionName, optionValue);
				}
			}
		}
	}
	
	//
	// property methods
	//
	
	@Override
	public String getProperty(String key)
	{
		String value = super.getProperty(key);
		
		if (log != null)
			log.printf("info: parameter %s set to %s\n", key, value);
		
		return value;
	}
	
	@Override
	public String getProperty(String key, String defaultValue)
	{
		String value = super.getProperty(key, defaultValue);
		
		if (log != null)
			log.printf("info: parameter %s set to %s\n", key, value);
		
		return value;
	}
	
	//
	// default-properties factory-methods
	//
	
	public static Properties defaultsIndex()
	{
		Properties properties = new Properties();
		
		properties.put("update", "false");
		
		return properties;
	}
	
	public static Properties defaultsSearch()
	{	
		Properties properties = new Properties();

		// default distance properties
		properties.setProperty("distance-function", "reciprocal");
		properties.setProperty("distance-policy",   "query");
		properties.setProperty("distance-k",        "1");
		properties.setProperty("distance-d0",       "1");
		properties.setProperty("distance-dmax",     "200");

		// default number-of-results properties
		properties.setProperty("num-results-labels",   "1000");
		properties.setProperty("num-results-listings", "1000");
		properties.setProperty("num-results-output",   "100");
		
		return properties;
	}
}
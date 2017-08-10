package com.altran.gdc.robotframework.testfxlibrary.keywords;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.python.util.PythonInterpreter;
import org.robotframework.javalib.annotation.RobotKeywords;

import com.altran.gdc.robotframework.testfxlibrary.exceptions.testfxlibraryNonFatalException;

@RobotKeywords
public class Logging {

	protected final static Map<String, String[]> VALID_LOG_LEVELS;

	static {
		VALID_LOG_LEVELS = new HashMap<String, String[]>();
		VALID_LOG_LEVELS.put("debug", new String[] { "debug", "" });
		VALID_LOG_LEVELS.put("html", new String[] { "info", ", True, False" });
		VALID_LOG_LEVELS.put("info", new String[] { "info", "" });
		VALID_LOG_LEVELS.put("trace", new String[] { "trace", "" });
		VALID_LOG_LEVELS.put("warn", new String[] { "warn", "" });
	}
	
	protected void trace(String msg) throws IOException {
		log(msg, "trace");
	}

	protected void debug(String msg) throws IOException {
		log(msg, "debug");
	}

	protected void info(String msg) throws IOException {
		log(msg, "info");
	}

	protected void html(String msg) throws IOException {
		log(msg, "html");
	}

	protected void warn(String msg) throws IOException {
		log(msg, "warn");
	}

	protected void log(String msg, String logLevel) throws IOException {
		String[] methodParameters = VALID_LOG_LEVELS.get(logLevel.toLowerCase());
		if (methodParameters != null) {
			log0(msg, methodParameters[0], methodParameters[1]);
		} else {
			throw new testfxlibraryNonFatalException(String.format("Given log level %s is invalid.", logLevel));
		}
	}

	/**
	 * Log the given message with the Robot logger.<br>
	 * <br>
	 * There is a hard limit of 100k in the Jython source code parser. 
	 * Therefore messages larger than 1k are saved on disk and the later
	 * read back into memory on the Jython side. 
	 */
	protected void log0(String msg, String methodName, String methodArguments) throws IOException {
		if (msg.length() > 1024) {
			// Message is too large.
			// There is a hard limit of 100k in the Jython source code parser
			FileWriter writer = null;
			try {
				// Write message to temp file
				File tempFile = File.createTempFile("testfxlibrary-", ".log");
				tempFile.deleteOnExit();
				writer = new FileWriter(tempFile);
				writer.write(msg);
				writer.close();
				
				// Read the message in Python back and log it.
				loggingPythonInterpreter.get().exec(
						String.format("from __future__ import with_statement\n" + "\n"
								+ "with open('%s', 'r') as msg_file:\n" + "    msg = msg_file.read()\n"
								+ "    logger.%s(msg%s)", tempFile.getAbsolutePath().replace("\\", "\\\\"), methodName,
								methodArguments));
				
			} catch (IOException e) {
				throw new testfxlibraryNonFatalException("Error in handling temp file for long log message.", e);
			} finally {
				if (writer != null) {
					writer.close();
				}
			}
		} else {
			// Message is small enough to get parsed by Jython
			loggingPythonInterpreter.get().exec(
					String.format("logger.%s('%s'%s)", methodName, msg.replace("'", "\\'").replace("\n", "\\n"),
							methodArguments));
		}
	}
	
	/**
	 * Thread local variable with loaded logger.
	 */
	protected static ThreadLocal<PythonInterpreter> loggingPythonInterpreter = new ThreadLocal<PythonInterpreter>() {
		@Override
		protected PythonInterpreter initialValue() {
			PythonInterpreter pythonInterpreter = new PythonInterpreter();
			pythonInterpreter.exec("from robot.variables import GLOBAL_VARIABLES; from robot.api import logger;");
			return pythonInterpreter;
		}
	};

}
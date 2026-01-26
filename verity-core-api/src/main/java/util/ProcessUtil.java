package util;

import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProcessUtil {
	
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void killProcess(String processName) {

		try {
			ProcessHandle.allProcesses()
					.filter(ph -> ph.info().command()
							.map(cmd -> cmd.endsWith(processName) || cmd.endsWith(processName + ".exe")).orElse(false))
					.forEach(p -> {

						if (p != null) {
							try {
								ProcessHandle targetProcess = p;
								LOGGER.info("Found process: pid({}) - {}",
										 targetProcess.pid(),
										 targetProcess.info().command().orElse("N/A")  
								 );
								
								targetProcess.destroy(); // Graceful termination

								LOGGER.info("Attempted to gracefully terminate process({})", targetProcess.pid());
								waitForTemination(targetProcess, 60, Duration.ofSeconds(1));
								if (targetProcess.isAlive()) {
									LOGGER.info("Graceful termination of process with pid = {} failed",	 targetProcess.pid() );
									targetProcess.destroyForcibly();
									LOGGER.info("Attempted to forcibly terminate process with pid({})",
											 targetProcess.pid());
									waitForTemination(targetProcess, 60, Duration.ofSeconds(1));
								}
								if (targetProcess.isAlive()) {
									LOGGER.error("Unable to terminate process with pid({})" , targetProcess.pid());
								} else {
									LOGGER.info("Process with pid({}) terminated",  + targetProcess.pid());
								}
							} catch (Throwable e) {
								e.printStackTrace();
							}
						} else {
							LOGGER.info("Process '{}' not found.",  processName);
						}
					});
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static void waitForTemination(ProcessHandle targetProcess, int maxInterval,
			Duration waitDurationPerInterval) {
		int wait = 0;
		while (targetProcess.isAlive() && wait < maxInterval) {
			sleep(waitDurationPerInterval);
			wait++;
		}
	}

	public static void sleep(Duration d) {
		try {
			Thread.sleep(d);
		} catch (Throwable e) {
			// ignore
		}
	}

}

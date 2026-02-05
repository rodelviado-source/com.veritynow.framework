package util;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProcessUtil {
	
	private static final Logger LOGGER = LogManager.getLogger();
	static Random random = new Random();
	
	public static void killProcess(String processName) {

		try {
			ProcessHandle.allProcesses()
					.filter(ph -> ph.info().command()
							.map(cmd -> cmd.endsWith(processName) || cmd.endsWith(processName + ".exe")).orElse(false))
					.forEach(p -> {

						if (p != null) {
							try {
								
								ProcessHandle targetProcess = p;
								LOGGER.trace("Found process: pid({}) - {}",
										 targetProcess.pid(),
										 targetProcess.info().command().orElse("N/A")  
								 );
								
								targetProcess.destroy(); // Graceful termination

								LOGGER.trace("Attempted to gracefully terminate process({})", targetProcess.pid());
								waitForTemination(targetProcess, 60, Duration.ofSeconds(1));
								if (targetProcess.isAlive()) {
									LOGGER.trace("Graceful termination of process with pid = {} failed",	 targetProcess.pid() );
									targetProcess.destroyForcibly();
									LOGGER.trace("Attempted to forcibly terminate process with pid({})",
											 targetProcess.pid());
									waitForTemination(targetProcess, 60, Duration.ofSeconds(1));
								}
								if (targetProcess.isAlive()) {
									LOGGER.error("Unable to terminate process with pid({})" , targetProcess.pid());
								} else {
									LOGGER.trace("Process with pid({}) terminated",  + targetProcess.pid());
								}
							} catch (Throwable e) {
								e.printStackTrace();
							}
						} else {
							LOGGER.trace("Process '{}' not found.",  processName);
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
	
	public  static int jitter(int baseDelayMs, int maxJitter) {
		Random random = new Random();
	    // Calculate jittered delay: base + random(0 to maxJitter)
	    return  baseDelayMs + random.nextInt(maxJitter);
	}

	
	public static Duration backoffWithJitter(int delayBetweenAttempts, int attempt, int maxDelay) {
		 int expDelay =  Math.min(maxDelay, delayBetweenAttempts * (int) Math.pow(2, attempt));
		 return Duration.ofMillis(ThreadLocalRandom.current().nextInt(0, expDelay));
	}
}

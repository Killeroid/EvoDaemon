package EvoDaemon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import EvoDaemon.Strategies.*;
import EvoDaemon.Transformers.*;
import EvoDaemon.Utils.*;
import EvoDaemon.Runners.*;


/*
 * Daemon that runs every x minutes and transforms code
 */
public class EvoDaemon implements Runnable {

	private boolean Debug;
	public static ConcurrentLinkedQueue<HashMap<String, Object>> evoqueue;
	private static ExecutorService executor = Executors.newSingleThreadExecutor();
	private Future<?> runningTask;
	public static long currEpoch;
	public static long epochLengthSecs = 30L;
	private boolean shutDownNow = false;
	//public static Instrumentation INSTRUMENTATION;
	private static volatile Class<?> AgentClass;

	public EvoDaemon() {
		System.out.println("[INFO] Initializing daemon thread...");
		currEpoch = 0L;
	}

	public EvoDaemon(String jar) {
		File jarFile = new File(jar);
		jarFile.deleteOnExit();
		currEpoch = 0L;
		evoqueue = new ConcurrentLinkedQueue<HashMap<String, Object>>();
		runningTask = null;
	}

	public static void runAtEpoch() {

	}

	public static void startDaemon(String jar) {
		System.out.println("--> [INFO      ] Starting daemon thread");
		try {
			Thread daemon = new Thread(new EvoDaemon(jar));
			daemon.setName("EvoDaemon");
			daemon.setUncaughtExceptionHandler(new DefaultExceptionHandler());
			daemon.start();

		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/*
	 * Get agent class loaded in JVM
	 */
	public static Class<?> getAgentClass() {
		try 
		{
			Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass("EvoAgent.Agent");
			return clazz;			
		} catch (ClassNotFoundException e) {
			System.out.println("--> [NOTICE    ] Could not load Agent");
		}
		return null;
	}

	/*
	 * Get instrumentation interface form agent loaded in JVM
	 */
	public static Instrumentation getInstrumentation() {
		try 
		{	
			Field AgentInstrumentationField = AgentClass.getDeclaredField("INSTRUMENTATION");
			//getAllLoadedClasses(AgentInstrumentation);
			
			return (Instrumentation) AgentInstrumentationField.get(null);			
		} catch (NoSuchFieldException e) {
			System.out.println("--> [NOTICE    ] Could not find Agent Instrumentation");
		} catch (SecurityException e) {
			System.out.println("--> [NOTICE    ] Could not find Agent Instrumentation");
		} catch (IllegalArgumentException e) {
			System.out.println("--> [NOTICE    ] Could not get Agent Instrumentation");
		} catch (IllegalAccessException e) {
			System.out.println("--> [NOTICE    ] Could not get Agent Instrumentation");
		}
		return null;
	}


	/*
	 * Print out all loaded classes
	 */
	public static void getAllLoadedClasses(Instrumentation instr) {
		for (Class<?> c : instr.getAllLoadedClasses()) {
			System.out.printf("%-55s %-55s %-45s\n", c.getName(), c.getCanonicalName(), c.getClassLoader());
		}
	}

	/*
	 * Run daemon thread
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@SuppressWarnings("unchecked")
	public void run() {
		System.out.println("--> [INFO      ] Starting daemon thread");
		AgentClass = getAgentClass();
		if (getInstrumentation() == null) {
			System.out.println("--> [ERROR     ] Unable to find instrumentation interface");
			return;
		}
		currEpoch = 0L;   //Start the epoch counting
		long startofEpoch = System.currentTimeMillis();
		Thread.currentThread().setUncaughtExceptionHandler(new DefaultExceptionHandler());

		
		OnLoad firstpass = new OnLoad(new HitCounter());
		getInstrumentation().addTransformer(firstpass, true);
		//getInstrumentation().addTransformer(new Deterministic(), true);

		RetransformerRunner.schedule();

		HitCounterRunner hitter = new HitCounterRunner();
		for (;;) {

			if (this.shutDownNow || executor.isShutdown() || executor.isTerminated()) {
				System.out.println("--> [WARNING   ] Excecutor is shutting down");
				break;
			}

			/*
			 * Increase epoch
			 */
			if (System.currentTimeMillis() >= (startofEpoch + (1000 * epochLengthSecs))) {
				currEpoch += 1L;
				startofEpoch = System.currentTimeMillis();
				if (currEpoch > 1) {
					Deterministic.schedule();
				}
			}

			if (runningTask != null && !runningTask.isDone()) {
				//System.out.println("[NOTICE] A task is running");
				continue;
			} else {
				runningTask = null;
			}

			try {

				HashMap<String, Object> action = evoqueue.poll();
				if (action != null) {
					//switch((actionType) action.get("actionType")) 
					switch((Integer) action.get("actionType"))
					{
					case 1: //HitCounter : 
						hitter.newHit((String) action.get("methodName"), (String) action.get("desc") , (String) action.get("methodOwner") , (Long) action.get("hitTime"));
						runningTask = executor.submit(hitter); 
						break;
					case 2: //EvoAction : 
						runningTask = executor.submit(((Class<? extends Callable<?>>) action.get("actionClass")).getDeclaredConstructor().newInstance());
						if (action.get("actionClass") == Deterministic.class) {
							EvoDaemon.getInstrumentation().removeTransformer(new Deterministic());
							System.out.println("--> [NOTICE    ] Removing Deterministic from instrumentation");
						}
						break;
					}
				}

			} catch (RejectedExecutionException e) {
				System.out.println("--> [ERROR     ] Task was rejected");	
			} catch (InstantiationException e) {
				System.out.println("--> [ERROR     ] Could not instantiate task");
			} catch (IllegalAccessException e) {
				System.out.println("--> [ERROR     ] Could not instantiate task");
			} catch (IllegalArgumentException e) {
				System.out.println("--> [ERROR     ] Could not instantiate task");
			} catch (InvocationTargetException e) {
				System.out.println("--> [ERROR     ] Could not instantiate task");
			} catch (NoSuchMethodException e) {
				System.out.println("--> [ERROR     ] Could not instantiate task");
			} catch (SecurityException e) {
				System.out.println("--> [ERROR     ] Could not instantiate task");
			}
		}
		hitter = null;
	}

	/*
	 * The following method shuts down an ExecutorService in two phases, 
	 * first by calling shutdown to reject incoming tasks, and then calling shutdownNow, 
	 * if necessary, to cancel any lingering tasks
	 * 
	 * Sourced from: http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/ExecutorService.html
	 */
	public void shutdownAndAwaitTermination(ExecutorService pool) {
		System.out.println("[INFO] Shutting down helper threads");
		this.shutDownNow = true;
		pool.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(60, TimeUnit.SECONDS))
					System.err.println("--> [ERROR     ] Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/*
	 * Get byte array from class file
	 */
	public static byte[] getByteArray(Class<?> c) {

		if (c != null) {
			try {

				InputStream in = c.getClass().getClassLoader().getResourceAsStream(c.getName().replace('.', '/') + ".class");
				byte[] bytes = IOUtils.toByteArray(in);
				in.close();
				System.out.printf(" >>>>>>> %-55s\n", c.getName());
				return bytes;
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}

		}
		return null;
	}

	/*
	 * Check if class can be instrumented
	 */
	public static boolean canInstrument(Class<?> c) {
		if (getInstrumentation().isModifiableClass(c) && c.getClassLoader() != null) {
			//if (INSTRUMENTATION.isModifiableClass(c)) {
			return AgentData.canInstrument(c.getName());	
		} else {
			return false;
		}
	}

	/*
	 * Check if class thats dervied from the classnam ecan be instrumented
	 */
	public static boolean canInstrument(String className) {
		return AgentData.canInstrument(className);
	}

	/*
	 * Get bytecode from a class
	 */
	public static byte[] getByteCode(Class<?> c) 
			throws IOException {
		byte[] bytecode = null;
		if (c != null) {
			String normalizedClassName = c.getName().replaceAll("/", ".");
			ClassWriter writer = new ClassWriter(0);
			ClassReader reader = new ClassReader(normalizedClassName);
			//ClassReader reader = new ClassReader(c.getName());
			//return reader.b;
			reader.accept(writer, 0);
			bytecode = writer.toByteArray();
		}
		return bytecode;
	}

	/*
	 * Retransform all loaded classes
	 */
	public static void retransformClasses() {
		System.out.println("--> [INFO      ] Transforming classes");
		//System.out.printf("%-55s %-55s %-45s\n", "NAME", "CANONICAL NAME", "CLASSLOADER");
		for (Class<?> c : getInstrumentation().getAllLoadedClasses()) {
			if (canInstrument(c)) {
				try {
					System.out.println("--> [INFO      ] Transforming " + c.getName() + "...");
					getInstrumentation().retransformClasses(new Class<?>[] { c });
				} catch (Exception e) {
					e.printStackTrace();
					//System.exit(1);
				}
			} 
		}
		System.out.println("--> [INFO      ] Finished transforming classes");
	}

	/*
	 * Get class from classname
	 */
	public static Class<?> getClass(String className) {
		Class<?> result = null;
		for (@SuppressWarnings("rawtypes") Class c : getInstrumentation().getAllLoadedClasses()) {
			//System.out.println("Trying to find class: " + className + " and this is: " + c.getName());
			if (canInstrument(c) && className.replace("/", ".").equalsIgnoreCase(c.getName())) {
				System.out.println("Trying to find class: " + className.replace("/", ".") + " and this is: " + c.getName());
				result = c;
				break;
			}
		}
		return result;
	}

	public void printVisitCounts() {
		for (Entry<String, HashMap<String, AtomicLong>> entry : AgentData.visitStats.entrySet()) {
			String methodFullNameDesc = entry.getKey();
			String methodName = methodFullNameDesc.substring(0, methodFullNameDesc.lastIndexOf(" >> ")) ;

			System.out.printf("%-70s %-10d\n", "NAME", "VISITS");
			//normalizedOwner + "." + methodName + " >> " + normalizedDesc;
			System.out.printf("%-70s %-10d\n", methodName, entry.getValue().get("visitCount").intValue());
		}

	}

	/*
	 * Record a visit to a number
	 */
	public static void hit(String methodName, String desc, String methodOwner) {
		long hitTime = System.nanoTime();
		HashMap<String, Object> hitter = new HashMap<String, Object>();
		hitter.put("methodName", methodName);
		hitter.put("desc", desc);
		hitter.put("methodOwner", methodOwner);
		hitter.put("hitTime", hitTime);
		//hitter.put("actionType", actionType.HitCounter);
		hitter.put("actionType", 1);
		evoqueue.add(hitter);
	}



	

	static enum actionType {
		HitCounter(1), EvoAction(2);

		private int code;

		private actionType(int c) {
			code = c;
		}

		public int getCode() {
			return code;
		}


	} 

	static class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {

		public void uncaughtException(Thread t, Throwable e) {
			System.out.println(t + " throws an uncaught exception: " + e + " Line: " + t.getStackTrace()[0].getLineNumber());
			for (StackTraceElement ste : t.getStackTrace()) {
				System.out.println(ste);
			}

		}

	}




}
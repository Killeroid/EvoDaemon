package EvoDaemon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
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
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import EvoDaemon.Transformers.*;
import EvoDaemon.Utils.*;
import EvoDaemon.Runners.*;


/*
 * Daemon that runs every x minutes and transforms code
 */
public class EvoDaemon implements Runnable {

	private boolean Debug;
	public static ConcurrentLinkedQueue<Class<? extends Callable<?>>> queue;
	public static ConcurrentLinkedQueue<HashMap<String, Object>> evoqueue;
	private static ExecutorService executor = Executors.newSingleThreadExecutor();
	private Future<?> runningTask;
	private long currEpoch;
	private boolean shutDownNow = false;
	public static volatile Instrumentation INSTRUMENTATION;

	public EvoDaemon() {
		System.out.println("Initializing daemon thread");
		this.currEpoch = System.nanoTime();
	}

	public EvoDaemon(Instrumentation instr, String jar) {
		File jarFile = new File(jar);
		jarFile.deleteOnExit();
		this.currEpoch = System.nanoTime();
		INSTRUMENTATION = instr;
		/*for (Class<?> c : instr.getAllLoadedClasses()) {
			System.out.printf("%-55s %-55s %-45s\n", c.getName(), c.getCanonicalName(), c.getClassLoader());
		}*/

		//ClassRedefinition redefs = new ClassRedefinition(new HitCounter());
		//INSTRUMENTATION.addTransformer(redefs, true);
		evoqueue = new ConcurrentLinkedQueue<HashMap<String, Object>>();
		queue = new ConcurrentLinkedQueue<Class<? extends Callable<?>>>();
		runningTask = null;
	}
	
	public static void startDaemon(Instrumentation instr, String jar) {
		System.out.println("[SUCCESS] Starting daemon thread");
		try {
			Thread daemon = new Thread(new EvoDaemon(instr, jar));
			daemon.setName("EvoDaemon");
			//daemon.setUncaughtExceptionHandler(new DefaultExceptionHandler());
			System.out.println("Daemon thread classloader: " + daemon.getContextClassLoader());
			daemon.start();

		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public static void getAllLoadedClasses(Instrumentation instr) {
		for (Class<?> c : instr.getAllLoadedClasses()) {
			//System.out.printf("%-55s %-55s %-45s\n", c.getName(), c.getCanonicalName(), c.getClassLoader());
		}
	}

	public void run() {
		System.out.println("[SUCCESS] Starting daemon thread");
		//Thread.currentThread().setUncaughtExceptionHandler(new DefaultExceptionHandler());
		
		
		ClassRedefinition redefs = new ClassRedefinition(new HitCounter());
		INSTRUMENTATION.addTransformer(redefs, true);
		System.out.println("[SUCCESS-1] Add ClassRedefinition files");
		
		//retransformClasses();
		
		System.out.println("[SUCCESS-2] Transformed files");
		//runningTask = executor.submit(new AgentTransformer(INSTRUMENTATION));
		
		AgentTransformer.schedule();
		//evoqueue.add(new HashMap<String, Object>() {{ put("actionType", 2); }});
		System.out.println("[SUCCESS] Scheduled a transformer");
		HitCounterRunner hitter = new HitCounterRunner();
		for (;;) {

			if (this.shutDownNow || executor.isShutdown() || executor.isTerminated()) {
				System.out.println("[WARNING] Excecutor is shutting down");
				break;
			}
			
			if (runningTask != null && !runningTask.isDone()) {
				//System.out.println("[NOTICE] A task is running");
				//System.out.print(".");
				continue;
			} else {
				runningTask = null;
			}
			
			System.out.print("\n");
			

			try {
				
				//System.out.println("[NOTICE] Querying for new action");
				HashMap action = null;
				// Will keep running and executing task one after the other until error occurs or we kill it 


				do {
					action = evoqueue.poll();
					if (action != null) {
						//switch((actionType) action.get("actionType")) 
						switch((Integer) action.get("actionType"))
						{
						case 1: //HitCounter : 
							hitter.newHit((String) action.get("methodName"), (String) action.get("desc") , (String) action.get("methodOwner") , (Long) action.get("hitTime"));
							runningTask = executor.submit(hitter); 
							break;
						case 2: //EvoAction : 
							//runningTask = executor.submit(queue.poll().newInstance()); 
							runningTask = executor.submit(new AgentTransformer(INSTRUMENTATION)); 
							break;
						}
					}

				} while ((runningTask == null || runningTask.isDone() || runningTask.isCancelled()));



			} catch (RejectedExecutionException e) {
				//executor.shutdown();
				e.printStackTrace();
				System.out.println("[ERROR] Task was rejected");
				//shutdownAndAwaitTermination(executor);	
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
	// 
	public void shutdownAndAwaitTermination(ExecutorService pool) {
		this.shutDownNow = true;
		pool.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(60, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
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

	public static boolean canInstrument(Class<?> c) {
		if (INSTRUMENTATION.isModifiableClass(c) && c.getClassLoader() != null) {
			//if (INSTRUMENTATION.isModifiableClass(c)) {
			return AgentData.canInstrument(c.getName());	
		} else {
			return false;
		}
	}
	
	public static boolean canInstrument(String className) {
		return AgentData.canInstrument(className);
	}
	
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
	
	public static void retransformClasses() {
		System.out.println("\n----------------Inserting Visitor Counter----------------\n");
		//System.out.printf("%-55s %-55s %-45s\n", "NAME", "CANONICAL NAME", "CLASSLOADER");
		for (Class<?> c : INSTRUMENTATION.getAllLoadedClasses()) {
			if (canInstrument(c)) {
				try {
					//System.out.printf("%-55s %-55s %-45s\n", c.getName(), c.getCanonicalName(), c.getClassLoader());
					System.out.println("[INSERT]" + c.getName());
					INSTRUMENTATION.retransformClasses(new Class<?>[] { c });
				} catch (Exception e) {
					e.printStackTrace();
					//System.exit(1);
				}
			} 
		}
		System.out.println("[SUCCESS] Visit Counter inserted");
	}
	
	/*
	 * Get class from classname
	 */
	public static Class<?> getClass(String className) {
		Class<?> result = null;
		for (@SuppressWarnings("rawtypes") Class c : INSTRUMENTATION.getAllLoadedClasses()) {
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
		for (Entry<String, AtomicInteger> entry : VisitCounter.getVisits().entrySet()) {
			String methodFullNameDesc = entry.getKey();
			String methodName = methodFullNameDesc.substring(0, methodFullNameDesc.lastIndexOf(" >> ")) ;

			System.out.printf("%-70s %-10d\n", "NAME", "VISITS");
			//normalizedOwner + "." + methodName + " >> " + normalizedDesc;
			System.out.printf("%-70s %-10d\n", methodName, entry.getValue().intValue());
		}

	}

	public void countVisit(String methodName, String desc, String methodOwner, long hitTime) {
		String normalizedOwner = methodOwner.replaceAll(Pattern.quote("."), "/");
		String normalizedDesc = (desc == null) ? "" : desc;
		String methodFullName = normalizedOwner + "." + methodName + " >> " + normalizedDesc;
		//String methodCaller = getMethodCaller();

		AgentData.visitStats.putIfAbsent(methodFullName, new HashMap<String, AtomicLong>() {{ 
			put("visitCount", new AtomicLong(0));
			put("lastVisit", new AtomicLong(System.currentTimeMillis()));
			put("visitFreq", new AtomicLong(0));
		}});


		//Count visits
		long count = AgentData.visitStats.get(methodFullName).get("visitCount").incrementAndGet();
		long lastVisit = AgentData.visitStats.get(methodFullName).get("lastVisit").getAndSet(hitTime);

		if (count <= 2) {
			AgentData.visitStats.get(methodFullName).get("visitFreq").set(hitTime - lastVisit);
		} else {
			long lastVisitFreq = AgentData.visitStats.get(methodFullName).get("visitFreq").get();
			//long newVisitFreq = ((hitTime - lastVisit) + ((count - 2) * lastVisitFreq)) / (count - 1) ;
			AgentData.visitStats.get(methodFullName).get("visitFreq").set(((hitTime - lastVisit) + ((count - 2) * lastVisitFreq)) / (count - 1));
		}


		//Count number of times invokee
		//AgentData.methodInvokeeCount.putIfAbsent(methodCaller, new AtomicLong(0));
		//AgentData.methodInvokeeCount.get(methodCaller).incrementAndGet();



	}

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
	


	class HitCounterRunner implements Runnable {
		private String className;
		private String methodName;
		private String methodDesc;
		private long hitTime;
		private boolean canRun = false;



		HitCounterRunner() {
			reset();
		}


		HitCounterRunner(String methodName, String desc, String methodOwner, long hitTime) {
			this.className = methodOwner;
			this.methodName = methodName;
			this.methodDesc = desc;
			this.hitTime = hitTime;
			this.canRun = true;
		}

		public void reset() {
			this.className = null;
			this.methodName = null;
			this.methodDesc = null;
			this.hitTime = 0;
			this.canRun = false;
		}

		public void newHit(String methodName, String desc, String methodOwner, long hitTime) {
			this.className = methodOwner;
			this.methodName = methodName;
			this.methodDesc = desc;
			this.hitTime = hitTime;
			this.canRun = true;
		}

		public void run() {
			if (this.canRun) {
				String normalizedOwner = this.className.replaceAll(Pattern.quote("."), "/");
				String normalizedDesc = (this.methodDesc == null) ? "" : this.methodDesc;
				String methodFullName = normalizedOwner + "." + methodName + " >> " + normalizedDesc;

				//Set default values
				AgentData.visitStats.putIfAbsent(methodFullName, new HashMap<String, AtomicLong>() {{ 
					put("visitCount", new AtomicLong(0));
					put("lastVisit", new AtomicLong(System.currentTimeMillis()));
					put("visitFreq", new AtomicLong(0));
				}});

				//Count visits
				long count = AgentData.visitStats.get(methodFullName).get("visitCount").incrementAndGet();
				long lastVisit = AgentData.visitStats.get(methodFullName).get("lastVisit").getAndSet(this.hitTime);

				if (count <= 2) {
					AgentData.visitStats.get(methodFullName).get("visitFreq").set(this.hitTime - lastVisit);
				} else {
					long lastVisitFreq = AgentData.visitStats.get(methodFullName).get("visitFreq").get();
					//long newVisitFreq = ((hitTime - lastVisit) + ((count - 2) * lastVisitFreq)) / (count - 1) ;
					AgentData.visitStats.get(methodFullName).get("visitFreq").set(((this.hitTime - lastVisit) + ((count - 2) * lastVisitFreq)) / (count - 1));
				}
				//System.out.print(" Method: " + normalizedOwner + "." + methodName + "() | Visit: " + count);
				reset();
				
			}

		}
	}


	static class Remover implements Runnable {
		private String className;
		private String methodName;
		private String methodDesc;

		Remover(String className, String methodName, String methodDesc) {
			this.className = className;
			this.methodName = methodName;
			this.methodDesc = methodDesc;
		}
		public void run() {
			String normalizedOwner = this.className.replaceAll(Pattern.quote("."), "/");
			String normalizedDesc = (this.methodDesc == null) ? "" : this.methodDesc;
			String methodFullName = normalizedOwner + "." + methodName + " >> " + normalizedDesc;
			AgentData.visitCounts.putIfAbsent(methodFullName, new AtomicLong(0));
			AgentData.visitCounts.get(methodFullName).incrementAndGet();
		}
	}

	static class CounterInserter implements Runnable {

		public void run() {
			for (Class<?> c : EvoDaemon.INSTRUMENTATION.getAllLoadedClasses()) {
				if (EvoDaemon.canInstrument(c)) {
					try {
						System.out.printf("%-55s %-55s %-45s\n", c.getName(), c.getCanonicalName(), c.getClassLoader());
						EvoDaemon.INSTRUMENTATION.retransformClasses(new Class<?>[] { c });
					} catch (Exception e) {
						e.printStackTrace();
						//System.exit(1);
					}
				} 
			}
			try {
				//redefs.redefineClasses(Agent.INSTRUMENTATION);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}	
	}

	static class HitFrequencyCounter implements Runnable {
		private int period;

		public HitFrequencyCounter(int periodInSecs) {
			this.period = periodInSecs;
		}

		public HitFrequencyCounter() {
			this.period = 60;
		}

		public void run() {
			long startTime = System.currentTimeMillis();

		}

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
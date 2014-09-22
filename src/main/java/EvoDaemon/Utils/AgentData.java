package EvoDaemon.Utils;

import java.lang.instrument.ClassDefinition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import EvoDaemon.Transformers.HitCounter;


public class AgentData {
	

	/*
	 * Stores list of classes that should not be instrumented
	 * https://community.oracle.com/thread/1524222?start=0&tstart=0
	 * 
	 */
	@SuppressWarnings("serial")
	private static ConcurrentHashMap<String, Integer> doNotInstrument = new ConcurrentHashMap<String, Integer>() {{
			put("com.sun.tools.attach", 0);
			put("sun.tools.attach", 0);
			put("org.objectweb.asm", 0);
			put("java.lang.String", 0);
			put("java.lang.Class", 0);
			put("java.lang.Throwable", 0);
			put("java.lang.ref.Reference", 0);
			put("java.lang.ref.SoftReference", 0);
			put("java.lang.ClassLoader", 0);
			put("java.lang.System", 0);
			put("java.lang.StackTraceElement", 0);
			put("java.lang.Object", 0);
			put("AgentJ", 0);
			//put("Test.", 0);
			//put("java.", 0);
			put("java.io", 0);
			put("sun.instrument", 0);
			put("java.lang.instrument", 0);
			put("sun.reflect", 0);
			put("java.lang.Shutdown", 0);
			put("com.sun.", 0);
			put("sun.misc", 0);
			put("EvoDaemon.", 0);
			put("EvoAgent.", 0);
			put("java.util.concurrent", 0);
			
	}};
	
	public static ConcurrentHashMap<String, AtomicLong> visitCounts = new ConcurrentHashMap<String, AtomicLong>(); //store number of visits
	
	public static ConcurrentHashMap<String, HashMap<String, AtomicLong>> visitStats = new ConcurrentHashMap<String, HashMap<String, AtomicLong>>();
	
	public static ConcurrentHashMap<String, AtomicLong> methodInvokeeCount = new ConcurrentHashMap<String, AtomicLong>(); //store number of times a method invokes something
	
	private static ConcurrentHashMap<String, HashSet<ClassDefinition>> toBeRedefined = new ConcurrentHashMap<String, HashSet<ClassDefinition>>();
	
	private static ConcurrentHashMap<String, Class<?>> loadedClasses = new ConcurrentHashMap<String, Class<?>>();  //All loaded classes
	
	public static long CollectionFreq = 60000;
	
	
	public static Set<String> getDoNotInstrument() {
		return doNotInstrument.keySet();
	}
	
	public static void addNoInstrumentClass(String name) {
		doNotInstrument.putIfAbsent(name, 0);
	}
	
	public static boolean canInstrument(String className) {
		boolean result = true;
		String normalizedClassName = className.replaceAll("/", ".");
		for (String prefix: doNotInstrument.keySet()) {
			if (normalizedClassName.startsWith(prefix)) {
				result = false;
				break;
			}
		}
		return result;
	}
	
	public static void addNewRedefinition(String name, ClassDefinition redef) {
		if (toBeRedefined.containsKey(name)) {
			toBeRedefined.get(name).add(redef);
		} else {
			HashSet<ClassDefinition> defs = new HashSet<ClassDefinition>();
			defs.add(redef);
			toBeRedefined.putIfAbsent(name, defs);
		}

	}
	
	public static void doneRedefining() {
		
	}
	
	
	public static void addClass(Class<?> clazz) {
		loadedClasses.putIfAbsent(clazz.getName(), clazz);
	}
	
	public static Class<?> classLoaded(String name) {
		return loadedClasses.get(name);
	}
	
	public static void freq(double periodSecs) {
		
	}
	
	
	/*
	 * Record when method is called
	 * 
	 * VisitFreq = old_freq + ((new_freq - old_freq) / currentHits)
	 * old_freq = (new_hit_Time - last_hit_time)
	 */
	public static void hit(String methodName, String desc, String methodOwner, long hitTime) {
		String normalizedOwner = methodOwner.replaceAll(Pattern.quote("."), "/");
		String normalizedDesc = (desc == null) ? "" : desc;
		String methodFullName = normalizedOwner + "." + methodName + " >> " + normalizedDesc;
		String methodCaller = HitCounter.getMethodCaller();
		
		if (AgentData.visitStats.containsKey(methodFullName)) {
			//Count visits
			//AgentData.visitCounts.putIfAbsent(methodFullName, new AtomicLong(0));
			//AgentData.visitCounts.get(methodFullName).incrementAndGet();
			//visitCounts.putIfAbsent(methodFullName, new AtomicLong(0));
			//visitCounts.get(methodFullName).incrementAndGet();
			
			//long hitTime = System.nanoTime();
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
			AgentData.methodInvokeeCount.putIfAbsent(methodCaller, new AtomicLong(0));
			AgentData.methodInvokeeCount.get(methodCaller).incrementAndGet();
			
			
			System.out.print(" Method: " + normalizedOwner + "." + methodName + "() | Visit: " + AgentData.visitStats.get(methodFullName).get("visitCount")
					 + " visitFreq: " + AgentData.visitStats.get(methodFullName).get("visitFreq").get());
			System.out.print(" | ");
			System.out.print("Invoked by " + methodCaller + "\n");
		}

		
		
	}
	
	
	public static double cummAvg() {
		
		return 0;
	}
	
	
	
	
	
	
}
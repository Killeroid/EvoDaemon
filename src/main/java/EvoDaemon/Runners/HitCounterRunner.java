package EvoDaemon.Runners;

import java.util.regex.Pattern;

import EvoDaemon.Utils.AgentData;

/*
 * A runnable update the records of method visits
 */
public class HitCounterRunner implements Runnable {
	private String className;
	private String methodName;
	private String methodDesc;
	private long hitTime;
	private boolean canRun = false;



	public HitCounterRunner() {
		reset();
	}


	public HitCounterRunner(String methodName, String desc, String methodOwner, long hitTime) {
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
			AgentData.initRecord(methodFullName);


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


			/*
			 * Update max's
			 */
			if (AgentData.countMaxs.get("maxVisit").longValue() < count) 
				AgentData.countMaxs.get("maxVisit").getAndSet(count);

			long visitFreq = AgentData.visitStats.get(methodFullName).get("visitFreq").longValue();

			if (AgentData.countMaxs.get("maxFreq").longValue() < visitFreq) 
				AgentData.countMaxs.get("maxFreq").getAndSet(visitFreq);

			//Count number of times invokee
			//AgentData.methodInvokeeCount.putIfAbsent(methodCaller, new AtomicLong(0));
			//AgentData.methodInvokeeCount.get(methodCaller).incrementAndGet();

			//System.out.print(" Method: " + normalizedOwner + "." + methodName + "() | Visit: " + count);
			reset();

		}

	}
}
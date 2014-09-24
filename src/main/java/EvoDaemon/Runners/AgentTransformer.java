package EvoDaemon.Runners;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.concurrent.Callable;

import EvoDaemon.EvoDaemon;


public class AgentTransformer implements Callable<Boolean> {
	public static String name = "AgentTransformer";
	
	public AgentTransformer() {
	}

	public Boolean call() {
		System.out.println("--> [INFO      ] AgentTransformer running");
		//this.instrumentor.retransformClasses();
		EvoDaemon.retransformClasses();
		System.out.println("--> [INFO      ] AgentTransformer completed");
		return true;
		
	}
	
	public static void schedule() {
		//System.out.println("[INFO] Scheduling an agent transformer");
		HashMap<String, Object> hitter = new HashMap<String, Object>();
		//hitter.put("actionType", actionType.EvoAction);
		hitter.put("actionType", 2);
		hitter.put("actionClass", AgentTransformer.class);
		//System.out.println("[NOTICE] Adding an agent transformer to the queue");
		EvoDaemon.evoqueue.add(hitter);
		System.out.println("--> [INFO      ] Scheduled an agent transformer");
	}
}
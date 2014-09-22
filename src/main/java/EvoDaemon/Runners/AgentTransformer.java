package EvoDaemon.Runners;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.concurrent.Callable;

import EvoDaemon.EvoDaemon;


public class AgentTransformer implements Callable<Boolean> {
	volatile Instrumentation instrumentor;
	
	public AgentTransformer(Instrumentation instr) {
		this.instrumentor = instr;
	}

	public Boolean call() {
		System.out.println("[SUCCESS] AgentTransformer Running");
		//this.instrumentor.retransformClasses();
		EvoDaemon.retransformClasses();
		return true;
		
	}
	
	public static void schedule() {
		System.out.println("[NOTICE] Scheduling an agent transformer");
		HashMap<String, Object> hitter = new HashMap<String, Object>();
		//hitter.put("actionType", actionType.EvoAction);
		hitter.put("actionType", 2);
		System.out.println("[NOTICE] Adding an agent transformer to the queue");
		EvoDaemon.evoqueue.add(hitter);
		EvoDaemon.queue.add(AgentTransformer.class);
	}
}
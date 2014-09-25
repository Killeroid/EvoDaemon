package EvoDaemon.Strategies;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import EvoDaemon.EvoDaemon;
import EvoDaemon.Utils.AgentData;


public class Deterministic implements Callable<Boolean>, ClassFileTransformer {
	
	//private Instrumentation INSTRUMENTATION;
	private static long minVisitCount = 5;
	public static String name = "Deterministic Evolutionary Strategy";
	private static boolean canRun = false;
	
	
	private Set<Class <? extends ClassFileTransformer>> transformers;
	private static Set<String> removableMethods;
	
	public Deterministic() {

	}

	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		
		if (!canRun) {
			return null;
		}
		
		final String normalizedClassName = className.replaceAll("/", ".");
		final String loaderName = "[" + loader.toString() + "]"; 

		
		ClassReader reader = null;
		try {
			reader = new ClassReader(classfileBuffer);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
		    @Override
		    public MethodVisitor visitMethod(int access, final String name, String desc, String signature, String[] exceptions) {
		    	
		    	if (canBeRemoved(normalizedClassName, name, desc)) {
		    		System.out.println("--> [REMOVE    ] Removing call to CLASS: " + loaderName + normalizedClassName + "." + name + "()");
		    		return null;
		    	} else {
		    		return cv.visitMethod(access, name, desc, signature, exceptions);

		    	}
		    	
		        
		        
		    }
		};
		
	
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);
		
		byte[] output = writer.toByteArray();
		
		AgentData.addNewRedefinition(className, output);

		return output;    
	}
	
	public void addTransformer(Class<? extends ClassFileTransformer> trans) {
		transformers.add(trans);
	}
	
	public static boolean canBeRemoved1(String className, String methodName, String desc) {
		String normalizedOwner = className.replaceAll(Pattern.quote("."), "/");
		String normalizedDesc = (desc == null) ? "" : desc;
		String methodFullName = normalizedOwner + "." + methodName + " >> " + normalizedDesc;
		
		if (removableMethods.contains(methodFullName)) {
			return true;
		} else {
			return false;
		}	
	}
	
	public static boolean canBeRemoved(String className, String methodName, String desc) {
		if (methodName.startsWith("<init>") || methodName.startsWith("<clinit>")) {
			return false;
		}
		if (AgentData.getMethodDetails(methodName, desc, className).get("visitCount").longValue() > minVisitCount) {
			return false;
		} else {
			return true;
		}		
	}
	
	
	
	public static boolean canBeRemovedProbability1(String className, String methodName, String desc) {
		if (methodName.startsWith("<init>") || methodName.startsWith("<clinit>"))
			return false;
		
		
		Random random = new Random();
		long rnd = random.nextInt(100);
		
		//System.out.println("[NOTICE] prob: " + prob + " rnd: " + rnd);
		if ((rnd % 2) == 1) {
			return true;
		} else {
			return false;
		}
	}
	
	
	public static void schedule() {
		HashMap<String, Object> hitter = new HashMap<String, Object>();
		//hitter.put("actionType", actionType.EvoAction);
		hitter.put("actionType", 2);
		hitter.put("actionClass", Deterministic.class);
		//System.out.println("[NOTICE] Adding an agent transformer to the queue");
		EvoDaemon.evoqueue.add(hitter);
		canRun = true;
		System.out.println("--> [INFO      ] Scheduled an Deterministic Evolutionary Strategy");
		
		//scheduleFirstInvocation(invocationIntervalSecs);
	}
	

	public Boolean call() {
		try {
			System.out.println("--> [EVOLUTION ] Deterministic Evolutionary Strategy Running");
			//EvoDaemon.getInstrumentation().addTransformer(this, true);
			EvoDaemon.retransformClasses();
			//EvoDaemon.INSTRUMENTATION.removeTransformer(this);
			removeTransformer();
			//AgentData.redefineClass(this.INSTRUMENTATION);
			canRun = false;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			EvoDaemon.getInstrumentation().removeTransformer(this);
			return false;
		}
		
	}
	
	public void removeTransformer() {
		boolean notDone = true;
		do {
			notDone = EvoDaemon.getInstrumentation().removeTransformer(new Deterministic());
			System.out.println("--> [NOTICE    ] Removing Deterministic from instrumentation");
		} while (notDone != false);	
	}
	
}
package EvoDaemon.Strategies;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
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


public class Evolutionary implements Callable<Boolean>, ClassFileTransformer {
	
	private Instrumentation INSTRUMENTATION;
	private static long nextInvocation;
	private static long invocationIntervalSecs = 60;
	public static String name = "Fitness Based Evolutionary Strategy";
	
	
	private Set<Class <? extends ClassFileTransformer>> transformers;
	private static Set<String> removableMethods;
	
	public Evolutionary(Instrumentation instr) {
		this.INSTRUMENTATION = instr;
	}

	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		
		final String normalizedClassName = className.replaceAll("/", ".");
		final String loaderName = "[" + loader.toString() + "]"; 
		
		
		if (classBeingRedefined == null || classfileBuffer == null) {
			return classfileBuffer;
		}
		
		ClassReader reader = null;
		try {
			//reader = new ClassReader(normalizedClassName);
			//reader = new ClassReader(className);
			reader = new ClassReader(classfileBuffer);
		} catch (Exception e1) {//(IOException e1) {
			e1.printStackTrace();
		}
		
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
		    @Override
		    public MethodVisitor visitMethod(int access, final String name, String desc, String signature, String[] exceptions) {
		    	/*Not enough visits*/
		    	if (canBeRemovedProbability(normalizedClassName, name, desc)) {
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
		if (methodName.startsWith("<init>") || methodName.startsWith("<clinit>"))
			return false;
		return canBeRemovedProbability(className, methodName, desc);		
	}
	
	public static boolean canBeRemovedProbability(String className, String methodName, String desc) {
		if (methodName.startsWith("<init>") || methodName.startsWith("<clinit>")) {
			return false;
			}
		
		long prob = (100 * AgentData.getMethodDetails(methodName, desc, className).get("visitCount").longValue()) / 
				AgentData.countMaxs.get("maxVisit").longValue();
		
		Random random = new Random();
		long rnd = random.nextInt(100);
		

		if (rnd >= prob) {
			return true;
		} else {
			return false;
		}
	}
	
	
	public static void schedule() {
		HashMap<String, Object> hitter = new HashMap<String, Object>();
		hitter.put("actionType", 2);
		hitter.put("actionClass", Evolutionary.class);
		EvoDaemon.evoqueue.add(hitter);
		System.out.println("---> [SCHEDULE  ] Scheduled a Fitness Based Evolutionary Strategy");
	}
	
	public static void scheduleNextInvocation(long intervalinSecs) {
		invocationIntervalSecs = intervalinSecs;
		if (System.currentTimeMillis() >= nextInvocation) {
			schedule();
		}
	}
	
	public static void scheduleNextInvocation() {	
		if (System.currentTimeMillis() >= nextInvocation) {
			schedule();
		}
	}
	
	public static void scheduleFirstInvocation(long secs) {
		nextInvocation = System.currentTimeMillis() + (secs * 1000);
		invocationIntervalSecs = secs;
		
	}



	public Boolean call() {
		try {
			System.out.println("--> [EVOLUTION ] Fitness Based Evolutionary Strategy Running");
			EvoDaemon.getInstrumentation().addTransformer(this, true);
			EvoDaemon.retransformClasses();
			EvoDaemon.getInstrumentation().removeTransformer(this);
			AgentData.redefineClass(this.INSTRUMENTATION);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			this.INSTRUMENTATION.removeTransformer(this);
			return false;
		}
		
	}
	
	
	static class MethodRemover extends MethodVisitor {
		
		private String methodClass;

		public MethodRemover(MethodVisitor mvv, String className) {
			super(Opcodes.ASM5, mvv);
			this.methodClass = className;
			// TODO Auto-generated constructor stub
		}
		
		@Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            System.out.println("Name: " + name + " Owner: " + owner + " desc: " + desc + " Opcode:" + opcode + " Interface: " + itf);
            if (!Evolutionary.canBeRemoved(this.methodClass, name, desc)) {
            	super.visitMethodInsn(opcode, owner, name, desc, itf);
            } else {
            	System.out.println("Removing call to methodA");
            	//super.visitInsn(Opcodes.NOP);
            	//super.visitInsn(Opcodes.POP);
            	
            }
            //super.visitMethodInsn(opcode, owner, name, desc, itf);
            
        }
		
	}
	
	
	
}
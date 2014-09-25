package EvoDaemon.Strategies;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import EvoDaemon.EvoDaemon;
import EvoDaemon.Utils.AgentData;


public class Evolutionary implements Callable<Boolean>, ClassFileTransformer {

	private Instrumentation INSTRUMENTATION;
	public static String name = "Fitness Based Evolutionary Strategy";


	private Set<Class <? extends ClassFileTransformer>> transformers;

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
			reader = new ClassReader(classfileBuffer);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);

		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
			@Override
			public MethodVisitor visitMethod(int access, final String name, String desc, String signature, String[] exceptions) {
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



}
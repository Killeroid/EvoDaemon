package EvoDaemon.Transformers;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;

import EvoDaemon.Utils.AgentData;

//import EvoDaemon.Agent;


public class EvoInstrumentor implements ClassFileTransformer {

	private static ArrayList<ClassDefinition> defs = new ArrayList<ClassDefinition>();
	private ClassFileTransformer transformer;

	public EvoInstrumentor(ClassFileTransformer trans) {
		this.transformer = trans;
	}

	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {

		if (AgentData.canInstrument(className)) {
			Class<?> clazz = null;
			byte[] newClassBuffer = this.transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
			//System.out.println("Storing def for file : " + className);
			try {
				clazz = Class.forName(className.replaceAll("/", "."), false, loader);
				defs.add(new ClassDefinition(clazz, newClassBuffer));
				return newClassBuffer;
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new IllegalClassFormatException(className + "was not found");
				
			} 
			
		} else {
			return classfileBuffer;
		}

		
	}

	public synchronized void redefineClasses(Instrumentation instr) throws ClassNotFoundException, UnmodifiableClassException {

		//System.out.println("Hows many defs do we have?: " + defs.size());
		ClassDefinition[] redefs = new ClassDefinition[defs.size()];
		for (int i = 0; i < defs.size(); i++) {
			redefs[i] = defs.get(i);
		}

		instr.redefineClasses(redefs);
		defs = new ArrayList<ClassDefinition>();
		redefs = null;

	}

	public void cleanup() {
		defs = new ArrayList<ClassDefinition>();
		this.transformer = null;
	}






}
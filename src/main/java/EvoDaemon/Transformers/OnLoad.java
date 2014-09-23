package EvoDaemon.Transformers;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import EvoDaemon.Utils.AgentData;



public class OnLoad implements ClassFileTransformer {

	private static ArrayList<ClassDefinition> defs = new ArrayList<ClassDefinition>();
	private ClassFileTransformer transformer;
	//private static Set<String> redefined;
	private static Set<String> transformed;
	public static String name = "OnLoad Transformer";
	
	
	private String uuid;
	private boolean canRedefined;
	
	
	public OnLoad(ClassFileTransformer trans) {
		this.transformer = trans;
		this.uuid = UUID.randomUUID().toString();
		OnLoad.transformed = new HashSet<String>();
		//OnLoad.redefined = new HashSet<String>();
		this.canRedefined = false;
	}
	
	public OnLoad(ClassFileTransformer trans, boolean redef) {
		this.transformer = trans;
		this.uuid = UUID.randomUUID().toString();
		OnLoad.transformed = new HashSet<String>();
		//OnLoad.redefined = new HashSet<String>();
		this.canRedefined = redef;
	}
	
	public String getUUID() {
		return this.uuid;
	}

	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {

		
		if (AgentData.canInstrument(className) && canBeRedefined(className)) {
			byte[] newClassBuffer = this.transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
			transformed.add(className);
			return newClassBuffer;
		} else {
			return classfileBuffer;
		}

		
	}

	
	//Call to redefine classes
	public synchronized void redefineClasses(Instrumentation instr) throws ClassNotFoundException, UnmodifiableClassException {
		if (defs.size() > 0) {
			ClassDefinition[] redefs = new ClassDefinition[defs.size()];
			defs.toArray(redefs);
			instr.redefineClasses(redefs);
			//OnLoad.redefined.addAll(transformed);
			OnLoad.defs = new ArrayList<ClassDefinition>();
			redefs = null;
			OnLoad.transformed = new HashSet<String>();
		}
	}
	

	//Aid in garbage collection
	public void cleanup() {
		defs = new ArrayList<ClassDefinition>();
		this.transformer = null;
		this.uuid = null;
		OnLoad.transformed = null;
		//OnLoad.redefined = null;
	}

	public boolean equals(Object obj) {
		
		if (obj instanceof OnLoad) {
			if (this.getUUID() == ((OnLoad) obj).getUUID()) {
				return true;
			} else {
				return false;
			}		
		} else {
			return false;
		}
	}
	
	public boolean canBeRedefined(String className) {
		if (!this.canRedefined && OnLoad.transformed.contains(className) ) {
			//System.out.println("[WARNING]: " + className.replaceAll("/", ".") + " already redefined. Skipping...");
			return false;
		} else {
			System.out.println("--[OnLoad]--> " + className);
			return true;
		}
	}
}
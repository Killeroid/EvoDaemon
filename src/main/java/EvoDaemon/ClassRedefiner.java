package EvoDaemon;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;


public class ClassRedefiner {

	Class<?> transformer;

	public ClassRedefiner(Class<?> transformerClass) throws Exception {
		if (ClassFileTransformer.class.isAssignableFrom(transformerClass)) {
			this.transformer = transformerClass;
		} else {
			throw new Exception("Wrong type of transformer");
		}
	}


	public ClassDefinition getClassDefinition(Class<?> classBeingRedefined) 
			throws InstantiationException, IllegalAccessException, IOException, IllegalClassFormatException {
		if (classBeingRedefined == null) {
			throw new NullPointerException("The supplied class is null");
		} else {
			ClassFileTransformer cft = (ClassFileTransformer) this.transformer.newInstance();
			ClassLoader loader = classBeingRedefined.getClassLoader();
			String className = classBeingRedefined.getName();
			ProtectionDomain protectionDomain = classBeingRedefined.getProtectionDomain();
			byte[] classfileBuffer = EvoDaemon.getByteCode(classBeingRedefined);

			byte[] reDefinedclassfileBuffer = cft.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
			return new ClassDefinition(classBeingRedefined, reDefinedclassfileBuffer);
		}
	}

	private static ClassDefinition getClassDefinition(Class<?> classBeingRedefined, Class<?> transformerClass) 
			throws Exception {
		if (classBeingRedefined == null) {
			throw new NullPointerException("The supplied class is null");
		} else if (!ClassFileTransformer.class.isAssignableFrom(transformerClass)) {
			throw new Exception("Wrong type of transformer");
		} else {
			ClassFileTransformer cft = (ClassFileTransformer) transformerClass.newInstance();
			ClassLoader loader = classBeingRedefined.getClassLoader();
			String className = classBeingRedefined.getName();
			ProtectionDomain protectionDomain = classBeingRedefined.getProtectionDomain();
			byte[] classfileBuffer = EvoDaemon.getByteCode(classBeingRedefined);

			byte[] reDefinedclassfileBuffer = cft.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
			return new ClassDefinition(classBeingRedefined, reDefinedclassfileBuffer);
		}

	}

	public static void redefine(Class<?>[] classesBeingRedefined, Class<?> transformerClass, Instrumentation instr) 
			throws Exception {
		ClassDefinition[] classdefs = new ClassDefinition[classesBeingRedefined.length];
		if (classesBeingRedefined.length == 0) {
			throw new NullPointerException("There are no classes supplied to be redefined");
		} else if (instr == null) {
			throw new NullPointerException("The instrumentation interface was not loaded");
		} else {
			for (int i = 0; i < classesBeingRedefined.length; i++) {
				classdefs[i] = getClassDefinition(classesBeingRedefined[i], transformerClass);
			}
			try {
				instr.redefineClasses(classdefs);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}

	public static void redefine(Class<?> classBeingRedefined, Class<?> transformerClass, Instrumentation instr) 
			throws Exception {
		if (classBeingRedefined == null) {
			throw new NullPointerException("The supplied class is null");
		} else {
			Class<?>[] classesBeingRedefined = new Class<?>[] {classBeingRedefined};
			redefine(classesBeingRedefined, transformerClass, instr);
		}
	}


}
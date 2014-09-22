package EvoDaemon.Transformers;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.objectweb.asm.*;
import EvoDaemon.*;



public class RemoveMethod implements ClassFileTransformer{
	private Set<String> instrumentedClasses = new HashSet<String>();
	String currentClassName;

	public byte[] transform(ClassLoader    loader,
            String              className,
            Class<?>            classBeingRedefined,
            ProtectionDomain    protectionDomain,
            byte[]              classfileBuffer)
			throws IllegalClassFormatException {
		
		final String normalizedClassName = className.replaceAll("/", ".");
		currentClassName = className.replaceAll(Pattern.quote("."), "/");
		
		
		

		if (!EvoDaemon.canInstrument(className) && instrumentedClasses.contains(normalizedClassName)) {
			return classfileBuffer;
			} 
		
		
		ClassReader reader = null;
		try {
			reader = new ClassReader(normalizedClassName);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		System.out.println("************Can we slim: " + className);
		
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
		    @Override
		    public MethodVisitor visitMethod(int access, final String name, String desc, String signature, String[] exceptions) {
		    	//final String methodDesc = (desc == null) ? "" : desc;
		    	/*Not enough visits*/
		    	if (canBeRemoved()) {
		    		return null;
		    	} else {
		    		MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
		    		return mv;
		    	}
		    	
		        
		        
		    }
		};
		
	
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);

		return writer.toByteArray();    
	}
	
	public boolean canBeRemoved() {
		return true;
		
	}

}

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
import EvoDaemon.Utils.VisitCounter;



public class FirstPass implements ClassFileTransformer{
	private static Set<String> instrumentedClasses = new HashSet<String>();
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

		final VisitCounter HitMan = new VisitCounter(normalizedClassName);
		
		ClassReader reader = null;
		try {
			reader = new ClassReader(normalizedClassName);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
		    @Override
		    public MethodVisitor visitMethod(int access, final String name, String desc, String signature, String[] exceptions) {
		    	final String methodDesc = (desc == null) ? "" : desc;
		        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
		        return HitMan.insertCounter(mv, access, name, currentClassName, methodDesc, signature, exceptions);
		    }
		};
		
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);
		
//		StringWriter sw = new StringWriter();
//		PrintWriter pw = new PrintWriter(sw);
//		CheckClassAdapter.verify(new ClassReader(writer.toByteArray()), false, pw);
//	    Assert.assertTrue(sw.toString(), sw.toString().length()==0);
		
		
		instrumentedClasses.add(normalizedClassName); //Make sure not to instrument this class again
		return writer.toByteArray();    
	}	
}

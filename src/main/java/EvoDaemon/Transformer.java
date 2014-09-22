package EvoDaemon;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Constructor;
import java.security.ProtectionDomain;
import java.lang.*;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.*;

public class Transformer implements ClassFileTransformer {

	protected String AdapterName;
	
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		// TODO Auto-generated method stub
		System.out.println("Modifying: " + className);
		byte[] result = classfileBuffer;
		try {
			// Create a reader for the existing bytes.	
			ClassReader reader = new ClassReader(classfileBuffer);
			// Create a writer
			ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
			// Create our class adapter, pointing to the class writer
			// and then tell the reader to notify our visitor of all 
			// bytecode instructions
			
			//reader.accept(Adapter().getConstructor(writer, className), true);
			// get the result from the writer.
			result = writer.toByteArray();
		}
		// Runtime exceptions thrown by the above code disappear
		// This catch ensures that they are at least reported.
		catch(Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	@SuppressWarnings("rawtypes")
	public void Adapter() {
		
		Object result;
		try {
			result = Class.forName(AdapterName).newInstance();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		//return result;
	}
	
	
	
}
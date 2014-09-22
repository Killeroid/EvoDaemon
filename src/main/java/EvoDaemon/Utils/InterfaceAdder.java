package EvoDaemon.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;



public abstract class InterfaceAdder extends ClassVisitor {
	
	private Set<String> newInterfaces;
	//private Class<?> newInterface;
	
	public InterfaceAdder(ClassVisitor visitor, Class<?> itf) {
		super(Opcodes.ASM5, visitor);
		//this.newInterface = itf;
		this.newInterfaces = new HashSet<String>();
		this.newInterfaces.add(Type.getInternalName(itf));
	}

	public InterfaceAdder(int asmVersion, ClassVisitor visitor, Class<?> itf) {
		super(asmVersion, visitor);
		//this.newInterface = itf;
		this.newInterfaces = new HashSet<String>();
		this.newInterfaces.add(Type.getInternalName(itf));
	}
	

	public InterfaceAdder(ClassVisitor cv, Set<Class<?>> newInterfaces) {
		super(Opcodes.ASM5, cv);
		for (Class<?> itf: newInterfaces) {
			this.newInterfaces.add(Type.getInternalName(itf));
		}
	}
	
//	public InterfaceAdder(ClassVisitor cv, Set<String> newInterfaces) {
//		super(Opcodes.ASM5, cv);
//		for (String itf: newInterfaces) {
//			this.newInterfaces.add(itf);
//		}
//	}
	
	public abstract MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions);
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		//Set<String> newInterfaces = new HashSet<String>(interfaces.length + 1);
		newInterfaces.addAll(Arrays.asList(interfaces));
		//newInterfaces.add(Type.getInternalName(newInterface));
		cv.visit(version, access, name, signature, superName, (String[]) newInterfaces.toArray());
	} 
	
	public boolean isInterfaceLoaded(ClassLoader loader, String name) {
		//loader
		
		return true;
	}
	
}
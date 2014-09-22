package EvoDaemon;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

import EvoDaemon.Utils.*;

public class Transforms implements ClassFileTransformer{
	private Set<String> instrumentedClasses = new HashSet<String>();

	public byte[] transform(ClassLoader    loader,
            String              className,
            Class<?>            classBeingRedefined,
            ProtectionDomain    protectionDomain,
            byte[]              classfileBuffer)
			throws IllegalClassFormatException {
		
		final String normalizedClassName = className.replaceAll("/", ".");
		Map<String, ArrayList<Map<String, Object>>> results;
		

		if (AgentData.canInstrument(className) && !instrumentedClasses.contains(normalizedClassName)) {
			System.out.println();
			System.out.println("Processing class " + normalizedClassName);
			instrumentedClasses.add(normalizedClassName);
			//results = Agent.getMethods(classBeingRedefined);
			
		} else {
			return classfileBuffer;
		}
		
		System.out.println("Opcodes.INVOKESTATIC: " + Opcodes.INVOKESTATIC + " Opcodes.INVOKEVIRTUAL: " + Opcodes.INVOKEVIRTUAL +" Opcodes.INVOKESPECIAL: " + Opcodes.INVOKESPECIAL +" Opcodes.INVOKEINTERFACE: " + Opcodes.INVOKEINTERFACE);
        System.out.println("Opcodes.ACC_PUBLIC: " + Opcodes.ACC_PUBLIC + " Opcodes.ACC_PRIVATE: " + Opcodes.ACC_PRIVATE +" Opcodes.ACC_PROTECTED: " + Opcodes.ACC_PROTECTED +" Opcodes.ACC_STATIC: " + Opcodes.ACC_STATIC + " Opcodes.ACC_SUPER: " + Opcodes.ACC_SUPER);

        
		//System.out.println();
		//System.out.println("Processing class " + className);

//		String normalizedClassName = className.replaceAll("/", ".");
//
//		ClassReader classReader = null;
//		
//		try {
//			classReader = new ClassReader(normalizedClassName);
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
//		ClassWriter cw = null; 
//		
//		//ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
//		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "test", "(" + "Z" + ")V", null, null);  //Z is boolean http://asm.ow2.org/doc/faq.html#Q7
//		ClassNode classNode = new ClassNode();
//		classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
//
//		@SuppressWarnings("unchecked")
//		List<MethodNode> allMethods = classNode.methods;
//		for (MethodNode methodNode : allMethods){
//			System.out.println(methodNode.name);
//		}
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
		    	if (name.contains("runner2")) {
		    		System.out.println("Removing runner 2");
		    		return null;
		    	}
		        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
		        
		        
		        System.out.println("\n---- Body of Method " + name + " ----" );
		        //if (!name.contains("runner2")) return null;
		        
		        

		        MethodVisitor mv1 = new MethodVisitor(Opcodes.ASM5, mv) {
		            public void visitCode() {
		                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "EvoDaemon/Agent", "logMethodInvocation", "()V", false);
		            }
		        };

		        
		        MethodVisitor mv2 = new MethodVisitor(Opcodes.ASM5, mv) {
		        	
		        	@Override
	                public void visitJumpInsn(int opcode, Label label) {
	                    System.out.println("Name: " + name + " Opcode:" + opcode + " Label: " + label);
	                    super.visitJumpInsn(opcode, label);
	                }
		        	
//		            public void visitCode() {
//		                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "EvoDaemon/Agent", "logMethodInvocation", "()V", false);
//		            }
		        };
		        
		        MethodVisitor mv3 = new MethodVisitor(Opcodes.ASM5, mv) {
		        	
		        	@Override
	                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
	                    System.out.println("Name: " + name + " Owner: " + owner + " desc: " + desc + " Opcode:" + opcode + " Interface: " + itf);
	                    if (!name.equalsIgnoreCase("methodA")) {
	                    	super.visitMethodInsn(opcode, owner, name, desc, itf);
	                    } else {
	                    	System.out.println("Removing call to methodA");
	                    	//super.visitInsn(Opcodes.NOP);
	                    	//super.visitInsn(Opcodes.POP);
	                    	super.visitMethodInsn(opcode, owner, name, desc, itf);
	                    }
	                    //super.visitMethodInsn(opcode, owner, name, desc, itf);
	                    
	                }
		        	
		        	@Override
		        	public void visitMaxs(int stack, int locals) {
		        		super.visitMaxs(0,0);
		        	}
		        	
		           
		        };
		        
		        InstructionAdapter i1 = new InstructionAdapter(Opcodes.ASM5, mv) {
		        	
//		        	@Override
//	                public void visitVarInsn(int opcode, int var) {
//	                    System.out.println("Name: " + name + " Opcode:" + opcode + " Var: " + var);
//	                    super.visitVarInsn(opcode, var);
//	                }
		        	
		        	@Override
	                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
	                    System.out.println("Name: " + name + " Owner: " + owner + " desc: " + desc + " Opcode:" + opcode + " Interface: " + itf);
	                    if (!name.equalsIgnoreCase("methodA")) {
	                    	super.visitMethodInsn(opcode, owner, name, desc, itf);
	                    } else {
	                    	//System.out.println("Removing call to methodA");
	                    	//super.visitInsn(Opcodes.RETURN);
	                    	//return;
	                    }
	                    //super.visitMethodInsn(opcode, owner, name, desc, itf);
		        	
		        	}
//		        	@Override
//	                public void visitTypeInsn(int opcode, String type) {
//	                    System.out.println("Name: " + name + " Opcode:" + opcode + " Type: " + type);
//	                    super.visitInsn(opcode);
//	                }
//		        	
//		        	@Override
//		        	public void visitLabel(Label label) {
//		        		System.out.println("Name: " + name + " Label:" + label.toString());
//	                    super.visitLabel(label	);
//		        	}
		        	
//		        	@Override
//		        	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,Object... bsmArgs) {
//		        		
//		        		System.out.println("Name: " + name + "desc: " + desc);
//		        		super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
//		        	}
		        	
//		        	@Override
//	                public void visitFieldInsn(int opcode, String owner, String dname, String desc) {
//	                    System.out.println("Method: " + name + " Name: " + dname + " Opcode:" + opcode + " desc: " + desc);
//	                    super.visitFieldInsn(opcode, owner, dname, desc);
//	                }
		        	
		        };
		        
		        //mv3.visitMaxs(0, 0);
		        return mv3;
		    }
		};
		
		
		
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);
		//ClassReader printer = new ClassReader(writer.toByteArray());
		//ClassReader printer = new ClassReader(classfileBuffer);
		//printer.accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(System.out)), ClassReader.SKIP_DEBUG);
		return writer.toByteArray();    

		//return classfileBuffer;
	}
	
	

//	private static void processBizMethods(Class classObject) {
//		if (MyBusinessClass.class.equals(classObject)){
//			Method[] allMethods = classObject.getDeclaredMethods();
//			for (Method aMethod : allMethods){
//				System.out.println(aMethod.getName());
//				int modifiers = aMethod.getModifiers();
//				if (Modifier.isPrivate(modifiers)){
//					System.out.println('Method ' +
//                                        aMethod.getName() + ' is private');
//				}
//			}
//		}
//	}
//
//	public static void main(String[] args) {
//		processBizMethods(MyBusinessClass.class);
//	}
}

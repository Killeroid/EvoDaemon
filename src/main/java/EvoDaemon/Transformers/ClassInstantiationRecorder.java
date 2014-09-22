package EvoDaemon.Transformers;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import EvoDaemon.Utils.AgentData;

public class ClassInstantiationRecorder implements ClassFileTransformer {
	
	private static Set<String> instrumentedClasses = new HashSet<String>();
	public static MethodInsnNode visitInstanceInsn = new MethodInsnNode(Opcodes.INVOKESTATIC, "AgentJ/Agent", "countVisit", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);

	private ArrayList<String> noCounter = new ArrayList<String>(); //List of methods missing the counter
	
	String currentClassName;
	
	public byte[] transform(ClassLoader    loader,
            String              className,
            Class<?>            classBeingRedefined,
            ProtectionDomain    protectionDomain,
            byte[]              classfileBuffer)
			throws IllegalClassFormatException {
		
		final String normalizedClassName = className.replaceAll("/", ".");
		currentClassName = className.replaceAll(Pattern.quote("."), "/");
		

		if (!AgentData.canInstrument(className) || instrumentedClasses.contains(normalizedClassName)) {
			return classfileBuffer;
			} 

		
		ClassReader reader = null;
		try {
			reader = new ClassReader(classfileBuffer);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		ClassNode classNode = new ClassNode();
		reader.accept(classNode, ClassReader.EXPAND_FRAMES);
		
		
		//<init> is the constructor for the instance, and non-static field initialization
		//<clinit> are the static initialization blocks for the class, and static field initialization.
		//http://stackoverflow.com/questions/8517121/java-vmspec-what-is-difference-between-init-and-clinit
		
		for (final Object obj : classNode.methods) {
			MethodNode mNode = (MethodNode)obj;
			if (mNode.name.equalsIgnoreCase("<init>")) {
				String normalizedOwner = className.replaceAll(Pattern.quote("."), "/");
				String normalizedDesc = (mNode.desc == null) ? "" : mNode.desc;
				String methodFullName = normalizedOwner + "." + mNode.name + " >> " + normalizedDesc;
				if (!mNode.instructions.contains(visitInstanceInsn)) {
					noCounter.add(methodFullName);
				}
			}
			
			//System.out.println("Method: " + methodFullName);
			//AgentData.visitCounts.putIfAbsent(methodFullName, new AtomicLong(0));
			//Note methods without the counter
			
		}
		
		//return classfileBuffer;
		
		reader = new ClassReader(classfileBuffer);
		
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
		    @Override
		    public MethodVisitor visitMethod(int access, final String name, String desc, String signature, String[] exceptions) {
		        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
		        
				String normalizedDesc = (desc == null) ? "" : desc;
				String methodFullName = currentClassName + "." + name + " >> " + normalizedDesc;
				System.out.println("Method: " + methodFullName);
				return mv;
				/*if (noCounter.contains(methodFullName)) {
					//System.out.print(methodFullName + " --------- \n");
					noCounter.remove(methodFullName); //Help garbage collection
					EnteringAdapter mv_insert = new EnteringAdapter(mv, access, name, desc, currentClassName); 
		        	return mv_insert;
				} else {
					//System.out.print(methodFullName + " ********** \n");
					return mv;
				}*/
		    }
		};
		
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);
				
		instrumentedClasses.add(normalizedClassName); //Make sure not to instrument this class again
		return writer.toByteArray();    
	}	
	
	
	static class EnteringAdapter extends AdviceAdapter {
		private String methodName;
		private String methodOwner;
		private String methodDesc;
		
		public EnteringAdapter(MethodVisitor mv,int access, String name, String desc, String className) {
		    super(Opcodes.ASM5, mv, access, name, desc);
		    this.methodName = name;//normalizedClassName;
		    this.methodOwner = className.replaceAll(Pattern.quote("."), "/");
		    this.methodDesc = (desc == null) ? "" : desc;
		}
		
		//Put args on stack and supply them to countVisits functions
		protected void onMethodEnter() {
			super.visitLdcInsn(methodName);
			super.visitLdcInsn(methodDesc);
		    super.visitLdcInsn(methodOwner);
		    super.visitMethodInsn(Opcodes.INVOKESTATIC, "AgentJ/Agent", "countVisit", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
		}
		
		public void visitMaxs(int stack, int locals) {
			super.visitMaxs(stack, locals);
		}
	}
	
}
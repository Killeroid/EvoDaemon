package EvoDaemon.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;


public class VisitCounter {
	
	private static ConcurrentHashMap<String, AtomicInteger> visitCounts = new ConcurrentHashMap<String, AtomicInteger>();
	public static MethodInsnNode visitCounterInsn = new MethodInsnNode(Opcodes.INVOKESTATIC, "AgentJ/Agent", "countVisit", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
	private ArrayList<String> noCounter = new ArrayList<String>(); //List of methods missing the counter

	public VisitCounter(String className) {
		try {
			ClassReader reader = new ClassReader(className);
			ClassNode classNode = new ClassNode();
			reader.accept(classNode, 0);

			for (final Object obj : classNode.methods) {
				MethodNode mNode = (MethodNode)obj;
				String normalizedOwner = className.replaceAll(Pattern.quote("."), "/");
				String normalizedDesc = (mNode.desc == null) ? "" : mNode.desc;
				String methodFullName = normalizedOwner + "." + mNode.name + " >> " + normalizedDesc;
				visitCounts.putIfAbsent(methodFullName, new AtomicInteger(0));
				//Note methods without the counter
				if (!mNode.instructions.contains(visitCounterInsn)) {
					noCounter.add(methodFullName);
				}
			}
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
	}
	



	// Called by instrumented methods anytime they're accessed
	public static void countVisit(String methodName, String desc, String methodOwner) {
		String normalizedOwner = methodOwner.replaceAll(Pattern.quote("."), "/");
		String normalizedDesc = (desc == null) ? "" : desc;
		String methodFullName = normalizedOwner + "." + methodName + " >> " + normalizedDesc;
		visitCounts.putIfAbsent(methodFullName, new AtomicInteger(0));
		visitCounts.get(methodFullName).incrementAndGet();
		//System.out.print(" Method: " + normalizedOwner + "." + methodName + "() | Visit: " + visitCounts.get(methodFullName).get());
		//System.out.print(" | ");
		//getMethodCaller();
	}
	
	// Update call counter when method is removed
	public static void resetVisits(String methodName, String desc, String methodOwner) {
		String normalizedOwner = methodOwner.replaceAll(Pattern.quote("."), "/");
		String normalizedDesc = (desc == null) ? "" : desc;
		String methodFullName = normalizedOwner + "." + methodName + " >> " + normalizedDesc;
		visitCounts.remove(methodFullName);
	}
	
	// Get the caller of the counter method
	public static String getMethodCaller() {
		final StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
		String result = ste.getClassName() + "." + ste.getMethodName() + "()";
		System.out.print("Invoked by " + result);
		return result;
	}
	
	// Insert counter caller into method that is being instrumented
	// Checks and makes sure caller hasnt already been inserted
	public MethodVisitor insertCounter(MethodVisitor mv, int access, final String name, String owner, String desc, String signature, String[] exceptions) {
		String normalizedOwner = owner.replaceAll(Pattern.quote("."), "/");
		String normalizedDesc = (desc == null) ? "" : desc;
		String methodFullName = normalizedOwner + "." + name + " >> " + normalizedDesc;
		
		if (noCounter.contains(methodFullName)) {
			//System.out.print(" --------- ");
			noCounter.remove(methodFullName); //Help garbage collection
			EnteringAdapter mv_insert = new EnteringAdapter(mv, access, name, desc, owner); 
        	return mv_insert;
		} else {
			//System.out.print(" ********** ");
			return mv;
		}
	}
	
	public static ConcurrentHashMap<String, AtomicInteger> getVisits() {
		return visitCounts;
	}
	
	// Adapter to actually insert call to countVisit in instrumented method
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
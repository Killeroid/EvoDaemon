package EvoDaemon.Utils;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class MethodReturnExaminer extends AdviceAdapter  {

	public MethodReturnExaminer(int api, MethodVisitor mv, int access, String name, String desc) {
		super(api, mv, access, name, desc);

	}

	public MethodReturnExaminer(MethodVisitor mv, int access, String name, String desc) {
		super(Opcodes.ASM5, mv, access, name, desc);

	}

	public void onMethodExit(int opcode) {
		if(opcode==RETURN) {
			super.visitInsn(ACONST_NULL);
		} else if(opcode==ARETURN || opcode==ATHROW) {
			dup();
		} else {
			if(opcode==LRETURN || opcode==DRETURN) {
				dup2();
			} else {
				dup();
			}
			box(Type.getReturnType(this.methodDesc));
		}
		super.visitIntInsn(SIPUSH, opcode);
		super.visitMethodInsn(Opcodes.INVOKESTATIC, this.getClass().getName(), "onExit", "(Ljava/lang/Object;I)V", false);
		
	}

	public static void onExit(Object param, int opcode) {
		
	}

}
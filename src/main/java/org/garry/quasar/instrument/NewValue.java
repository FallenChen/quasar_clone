package org.garry.quasar.instrument;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

public class NewValue extends BasicValue {

    public final boolean isDupped;
    public final AbstractInsnNode insn;
    public boolean omitted;

    public NewValue(Type type,boolean isDupped,AbstractInsnNode insn)
    {
        super(type);
        this.isDupped = isDupped;
        this.insn = insn;
    }

    String formatInsn()
    {
        switch (insn.getOpcode())
        {
            case Opcodes.NEW:
                return "NEW " + ((TypeInsnNode)insn).desc;
            case Opcodes.DUP:
                return "DUP";
            default:
                return "UNEXPECTED INSTRUCTION: " + insn.getOpcode();
        }
    }
}

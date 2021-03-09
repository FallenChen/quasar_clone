package org.garry.quasar.instrument;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.Map;

public class OmittedInstruction extends AbstractInsnNode {

    private final AbstractInsnNode orgInsn;

    public OmittedInstruction(AbstractInsnNode orgInsn)
    {
        super(orgInsn.getOpcode());
        this.orgInsn = orgInsn;
    }

    @Override
    public int getType() {
       return orgInsn.getType();
    }

    @Override
    public void accept(MethodVisitor methodVisitor) {

    }

    @Override
    public AbstractInsnNode clone(Map<LabelNode, LabelNode> clonedLabels) {
       return new OmittedInstruction(orgInsn.clone(clonedLabels));
    }
}

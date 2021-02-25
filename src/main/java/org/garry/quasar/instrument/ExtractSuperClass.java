package org.garry.quasar.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class ExtractSuperClass extends ClassVisitor {

    String superClass;

    public ExtractSuperClass()
    {
        super(Opcodes.ASM4);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
       this.superClass = superName;
    }
}

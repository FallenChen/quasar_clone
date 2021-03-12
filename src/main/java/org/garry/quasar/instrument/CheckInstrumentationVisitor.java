package org.garry.quasar.instrument;

import org.garry.quasar.SuspendExecution;
import org.objectweb.asm.*;

/**
 * Check if a class contains suspendable methods
 * Basically this class checks if a method is declared to throw {@link SuspendExecution}
 */
public class CheckInstrumentationVisitor extends ClassVisitor {

    static final String EXCEPTION_NAME = Type.getInternalName(SuspendExecution.class);
    static final String EXCEPTION_DESC = Type.getDescriptor(SuspendExecution.class);

    private String className;
    private MethodDatabase.ClassEntry classEntry;
    private boolean hasSuspendable;
    private boolean alreadyInstrumented;

    public CheckInstrumentationVisitor()
    {
        super(Opcodes.ASM4);
    }

    public boolean needsInstrumentation()
    {
        return hasSuspendable;
    }

    MethodDatabase.ClassEntry getClassEntry()
    {
        return classEntry;
    }

    public String getName()
    {
        return className;
    }

    public boolean isAlreadyInstrumented()
    {
        return alreadyInstrumented;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
       this.className = name;
       this.classEntry = new MethodDatabase.ClassEntry(superName);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
       if(descriptor.equals(InstrumentClass.ALREADY_INSTRUMENTED_NAME))
       {
           alreadyInstrumented = true;
       }
       return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        boolean suspendable = checkExceptions(exceptions);
        if(suspendable)
        {
            hasSuspendable = true;
            if((access & Opcodes.ACC_SYNCHRONIZED) == Opcodes.ACC_SYNCHRONIZED)
            {
                throw new UnableToInstrumentException("synchronized method", className,name,descriptor);
            }
        }
        classEntry.set(name,descriptor,suspendable);
        return null;
    }

    public static boolean checkExceptions(String[] exceptions)
    {
        if(exceptions != null)
        {
            for(String ex : exceptions)
            {
                if(ex.equals(EXCEPTION_NAME))
                {
                    return true;
                }
            }
        }
        return false;
    }
}

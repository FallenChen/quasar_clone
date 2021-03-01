package org.garry.quasar.instrument;

import org.garry.quasar.Coroutine;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

import java.util.ArrayList;
import java.util.List;

/**
 * Instrument a class by instrumenting all suspendable methods and copying the others
 */
public class InstrumentClass extends ClassVisitor {

    static final String COROUTINE_NAME = Type.getInternalName(Coroutine.class);
    static final String ALREADY_INSTRUMENTED_NAME = Type.getDescriptor(AlreadyInstrumented.class);

    private final MethodDatabase db;
    private final boolean forceInstrumentation;
    private String className;
    private MethodDatabase.ClassEntry classEntry;
    private boolean alreadyInstrumented;
    private ArrayList<MethodNode> methods;

    public InstrumentClass(ClassVisitor cv, MethodDatabase db, boolean forceInstrumentation)
    {
        super(Opcodes.ASM4,cv);
        this.db = db;
        this.forceInstrumentation = forceInstrumentation;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.classEntry = new MethodDatabase.ClassEntry(superName);

        if(version < Opcodes.V1_5)
        {
            version = Opcodes.V1_5;
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if(descriptor.equals(InstrumentClass.ALREADY_INSTRUMENTED_NAME))
        {
            alreadyInstrumented = true;
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        boolean suspendable = CheckInstrumentationVisitor.checkExceptions(exceptions);
        classEntry.set(name,descriptor,suspendable);

        if(suspendable && checkAccess(access) && !(className.equals(COROUTINE_NAME) && name.equals("yield")))
        {
            if(db.isDebug())
            {
                db.log(LogLevel.INFO,"Instrumenting method %s#%s",className,name);
            }

            if(methods == null)
            {
                methods = new ArrayList<>();
            }
            MethodNode mn = new MethodNode(access, name, descriptor, signature, exceptions);
            methods.add(mn);
            return mn;
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        db.recordSuspendableMethods(className,classEntry);

        if(methods != null)
        {
            if(alreadyInstrumented && !forceInstrumentation)
            {
                for(MethodNode mn: methods)
                {
                    mn.accept(makeOutMV(mn));
                }
            }else {
                if(!alreadyInstrumented)
                {
                    super.visitAnnotation(ALREADY_INSTRUMENTED_NAME,true);
                }
                for(MethodNode mn: methods)
                {
                    MethodVisitor outMV = makeOutMV(mn);
                    try {
                        InstrumentMethod im = new InstrumentMethod(db, className, mn);
                        if(im.collectCodeBlocks())
                        {
                            if(mn.name.charAt(0) == '<')
                            {
                                throw new UnableToInstrumentException("special method",className,mn.name,mn.desc);
                            }
                            im.accept(outMV);
                        }else {
                            mn.accept(outMV);
                        }
                    }catch (AnalyzerException ex)
                    {
                        ex.printStackTrace();
                        throw new InternalError(ex.getMessage());
                    }
                }
            }
        }
        super.visitEnd();
    }

    private MethodVisitor makeOutMV(MethodNode mn)
    {
        return super.visitMethod(mn.access,mn.name,mn.desc,mn.signature,toStringArray(mn.exceptions));
    }

    private static boolean checkAccess(int access)
    {
        return (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0;
    }

    private static String[] toStringArray(List<?> l)
    {
        if(l.isEmpty())
        {
            return null;
        }

        return l.toArray(new String[l.size()]);
    }
}

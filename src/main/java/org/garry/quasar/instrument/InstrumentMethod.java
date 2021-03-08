package org.garry.quasar.instrument;

import org.garry.quasar.SuspendException;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.List;
import java.util.Stack;

/**
 * Instrument a method to allow suspension
 */
public class InstrumentMethod {

    // todo 什么意思
    private static final String STACK_NAME = Type.getInternalName(Stack.class);

    private final MethodDatabase db;
    private final String className;
    private final MethodNode mn;
    private final Frame[] frames;
    private final int lvarStack; // The maximum number of local variables of this method.
    private final int firstLocal; // ???

    private FrameInfo[] codeBlocks = new FrameInfo[32];
    private int numCodeBlocks;
    private int additionalLocals;

    private boolean warnedAboutMonitors; // ???
    private int warnedAboutBlocking;  // ???

    private static final BlockingMethod BLOCKING_METHODS[] = {
            new BlockingMethod("java/lang/Thread", "sleep", "(J)V", "(JI)V"),
            new BlockingMethod("java/lang/Thread", "join", "()V", "(J)V", "(JI)V"),
            new BlockingMethod("java/lang/Object", "wait", "()V", "(J)V", "(JI)V"),
            new BlockingMethod("java/util/concurrent/locks/Lock", "lock", "()V"),
            new BlockingMethod("java/util/concurrent/locks/Lock", "lockInterruptibly", "()V"),
    };


    public InstrumentMethod(MethodDatabase db, String className, MethodNode mn) throws AnalyzerException {
        this.db = db;
        this.className = className;
        this.mn = mn;

        Analyzer a = new TypeAnalyzer(db);
        this.frames = a.analyze(className,mn);
        this.lvarStack = mn.maxLocals;
        this.firstLocal = ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) ? 0 : 1;
    }


    public boolean collectCodeBlocks()
    {
        int numIns = mn.instructions.size();

        for(int i=0; i<numIns; i++)
        {
            Frame f = frames[i];
            if(f != null) { // reachable
                AbstractInsnNode in = mn.instructions.get(i);
                if(in.getType() == AbstractInsnNode.METHOD_INSN)
                {
                    MethodInsnNode min = (MethodInsnNode) in;
                    int opcode = min.getOpcode();
                    if(db.isMethodSuspendable(min.owner,min.name,min.desc,
                            opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESTATIC))
                    {
                        db.log(LogLevel.DEBUG, "Method call at instruction %d to %s#%s%s is suspendable",
                                i,min.owner,min.name,min.desc);
                        FrameInfo fi = addCodeBlock(f,i);
                        splitTryCatch(fi);
                    }else
                    {
                        int blockingId = isBlockingCall(min);
                        if(blockingId >=0)
                        {
                            int mask = 1 << blockingId;
                            if(!db.isAllowBlocking())
                            {
                                throw new UnableToInstrumentException("blocking call to " +
                                        min.owner + "#" + min.name + min.desc, className,mn.name,mn.desc);
                            }else if((warnedAboutBlocking & mask) == 0)
                            {
                                warnedAboutBlocking |= mask;
                                db.log(LogLevel.WARNING, "Method %s#%s%s contains potentially blocking call" +
                                        "to " + min.owner + "#" + min.name,min.desc,className,mn.name,mn.desc);
                            }
                        }
                    }
                }
            }
        }
        addCodeBlock(null,numIns);

        return numCodeBlocks > 1;
    }

    private static int isBlockingCall(MethodInsnNode ins)
    {
        for(int i=0,n=BLOCKING_METHODS.length; i< n; i++)
        {
            if(BLOCKING_METHODS[i].match(ins))
            {
                return i;
            }
        }
        return -1;
    }


    private FrameInfo addCodeBlock(Frame f, int end)
    {
        if(++numCodeBlocks == codeBlocks.length)
        {
            FrameInfo[] newArray = new FrameInfo[numCodeBlocks*2];
            System.arraycopy(codeBlocks,0,newArray,0,codeBlocks.length);
            codeBlocks = newArray;
        }
        FrameInfo fi = new FrameInfo(f,firstLocal,end,mn.instructions,db);
        codeBlocks[numCodeBlocks] = fi;
        return fi;
    }

    // makes the given MethodVisitor visit method
    public void accept(MethodVisitor mv)
    {
        db.log(LogLevel.INFO,"Instrumenting method %s%s%s",className,mn.name,mn.desc);

        mv.visitCode();

        Label lMethodStart = new Label();
        Label lMethodEnd = new Label();
        Label lCatchSEE = new Label();
        Label lCatchAll = new Label();
        Label[] lMethodCalls = new Label[numCodeBlocks - 1];

        for(int i= 1; i<numCodeBlocks; i++)
        {
            lMethodCalls[i-1] = new Label();
        }

        mv.visitTryCatchBlock(lMethodStart, lMethodEnd, lCatchSEE, CheckInstrumentationVisitor.EXCEPTION_NAME);

        for(Object o : mn.tryCatchBlocks)
        {
            TryCatchBlockNode tcb = (TryCatchBlockNode) o;
            if(CheckInstrumentationVisitor.EXCEPTION_NAME.equals(tcb.type))
            {
                throw new UnableToInstrumentException("catch for " +
                        SuspendException.class.getSimpleName(), className, mn.name, mn.desc);
            }
            tcb.accept(mv);
        }

        if(mn.visibleParameterAnnotations != null)
        {
            dumpParameterAnnotations(mv,mn.visibleParameterAnnotations,true);
        }

        if(mn.invisibleParameterAnnotations != null)
        {
            dumpParameterAnnotations(mv,mn.invisibleParameterAnnotations,false);
        }

        if(mn.visibleAnnotations != null)
        {
            for(Object o : mn.visibleAnnotations)
            {
                AnnotationNode an = (AnnotationNode) o;
                an.accept(mv.visitAnnotation(an.desc,true));
            }
        }

        mv.visitTryCatchBlock(lMethodStart,lMethodEnd,lCatchAll,null);

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_NAME, "getStack", "()L"+STACK_NAME+";");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ASTORE, lvarStack);

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "nextMethodEntry", "()I");
        mv.visitTableSwitchInsn(1, numCodeBlocks-1, lMethodStart, lMethodCalls);

        mv.visitLabel(lMethodStart);
        dumpCodeBlock(mv, 0, 0);

        for(int i = 1; i<numCodeBlocks; i++)
        {
            FrameInfo fi = codeBlocks[i];

            MethodInsnNode min = (MethodInsnNode) mn.instructions.get(fi.endInstruction);
            if(InstrumentClass.COROUTINE_NAME.equals(min.owner) && "yield".equals(min.name))
            {
                // special case - call to yield() - resume AFTER the call
                if(min.getOpcode() != Opcodes.INVOKESTATIC)
                {
                    throw new UnableToInstrumentException("invalid call to yiled()",className,mn.name,mn.desc);
                }
                emitStoreState(mv,i,fi);
                mv.visitFieldInsn(Opcodes.GETSTATIC, STACK_NAME,
                        "exception_instance_not_for_user_code",
                        CheckInstrumentationVisitor.EXCEPTION_DESC);
                mv.visitInsn(Opcodes.ATHROW);
                min.accept(mv);
                mv.visitLabel(lMethodCalls[i-1]);
                emitRestoreState(mv,i,fi);
                dumpCodeBlock(mv,i,1);// skip the call
            }else {
                // normal case - call to a suspendable method - resume before the call
                emitStoreState(mv,i,fi);
                mv.visitLabel(lMethodCalls[i-1]);
                emitRestoreState(mv,i,fi);
                dumpCodeBlock(mv,i,0);
            }
        }

        mv.visitLabel(lMethodEnd);

        mv.visitLabel(lCatchAll);
        emitPopMethod(mv);
        mv.visitLabel(lCatchSEE);
        mv.visitInsn(Opcodes.ATHROW);// rethrow shared between catchAll and catchSSE

        if(mn.localVariables != null)
        {
            for(Object o : mn.localVariables)
            {
                ((LocalVariableNode)o).accept(mv);
            }
        }

        mv.visitMaxs(mn.maxStack + 3, mn.maxLocals+1+additionalLocals);
        mv.visitEnd();

    }

    private void splitTryCatch(FrameInfo fi)
    {
        for (int i=0; i<mn.tryCatchBlocks.size(); i++)
        {
            TryCatchBlockNode tcb = (TryCatchBlockNode) mn.tryCatchBlocks.get(i);

            int start = getLabelIdx(tcb.start);
            int end = getLabelIdx(tcb.end);

            if(start <= fi.endInstruction && end >= fi.endInstruction)
            {
                // need to split try/catch around the suspendable call
                if(start == fi.endInstruction)
                {
                    tcb.start = fi.createAfterLabel();
                }else
                {
                    if(end > fi.endInstruction)
                    {
                        TryCatchBlockNode tcb2 = new TryCatchBlockNode(fi.createAfterLabel,
                                tcb.end, tcb.handler, tcb.type);
                        mn.tryCatchBlocks.add(i+1, tcb2);
                    }
                    tcb.end = fi.createBeforeLabel();
                }
            }
        }
    }

    private void dumpCodeBlock(MethodVisitor mv, int idx, int skip)
    {
        int start = codeBlocks[idx].endInstruction;
        int end = codeBlocks[idx+1].endInstruction;

        for(int i= start+skip; i<end; i++)
        {
            AbstractInsnNode ins = mn.instructions.get(i);
            switch (ins.getOpcode())
            {
                case Opcodes.RETURN:
                case Opcodes.ARETURN:
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                case Opcodes.FRETURN:
                case Opcodes.DRETURN:
                    emitPopMethod(mv);
                    break;
                case Opcodes.MONITORENTER:
                case Opcodes.MONITOREXIT:
                    if(!db.isAllowMonitors())
                    {
                        throw new UnableToInstrumentException("synchronisation",className,mn.name,mn.desc);
                    }else if(!warnedAboutMonitors)
                    {
                        warnedAboutMonitors = true;
                        db.log(LogLevel.WARNING,"Method %s#%s%s contains synchronisation", className,mn.name, mn.desc);
                    }
                    break;
                case Opcodes.INVOKEVIRTUAL:
                    MethodInsnNode min = (MethodInsnNode) ins;
                    if("<init>".equals(min.name))
                    {
                        int argSize = TypeAnalyzer.getNumArguments(min.desc);
                        Frame frame = frames[i];
                        int stackIndex = frame.getStackSize() - argSize - 1;
                        Value thisValue = frame.getStack(stackIndex);
                        if(stackIndex >=1 && isNewValue(thisValue,true) &&
                                             isNewValue(frame.getStack(stackIndex-1),false))
                        {
                            NewValue newValue = (NewValue) thisValue;
                            if(newValue.omitted)
                            {
                                emitNewAndDup(mv,frame,stackIndex,min);
                            }
                        }else
                        {
                            db.log(LogLevel.WARNING,"Expected to find a NewValue on stack index %d: %s",stackIndex,frame);
                        }
                    }
                    break;
            }
            ins.accept(mv);
        }
    }

    private static void dumpParameterAnnotations(MethodVisitor mv, List[] parameterAnnotations,boolean visible)
    {
        for(int i= 0; i < parameterAnnotations.length; i++)
        {
            for(Object e : parameterAnnotations[i])
            {
                AnnotationNode an = (AnnotationNode)e;
                an.accept(mv.visitParameterAnnotation(i,an.desc,visible));
            }
        }
    }

    private static void emitConst(MethodVisitor mv, int value)
    {
        if(value >=-1 && value <=5)
        {
            mv.visitInsn(Opcodes.ICONST_0 + value);
        }else if((byte)value == value)
        {
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        }else if((short)value == value)
        {
            mv.visitIntInsn(Opcodes.SIPUSH,value);
        }else
        {
            mv.visitLdcInsn(value);
        }
    }





    static class FrameInfo{

        final int endInstruction;
        final int numSlots;
        final int numObjSlots;
        final int[] localSlotIndices;
        final int[] stackSlotIndices;

        FrameInfo(Frame f, int firstLocal, int endInstruction, InsnList insnList, MethodDatabase db)
        {
            this.endInstruction = endInstruction;

            int idxObj = 0;  // 对象
            int idxPrim = 0; // 基础元素

            if(f !=null)
            {
                stackSlotIndices = new int[f.getStackSize()];
                for(int i=0; i<f.getStackSize(); i++)
                {
                    BasicValue v = (BasicValue) f.getStack(i);
                    if(v instanceof NewValue)
                    {
                        NewValue newValue = (NewValue) v;
                        if(db.isDebug())
                        {
                            db.log(LogLevel.DEBUG, "Omit value from stack idx %d at instruction %d with type %s generated by %s",
                                    i, endInstruction, v, newValue.formatInsn());
                        }
                        if(!newValue.omitted)
                        {
                            newValue.omitted = true;
                            if(db.isDebug())
                            {
                                // need to log index before replacing instruction
                                db.log(LogLevel.DEBUG, "Omitting instruction %d: %s", insnList.indexOf(newValue.insn), newValue.formatInsn());
                            }
                            insnList.set(newValue.insn, new OmittedInstruction(newValue.insn));
                        }
                        stackSlotIndices[i] = -666;
                    }else if(!isNullType(v))
                    {
                        if(v.isReference())
                        {
                            stackSlotIndices[i] = idxObj++;
                        }else {
                            stackSlotIndices[i] = idxPrim++;
                        }
                    }else {
                        stackSlotIndices[i] = -666; // an invalid index
                    }
                }

                localSlotIndices = new int[f.getLocals()];
                for(int i = firstLocal; i<f.getLocals(); i++)
                {
                    BasicValue v = (BasicValue) f.getLocal(i);
                    if(!isNullType(v))
                    {
                        if(v.isReference())
                        {
                            localSlotIndices[i] = idxObj++;
                        }else {
                            localSlotIndices[i] = idxPrim++;
                        }
                    }else {
                        localSlotIndices[i] = -666;
                    }
                }
            }else
            {
                stackSlotIndices = null;
                localSlotIndices = null;
            }

            numSlots = Math.max(idxPrim,idxObj);
            numObjSlots = idxObj;
        }
    }


    private static class BlockingMethod
    {
        final String owner;
        final String name;
        final String[] descs;

        public BlockingMethod(String owner, String name, String ... descs) {
            this.owner = owner;
            this.name = name;
            this.descs = descs;
        }

        public boolean match(MethodInsnNode min)
        {
            if(owner.equals(min.owner) && name.equals(min.name))
            {
                for(String desc: descs)
                {
                    if(desc.equals(min.desc))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

    }

}

package org.garry.quasar.instrument;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.*;

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
                        spliTryCatch(fi);
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
                                db.log(LogLevel.DEBUG, "Omitting instruction %d: %s", insnList.indexOf(newValue.insn), newValue.formatInsn())
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

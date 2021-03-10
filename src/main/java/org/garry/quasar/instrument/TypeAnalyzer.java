package org.garry.quasar.instrument;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.*;

public class TypeAnalyzer extends Analyzer {

    public TypeAnalyzer(MethodDatabase db)
    {
        super(new TypeInterpreter(db));
    }

    @Override
    protected Frame newFrame(int numLocals, int numStack) {
       return new TypeFrame(numLocals, numStack);
    }

    @Override
    protected Frame newFrame(Frame src) {
       return new TypeFrame(src);
    }

    /**
     * Computes the number of arguments
     * Returns the same result as {@code Type.getArgumentTypes(desc).length}
     * just with no memory allocations
     */
    static int getNumArguments(String methodDesc)
    {
        int off = 1;
        int size = 0;
        for(;;)
        {
            char car = methodDesc.charAt(off++);
            if(car == ')')
            {
                return size;
            }
            if(car != '[')
            {
                ++size;
                if(car == 'L')
                {
                    off = methodDesc.indexOf(';',off) + 1;
                }
            }
        }
    }




    static class TypeFrame extends Frame
    {
        TypeFrame(int nLocals, int nStack)
        {
            super(nLocals,nStack);
        }

        TypeFrame(Frame src)
        {
            super(src);
        }

        @Override
        public void execute(AbstractInsnNode insn, Interpreter interpreter) throws AnalyzerException {
           switch (insn.getOpcode())
           {
               case Opcodes.INVOKEVIRTUAL:
               case Opcodes.INVOKESPECIAL:
               case Opcodes.INVOKESTATIC:
               case Opcodes.INVOKEINTERFACE:
               {
                   String desc = ((MethodInsnNode) insn).desc;
                   for(int i = getNumArguments(desc); i>0; --i)
                   {
                       pop();
                   }
                   if(insn.getOpcode() != Opcodes.INVOKESTATIC)
                   {
                       pop();
                       if(insn.getOpcode() == Opcodes.INVOKESPECIAL && getStackSize() > 0)
                       {
                           if("<init>".equals(((MethodInsnNode)insn).name))
                           {
                               Value value = pop();
                               if(value instanceof NewValue)
                               {
                                   value = new BasicValue(((NewValue)value).getType());
                               }
                               push(value);
                           }
                       }
                   }
                   Type returnType = Type.getReturnType(desc);
                   if(returnType != Type.VOID_TYPE)
                   {
                       push(interpreter.newValue(returnType));
                   }
                   break;
               }
               default:
                   super.execute(insn,interpreter);
           }
        }
    }
}

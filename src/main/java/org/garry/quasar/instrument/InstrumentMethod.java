package org.garry.quasar.instrument;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

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


    public InstrumentMethod(MethodDatabase db, String className, MethodNode mn) throws AnalyzerException {
        this.db = db;
        this.className = className;
        this.mn = mn;

        Analyzer a = new TypeAnalyzer(db);
        this.frames = a.analyze(className,mn);
        this.lvarStack = mn.maxLocals;
        this.firstLocal = ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) ? 0 : 1;
    }




}

package org.garry.quasar.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

public class InstrumentClass extends ClassVisitor {

    static final String ALREADY_INSTRUMENTED_NAME = Type.getDescriptor(AlreadyInstrumented.class);
}

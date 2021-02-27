package org.garry.quasar.instrument;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

/**
 * An extension to {@link BasicInterpreter} which collects the type of objects and arrays
 */
public class TypeInterpreter extends BasicInterpreter {

    private final MethodDatabase db;

    public TypeInterpreter(MethodDatabase db)
    {
        this.db = db;
    }

    @Override
    public BasicValue newValue(Type type) {
        if(type == null)
        {
            return BasicValue.UNINITIALIZED_VALUE;
        }
        return super.newValue(type);
    }
}

package org.garry.quasar.instrument;

import org.objectweb.asm.tree.analysis.Analyzer;

public class TypeAnalyzer extends Analyzer {

    public TypeAnalyzer(MethodDatabase db)
    {
        super(new TypeInterpreter(db));
    }


}

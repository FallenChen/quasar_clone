package org.garry.quasar.instrument;


/**
 * This exception is thrown when an unsupported construct was found in a class
 * that must be instrumented for suspension
 */
public class UnableToInstrumentException extends RuntimeException{

    private final String reason;
    private final String className;
    private final String methodName;
    private final String methodDesc;

    public UnableToInstrumentException(String reason, String className,String methodName,String methodDesc)
    {
        super(String.format("Unable to instrument class %s#%s%s because of %s", className,methodName,methodDesc,reason));
        this.reason = reason;
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
    }

    public String getReason() {
        return reason;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }
}

package org.garry.quasar.instrument;

/**
 * Allow access to the ANT logging routines
 */
public interface Log {

    void log(LogLevel level, String msg, Object... args);

    void error(String msg, Exception ex);

}

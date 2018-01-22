package org.python.pydev.logging;

import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;

public class PyLoggingPreferences {

    public static final String DEBUG_CODE_COMPLETION = "DEBUG_CODE_COMPLETION";
    public static final boolean DEFAULT_DEBUG_CODE_COMPLETION = false;

    public static final String DEBUG_ANALYSIS_REQUESTS = "DEBUG_ANALYSIS_REQUESTS";
    public static final boolean DEFAULT_DEBUG_ANALYSIS_REQUESTS = false;

    public static final String DEBUG_INTERPRETER_AUTO_UPDATE = "DEBUG_INTERPRETER_UPDATE";
    public static final boolean DEFAULT_DEBUG_INTERPRETER_AUTO_UPDATE = false;

    public static boolean isToDebugCodeCompletion() {
        if (SharedCorePlugin.inTestMode()) {
            return false;
        }
        return PydevPrefs.getPreferences().getBoolean(DEBUG_CODE_COMPLETION);
    }

    public static boolean isToDebugAnalysisRequests() {
        if (SharedCorePlugin.inTestMode()) {
            return false;
        }
        return PydevPrefs.getPreferences().getBoolean(DEBUG_ANALYSIS_REQUESTS);
    }

    public static boolean isToDebugInterpreterAutoUpdate() {
        if (SharedCorePlugin.inTestMode()) {
            return false;
        }
        return PydevPrefs.getPreferences().getBoolean(DEBUG_INTERPRETER_AUTO_UPDATE);
    }

}

package org.example;

import java.util.ArrayList;
import java.util.HashMap;

public interface Mock {
    HashMap<String, ArrayList<ArrayList<Object>>> getInvocations();
    void clearInvocations();
    void enableVerificationMode(boolean flag);
}

package org.example;

import java.util.ArrayList;
import java.util.List;

public class ArgumentMatcher {
    private List<Object> givens;

    ArgumentMatcher(List<Object> givens) {
        this.givens = new ArrayList<>(givens);
    }

    boolean matches(List<Object> parameters){
        for(int i = 0; i < givens.size(); i++) {
            Object param = givens.get(i);
            if (ArgumentMatchers.ANY.equals(param)) {
                continue;
            }
            if(OurMockito.isCaptor(param)) {
                ArgumentCaptor argumentCaptor = OurMockito.getCaptor(param);
                argumentCaptor.setValue(parameters.get(i));
                continue;
            }
            if (!param.equals(parameters.get(i))){
                return false;
            }
        }
        return true;
    }
}

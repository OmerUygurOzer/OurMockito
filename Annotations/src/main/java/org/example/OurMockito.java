package org.example;

import java.util.*;

public class OurMockito {
    private static final ArgumentMatcher NO_PARAM_MATCHER = new ArgumentMatcher(new ArrayList<>());
    private static String currentMethodId;
    private static Mock currentMock;
    private static List<Object> currentArgs = new ArrayList<>();
    private static Map<String, List<ArgumentMatcher>> sMatchers = new HashMap<>();
    private static Map<ArgumentMatcher, Object> sReturnValues = new HashMap<>();
    private static Map<Object, ArgumentCaptor> sCaptors = new HashMap<>();

    static <T> T getReturnVal(String uuid, List<Object> params) {
        if(!sMatchers.containsKey(uuid)) {
            return null;
        }
        List<ArgumentMatcher> matchers = sMatchers.get(uuid);
        for(ArgumentMatcher argumentMatcher : matchers){
            if (argumentMatcher.matches(params)) {
                return (T) sReturnValues.get(argumentMatcher);
            }
        }
        return null;
    }

    static boolean isCaptor(Object object) {
        return sCaptors.containsKey(object);
    }

    static ArgumentCaptor getCaptor(Object object) {
        return sCaptors.get(object);
    }

    static void registerCaptor(Object key, ArgumentCaptor argumentCaptor) {
        sCaptors.put(key, argumentCaptor);
    }

    static void setInvocation(Mock mock, String uuid){
        currentMock = mock;
        currentMethodId = uuid;
    }

    static void addArg(Object arg){
        currentArgs.add(arg);
    }

    public static void reset(Object object){
        Mock mock = (Mock) object;
        Set<String> methods = mock.getInvocations().keySet();
        for(String method : methods) {
            for(ArgumentMatcher argumentMatcher: sMatchers.get(method)){
                sReturnValues.remove(argumentMatcher);
            }
            sMatchers.get(method).clear();
            sMatchers.remove(method);
        }
        mock.clearInvocations();
    }

    public static <T> T verify(T stub){
        Mock mock = (Mock) stub;
        mock.enableVerificationMode(true);
        return stub;
    }

    static void verifyCurrentMethodCalled() {
        Set<String> calledMethods = currentMock.getInvocations().keySet();
        assert(calledMethods.contains(currentMethodId)):"Method has not been invoked";
        ArgumentMatcher verificationMatcher = new ArgumentMatcher(currentArgs);
        boolean matchingInvocationFound = false;
        for(List<Object> params: currentMock.getInvocations().get(currentMethodId)) {
            matchingInvocationFound |= verificationMatcher.matches(params);
        }
        assert(matchingInvocationFound): "Matching invocation has not been found";
        currentMock = null;
        currentMethodId = null;
        currentArgs.clear();
    }

    public static <T> Stubber<T> when(T stub){
        return new Stubber<T>();
    }

    public static class Stubber<T> {
        void thenReturn(T returnType) {
            ArgumentMatcher argumentMatcher = null;
            if(currentArgs.isEmpty()) {
                argumentMatcher = NO_PARAM_MATCHER;
            } else {
                argumentMatcher = new ArgumentMatcher(currentArgs);
            }
            if (!sMatchers.containsKey(currentMethodId)){
                sMatchers.put(currentMethodId, new ArrayList<>());
            }
            sMatchers.get(currentMethodId).add(argumentMatcher);
            ArrayList<ArrayList<Object>> invocations = currentMock.getInvocations().get(currentMethodId);
            invocations.remove(invocations.size()-1);
            sReturnValues.put(argumentMatcher, returnType);
            currentArgs.clear();
            currentMock = null;
            currentMethodId = null;
        }
    }

}

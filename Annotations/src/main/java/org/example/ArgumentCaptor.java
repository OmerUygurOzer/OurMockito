package org.example;

public class ArgumentCaptor<T> {
    public static <K> ArgumentCaptor<K> forClass(Class<K> clazz){
        return new ArgumentCaptor<K>();
    }

    private Object value;

    public T capture() {
        Object captureKey = new Object();
        OurMockito.registerCaptor(captureKey, this);
        OurMockito.addArg(captureKey);
        return null;
    };

    public T getValue() {
        return (T) value;
    }

    void setValue(Object object) {
        value = object;
    }

}

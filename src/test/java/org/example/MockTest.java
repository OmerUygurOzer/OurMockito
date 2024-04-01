package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.example.ArgumentMatchers.anyInt;
import static org.example.ArgumentMatchers.eq;
import static org.example.OurMockito.verify;
import static org.example.OurMockito.when;

@RunWith(JUnit4.class)
public class MockTest {

    @OurMock
    TestTargetClass testTargetClass;

    ArgumentCaptor<String> argumentCaptor;

    @BeforeEach
    public void setUp() {
        OurMockitoAnnotations.initMocks(this);
        when(testTargetClass.getName()).thenReturn("Omer");
        when(testTargetClass.doStuffWithMoarParams(anyInt(), eq(5f),
                eq("teststring"))).thenReturn("testval");
        argumentCaptor = ArgumentCaptor.forClass(String.class);
    }

    @Test
    public void testGetName() {
        System.out.println("Test:"+testTargetClass.getName());
        System.out.println("Test:"+testTargetClass.doStuffWithMoarParams(3, 5.0f, "teststring"));
        verify(testTargetClass).doStuffWithMoarParams(anyInt(), eq(5.0f), eq("teststring"));
        verify(testTargetClass).doStuffWithMoarParams(anyInt(), eq(5.0f), argumentCaptor.capture());

        System.out.println("Test:"+argumentCaptor.getValue());
    }

}

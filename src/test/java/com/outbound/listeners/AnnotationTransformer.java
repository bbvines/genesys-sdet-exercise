package com.outbound.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Automatically applies RetryAnalyzer to every @Test method in the suite.
 *
 * Without this, retryAnalyzer = RetryAnalyzer.class would need to be added
 * to every single @Test annotation manually. This listener wires it up
 * globally so no test is forgotten.
 *
 * Registered in testng.xml under <listeners>.
 */
public class AnnotationTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {
        // Apply RetryAnalyzer to every @Test automatically
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}

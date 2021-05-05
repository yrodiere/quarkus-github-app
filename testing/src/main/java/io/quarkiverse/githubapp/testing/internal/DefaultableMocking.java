package io.quarkiverse.githubapp.testing.internal;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.Consumer;

import org.mockito.Answers;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

final class DefaultableMocking<M> {

    static <M> DefaultableMocking<M> create(Class<M> clazz, Object id, Consumer<MockSettings> mockSettingsContributor,
            DefaultMockAnswer defaultMockAnswer) {
        String name = clazz.getSimpleName() + "#" + id;
        MockSettings mockSettings = Mockito.withSettings().name(name)
                .withoutAnnotations()
                .defaultAnswer(defaultMockAnswer);
        mockSettingsContributor.accept(mockSettings);
        M mock = Mockito.mock(clazz, mockSettings);
        return new DefaultableMocking<>(mock, name);
    }

    private final M mock;
    private final String name;

    private DefaultableMocking(M mock, String name) {
        this.mock = mock;
        this.name = name;
    }

    M mock() {
        return mock;
    }

    String name() {
        return name;
    }

    Object callMock(InvocationOnMock invocation) throws Throwable {
        return call(mock, invocation);
    }

    Object callMockOrDefault(InvocationOnMock invocation, Answer<?> defaultAnswer) throws Throwable {
        try {
            return callMock(invocation);
        } catch (MissingMockBehaviorError error) {
            call(Mockito.verify(mock, Mockito.atLeastOnce()), invocation);
            return defaultAnswer.answer(invocation);
        }
    }

    Object call(Object self, InvocationOnMock invocation) throws IllegalAccessException, InvocationTargetException {
        Object[] argumentsForJava = unexpandArguments(invocation);
        try {
            return invocation.getMethod().invoke(self, argumentsForJava);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw e;
            }
        }
    }

    // invocation.getArguments() expands varargs, so we need to put them back into an array
    private Object[] unexpandArguments(InvocationOnMock invocation) {
        Method method = invocation.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] unexpandedArguments = new Object[parameters.length];
        Object[] argumentsWithExpandedVarArgs = invocation.getArguments();
        for (int i = 0; i < unexpandedArguments.length; i++) {
            if (parameters[i].isVarArgs()) {
                // Wrap all remaining arguments into an array
                int varArgSize = argumentsWithExpandedVarArgs.length - parameters.length + 1;
                Object varArgs = Array.newInstance(parameters[i].getType().getComponentType(), varArgSize);
                if (varArgSize > 0) {
                    System.arraycopy(argumentsWithExpandedVarArgs, i, varArgs, 0, varArgSize);
                }
                unexpandedArguments[i] = varArgs;
            } else {
                unexpandedArguments[i] = argumentsWithExpandedVarArgs[i];
            }
        }
        return unexpandedArguments;
    }

    static class DefaultMockAnswer implements Answer<Object>, Serializable {
        public boolean allowMockConfigurationAndVerifying = true;
        private final Answer<Object> answerWhenConfiguringAndVerifying = Answers.RETURNS_DEFAULTS;

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            if (allowMockConfigurationAndVerifying) {
                return answerWhenConfiguringAndVerifying.answer(invocation);
            } else {
                throw new MissingMockBehaviorError(invocation);
            }
        }
    }
}

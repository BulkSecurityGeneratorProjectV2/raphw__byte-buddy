package net.bytebuddy.dynamic;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

public class ModifierResolverDesynchronizing {

    private static final int MODIFIERS = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    public MethodDescription methodDescription;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getAdjustedModifiers(anyBoolean())).thenReturn(MODIFIERS);
    }

    @Test
    public void testModifierResolutionImplemented() throws Exception {
        assertThat(ModifierResolver.Desynchronizing.INSTANCE.transform(methodDescription, true), is(MODIFIERS & ~Opcodes.ACC_SYNCHRONIZED));
        verify(methodDescription).getAdjustedModifiers(true);
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testModifierResolutionNonImplemented() throws Exception {
        assertThat(ModifierResolver.Desynchronizing.INSTANCE.transform(methodDescription, false), is(MODIFIERS & ~Opcodes.ACC_SYNCHRONIZED));
        verify(methodDescription).getAdjustedModifiers(false);
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ModifierResolver.Desynchronizing.class).apply();
    }
}

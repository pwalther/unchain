package ch.redmoon.unchain.client.provider;

import ch.redmoon.unchain.client.UnchainClient;
import ch.redmoon.unchain.client.UnchainContext;
import ch.redmoon.unchain.client.model.Variant;
import ch.redmoon.unchain.client.model.VariantPayload;
import dev.openfeature.sdk.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnchainFeatureProviderTest {

    @Mock
    private UnchainClient unchainClient;

    private UnchainFeatureProvider provider;

    @BeforeEach
    void setUp() {
        provider = new UnchainFeatureProvider(unchainClient);
    }

    @Test
    void getName() {
        assertEquals("UnchainFeatureProvider", provider.getMetadata().getName());
    }

    @Test
    void getBooleanEvaluation() {
        when(unchainClient.isEnabled(eq("feature-key"), any(UnchainContext.class))).thenReturn(true);

        EvaluationContext ctx = new ImmutableContext("user-1");
        ProviderEvaluation<Boolean> eval = provider.getBooleanEvaluation("feature-key", false, ctx);

        assertTrue(eval.getValue());
        assertEquals(Reason.TARGETING_MATCH.toString(), eval.getReason());

        verify(unchainClient).isEnabled(eq("feature-key"), any(UnchainContext.class));
    }

    @Test
    void getStringEvaluation() {
        Variant variant = new Variant();
        variant.setName("string-variant");
        VariantPayload payload = new VariantPayload();
        payload.setType("string");
        payload.setValue("foo");
        variant.setPayload(payload);

        when(unchainClient.getVariant(eq("feature-key"), any(UnchainContext.class))).thenReturn(variant);

        EvaluationContext ctx = new ImmutableContext("user-1");
        ProviderEvaluation<String> eval = provider.getStringEvaluation("feature-key", "default", ctx);

        assertEquals("foo", eval.getValue());
        assertEquals("string-variant", eval.getVariant());
        assertEquals(Reason.TARGETING_MATCH.toString(), eval.getReason());
    }

    @Test
    void getStringEvaluation_NoPayload() {
        Variant variant = new Variant();
        variant.setName("simple-variant");
        // No payload

        when(unchainClient.getVariant(eq("feature-key"), any(UnchainContext.class))).thenReturn(variant);

        EvaluationContext ctx = new ImmutableContext("user-1");
        ProviderEvaluation<String> eval = provider.getStringEvaluation("feature-key", "default", ctx);

        assertEquals("simple-variant", eval.getValue()); // Fallback to variant name
        assertEquals("simple-variant", eval.getVariant());
    }

    @Test
    void getStringEvaluation_Default() {
        when(unchainClient.getVariant(eq("feature-key"), any(UnchainContext.class))).thenReturn(null);

        EvaluationContext ctx = new ImmutableContext("user-1");
        ProviderEvaluation<String> eval = provider.getStringEvaluation("feature-key", "default", ctx);

        assertEquals("default", eval.getValue());
        assertEquals(Reason.DEFAULT.toString(), eval.getReason());
    }

    @Test
    void getIntegerEvaluation() {
        Variant variant = new Variant();
        variant.setName("int-variant");
        VariantPayload payload = new VariantPayload();
        payload.setType("string");
        payload.setValue("42");
        variant.setPayload(payload);

        when(unchainClient.getVariant(eq("feature-key"), any(UnchainContext.class))).thenReturn(variant);

        EvaluationContext ctx = new ImmutableContext("user-1");
        ProviderEvaluation<Integer> eval = provider.getIntegerEvaluation("feature-key", 0, ctx);

        assertEquals(42, eval.getValue());
    }

    @Test
    void contextMapping() {
        when(unchainClient.isEnabled(anyString(), any(UnchainContext.class))).thenReturn(true);

        MutableContext ctx = new MutableContext("target-user");
        ctx.add("attr1", "val1");
        ctx.add("attr2", 123);

        provider.getBooleanEvaluation("key", false, ctx);

        ArgumentCaptor<UnchainContext> captor = ArgumentCaptor.forClass(UnchainContext.class);
        verify(unchainClient).isEnabled(eq("key"), captor.capture());

        UnchainContext captured = captor.getValue();
        assertEquals("target-user", captured.getUserId());
        assertEquals("val1", captured.getProperty("attr1"));
        assertEquals("123", captured.getProperty("attr2"));
    }
}

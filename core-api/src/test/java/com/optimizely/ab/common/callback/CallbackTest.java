package com.optimizely.ab.common.callback;

import org.junit.Test;

import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class CallbackTest {
    @Test
    public void testHolder_defaultsNoOp() {
        new Callback.Holder<>().success(new Object());
        new Callback.Holder<>().failure(new Object(), new RuntimeException());
    }

    @Test
    public void testHolder_setSuccess() {
        Callback.Holder<Integer> holder = new Callback.Holder<>();

        Callback<Number> cb = (Callback<Number>) spy(Callback.class);
        holder.set(cb);

        holder.success(1);
        verify(cb, only()).success(1);

        // can be cleared (defaults to no-op)
        holder.set(null);
        holder.success(2);
        holder.failure(-2, new RuntimeException());

        verifyNoMoreInteractions(cb);
    }

    @Test
    public void testHolder_setFailure() {
        Callback.Holder<Integer> holder = new Callback.Holder<>();

        Callback<Number> cb = (Callback<Number>) spy(Callback.class);
        holder.set(cb);

        RuntimeException ex = new RuntimeException();
        holder.failure(1, ex);
        verify(cb, only()).failure(1, ex);

        // can be cleared (defaults to no-op)
        holder.set(null);
        holder.success(2);
        holder.failure(-2, new RuntimeException());

        verifyNoMoreInteractions(cb);
    }
}
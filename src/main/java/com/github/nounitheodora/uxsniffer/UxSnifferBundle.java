package com.github.nounitheodora.uxsniffer;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class UxSnifferBundle extends DynamicBundle {

    @NonNls
    public static final String BUNDLE = "messages.UxSnifferBundle";

    private static final UxSnifferBundle INSTANCE = new UxSnifferBundle();

    private UxSnifferBundle() {
        super(BUNDLE);
    }

    public static @NotNull String message(
            @NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
            Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }
}

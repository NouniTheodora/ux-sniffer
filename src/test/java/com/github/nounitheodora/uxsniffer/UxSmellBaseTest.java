package com.github.nounitheodora.uxsniffer;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public abstract class UxSmellBaseTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }
}

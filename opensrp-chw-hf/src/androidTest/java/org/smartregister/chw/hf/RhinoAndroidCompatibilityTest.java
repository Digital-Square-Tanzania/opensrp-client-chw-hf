package org.smartregister.chw.hf;

import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

@RunWith(AndroidJUnit4.class)
public class RhinoAndroidCompatibilityTest {

    @Test
    public void wrapsJavaObjectWithoutJavaCompilerApi() {
        Context context = Context.enter();
        try {
            context.setOptimizationLevel(-1);
            Scriptable scope = context.initStandardObjects();

            assertNotNull(Context.toObject(new Object(), scope));
        } finally {
            Context.exit();
        }
    }
}

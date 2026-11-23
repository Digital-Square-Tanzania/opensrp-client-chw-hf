package org.smartregister.chw.hf.runtime;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RhinoAndroidRuntimeTest {

    @Test
    public void initializesJavaBridgeAndRunsRulesWithoutJavaCompilerModule() throws Exception {
        String classpath = codePath(RhinoAndroidRuntimeTest.class) + File.pathSeparator + codePath(Context.class);
        Process process = new ProcessBuilder(
                Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "--limit-modules", "java.base",
                "-cp", classpath, RuleSmokeCheck.class.getName())
                .redirectErrorStream(true)
                .start();
        try {
            assertTrue("Rhino smoke check timed out", process.waitFor(20, TimeUnit.SECONDS));
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(output, 0, process.exitValue());
        } finally {
            process.destroyForcibly();
        }
    }

    private static String codePath(Class<?> source) throws Exception {
        return Paths.get(source.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    }

    public static class RuleSmokeCheck {
        public static void main(String[] args) throws Exception {
            try {
                Class.forName("javax.lang.model.SourceVersion");
                throw new AssertionError("The Java compiler module must be absent");
            } catch (ClassNotFoundException expected) {
                // Android does not provide this compiler API.
            }

            Context context = Context.enter();
            try {
                // Mirror the interpreter and Java bridge setup used by ZiggyService.
                context.setOptimizationLevel(-1);
                ScriptableObject scope = context.initStandardObjects();
                List<String> facts = new ArrayList<>(Arrays.asList("eligible", "screened"));
                scope.put("facts", scope, Context.toObject(facts, scope));

                String rule = "facts.size() === 2 && String(facts.get(0)) === 'eligible'";
                if (!Boolean.TRUE.equals(context.evaluateString(scope, rule, "synthetic-rule", 1, null))) {
                    throw new AssertionError("The rule should match the initial facts");
                }

                facts.remove(0);
                if (!Boolean.FALSE.equals(context.evaluateString(scope, rule, "synthetic-rule", 1, null))) {
                    throw new AssertionError("The rule should reject the changed facts");
                }
            } finally {
                Context.exit();
            }
        }
    }
}

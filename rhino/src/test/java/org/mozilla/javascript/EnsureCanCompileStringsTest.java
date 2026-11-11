package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests for {@link Context#ensureCanCompileStrings(VarScope, String)}. */
class EnsureCanCompileStringsTest {

    private static final class CheckingContext extends Context {
        private final List<TopLevel> scopes_ = new ArrayList<>();
        private final List<String> sources_ = new ArrayList<>();

        CheckingContext(ContextFactory factory) {
            super(factory);
        }

        @Override
        protected void ensureCanCompileStrings(VarScope scope, String source) {
            scopes_.add(ScriptableObject.getTopLevelScope(scope));
            sources_.add(source);
            if (source.contains("blocked")) {
                throw ScriptRuntime.constructError("EvalError", "blocked by the embedding");
            }
        }
    }

    private static final ContextFactory FACTORY =
            new ContextFactory() {
                @Override
                protected Context makeContext() {
                    final Context cx = new CheckingContext(this);
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    return cx;
                }
            };

    @Test
    void directEval() {
        FACTORY.call(
                cx -> {
                    final TopLevel global = cx.initStandardObjects();
                    assertEquals(
                            2, cx.evaluateString(global, "eval('1 + 1')", "test", 1, null));

                    final CheckingContext checking = (CheckingContext) cx;
                    assertEquals(List.of("1 + 1"), checking.sources_);
                    assertSame(global, checking.scopes_.get(0));
                    return null;
                });
    }

    @Test
    void indirectEvalOfOtherRealm() {
        FACTORY.call(
                cx -> {
                    final TopLevel global = cx.initStandardObjects();
                    final TopLevel other = cx.initStandardObjects();
                    ScriptableObject.putProperty(global, "other", other.getGlobalThis());

                    assertEquals(
                            2, cx.evaluateString(global, "other.eval('1 + 1')", "test", 1, null));

                    final CheckingContext checking = (CheckingContext) cx;
                    assertEquals(List.of("1 + 1"), checking.sources_);
                    assertSame(other, checking.scopes_.get(0));
                    return null;
                });
    }

    @Test
    void functionConstructorOfOtherRealm() {
        FACTORY.call(
                cx -> {
                    final TopLevel global = cx.initStandardObjects();
                    final TopLevel other = cx.initStandardObjects();
                    ScriptableObject.putProperty(global, "other", other.getGlobalThis());

                    assertEquals(
                            3,
                            cx.evaluateString(
                                    global,
                                    "new other.Function('a', 'return a + 1')(2)",
                                    "test",
                                    1,
                                    null));

                    final CheckingContext checking = (CheckingContext) cx;
                    assertEquals(1, checking.sources_.size());
                    assertSame(other, checking.scopes_.get(0));
                    return null;
                });
    }

    @Test
    void blocked() {
        FACTORY.call(
                cx -> {
                    final TopLevel global = cx.initStandardObjects();
                    assertEquals(
                            "EvalError EvalError",
                            cx.evaluateString(
                                    global,
                                    "var r = '';\n"
                                            + "try { eval('\"blocked\"'); r += 'ok'; }"
                                            + " catch(e) { r += e.name; }\n"
                                            + "r += ' ';\n"
                                            + "try { new Function('return \"blocked\"'); r += 'ok'; }"
                                            + " catch(e) { r += e.name; }\n"
                                            + "r",
                                    "test",
                                    1,
                                    null));
                    return null;
                });
    }
}

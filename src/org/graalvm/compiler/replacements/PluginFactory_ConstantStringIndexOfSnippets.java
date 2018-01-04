// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.replacements;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import org.graalvm.compiler.nodes.ConstantNode;

public class PluginFactory_ConstantStringIndexOfSnippets implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.replacements.ConstantStringIndexOfSnippets
    //       method: charArrayBaseOffset(org.graalvm.compiler.core.common.spi.ArrayOffsetProvider)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class ConstantStringIndexOfSnippets_charArrayBaseOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            assert checkInjectedArgument(b, args[0], targetMethod);
            org.graalvm.compiler.core.common.spi.ArrayOffsetProvider arg0 = injectedArrayOffsetProvider;
            int result = org.graalvm.compiler.replacements.ConstantStringIndexOfSnippets.charArrayBaseOffset(arg0);
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.core.common.spi.ArrayOffsetProvider injectedArrayOffsetProvider;

        private ConstantStringIndexOfSnippets_charArrayBaseOffset(InjectionProvider injection) {
            this.injectedArrayOffsetProvider = injection.getInjectedArgument(org.graalvm.compiler.core.common.spi.ArrayOffsetProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new ConstantStringIndexOfSnippets_charArrayBaseOffset(injection), org.graalvm.compiler.replacements.ConstantStringIndexOfSnippets.class, "charArrayBaseOffset", org.graalvm.compiler.core.common.spi.ArrayOffsetProvider.class);
    }
}

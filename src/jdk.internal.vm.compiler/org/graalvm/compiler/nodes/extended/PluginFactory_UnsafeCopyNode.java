// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.nodes.extended;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

public class PluginFactory_UnsafeCopyNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.nodes.extended.UnsafeCopyNode
    //       method: copy(java.lang.Object,long,java.lang.Object,long,jdk.vm.ci.meta.JavaKind,jdk.internal.vm.compiler.word.LocationIdentity)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$CustomFactoryPlugin
    private static final class UnsafeCopyNode_copy extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            ValueNode arg2 = args[2];
            ValueNode arg3 = args[3];
            jdk.vm.ci.meta.JavaKind arg4;
            if (args[4].isConstant()) {
                arg4 = snippetReflection.asObject(jdk.vm.ci.meta.JavaKind.class, args[4].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            jdk.internal.vm.compiler.word.LocationIdentity arg5;
            if (args[5].isConstant()) {
                arg5 = snippetReflection.asObject(jdk.internal.vm.compiler.word.LocationIdentity.class, args[5].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            return org.graalvm.compiler.nodes.extended.UnsafeCopyNode.intrinsify(b, targetMethod, arg0, arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private UnsafeCopyNode_copy(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new UnsafeCopyNode_copy(injection), org.graalvm.compiler.nodes.extended.UnsafeCopyNode.class, "copy", java.lang.Object.class, long.class, java.lang.Object.class, long.class, jdk.vm.ci.meta.JavaKind.class, jdk.internal.vm.compiler.word.LocationIdentity.class);
    }
}

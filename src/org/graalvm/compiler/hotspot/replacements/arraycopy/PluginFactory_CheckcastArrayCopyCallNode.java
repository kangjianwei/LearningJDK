// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.hotspot.replacements.arraycopy;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

import jdk.vm.ci.meta.JavaKind;

public class PluginFactory_CheckcastArrayCopyCallNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.replacements.arraycopy.CheckcastArrayCopyCallNode
    //       method: checkcastArraycopy(java.lang.Object,int,java.lang.Object,int,int,org.graalvm.compiler.word.Word,java.lang.Object,boolean)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class CheckcastArrayCopyCallNode_checkcastArraycopy extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider arg0 = injectedHotSpotGraalRuntimeProvider;
            ValueNode arg1 = args[0];
            ValueNode arg2 = args[1];
            ValueNode arg3 = args[2];
            ValueNode arg4 = args[3];
            ValueNode arg5 = args[4];
            ValueNode arg6 = args[5];
            ValueNode arg7 = args[6];
            boolean arg8;
            if (args[7].isConstant()) {
                arg8 = args[7].asJavaConstant().asInt() != 0;
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.hotspot.replacements.arraycopy.CheckcastArrayCopyCallNode node = new org.graalvm.compiler.hotspot.replacements.arraycopy.CheckcastArrayCopyCallNode(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
            b.addPush(JavaKind.Int, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider injectedHotSpotGraalRuntimeProvider;

        private CheckcastArrayCopyCallNode_checkcastArraycopy(InjectionProvider injection) {
            this.injectedHotSpotGraalRuntimeProvider = injection.getInjectedArgument(org.graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new CheckcastArrayCopyCallNode_checkcastArraycopy(injection), org.graalvm.compiler.hotspot.replacements.arraycopy.CheckcastArrayCopyCallNode.class, "checkcastArraycopy", java.lang.Object.class, int.class, java.lang.Object.class, int.class, int.class, org.graalvm.compiler.word.Word.class, java.lang.Object.class, boolean.class);
    }
}

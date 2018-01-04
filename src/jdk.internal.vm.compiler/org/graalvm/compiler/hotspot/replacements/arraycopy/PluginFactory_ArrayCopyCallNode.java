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

public class PluginFactory_ArrayCopyCallNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.replacements.arraycopy.ArrayCopyCallNode
    //       method: arraycopy(java.lang.Object,int,java.lang.Object,int,int,jdk.vm.ci.meta.JavaKind,jdk.internal.vm.compiler.word.LocationIdentity,boolean,boolean,boolean)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class ArrayCopyCallNode_arraycopy_Object_int_Object_int_int_JavaKind_LocationIdentity_boolean_boolean_boolean extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider arg0 = injectedHotSpotGraalRuntimeProvider;
            ValueNode arg1 = args[0];
            ValueNode arg2 = args[1];
            ValueNode arg3 = args[2];
            ValueNode arg4 = args[3];
            ValueNode arg5 = args[4];
            jdk.vm.ci.meta.JavaKind arg6;
            if (args[5].isConstant()) {
                arg6 = snippetReflection.asObject(jdk.vm.ci.meta.JavaKind.class, args[5].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            jdk.internal.vm.compiler.word.LocationIdentity arg7;
            if (args[6].isConstant()) {
                arg7 = snippetReflection.asObject(jdk.internal.vm.compiler.word.LocationIdentity.class, args[6].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            boolean arg8;
            if (args[7].isConstant()) {
                arg8 = args[7].asJavaConstant().asInt() != 0;
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            boolean arg9;
            if (args[8].isConstant()) {
                arg9 = args[8].asJavaConstant().asInt() != 0;
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            boolean arg10;
            if (args[9].isConstant()) {
                arg10 = args[9].asJavaConstant().asInt() != 0;
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.hotspot.replacements.arraycopy.ArrayCopyCallNode node = new org.graalvm.compiler.hotspot.replacements.arraycopy.ArrayCopyCallNode(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider injectedHotSpotGraalRuntimeProvider;
        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private ArrayCopyCallNode_arraycopy_Object_int_Object_int_int_JavaKind_LocationIdentity_boolean_boolean_boolean(InjectionProvider injection) {
            this.injectedHotSpotGraalRuntimeProvider = injection.getInjectedArgument(org.graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.arraycopy.ArrayCopyCallNode
    //       method: arraycopy(java.lang.Object,int,java.lang.Object,int,int,jdk.vm.ci.meta.JavaKind,boolean,boolean,boolean)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class ArrayCopyCallNode_arraycopy_Object_int_Object_int_int_JavaKind_boolean_boolean_boolean extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider arg0 = injectedHotSpotGraalRuntimeProvider;
            ValueNode arg1 = args[0];
            ValueNode arg2 = args[1];
            ValueNode arg3 = args[2];
            ValueNode arg4 = args[3];
            ValueNode arg5 = args[4];
            jdk.vm.ci.meta.JavaKind arg6;
            if (args[5].isConstant()) {
                arg6 = snippetReflection.asObject(jdk.vm.ci.meta.JavaKind.class, args[5].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            boolean arg7;
            if (args[6].isConstant()) {
                arg7 = args[6].asJavaConstant().asInt() != 0;
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            boolean arg8;
            if (args[7].isConstant()) {
                arg8 = args[7].asJavaConstant().asInt() != 0;
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            boolean arg9;
            if (args[8].isConstant()) {
                arg9 = args[8].asJavaConstant().asInt() != 0;
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.hotspot.replacements.arraycopy.ArrayCopyCallNode node = new org.graalvm.compiler.hotspot.replacements.arraycopy.ArrayCopyCallNode(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider injectedHotSpotGraalRuntimeProvider;
        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private ArrayCopyCallNode_arraycopy_Object_int_Object_int_int_JavaKind_boolean_boolean_boolean(InjectionProvider injection) {
            this.injectedHotSpotGraalRuntimeProvider = injection.getInjectedArgument(org.graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new ArrayCopyCallNode_arraycopy_Object_int_Object_int_int_JavaKind_LocationIdentity_boolean_boolean_boolean(injection), org.graalvm.compiler.hotspot.replacements.arraycopy.ArrayCopyCallNode.class, "arraycopy", java.lang.Object.class, int.class, java.lang.Object.class, int.class, int.class, jdk.vm.ci.meta.JavaKind.class, jdk.internal.vm.compiler.word.LocationIdentity.class, boolean.class, boolean.class, boolean.class);
        plugins.register(new ArrayCopyCallNode_arraycopy_Object_int_Object_int_int_JavaKind_boolean_boolean_boolean(injection), org.graalvm.compiler.hotspot.replacements.arraycopy.ArrayCopyCallNode.class, "arraycopy", java.lang.Object.class, int.class, java.lang.Object.class, int.class, int.class, jdk.vm.ci.meta.JavaKind.class, boolean.class, boolean.class, boolean.class);
    }
}

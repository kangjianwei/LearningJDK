/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javax.lang.model.element;

import java.util.List;

/**
 * Represents a module program element.  Provides access to
 * information about the module, its directives, and its members.
 *
 * @jls 7.7 Module Declarations
 * @spec JPMS
 * @see javax.lang.model.util.Elements#getModuleOf
 * @since 9
 */
// 模块元素
public interface ModuleElement extends Element, QualifiedNameable {
    
    /**
     * Returns the fully qualified name of this module.  For an
     * {@linkplain #isUnnamed() unnamed module}, an empty name is returned.
     *
     * @return the fully qualified name of this module, or an
     * empty name if this is an unnamed module
     *
     * @apiNote If the module name consists of one identifier, then
     * this method returns that identifier, which is deemed to be
     * module's fully qualified name despite not being in qualified
     * form.  If the module name consists of more than one identifier,
     * then this method returns the entire name.
     * @jls
     * 6.2 Names and Identifiers
     */
    // 返回模块的全限定名称
    @Override
    Name getQualifiedName();
    
    /**
     * Returns the simple name of this module.  For an {@linkplain
     * #isUnnamed() unnamed module}, an empty name is returned.
     *
     * @return the simple name of this module or an empty name if
     * this is an unnamed module
     *
     * @apiNote If the module name consists of one identifier, then
     * this method returns that identifier.  If the module name
     * consists of more than one identifier, then this method returns
     * the rightmost such identifier, which is deemed to be the
     * module's simple name.
     * @jls 6.2 Names and Identifiers
     */
    // 返回模块的简单名称
    @Override
    Name getSimpleName();
    
    /**
     * Returns {@code true} if this is an open module and {@code
     * false} otherwise.
     *
     * @return {@code true} if this is an open module and {@code
     * false} otherwise
     */
    // 是否为open模块
    boolean isOpen();
    
    /**
     * Returns {@code true} if this is an unnamed module and {@code
     * false} otherwise.
     *
     * @return {@code true} if this is an unnamed module and {@code
     * false} otherwise
     *
     * @jls 7.7.5 Unnamed Modules
     */
    // 是否为未命名模块
    boolean isUnnamed();
    
    /**
     * Returns the packages within this module.
     *
     * @return the packages within this module
     */
    // 返回该元素直接包含的包元素
    @Override
    List<? extends Element> getEnclosedElements();
    
    /**
     * Returns {@code null} since a module is not enclosed by another
     * element.
     *
     * @return {@code null}
     */
    // 返回包围该元素的最内层的元素，即包含null
    @Override
    Element getEnclosingElement();
    
    /**
     * Returns the directives contained in the declaration of this module.
     *
     * @return the directives in the declaration of this module
     */
    // 返回此模块声明中包含的指令
    List<? extends Directive> getDirectives();
    
    /**
     * The {@code kind} of a directive.
     *
     * <p>Note that it is possible additional directive kinds will be added
     * to accommodate new, currently unknown, language structures added to
     * future versions of the Java&trade; programming language.
     *
     * @spec JPMS
     * @since 9
     */
    // 模块声明中的指令修饰符
    enum DirectiveKind {
        /** A "requires (static|transitive)* module-name" directive. */
        REQUIRES,   // requires
        /** An "exports package-name [to module-name-list]" directive. */
        EXPORTS,    // exports
        /** An "opens package-name [to module-name-list]" directive. */
        OPENS,      // opens
        /** A "uses service-name" directive. */
        USES,       // uses
        /** A "provides service-name with implementation-name" directive. */
        PROVIDES    // provides
    }
    
    /**
     * Represents a directive within the declaration of this
     * module. The directives of a module declaration configure the
     * module in the Java Platform Module System.
     *
     * @spec JPMS
     * @since 9
     */
    // 模块声明中的指令信息
    interface Directive {
        /**
         * Returns the {@code kind} of this directive.
         *
         * @return the kind of this directive
         */
        DirectiveKind getKind();
        
        /**
         * Applies a visitor to this directive.
         *
         * @param <R> the return type of the visitor's methods
         * @param <P> the type of the additional parameter to the visitor's methods
         * @param v   the visitor operating on this directive
         * @param p   additional parameter to the visitor
         *
         * @return a visitor-specified result
         */
        <R, P> R accept(DirectiveVisitor<R, P> v, P p);
    }
    
    /**
     * A visitor of module directives, in the style of the visitor design
     * pattern.  Classes implementing this interface are used to operate
     * on a directive when the kind of directive is unknown at compile time.
     * When a visitor is passed to a directive's {@link Directive#accept
     * accept} method, the <code>visit<i>Xyz</i></code> method applicable
     * to that directive is invoked.
     *
     * <p> Classes implementing this interface may or may not throw a
     * {@code NullPointerException} if the additional parameter {@code p}
     * is {@code null}; see documentation of the implementing class for
     * details.
     *
     * <p> <b>WARNING:</b> It is possible that methods will be added to
     * this interface to accommodate new, currently unknown, language
     * structures added to future versions of the Java&trade; programming
     * language. Methods to accommodate new language constructs will
     * be added in a source <em>compatible</em> way using
     * <em>default methods</em>.
     *
     * @param <R> the return type of this visitor's methods.  Use {@link
     *            Void} for visitors that do not need to return results.
     * @param <P> the type of the additional parameter to this visitor's
     *            methods.  Use {@code Void} for visitors that do not need an
     *            additional parameter.
     *
     * @spec JPMS
     * @since 9
     */
    // 模块声明指令访问器
    interface DirectiveVisitor<R, P> {
        /**
         * Visits a {@code requires} directive.
         *
         * @param d the directive to visit
         * @param p a visitor-specified parameter
         *
         * @return a visitor-specified result
         */
        // requires指令访问
        R visitRequires(RequiresDirective d, P p);
        
        /**
         * Visits an {@code exports} directive.
         *
         * @param d the directive to visit
         * @param p a visitor-specified parameter
         *
         * @return a visitor-specified result
         */
        // exports指令访问
        R visitExports(ExportsDirective d, P p);
        
        /**
         * Visits an {@code opens} directive.
         *
         * @param d the directive to visit
         * @param p a visitor-specified parameter
         *
         * @return a visitor-specified result
         */
        // opens指令访问
        R visitOpens(OpensDirective d, P p);
        
        /**
         * Visits a {@code provides} directive.
         *
         * @param d the directive to visit
         * @param p a visitor-specified parameter
         *
         * @return a visitor-specified result
         */
        // provides指令
        R visitProvides(ProvidesDirective d, P p);
        
        /**
         * Visits a {@code uses} directive.
         *
         * @param d the directive to visit
         * @param p a visitor-specified parameter
         *
         * @return a visitor-specified result
         */
        // uses指令
        R visitUses(UsesDirective d, P p);
        
        /**
         * Visits any directive as if by passing itself to that
         * directive's {@link Directive#accept accept} method and passing
         * {@code null} for the additional parameter.
         *
         * @param d the directive to visit
         *
         * @return a visitor-specified result
         *
         * @implSpec The default implementation is {@code d.accept(v, null)}.
         */
        default R visit(Directive d) {
            return d.accept(this, null);
        }
        
        /**
         * Visits any directive as if by passing itself to that
         * directive's {@link Directive#accept accept} method.
         *
         * @param d the directive to visit
         * @param p a visitor-specified parameter
         *
         * @return a visitor-specified result
         *
         * @implSpec The default implementation is {@code d.accept(v, p)}.
         */
        default R visit(Directive d, P p) {
            return d.accept(this, p);
        }
        
        /**
         * Visits an unknown directive.
         * This can occur if the language evolves and new kinds of directive are added.
         *
         * @param d the directive to visit
         * @param p a visitor-specified parameter
         *
         * @return a visitor-specified result
         *
         * @throws UnknownDirectiveException a visitor implementation may optionally throw this exception
         * @implSpec The default implementation throws {@code new UnknownDirectiveException(d, p)}.
         */
        // 未知指令
        default R visitUnknown(Directive d, P p) {
            throw new UnknownDirectiveException(d, p);
        }
    }
    
    /**
     * A dependency of a module.
     *
     * @spec JPMS
     * @since 9
     */
    // requires指令访问器
    interface RequiresDirective extends Directive {
        /**
         * Returns whether or not this is a static dependency.
         *
         * @return whether or not this is a static dependency
         */
        // 是否为static请求
        boolean isStatic();
        
        /**
         * Returns whether or not this is a transitive dependency.
         *
         * @return whether or not this is a transitive dependency
         */
        // 是否为transitive请求
        boolean isTransitive();
        
        /**
         * Returns the module that is required
         *
         * @return the module that is required
         */
        // 获取依赖模块
        ModuleElement getDependency();
    }
    
    /**
     * An exported package of a module.
     *
     * @spec JPMS
     * @since 9
     */
    // exports指令访问器
    interface ExportsDirective extends Directive {
        
        /**
         * Returns the package being exported.
         *
         * @return the package being exported
         */
        // 获取导出的包
        PackageElement getPackage();
        
        /**
         * Returns the specific modules to which the package is being exported,
         * or null, if the package is exported to all modules which
         * have readability to this module.
         *
         * @return the specific modules to which the package is being exported
         */
        // 获取目标模块
        List<? extends ModuleElement> getTargetModules();
    }
    
    /**
     * An opened package of a module.
     *
     * @spec JPMS
     * @since 9
     */
    // opens指令访问器
    interface OpensDirective extends Directive {
        
        /**
         * Returns the package being opened.
         *
         * @return the package being opened
         */
        // 获取开放的包
        PackageElement getPackage();
        
        /**
         * Returns the specific modules to which the package is being open
         * or null, if the package is open all modules which
         * have readability to this module.
         *
         * @return the specific modules to which the package is being opened
         */
        // 获取目标模块
        List<? extends ModuleElement> getTargetModules();
    }
    
    /**
     * An implementation of a service provided by a module.
     *
     * @spec JPMS
     * @since 9
     */
    // provides指令访问器
    interface ProvidesDirective extends Directive {
        /**
         * Returns the service being provided.
         *
         * @return the service being provided
         */
        // 返回提供的服务接口
        TypeElement getService();
        
        /**
         * Returns the implementations of the service being provided.
         *
         * @return the implementations of the service being provided
         */
        // 返回服务实现类
        List<? extends TypeElement> getImplementations();
    }
    
    /**
     * A reference to a service used by a module.
     *
     * @spec JPMS
     * @since 9
     */
    // uses指令访问器
    interface UsesDirective extends Directive {
        /**
         * Returns the service that is used.
         *
         * @return the service that is used
         */
        // 返回使用的服务接口
        TypeElement getService();
    }
}

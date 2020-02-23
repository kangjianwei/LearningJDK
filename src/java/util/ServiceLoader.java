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

package java.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.internal.loader.BootLoader;
import jdk.internal.loader.ClassLoaders;
import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.VM;
import jdk.internal.module.ServicesCatalog;
import jdk.internal.module.ServicesCatalog.ServiceProvider;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;

/**
 * A facility to load implementations of a service.
 *
 * <p> A <i>service</i> is a well-known interface or class for which zero, one,
 * or many service providers exist. A <i>service provider</i> (or just
 * <i>provider</i>) is a class that implements or subclasses the well-known
 * interface or class. A {@code ServiceLoader} is an object that locates and
 * loads service providers deployed in the run time environment at a time of an
 * application's choosing. Application code refers only to the service, not to
 * service providers, and is assumed to be capable of differentiating between
 * multiple service providers as well as handling the possibility that no service
 * providers are located.
 *
 * <h3> Obtaining a service loader </h3>
 *
 * <p> An application obtains a service loader for a given service by invoking
 * one of the static {@code load} methods of ServiceLoader. If the application
 * is a module, then its module declaration must have a <i>uses</i> directive
 * that specifies the service; this helps to locate providers and ensure they
 * will execute reliably. In addition, if the service is not in the application
 * module, then the module declaration must have a <i>requires</i> directive
 * that specifies the module which exports the service.
 *
 * <p> A service loader can be used to locate and instantiate providers of the
 * service by means of the {@link #iterator() iterator} method. {@code ServiceLoader}
 * also defines the {@link #stream() stream} method to obtain a stream of providers
 * that can be inspected and filtered without instantiating them.
 *
 * <p> As an example, suppose the service is {@code com.example.CodecFactory}, an
 * interface that defines methods for producing encoders and decoders:
 *
 * <pre>{@code
 *     package com.example;
 *     public interface CodecFactory {
 *         Encoder getEncoder(String encodingName);
 *         Decoder getDecoder(String encodingName);
 *     }
 * }</pre>
 *
 * <p> The following code obtains a service loader for the {@code CodecFactory}
 * service, then uses its iterator (created automatically by the enhanced-for
 * loop) to yield instances of the service providers that are located:
 *
 * <pre>{@code
 *     ServiceLoader<CodecFactory> loader = ServiceLoader.load(CodecFactory.class);
 *     for (CodecFactory factory : loader) {
 *         Encoder enc = factory.getEncoder("PNG");
 *         if (enc != null)
 *             ... use enc to encode a PNG file
 *             break;
 *         }
 * }</pre>
 *
 * <p> If this code resides in a module, then in order to refer to the
 * {@code com.example.CodecFactory} interface, the module declaration would
 * require the module which exports the interface. The module declaration would
 * also specify use of {@code com.example.CodecFactory}:
 * <pre>{@code
 *     requires com.example.codec.core;
 *     uses com.example.CodecFactory;
 * }</pre>
 *
 * <p> Sometimes an application may wish to inspect a service provider before
 * instantiating it, in order to determine if an instance of that service
 * provider would be useful. For example, a service provider for {@code
 * CodecFactory} that is capable of producing a "PNG" encoder may be annotated
 * with {@code @PNG}. The following code uses service loader's {@code stream}
 * method to yield instances of {@code Provider<CodecFactory>} in contrast to
 * how the iterator yields instances of {@code CodecFactory}:
 * <pre>{@code
 *     ServiceLoader<CodecFactory> loader = ServiceLoader.load(CodecFactory.class);
 *     Set<CodecFactory> pngFactories = loader
 *            .stream()                                              // Note a below
 *            .filter(p -> p.type().isAnnotationPresent(PNG.class))  // Note b
 *            .map(Provider::get)                                    // Note c
 *            .collect(Collectors.toSet());
 * }</pre>
 * <ol type="a">
 * <li> A stream of {@code Provider<CodecFactory>} objects </li>
 * <li> {@code p.type()} yields a {@code Class<CodecFactory>} </li>
 * <li> {@code get()} yields an instance of {@code CodecFactory} </li>
 * </ol>
 *
 * <h3> Designing services </h3>
 *
 * <p> A service is a single type, usually an interface or abstract class. A
 * concrete class can be used, but this is not recommended. The type may have
 * any accessibility. The methods of a service are highly domain-specific, so
 * this API specification cannot give concrete advice about their form or
 * function. However, there are two general guidelines:
 * <ol>
 * <li><p> A service should declare as many methods as needed to allow service
 * providers to communicate their domain-specific properties and other
 * quality-of-implementation factors. An application which obtains a service
 * loader for the service may then invoke these methods on each instance of
 * a service provider, in order to choose the best provider for the
 * application. </p></li>
 * <li><p> A service should express whether its service providers are intended
 * to be direct implementations of the service or to be an indirection
 * mechanism such as a "proxy" or a "factory". Service providers tend to be
 * indirection mechanisms when domain-specific objects are relatively
 * expensive to instantiate; in this case, the service should be designed
 * so that service providers are abstractions which create the "real"
 * implementation on demand. For example, the {@code CodecFactory} service
 * expresses through its name that its service providers are factories
 * for codecs, rather than codecs themselves, because it may be expensive
 * or complicated to produce certain codecs. </p></li>
 * </ol>
 *
 * <h3> <a id="developing-service-providers">Developing service providers</a> </h3>
 *
 * <p> A service provider is a single type, usually a concrete class. An
 * interface or abstract class is permitted because it may declare a static
 * provider method, discussed later. The type must be public and must not be
 * an inner class.
 *
 * <p> A service provider and its supporting code may be developed in a module,
 * which is then deployed on the application module path or in a modular
 * image. Alternatively, a service provider and its supporting code may be
 * packaged as a JAR file and deployed on the application class path. The
 * advantage of developing a service provider in a module is that the provider
 * can be fully encapsulated to hide all details of its implementation.
 *
 * <p> An application that obtains a service loader for a given service is
 * indifferent to whether providers of the service are deployed in modules or
 * packaged as JAR files. The application instantiates service providers via
 * the service loader's iterator, or via {@link Provider Provider} objects in
 * the service loader's stream, without knowledge of the service providers'
 * locations.
 *
 * <h3> Deploying service providers as modules </h3>
 *
 * <p> A service provider that is developed in a module must be specified in a
 * <i>provides</i> directive in the module declaration. The provides directive
 * specifies both the service and the service provider; this helps to locate the
 * provider when another module, with a <i>uses</i> directive for the service,
 * obtains a service loader for the service. It is strongly recommended that the
 * module does not export the package containing the service provider. There is
 * no support for a module specifying, in a <i>provides</i> directive, a service
 * provider in another module.
 *
 * <p> A service provider that is developed in a module has no control over when
 * it is instantiated, since that occurs at the behest of the application, but it
 * does have control over how it is instantiated:
 *
 * <ul>
 *
 * <li> If the service provider declares a provider method, then the service
 * loader invokes that method to obtain an instance of the service provider. A
 * provider method is a public static method named "provider" with no formal
 * parameters and a return type that is assignable to the service's interface
 * or class.
 * <p> In this case, the service provider itself need not be assignable to the
 * service's interface or class. </li>
 *
 * <li> If the service provider does not declare a provider method, then the
 * service provider is instantiated directly, via its provider constructor. A
 * provider constructor is a public constructor with no formal parameters.
 * <p> In this case, the service provider must be assignable to the service's
 * interface or class </li>
 *
 * </ul>
 *
 * <p> A service provider that is deployed as an
 * {@linkplain java.lang.module.ModuleDescriptor#isAutomatic automatic module} on
 * the application module path must have a provider constructor. There is no
 * support for a provider method in this case.
 *
 * <p> As an example, suppose a module specifies the following directives:
 * <pre>{@code
 *     provides com.example.CodecFactory with com.example.impl.StandardCodecs;
 *     provides com.example.CodecFactory with com.example.impl.ExtendedCodecsFactory;
 * }</pre>
 *
 * <p> where
 *
 * <ul>
 * <li> {@code com.example.CodecFactory} is the two-method service from
 * earlier. </li>
 *
 * <li> {@code com.example.impl.StandardCodecs} is a public class that implements
 * {@code CodecFactory} and has a public no-args constructor. </li>
 *
 * <li> {@code com.example.impl.ExtendedCodecsFactory} is a public class that
 * does not implement CodecFactory, but it declares a public static no-args
 * method named "provider" with a return type of {@code CodecFactory}. </li>
 * </ul>
 *
 * <p> A service loader will instantiate {@code StandardCodecs} via its
 * constructor, and will instantiate {@code ExtendedCodecsFactory} by invoking
 * its {@code provider} method. The requirement that the provider constructor or
 * provider method is public helps to document the intent that the class (that is,
 * the service provider) will be instantiated by an entity (that is, a service
 * loader) which is outside the class's package.
 *
 * <h3> Deploying service providers on the class path </h3>
 *
 * A service provider that is packaged as a JAR file for the class path is
 * identified by placing a <i>provider-configuration file</i> in the resource
 * directory {@code META-INF/services}. The name of the provider-configuration
 * file is the fully qualified binary name of the service. The provider-configuration
 * file contains a list of fully qualified binary names of service providers, one
 * per line.
 *
 * <p> For example, suppose the service provider
 * {@code com.example.impl.StandardCodecs} is packaged in a JAR file for the
 * class path. The JAR file will contain a provider-configuration file named:
 *
 * <blockquote>{@code
 * META-INF/services/com.example.CodecFactory
 * }</blockquote>
 *
 * that contains the line:
 *
 * <blockquote>{@code
 * com.example.impl.StandardCodecs # Standard codecs
 * }</blockquote>
 *
 * <p><a id="format">The provider-configuration file must be encoded in UTF-8. </a>
 * Space and tab characters surrounding each service provider's name, as well as
 * blank lines, are ignored. The comment character is {@code '#'}
 * ({@code '&#92;u0023'} <span style="font-size:smaller;">NUMBER SIGN</span>);
 * on each line all characters following the first comment character are ignored.
 * If a service provider class name is listed more than once in a
 * provider-configuration file then the duplicate is ignored. If a service
 * provider class is named in more than one configuration file then the duplicate
 * is ignored.
 *
 * <p> A service provider that is mentioned in a provider-configuration file may
 * be located in the same JAR file as the provider-configuration file or in a
 * different JAR file. The service provider must be visible from the class loader
 * that is initially queried to locate the provider-configuration file; this is
 * not necessarily the class loader which ultimately locates the
 * provider-configuration file.
 *
 * <h3> Timing of provider discovery </h3>
 *
 * <p> Service providers are loaded and instantiated lazily, that is, on demand.
 * A service loader maintains a cache of the providers that have been loaded so
 * far. Each invocation of the {@code iterator} method returns an {@code Iterator}
 * that first yields all of the elements cached from previous iteration, in
 * instantiation order, and then lazily locates and instantiates any remaining
 * providers, adding each one to the cache in turn. Similarly, each invocation
 * of the stream method returns a {@code Stream} that first processes all
 * providers loaded by previous stream operations, in load order, and then lazily
 * locates any remaining providers. Caches are cleared via the {@link #reload
 * reload} method.
 *
 * <h3> <a id="errors">Errors</a> </h3>
 *
 * <p> When using the service loader's {@code iterator}, the {@link
 * Iterator#hasNext() hasNext} and {@link Iterator#next() next} methods will
 * fail with {@link ServiceConfigurationError} if an error occurs locating,
 * loading or instantiating a service provider. When processing the service
 * loader's stream then {@code ServiceConfigurationError} may be thrown by any
 * method that causes a service provider to be located or loaded.
 *
 * <p> When loading or instantiating a service provider in a module, {@code
 * ServiceConfigurationError} can be thrown for the following reasons:
 *
 * <ul>
 *
 * <li> The service provider cannot be loaded. </li>
 *
 * <li> The service provider does not declare a provider method, and either
 * it is not assignable to the service's interface/class or does not have a
 * provider constructor. </li>
 *
 * <li> The service provider declares a public static no-args method named
 * "provider" with a return type that is not assignable to the service's
 * interface or class. </li>
 *
 * <li> The service provider class file has more than one public static
 * no-args method named "{@code provider}". </li>
 *
 * <li> The service provider declares a provider method and it fails by
 * returning {@code null} or throwing an exception. </li>
 *
 * <li> The service provider does not declare a provider method, and its
 * provider constructor fails by throwing an exception. </li>
 *
 * </ul>
 *
 * <p> When reading a provider-configuration file, or loading or instantiating
 * a provider class named in a provider-configuration file, then {@code
 * ServiceConfigurationError} can be thrown for the following reasons:
 *
 * <ul>
 *
 * <li> The format of the provider-configuration file violates the <a
 * href="ServiceLoader.html#format">format</a> specified above; </li>
 *
 * <li> An {@link IOException IOException} occurs while reading the
 * provider-configuration file; </li>
 *
 * <li> A service provider cannot be loaded; </li>
 *
 * <li> A service provider is not assignable to the service's interface or
 * class, or does not define a provider constructor, or cannot be
 * instantiated. </li>
 *
 * </ul>
 *
 * <h3> Security </h3>
 *
 * <p> Service loaders always execute in the security context of the caller
 * of the iterator or stream methods and may also be restricted by the security
 * context of the caller that created the service loader.
 * Trusted system code should typically invoke the methods in this class, and
 * the methods of the iterators which they return, from within a privileged
 * security context.
 *
 * <h3> Concurrency </h3>
 *
 * <p> Instances of this class are not safe for use by multiple concurrent
 * threads.
 *
 * <h3> Null handling </h3>
 *
 * <p> Unless otherwise specified, passing a {@code null} argument to any
 * method in this class will cause a {@link NullPointerException} to be thrown.
 *
 * @param <S> The type of the service to be loaded by this loader
 *
 * @author Mark Reinhold
 * @revised 9
 * @spec JPMS
 * @since 1.6
 */
/*
 * 服务加载器，加载系统中已注册的指定类型的服务
 *
 * 在非模块化系统中，需要在项目根目录的/META-INF/services文件夹下放置注册文件
 * 在模块化系统中，只需要在module-info中填写provides和use信息就可以了
 */
public final class ServiceLoader<S> implements Iterable<S> {
    
    private static JavaLangAccess LANG_ACCESS;  // java.lang包中的后门
    
    // The class of the service type
    private final String serviceName;   // 当前服务加载器将要加载的服务名称（服务接口名称）
    
    // The class or interface representing the service being loaded
    private final Class<S> service; // 当前服务加载器将要加载的服务类型（服务接口）
    
    // The module layer used to locate providers; null when locating providers using a class loader
    private final ModuleLayer layer;    // 用于定位服务提供者的layer，如果使用类加载器搜索，则此项可以设置为null
    
    // The class loader used to locate, load, and instantiate providers; null when locating provider using a module layer
    private final ClassLoader loader;   // 用来加载服务接口的类加载器
    
    private final List<S> instantiatedProviders = new ArrayList<>();    // 缓存已经加载过的服务提供者对象
    
    // The lazy-lookup iterator for iterator operations
    private Iterator<Provider<S>> lookupIterator1;  // 服务查询器，用来查找服务提供者工厂，用在普通迭代操作中
    
    // The lazy-lookup iterator for stream operations
    private Iterator<Provider<S>> lookupIterator2;  // 服务查询器，用来查找服务提供者工厂，用在流式操作中
    
    private final List<Provider<S>> loadedProviders = new ArrayList<>();    // 在流式操作中缓存查找到的服务提供者工厂
    
    // true when all providers loaded
    private boolean loadedAllProviders; // 记录在流式操作中是否已加载所有服务提供者工厂
    
    // Incremented when reload is called
    private int reloadCount;    // 记录服务加载器的重置次数
    
    // The access control context taken when the ServiceLoader is created
    private final AccessControlContext acc;
    
    
    static {
        LANG_ACCESS = SharedSecrets.getJavaLangAccess();
    }
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initializes a new instance of this class for locating service providers
     * in a module layer.
     *
     * @throws ServiceConfigurationError If {@code svc} is not accessible to {@code caller} or the caller
     *                                   module does not use the service type.
     */
    private ServiceLoader(Class<?> caller, ModuleLayer layer, Class<S> service) {
        Objects.requireNonNull(caller);
        Objects.requireNonNull(layer);
        Objects.requireNonNull(service);
        
        // 确保服务的使用者caller可以使用服务service
        checkCaller(caller, service);
        
        this.service = service;
        this.serviceName = service.getName();
        this.layer = layer;
        this.loader = null;
        this.acc = (System.getSecurityManager() != null) ? AccessController.getContext() : null;
    }
    
    /**
     * Initializes a new instance of this class for locating service providers
     * via a class loader.
     *
     * @throws ServiceConfigurationError If {@code svc} is not accessible to {@code caller} or the caller
     *                                   module does not use the service type.
     */
    private ServiceLoader(Class<?> caller, Class<S> service, ClassLoader cl) {
        Objects.requireNonNull(service);
        
        // 虚拟机是否已经完成初始化
        if(VM.isBooted()) {
            // 确保服务的使用者caller可以使用服务service
            checkCaller(caller, service);
            if(cl == null) {
                cl = ClassLoader.getSystemClassLoader();
            }
        } else {
            /*
             * if we get here then it means that ServiceLoader is being used before the VM initialization has completed.
             * At this point then only code in the java.base should be executing.
             */
            // 虚拟机初始化期间只允许java.base中的代码执行
            Module callerModule = caller.getModule();
            Module base = Object.class.getModule();
            Module svcModule = service.getModule();
            if(callerModule != base || svcModule != base) {
                fail(service, "not accessible to " + callerModule + " during VM init");
            }
            
            // restricted to boot loader during startup
            cl = null;
        }
        
        this.service = service;
        this.serviceName = service.getName();
        this.layer = null;
        this.loader = cl;
        this.acc = (System.getSecurityManager() != null) ? AccessController.getContext() : null;
    }
    
    /**
     * Initializes a new instance of this class for locating service providers
     * via a class loader.
     *
     * @throws ServiceConfigurationError If the caller module does not use the service type.
     * @apiNote For use by ResourceBundle
     */
    private ServiceLoader(Module callerModule, Class<S> service, ClassLoader cl) {
        // callerModule需要对service服务声明uses权限
        if(!callerModule.canUse(service)) {
            fail(service, callerModule + " does not declare `uses`");
        }
        
        this.service = Objects.requireNonNull(service);
        this.serviceName = service.getName();
        this.layer = null;
        this.loader = cl;
        this.acc = (System.getSecurityManager() != null) ? AccessController.getContext() : null;
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 加载服务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new service loader for the given service type, using the
     * current thread's {@linkplain java.lang.Thread#getContextClassLoader
     * context class loader}.
     *
     * <p> An invocation of this convenience method of the form
     * <pre>{@code
     *     ServiceLoader.load(service)
     * }</pre>
     *
     * is equivalent to
     *
     * <pre>{@code
     *     ServiceLoader.load(service, Thread.currentThread().getContextClassLoader())
     * }</pre>
     *
     * @param <S>     the class of the service type
     * @param service The interface or abstract class representing the service
     *
     * @return A new service loader
     *
     * @throws ServiceConfigurationError if the service type is not accessible to the caller or the
     *                                   caller is in an explicit module and its module descriptor does
     *                                   not declare that it uses {@code service}
     * @apiNote Service loader objects obtained with this method should not be
     * cached VM-wide. For example, different applications in the same VM may
     * have different thread context class loaders. A lookup by one application
     * may locate a service provider that is only visible via its thread
     * context class loader and so is not suitable to be located by the other
     * application. Memory leaks can also arise. A thread local may be suited
     * to some applications.
     * @revised 9
     * @spec JPMS
     */
    // 使用当前线程上下文类加载器加载指定的服务
    @CallerSensitive
    public static <S> ServiceLoader<S> load(Class<S> service) {
        // 获取当前线程上下文类加载器
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
    
        // 获取load()的调用者所在的类
        Class<?> caller = Reflection.getCallerClass();
    
        return new ServiceLoader<>(caller, service, cl);
    }
    
    /**
     * Creates a new service loader for the given service. The service loader
     * uses the given class loader as the starting point to locate service
     * providers for the service. The service loader's {@link #iterator()
     * iterator} and {@link #stream() stream} locate providers in both named
     * and unnamed modules, as follows:
     *
     * <ul>
     * <li> <p> Step 1: Locate providers in named modules. </p>
     *
     * <p> Service providers are located in all named modules of the class
     * loader or to any class loader reachable via parent delegation. </p>
     *
     * <p> In addition, if the class loader is not the bootstrap or {@linkplain
     * ClassLoader#getPlatformClassLoader() platform class loader}, then service
     * providers may be located in the named modules of other class loaders.
     * Specifically, if the class loader, or any class loader reachable via
     * parent delegation, has a module in a {@linkplain ModuleLayer module
     * layer}, then service providers in all modules in the module layer are
     * located.  </p>
     *
     * <p> For example, suppose there is a module layer where each module is
     * in its own class loader (see {@link ModuleLayer#defineModulesWithManyLoaders
     * defineModulesWithManyLoaders}). If this {@code ServiceLoader.load} method
     * is invoked to locate providers using any of the class loaders created for
     * the module layer, then it will locate all of the providers in the module
     * layer, irrespective of their defining class loader. </p>
     *
     * <p> Ordering: The service loader will first locate any service providers
     * in modules defined to the class loader, then its parent class loader,
     * its parent parent, and so on to the bootstrap class loader. If a class
     * loader has modules in a module layer then all providers in that module
     * layer are located (irrespective of their class loader) before the
     * providers in the parent class loader are located. The ordering of
     * modules in same class loader, or the ordering of modules in a module
     * layer, is not defined. </p>
     *
     * <p> If a module declares more than one provider then the providers
     * are located in the order that its module descriptor {@linkplain
     * java.lang.module.ModuleDescriptor.Provides#providers() lists the
     * providers}. Providers added dynamically by instrumentation agents (see
     * {@link java.lang.instrument.Instrumentation#redefineModule redefineModule})
     * are always located after providers declared by the module. </p> </li>
     *
     * <li> <p> Step 2: Locate providers in unnamed modules. </p>
     *
     * <p> Service providers in unnamed modules are located if their class names
     * are listed in provider-configuration files located by the class loader's
     * {@link ClassLoader#getResources(String) getResources} method. </p>
     *
     * <p> The ordering is based on the order that the class loader's {@code
     * getResources} method finds the service configuration files and within
     * that, the order that the class names are listed in the file. </p>
     *
     * <p> In a provider-configuration file, any mention of a service provider
     * that is deployed in a named module is ignored. This is to avoid
     * duplicates that would otherwise arise when a named module has both a
     * <i>provides</i> directive and a provider-configuration file that mention
     * the same service provider. </p>
     *
     * <p> The provider class must be visible to the class loader. </p> </li>
     *
     * </ul>
     *
     * @param <S>     the class of the service type
     * @param service The interface or abstract class representing the service
     * @param loader  The class loader to be used to load provider-configuration files
     *                and provider classes, or {@code null} if the system class
     *                loader (or, failing that, the bootstrap class loader) is to be
     *                used
     *
     * @return A new service loader
     *
     * @throws ServiceConfigurationError if the service type is not accessible to the caller or the
     *                                   caller is in an explicit module and its module descriptor does
     *                                   not declare that it uses {@code service}
     * @apiNote If the class path of the class loader includes remote network
     * URLs then those URLs may be dereferenced in the process of searching for
     * provider-configuration files.
     *
     * <p> This activity is normal, although it may cause puzzling entries to be
     * created in web-server logs.  If a web server is not configured correctly,
     * however, then this activity may cause the provider-loading algorithm to fail
     * spuriously.
     *
     * <p> A web server should return an HTTP 404 (Not Found) response when a
     * requested resource does not exist.  Sometimes, however, web servers are
     * erroneously configured to return an HTTP 200 (OK) response along with a
     * helpful HTML error page in such cases.  This will cause a {@link
     * ServiceConfigurationError} to be thrown when this class attempts to parse
     * the HTML page as a provider-configuration file.  The best solution to this
     * problem is to fix the misconfigured web server to return the correct
     * response code (HTTP 404) along with the HTML error page.
     * @revised 9
     * @spec JPMS
     */
    // 使用指定的类加载器加载指定的服务
    @CallerSensitive
    public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader) {
        // 获取load()的调用者所在的类
        Class<?> caller = Reflection.getCallerClass();
    
        return new ServiceLoader<>(caller, service, loader);
    }
    
    /**
     * Creates a new service loader for the given service type to load service
     * providers from modules in the given module layer and its ancestors. It
     * does not locate providers in unnamed modules. The ordering that the service
     * loader's {@link #iterator() iterator} and {@link #stream() stream} locate
     * providers and yield elements is as follows:
     *
     * <ul>
     * <li><p> Providers are located in a module layer before locating providers
     * in parent layers. Traversal of parent layers is depth-first with each
     * layer visited at most once. For example, suppose L0 is the boot layer, L1
     * and L2 are modules layers with L0 as their parent. Now suppose that L3 is
     * created with L1 and L2 as the parents (in that order). Using a service
     * loader to locate providers with L3 as the context will locate providers
     * in the following order: L3, L1, L0, L2. </p></li>
     *
     * <li><p> If a module declares more than one provider then the providers
     * are located in the order that its module descriptor
     * {@linkplain java.lang.module.ModuleDescriptor.Provides#providers()
     * lists the providers}. Providers added dynamically by instrumentation
     * agents are always located after providers declared by the module. </p></li>
     *
     * <li><p> The ordering of modules in a module layer is not defined. </p></li>
     * </ul>
     *
     * @param <S>     the class of the service type
     * @param layer   The module layer
     * @param service The interface or abstract class representing the service
     *
     * @return A new service loader
     *
     * @throws ServiceConfigurationError if the service type is not accessible to the caller or the
     *                                   caller is in an explicit module and its module descriptor does
     *                                   not declare that it uses {@code service}
     * @apiNote Unlike the other load methods defined here, the service type
     * is the second parameter. The reason for this is to avoid source
     * compatibility issues for code that uses {@code load(S, null)}.
     * @spec JPMS
     * @since 9
     */
    // 在模块层layer中加载指定的服务；service是表示服务的接口或抽象类
    @CallerSensitive
    public static <S> ServiceLoader<S> load(ModuleLayer layer, Class<S> service) {
        // 获取load()的调用者所在的类
        Class<?> caller = Reflection.getCallerClass();
    
        return new ServiceLoader<>(caller, layer, service);
    }
    
    /**
     * Creates a new service loader for the given service type, using the
     * {@linkplain ClassLoader#getPlatformClassLoader() platform class loader}.
     *
     * <p> This convenience method is equivalent to: </p>
     *
     * <pre>{@code
     *     ServiceLoader.load(service, ClassLoader.getPlatformClassLoader())
     * }</pre>
     *
     * <p> This method is intended for use when only installed providers are
     * desired.  The resulting service will only find and load providers that
     * have been installed into the current Java virtual machine; providers on
     * the application's module path or class path will be ignored.
     *
     * @param <S>     the class of the service type
     * @param service The interface or abstract class representing the service
     *
     * @return A new service loader
     *
     * @throws ServiceConfigurationError if the service type is not accessible to the caller or the
     *                                   caller is in an explicit module and its module descriptor does
     *                                   not declare that it uses {@code service}
     * @revised 9
     * @spec JPMS
     */
    // 使用platform类加载器加载已安装的服务，通常是扩展服务，不包括类路径和用户在应用中定义的服务
    @CallerSensitive
    public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
        // 获取platform类加载器
        ClassLoader cl = ClassLoader.getPlatformClassLoader();
    
        // 获取load()的调用者所在的类
        Class<?> caller = Reflection.getCallerClass();
    
        return new ServiceLoader<>(caller, service, cl);
    }
    
    /**
     * Creates a new service loader for the given service type, class
     * loader, and caller.
     *
     * @param <S>          the class of the service type
     * @param service      The interface or abstract class representing the service
     * @param loader       The class loader to be used to load provider-configuration files
     *                     and provider classes, or {@code null} if the system class
     *                     loader (or, failing that, the bootstrap class loader) is to be
     *                     used
     * @param callerModule The caller's module for which a new service loader is created
     *
     * @return A new service loader
     */
    // 在callerModule模块中使用loader类加载器加载服务
    static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader, Module callerModule) {
        return new ServiceLoader<>(callerModule, service, loader);
    }
    
    /*▲ 加载服务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取服务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an iterator to lazily load and instantiate the available
     * providers of this loader's service.
     *
     * <p> To achieve laziness the actual work of locating and instantiating
     * providers is done by the iterator itself. Its {@link Iterator#hasNext
     * hasNext} and {@link Iterator#next next} methods can therefore throw a
     * {@link ServiceConfigurationError} for any of the reasons specified in
     * the <a href="#errors">Errors</a> section above. To write robust code it
     * is only necessary to catch {@code ServiceConfigurationError} when using
     * the iterator. If an error is thrown then subsequent invocations of the
     * iterator will make a best effort to locate and instantiate the next
     * available provider, but in general such recovery cannot be guaranteed.
     *
     * <p> Caching: The iterator returned by this method first yields all of
     * the elements of the provider cache, in the order that they were loaded.
     * It then lazily loads and instantiates any remaining service providers,
     * adding each one to the cache in turn. If this loader's provider caches are
     * cleared by invoking the {@link #reload() reload} method then existing
     * iterators for this service loader should be discarded.
     * The {@code  hasNext} and {@code next} methods of the iterator throw {@link
     * java.util.ConcurrentModificationException ConcurrentModificationException}
     * if used after the provider cache has been cleared.
     *
     * <p> The iterator returned by this method does not support removal.
     * Invoking its {@link java.util.Iterator#remove() remove} method will
     * cause an {@link UnsupportedOperationException} to be thrown.
     *
     * @return An iterator that lazily loads providers for this loader's
     * service
     *
     * @apiNote Throwing an error in these cases may seem extreme.  The rationale
     * for this behavior is that a malformed provider-configuration file, like a
     * malformed class file, indicates a serious problem with the way the Java
     * virtual machine is configured or is being used.  As such it is preferable
     * to throw an error rather than try to recover or, even worse, fail silently.
     * @revised 9
     * @spec JPMS
     */
    // 返回服务提供者迭代器
    public Iterator<S> iterator() {
        
        // create lookup iterator if needed
        if(lookupIterator1 == null) {
            // 获取服务查询器，通过该查询器可以查找所有注册的服务提供者的服务提供者工厂
            lookupIterator1 = newLookupIterator();
        }
    
        // 以迭代器形式返回服务提供者查询器，以便查询服务提供者
        return new Iterator<S>() {
            
            /* record reload count */
            // 记录对服务目录缓存的清理次数
            final int expectedReloadCount = ServiceLoader.this.reloadCount;
    
            // index into the cached providers list
            int index = 0;
    
            // 是否存在下一个服务提供者(可能是潜在存在的，比如存在服务提供者工厂)
            @Override
            public boolean hasNext() {
                // 确保服务查询器有效
                checkReloadCount();
    
                // 已遍历的服务索引 < 已实例化的服务索引
                if(index<instantiatedProviders.size()) {
                    return true;
                }
    
                // 是否存在服务提供者工厂
                return lookupIterator1.hasNext();
            }
    
            // 返回下一个服务提供者
            @Override
            public S next() {
                // 确保服务查询器有效
                checkReloadCount();
                
                S next;
                
                if(index<instantiatedProviders.size()) {
                    next = instantiatedProviders.get(index);
                } else {
                    // 获取下一个服务提供者的服务提供者工厂
                    Provider<S> nextProvider = lookupIterator1.next();
    
                    // 创建服务提供者的实例
                    next = nextProvider.get();
    
                    // 对服务提供者进行缓存
                    instantiatedProviders.add(next);
                }
    
                // 统计已加载的服务提供者数量
                index++;
                
                return next;
            }
    
            /**
             * Throws ConcurrentModificationException if the list of cached providers has been cleared by reload.
             */
            // 防止出现同步问题，即在查找某个服务的过程中服务查询器被重置
            private void checkReloadCount() {
                if(ServiceLoader.this.reloadCount != expectedReloadCount) {
                    throw new ConcurrentModificationException();
                }
            }
        };
    }
    
    /**
     * Load the first available service provider of this loader's service. This
     * convenience method is equivalent to invoking the {@link #iterator()
     * iterator()} method and obtaining the first element. It therefore
     * returns the first element from the provider cache if possible, it
     * otherwise attempts to load and instantiate the first provider.
     *
     * <p> The following example loads the first available service provider. If
     * no service providers are located then it uses a default implementation.
     * <pre>{@code
     *    CodecFactory factory = ServiceLoader.load(CodecFactory.class)
     *                                        .findFirst()
     *                                        .orElse(DEFAULT_CODECSET_FACTORY);
     * }</pre>
     *
     * @return The first service provider or empty {@code Optional} if no
     * service providers are located
     *
     * @throws ServiceConfigurationError If a provider class cannot be loaded for any of the reasons
     *                                   specified in the <a href="#errors">Errors</a> section above.
     * @spec JPMS
     * @since 9
     */
    // 获取第一个服务提供者
    public Optional<S> findFirst() {
        Iterator<S> iterator = iterator();
        if(iterator.hasNext()) {
            return Optional.of(iterator.next());
        } else {
            return Optional.empty();
        }
    }
    
    
    /**
     * Returns a stream to lazily load available providers of this loader's
     * service. The stream elements are of type {@link Provider Provider}, the
     * {@code Provider}'s {@link Provider#get() get} method must be invoked to
     * get or instantiate the provider.
     *
     * <p> To achieve laziness the actual work of locating providers is done
     * when processing the stream. If a service provider cannot be loaded for any
     * of the reasons specified in the <a href="#errors">Errors</a> section
     * above then {@link ServiceConfigurationError} is thrown by whatever method
     * caused the service provider to be loaded. </p>
     *
     * <p> Caching: When processing the stream then providers that were previously
     * loaded by stream operations are processed first, in load order. It then
     * lazily loads any remaining service providers. If this loader's provider
     * caches are cleared by invoking the {@link #reload() reload} method then
     * existing streams for this service loader should be discarded. The returned
     * stream's source {@link Spliterator spliterator} is <em>fail-fast</em> and
     * will throw {@link ConcurrentModificationException} if the provider cache
     * has been cleared. </p>
     *
     * <p> The following examples demonstrate usage. The first example creates
     * a stream of {@code CodecFactory} objects, the second example is the same
     * except that it sorts the providers by provider class name (and so locate
     * all providers).
     * <pre>{@code
     *    Stream<CodecFactory> providers = ServiceLoader.load(CodecFactory.class)
     *            .stream()
     *            .map(Provider::get);
     *
     *    Stream<CodecFactory> providers = ServiceLoader.load(CodecFactory.class)
     *            .stream()
     *            .sorted(Comparator.comparing(p -> p.type().getName()))
     *            .map(Provider::get);
     * }</pre>
     *
     * @return A stream that lazily loads providers for this loader's service
     *
     * @spec JPMS
     * @since 9
     */
    // 返回流化的服务提供者工厂
    public Stream<Provider<S>> stream() {
        // use cached providers as the source when all providers loaded
        if(loadedAllProviders) {
            // 获取在流式操作中缓存的服务提供者工厂
            return loadedProviders.stream();
        }
    
        // create lookup iterator if needed
        if(lookupIterator2 == null) {
            // 获取服务查询器，通过该查询器可以查找所有注册的服务提供者的服务提供者工厂
            lookupIterator2 = newLookupIterator();
        }
    
        /* use lookup iterator and cached providers as source */
        // 构造一个可分割迭代器，用来获取流中下一个服务提供者工厂
        Spliterator<Provider<S>> spliterator = new ProviderSpliterator<>(lookupIterator2);
    
        return StreamSupport.stream(spliterator, false);
    }
    
    /*▲ 获取服务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns a new lookup iterator.
     */
    // 返回服务查询器，通过该查询器可以查找所有注册的服务提供者的服务提供者工厂
    private Iterator<Provider<S>> newLookupIterator() {
        assert layer == null || loader == null;
    
        if(layer != null) {
            return new LayerLookupIterator<>();
        }
    
        // 创建命名模块中的服务查询器（会通过懒加载生成服务目录）
        Iterator<Provider<S>> first = new ModuleServicesLookupIterator<>();
    
        // 创建类路径下（未命名模块）的服务查询器（后续通过懒加载创建服务目录）
        Iterator<Provider<S>> second = new LazyClassPathLookupIterator<>();
    
        // 返回构造服务提供者工厂的迭代器
        return new Iterator<Provider<S>>() {
        
            // 判断是否存在未遍历的服务提供者，服务提供者信息是在遍历过程中动态建立的
            @Override
            public boolean hasNext() {
                /*
                 * 首先在命名模块的服务查询器中查找是否存在下一个服务提供者的服务提供者工厂，
                 * 如果找不到，再去类路径下（未命名模块）的服务查询器中查找。
                 */
                if(first.hasNext()) {
                    return true;
                }
            
                /*
                 * 在类路径下（未命名模块）的服务查询器查找下一个服务提供者的服务提供者工厂，
                 * 该查找过程中会懒加载地记录类路径下的所有服务提供者。
                 */
                return second.hasNext();
            }
        
            // 返回下一个服务提供者的服务提供者工厂
            @Override
            public Provider<S> next() {
                // 优先从命名模块中的服务查询器中查找
                if(first.hasNext()) {
                    return first.next();
                
                    // 如果上面找不到，再到类路径下的服务查询器中查找
                } else if(second.hasNext()) {
                    return second.next();
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    
    /**
     * Loads a service provider in a module.
     *
     * Returns {@code null} if the service provider's module doesn't read
     * the module with the service type.
     *
     * @throws ServiceConfigurationError if the class cannot be loaded or
     *                                   isn't the expected sub-type (or doesn't define a provider
     *                                   factory method that returns the expected type)
     */
    // 返回指定服务提供者的服务提供者工厂
    private Provider<S> loadProvider(ServiceProvider provider) {
        // 确保服务提供者工厂所在的模块可以读取到服务所在的模块
        Module module = provider.module();
        if(!module.canRead(service.getModule())) {
            // module does not read the module with the service type
            return null;
        }
    
        // 服务提供者名称
        String providerName = provider.providerName();
    
        // 服务提供者接口/抽象类
        Class<?> providerClass = null;
    
        // 构造服务提供者的类对象
        if(acc == null) {
            try {
                providerClass = Class.forName(module, providerName);
            } catch(LinkageError e) {
                fail(service, "Unable to load " + providerName, e);
            }
        } else {
            // 根据类的全名和所在的Module加载类对象，如果该module下没有该类，返回null
            PrivilegedExceptionAction<Class<?>> pa = () -> Class.forName(module, providerName);
            try {
                // 创建服务提供者的类对象
                providerClass = AccessController.doPrivileged(pa);
            } catch(Throwable x) {
                if(x instanceof PrivilegedActionException) {
                    x = x.getCause();
                }
                fail(service, "Unable to load " + providerName, x);
                return null;
            }
        }
    
        if(providerClass == null) {
            fail(service, "Provider " + providerName + " not found");
        }
    
        // 服务提供者的修饰符必须为public
        int mods = providerClass.getModifiers();
        if(!Modifier.isPublic(mods)) {
            fail(service, providerClass + " is not public");
        }
    
        /* if provider in explicit module then check for static factory method */
        // 如果providerClass所在module为显式命名模块，则检查静态工厂方法
        if(inExplicitModule(providerClass)) {
            /*
             * 获取服务提供者中的静态工厂方法，形如：
             *
             * public static IService provider(){
             *     return new ServiceA();
             * }
             * 该方法必须用public修饰，且方法名必须为provider
             * 不能同时存在多个静态方法
             */
            Method factoryMethod = findStaticProviderMethod(providerClass);
        
            // 如果服务提供者中存在静态工厂方法
            if(factoryMethod != null) {
                // 获取工厂方法的返回值
                Class<?> returnType = factoryMethod.getReturnType();
            
                // 确保该工厂方法的返回值是接口服务类的子类型
                if(!service.isAssignableFrom(returnType)) {
                    fail(service, factoryMethod + " return type not a subtype");
                }
            
                // 使用指定的工厂方法构造服务提供者工厂
                return new ProviderImpl<S>(service, (Class<? extends S>) returnType, factoryMethod, acc);
            }
        }
    
        // 没有工厂方法时，确保服务提供者是服务接口/抽象类的子类型
        if(!service.isAssignableFrom(providerClass)) {
            fail(service, providerClass.getName() + " not a subtype");
        }
    
        @SuppressWarnings("unchecked")
        Class<? extends S> type = (Class<? extends S>) providerClass;
    
        // 获取服务提供者工厂的无参public构造器
        @SuppressWarnings("unchecked")
        Constructor<? extends S> ctor = (Constructor<? extends S>) getConstructor(providerClass);
    
        // 使用无参构造器构造服务提供者工厂对象
        return new ProviderImpl<S>(service, type, ctor, acc);
    }
    
    /**
     * Returns the public no-arg constructor of a class.
     *
     * @throws ServiceConfigurationError if the class does not have public no-arg constructor
     */
    // 返回指定类中的无参public构造器
    private Constructor<?> getConstructor(Class<?> clazz) {
        PrivilegedExceptionAction<Constructor<?>> pa = new PrivilegedExceptionAction<>() {
            @Override
            public Constructor<?> run() throws Exception {
                // 获取无参public构造方法
                Constructor<?> ctor = clazz.getConstructor();
    
                // 确保clazz所在模块为显式命名模块
                if(inExplicitModule(clazz)) {
                    // 设置允许反射访问
                    ctor.setAccessible(true);
                }
                
                return ctor;
            }
        };
        
        Constructor<?> ctor = null;
        try {
            ctor = AccessController.doPrivileged(pa);
        } catch(Throwable x) {
            if(x instanceof PrivilegedActionException) {
                x = x.getCause();
            }
    
            String cn = clazz.getName();
            fail(service, cn + " Unable to get public no-arg constructor", x);
        }
        
        return ctor;
    }
    
    /**
     * Returns the public static "provider" method if found.
     *
     * @throws ServiceConfigurationError if there is an error finding the
     *                                   provider method or there is more than one public static
     *                                   provider method
     */
    /*
     * 返回服务提供者中的静态工厂方法，形如：
     * public static IService provider(){
     *     return new ServiceA();
     * }
     * 该方法必须用public修饰，且方法名必须为provider
     * 不能同时存在多个静态方法
     */
    private Method findStaticProviderMethod(Class<?> clazz) {
        List<Method> methods = null;
    
        try {
            // 通过反射查找工厂方法
            methods = LANG_ACCESS.getDeclaredPublicMethods(clazz, "provider");
        } catch(Throwable x) {
            fail(service, "Unable to get public provider() method", x);
        }
        
        if(methods.isEmpty()) {
            // does not declare a public provider method
            return null;
        }
        
        // locate the static methods, can be at most one
        Method result = null;   // 记录静态工厂方法，只能有一个静态的
        
        // 遍历所有工厂方法
        for(Method method : methods) {
            int mods = method.getModifiers();
            assert Modifier.isPublic(mods);
    
            // 找出静态工厂方法
            if(Modifier.isStatic(mods)) {
                if(result != null) {
                    fail(service, clazz + " declares more than one" + " public static provider() method");
                }
                result = method;
            }
        }
        
        // 如果存在静态方法，禁用安全检查
        if(result != null) {
            Method m = result;
            PrivilegedAction<Void> pa = () -> {
                m.setAccessible(true);
                return null;
            };
    
            AccessController.doPrivileged(pa);
        }
        
        return result;
    }
    
    /**
     * Returns {@code true} if the provider is in an explicit module
     */
    // 判断clazz当前所在module是否为显式命名模块（既不是未命名module，也不是自动module）
    private boolean inExplicitModule(Class<?> clazz) {
        Module module = clazz.getModule();
        return module.isNamed() && !module.getDescriptor().isAutomatic();
    }
    
    /**
     * Clear this loader's provider cache so that all providers will be
     * reloaded.
     *
     * <p> After invoking this method, subsequent invocations of the {@link
     * #iterator() iterator} or {@link #stream() stream} methods will lazily
     * locate providers (and instantiate in the case of {@code iterator})
     * from scratch, just as is done by a newly-created service loader.
     *
     * <p> This method is intended for use in situations in which new service
     * providers can be installed into a running Java virtual machine.
     */
    // 重置服务加载器，清除所有服务缓存
    public void reload() {
        lookupIterator1 = null;
        instantiatedProviders.clear();
        
        lookupIterator2 = null;
        loadedProviders.clear();
        loadedAllProviders = false;
        
        // increment count to allow CME be thrown
        reloadCount++;
    }
    
    /**
     * Checks that the given service type is accessible to types in the given
     * module, and check that the module declares that it uses the service type.
     */
    // 确保服务的使用者caller可以访问服务service
    private static void checkCaller(Class<?> caller, Class<?> service) {
        if(caller == null) {
            fail(service, "no caller to check if it declares `uses`");
        }
        
        // Check access to the service type
        Module callerModule = caller.getModule();
        int modifiers = service.getModifiers();
        
        // 确保可以在caller中显式访问service中具有modifiers修饰符的成员
        if(!Reflection.verifyMemberAccess(caller, service, null, modifiers)) {
            fail(service, "service type not accessible to " + callerModule);
        }
        
        /* If the caller is in a named module then it should "uses" the service type */
        // 负责load服务的模块需要use服务（对未命名模块没有要求）
        if(!callerModule.canUse(service)) {
            fail(service, callerModule + " does not declare `uses`");
        }
    }
    
    private static void fail(Class<?> service, String msg, Throwable cause) throws ServiceConfigurationError {
        throw new ServiceConfigurationError(service.getName() + ": " + msg, cause);
    }
    
    private static void fail(Class<?> service, String msg) throws ServiceConfigurationError {
        throw new ServiceConfigurationError(service.getName() + ": " + msg);
    }
    
    private static void fail(Class<?> service, URL u, int line, String msg) throws ServiceConfigurationError {
        fail(service, u + ":" + line + ": " + msg);
    }
    
    
    /**
     * Returns a string describing this service.
     *
     * @return A descriptive string
     */
    public String toString() {
        return "java.util.ServiceLoader[" + service.getName() + "]";
    }
    
    
    /**
     * Implements lazy service provider lookup of service providers that are provided by modules defined to a class loader
     * or to modules in layers with a module defined to the class loader.
     */
    // 基于命名模块的服务查询器
    private final class ModuleServicesLookupIterator<T> implements Iterator<Provider<T>> {
        ClassLoader currentLoader;          // 加载服务接口的类加载器
        Iterator<ServiceProvider> iterator; // 存储了待加载服务的所有服务提供者的迭代器
        Provider<T> nextProvider;           // 服务提供者工厂，用于构造某个服务提供者对象
        
        ServiceConfigurationError nextError;
        
        ModuleServicesLookupIterator() {
            this.currentLoader = loader;
            
            // 获取一个迭代器，用来遍历待加载服务的所有服务提供者
            this.iterator = iteratorFor(loader);
        }
        
        // 是否存在待加载服务的服务提供者工厂
        @Override
        public boolean hasNext() {
            while(nextProvider == null && nextError == null) {
                // 如果不存在下一个服务提供者，则会尝试向父级类加载器的服务目录中查找
                while(!iterator.hasNext()) {
                    if(currentLoader == null) {
                        return false;   // bootstrap类加载器
                    }
    
                    // 获取父级类加载器
                    currentLoader = currentLoader.getParent();
    
                    // 获取一个迭代器，用来遍历待加载服务的所有服务提供者
                    iterator = iteratorFor(currentLoader);
                }
    
                // 获取下一个服务提供者
                ServiceProvider provider = iterator.next();
    
                try {
                    // 获取指定服务提供者的服务提供者工厂
                    nextProvider = (Provider<T>) loadProvider(provider);
                } catch(ServiceConfigurationError e) {
                    nextError = e;
                }
            }
    
            return true;
        }
        
        // 返回下一个服务提供者工厂
        @Override
        public Provider<T> next() {
            // 如果不包含下一个元素，会抛异常
            if(!hasNext()) {
                throw new NoSuchElementException();
            }
            
            // 获取服务提供者工厂
            Provider<T> provider = nextProvider;
            if(provider != null) {
                nextProvider = null;
                return provider;
            } else {
                ServiceConfigurationError e = nextError;
                assert e != null;
                nextError = null;
                throw e;
            }
        }
        
        /**
         * Returns iterator to iterate over the implementations of {@code service} in the given layer.
         */
        /*
         * 获取待加载服务在layer模块层的服务目录中关联的服务提供者列表
         * 在获取过程中，会动态创建layer模块层的服务目录，这是一种懒加载机制
         *
         * 注：如果该服务接口处于未命名模块，那么此处还无法找到服务提供者列表
         */
        private List<ServiceProvider> providers(ModuleLayer layer) {
            // 获取当前模块层的服务目录，该服务目录中缓存了当前模块层内所有模块的所有服务提供者信息
            ServicesCatalog catalog = LANG_ACCESS.getServicesCatalog(layer);
    
            // 在当前模块层的服务集中查找serviceName对应的服务提供者列表
            return catalog.findServices(serviceName);
        }
        
        /**
         * Returns the class loader that a module is defined to
         */
        // 返回加载指定模块的类加载器
        private ClassLoader loaderFor(Module module) {
            SecurityManager sm = System.getSecurityManager();
            if(sm != null) {
                PrivilegedAction<ClassLoader> pa = module::getClassLoader;
                return AccessController.doPrivileged(pa);
            }
    
            return module.getClassLoader();
        }
        
        /**
         * Returns an iterator to iterate over the implementations of {@code
         * service} in modules defined to the given class loader or in custom
         * layers with a module defined to this class loader.
         */
        // 返回一个迭代器，用来遍历待加载服务的所有服务提供者
        private Iterator<ServiceProvider> iteratorFor(ClassLoader loader) {
            /* modules defined to the class loader */
            // 由当前ClassLoader定义的module或class中的服务目录
            ServicesCatalog catalog;
    
            /*
             * 由于bootstrap class loader是个C++类，所以无法在其中设置一个CLV大本营
             * 因此，这里使用BootLoader来弥补loader是bootstrap class loader的情形
             *
             * 对于app class loader或platform class loader，它们是Java类
             * 在这两种class loader内部可以直接设置一个CLV大本营缓存服务目录
             *
             * 当然，这里还可能出现自定义的类加载器，其行为与app/platform类加载器类似
             */
    
            // 返回loder可以加载到的服务目录，如果不存在则返回null
            if(loader == null) {
                // 处理bootstrap类加载器的情形
                catalog = BootLoader.getServicesCatalog();
            } else {
                // 处理app/platform类加载器或自定义类加载器的情形
                catalog = ServicesCatalog.getServicesCatalogOrNull(loader);
            }
    
            List<ServiceProvider> providers;
    
            if(catalog == null) {
                // 如果没有找到缓存的服务目录，则创建一个空的服务目录
                providers = List.of();
            } else {
                // 如果存在缓存的服务目录，则根据serviceName在服务集中查找服务提供者列表
                providers = catalog.findServices(serviceName);
            }
    
            /* modules in layers that define modules to the class loader */
            ClassLoader platformClassLoader = ClassLoaders.platformClassLoader();
    
            /*
             * 如果loder类加载器是bootstrap/platform类加载器，则可以直接返回了，
             * 因为对这两种类加载器来说，其对应的服务目录在初始化模块系统时已经生成了。
             */
            if(loader == null || loader == platformClassLoader) {
                return providers.iterator();
            }
    
            /* 处理loader是app类加载器或自定义类加载器的情形，则可能需要懒加载 */
    
            // 生成已有服务提供者列表的镜像
            List<ServiceProvider> allProviders = new ArrayList<>(providers);
    
            // 获取一个流，其中包含了loader参与定义过的所有模块层
            Stream<ModuleLayer> layers = LANG_ACCESS.layers(loader);
    
            // 模块层迭代器，用来遍历layers中所有模块层
            Iterator<ModuleLayer> iterator = layers.iterator();
    
            while(iterator.hasNext()) {
                // 获取下一个模块层
                ModuleLayer layer = iterator.next();
        
                // 获取待加载服务在layer模块层的服务目录中关联的服务提供者列表
                List<ServiceProvider> spList = providers(layer);
        
                // 如果找到了匹配的服务提供者列表，则对其进行遍历
                for(ServiceProvider provider : spList) {
                    // 获取该服务提供者所在的模块
                    Module module = provider.module();
            
                    // 获取加载指定模块的类加载器
                    ClassLoader moduleLoader = loaderFor(module);
            
                    // 确保该类加载器不是bootstrap/platform类加载器
                    if(moduleLoader != null && moduleLoader != platformClassLoader) {
                        // 将该服务提供者记录下来
                        allProviders.add(provider);
                    }
                }
            }
    
            // 以迭代器形式返回查到的所有服务提供者
            return allProviders.iterator();
        }
    }
    
    /**
     * Implements lazy service provider lookup where the service providers are
     * configured via service configuration files. Service providers in named
     * modules are silently ignored by this lookup iterator.
     */
    // 基于类路径下（未命名模块）的服务查询器
    private final class LazyClassPathLookupIterator<T> implements Iterator<Provider<T>> {
        static final String PREFIX = "META-INF/services/";  // 固定的服务路径
        
        Enumeration<URL> configs;   // 待加载服务对应的所有服务注册文件的URL
        
        // to avoid duplicates
        Set<String> providerNames = new HashSet<>();    // 服务提供者的全限定类名
        
        Iterator<String> pending;   // 待处理的服务提供者名称列表
        
        Provider<T> nextProvider;   // 本次查找到的服务提供者信息，每次被获取后都会清理此标记
        
        ServiceConfigurationError nextError;
        
        LazyClassPathLookupIterator() {
        }
        
        /*
         * 在类路径下（未命名模块）的服务查询器查找下一个服务提供者，
         * 返回值指示是否存在这样的服务提供者。
         *
         * 该查找过程中会懒加载地创建类路径下的服务目录。
         */
        @Override
        public boolean hasNext() {
            if(acc == null) {
                return hasNextService();
            }
            
            PrivilegedAction<Boolean> action = new PrivilegedAction<>() {
                public Boolean run() {
                    return hasNextService();
                }
            };
            
            return AccessController.doPrivileged(action, acc);
        }
        
        // 返回下一个服务提供者工厂
        @Override
        public Provider<T> next() {
            if(acc == null) {
                return nextService();
            }
            
            PrivilegedAction<Provider<T>> action = new PrivilegedAction<>() {
                public Provider<T> run() {
                    return nextService();
                }
            };
            return AccessController.doPrivileged(action, acc);
        }
        
        // 是否存在下一个目标服务的服务提供者
        @SuppressWarnings("unchecked")
        private boolean hasNextService() {
            // 不存在服务提供者信息时，需要再次查找
            while(nextProvider == null && nextError == null) {
                try {
                    // 获取下一个服务提供者的类对象
                    Class<?> clazz = nextProviderClass();
                    if(clazz == null) {
                        return false;
                    }
                    
                    /*
                     * 忽略命名模块中的服务提供者
                     * 因为如果存在这样的服务提供者，之前在命名模块中的服务查询器中就找到了
                     */
                    if(clazz.getModule().isNamed()) {
                        // ignore class if in named module
                        continue;
                    }
                    
                    // 确保该服务提供者确实是服务接口的子类
                    if(service.isAssignableFrom(clazz)) {
    
                        // 获取服务提供者的无参public构造器
                        Constructor<? extends S> ctor = (Constructor<? extends S>) getConstructor((Class<? extends S>) clazz);
    
                        // 构造服务提供者工厂
                        nextProvider = (ProviderImpl<T>) new ProviderImpl<S>(service, (Class<? extends S>) clazz, ctor, acc);
                    } else {
                        fail(service, clazz.getName() + " not a subtype");
                    }
                } catch(ServiceConfigurationError e) {
                    nextError = e;
                }
            }
            
            return true;
        }
        
        // 返回下一个服务提供者信息
        private Provider<T> nextService() {
            // 如果不存在下一个目标服务的服务提供者，会抛异常
            if(!hasNextService()) {
                throw new NoSuchElementException();
            }
    
            Provider<T> provider = nextProvider;
            if(provider != null) {
                nextProvider = null;    // 置空
                return provider;
            } else {
                ServiceConfigurationError e = nextError;
                assert e != null;
                nextError = null;
                throw e;
            }
        }
        
        /**
         * Loads and returns the next provider class.
         */
        // 返回下一个服务提供者的类对象，这个过程中会懒加载式地查找出所有服务提供者
        private Class<?> nextProviderClass() {
            // 找出待加载服务对应的所有服务注册文件
            if(configs == null) {
                try {
                    // 获取存放服务提供者信息的服务注册文件的路径（路径前缀是固定的）
                    String fullName = PREFIX + service.getName();
        
                    /*
                     * 接下来，需要自顶向下加载资源，截止到loader类加载器；
                     * 最后返回【所有】匹配资源的URL，存入configs。
                     */
        
                    if(loader == null) {
                        // 处理bootstrap类加载的情形
                        configs = ClassLoader.getSystemResources(fullName);
            
                        // 处理platform类加载的情形
                    } else if(loader == ClassLoaders.platformClassLoader()) {
                        // platform类加载器没有关联类路径，但是它的父级类加载器bootstrap可能有关联的类路径
                        if(BootLoader.hasClassPath()) {
                            configs = BootLoader.findResources(fullName);
                        } else {
                            configs = Collections.emptyEnumeration();
                        }
            
                        // 处理app类加载或自定义类加载器的情形
                    } else {
                        configs = loader.getResources(fullName);
                    }
                } catch(IOException x) {
                    fail(service, "Error locating configuration files", x);
                }
            }
    
            // 如果不存在待处理的服务提供者名称列表，或者该列表已被处理完了，则需要继续搜寻下一批服务提供者
            while(pending == null || !pending.hasNext()) {
                // 如果不存在下一个服务提供者，则返回null
                if(!configs.hasMoreElements()) {
                    return null;
                }
        
                // 获取下一个服务提供者的位置信息
                URL url = configs.nextElement();
        
                // 解析url处的服务注册文件，获取到所有服务提供者的全限定类名，并以迭代器的形式返回(对于已解析过的服务提供者，不会重复列出)
                pending = parse(url);
            }
    
            // 获取下一个服务提供者的全限定类名
            String providerName = pending.next();
            try {
                // 创建服务提供者的类对象并返回，不初始化
                return Class.forName(providerName, false, loader);
            } catch(ClassNotFoundException x) {
                fail(service, "Provider " + providerName + " not found");
                return null;
            }
        }
        
        /**
         * Parse the content of the given URL as a provider-configuration file.
         */
        // 解析url处的服务注册文件，获取到所有服务提供者的全限定类名，并以迭代器的形式返回(对于已解析过的服务提供者，不会重复列出)
        private Iterator<String> parse(URL url) {
            Set<String> names = new LinkedHashSet<>(); // preserve insertion order
            
            try {
                // 打开连接
                URLConnection connection = url.openConnection();
                connection.setUseCaches(false);
                
                try(
                    InputStream in = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"))
                ) {
                    int line = 1;
                    
                    // 从服务注册文件中解析出服务提供者的全限定类名，返回值代表接下来该读第几行了
                    while((line = parseLine(url, reader, line, names)) >= 0) {
                        // 通过循环，解析出所有服务提供者的全限定类名
                    }
                }
            } catch(IOException x) {
                fail(service, "Error accessing configuration file", x);
            }
            
            // 返回服务提供者名称的迭代器
            return names.iterator();
        }
        
        /**
         * Parse a single line from the given configuration file,
         * adding the name on the line to set of names if not already seen.
         */
        /*
         * 从服务注册文件中解析出服务提供者的类名全名，返回值代表接下来该读第几行了
         *
         * url   ：服务注册文件的路径
         * reader：服务注册文件的字符流
         * names ：存储服务注册文件中解析出的服务提供者类名全名
         * line  ：记录当前读取的是第几行
         */
        private int parseLine(URL url, BufferedReader reader, int line, Set<String> names) throws IOException {
            // 读取一行
            String providerName = reader.readLine();
            if(providerName == null) {
                return -1;
            }
    
            // 获取当前行中'#'的位置
            int ci = providerName.indexOf('#');
    
            // 如果当前行存在'#'号，说明存在注释，则需要只截取注释之前的服务提供者类名
            if(ci >= 0) {
                providerName = providerName.substring(0, ci);
            }
    
            // 去除名称两端空白
            providerName = providerName.trim();
    
            int len = providerName.length();
            if(len != 0) {
                // 注册的服务提供者名称里不能存在空格或制表符
                if((providerName.indexOf(' ') >= 0) || (providerName.indexOf('\t') >= 0)) {
                    fail(service, url, line, "Illegal configuration-file syntax");
                }
        
                // 获取providerName中索引0处的符号的Unicode编码
                int cp = providerName.codePointAt(0);
        
                // 首先校验服务提供者的名称是否包含合规的前缀(校验第一个符号)
                if(!Character.isJavaIdentifierStart(cp)) {
                    fail(service, url, line, "Illegal provider-class name: " + providerName);
                }
        
                // 接下来，从服务提供者名称中的第二个符号起，遍历后续的所有符号，逐个校验其是否合规
                for(int i = Character.charCount(cp); i<len; i += Character.charCount(cp)) {
                    // 获取providerName中索引i处的符号的Unicode编码
                    cp = providerName.codePointAt(i);
            
                    // 校验每个符号是否为合格的Java标识符
                    if(!Character.isJavaIdentifierPart(cp) && (cp != '.')) {
                        fail(service, url, line, "Illegal provider-class name: " + providerName);
                    }
                }
        
                // 将该名称放入缓存
                if(providerNames.add(providerName)) {
                    // 如果该名称首次出现，也将其放入names中一份
                    names.add(providerName);
                }
            }
    
            // 返回第一行的行号
            return line + 1;
        }
    }
    
    /**
     * Implements lazy service provider lookup of service providers that
     * are provided by modules in a module layer (or parent layers)
     */
    // 基于模块层的服务查询器，用于直接在模块层系统中搜索服务提供者
    private final class LayerLookupIterator<T> implements Iterator<Provider<T>> {
        Deque<ModuleLayer> stack = new ArrayDeque<>();  // 深度优先遍历父级模块层时用到的辅助栈
        Set<ModuleLayer> visited = new HashSet<>();     // 记录已经遍历过的父级模块层
        
        Iterator<ServiceProvider> iterator; // 待加载服务在指定模块层下的服务提供者的迭代器
        
        Provider<T> nextProvider;   // 下一个服务提供者工厂
        
        ServiceConfigurationError nextError;
        
        LayerLookupIterator() {
            visited.add(layer);
            stack.push(layer);
        }
        
        // 是否(潜在地)包含下一个服务提供者
        @Override
        public boolean hasNext() {
            while(nextProvider == null && nextError == null) {
                // get next provider to load
                while(iterator == null || !iterator.hasNext()) {
                    
                    // next layer (DFS order)
                    if(stack.isEmpty()) {
                        return false;
                    }
                    
                    ModuleLayer layer = stack.pop();
                    
                    // 将layer的父模块层加入待处理列表
                    List<ModuleLayer> parents = layer.parents();
                    
                    // 逆序遍历，因为要入栈出栈
                    for(int i = parents.size() - 1; i >= 0; i--) {
                        ModuleLayer parent = parents.get(i);
                        if(!visited.contains(parent)) {
                            visited.add(parent);
                            stack.push(parent);
                        }
                    }
                    
                    // 获取待加载服务在指定模块层下的服务提供者的迭代器
                    iterator = providers(layer);
                }
                
                // 获取下一个服务提供者
                ServiceProvider provider = iterator.next();
                
                try {
                    // 获取指定服务提供者的服务提供者工厂
                    nextProvider = (Provider<T>) loadProvider(provider);
                } catch(ServiceConfigurationError e) {
                    nextError = e;
                }
            }
            
            return true;
        }
        
        // 返回下一个服务提供者工厂
        @Override
        public Provider<T> next() {
            if(!hasNext()) {
                throw new NoSuchElementException();
            }
            
            Provider<T> provider = nextProvider;
            if(provider != null) {
                nextProvider = null;
                return provider;
            } else {
                ServiceConfigurationError e = nextError;
                assert e != null;
                nextError = null;
                throw e;
            }
        }
        
        // 返回待加载服务在指定模块层下的服务提供者的迭代器
        private Iterator<ServiceProvider> providers(ModuleLayer layer) {
            // 获取当前模块层的服务目录（包含所有模块内的所有提供的服务）
            ServicesCatalog catalog = LANG_ACCESS.getServicesCatalog(layer);
    
            // 在当前模块层的服务集中查找serviceName对应的服务提供者列表
            List<ServiceProvider> list = catalog.findServices(serviceName);
    
            // 以迭代器形式返回找到的服务提供者列表
            return list.iterator();
        }
    }
    
    /**
     * Represents a service provider located by {@code ServiceLoader}.
     *
     * <p> When using a loader's {@link ServiceLoader#stream() stream()} method
     * then the elements are of type {@code Provider}. This allows processing
     * to select or filter on the provider class without instantiating the
     * provider. </p>
     *
     * @param <S> The service type
     *
     * @spec JPMS
     * @since 9
     */
    // 服务提供者工厂
    public static interface Provider<S> extends Supplier<S> {
        /**
         * Returns the provider type. There is no guarantee that this type is
         * accessible or that it has a public no-args constructor. The {@link
         * #get() get()} method should be used to obtain the provider instance.
         *
         * <p> When a module declares that the provider class is created by a
         * provider factory then this method returns the return type of its
         * public static "{@code provider()}" method.
         *
         * @return The provider type
         */
        // 返回服务提供者类型
        Class<? extends S> type();
        
        /**
         * Returns an instance of the provider.
         *
         * @return An instance of the provider.
         *
         * @throws ServiceConfigurationError If the service provider cannot be instantiated, or in the
         *                                   case of a provider factory, the public static
         *                                   "{@code provider()}" method returns {@code null} or throws
         *                                   an error or exception. The {@code ServiceConfigurationError}
         *                                   will carry an appropriate cause where possible.
         */
        // 返回使用当前服务提供者工厂创建的服务提供者
        @Override
        S get();
    }
    
    /**
     * A Provider implementation that supports invoking, with reduced
     * permissions, the static factory to obtain the provider or the
     * provider's no-arg constructor.
     */
    // 服务提供者工厂的实现类
    private static class ProviderImpl<S> implements Provider<S> {
        final Class<S> service;              // 服务接口/抽象类类型
        final Class<? extends S> type;       // 服务提供者类型
        final Method factoryMethod;          // 服务提供者中的静态工厂方法
        final Constructor<? extends S> ctor; // 用于构造服务提供者对象的public无参构造方法
        final AccessControlContext acc;
        
        ProviderImpl(Class<S> service, Class<? extends S> type, Method factoryMethod, AccessControlContext acc) {
            this.service = service;
            this.type = type;
            this.factoryMethod = factoryMethod;
            this.ctor = null;
            this.acc = acc;
        }
        
        ProviderImpl(Class<S> service, Class<? extends S> type, Constructor<? extends S> ctor, AccessControlContext acc) {
            this.service = service;
            this.type = type;
            this.factoryMethod = null;
            this.ctor = ctor;
            this.acc = acc;
        }
        
        // 返回服务提供者类型
        @Override
        public Class<? extends S> type() {
            return type;
        }
        
        // 返回使用当前服务提供者工厂创建的服务提供者
        @Override
        public S get() {
            // 如果存在工厂方法，则通过工厂方法反射创建对象
            if(factoryMethod != null) {
                return invokeFactoryMethod();
            } else {
                // 通过构造器反射创建对象
                return newInstance();
            }
        }
        
        /**
         * Invokes the provider's "provider" method to instantiate a provider.
         * When running with a security manager then the method runs with permissions
         * that are restricted by the security context of whatever created this loader.
         */
        // 使用工厂方法创建服务提供者对象
        private S invokeFactoryMethod() {
            Object provider = null;
            Throwable exc = null;
    
            if(acc == null) {
                try {
                    provider = factoryMethod.invoke(null);
                } catch(Throwable x) {
                    exc = x;
                }
            } else {
                PrivilegedExceptionAction<?> pa = new PrivilegedExceptionAction<>() {
                    @Override
                    public Object run() throws Exception {
                        return factoryMethod.invoke(null);
                    }
                };
        
                // invoke factory method with permissions restricted by acc
                try {
                    provider = AccessController.doPrivileged(pa, acc);
                } catch(Throwable x) {
                    if(x instanceof PrivilegedActionException) {
                        x = x.getCause();
                    }
                    exc = x;
                }
            }
    
            if(exc != null) {
                if(exc instanceof InvocationTargetException) {
                    exc = exc.getCause();
                }
                fail(service, factoryMethod + " failed", exc);
            }
    
            if(provider == null) {
                fail(service, factoryMethod + " returned null");
            }
    
            return (S) provider;
        }
        
        /**
         * Invokes Constructor::newInstance to instantiate a provider. When running
         * with a security manager then the constructor runs with permissions that
         * are restricted by the security context of whatever created this loader.
         */
        // 使用构造器创建服务提供者对象
        private S newInstance() {
            S provider = null;
            
            Throwable exc = null;
            
            if(acc == null) {
                try {
                    provider = ctor.newInstance();
                } catch(Throwable x) {
                    exc = x;
                }
            } else {
                PrivilegedExceptionAction<S> pa = new PrivilegedExceptionAction<>() {
                    @Override
                    public S run() throws Exception {
                        return ctor.newInstance();
                    }
                };
                
                // invoke constructor with permissions restricted by acc
                try {
                    provider = AccessController.doPrivileged(pa, acc);
                } catch(Throwable x) {
                    if(x instanceof PrivilegedActionException) {
                        x = x.getCause();
                    }
                    exc = x;
                }
            }
    
            if(exc != null) {
                if(exc instanceof InvocationTargetException) {
                    exc = exc.getCause();
                }
        
                String cn = ctor.getDeclaringClass().getName();
        
                fail(service, "Provider " + cn + " could not be instantiated", exc);
            }
    
            return provider;
        }
        
        /*
         * For now, equals/hashCode uses the access control context
         * to ensure that two Providers created with different contexts are not equal when running with a security manager.
         */
        
        @Override
        public int hashCode() {
            return Objects.hash(service, type, acc);
        }
        
        @Override
        public boolean equals(Object ob) {
            if(!(ob instanceof ProviderImpl)) {
                return false;
            }
            
            ProviderImpl<?> that = (ProviderImpl<?>) ob;
            
            return this.service == that.service && this.type == that.type && Objects.equals(this.acc, that.acc);
        }
        
    }
    
    // 可分割迭代器，用来获取流中下一个服务提供者工厂
    private class ProviderSpliterator<T> implements Spliterator<Provider<T>> {
        final int expectedReloadCount = ServiceLoader.this.reloadCount;
        
        final Iterator<Provider<T>> iterator;   // 服务查询器，通过该查询器可以查找所有注册的服务提供者的服务提供者工厂
        int index = 0;  // 索引，指示当前需要获取哪个服务提供者工厂
        
        ProviderSpliterator(Iterator<Provider<T>> iterator) {
            this.iterator = iterator;
        }
        
        @Override
        public Spliterator<Provider<T>> trySplit() {
            return null;
        }
        
        // 获取指定索引处的服务提供者工厂，并对其进行择取操作
        @Override
        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super Provider<T>> action) {
            if(ServiceLoader.this.reloadCount != expectedReloadCount) {
                throw new ConcurrentModificationException();
            }
            
            Provider<T> next = null;
    
            // 从缓存中获取服务提供者工厂
            if(index<loadedProviders.size()) {
                next = (Provider<T>) loadedProviders.get(index++);
    
                // 如果存在下一个服务提供者的服务提供者工厂
            } else if(iterator.hasNext()) {
                // 获取服务提供者工厂
                next = iterator.next();
    
                // 缓存服务提供者工厂
                loadedProviders.add((Provider<S>) next);
    
                // 记录已获取的服务提供者工厂数量
                index++;
            } else {
                loadedAllProviders = true;
            }
    
            if(next == null) {
                return false;
            }
    
            // 如果存在服务提供者工厂，需要对其执行择取操作
            action.accept(next);
    
            return true;
        }
        
        @Override
        public int characteristics() {
            // not IMMUTABLE as structural interference possible
            // not NOTNULL so that the characteristics are a subset of the
            // characteristics when all Providers have been located.
            return Spliterator.ORDERED;
        }
        
        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }
    }
    
}

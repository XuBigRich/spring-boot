/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.liquibase;

import liquibase.servicelocator.CustomResolverServiceLocator;
import liquibase.servicelocator.ServiceLocator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.ClassUtils;

/**
 * {@link ApplicationListener} that replaces the liquibase {@link ServiceLocator} with a
 * version that works with Spring Boot executable archives.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 1.0.0
 */
public class LiquibaseServiceLocatorApplicationListener implements ApplicationListener<ApplicationStartingEvent> {

	private static final Log logger = LogFactory.getLog(LiquibaseServiceLocatorApplicationListener.class);

	/**
	 * 监听者模式 监听事件 ，仅仅使用了事件源的类加载器
	 *
	 * @param event
	 */
	@Override
	public void onApplicationEvent(ApplicationStartingEvent event) {
//		ClassUtils.isPresent 这个方法会先寻找 ClassUtils 的静态属性 commonClassCache 与primitiveTypeNameMap，看是否已经将 className 加载并缓存
		//若没有，则先将 className进行处理， 排除数组 和list类型的类全名，仅保存真正的 类,然后使用类装载器进行装载
		//这个代码的作用：
		// 	对liquibase.servicelocator.CustomResolverServiceLocator 这个类 进行装载，使用与SpringApplication一样的类加载器加载
		if (ClassUtils.isPresent("liquibase.servicelocator.CustomResolverServiceLocator",
				event.getSpringApplication().getClassLoader())) {
			//执行初始化LiquibasePresent，执行replaceServiceLocator方法
			new LiquibasePresent().replaceServiceLocator();
		}
	}

	/**
	 * 内部类防止类未被发现的问题。  ???
	 * Inner class to prevent class not found issues.
	 */
	private static class LiquibasePresent {

		void replaceServiceLocator() {
			//服务定位器模式
			//自定义服务定位解析器，继承自ServiceLocator 类
			// ServiceLocator初始化时，他会先加载一系列 预先准备的服务定位器，若这些准备好的不存在，那么使用默认的服务定位器

			//默认的服务定位器：
			// 默认的服务定位器在初始化的时候，会 通过setResourceAccessor方法，将初始化好的 ClassLoaderResourceAccessor对象封装到resourceAccessor属性中
			//初始化 ClassLoaderResourceAccessor
			//ClassLoaderResourceAccessor在初始化时 ，会先获取哪个类加载器加载的本对象，然后通过这个类加载器 ，找到他所有加载过的类的jar包 实际地址;
			//将取到的jar包实际地址，放入到ClassLoaderResourceAccessor类的rootStrings属性中
			CustomResolverServiceLocator customResolverServiceLocator = new CustomResolverServiceLocator(
					//放入处理类
					new SpringPackageScanClassResolver(logger));
			//ServiceLocator 的构造函数是只能在包内和  子类中实现，所以外部无法对他进行初始化，但是 他有一个静态的ServiceLocator属性 可供其他类调用
			ServiceLocator.setInstance(customResolverServiceLocator);
		}

	}

}

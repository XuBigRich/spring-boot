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

package org.springframework.boot.convert;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.StringValueResolver;

/**
 * A specialization of {@link FormattingConversionService} configured by default with
 * converters and formatters appropriate for most Spring Boot applications.
 * <p>
 * Designed for direct instantiation but also exposes the static
 * {@link #addApplicationConverters} and
 * {@link #addApplicationFormatters(FormatterRegistry)} utility methods for ad-hoc use
 * against registry instance.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * 应用转换服务 -》FormattingConversionService-》 extends GenericConversionService   implements FormatterRegistry, EmbeddedValueResolverAware
 * GenericConversionService -》GenericConversionService
 */
public class ApplicationConversionService extends FormattingConversionService {
	//私有的静态的共享实例 听起来名字怪怪的
	private static volatile ApplicationConversionService sharedInstance;

	public ApplicationConversionService() {
		this(null);
	}

	public ApplicationConversionService(StringValueResolver embeddedValueResolver) {
		if (embeddedValueResolver != null) {
			setEmbeddedValueResolver(embeddedValueResolver);
		}
		configure(this);
	}

	/**
	 * 太搞笑了这竟然像一个单例模式
	 * <p>
	 * Return a shared default application {@code ConversionService} instance, lazily
	 * building it once needed.
	 * <p>
	 * Note: This method actually returns an {@link ApplicationConversionService}
	 * instance. However, the {@code ConversionService} signature has been preserved for
	 * binary compatibility.
	 *
	 * @return the shared {@code ApplicationConversionService} instance (never
	 * {@code null})
	 */
	public static ConversionService getSharedInstance() {
		//查看那个私有的共享示例是否需要初始化
		ApplicationConversionService sharedInstance = ApplicationConversionService.sharedInstance;
		//如果需要初始化
		if (sharedInstance == null) {
			//上锁
			synchronized (ApplicationConversionService.class) {
				//判断上锁期间是否已经被初始化过
				sharedInstance = ApplicationConversionService.sharedInstance;
				//如果依然没有被初始化
				if (sharedInstance == null) {
					//使用默认的构造方法创建一个静态实例
					sharedInstance = new ApplicationConversionService();
					//将这个静态实例赋值给 自己的变量sharedInstance
					ApplicationConversionService.sharedInstance = sharedInstance;
				}
			}
		}
		//返回这个 私有属性变量，这里将私有的静态变量开放出去，所以这可能依然是一个单例模式，因为一个ApplicationConversionService类中只能有一个sharedInstance属性对象
		return sharedInstance;
	}

	/**
	 * Configure the given {@link FormatterRegistry} with formatters and converters
	 * appropriate for most Spring Boot applications.
	 *
	 * @param registry the registry of converters to add to (must also be castable to
	 *                 ConversionService, e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given FormatterRegistry could not be cast to a
	 *                            ConversionService
	 */
	public static void configure(FormatterRegistry registry) {
		DefaultConversionService.addDefaultConverters(registry);
		DefaultFormattingConversionService.addDefaultFormatters(registry);
		addApplicationFormatters(registry);
		addApplicationConverters(registry);
	}

	/**
	 * Add converters useful for most Spring Boot applications.
	 *
	 * @param registry the registry of converters to add to (must also be castable to
	 *                 ConversionService, e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given ConverterRegistry could not be cast to a
	 *                            ConversionService
	 */
	public static void addApplicationConverters(ConverterRegistry registry) {
		addDelimitedStringConverters(registry);
		registry.addConverter(new StringToDurationConverter());
		registry.addConverter(new DurationToStringConverter());
		registry.addConverter(new NumberToDurationConverter());
		registry.addConverter(new DurationToNumberConverter());
		registry.addConverter(new StringToDataSizeConverter());
		registry.addConverter(new NumberToDataSizeConverter());
		registry.addConverter(new StringToFileConverter());
		registry.addConverterFactory(new LenientStringToEnumConverterFactory());
		registry.addConverterFactory(new LenientBooleanToEnumConverterFactory());
	}

	/**
	 * Add converters to support delimited strings.
	 *
	 * @param registry the registry of converters to add to (must also be castable to
	 *                 ConversionService, e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given ConverterRegistry could not be cast to a
	 *                            ConversionService
	 */
	public static void addDelimitedStringConverters(ConverterRegistry registry) {
		ConversionService service = (ConversionService) registry;
		registry.addConverter(new ArrayToDelimitedStringConverter(service));
		registry.addConverter(new CollectionToDelimitedStringConverter(service));
		registry.addConverter(new DelimitedStringToArrayConverter(service));
		registry.addConverter(new DelimitedStringToCollectionConverter(service));
	}

	/**
	 * Add formatters useful for most Spring Boot applications.
	 *
	 * @param registry the service to register default formatters with
	 */
	public static void addApplicationFormatters(FormatterRegistry registry) {
		registry.addFormatter(new CharArrayFormatter());
		registry.addFormatter(new InetAddressFormatter());
		registry.addFormatter(new IsoOffsetFormatter());
	}

	/**
	 * Add {@link GenericConverter}, {@link Converter}, {@link Printer}, {@link Parser}
	 * and {@link Formatter} beans from the specified context.
	 *
	 * @param registry    the service to register beans with
	 * @param beanFactory the bean factory to get the beans from
	 * @since 2.2.0
	 */
	public static void addBeans(FormatterRegistry registry, ListableBeanFactory beanFactory) {
		Set<Object> beans = new LinkedHashSet<>();
		beans.addAll(beanFactory.getBeansOfType(GenericConverter.class).values());
		beans.addAll(beanFactory.getBeansOfType(Converter.class).values());
		beans.addAll(beanFactory.getBeansOfType(Printer.class).values());
		beans.addAll(beanFactory.getBeansOfType(Parser.class).values());
		for (Object bean : beans) {
			if (bean instanceof GenericConverter) {
				registry.addConverter((GenericConverter) bean);
			} else if (bean instanceof Converter) {
				registry.addConverter((Converter<?, ?>) bean);
			} else if (bean instanceof Formatter) {
				registry.addFormatter((Formatter<?>) bean);
			} else if (bean instanceof Printer) {
				registry.addPrinter((Printer<?>) bean);
			} else if (bean instanceof Parser) {
				registry.addParser((Parser<?>) bean);
			}
		}
	}

}

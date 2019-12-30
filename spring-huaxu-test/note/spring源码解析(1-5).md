# 第一章 Spring整体架构和环境搭建

<https://blog.csdn.net/weixin_39923425/article/details/93660826>

## 1.1 spring涉及的模块

| [Overview](https://docs.spring.io/spring/docs/current/spring-framework-reference/overview.html#overview) | history, design philosophy, feedback, getting started.       |
| ------------------------------------------------------------ | :----------------------------------------------------------- |
| [Core](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#spring-core) | IoC Container, Events, Resources, i18n, Validation, Data Binding, Type Conversion, SpEL, AOP. |
| [Testing](https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#testing) | Mock Objects, TestContext Framework, Spring MVC Test, WebTestClient. |
| [Data Access](https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#spring-data-tier) | Transactions, DAO Support, JDBC, O/R Mapping, XML Marshalling. |
| [Web Servlet](https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#spring-web) | Spring MVC, WebSocket, SockJS, STOMP Messaging.              |
| [Web Reactive](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#spring-webflux) | Spring WebFlux, WebClient, WebSocket.                        |
| [Integration](https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#spring-integration) | Remoting, JMS, JCA, JMX, Email, Tasks, Scheduling, Caching.  |
| [Languages](https://docs.spring.io/spring/docs/current/spring-framework-reference/languages.html#languages) | Kotlin, Groovy, Dynamic Languages.                           |



# 第二章 容器的基本实现

## 2.1  Spring的结构组成

#### DefaultListableBeanFactory

> 是整个bean加载的核心部分, 是spring注册及加载bean的默认实现;

经典的使用: XmlBeanFactory继承自DefaultListableBeanFactory
(1) XmlBeanFactory对DefaultListableBeanFactory类进行了扩展,  主要用于从XML文档中读取BeanDefinition.

(2) 对于注册及获取Bean都是使用从父类DefaultListableBeanFactory继承的方法去实现, 而唯独与父类不同的个性化实现就是增加了XmlBeanDefinitionReader类型的reader属性.

(3) 在XmlbeanFactory中主要使用reader属性对资源文件进行读取和注册.

#### XmlBeanDefinitionReader

> xml配置文件的读取是Spring中重要的功能, 此类中整合了资源文件的读取,解析及注册

(1) 通过继承自AbstractBeanDefinitionReader中的方法, 来使用ResourLoader将资源文件路径转化为对应的Resource文件.

(2) 通过DocumentLoader对Resource文件进行转换, 将Resource文件转换为Document文件.

(3) 通过实现接口BeanDefinitionDocumentReader 的 DefaultBeanDefinitionDocumentReader类对Document进行解析, 并使用DeanDefinitionParserDelegate 对 Element 进行解析.





## 2.2  容器的基础XmlBeanFactory

```java
BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring-context.xml"));
```

> 调用过程
> 	(1) 调用ClassPathResource的构造函数来构造Resource资源文件的实例对象,
>
> ​	(2) 根据Resource进行XmlBeanFactory的初始化.	

#### 2.2.1 配置文件封装

(1) org.springframework.core.io.Resource接口
      抽象了所有Spring内部使用到的底层资源: File, URL, Classpath等.
      定义了三个判断资源状态的方法: 存在性 exists(),  可读性 isReadable(), 是否存乎打开状态isOpen()

(2) 不同来源的资源文件都有对应的Resource实现: 

​	文件: fileSystemResource
​	Classpath: ClassPathResource
​	URL资源:UrlResource
​	InputStream资源:inputStreamResource 等

(3) 举例说明

```java
Resource resource = new ClassPathResource("spring-context.xml");
InputStream inputStream = resource.getInputStream();

public XmlBeanFactory(Resource rs, BeanFactory py) throws BeansException {
		// 忽略给定接口的自动装配功能
		// 这里的典型应用就是通过其它解析Appliation上下文注册依赖, 蕾丝与BeanFactory通过beanFactroyAware进行注入或者ApplicationContext通过ApplicationContextAware
		super(py);
		// 真正加载数据的实现
		this.reader.loadBeanDefinitions(rs);
}

/**
	* Create a new AbstractAutowireCapableBeanFactory.
	*/
public AbstractAutowireCapableBeanFactory() {
		super();
		// 忽略给定接口的自动装配功能
		ignoreDependencyInterface(BeanNameAware.class);
		ignoreDependencyInterface(BeanFactoryAware.class);
		ignoreDependencyInterface(BeanClassLoaderAware.class);
}
```



#### 2.2.2 加载Bean

> 这里的加载就是2.2.1 中提到的真正数据的地方- loadBeanDefinitions(resource) 方法, 这里是整个资源加载的切入点.

(1) 封装资源文件

```java
@Override
public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource));
}
// 将Resource使用EncodedResource类进行封装
```

(2) 获取输入流

```java
Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
if (currentResources == null) {
		currentResources = new HashSet<>(4);
		this.resourcesCurrentlyBeingLoaded.set(currentResources);
}

// 防止循环加载, 使用一个ThreadLocal<Set<EncodedResource>>
if (!currentResources.add(encodedResource)) {
		throw new BeanDefinitionStoreException("Detected cyclic loading of " + encodedResource + " - check your import definitions!");
}

InputStream inputStream = encodedResource.getResource().getInputStream();

// 从Resource中获取对应的InputSteam并构造InputSource
```

(3) 通过构造的InputSource实例和Resource实例继续调用函数doLoadBeanDefinitions.

```java
Document doc = doLoadDocument(inputSource, resource);
int count = registerBeanDefinitions(doc, resource);
if (logger.isDebugEnabled()) {
  logger.debug("Loaded " + count + " bean definitions from " + resource);
}
return count;

// 1. 获取对XML文件的验证模式
// 2. 加载XML文件, 并得到对应的Document
// 3. 根据返回的Document注册Bean信息
```





## 2.3 获取XML的验证模式

> 引言: XML文件的验证模式保证了XML文件的正确性, 而比较常用的验证模式有两种: DTD和XSD. 

#### 2.3.1 DTD与XSD区别

- DTD 文档类型定义, 包含元素的定义规则, 元素间关系的定义规则, 元素可使用的属性, 可使用的实体或符号规则
  `<!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN" "https://www.springframework.org/dtd/spring-beans-2.0.dtd"> `

- XSD xml文档结构, 要生命名称空间, 还必须指定名称空间对应的XML schema文档的存储位置. 一部分是名称空间的URI, 另一部分就是改名称空间所标识的文件位置或者URL地址

  `<beans xmlns="http://www.springframework.org/schema/beans"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd
  ">`

#### 2.3.2 验证模式的读取

```java
protected int getValidationModeForResource(Resource resource) {
		int validationModeToUse = getValidationMode();
  	// 如果手动指定了验证模式则使用指定的验证模式
  	// 如果使用Groovy或者手动设置setValidating()
		if (validationModeToUse != VALIDATION_AUTO) {
				return validationModeToUse;
		}
  	// 如果未指定则使用自动检测
		int detectedMode = detectValidationMode(resource);
		if (detectedMode != VALIDATION_AUTO) {
				return detectedMode;
		}
		return VALIDATION_XSD;
}

while ((content = reader.readLine()) != null) {
				content = consumeCommentTokens(content);
				// 如果是数据行 或者 或者是注释就略过
				if (this.inComment || !StringUtils.hasText(content)) {
					continue;
				}
  			// content.contains("DOCTYPE");
  			// 判断是否存在 DOCTYPE 字符串
				if (hasDoctype(content)) {
					isDtdValidated = true;
					break;
				}
				// 验证模式一定在开始<符号之前
				if (hasOpeningTag(content)) {
					// End of meaningful data...
					break;
				}
			}
return (isDtdValidated ? VALIDATION_DTD : VALIDATION_XSD);
```





## 2.4 获取Document

```java
@Override
public Document loadDocument(InputSource inputSource, EntityResolver entityResolver,
			ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception {
		DocumentBuilderFactory factory = createDocumentBuilderFactory(validationMode, namespaceAware);
		if (logger.isTraceEnabled()) {
			logger.trace("Using JAXP provider [" + factory.getClass().getName() + "]");
		}
		DocumentBuilder builder = createDocumentBuilder(factory, entityResolver, errorHandler);
		return builder.parse(inputSource);
}
// 正常解析Document. 没有特殊的
```

#### 2.4.1 EntityResolver用法

> 为何要在loadDocument方法中涉及一个参数EntityResolver?
> 官网解释:
> 如果SAX应用程序需要实现自定义处理外部实体, 则必须实现此接口并使用setEntityResolver方法向SAX驱动器注册一个实例.
>
> 所以这个的作用是项目本身就可以提供一个如何寻找DTD声明的方法, 即由程序来实现寻找DTD声明的过程, 比如我们将DTD放到项目某处, 在实现时直接将此文档读取并返回给SAX即可. 这样就避免来通过网络来寻找对应的声明

```java
public interface EntityResolver {
  	public abstract InputSource resolveEntity (String publicId, String systemId);
}
```

(1)  如果我们在解析验证模式为XSD的配置文件,
`<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
         http://www.springframework.org/schema/beans/spring-beans.xsd
">`

- publicId = null
- systemId = http://www.springframework.org/schema/beans/spring-beans.xsd

(2) 如果我们在解析验证模式为DTD的配置文件,

`<!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN" "https://www.springframework.org/dtd/spring-beans-2.0.dtd">`

* publicId = -//SPRING//DTD BEAN 2.0//EN
* systemId = https://www.springframework.org/dtd/spring-beans-2.0.dtd

(3) 总结

对不同的验证模式, Spring使用了不同的解析器. 比如加载DTD类型的BeansDtdResolver的resolveEntity是直接截取systemId最后的xx.dtd, 然后去当前路径下寻找, 而加载XSD类型的PluggableSchemaResolver类的resolveEntity是默认到META-INF/Spring.schemas文件中找到systemid所对应文件并加载





## 2.5 解析及注册BeanDefinitions

```java
public int registerBeanDefinitions(Document doc, Resource resource) {
  	// 使用反射实例化对象 DefaultBeanDefinitionDocumentReader.class
		BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
		// 记录统计前beanDefinition的加载个数
  	int countBefore = getRegistry().getBeanDefinitionCount();
  	// 加载及注册bean
		documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
  	// 记录本次加载的beanDefinition个数
		return getRegistry().getBeanDefinitionCount() - countBefore;
}
```

#### 2.5.1 profile属性的使用

```xml
<beans profile="dev"></beans>
<beans profile="uat"></beans>
```

这个特性是我们同时在配置文件中部署两套配置来适用于生产环境和开发环境

```java
protected void doRegisterBeanDefinitions(Element root) {
		BeanDefinitionParserDelegate parent = this.delegate;
		this.delegate = createDelegate(getReaderContext(), root, parent);

		if (this.delegate.isDefaultNamespace(root)) {
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			if (StringUtils.hasText(profileSpec)) {
				String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
						profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
				// We cannot use Profiles.of(...) since profile expressions are not supported
				// in XML config. See SPR-12458 for details.
				// 判断是否匹配环境 profile 属性
				if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipped XML bean definition file due to specified profiles [" + profileSpec +
								"] not matching: " + getReaderContext().getResource());
					}
					return;
				}
			}
		}
		
		preProcessXml(root);
		parseBeanDefinitions(root, this.delegate);
		postProcessXml(root);
		this.delegate = parent;
}
```

#### 2.5.2 解析并注册BeanDefinition

```java
protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		if (delegate.isDefaultNamespace(root)) {
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					Element ele = (Element) node;
					if (delegate.isDefaultNamespace(ele)) {
						// 使用默认的方法对bean进行处理
						// <bean id="test" class="com.test" >
						parseDefaultElement(ele, delegate);
					} else {
						// 使用自定义的方法进行处理
						// <my-test-ew/>
						delegate.parseCustomElement(ele);
					}
				}
			}
		} else {
			delegate.parseCustomElement(root);
		}
}
```



# 第三章 默认标签的解析

> ```
> private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
>    if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) { // import
>       importBeanDefinitionResource(ele);
>    }
>    else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) { // alias
>       processAliasRegistration(ele);
>    }
>    else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) { // bean
>       processBeanDefinition(ele, delegate);
>    }
>    else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) { // beans
>       // recurse
>       doRegisterBeanDefinitions(ele);
>    }
> }
> 分别对四种不同标签做了不同的处理 import alias bean beans
> ```

## 3.1 bean标签的解析及注册

```java
protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		// 通过ele解析出bdHolder实例, 此类已经包含各种属性了, class, name, id, alias之类的属性
  	BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
        bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
        try {
          // Register the final decorated instance.
          // 注册最后的修饰实例, 将BeanDefinition存在map中
          BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
        }
        catch (BeanDefinitionStoreException ex) {
          getReaderContext().error("Failed to register bean definition with name '" +
              bdHolder.getBeanName() + "'", ele, ex);
        }
        // Send registration event.
      	// 发出响应事件, 通知相关的监听器, 这个bean已经加载完成
        getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
}
```

### 3.1.1 解析Beandefinition

> 主要工作流程
> (1) 提取元素中的id已经name属性
>
> (2) 进一步解析其他所有属性并统一封装至GenericBeanDefinition类型的实例中
>
> (3) 如果检测到bean没有指定beanName, 那么使用默认规则为此Bean生成beanName
>
> (4) 将获取到的信息封装到BeanDefinittionHolder的实例中


以下是针对第二步, 封装GenericBeanDefinition类型变量的实例.

```java
// 创建用于承载属性的AbstractBeanDefinition类型的GenericBeanDefinition
AbstractBeanDefinition bd = createBeanDefinition(className, parent);

// 硬编码解析默认bean的各种属性, 懒加载, 是否单例, 初始化参数等
parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);

// 提取描述信息
bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));
// 解析元数据
parseMetaElements(ele, bd);
// 解析lookup-method属性
parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
// 解析replaced-method属性
parseReplacedMethodSubElements(ele, bd.getMethodOverrides());
// 解析构造函数参数
parseConstructorArgElements(ele, bd);
// 解析property子元素
parsePropertyElements(ele, bd);
// 解析qualifier子元素
parseQualifierElements(ele, bd);

bd.setResource(this.readerContext.getResource());
bd.setSource(extractSource(ele));
return bd;
```

#### 1. 创建用于属性承载的BeanDefinition

> BeanDefinition是一个接口, 在Spring中存在三种实现:
> |-AbstractBeanDefinition
> 	|- RootBeanDefinition
> 	|- ChildBeanDefinition
> 	|- GenericBeanDefinition

要解析属性首先要创建用于承载属性的实例, 也就是创建GenericBeanDefinition类型的实例

```java
public static AbstractBeanDefinition createBeanDefinition(
			@Nullable String parentName, @Nullable String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
		
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setParentName(parentName);
		if (className != null) {
        if (classLoader != null) {
          	// 如果classLoader不为空, 则使用以传入的classLoader同一虚拟机加载类对象, 否则只是记录className
          	bd.setBeanClass(ClassUtils.forName(className, classLoader));
        } else {
          	bd.setBeanClassName(className);
        }
		}
		return bd;
}
```

#### 2. 解析各种属性

硬编码解析默认bean的各种属性, 懒加载, 是否单例, 初始化参数等

```java
public AbstractBeanDefinition parseBeanDefinitionAttributes(Element ele, String beanName,
			@Nullable BeanDefinition containingBean, AbstractBeanDefinition bd) {
		// 解析scope属性, 如果以singleton开头则报错
		if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) {
			error("Old 1.x 'singleton' attribute in use - upgrade to 'scope' declaration", ele);
		}
		else if (ele.hasAttribute(SCOPE_ATTRIBUTE)) {
			bd.setScope(ele.getAttribute(SCOPE_ATTRIBUTE));
		}
		else if (containingBean != null) {
			// Take default from containing bean in case of an inner bean definition.
			// 在嵌入beanDifintion情况下且没有单独制定scope属性则使用父类默认的属性
      bd.setScope(containingBean.getScope());
		}
		// 解析abstract属性[设置此属性,将不会被初始化]
		if (ele.hasAttribute(ABSTRACT_ATTRIBUTE)) {
			bd.setAbstract(TRUE_VALUE.equals(ele.getAttribute(ABSTRACT_ATTRIBUTE)));
		}
		// 解析lazy-init属性[延迟加载:在第一次向容器索取时初始化]
		String lazyInit = ele.getAttribute(LAZY_INIT_ATTRIBUTE);
		if (isDefaultValue(lazyInit)) {
			lazyInit = this.defaults.getLazyInit();
		}
  	// 若没有设置或设置成其它都会被设置为false
		bd.setLazyInit(TRUE_VALUE.equals(lazyInit));
  	// 设置autowire属性[自动装配no,byName,byType,constructor,autodetect,default]
		String autowire = ele.getAttribute(AUTOWIRE_ATTRIBUTE);
		bd.setAutowireMode(getAutowireMode(autowire));
		// 解析depends-on属性[depends-on用于强依赖关系,先后顺序,ref则不用]
		if (ele.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
			String dependsOn = ele.getAttribute(DEPENDS_ON_ATTRIBUTE);
			bd.setDependsOn(StringUtils.tokenizeToStringArray(dependsOn, MULTI_VALUE_ATTRIBUTE_DELIMITERS));
		}
		// 设置autowire-candidate属性[容器在查找自动装配对象时，将不考虑该bean，即该bean不会被作为其它bean自动装配的候选者]
		String autowireCandidate = ele.getAttribute(AUTOWIRE_CANDIDATE_ATTRIBUTE);
		if (isDefaultValue(autowireCandidate)) {
			String candidatePattern = this.defaults.getAutowireCandidates();
			if (candidatePattern != null) {
				String[] patterns = StringUtils.commaDelimitedListToStringArray(candidatePattern);
				bd.setAutowireCandidate(PatternMatchUtils.simpleMatch(patterns, beanName));
			}
		}
		else {
			bd.setAutowireCandidate(TRUE_VALUE.equals(autowireCandidate));
		}
		// 解析primary属性[自动装配时当出现多个Bean候选者时]
		if (ele.hasAttribute(PRIMARY_ATTRIBUTE)) {
			bd.setPrimary(TRUE_VALUE.equals(ele.getAttribute(PRIMARY_ATTRIBUTE)));
		}
		// 解析init-method属性[用于指定初始化方法]
		if (ele.hasAttribute(INIT_METHOD_ATTRIBUTE)) {
			String initMethodName = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
			bd.setInitMethodName(initMethodName);
		}
		else if (this.defaults.getInitMethod() != null) {
			bd.setInitMethodName(this.defaults.getInitMethod());
			bd.setEnforceInitMethod(false);
		}
		// 解析destroy-method属性[用于指定销毁方法]
		if (ele.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
			String destroyMethodName = ele.getAttribute(DESTROY_METHOD_ATTRIBUTE);
			bd.setDestroyMethodName(destroyMethodName);
		}
		else if (this.defaults.getDestroyMethod() != null) {
			bd.setDestroyMethodName(this.defaults.getDestroyMethod());
			bd.setEnforceDestroyMethod(false);
		}
		// 解析factory-method属性[用于调用工厂类方法-非静态]
		if (ele.hasAttribute(FACTORY_METHOD_ATTRIBUTE)) {
			bd.setFactoryMethodName(ele.getAttribute(FACTORY_METHOD_ATTRIBUTE));
		}
  	// 解析factory-bean属性[用于实例化工厂类-静态]
		if (ele.hasAttribute(FACTORY_BEAN_ATTRIBUTE)) {
			bd.setFactoryBeanName(ele.getAttribute(FACTORY_BEAN_ATTRIBUTE));
		}
		return bd;
}
```

#### 3. 解析子元素meta

```xml
// 首先回顾一下mate属性的使用
<bean id="myBean" class="bean.MyBean" >
   <meta key="speed" value="12ms"/>
</bean>
// speed 并不会体现在MyBean的属性当中, 而是一个额外的声明, 可以通过BeanDefinition的getAttribute(key)获取
```

```java
public void parseMetaElements(Element ele, BeanMetadataAttributeAccessor attributeAccessor) {
		NodeList nl = ele.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, META_ELEMENT)) {
				Element metaElement = (Element) node;
				String key = metaElement.getAttribute(KEY_ATTRIBUTE);
				String value = metaElement.getAttribute(VALUE_ATTRIBUTE);
				// 使用key，value记录信息并构建BeanMetadataAttribute
				BeanMetadataAttribute attribute = new BeanMetadataAttribute(key, value);
				attribute.setSource(extractSource(metaElement));
				// 记录信息，到一个linkJHashMap，如果name是空就是删除对应的
				attributeAccessor.addMetadataAttribute(attribute);
			}
		}
}
```

#### 4. 解析子元素lookup-method

> 注入器注入:  这是一种特殊的方法注入, 它是把一个方法声明为返回某种类型的bean, 但实际要返回的bean是在配置文件里面配置的, 可以做到可插拔, 接触程序依赖

```java
public void parseLookupOverrideSubElements(Element beanEle, MethodOverrides overrides) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
        Node node = nl.item(i);
        // 仅当在bean元素下且元素为lookup-method才有效
        if (isCandidateElement(node) && nodeNameEquals(node, LOOKUP_METHOD_ELEMENT)) {
            Element ele = (Element) node;
            String methodName = ele.getAttribute(NAME_ATTRIBUTE);
            String beanRef = ele.getAttribute(BEAN_ELEMENT);
            // 通过beanRef拿到需要实例的对象，触发的代码还没有找到
            LookupOverride override = new LookupOverride(methodName, beanRef);
            override.setSource(extractSource(ele));
            overrides.addOverride(override);
        }
		}
}
```

#### 5. 解析子元素replaced-menthod

> 方法替换:   可以在运行时用新的方法替换现有的方法. 比look-up更加强大

```java
public void parseReplacedMethodSubElements(Element beanEle, MethodOverrides overrides) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
        Node node = nl.item(i);
        // 仅当在bean元素下且元素为replaced-method才有效
        if (isCandidateElement(node) && nodeNameEquals(node, REPLACED_METHOD_ELEMENT)) {
            Element replacedMethodEle = (Element) node;
            // 提取要替换旧的方法
            String name = replacedMethodEle.getAttribute(NAME_ATTRIBUTE);
            // 提取对应新的方法
            String callback = replacedMethodEle.getAttribute(REPLACER_ATTRIBUTE);
            ReplaceOverride replaceOverride = new ReplaceOverride(name, callback);
            // Look for arg-type match elements.
            // 只重载具有对应入参数的方法，可以为空
            List<Element> argTypeEles = DomUtils.getChildElementsByTagName(replacedMethodEle, ARG_TYPE_ELEMENT);
            for (Element argTypeEle : argTypeEles) {
                // 记录参数
                String match = argTypeEle.getAttribute(ARG_TYPE_MATCH_ATTRIBUTE);
                match = (StringUtils.hasText(match) ? match : DomUtils.getTextValue(argTypeEle));
                if (StringUtils.hasText(match)) {
                  	replaceOverride.addTypeIdentifier(match);
                }
            }
            replaceOverride.setSource(extractSource(replacedMethodEle));
            overrides.addOverride(replaceOverride);
        }
		}
}
```

#### 6. 解析子元素constructor-arg

> 构造函数的解析是常用且复杂的…..

```java
public void parseConstructorArgElement(Element ele, BeanDefinition bd) {
		// 提取index属性
		String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE);
		// 提取type属性
		String typeAttr = ele.getAttribute(TYPE_ATTRIBUTE);
		// 提取name属性
		String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
		if (StringUtils.hasLength(indexAttr)) {
			try {
				int index = Integer.parseInt(indexAttr);
				if (index < 0) {
					error("'index' cannot be lower than 0", ele);
				}
				else {
					try {
              this.parseState.push(new ConstructorArgumentEntry(index));
              // 解析ele对应属性元素, 后文重点解析
              Object value = parsePropertyValue(ele, bd, null);
              ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
              if (StringUtils.hasLength(typeAttr)) {
                valueHolder.setType(typeAttr);
              }
              if (StringUtils.hasLength(nameAttr)) {
                valueHolder.setName(nameAttr);
              }
              valueHolder.setSource(extractSource(ele));
              // 不允许重复指定相同参数
              if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(index)) {
                error("Ambiguous constructor-arg entries for index " + index, ele);
              }
              else {
                bd.getConstructorArgumentValues().addIndexedArgumentValue(index, valueHolder);
              }
					}
					finally {
						this.parseState.pop();
					}
				}
			}
			catch (NumberFormatException ex) {
				error("Attribute 'index' of tag 'constructor-arg' must be an integer", ele);
			}
		}
		else {// 没有index属性则忽略去属性， 自动寻找
			try {
          this.parseState.push(new ConstructorArgumentEntry());
          Object value = parsePropertyValue(ele, bd, null);
          ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
          if (StringUtils.hasLength(typeAttr)) {
            valueHolder.setType(typeAttr);
          }
          if (StringUtils.hasLength(nameAttr)) {
            valueHolder.setName(nameAttr);
          }
          valueHolder.setSource(extractSource(ele));
          bd.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
			}
			finally {
					this.parseState.pop();
			}
		}
}
```

- 如果配置中指定了index属性,操作步骤如下

  (1) 解析constructor-arg的子元素

  (2) 使用ConstructorArgumentValues.ValueHolder类型来封装解析出来的元素

  (3) 将type/name/index属性一并封装在CV类型中, 并添加至当前BeanDefinition的ConstructorArgumentValues的indexedArgumentValues属性中.

- 如果配置中没有指定index属性,操作步骤如下

  (1) 解析constructor-arg的子元素

  (2) 使用ConstructorArgumentValues.ValueHolder类型来封装解析出来的元素

  (3) 将type/name/index属性一并封装在CV类型中, 并添加至当前BeanDefinition的ConstructorArgumentValues的genericArgumentValues属性中.

针对是否制定index属性来讲, 关键在于信息被保存的位置.

```java
public Object parsePropertyValue(Element ele, BeanDefinition bd, @Nullable String propertyName) {
		String elementName = (propertyName != null ?
				"<property> element for property '" + propertyName + "'" :
				"<constructor-arg> element");

		// Should only have one child element: ref, value, list, etc.
		// 一个属性只能对应一种类型: ref, value, list, etc.
		NodeList nl = ele.getChildNodes();
		Element subElement = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			// 对应description 或者 meta不处理
			if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT) &&
					!nodeNameEquals(node, META_ELEMENT)) {
				// Child element is what we're looking for.
				if (subElement != null) {
					error(elementName + " must not contain more than one sub-element", ele);
				}
				else {
					subElement = (Element) node;
				}
			}
		}

		// 解析ref属性
		boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE);
		// 解析value属性
		boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);
		if ((hasRefAttribute && hasValueAttribute) ||
				((hasRefAttribute || hasValueAttribute) && subElement != null)) {
			/**
			 *  在constructor-arg上不存在：
			 *  	1. 同时有ref和value属性
			 *  	2. 存在ref属性或者value又有子元素
			 */
			error(elementName +
					" is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
		}

		if (hasRefAttribute) {
			// ref属性的处理，使用RuntimeBeanReference封装此信息
			String refName = ele.getAttribute(REF_ATTRIBUTE);
			if (!StringUtils.hasText(refName)) {
				error(elementName + " contains empty 'ref' attribute", ele);
			}
			RuntimeBeanReference ref = new RuntimeBeanReference(refName);
			ref.setSource(extractSource(ele));
			return ref;
		}
		else if (hasValueAttribute) {
			// value属性的处理， 使用TypedStringValue封装
			TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
			valueHolder.setSource(extractSource(ele));
			return valueHolder;
		}
		else if (subElement != null) {
			// 解析子元素
			return parsePropertySubElement(subElement, bd);
		}
		else {
			// Neither child element nor "ref" or "value" attribute found.
			error(elementName + " must specify a ref or value", ele);
			return null;
		}
}
```

> 从代码看来, 对构造参数属性的解析, 经历了一下以下几个过程
>
> (1) 略过desciption或者meta
>
> (2) 提取constructor-arg上的ref和value属性, 以便于根据规则验证正确性
>
> (3) ref属性的处理, 使用RuntimeBeanReference封装对应的ref名称
>
> (4) value属性的处理, 使用TypeStringValue封装
>
> (5) 子元素的处理, 以下针对子元素的parsePropertySubElement方法进行分类处理

```java
public Object parsePropertySubElement(Element ele, @Nullable BeanDefinition bd, @Nullable String defaultValueType) {
		if (!isDefaultNamespace(ele)) {
				return parseNestedCustomElement(ele, bd);
		}
		// 以bean开头的
		else if (nodeNameEquals(ele, BEAN_ELEMENT)) {
        BeanDefinitionHolder nestedBd = parseBeanDefinitionElement(ele, bd);
        if (nestedBd != null) {
          nestedBd = decorateBeanDefinitionIfRequired(ele, nestedBd, bd);
        }
        return nestedBd;
		}
		// 解析对应ref引用
		else if (nodeNameEquals(ele, REF_ELEMENT)) {
        // A generic reference to any name of any bean.
        String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
        boolean toParent = false;
        if (!StringUtils.hasLength(refName)) {
          // A reference to the id of another bean in a parent context.
          refName = ele.getAttribute(PARENT_REF_ATTRIBUTE);
          toParent = true;
          if (!StringUtils.hasLength(refName)) {
            error("'bean' or 'parent' is required for <ref> element", ele);
            return null;
          }
        }
        if (!StringUtils.hasText(refName)) {
          error("<ref> element contains empty target attribute", ele);
          return null;
        }
        RuntimeBeanReference ref = new RuntimeBeanReference(refName, toParent);
        ref.setSource(extractSource(ele));
        return ref;
		}
		// 对idref的解析
		else if (nodeNameEquals(ele, IDREF_ELEMENT)) {
				return parseIdRefElement(ele);
		}
		// 对value的解析
		else if (nodeNameEquals(ele, VALUE_ELEMENT)) {
				return parseValueElement(ele, defaultValueType);
		}
		// 对null元素的解析
		else if (nodeNameEquals(ele, NULL_ELEMENT)) {
        // It's a distinguished null value. Let's wrap it in a TypedStringValue
        // object in order to preserve the source location.
        TypedStringValue nullHolder = new TypedStringValue(null);
        nullHolder.setSource(extractSource(ele));
        return nullHolder;
		}
		// 对array的解析
		else if (nodeNameEquals(ele, ARRAY_ELEMENT)) {
				return parseArrayElement(ele, bd);
		}
		// 对list的解析
		else if (nodeNameEquals(ele, LIST_ELEMENT)) {
				return parseListElement(ele, bd);
		}
		// 对set的解析
		else if (nodeNameEquals(ele, SET_ELEMENT)) {
				return parseSetElement(ele, bd);
		}
		// 对map的解析
		else if (nodeNameEquals(ele, MAP_ELEMENT)) {
				return parseMapElement(ele, bd);
		}
		// 对prop对解析
		else if (nodeNameEquals(ele, PROPS_ELEMENT)) {
				return parsePropsElement(ele);
		}
		else {
				error("Unknown property sub-element: [" + ele.getNodeName() + "]", ele);
				return null;
		}
}
```

#### 7. 解析子元素property

```java
public void parsePropertyElement(Element ele, BeanDefinition bd) {
		// 获取配置文件中的name属性值
		String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
		if (!StringUtils.hasLength(propertyName)) {
				error("Tag 'property' must have a 'name' attribute", ele);
				return;
		}
		this.parseState.push(new PropertyEntry(propertyName));
		try {
			// 不允许多次对统一属性进行配置
			if (bd.getPropertyValues().contains(propertyName)) {
					error("Multiple 'property' definitions for property '" + propertyName + "'", ele);
					return;
			}
			// 此方法在构造方法时已经解读过, 这里propertyName是实际值, 而构造方法是null
			Object val = parsePropertyValue(ele, bd, propertyName);
			PropertyValue pv = new PropertyValue(propertyName, val);
			parseMetaElements(ele, pv);
			pv.setSource(extractSource(ele));
			bd.getPropertyValues().addPropertyValue(pv);
		}
		finally {
				this.parseState.pop();
		}
}
```

#### 8. 解析子元素qualifier

这个元素更多情况是在注解的情况下使用,   正常的spring容器中匹配的候选Bean数目必须有且仅有一个, 当不匹配的是就需要制定

### 3.1.2 AbstractBeanDefinition属性

> xml中所有的的配置都可以在GenericBeanDefinition的实例类中找到对应的配置, 而GenericBean只是一个子类实现, 真正的通用属性都保存在了AbstractBeanDefinition中

```java
public abstract class AbstractBeanDefinition extends BeanMetadataAttributeAccessor
		implements BeanDefinition, Cloneable {

    @Nullable
    private volatile Object beanClass;

    // bean的作用范围， 对应bean的scope属性
    @Nullable
    private String scope = SCOPE_DEFAULT;

    // 是否抽象
    private boolean abstractFlag = false;

    // 是否延迟加载
    private boolean lazyInit = false;

    // 自动注入模式
    private int autowireMode = AUTOWIRE_NO;

    // 依赖检查
    private int dependencyCheck = DEPENDENCY_CHECK_NONE;

    @Nullable
    private String[] dependsOn;

    // 当autowireCandidate为false时， 这样容器在查找自动装配对象时，
    // 不会考虑该bean， 但是该bean还是可以使用自动装配注入其它bean
    private boolean autowireCandidate = true;

    // 自动装配bean时有多个候选者，优先选用这个注入
    private boolean primary = false;

    private final Map<String, AutowireCandidateQualifier> qualifiers = new LinkedHashMap<>();

    @Nullable
    private Supplier<?> instanceSupplier;

    // 允许访问非公开的构造器和方法
    private boolean nonPublicAccessAllowed = true;

    private boolean lenientConstructorResolution = true;

    @Nullable
    private String factoryBeanName;

    @Nullable
    private String factoryMethodName;

    @Nullable
    private ConstructorArgumentValues constructorArgumentValues;

    @Nullable
    private MutablePropertyValues propertyValues;

    @Nullable
    private MethodOverrides methodOverrides;

    @Nullable
    private String initMethodName;

    @Nullable
    private String destroyMethodName;

    private boolean enforceInitMethod = true;

    private boolean enforceDestroyMethod = true;

    // 是否是用户定义的 而不是应用程序本身定义的， 创建aop时为true
    private boolean synthetic = false;

    // 默认是用户，
    // 用户 | 某些复杂配置的一部分 | 完全内部使用，与用户无关
    private int role = BeanDefinition.ROLE_APPLICATION;

    // bean的描述信息
    @Nullable
    private String description;

    // bean定义的资源
    @Nullable
    private Resource resource;
}
```

### 3.1.3 解析默认标签中的自定义标签元素

```java
bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
```

> 当spring中的bean使用的是默认的标签配置, 但是其中子元素却使用了自定义的配置时, 这句代码便会起作用.

```java
public BeanDefinitionHolder decorateIfRequired(
			Node node, BeanDefinitionHolder originalDef, @Nullable BeanDefinition containingBd) {

		// 获取自定义标签的命名空间
		String namespaceUri = getNamespaceURI(node);
		// 对于非默认标签进行修饰
		if (namespaceUri != null && !isDefaultNamespace(namespaceUri)) {
			// 根据对应的命名空间找到对应的处理器
			NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
			if (handler != null) {
				// 进行修饰
				BeanDefinitionHolder decorated =
						handler.decorate(node, originalDef, new ParserContext(this.readerContext, this, containingBd));
				if (decorated != null) {
					return decorated;
				}
			}
			else if (namespaceUri.startsWith("http://www.springframework.org/")) {
				error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", node);
			}
			else {
				// A custom namespace, not to be handled by Spring - maybe "xml:...".
				if (logger.isDebugEnabled()) {
					logger.debug("No Spring NamespaceHandler found for XML schema namespace [" + namespaceUri + "]");
				}
			}
		}
		return originalDef;
}
```

### 3.1.4 注册解析的BeanDefinition

> 总结一下: bean标签的解析有四个步骤, 对于配置文件就是
> 解析-》 装饰-》 注册-》 通知-》
> 这里说的就是注册
> BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());

```java
public static void registerBeanDefinition(
			BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		// Register bean definition under primary name.
		// 使用beanName标示做唯一注册
		String beanName = definitionHolder.getBeanName();
		registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

		// Register aliases for bean name, if any.
		// 注册所有别名
		String[] aliases = definitionHolder.getAliases();
		if (aliases != null) {
        for (String alias : aliases) {
          registry.registerAlias(beanName, alias);
        }
		}
}
```

主要步骤就是注册到BeanDefinitionRegistry类型的实例registry中, 对于BeanDefinition的注册分为了两部分

#### 1. 通过beanName注册BeanDefinition

```java
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");

		if (beanDefinition instanceof AbstractBeanDefinition) {
			try {
				/**
				 *  注册前的最后一次校验，这里的校验不同与之前的XML检验
				 *  主要是对于AbstractBeanDefinition属性中的methodOverrides校验，
				 *  校验methodOverrides是否与工厂方法并存或者methodOverrides对应的方法根本不存在
				 */
				((AbstractBeanDefinition) beanDefinition).validate();
			}
			catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Validation of bean definition failed", ex);
			}
		}

		// 这里是4.1.2 版本后的新特性， 如果没有开启allowBeanDefinitionOverriding 并且bean已经存在抛出异常
		BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
		if (existingDefinition != null) {
			if (!isAllowBeanDefinitionOverriding()) {
				throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
			}
			else if (existingDefinition.getRole() < beanDefinition.getRole()) {
				// e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
				if (logger.isInfoEnabled()) {
					logger.info("Overriding user-defined bean definition for bean '" + beanName +
							"' with a framework-generated bean definition: replacing [" +
							existingDefinition + "] with [" + beanDefinition + "]");
				}
			}
			else if (!beanDefinition.equals(existingDefinition)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Overriding bean definition for bean '" + beanName +
							"' with a different definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Overriding bean definition for bean '" + beanName +
							"' with an equivalent definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			this.beanDefinitionMap.put(beanName, beanDefinition);
		}
		else {
			// 因为beanDefinitionMap是全局变量，所以会存在并发访问的情况
			if (hasBeanCreationStarted()) {// 4.0新特性 检查工厂的bean创建阶段是否已经开始，即是否有任何bean被标记为同时创建。

				// Cannot modify startup-time collection elements anymore (for stable iteration)
				// 不能再修改启动时间集合元素(为了稳定的迭代)
				synchronized (this.beanDefinitionMap) {
					// TODO 疑问, 这里没有直接add, 而是new 一个List去覆盖旧的意义是什么
          // 解答： 防止jdk暴力扩容， 节省空间
					this.beanDefinitionMap.put(beanName, beanDefinition);
					List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
					updatedDefinitions.addAll(this.beanDefinitionNames);
					updatedDefinitions.add(beanName);
					this.beanDefinitionNames = updatedDefinitions;
					removeManualSingletonName(beanName);
				}
			}
			else {
				// Still in startup registration phase
				// 仍在启动注册阶段
				this.beanDefinitionMap.put(beanName, beanDefinition);
				this.beanDefinitionNames.add(beanName);
				removeManualSingletonName(beanName);
			}
			this.frozenBeanDefinitionNames = null;
		}

		if (existingDefinition != null || containsSingleton(beanName)) {
			// 重置所有beanName对应的缓存
			resetBeanDefinition(beanName);
		}
}
```

#### 2. 通过别名注册BeanDefinition

```java
public void registerAlias(String name, String alias) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.hasText(alias, "'alias' must not be empty");
		synchronized (this.aliasMap) {
			// 如果name与alias相同的话不记录, 并删除对应的alias
			if (alias.equals(name)) {
				this.aliasMap.remove(alias);
				if (logger.isDebugEnabled()) {
					logger.debug("Alias definition '" + alias + "' ignored since it points to same name");
				}
			}
			else {
				String registeredName = this.aliasMap.get(alias);
				if (registeredName != null) {
					// 如果aliasMap中存在已经注册的，则结束
					if (registeredName.equals(name)) {
						// An existing alias - no need to re-register
						return;
					}
					// 如果alias不允许被覆盖则抛出异常
					if (!allowAliasOverriding()) {
						throw new IllegalStateException("Cannot define alias '" + alias + "' for name '" +
								name + "': It is already registered for name '" + registeredName + "'.");
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Overriding alias '" + alias + "' definition for registered name '" +
								registeredName + "' with new target name '" + name + "'");
					}
				}
				// 当A-》B存在时，若再次出现A-》C-》B时候则会抛出异常
				checkForAliasCircle(name, alias);
				this.aliasMap.put(alias, name);
				if (logger.isTraceEnabled()) {
					logger.trace("Alias definition '" + alias + "' registered for name '" + name + "'");
				}
			}
		}
}
```

### 3.1.5 通知监听器解析及注册完成

```java
getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
```

spring没有对此方法进行实现, 程序开发人员需要对注册BeanDefinition事件进行监听时可以通过注册监听器的方式并将逻辑写入.



## 3.2 alias标签的解析

关于别名的使用

```xml
<bean id="myBean" name="myBean2,myBean3" class="bean.MyBean" >
   <meta key="speed" value="12ms"/>
</bean>
<alias name="myBean" alias="aliasMyBean"/>
```

```java
protected void processAliasRegistration(Element ele) {
		// 获取name
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		// 获取alias
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		if (valid) {
			try {
				// 注册alias
				getReaderContext().getRegistry().registerAlias(name, alias);
			}
			catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			// 别名注册后通知监听器做相应处理
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
}
```

## 3.3 import标签的解析

对于配置文件超级大可以采用分离配置比如 <import resource="spring-aop.xml"/>

```java
protected void importBeanDefinitionResource(Element ele) {
		// 获取resource 属性
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		// Resolve system properties: e.g. "${user.dir}"
		// 解析系统属性格式 例如. ${user.dir}
		location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<>(4);

		// Discover whether the location is an absolute or relative URI
		// 判定location是绝对路径还是相对路径
		boolean absoluteLocation = false;
		try {
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		}
		catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// Absolute or relative?
		if (absoluteLocation) {
			try {
				// 如果是绝对url则直接根据地址加载对应的配置文件
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		}
		else {
			// No URL -> considering resource location as relative to the current file.
			// 如果是相对路径则根据相对路径地址推出绝对路径
			try {
				int importCount;
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				if (relativeResource.exists()) {
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				}
				else {
					String baseLocation = getReaderContext().getResource().getURL().toString();
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			}
			catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from relative location [" + location + "]", ele, ex);
			}
		}
		// 解析后进行监听器激活处理
		Resource[] actResArray = actualResources.toArray(new Resource[0]);
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
}
```

## 3.4 嵌入式beans标签的解析

```
<beans> 
 		<aop:aspectj-autoproxy />
   	<bean id="aopBean" class="bean.aop.AopBean"/>
   	<beans></beans>
</beans>
```

这里的实现逻辑和单独的配置文件没有区别, 练方法都是公用 protected void doRegisterBeanDefinitions(Element root);



# 第四章 自定义标签的解析

暂时跳过



# 第五章 bean的加载

对于加载bean的功能, 在spring中的调用方式为: 

```java
Car card4 = (Car) beanFactory.getBean("car4");
```

```java
protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
			@Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {

		// 提取对应的beanName
		final String beanName = transformedBeanName(name);
		Object bean;

		// Eagerly check singleton cache for manually registered singletons.
		/**
		 *  检查缓存中或者实例工厂中是否有对应的实例
		 *	创建单例bean的时候会存在依赖注入的情况， 而在创建依赖的时候， 要避免循环依赖
		 *	spring创建bean的原则是不等bean创建完成就会将创建的bean的ObjectFactory提早曝光
		 *	也就是ObjectFactory加入到缓存中， 一旦下个bean创建是需要依赖上个bean则直接使用ObjectFactory
		 */
		Object sharedInstance = getSingleton(beanName);
		if (sharedInstance != null && args == null) {
			if (logger.isTraceEnabled()) {
				if (isSingletonCurrentlyInCreation(beanName)) {
					logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
							"' that is not fully initialized yet - a consequence of a circular reference");
				}
				else {
					logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
				}
			}
			// 返回对应的实例， 有时候存在诸如BeanFactory的情况并不是直接返回实例本身而是返回指定方法返回的实例
			bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		}

		else {
			// Fail if we're already creating this bean instance:
			// We're assumably within a circular reference.

			// 失败，如果我们已经创建了这个bean实例:
			// 我们假设在一个循环引用中。

			/**
			 *  只有在单例情况下才会尝试解决循环依赖，原型模式情况下，如果存在
			 *  A中有B的属性，B中有A的属性，那么当依赖注入的时候，就会产生当A还未创建完的时候因为
			 *  对于B的创建再次返回创建A，造成循环依赖也就是下面这种情况
			 */
			if (isPrototypeCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(beanName);
			}

			// Check if bean definition exists in this factory.
			// 如果BeanDefinitionMap中就是在所有已经加载的类中不包括beanName则尝试从parentBeanFactory中检测
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// Not found -> check parent.
				String nameToLookup = originalBeanName(name);
				if (parentBeanFactory instanceof AbstractBeanFactory) {
					return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
							nameToLookup, requiredType, args, typeCheckOnly);
				}
				else if (args != null) {
					// Delegation to parent with explicit args.
					return (T) parentBeanFactory.getBean(nameToLookup, args);
				}
				else if (requiredType != null) {
					// No args -> delegate to standard getBean method.
					return parentBeanFactory.getBean(nameToLookup, requiredType);
				}
				else {
					return (T) parentBeanFactory.getBean(nameToLookup);
				}
			}

			// 如果不是仅仅做类型检查则是创建bean， 这里要进行记录
			if (!typeCheckOnly) {
				markBeanAsCreated(beanName);
			}

			try {
				// 将存储XML配置文件的GenericBeanDefinition转化为RootBeanDefinition，
				// 如果指定beanName是子bean的话同时会合并父类的相关属性
				final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				checkMergedBeanDefinition(mbd, beanName, args);

				// Guarantee initialization of beans that the current bean depends on.
				String[] dependsOn = mbd.getDependsOn();
				// 若存在依赖，则需要实例化依赖的bean
				if (dependsOn != null) {
					for (String dep : dependsOn) {
						if (isDependent(beanName, dep)) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
						}
						// 缓存依赖调用
						registerDependentBean(dep, beanName);
						try {
							getBean(dep);
						}
						catch (NoSuchBeanDefinitionException ex) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"'" + beanName + "' depends on missing bean '" + dep + "'", ex);
						}
					}
				}

				// Create bean instance.
				// 实例化依赖的bean后便可以实例化mbd本身了
				if (mbd.isSingleton()) {
					sharedInstance = getSingleton(beanName, () -> {
						try {
							return createBean(beanName, mbd, args);
						}
						catch (BeansException ex) {
							// Explicitly remove instance from singleton cache: It might have been put there
							// eagerly by the creation process, to allow for circular reference resolution.
							// Also remove any beans that received a temporary reference to the bean.
							destroySingleton(beanName);
							throw ex;
						}
					});
					bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
				}

				else if (mbd.isPrototype()) {
					// It's a prototype -> create a new instance.
					// prototype模式的创建（new）
					Object prototypeInstance = null;
					try {
						beforePrototypeCreation(beanName);
						prototypeInstance = createBean(beanName, mbd, args);
					}
					finally {
						afterPrototypeCreation(beanName);
					}
					bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
				}

				else {
					// 指定的scope上实例化bean
					String scopeName = mbd.getScope();
					final Scope scope = this.scopes.get(scopeName);
					if (scope == null) {
						throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
					}
					try {
						Object scopedInstance = scope.get(beanName, () -> {
							beforePrototypeCreation(beanName);
							try {
								return createBean(beanName, mbd, args);
							}
							finally {
								afterPrototypeCreation(beanName);
							}
						});
						bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
					}
					catch (IllegalStateException ex) {
						throw new BeanCreationException(beanName,
								"Scope '" + scopeName + "' is not active for the current thread; consider " +
								"defining a scoped proxy for this bean if you intend to refer to it from a singleton",
								ex);
					}
				}
			}
			catch (BeansException ex) {
				cleanupAfterBeanCreationFailure(beanName);
				throw ex;
			}
		}

		// Check if required type matches the type of the actual bean instance.
		// 检查需要的类型是否符合bean的实际类型
		if (requiredType != null && !requiredType.isInstance(bean)) {
			try {
				T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
				if (convertedBean == null) {
					throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
				}
				return convertedBean;
			}
			catch (TypeMismatchException ex) {
				if (logger.isTraceEnabled()) {
					logger.trace("Failed to convert bean '" + name + "' to required type '" +
							ClassUtils.getQualifiedName(requiredType) + "'", ex);
				}
				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
			}
		}
		return (T) bean;
}
```

具体的逻辑步骤

1. 转换对应的beanName 
   因为传入的参数name可能是别名, 也可以是FactoryBean, 所以需要进行一系列的解析, 解析的内容包括
   * 去除FactoryBean的修饰符, 也就是如果name="&aa", 那么会首先出去&而是name=aa
   * 取指定alias所标示的最终beanName, 例如如果A指向名称为B的bean则返回B, 若别名A指向别名B, 别名B又指向别名C的bean, 则返回C
2. 尝试从缓存中加载单例
3. bean的实例化
   如果从缓存中得到bean的原始状态, 则需要对bean进行实例化, 这里有必要强调一下, 缓存中记录的只是最原始的bean状态, 并不一定是我们最终想要的bean.
4. 原型模式的依赖检查
   这里就是单例情况下的循环依赖问题, 如果存在A中对B的属性, B中有A的属性, 就是isPrototypeCurrentlyInCreation(beanName)的处理
5. 检测parentBeanFactory
6. 将存储XML配置文件的GenericBeanDefinition转换为RootBeanDefinition
7. 寻找依赖
8. 针对不同的scope进行bean的创建
9. 类型转换



## 5.1 FactoryBean的使用

spring通过反射机制利用bean的class属性指定实现类来实例化bean. 用户可以通过实现该接口定制实例化bean的逻辑.

```java
package org.springframework.beans.factory;
public interface FactoryBean<T> {

    @Nullable
  	// 返回由FactoryBean创建的bean实例, 如果isSingleton实例返回true,则该实例会放到Spring容器中单实力缓存池中
    T getObject() throws Exception;

    @Nullable
  	// 返回FactoryBean创建的bean类型
  	// 当属性文件中<bean>的class属性配置的实现是FactoryBean时, 通过getBean()方法返回的不是FactoryBean本身, 而是Factory#getObject()方法所返回的对象. 相当于FactoryBean#getObject代理啦getBean()方法
    Class<?> getObjectType();

  	// 返回由FactoryBean创建的bean实例的作用域是singleton还是prototype
    default boolean isSingleton() {
      	return true;
    }
}
```

## 5.2 缓存中获取单例bean

单例的bean在spring的容器中只会被创建一次, 后续再获取bean直接从单例缓存中获取,  当然这里也只是尝试加载, 首先尝试从缓存中加载, 然后再次尝试从singletonFactories中加载. 因为创建依赖的时候为了避免循环依赖, 会将bean提取曝光在ObjectFactory的缓存中.

```java
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);
```

```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		Object singletonObject = this.singletonObjects.get(beanName);
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			// 缓存中不存在， 并且此bean正在创建，
			// 锁定全局进行处理
			synchronized (this.singletonObjects) {
				// 如果此bean正在加载， 则不处理
				singletonObject = this.earlySingletonObjects.get(beanName);
				if (singletonObject == null && allowEarlyReference) {// 允许早期依赖， 且没有加载
            ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
            if (singletonFactory != null) {
                // 调用预先设定的getObject方法
                singletonObject = singletonFactory.getObject();
                // 记录在早期单例对象的缓存中
                this.earlySingletonObjects.put(beanName, singletonObject);
                // 两个缓存互斥
                this.singletonFactories.remove(beanName);
            }
				}
			}
		}
		return singletonObject;
}
```

这个方法涉及循环依赖的检测, 以及设计很多变量的记录存取. 这个方法尝试从singletonObject里面获取实例, 如果获取不到再从earlySingletonObjects里面获取,  如果还获取不到, 再尝试从singletonFactories里面获取beanName对应的ObjectFactory, 然后调用这个ObjectFactory的getObject来创建bean, 并放到earleSingletonObjects里面去, 并且从singletonFactonFacotories里面remove掉这个ObjectFactory, 而对于后续的所有内存操作都是为了循环检测时候使用,也就是allowEarlyReference为ture的情况下才会使用.

## 5.3 从bean的实例中获取对象

> 检测当前bean是否是factoryBean类型的bean, 如果是, 那么需要调用该bean对应的factoryBean实例中最难过的getObject作为返回值

举例说明:  我们从缓存中获取到的bean是通过不同的scope策略加载的bean都只是从最原始的bean状态,  并不一定是我们最终想要的,  假设我们需要对工厂bean进行处理, 那么这里得到的其实是工厂bean的初始状态, 但是我们真正需要的是工厂bean中定义的factory-method方法中返回的bean, 而getObjectForBeanInstance方法就是完成这个工作.

```java
protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

		// Don't let calling code try to dereference the factory if the bean isn't a factory.
		// 如果bean不是工厂，不要让调用代码尝试取消对工厂的引用。
		if (BeanFactoryUtils.isFactoryDereference(name)) {
			if (beanInstance instanceof NullBean) {
				return beanInstance;
			}
			if (!(beanInstance instanceof FactoryBean)) {
				throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
			}
		}

		// Now we have the bean instance, which may be a normal bean or a FactoryBean.
		// If it's a FactoryBean, we use it to create a bean instance, unless the
		// caller actually wants a reference to the factory.

		// 现在我们有了bean实例，它可以是一个普通的bean，也可以是一个FactoryBean。
		// 如果它是一个FactoryBean，我们使用它来创建一个bean实例，除非
		// 呼叫者实际上想要一个工厂的参考。
		if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
			return beanInstance;
		}

		// 加载beanFactory
		Object object = null;
		if (mbd == null) {
			// 尝试从缓存中加载bean
			object = getCachedObjectForFactoryBean(beanName);
		}
		if (object == null) {
			// Return bean instance from factory.
			// 这里已经明确是FactoryBean类型
			FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
			// Caches object obtained from FactoryBean if it is a singleton.
			// containsBeanDefinition检测beanDefinitionMap就是在所有已经加载的类中检测是否定义beanName
			if (mbd == null && containsBeanDefinition(beanName)) {
				// 将存储XML配置文件的GenericBeanDefinition转化为RootBeanDefinition，
				// 如果指定beanName是子bean的话同时会合并父类的相关属性
				mbd = getMergedLocalBeanDefinition(beanName);
			}
			// 是否用户定义的而不是程序本身定义的
			boolean synthetic = (mbd != null && mbd.isSynthetic());
			object = getObjectFromFactoryBean(factory, beanName, !synthetic);
		}
		return object;
}
```

核心的代码是委托给getObjectFromFactoryBean

1. 对factoryBean正确性的检验
2. 对非factoryBean不做任何处理
3. 对bean进行转换
4. 将从factory中解析bean 的工作委托给getObjectFromFactoryBean

```java
protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
		// 如果是单例模式
		if (factory.isSingleton() && containsSingleton(beanName)) {
			synchronized (getSingletonMutex()) {
				Object object = this.factoryBeanObjectCache.get(beanName);
				// 双重校验锁
				if (object == null) {
					object = doGetObjectFromFactoryBean(factory, beanName);
					// Only post-process and store if not put there already during getObject() call above
					// (e.g. because of circular reference processing triggered by custom getBean calls)
					//在调用getObject()的时候，只有在处理和存储的时候才会用到
					//(例如，由于自定义getBean调用触发的循环引用处理)
					Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
					if (alreadyThere != null) {
						object = alreadyThere;
					}
					else {
						if (shouldPostProcess) {
							if (isSingletonCurrentlyInCreation(beanName)) {
								// Temporarily return non-post-processed object, not storing it yet..
								return object;
							}
							beforeSingletonCreation(beanName);
							try {
								object = postProcessObjectFromFactoryBean(object, beanName);
							}
							catch (Throwable ex) {
								throw new BeanCreationException(beanName,
										"Post-processing of FactoryBean's singleton object failed", ex);
							}
							finally {
								afterSingletonCreation(beanName);
							}
						}
						if (containsSingleton(beanName)) {
							this.factoryBeanObjectCache.put(beanName, object);
						}
					}
				}
				return object;
			}
		}
		else {
        Object object = doGetObjectFromFactoryBean(factory, beanName);
        if (shouldPostProcess) {
            try {
              	object = postProcessObjectFromFactoryBean(object, beanName);
            }
            catch (Throwable ex) {
              	throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
            }
        }
        return object;
		}
}
```

```java
private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName)
			throws BeanCreationException {

		Object object;
		try {
			// 需要校验权限
			if (System.getSecurityManager() != null) {
				AccessControlContext acc = getAccessControlContext();
				try {
					object = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) factory::getObject, acc);
				}
				catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			}
			else {
				// 直接调用getObject方法
				object = factory.getObject();
			}
		}
		catch (FactoryBeanNotInitializedException ex) {
			throw new BeanCurrentlyInCreationException(beanName, ex.toString());
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
		}

		// Do not accept a null value for a FactoryBean that's not fully
		// initialized yet: Many FactoryBeans just return null then.
		if (object == null) {
			if (isSingletonCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(
						beanName, "FactoryBean which is currently in creation returned null from getObject");
			}
			object = new NullBean();
		}
		return object;
}
```

还需要了解一个规则: 尽可能保证所有的bean初始化后都会调用注册的BeanPostProcessor的postProcessAfterInitialization方法进行处理, 在实际开发过程中大可以针对此特性设计自己的业务逻辑.

## 5.4 获取单例

具体及处理操作包括的内容

1. 检查缓存是否已经加载过
2. 若没有加载, 则记录beanname的正在加载状态
3. 加载单例前记录加载状态
4. 通过调用参数传入的objectFactory的个体object方法实例化bean
5. 加载单例后的处理方法调用
6. 将结果记录至缓存并删除bean过程中所记录的各种辅助状态
7. 返回处理结果

```java
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");
		// 全局变量同步
		synchronized (this.singletonObjects) {
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) {
				// 首先检查对应的bean是否已经加载过，因为singleton就是复用以前创建的bean
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				// 将要创建的bean记录在缓存中， 这样便可以对缓存依赖进行检测
				beforeSingletonCreation(beanName);
				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
					// 初始化bean
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					// 将创建完成的bean在缓存中移除， 这样便可以对缓存依赖进行检测
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
					// 加入缓存
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
}
```

## 5.5 准备创建bean

具体步骤包括

1. 根据设置的class属性或者根据className来解析class
2. 对override属性进行标记及验证(其实就是lookup-method, replace-method),  就是将这两个配置统一存放在AbstractBeanDefinition中的MethodOverrides属性中
3. 应用初始化前的后处理器, 解析指定bean是否存在初始化前的短路操作
4. 创建bean

```java
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException {

		if (logger.isTraceEnabled()) {
			logger.trace("Creating instance of bean '" + beanName + "'");
		}
		RootBeanDefinition mbdToUse = mbd;

		// Make sure bean class is actually resolved at this point, and
		// clone the bean definition in case of a dynamically resolved Class
		// which cannot be stored in the shared merged bean definition.

		// 确保此时bean类已经解析，并且
		// 在动态解析类的情况下，克隆bean定义
		// 不能存储在共享的合并bean定义中。

		// 锁定class， 根据设置的class属性或者根据className来解析class
		Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
		if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
			mbdToUse = new RootBeanDefinition(mbd);
			mbdToUse.setBeanClass(resolvedClass);
		}

		// Prepare method overrides.
		// 验证及准备覆盖的方法
		try {
			mbdToUse.prepareMethodOverrides();
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
					beanName, "Validation of method overrides failed", ex);
		}

		try {
			// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
			// 给BeanPostProcessors一个机会来返回代理， 替换真正的实例
			Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
			if (bean != null) {
				return bean;
			}
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
					"BeanPostProcessor before instantiation of bean failed", ex);
		}

		try {
			Object beanInstance = doCreateBean(beanName, mbdToUse, args);
			if (logger.isTraceEnabled()) {
				logger.trace("Finished creating instance of bean '" + beanName + "'");
			}
			return beanInstance;
		}
		catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
			// A previously detected exception with proper bean creation context already,
			// or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
		}
}
```

### 5.5.1 处理override属性

```java
protected void prepareMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
		// 获取对应类中对应方法名的个数
		int count = ClassUtils.getMethodCountForName(getBeanClass(), mo.getMethodName());
		if (count == 0) {
			throw new BeanDefinitionValidationException(
					"Invalid method override: no method with name '" + mo.getMethodName() +
					"' on class [" + getBeanClassName() + "]");
		}
		else if (count == 1) {
			// Mark override as not overloaded, to avoid the overhead of arg type checking.
			// 标记MethodOverride暂未被覆盖， 避免参数检查的开销
			mo.setOverloaded(false);
		}
}
```

如果一个类中存在若干个重载方法,那么, 在函数调用及增强的时候还需要根据参数类型进行匹配. 来最终确认当前调用的到底是哪个函数, 但是, spring将一部分匹配工作在这里完成了, 如果当前类中的方法只有一个, 那么就设置重载该方法没有被重载,  这样在后续调用的时候便可以直接使用找到的方法, 而不需要进行方法的参数匹配验证了, 而且还可以提前对方法存在性进行验证, 正可谓一箭双雕.

### 5.5.2 实例化的前置处理

```java
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
		Object bean = null;
		// 如果尚未被解析
		if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
			// Make sure bean class is actually resolved at this point.
			if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
				Class<?> targetType = determineTargetType(beanName, mbd);
				if (targetType != null) {
					bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
					if (bean != null) {
						bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
					}
				}
			}
			mbd.beforeInstantiationResolved = (bean != null);
		}
		return bean;
}
```

#### 1. 实例化前的后处理器应用

bean的实例化前调用, 也就是将ABstractBeanDefinition转化为BeanWrapper前的处理. 给子类秀海BeanDefinition的机会, 也就是说当程序经过这个方法后, bean可能已经不是我们认为的bean, 而是或许成为了一个经过处理的代理bean, 可能是通过cglib生成的.

```java
protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
		for (BeanPostProcessor bp : getBeanPostProcessors()) {
			if (bp instanceof InstantiationAwareBeanPostProcessor) {
				InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
				Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
}
```



#### 2. 实例化后的后处理器应用

在bean初始化后尽可能保证将注册的后处理器的postProcessAfterInitalization方法应用到该bean中, 因为如果返回的bean不为空, 那么便不会再次经历普通bean的创建过程, 所以只能在这里应用后处理器的postProcessAfterInitalization.

```java
public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException {
		Object result = existingBean;
		for (BeanPostProcessor processor : getBeanPostProcessors()) {
			Object current = processor.postProcessAfterInitialization(result, beanName);
			if (current == null) {
				return result;
			}
			result = current;
		}
		return result;
}
```

## 5.6 循环依赖

### 5.6.1 什么是循环依赖

> 两个或多个bean互相之间的持有对方, 最终反映为一个环.
> 注意区分一个概念, 循环调用和循环依赖
> 循环调用是方法之间的调用, 无法解决, 除非有终结条件, 否则就是死循环, 最终导致内存溢出错误

### 5.6.2 Spring如何解决循环依赖

1. 构造器循环依赖
   表示通过构造器注入构成的循环依赖, 此依赖是无法解决的, 只能抛出BeanCurrentlyInCreationException异常表示循环依赖.

   Spring容器将每一个正在创建的bean标识符放在一个"当前创建bean池"中, bean标识符在创建过程中将一直保持在这个池中, 因此如果在创建bean过程中发现自己已经在"当前创建bean"里时, 将抛出BeanCurrentlyInCreationException异常表示循环依赖; 而对于创建完毕将从"当前创建bean池"中清除掉.

   ```java
   Caused by: org.springframework.beans.factory.BeanCurrentlyInCreationException: Error creating bean with name 'beanA': Requested bean is currently in creation: Is there an unresolvable circular reference?
   	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.beforeSingletonCreation(DefaultSingletonBeanRegistry.java:374)
   	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:242)
   	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:340)
   	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:199)
   	at org.springframework.beans.factory.support.BeanDefinitionValueResolver.resolveReference(BeanDefinitionValueResolver.java:303)
   
   ```

2. setter循环依赖
   表示通过setter注入方式构成的循环依赖. 

   > spring容器通过提前暴露刚完成构造器注入但为完成其他步骤的bean来完成的, 而且只能解决单例作用域的bean循环依赖.  [通过提前暴露的一个单例工厂方法, 从而使其它bean能引用到该bean.]

   addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));

   参考步骤: 

   (1)  spring 容器创建单例"testA" bean, 首先根据无参构造器创建bean, 并暴露一个"ObjectFactory" 用于返回一个提前暴露一个创建中的bean, 并将"testA"标识符放到"当前创建bean池", 然后进行setter注入testB

   (2)  spring容器创建单例"testB" bean, 首先根据无参构造器创建bean, 并暴露一个"ObjectFactory"  用于返回一个提前暴露一个创建的bean, 并将"testB"标识符放到"当前创建bean池",然后进行setter注入testC

   (3) spring容器创建单例"testC"bean, 首先根据无参构造函器创建bean, 并暴露一个"ObjectFactory" 用于返回一个提前暴露一个创建的bean, 并将"testC"标识符放到"当前创建bean池",然后进行setter注入"testA", 进行注入"testA" 时由于提前暴露了"ObjectFactory"工厂, 从而使用他返回提前暴露一个创建中的bean.

   (4) 最后在依赖注入"testB"和"testA", 完成setter注入

3. prototype范围的依赖处理
   对于"pritotype"作用域bean, spring容器无法完成依赖注入, 因为spring容器不进行缓存"prototype"作用于的bean, 因此无法提前暴露一个创建中的bean.



## 5.7 创建bean

概要思路:

(1) 如果是单例则需要首先清楚缓存

(2) 实例化bean, 将BeanDefinition转换为BeanWrapper, 转换时一个复杂的过程, 但是我们可以尝试概括大致的功能, 如下

 * 如果存在功能方法则使用工厂方法进行初始化
 * 一个类有多个构造函数, 每个构造函数都有不同的参数, 所以需要根据参数锁定构造函数并进行初始化
 * 如果既不存在工厂方法也不存在带有参数的构造函数, 则使用默认的构造函数进行bean的实例化

(3) MergedBeanDefinitionPostProcessors的应用

bean合并后的处理, Autowired注解正是通过此方法实现诸如类型的预解析

(4) 依赖处理
当创建B的时候, 涉及循环依赖, 就通过放入缓存中的objectFactory来创建实例, 这样就解决循环依赖的问题

(5) 属性填充. 将所有的属性填充至bean的实例中

(6) 循环依赖检查
这里的循环检查只对单例有效, 而对于prorotype的bean, spring没有好的解决办法, 唯一要做的就是抛出异常.

(7) 注册DisposableBean

如果配置了destroy-method,这里需要注册以便于在销毁的时候调用.

(8) 完成创建并返回.

```java
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
			throws BeanCreationException {

		// Instantiate the bean.
		BeanWrapper instanceWrapper = null;
		if (mbd.isSingleton()) {
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		}
		if (instanceWrapper == null) {
			// 根据对应bean使用对应的策略创建新的实例，如： 工厂方法，构造函数自动注入，简单初始化
			instanceWrapper = createBeanInstance(beanName, mbd, args);
		}
		final Object bean = instanceWrapper.getWrappedInstance();
		Class<?> beanType = instanceWrapper.getWrappedClass();
		if (beanType != NullBean.class) {
			mbd.resolvedTargetType = beanType;
		}

		// Allow post-processors to modify the merged bean definition.
		// 允许后处理程序修改合并的bean定义
		synchronized (mbd.postProcessingLock) {
			if (!mbd.postProcessed) {
				try {
					// 应用MergedBeanDefinitionPostProcessors
					applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Post-processing of merged bean definition failed", ex);
				}
				mbd.postProcessed = true;
			}
		}

		// Eagerly cache singletons to be able to resolve circular references
		// even when triggered by lifecycle interfaces like BeanFactoryAware.
		/**
		 *  是否需要提早曝光： 单例&允许循环依赖&当前bean正在创建中，检测循环依赖
		 */
		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));
		if (earlySingletonExposure) {
			if (logger.isTraceEnabled()) {
				logger.trace("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			/**
			 *  spring容器通过提前暴露刚完成构造器注入但为完成其他步骤的bean来完成的,
			 *  而且只能解决单例作用域的bean循环依赖.  [通过提前暴露的一个单例工厂方法,
			 *  从而使其它bean能引用到该bean.]
			 */
			// 这里就是为避免循环依赖， 提取将bean初始化放入ObjectFactory中
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}

		// Initialize the bean instance.
		Object exposedObject = bean;
		try {
			// 对bean进行填充， 将各个属性填充进去， 其中可能存在其它依赖于其它bean的属性，这里需要递归创建
			populateBean(beanName, mbd, instanceWrapper);
			// 调用初始化方法，比如init-method
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		}
		catch (Throwable ex) {
			if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
				throw (BeanCreationException) ex;
			}
			else {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
			}
		}

		if (earlySingletonExposure) {
			Object earlySingletonReference = getSingleton(beanName, false);
			// earlySingletonReference只有检测到有循环依赖的情况才会不为null
			if (earlySingletonReference != null) {
				// 如果exposedObject没有在初始化方法中被改变， 也就是没有被增强
				if (exposedObject == bean) {
					exposedObject = earlySingletonReference;
				}
				else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
					for (String dependentBean : dependentBeans) {
						// 依赖检测
						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
							actualDependentBeans.add(dependentBean);
						}
					}
					/**
					 *  因为bean创建后其所依赖的bean一定是已经创建的
					 *  actualDependentBeans不为空则表示当前的bean创建后其依赖的bean却没有全部创建完，
					 *  也就是存在循环引用
					 */
					if (!actualDependentBeans.isEmpty()) {
						throw new BeanCurrentlyInCreationException(beanName,
								"Bean with name '" + beanName + "' has been injected into other beans [" +
								StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
								"] in its raw version as part of a circular reference, but has eventually been " +
								"wrapped. This means that said other beans do not use the final version of the " +
								"bean. This is often the result of over-eager type matching - consider using " +
								"'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}

		// Register bean as disposable.
		try {
			// 根据scope创建bean
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}

		return exposedObject;
}
```

### 5.7.1 创建bean的实例

实例化的具体逻辑:
(1) 如果在RootBeanfinition中存在factoryMethodName属性, 或者说在配置文件中配置了factory-method, 那么spring会尝试使用instantiateUsingFactoryMethod(beanName, mid, args)方法根据RootBeanfinition中的配置生成bean的实例

(2) 解析构造函数并进行构造函数的实例化.  采用缓存机制, 如果已经解析过, 则不需要重复解析而是直接从RootBeanfintion中的属性resolvedConstructorOrFactoryMethod缓存中的值去取, 否在需要再次解析, 并将结果添加至RootBeanfinition的resolvedConstructorOrFactoryMethod中.

```java
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
		// Make sure bean class is actually resolved at this point.
		// 解析class
		Class<?> beanClass = resolveBeanClass(mbd, beanName);

		if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
		}

		// 返回创建bean实例的回调(如果有的话)。
		Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
		if (instanceSupplier != null) {
			return obtainFromSupplier(instanceSupplier, beanName);
		}

		// 如果工厂方法不为空， 则使用工厂方法初始化策略
		if (mbd.getFactoryMethodName() != null) {
			return instantiateUsingFactoryMethod(beanName, mbd, args);
		}

		// Shortcut when re-creating the same bean...
		boolean resolved = false;
		boolean autowireNecessary = false;
		if (args == null) {
			synchronized (mbd.constructorArgumentLock) {
				// 如果一个类有多个构造函数， 每个构造函数都有不同的参数， 所以调用前需要先根据参数锁定
				// 构造函数或对应的工厂方法
				if (mbd.resolvedConstructorOrFactoryMethod != null) {
					resolved = true;
					autowireNecessary = mbd.constructorArgumentsResolved;
				}
			}
		}
		// 如果已经解析过则使用解析好的构造函数，不需要再次锁定
		if (resolved) {
			if (autowireNecessary) {
				// 构造函数自动注入
				return autowireConstructor(beanName, mbd, null, null);
			}
			else {
				// 使用默认构造函数构造
				return instantiateBean(beanName, mbd);
			}
		}

		// Candidate constructors for autowiring?
		// 需要根据参数解析构造函数
		Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
		if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
				mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
			// 构造函数自动注入
			return autowireConstructor(beanName, mbd, ctors, args);
		}

		// Preferred constructors for default construction?
		ctors = mbd.getPreferredConstructors();
		if (ctors != null) {
			return autowireConstructor(beanName, mbd, ctors, null);
		}

		// No special handling: simply use no-arg constructor.
		// 使用默认构造函数构造
		return instantiateBean(beanName, mbd);
}
```

#### 1. autowireConstructor

关于功能考虑了一下几个方面

(1) 构造函数参数的确定

* 根据explicitArgs参数判断
  如果传入的参数explicitArgs不为空, 那便可以直接确定参数,  因为explicitArgs是在调用Bean的时候用户指定的, 主要用于静态工厂方法的调用, 而这里是畹町完全匹配的参数.
* 缓存中获取
* 配置文件获取

(2) 构造函数的确定

(3) 根据确定的构造函数转换对应的参数类型

(4) 构造函数不确定性的验证

(5) 根据实例化策略以及得到的构造函数参数实例化bean.

```java
public BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd,
			@Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {

		BeanWrapperImpl bw = new BeanWrapperImpl();
		this.beanFactory.initBeanWrapper(bw);

		Constructor<?> constructorToUse = null;
		ArgumentsHolder argsHolderToUse = null;
		Object[] argsToUse = null;

		// explicitArgs通过getBean传入
		// 如果getBean调用的时候指定方法参数， 那么直接使用
		if (explicitArgs != null) {
			argsToUse = explicitArgs;
		}
		else {
			// 如果getBean方法时候没有， 则尝试从配置文件中解析
			Object[] argsToResolve = null;
			synchronized (mbd.constructorArgumentLock) {
				// 尝试从缓存中获取
				constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse != null && mbd.constructorArgumentsResolved) {
					// Found a cached constructor...
					argsToUse = mbd.resolvedConstructorArguments;
					if (argsToUse == null) {
						// 配置的构造函数参数
						argsToResolve = mbd.preparedConstructorArguments;
					}
				}
			}
			// 如果缓存中存在
			if (argsToResolve != null) {
				// 缓存中的只可能是原始值， 也可能是最终值
				argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve, true);
			}
		}

		// 没有被缓存
		if (constructorToUse == null || argsToUse == null) {
			// Take specified constructors, if any.
			Constructor<?>[] candidates = chosenCtors;
			if (candidates == null) {
				Class<?> beanClass = mbd.getBeanClass();
				try {
					candidates = (mbd.isNonPublicAccessAllowed() ?
							beanClass.getDeclaredConstructors() : beanClass.getConstructors());
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Resolution of declared constructors on bean Class [" + beanClass.getName() +
							"] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
				}
			}

			if (candidates.length == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
				Constructor<?> uniqueCandidate = candidates[0];
				if (uniqueCandidate.getParameterCount() == 0) {
					synchronized (mbd.constructorArgumentLock) {
						mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
						mbd.constructorArgumentsResolved = true;
						mbd.resolvedConstructorArguments = EMPTY_ARGS;
					}
					bw.setBeanInstance(instantiate(beanName, mbd, uniqueCandidate, EMPTY_ARGS));
					return bw;
				}
			}

			// Need to resolve the constructor.
			boolean autowiring = (chosenCtors != null ||
					mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
			ConstructorArgumentValues resolvedValues = null;

			int minNrOfArgs;
			if (explicitArgs != null) {
				minNrOfArgs = explicitArgs.length;
			}
			else {
				// 提取配置文件的配置的构造参数
				ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
				// 用于承载解析后的构造函数参数的只
				resolvedValues = new ConstructorArgumentValues();
				minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
			}

			// 排序给定的构造函数， public构造函数优先参数数量降序，非public构造函数参数数量降序
			AutowireUtils.sortConstructors(candidates);
			int minTypeDiffWeight = Integer.MAX_VALUE;
			Set<Constructor<?>> ambiguousConstructors = null;
			LinkedList<UnsatisfiedDependencyException> causes = null;

			for (Constructor<?> candidate : candidates) {
				Class<?>[] paramTypes = candidate.getParameterTypes();

				if (constructorToUse != null && argsToUse != null && argsToUse.length > paramTypes.length) {
					// 如果已经找到选用的构造函数 或者需要的参数个数小于当前的构造函数则终止
					// 因为已经按照参数个数降序排列
					// Already found greedy constructor that can be satisfied ->
					// do not look any further, there are only less greedy constructors left.
					break;
				}
				if (paramTypes.length < minNrOfArgs) {
					// 参数个数不相等
					continue;
				}

				ArgumentsHolder argsHolder;
				if (resolvedValues != null) {
					// 有参数则根据值构造对应类型的参数
					try {
						String[] paramNames = ConstructorPropertiesChecker.evaluate(candidate, paramTypes.length);
						if (paramNames == null) {
							// 获取参数名称探索器
							ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
							if (pnd != null) {
								// 获取指定构造函数的参数名称
								paramNames = pnd.getParameterNames(candidate);
							}
						}
						// 根据名称和数据类型创建参数持有者
						argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames,
								getUserDeclaredConstructor(candidate), autowiring, candidates.length == 1);
					}
					catch (UnsatisfiedDependencyException ex) {
						if (logger.isTraceEnabled()) {
							logger.trace("Ignoring constructor [" + candidate + "] of bean '" + beanName + "': " + ex);
						}
						// Swallow and try next constructor.
						if (causes == null) {
							causes = new LinkedList<>();
						}
						causes.add(ex);
						continue;
					}
				}
				else {
					// Explicit arguments given -> arguments length must match exactly.
					// 构造参数没有参数的情况
					if (paramTypes.length != explicitArgs.length) {
						continue;
					}
					argsHolder = new ArgumentsHolder(explicitArgs);
				}

				// 探测是否有不确定性的构造函数存在，例如不同的构造函数的父子关系
				int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
						argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
				// Choose this constructor if it represents the closest match.
				// 如果它代表着当前最接近的匹配则选择作为构造函数
				if (typeDiffWeight < minTypeDiffWeight) {
					constructorToUse = candidate;
					argsHolderToUse = argsHolder;
					argsToUse = argsHolder.arguments;
					minTypeDiffWeight = typeDiffWeight;
					ambiguousConstructors = null;
				}
				else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
					if (ambiguousConstructors == null) {
						ambiguousConstructors = new LinkedHashSet<>();
						ambiguousConstructors.add(constructorToUse);
					}
					ambiguousConstructors.add(candidate);
				}
			}

			if (constructorToUse == null) {
				if (causes != null) {
					UnsatisfiedDependencyException ex = causes.removeLast();
					for (Exception cause : causes) {
						this.beanFactory.onSuppressedException(cause);
					}
					throw ex;
				}
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Could not resolve matching constructor " +
						"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities)");
			}
			else if (ambiguousConstructors != null && !mbd.isLenientConstructorResolution()) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Ambiguous constructor matches found in bean '" + beanName + "' " +
						"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
						ambiguousConstructors);
			}

			if (explicitArgs == null && argsHolderToUse != null) {
				// 将解析的构造函数加入缓存
				argsHolderToUse.storeCache(mbd, constructorToUse);
			}
		}

		Assert.state(argsToUse != null, "Unresolved constructor arguments");
		bw.setBeanInstance(instantiate(beanName, mbd, constructorToUse, argsToUse));
		return bw;
}
```

#### 2.instantiateBean

```java
protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition mbd) {
   try {
      Object beanInstance;
      final BeanFactory parent = this;
      if (System.getSecurityManager() != null) {
         beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
               getInstantiationStrategy().instantiate(mbd, beanName, parent),
               getAccessControlContext());
      }
      else {
         // 直接调用实例化策略
         beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
      }
      BeanWrapper bw = new BeanWrapperImpl(beanInstance);
      initBeanWrapper(bw);
      return bw;
   }
   catch (Throwable ex) {
      throw new BeanCreationException(
            mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
   }
}
```

#### 3. 实例化策略

> 最简单的就是直接利用反射方法来构造实例对象, 但是spring并没有这么做

首先判断,  如果methodOverrides为空也就是用户没有使用replace或者lookup的配置方法, 那么直接使用反射的方式, 简单快捷, 但是如果使用了这两个特性, 需要将提供的功能切入进去.

```java
public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
		// Don't override the class with CGLIB if no overrides.
		// 如果有需要覆盖 或者 动态替换的方法， 则使用cglib动态代码， 因为可以在创建的同时将动态方法织入类中
		// 如果没有需要动态改变的方法，  就可以直接反射得到了
		if (!bd.hasMethodOverrides()) {
			Constructor<?> constructorToUse;
			synchronized (bd.constructorArgumentLock) {
				constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse == null) {
					final Class<?> clazz = bd.getBeanClass();
					if (clazz.isInterface()) {
						throw new BeanInstantiationException(clazz, "Specified class is an interface");
					}
					try {
						if (System.getSecurityManager() != null) {
							constructorToUse = AccessController.doPrivileged(
									(PrivilegedExceptionAction<Constructor<?>>) clazz::getDeclaredConstructor);
						}
						else {
							constructorToUse = clazz.getDeclaredConstructor();
						}
						bd.resolvedConstructorOrFactoryMethod = constructorToUse;
					}
					catch (Throwable ex) {
						throw new BeanInstantiationException(clazz, "No default constructor found", ex);
					}
				}
			}
			return BeanUtils.instantiateClass(constructorToUse);
		}
		else {
			// Must generate CGLIB subclass.
			return instantiateWithMethodInjection(bd, beanName, owner);
		}
}
```

### 5.7.2 记录创建bean的ObjectFactory

解决spring依赖的核心代码

```java
boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));
		if (earlySingletonExposure) {
        if (logger.isTraceEnabled()) {
          logger.trace("Eagerly caching bean '" + beanName +
                       "' to allow for resolving potential circular references");
        }
         /**
           *  spring容器通过提前暴露刚完成构造器注入但为完成其他步骤的bean来完成的,
           *  而且只能解决单例作用域的bean循环依赖.  [通过提前暴露的一个单例工厂方法,
           *  从而使其它bean能引用到该bean.]
           */
          // 这里就是为避免循环依赖， 提取将bean初始化放入ObjectFactory中
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}
```

#### earlySingletonExposure

>  提早曝光的单例,  影响的条件
> 单例&允许循环依赖&当前bean正在创建中，检测循环依赖

* mbd.isSingleton(): 没有太多可以解释的, 此RootBeanDefinition代表的是否是单例.

* this.allowCircularReferences: 是否允许循环依赖. 没有找到在配置文件中如何配置, 但是可以在AbstractRefreshableApplicationContext中提供的设置函数, 通过硬编码解决. 

  ```java
  ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring-context.xml");
  		context.setAllowBeanDefinitionOverriding(false); // 是否允许循环依赖
  ```

* isSingletonCurrentlyInCreation: 该bean是否在创建中, spring中, 会有个专门的属性默认会DefaultSingletonBeanRegistry的singletonsCurrentlyInCreation来记录bean的加载状态, 在开始创建的时候会将beanName记录在属性中, 在bean创建结束后将beanName移除.

#### 总结

在B中创建依赖A时通过ObjectFactory提供的实例化方法来中断A中的属性填充, 使B中持有的A的时候进行的,但是因为A与B中的A所表示的属性地址是一样的, 所以在A中创建好的属性填充自然可以通过B中的A获取.

### 5.7.3 属性注入

populateBean处理流程: 

(1) InstatntiantionAwareBeanPostProcessor处理器的postProcessAfterInstantiation函数的应用, 此函数可以控制程序是否继续进行属性填充.

(2) 根据注入类型/姓名, 提取依赖的bean, 并统一存入PropertyValues中

(3) 应用InstantiationAwareBeanPostProcessor处理器的postProcessProperties方法, 对属性获取完毕填充前对属性的再次处理

(4) 将所有PropertyValues的属性填充至BeanWrapper中

```java
protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
		if (bw == null) {
			if (mbd.hasPropertyValues()) {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
			}
			else {
				// Skip property population phase for null instance.
				// 没有可填充的属性
				return;
			}
		}

		// Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
		// state of the bean before properties are set. This can be used, for example,
		// to support styles of field injection.
		boolean continueWithPropertyPopulation = true;

		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof InstantiationAwareBeanPostProcessor) {
					InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
					// 返回值为是否继续填充bean
					if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
						continueWithPropertyPopulation = false;
						break;
					}
				}
			}
		}

		// 如果后处理器发出停止填充命令则终止后续的操作
		if (!continueWithPropertyPopulation) {
			return;
		}

		PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);

		if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME || mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
			MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
			// Add property values based on autowire by name if applicable.
			// 根据名称自动注入
			if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME) {
				autowireByName(beanName, mbd, bw, newPvs);
			}
			// Add property values based on autowire by type if applicable.
			// 根据类型自动注入
			if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
				autowireByType(beanName, mbd, bw, newPvs);
			}
			pvs = newPvs;
		}

		// 后处理器已经初始化
		boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
		// 需要依赖检查
		boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);

		PropertyDescriptor[] filteredPds = null;
		if (hasInstAwareBpps) {
			if (pvs == null) {
				pvs = mbd.getPropertyValues();
			}
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof InstantiationAwareBeanPostProcessor) {
					InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
					PropertyValues pvsToUse = ibp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
					if (pvsToUse == null) {
						if (filteredPds == null) {
							filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
						}
						// 对所有需要依赖检查的属性进行后置处理
						pvsToUse = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
						if (pvsToUse == null) {
							return;
						}
					}
					pvs = pvsToUse;
				}
			}
		}
		if (needsDepCheck) {
			if (filteredPds == null) {
				filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
			}
			checkDependencies(beanName, mbd, filteredPds, pvs);
		}

		if (pvs != null) {
			// 将属性应用到bean中
			applyPropertyValues(beanName, mbd, bw, pvs);
		}
}
```

#### 1. autowireByName

```java
protected void autowireByName(
      String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

   // 寻找bw中需要依赖注入的属性
   String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
   for (String propertyName : propertyNames) {
      if (containsBean(propertyName)) {
         // 递归初始化相关的bean
         Object bean = getBean(propertyName);
         pvs.add(propertyName, bean);
         // 注册依赖
         registerDependentBean(propertyName, beanName);
         if (logger.isTraceEnabled()) {
            logger.trace("Added autowiring by name from bean name '" + beanName +
                  "' via property '" + propertyName + "' to bean named '" + propertyName + "'");
         }
      }
      else {
         if (logger.isTraceEnabled()) {
            logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
                  "' by name: no matching bean found");
         }
      }
   }
}
```

#### 2. autowireByType

```java
protected void autowireByType(
      String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

   TypeConverter converter = getCustomTypeConverter();
   if (converter == null) {
      converter = bw;
   }

   Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
   // 寻找bw中需要依赖注入的属性
   String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
   for (String propertyName : propertyNames) {
      try {
         PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
         // Don't try autowiring by type for type Object: never makes sense,
         // even if it technically is a unsatisfied, non-simple property.
         if (Object.class != pd.getPropertyType()) {
            // 探测指定属性的set方法
            MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
            // Do not allow eager init for type matching in case of a prioritized post-processor.
            boolean eager = !PriorityOrdered.class.isInstance(bw.getWrappedInstance());
            DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
            // 解析指定beanName的属性所匹配的值，并把解析到的属性名称存储在autowiredArgument中.
            Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
            if (autowiredArgument != null) {
               pvs.add(propertyName, autowiredArgument);
            }
            for (String autowiredBeanName : autowiredBeanNames) {
               // 注册依赖
               registerDependentBean(autowiredBeanName, beanName);
               if (logger.isTraceEnabled()) {
                  logger.trace("Autowiring by type from bean name '" + beanName + "' via property '" +
                        propertyName + "' to bean named '" + autowiredBeanName + "'");
               }
            }
            autowiredBeanNames.clear();
         }
      }
      catch (BeansException ex) {
         throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
      }
   }
}
```

```java
public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {

		descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
		if (Optional.class == descriptor.getDependencyType()) {
			return createOptionalDependency(descriptor, requestingBeanName);
		}
		else if (ObjectFactory.class == descriptor.getDependencyType() ||
				ObjectProvider.class == descriptor.getDependencyType()) {
			// ObjectFactory || ObjectProvider 类注入的特殊处理
			return new DependencyObjectProvider(descriptor, requestingBeanName);
		}
		else if (javaxInjectProviderClass == descriptor.getDependencyType()) {
			// javaxInjectProviderClass类注入的特殊处理
			return new Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
		}
		else {
			Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(
					descriptor, requestingBeanName);
			if (result == null) {
				// 通用处理逻辑
				result = doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
			}
			return result;
		}
}
```

#### 3. applyPropertyValues

这里已经完成了对所有注入属性的获取, 但是获取的属性是以PropertyValues形式存在的, 还并没有应用到已经实例化的bean中, 这一工作时在applyPropertyValues中.

```java
protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
		if (pvs.isEmpty()) {
			return;
		}

		if (System.getSecurityManager() != null && bw instanceof BeanWrapperImpl) {
			((BeanWrapperImpl) bw).setSecurityContext(getAccessControlContext());
		}

		MutablePropertyValues mpvs = null;
		List<PropertyValue> original;

		if (pvs instanceof MutablePropertyValues) {
			mpvs = (MutablePropertyValues) pvs;
			// 如果mpvs对应的类型已经转化为对应的类型，那么可以直接设置到BeanWrapper中
			if (mpvs.isConverted()) {
				// Shortcut: use the pre-converted values as-is.
				try {
					bw.setPropertyValues(mpvs);
					return;
				}
				catch (BeansException ex) {
					throw new BeanCreationException(
							mbd.getResourceDescription(), beanName, "Error setting property values", ex);
				}
			}
			original = mpvs.getPropertyValueList();
		}
		else {
			// 如果mpvs并不是使用MutablePropertyValues封装的类型，那么直接使用原始的属性获取方法
			original = Arrays.asList(pvs.getPropertyValues());
		}

		TypeConverter converter = getCustomTypeConverter();
		if (converter == null) {
			converter = bw;
		}
		// 获取对应的解析器
		BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);

		// Create a deep copy, resolving any references for values.
		List<PropertyValue> deepCopy = new ArrayList<>(original.size());
		boolean resolveNecessary = false;
		// 遍历属性， 将属性转化为对应类的对应属性得类型
		for (PropertyValue pv : original) {
			if (pv.isConverted()) {
				deepCopy.add(pv);
			}
			else {
				String propertyName = pv.getName();
				Object originalValue = pv.getValue();
				Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
				Object convertedValue = resolvedValue;
				boolean convertible = bw.isWritableProperty(propertyName) &&
						!PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
				if (convertible) {
					convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
				}
				// Possibly store converted value in merged bean definition,
				// in order to avoid re-conversion for every created bean instance.
				if (resolvedValue == originalValue) {
					if (convertible) {
						pv.setConvertedValue(convertedValue);
					}
					deepCopy.add(pv);
				}
				else if (convertible && originalValue instanceof TypedStringValue &&
						!((TypedStringValue) originalValue).isDynamic() &&
						!(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
					pv.setConvertedValue(convertedValue);
					deepCopy.add(pv);
				}
				else {
					resolveNecessary = true;
					deepCopy.add(new PropertyValue(pv, convertedValue));
				}
			}
		}
		if (mpvs != null && !resolveNecessary) {
			mpvs.setConverted();
		}

		// Set our (possibly massaged) deep copy.
		try {
			bw.setPropertyValues(new MutablePropertyValues(deepCopy));
		}
		catch (BeansException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Error setting property values", ex);
		}
}
```

### 5.7.4 初始化bean

> 到这里已经完成来bean的实例化,  调用的是用户设定的初始化方法

```java
protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
   if (System.getSecurityManager() != null) {
      AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
         invokeAwareMethods(beanName, bean);
         return null;
      }, getAccessControlContext());
   }
   else {
      // 对特殊的bean进行处理： Aware/BeanClassLoaderAware/BeanFactoryAware
      invokeAwareMethods(beanName, bean);
   }

   Object wrappedBean = bean;
   if (mbd == null || !mbd.isSynthetic()) {
      // 应用后处理器
      wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
   }

   try {
      // 激活用户自定义的init方法
      invokeInitMethods(beanName, wrappedBean, mbd);
   }
   catch (Throwable ex) {
      throw new BeanCreationException(
            (mbd != null ? mbd.getResourceDescription() : null),
            beanName, "Invocation of init method failed", ex);
   }
   if (mbd == null || !mbd.isSynthetic()) {
      // 后处理器应用
      wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
   }
   return wrappedBean;
}
```

#### 1. 激活Aware方法

实现Aware接口的bean在被初始之后, 可以取得一些相对应的资源, 例如实现实现BeanFactoryAware的bean的 初始化之后, Spring容器将会被注入BeanFactory的实例, 而实现ApplicationContextAware的bean, 在bean被初始化后,  将会被注入applicationContext的实例等.

```java
private void invokeAwareMethods(final String beanName, final Object bean) {
   if (bean instanceof Aware) {
      if (bean instanceof BeanNameAware) {
         ((BeanNameAware) bean).setBeanName(beanName);
      }
      if (bean instanceof BeanClassLoaderAware) {
         ClassLoader bcl = getBeanClassLoader();
         if (bcl != null) {
            ((BeanClassLoaderAware) bean).setBeanClassLoader(bcl);
         }
      }
      if (bean instanceof BeanFactoryAware) {
         ((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
      }
   }
}
```

#### 2.处理器的应用

BeanPostProcessor是开放式架构的一个亮点, 给用户充足的权限的去更改或者拓展Spring, 在调用客户自定义初始化方法前以及调用自定义初始化方法之后分别会调用postProcessBeforeInitialization和postProcessAfterInitialization方法. 使用户可以根据自己的业务需求进行相应的处理.

```java
@Override
public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
      throws BeansException {

   Object result = existingBean;
   for (BeanPostProcessor processor : getBeanPostProcessors()) {
      Object current = processor.postProcessAfterInitialization(result, beanName);
      if (current == null) {
         return result;
      }
      result = current;
   }
   return result;
}
```

```java
@Override
public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
      throws BeansException {

   Object result = existingBean;
   for (BeanPostProcessor processor : getBeanPostProcessors()) {
      Object current = processor.postProcessBeforeInitialization(result, beanName);
      if (current == null) {
         return result;
      }
      result = current;
   }
   return result;
}
```

#### 3. 激活自定义的init方法

客户定制的初始化方法除了我们熟知的使用配置的init-method外, 还可以实现initalizingBean接口, 并在afterPropertiesSet中实现自己的初始化业务逻辑.

执行顺序:  afterPripertiesSet先执行, init-method后执行

```java
protected void invokeInitMethods(String beanName, final Object bean, @Nullable RootBeanDefinition mbd)
      throws Throwable {

   // 首先会检查是否是InitializingBean， 如果是的话，需要调用afterPropertiesSet方法。
   boolean isInitializingBean = (bean instanceof InitializingBean);
   if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
      if (logger.isTraceEnabled()) {
         logger.trace("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
      }
      if (System.getSecurityManager() != null) {
         try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
               ((InitializingBean) bean).afterPropertiesSet();
               return null;
            }, getAccessControlContext());
         }
         catch (PrivilegedActionException pae) {
            throw pae.getException();
         }
      }
      else {
         ((InitializingBean) bean).afterPropertiesSet();
      }
   }

   if (mbd != null && bean.getClass() != NullBean.class) {
      String initMethodName = mbd.getInitMethodName();
      if (StringUtils.hasLength(initMethodName) &&
            !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
            !mbd.isExternallyManagedInitMethod(initMethodName)) {
         // 调用自定义初始化方法
         invokeCustomInitMethod(beanName, bean, mbd);
      }
   }
}
```

### 5.7.5 注册DisposableBean

Spring中不但提供了对于初始化方法的拓展入口, 同样也提供了销毁方法的拓展入口, 对于销毁方法的拓展, 除了destroy-method方法外, 还可以注册后处理器DestructionAwareBeanPostProcessor来统一处理bean销毁方法,

```java
protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
   AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
   if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
      if (mbd.isSingleton()) {
         // Register a DisposableBean implementation that performs all destruction
         // work for the given bean: DestructionAwareBeanPostProcessors,
         // DisposableBean interface, custom destroy method.

         // 单例模式下注册需要销毁的bean，此方法中处理实现DisposableBean的bean
         // 并且对所有的bean使用DestructionAwareBeanPostProcessors处理
         registerDisposableBean(beanName,
               new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
      }
      else {
         // A bean with a custom scope...
         Scope scope = this.scopes.get(mbd.getScope());
         if (scope == null) {
            throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
         }
         scope.registerDestructionCallback(beanName,
               new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
      }
   }
}
```






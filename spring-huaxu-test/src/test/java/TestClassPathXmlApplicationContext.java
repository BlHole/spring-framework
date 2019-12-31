import bean.MyBean;
import bean.beanPostProcessor.MyBeanPostProcessor;
import bean.eventMulticaster.MyEvent;
import bean.prepareRefresh.MyClassPathXmlApplicationContext;
import bean.propertyEditor.DateBean;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/26 19:59</p>
 * <p>author：huaxu</p>
 */
public class TestClassPathXmlApplicationContext {

	@Test
	public void test() {
		ApplicationContext context = new ClassPathXmlApplicationContext("spring-context.xml");
		MyBean bean = context.getBean(MyBean.class);
		System.out.println(bean);
	}

	@Test
	public void testCycleCreateBean() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring-context.xml");
		context.setAllowBeanDefinitionOverriding(false); // 是否允许循环依赖
		System.out.println(context.getParent());
	}

	@Test
	// vm VAR=GOGO
	public void testMyClassPathXmlApplicationContext() throws Exception {
		BeanFactory factory = new MyClassPathXmlApplicationContext("spring-context.xml");
		System.out.println(factory.getBean(MyBean.class));
	}

	@Test
	public void testDateBean() throws Exception {
		BeanFactory factory = new ClassPathXmlApplicationContext("spring-context.xml");
		System.out.println(factory.getBean(DateBean.class));
	}

	@Test
	public void testBeanFactoryPostProcessor() throws Exception {
		ConfigurableListableBeanFactory bf = new XmlBeanFactory(new ClassPathResource("spring-context.xml"));
		BeanFactoryPostProcessor postProcessor = (BeanFactoryPostProcessor)bf.getBean("bfpp");
		postProcessor.postProcessBeanFactory(bf);

		System.out.println(bf.getBean("simpleBeanFactory"));
	}

	@Test
	public void testBeanPostProcessor() throws Exception {
		BeanFactory factory = new ClassPathXmlApplicationContext("spring-context.xml");
		MyBeanPostProcessor bean = factory.getBean(MyBeanPostProcessor.class);
		System.out.println(bean);
	}

	@Test
	public void testListener() throws Exception {
		ApplicationContext factory = new ClassPathXmlApplicationContext("spring-context.xml");
		MyEvent myEvent = new MyEvent("hello", "msg");
		factory.publishEvent(myEvent);
	}
}
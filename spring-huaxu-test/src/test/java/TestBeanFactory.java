import bean.MyBean;
import bean.aware.TestBeanFactoryAware;
import bean.factoryBean.Car;
import bean.lookup.LookUpTestCode;
import bean.replacedMenthod.ChangeMethod;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * <p>项目名称: spring</p>
 * <p>文件名称: MyBeanFactoryTest</p>
 * <p>文件描述: </p>
 * <p>创建日期: 2019/06/26 16:21</p>
 * <p>创建用户：huaxu</p>
 */
public class TestBeanFactory {

	@Test
	public void testBeanFactory() {
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring-context.xml"));

		BeanDefinition beanDefinition = ((XmlBeanFactory) beanFactory).getBeanDefinition("myBean");
		Assert.assertTrue("12ms".equals(beanDefinition.getAttribute("speed")));

		MyBean myBean = (MyBean) beanFactory.getBean("myBean");
		Assert.assertTrue("strTest".equals(myBean.getStr()));
	}

	@Test
	public void testLookup() {
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring-context.xml"));
		LookUpTestCode lookUpTestCode = (LookUpTestCode) beanFactory.getBean("lookupBean");
		lookUpTestCode.showMe();
	}

	@Test
	public void testMethod() {
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring-context.xml"));
		ChangeMethod method = (ChangeMethod) beanFactory.getBean("testMethod");
		method.changeMe();
		method.changeMe("test");
	}

	@Test
	public void testCarFactoryBean() throws Exception {
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring-context.xml"));
		Car car = (Car) beanFactory.getBean("testCarFactoryBean");
		System.out.println(car);
	}

	@Test
	public void testStaticFactoryBean() throws IOException {
		BeanFactory context = new XmlBeanFactory(new ClassPathResource("spring-context.xml"));
		Car bmw = (Car) context.getBean("bmwCar");
		Car audi = (Car) context.getBean("audiCar");
		System.out.println(bmw);
		System.out.println(audi);
	}

	@Test
	public void testInstanceFactoryBean() throws IOException {
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring-context.xml"));
		Car card4 = (Car) beanFactory.getBean("car4");
		Car card6 = (Car) beanFactory.getBean("car6");
		System.out.println(card4);
		System.out.println(card6);
	}

	@Test
	public void testBeanFactoryAware() throws Exception {
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring-context.xml"));
		TestBeanFactoryAware model = (TestBeanFactoryAware) beanFactory.getBean("testBeanFactoryAware");
		model.show();
	}
}
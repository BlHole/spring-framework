import bean.MyBean;
import bean.aop.AopBean;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>项目名称: spring</p>
 * <p>文件名称: MyBeanFactoryTest</p>
 * <p>文件描述: </p>
 * <p>创建日期: 2019/06/26 16:21</p>
 * <p>创建用户：huaxu</p>
 */
public class MyBeanFactoryTest {

	@Test
	public void testBeanFactory() {
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring-context.xml"));

		MyBean myBean = (MyBean) beanFactory.getBean("myBean");
		Assert.assertTrue("strTest".equals(myBean.getStr()));
	}

	@Test
	public void testResource() throws IOException {
		Resource resource = new ClassPathResource("spring-context.xml");
		InputStream inputStream = resource.getInputStream();
	}

	@Test
	public void testAop(){
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring-aop.xml"));
		AopBean bean = (AopBean) beanFactory.getBean("aopBean");
		Assert.assertTrue("huaxu".equals(bean.getName()));
	}
}
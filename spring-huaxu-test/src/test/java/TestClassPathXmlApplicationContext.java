import bean.MyBean;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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
}
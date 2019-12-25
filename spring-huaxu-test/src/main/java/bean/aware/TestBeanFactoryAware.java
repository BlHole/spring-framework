package bean.aware;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/25 14:24</p>
 * <p>author：huaxu</p>
 */
public class TestBeanFactoryAware implements BeanFactoryAware {

	private AwareBeanTest beanTest;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanTest = beanFactory.getBean(AwareBeanTest.class);
	}

	public void show(){
		System.out.println(beanTest);
	}
}
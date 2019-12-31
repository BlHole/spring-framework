package bean.beanPostProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/31 11:15</p>
 * <p>author：huaxu</p>
 */
public class MyBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("bean:" + bean);
		System.out.println("beanName:" + beanName);
		return null;
	}
}
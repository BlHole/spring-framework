package bean.prepareRefresh;

import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/30 16:49</p>
 * <p>author：huaxu</p>
 *
 * 需求:
 * 	工程在运行过程中用到的某个设置(例如VAR) 是从系统环境变量中取得的,
 * 	而如果用户没有在系统环境中设置这个参数, 那么工程可能不会工作.
 * 	解决方法有很多, Spring中也可以做, 就是拓展
 * 	@see ClassPathXmlApplicationContext
 */
public class MyClassPathXmlApplicationContext extends ClassPathXmlApplicationContext {

	public MyClassPathXmlApplicationContext(String... configLocations) throws BeansException {
		super(configLocations);
	}

	@Override
	protected void initPropertySources() {
		getEnvironment().setRequiredProperties("VAR");
	}
}
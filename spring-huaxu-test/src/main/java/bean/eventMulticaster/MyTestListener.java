package bean.eventMulticaster;

import org.springframework.context.ApplicationListener;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/31 11:44</p>
 * <p>author：huaxu</p>
 */
public class MyTestListener implements ApplicationListener<MyEvent> {

	@Override
	public void onApplicationEvent(MyEvent event) {
		event.print();
	}
}
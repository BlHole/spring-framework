package bean.lifecycleProcessor;

import org.springframework.context.Lifecycle;
import org.springframework.context.LifecycleProcessor;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2020/01/02 18:36</p>
 * <p>author：huaxu</p>
 */
public class MyLifecycleProcessor implements Lifecycle {

	@Override
	public void start() {
		System.out.println("------start-------");
	}

	@Override
	public void stop() {
		System.out.println("------stop-------");
	}

	@Override
	public boolean isRunning() {
		return false;
	}
}
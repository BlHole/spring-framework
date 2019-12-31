package bean.eventMulticaster;

import org.springframework.context.ApplicationEvent;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/31 11:42</p>
 * <p>author：huaxu</p>
 */
public class MyEvent extends ApplicationEvent {


	public String msg;

	/**
	 * Create a new ApplicationEvent.
	 *
	 * @param source the object on which the event initially occurred (never {@code null})
	 */
	public MyEvent(Object source) {
		super(source);
	}

	public MyEvent(Object source, String msg) {
		super(source);
		this.msg = msg;
	}

	public void print() {
		System.out.println(msg);
	}
}
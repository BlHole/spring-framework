package bean.replacedMenthod;

import org.springframework.beans.factory.support.MethodReplacer;

import java.lang.reflect.Method;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/19 17:34</p>
 * <p>author：huaxu</p>
 */
public class ChangerMethodReplaced implements MethodReplacer {

	@Override
	public Object reimplement(Object obj, Method method, Object[] args) throws Throwable {
		System.out.println("我替换了原有的方法");
		return null;
	}
}
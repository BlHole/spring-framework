package bean.cycleCreatebean;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/24 14:35</p>
 * <p>author：huaxu</p>
 */
public class BeanC {

	private BeanA beanA;

	public BeanC(BeanA beanA) {
		this.beanA = beanA;
	}

	public void c() {
		beanA.a();
	}

	public BeanA getBeanA() {
		return beanA;
	}

	public void setBeanA(BeanA beanA) {
		this.beanA = beanA;
	}
}
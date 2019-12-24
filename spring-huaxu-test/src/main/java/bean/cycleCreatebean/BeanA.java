package bean.cycleCreatebean;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/24 14:35</p>
 * <p>author：huaxu</p>
 */
public class BeanA {

	private BeanB beanB;

	public BeanA(BeanB beanB) {
		this.beanB = beanB;
	}

	public void a() {
		beanB.b();
	}

	public BeanB getBeanB() {
		return beanB;
	}

	public void setBeanB(BeanB beanB) {
		this.beanB = beanB;
	}
}
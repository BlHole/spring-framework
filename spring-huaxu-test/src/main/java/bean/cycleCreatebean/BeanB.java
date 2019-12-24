package bean.cycleCreatebean;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/24 14:35</p>
 * <p>author：huaxu</p>
 */
public class BeanB {

	private BeanC beanC;

	public BeanB(BeanC beanC) {
		this.beanC = beanC;
	}

	public void b() {
		beanC.c();
	}

	public BeanC getBeanC() {
		return beanC;
	}

	public void setBeanC(BeanC beanC) {
		this.beanC = beanC;
	}
}
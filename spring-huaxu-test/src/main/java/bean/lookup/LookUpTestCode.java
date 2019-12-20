package bean.lookup;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/19 16:45</p>
 * <p>author：huaxu</p>
 */
public abstract class LookUpTestCode {

	public void showMe(){
		this.getBean().showMe();
	}

	public abstract User getBean();
}
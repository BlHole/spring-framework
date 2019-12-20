package bean.lookup;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/19 16:52</p>
 * <p>author：huaxu</p>
 */
public class Student extends User {

	@Override
	public void showMe() {
		System.out.println("i am student");
	}
}
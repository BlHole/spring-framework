package bean.replacedMenthod;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/19 17:32</p>
 * <p>author：huaxu</p>
 */
public class ChangeMethod {

	public void changeMe() {
		System.out.println("changeMe");
	}
	public void changeMe(String content) {
		System.out.println("changeMe :: " + content);
	}
}
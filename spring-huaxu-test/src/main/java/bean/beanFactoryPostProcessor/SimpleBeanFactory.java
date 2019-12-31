package bean.beanFactoryPostProcessor;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/31 10:44</p>
 * <p>author：huaxu</p>
 */
public class SimpleBeanFactory {

	private String conn;
	private String pass;
	private String name;

	public void setConn(String conn) {
		this.conn = conn;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "SimpleBeanFactory{" +
				"conn='" + conn + '\'' +
				", pass='" + pass + '\'' +
				", name='" + name + '\'' +
				'}';
	}
}
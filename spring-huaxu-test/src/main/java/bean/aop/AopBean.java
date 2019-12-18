package bean.aop;


/**
 * <p>项目名称: spring</p>
 * <p>文件名称: AopBean</p>
 * <p>文件描述: </p>
 * <p>创建日期: 2019/07/05 16:32</p>
 * <p>创建用户：huaxu</p>
 */
public class AopBean {

	private String name = "huaxu";

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void test(){
		System.out.println("my name is test");
	}
}
package bean.factoryBean;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/19 16:19</p>
 * <p>author：huaxu</p>
 */
public class InstanceCarFactory {

	private Map<Integer, Car> map = new HashMap<Integer, Car>();

	public void setMap(Map<Integer, Car> map) {
		this.map = map;
	}

	public Car getCar(int id){
		return map.get(id);
	}
}
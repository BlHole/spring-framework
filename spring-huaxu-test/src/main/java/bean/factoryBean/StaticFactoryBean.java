package bean.factoryBean;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/19 16:11</p>
 * <p>author：huaxu</p>
 */
public class StaticFactoryBean {

	private static Map<Integer, Car> map = new HashMap<Integer,Car>();

	static{
		map.put(1, new Car(1,"Honda",300000));
		map.put(2, new Car(2,"Audi",440000));
		map.put(3, new Car(3,"BMW",540000));
	}

	public static Car getCar(int id){
		return map.get(id);
	}
}
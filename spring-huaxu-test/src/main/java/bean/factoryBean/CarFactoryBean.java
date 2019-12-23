package bean.factoryBean;

import org.springframework.beans.factory.FactoryBean;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/23 16:18</p>
 * <p>author：huaxu</p>
 */
public class CarFactoryBean implements FactoryBean<Car> {

	private String carInfo;

	@Override
	public Car getObject() throws Exception {
		String[] split = carInfo.split(",");
		Car car = new Car();
		car.setId(Integer.parseInt(split[0]));
		car.setName(split[1]);
		car.setPrice(Integer.parseInt(split[2]));
		return car;
	}

	@Override
	public Class<?> getObjectType() {
		return Car.class;
	}

	public String getCarInfo() {
		return carInfo;
	}

	public void setCarInfo(String carInfo) {
		this.carInfo = carInfo;
	}
}
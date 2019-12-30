package bean.propertyEditor;

import java.util.Date;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/30 20:07</p>
 * <p>author：huaxu</p>
 */
public class DateBean {

	private Date dateValue;

	public Date getDateValue() {
		return dateValue;
	}

	public void setDateValue(Date dateValue) {
		this.dateValue = dateValue;
	}

	@Override
	public String toString() {
		return "DateBean{" +
				"dateValue=" + dateValue +
				'}';
	}
}
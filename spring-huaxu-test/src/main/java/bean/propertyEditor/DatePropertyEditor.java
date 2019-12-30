package bean.propertyEditor;


import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/30 19:57</p>
 * <p>author：huaxu</p>
 */
public class DatePropertyEditor extends PropertyEditorSupport {

	private String format = "yyyy-MM-dd";

	public void setFormat(String format) {
		this.format = format;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		System.out.println("test my DatePropertyEditor text:" + text);
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		try {
			Date parse = sdf.parse(text);
			this.setValue(parse);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
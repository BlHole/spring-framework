package bean.converter;

import org.springframework.core.convert.converter.Converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2020/01/02 17:32</p>
 * <p>author：huaxu</p>
 */
public class String2DateConverter implements Converter<String, Date> {

	@Override
	public Date convert(String source) {
		try {
			return new SimpleDateFormat("yyyy-MM-dd").parse(source);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
}
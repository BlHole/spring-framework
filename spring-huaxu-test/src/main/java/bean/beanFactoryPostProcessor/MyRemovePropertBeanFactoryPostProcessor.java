package bean.beanFactoryPostProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.StringValueResolver;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>project: spring</p>
 * <p>description: </p>
 * <p>create: 2019/12/31 10:33</p>
 * <p>author：huaxu</p>
 */
public class MyRemovePropertBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	private Set<String> properts;

	public MyRemovePropertBeanFactoryPostProcessor() {
		this.properts = new HashSet<>();
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		String[] definitionNames = beanFactory.getBeanDefinitionNames();
		for (String name : definitionNames) {
			BeanDefinition bd = beanFactory.getBeanDefinition(name);
			StringValueResolver resolver = new StringValueResolver() {
				@Override
				public String resolveStringValue(String strVal) {
					return isProperts(strVal) ? "****" : strVal;
				}
			};
			BeanDefinitionVisitor definitionVisitor = new BeanDefinitionVisitor(resolver);
			definitionVisitor.visitBeanDefinition(bd);
		}
	}

	private boolean isProperts(Object value) {
		String up = value.toString().toUpperCase();
		return this.properts.contains(up);
	}

	public void setProperts(Set<String> properts) {
		this.properts.clear();
		for (String propert : properts) {
			this.properts.add(propert.toUpperCase());
		}
	}
}
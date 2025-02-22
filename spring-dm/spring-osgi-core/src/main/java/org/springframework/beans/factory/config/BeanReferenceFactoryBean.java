package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartFactoryBean;

/**
 * FactoryBean that exposes an arbitrary target bean under a different name.
 *
 * <p>Usually, the target bean will reside in a different bean definition file,
 * using this FactoryBean to link it in and expose it under a different name.
 * Effectively, this corresponds to an alias for the target bean.
 *
 * <p><b>NOTE:</b> For XML bean definition files, an {@code &lt;alias&gt;}
 * tag is available that effectively achieves the same.
 *
 * <p>A special capability of this FactoryBean is enabled through its configuration
 * as bean definition: The "targetBeanName" can be substituted through a placeholder,
 * in combination with Spring's {@link PropertyPlaceholderConfigurer}.
 * Thanks to Marcus Bristav for pointing this out!
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setTargetBeanName
 * @see PropertyPlaceholderConfigurer
 * @deprecated as of Spring 3.2, in favor of using regular bean name aliases
 * (which support placeholder parsing since Spring 2.5)
 */
@Deprecated
public class BeanReferenceFactoryBean implements SmartFactoryBean<Object>, BeanFactoryAware {

    private String targetBeanName;

    private BeanFactory beanFactory;


    /**
     * Set the name of the target bean.
     * <p>This property is required. The value for this property can be
     * substituted through a placeholder, in combination with Spring's
     * PropertyPlaceholderConfigurer.
     * @param targetBeanName the name of the target bean
     * @see PropertyPlaceholderConfigurer
     */
    public void setTargetBeanName(String targetBeanName) {
        this.targetBeanName = targetBeanName;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        if (this.targetBeanName == null) {
            throw new IllegalArgumentException("'targetBeanName' is required");
        }
        if (!this.beanFactory.containsBean(this.targetBeanName)) {
            throw new NoSuchBeanDefinitionException(this.targetBeanName, this.beanFactory.toString());
        }
    }


    @Override
    public Object getObject() throws BeansException {
        if (this.beanFactory == null) {
            throw new FactoryBeanNotInitializedException();
        }
        return this.beanFactory.getBean(this.targetBeanName);
    }

    @Override
    public Class<?> getObjectType() {
        if (this.beanFactory == null) {
            return null;
        }
        return this.beanFactory.getType(this.targetBeanName);
    }

    @Override
    public boolean isSingleton() {
        if (this.beanFactory == null) {
            throw new FactoryBeanNotInitializedException();
        }
        return this.beanFactory.isSingleton(this.targetBeanName);
    }

    @Override
    public boolean isPrototype() {
        if (this.beanFactory == null) {
            throw new FactoryBeanNotInitializedException();
        }
        return this.beanFactory.isPrototype(this.targetBeanName);
    }

    @Override
    public boolean isEagerInit() {
        return false;
    }

}

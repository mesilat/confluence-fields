package com.mesilat.jira;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class LuceneFieldCreatorFactory implements FactoryBean, InitializingBean {
    private static final String JIRA7_BEAN_NAME = Jira7LuceneFieldCreator.class.getName();
    private static final String JIRA8_BEAN_NAME = Jira8LuceneFieldCreator.class.getName();

    @Autowired
    private AutowireCapableBeanFactory beanFactory;
    @Autowired
    @ComponentImport("salApplicationProperties")
    private ApplicationProperties applicationProperties;

    private String beanClassName;

    @Override
    public Object getObject() throws Exception {
        return beanFactory.createBean(Class.forName(beanClassName), 4, true);
    }
    @Override
    public Class getObjectType() {
        return LuceneFieldCreator.class;
    }
    @Override
    public boolean isSingleton() {
        return true;
    }
    @Override
    public void afterPropertiesSet() throws Exception {
        String[] version = applicationProperties.getVersion().split("\\.");
        if ("8".equals(version[0])){
            beanClassName = JIRA8_BEAN_NAME;
        } else {
            beanClassName = JIRA7_BEAN_NAME;
        }
    }
}
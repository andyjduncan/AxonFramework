package org.axonframework.commandhandling.annotation;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandTargetResolver;
import org.axonframework.common.annotation.ClasspathParameterResolverFactory;
import org.axonframework.common.annotation.MultiParameterResolverFactory;
import org.axonframework.common.annotation.ParameterResolverFactory;
import org.axonframework.common.annotation.SpringBeanParameterResolverFactory;
import org.axonframework.domain.AggregateRoot;
import org.axonframework.repository.Repository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Spring FactoryBean that creates an AggregateAnnotationCommandHandler instance.
 *
 * @param <T> The aggregate type on which command handlers are declared.
 * @author Allard Buijze
 * @since 2.1
 */
public class AggregateAnnotationCommandHandlerFactoryBean<T extends AggregateRoot<?>>
        implements FactoryBean<AggregateAnnotationCommandHandler<T>>, InitializingBean, ApplicationContextAware {

    private CommandBus commandBus;
    private Class<T> aggregateType;
    private Repository<T> repository;
    private CommandTargetResolver commandTargetResolver = new AnnotationCommandTargetResolver();
    private ParameterResolverFactory parameterResolverFactory;

    private AggregateAnnotationCommandHandler<T> handler;
    private ApplicationContext applicationContext;

    @Override
    public AggregateAnnotationCommandHandler<T> getObject() throws Exception {
        return handler;
    }

    @Override
    public Class<?> getObjectType() {
        return AggregateAnnotationCommandHandler.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (parameterResolverFactory == null) {
            SpringBeanParameterResolverFactory springBeanParameterResolverFactory = new SpringBeanParameterResolverFactory();
            springBeanParameterResolverFactory.setApplicationContext(applicationContext);
            parameterResolverFactory = new MultiParameterResolverFactory(
                    ClasspathParameterResolverFactory.forClass(aggregateType),
                    springBeanParameterResolverFactory);
        }
        handler = new AggregateAnnotationCommandHandler<T>(aggregateType, repository, commandTargetResolver,
                                                           parameterResolverFactory);
        for (String cmd : handler.supportedCommands()) {
            commandBus.subscribe(cmd, handler);
        }
    }

    /**
     * Sets the CommandBus to subscribe the handler to
     *
     * @param commandBus the CommandBus to subscribe the handler to
     */
    @Required
    public void setCommandBus(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    /**
     * Sets the type of aggregate to inspect for annotations.
     *
     * @param aggregateType the type of aggregate to inspect for annotations
     */
    @Required
    public void setAggregateType(Class<T> aggregateType) {
        this.aggregateType = aggregateType;
    }

    /**
     * The repository from which to load aggregate instances. The repository must be compatible with the aggregate type
     * provided.
     *
     * @param repository the Repository from which to load aggregate instances.
     */
    @Required
    public void setRepository(Repository<T> repository) {
        this.repository = repository;
    }

    /**
     * The resolver providing the identifier (and version) of the aggregate a command targets. Defaults to an
     * {@link AnnotationCommandTargetResolver}.
     *
     * @param commandTargetResolver The CommandTargetResolver to resolve the target aggregate with
     */
    public void setCommandTargetResolver(CommandTargetResolver commandTargetResolver) {
        this.commandTargetResolver = commandTargetResolver;
    }

    /**
     * Sets the ParameterResolverFactory to create parameter resolver instances with. Defaults to a {@link
     * org.axonframework.common.annotation.ClasspathParameterResolverFactory} that uses the aggregateType's class
     * loader.
     *
     * @param parameterResolverFactory the ParameterResolverFactory to create parameter resolver instances with.
     */
    public void setParameterResolverFactory(ParameterResolverFactory parameterResolverFactory) {
        this.parameterResolverFactory = parameterResolverFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}

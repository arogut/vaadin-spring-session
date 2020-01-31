package com.example.vaadinspringsession;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapAttributeConfig;
import com.hazelcast.config.MapIndexConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.*;
import com.vaadin.flow.spring.SpringServlet;
import com.vaadin.flow.spring.SpringVaadinServletService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.hazelcast.HazelcastIndexedSessionRepository;
import org.springframework.session.hazelcast.HazelcastSessionRepository;
import org.springframework.session.hazelcast.PrincipalNameExtractor;
import org.springframework.session.hazelcast.config.annotation.web.http.EnableHazelcastHttpSession;

@Profile("session")
@Configuration
@EnableHazelcastHttpSession
public class SessionConfig {

    @Bean
    public HazelcastInstance hazelcastInstance(
            @Value("${hazelcast.max.no.heartbeat.seconds:60}") String hazelcastHeartbeat) {

        MapAttributeConfig attributeConfig =
                new MapAttributeConfig().setName(HazelcastIndexedSessionRepository.PRINCIPAL_NAME_ATTRIBUTE)
                        .setExtractor(PrincipalNameExtractor.class.getName());

        Config config = new Config();
        config.setProperty("hazelcast.max.no.heartbeat.seconds", hazelcastHeartbeat)
                .getMapConfig(HazelcastIndexedSessionRepository.DEFAULT_SESSION_MAP_NAME)
                .addMapAttributeConfig(attributeConfig)
                .addMapIndexConfig(new MapIndexConfig(HazelcastIndexedSessionRepository.PRINCIPAL_NAME_ATTRIBUTE, false));
        config.getGroupConfig().setName("admin");

        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    public ServletRegistrationBean<SpringServlet> springServlet(ApplicationContext applicationContext,
                                                                @Value("${vaadin.urlMapping}") String vaadinUrlMapping) {

        SpringServlet servlet = buildSpringServlet(applicationContext);
        ServletRegistrationBean<SpringServlet> registrationBean =
                new ServletRegistrationBean<>(servlet, vaadinUrlMapping, "/frontend/*");
        registrationBean.setLoadOnStartup(1);
        registrationBean.addInitParameter(Constants.SERVLET_PARAMETER_SYNC_ID_CHECK, "false");
        return registrationBean;
    }

    private SpringServlet buildSpringServlet(ApplicationContext applicationContext) {
        return new SpringServlet(applicationContext, false) {
            @Override
            protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) throws
                    ServiceException {
                SpringVaadinServletService service =
                        buildSpringVaadinServletService(this, deploymentConfiguration, applicationContext);
                service.init();
                return service;
            }
        };
    }

    private SpringVaadinServletService buildSpringVaadinServletService(SpringServlet servlet,
                                                                       DeploymentConfiguration deploymentConfiguration,
                                                                       ApplicationContext applicationContext) {
        return new SpringVaadinServletService(servlet, deploymentConfiguration, applicationContext) {
            @Override
            public void requestEnd(VaadinRequest request, VaadinResponse response, VaadinSession session) {
                if (session != null) {
                    try {
                        session.lock();
                        writeToHttpSession(request.getWrappedSession(), session);
                    } finally {
                        session.unlock();
                    }
                }
                super.requestEnd(request, response, session);
            }
        };
    }
}

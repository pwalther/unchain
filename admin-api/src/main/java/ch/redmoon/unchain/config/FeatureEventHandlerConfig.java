/*
   Copyright 2026 Philipp Walther

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package ch.redmoon.unchain.config;

import ch.redmoon.unchain.event.FeatureEventHandler;
import ch.redmoon.unchain.event.NoOpFeatureEventHandler;
import org.springframework.beans.BeansException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class FeatureEventHandlerConfig {

    private final ApplicationContext applicationContext;

    public FeatureEventHandlerConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Value("${unchain.sse.handler:}")
    private String handlerClassName;

    @Bean
    public FeatureEventHandler featureEventHandler() {
        if (!StringUtils.hasText(handlerClassName)) {
            return new NoOpFeatureEventHandler();
        }

        try {
            Class<?> clazz = Class.forName(handlerClassName);
            try {
                return (FeatureEventHandler) applicationContext.getBean(clazz);
            } catch (BeansException e) {
                return (FeatureEventHandler) applicationContext.getAutowireCapableBeanFactory()
                        .createBean(clazz);
            }
        } catch (Exception e) {
            // Fallback to NoOp if class not found or instantiation fails
            return new NoOpFeatureEventHandler();
        }
    }
}

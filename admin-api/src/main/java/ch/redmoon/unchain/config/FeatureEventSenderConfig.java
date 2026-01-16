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

import ch.redmoon.unchain.event.FeatureEventSender;
import ch.redmoon.unchain.event.NoOpFeatureEventSender;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class FeatureEventSenderConfig {

    private final ApplicationContext applicationContext;

    public FeatureEventSenderConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Value("${unchain.sse.sender:}")
    private String senderClassName;

    @Bean
    public FeatureEventSender featureEventSender() {
        if (!StringUtils.hasText(senderClassName)) {
            return new NoOpFeatureEventSender();
        }

        try {
            Class<?> clazz = Class.forName(senderClassName);
            try {
                return (FeatureEventSender) applicationContext.getBean(clazz);
            } catch (BeansException e) {
                return (FeatureEventSender) applicationContext.getAutowireCapableBeanFactory()
                        .createBean(clazz);
            }
        } catch (Exception e) {
            // Fallback to NoOp if class not found or instantiation fails
            return new NoOpFeatureEventSender();
        }
    }
}

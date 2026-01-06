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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.core.DefaultHttpLogWriter;
import org.zalando.logbook.core.DefaultSink;
import org.zalando.logbook.json.JsonHttpLogFormatter;

import static org.zalando.logbook.core.Conditions.exclude;
import static org.zalando.logbook.core.Conditions.requestTo;

@Configuration
public class LogbookConfig {

    @Bean
    public Logbook logbook() {
        return Logbook.builder()
                .condition(exclude(
                        requestTo("/health"),
                        requestTo("/h2-console/**"),
                        requestTo("/swagger-ui/**"),
                        requestTo("/v3/api-docs/**")))
                .sink(new DefaultSink(
                        new JsonHttpLogFormatter(),
                        new DefaultHttpLogWriter()))
                .build();
    }
}

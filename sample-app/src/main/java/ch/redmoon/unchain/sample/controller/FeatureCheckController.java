package ch.redmoon.unchain.sample.controller;

import ch.redmoon.unchain.client.UnchainClient;
import ch.redmoon.unchain.client.UnchainContext;
import ch.redmoon.unchain.client.model.Variant;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feature-check")
public class FeatureCheckController {

    private final UnchainClient unchainClient;

    public FeatureCheckController(UnchainClient unchainClient) {
        this.unchainClient = unchainClient;
    }

    @GetMapping
    public FeatureStatus getStatus(@RequestParam(required = false) String name,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String userId) {

        // Use provided params or fall back to SDK configuration defaults
        String effectiveProject = (project != null && !project.isBlank()) ? project
                : unchainClient.getConfig().getProjects().get(0);
        String effectiveEnv = (environment != null && !environment.isBlank()) ? environment
                : unchainClient.getConfig().getEnvironment();
        String effectiveName = (name != null && !name.isBlank()) ? name : "SampleFlag";

        UnchainContext context = UnchainContext.builder()
                .userId(userId != null ? userId : "guest-user")
                .build();

        boolean enabled = unchainClient.isEnabled(effectiveProject, effectiveName, effectiveEnv, context);
        Variant variant = unchainClient.getVariant(effectiveProject, effectiveName, effectiveEnv, context);

        return FeatureStatus.builder()
                .featureName(effectiveName)
                .project(effectiveProject)
                .environment(effectiveEnv)
                .enabled(enabled)
                .variantName(variant != null ? variant.getName() : null)
                .payload(variant != null && variant.getPayload() != null ? variant.getPayload().getValue() : null)
                .build();
    }

    @GetMapping("/stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamStatus(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String userId) {

        final org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(
                60 * 60 * 1000L); // 1 hour timeout

        // Initial state
        try {
            emitter.send(getStatus(name, project, environment, userId));
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        Runnable removeListener = unchainClient.addChangeListener(updatedProjectId -> {
            try {
                // In a real app, we might filter by projectId if the user only cares about one
                FeatureStatus status = getStatus(name, project, environment, userId);
                emitter.send(status);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(removeListener);
        emitter.onTimeout(removeListener);
        emitter.onError((e) -> removeListener.run());

        return emitter;
    }

    @Data
    @Builder
    public static class FeatureStatus {
        private String featureName;
        private String project;
        private String environment;
        private boolean enabled;
        private String variantName;
        private String payload;
    }
}

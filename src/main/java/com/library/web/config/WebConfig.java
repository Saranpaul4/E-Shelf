package com.library.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.library.web.service.PhotoStorage;

/** Tells Spring MVC to run our presence stamping on every request. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PresenceInterceptor presenceInterceptor;
    private final PhotoStorage photoStorage;

    public WebConfig(PresenceInterceptor presenceInterceptor, PhotoStorage photoStorage) {
        this.presenceInterceptor = presenceInterceptor;
        this.photoStorage = photoStorage;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(presenceInterceptor).addPathPatterns("/**");
    }

    /**
     * Profile pictures are written to the disk after the app started, so they cannot
     * be plain static files. This maps the web address /uploads/x.jpg onto the real
     * folder on this machine.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(photoStorage.folder().toUri().toString());
    }
}


package io.runcoach.common.service;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.jwt.JsonWebToken;

@RequestScoped
public class CurrentAccount {
    @Inject
    JsonWebToken principal;

    public String subject() {
        String subject = principal.getSubject();
        if (subject == null || subject.isBlank() || subject.length() > 255) {
            throw new NotAuthorizedException("Bearer");
        }
        return subject;
    }
}
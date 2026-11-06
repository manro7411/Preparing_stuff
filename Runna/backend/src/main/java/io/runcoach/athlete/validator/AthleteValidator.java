package io.runcoach.athlete.validator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.ext.RuntimeDelegate;

@ApplicationScoped
public class AthleteValidator {
    private static final RuntimeDelegate.HeaderDelegate<EntityTag> ENTITY_TAGS =
            RuntimeDelegate.getInstance().createHeaderDelegate(EntityTag.class);

    public void requireSubject(String subject) {
        if (subject == null || subject.isBlank() || subject.length() > 255) {
            throw new NotAuthorizedException("Bearer");
        }
    }

    public long requireVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new WebApplicationException("If-Match is required", 428);
        }
        try {
            EntityTag tag = ENTITY_TAGS.fromString(ifMatch.strip());
            if (tag.isWeak()) {
                throw new WebApplicationException("A strong ETag is required", 412);
            }
            long version = Long.parseLong(tag.getValue());
            if (version < 0 || !Long.toString(version).equals(tag.getValue())
                    || !ENTITY_TAGS.toString(tag).equals(ifMatch.strip())) {
                throw new IllegalArgumentException("Invalid version ETag");
            }
            return version;
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("If-Match must contain one quoted non-negative version");
        }
    }
}
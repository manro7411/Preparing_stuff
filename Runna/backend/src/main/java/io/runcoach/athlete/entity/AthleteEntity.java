package io.runcoach.athlete.entity;

import io.runcoach.common.entity.BaseEntity;
import io.runcoach.athlete.validator.IanaTimeZone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class AthleteEntity extends BaseEntity {
    @NotBlank
    @Size(max = 255)
    @Column(name = "auth_subject", nullable = false, unique = true, updatable = false, length = 255)
    public String authSubject;

    @NotBlank
    @Email
    @Size(max = 320)
    @Column(nullable = false, length = 320)
    public String email;

    @NotBlank
    @Size(max = 120)
    @Column(name = "display_name", nullable = false, length = 120)
    public String displayName;

    @NotBlank
    @Size(max = 64)
    @IanaTimeZone
    @Column(name = "time_zone", nullable = false, length = 64)
    public String timeZone;
}
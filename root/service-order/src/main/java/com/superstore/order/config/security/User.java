package com.superstore.order.config.security;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USER_PROFILE")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class) //required for auditing to work
public class User implements UserDetails {
    
    @Column(name = "USERNAME", length = 50, nullable = false)
    private String username;
    
    @Column(name = "PASSWORD", nullable = false, length = 100) // accommodates Bcrypt/Argon2 hashes safely.
    private String password;
    
    @Id
    @Column(name = "MOBILE", unique = true, nullable = false, length = 10)
    private String mobile;
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    private Role role = Role.GUEST;

    @Column(name = "CREATE_USER", length = 50, nullable = false, updatable = false)
    @CreatedBy
    private String createUser;

    @Column(name = "CREATE_TSTAMP", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createTstamp; //set automatically when entity is saved

    @Column(name = "MODIFY_USER", length = 50, nullable = false)
    @LastModifiedBy
    private String modifyUser;
	
    @Column(name = "MODIFY_TSTAMP", nullable = false)
    @LastModifiedDate
	private LocalDateTime modifyTstamp; //updated automatically on save/update

    @Builder.Default
    @Column(name = "ACTIVE_FLAG", length = 1, nullable = false)
    private String activeFlag = "Y";

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // safe null-check in case a user is initialized without a role.
        if (this.role == null) {
            return List.of();
        }
        // prefixes "ROLE_" only if your Role enum doesn't already have it.
        String roleName = role.name().startsWith("ROLE_") ? role.name() : "ROLE_" + role.name();
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    // Spring Security UserDetails Contract Methods.

    @Override
    public boolean isAccountNonExpired() {
        return true; 
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; 
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; 
    }

    @Override
    public boolean isEnabled() {
        return true; 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return mobile != null && Objects.equals(mobile, user.mobile);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
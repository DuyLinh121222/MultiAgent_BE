package com.mael.security;

import com.mael.entity.UserAccount;
import com.mael.entity.enums.AccountStatus;
import com.mael.entity.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Thông tin người dùng đang đăng nhập, lưu trong SecurityContext.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final String userId;
    private final String email;
    private final String password;
    private final Role role;
    private final boolean active;

    public UserPrincipal(String userId, String email, String password, Role role, boolean active) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = active;
    }

    public static UserPrincipal from(UserAccount account) {
        return new UserPrincipal(
                account.getId(),
                account.getEmail(),
                account.getPasswordHash(),
                account.getRole(),
                account.getStatus() == AccountStatus.ACTIVE);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }
}

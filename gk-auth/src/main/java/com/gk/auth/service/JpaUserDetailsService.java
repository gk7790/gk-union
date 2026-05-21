package com.gk.auth.service;

import cn.hutool.core.util.StrUtil;
import com.gk.common.constant.Constant;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.auth.dao.SecurityDao;
import com.gk.auth.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {
    private final SecurityDao securityDao;
    private final RedisUtils redisUtils;

    public SysUser getUserByUserId(String model, Long uid) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", uid);

        SysUser user = getByUid(model, uid);
        validateUser(user);

        Set<Long> dataScopeList = getDataScopeList(user.getId());
        user.setDeptIdList(dataScopeList);

        Set<String> roleAuth = getRoleAuthList(user.getId());
        user.setRoleAuthList(roleAuth);
        Set<String> permissions =  getUserPermissions(user.getId(), user.isSuperAdmin());

        List<SimpleGrantedAuthority> authorities = new ArrayList<>(roleAuth.size() +  permissions.size());
        for (String auth : roleAuth) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + auth));
        }
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        user.setAuthorities(authorities);
        return user;
    }

    @Override
    public SysUser loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<SysUser> userOpt = securityDao.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }
        SysUser user = userOpt.get();
        validateUser(user);
        return user;
    }

    private void validateUser(SysUser user) {
        if (!user.isEnabled()) {
            throw new DisabledException("用户已被禁用");
        }
        if (!user.isAccountNonLocked()) {
            throw new LockedException("用户账户已被锁定");
        }
        if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException("用户账户已过期");
        }
        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("用户凭证已过期");
        }
    }

    /**
     * 获取用户对应的部门数据权限
     * @param uid  用户uname
     * @return 返回部门ID列表
     */
    public SysUser getByUid(String model, Long uid) {
        String redisKey = RedisKeys.getSysLonginKey(model, "auth-user:" + uid);
        SysUser idList = redisUtils.get(redisKey, SysUser.class);
        if (ObjectUtils.isNotEmpty(idList)) {
            return idList;
        }

        Optional<SysUser> userOpt = securityDao.getUserByUserId(uid);
        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }

        SysUser sysUser = userOpt.get();
        redisUtils.set(redisKey, sysUser, TimeUnit.HOURS.toSeconds(5));
        return sysUser;
    }

    /**
     * 获取用户对应的部门数据权限
     * @param userId  用户ID
     * @return        返回部门ID列表
     */
    public Set<String> getRoleAuthList(Long userId) {
        String redisKey = RedisKeys.getSysLonginKey(Constant.ADMIN, "role-auth:" + userId);
        Set<String> idList = redisUtils.getSet(redisKey, String.class);
        if (ObjectUtils.isNotEmpty(idList)) {
            return idList;
        }
        Set<String> scopeList = securityDao.getRoleAuthList(userId);
        redisUtils.addSet(redisKey, scopeList, TimeUnit.HOURS.toSeconds(5));
        return scopeList;
    }

    public Set<String> getUserPermissions(Long userId, boolean isAdmin) {
        String redisKey = RedisKeys.getSysLonginKey(Constant.ADMIN, "permissions:" + userId);
        Set<String> permList = redisUtils.getSet(redisKey, String.class);
        if (CollectionUtils.isNotEmpty(permList)) {
            return permList;
        }
        //系统管理员，拥有最高权限
        List<String> permissionsList;
        if (isAdmin) {
            permissionsList = securityDao.getPermissionsList();
        } else {
            permissionsList = securityDao.getUserPermissionsList(userId);
        }

        //用户权限列表
        Set<String> permsSet = new HashSet<>();
        for (String permissions : permissionsList) {
            if (StrUtil.isBlank(permissions)) {
                continue;
            }
            permsSet.addAll(Arrays.asList(permissions.trim().split(",")));
        }

        redisUtils.addSet(redisKey, permsSet, TimeUnit.HOURS.toSeconds(5));
        return permsSet;
    }

    /**
     * 获取用户对应的部门数据权限
     * @param userId  用户ID
     * @return        返回部门ID列表
     */
    public Set<Long> getDataScopeList(Long userId) {
        String redisKey = RedisKeys.getSysLonginKey(Constant.ADMIN, "dataScope:" + userId);
        Set<Long> idList = redisUtils.getSet(redisKey, Long.class);
        if (ObjectUtils.isNotEmpty(idList)) {
            return idList;
        }
        Set<Long> scopeList = securityDao.getDataScopeList(userId);
        redisUtils.addSet(redisKey, scopeList, TimeUnit.HOURS.toSeconds(5));
        return scopeList;
    }
}

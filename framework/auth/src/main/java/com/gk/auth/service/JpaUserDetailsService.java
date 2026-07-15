package com.gk.auth.service;

import cn.hutool.core.util.StrUtil;
import com.gk.common.constant.Constant;
import com.gk.common.enums.MenuTypeEnum;
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
    private static final Long EMPTY_DATA_SCOPE_MARKER = -1L;

    private final SecurityDao securityDao;
    private final RedisUtils redisUtils;

    public SysUser getUserByUserId(String model, Long uid) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", uid);

        String redisKey = RedisKeys.getSysLonginKey(model, uid + "");
        SysUser sUser = redisUtils.get(redisKey, SysUser.class);
        if (ObjectUtils.isNotEmpty(sUser)) {
            return sUser;
        }

        SysUser user = getByUid(model, uid);
        validateUser(user);
        populateAuthorizations(user);

        redisUtils.set(redisKey, user, TimeUnit.HOURS.toSeconds(5));
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
        populateAuthorizations(user);
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
     *
     * @param uid 用户uname
     * @return 返回部门ID列表
     */
    public SysUser getByUid(String model, Long uid) {
        Optional<SysUser> userOpt = securityDao.getUserByUserId(uid);
        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }
        return userOpt.get();
    }

    /**
     * 获取用户对应的部门数据权限
     *
     * @param userSubjectId 用户ID
     * @return 返回部门ID列表
     */
    public Set<String> getRoleAuthList(Long userSubjectId) {
        return securityDao.getRoleAuthList(userSubjectId);
    }

    public Set<String> getUserPermissions(Long userSubjectId, boolean isAdmin) {
        //系统管理员，拥有最高权限
        List<String> permissionsList;
        List<String> authMenuTypes = toTypeCodes(MenuTypeEnum.auth());
        if (isAdmin) {
            permissionsList = securityDao.getPermissionsList(authMenuTypes);
        } else {
            permissionsList = securityDao.getUserPermissionsList(userSubjectId, authMenuTypes);
        }

        //用户权限列表
        Set<String> permsSet = new HashSet<>();
        for (String permissions : permissionsList) {
            if (StrUtil.isBlank(permissions)) {
                continue;
            }
            permsSet.addAll(Arrays.asList(permissions.trim().split(",")));
        }
        return permsSet;
    }

    private List<String> toTypeCodes(List<Integer> typeList) {
        if (CollectionUtils.isEmpty(typeList)) {
            return null;
        }
        return typeList.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
    }

    /**
     * 根据部门ID，获取所有子部门ID列表
     *
     * @param deptId 部门ID
     */
    public Set<Long> getSubDeptIdList(Long deptId) {
        String redisKey = RedisKeys.getDeptIdsKey(deptId);
        Set<Long> ids = redisUtils.getSet(redisKey, Long.class);
        if (CollectionUtils.isNotEmpty(ids)) {
            return ids;
        }
        Set<Long> subDeptIdList = securityDao.getSubDeptIdList(deptId);
        if  (CollectionUtils.isNotEmpty(subDeptIdList)) {
            redisUtils.addSet(redisKey, subDeptIdList, TimeUnit.HOURS.toSeconds(5));
        }
        return subDeptIdList;
    }

    public void evictLoginCache(Long userId, Long subjectId, Long deptId) {
        List<String> keys = new ArrayList<>(6);
        if (userId != null) {
            keys.add(RedisKeys.getSysLonginKey(Constant.ADMIN, userId.toString()));
            keys.add(RedisKeys.getUserMenuNavKey(userId));
            keys.add(RedisKeys.getUserPermissionsKey(userId));
            keys.add(RedisKeys.getSecurityUserKey(userId));
        }
        if (subjectId != null) {
            keys.add(RedisKeys.getSubjectDataScopeKey(subjectId));
        }
        if (deptId != null) {
            keys.add(RedisKeys.getDeptIdsKey(deptId));
        }
        if (!keys.isEmpty()) {
            redisUtils.delete(keys);
        }
    }

    public Set<Long> getDataScopeList(Long userSubjectId) {
        if (userSubjectId == null) {
            return Set.of();
        }
        String redisKey = RedisKeys.getSubjectDataScopeKey(userSubjectId);
        Set<Long> cachedScope = redisUtils.getSet(redisKey, Long.class);
        if (CollectionUtils.isNotEmpty(cachedScope)) {
            if (cachedScope.contains(EMPTY_DATA_SCOPE_MARKER)) {
                return Set.of();
            }
            return cachedScope;
        }

        Set<Long> dataScope = securityDao.getDataScopeList(userSubjectId);
        if (CollectionUtils.isNotEmpty(dataScope)) {
            redisUtils.addSet(redisKey, dataScope, TimeUnit.HOURS.toSeconds(5));
        } else {
            redisUtils.addSet(redisKey, List.of(EMPTY_DATA_SCOPE_MARKER), TimeUnit.HOURS.toSeconds(5));
        }
        return dataScope == null ? Set.of() : dataScope;
    }

    private void populateAuthorizations(SysUser user) {
        refreshRoleAuths(user);
        repopulateAuthorities(user);
    }

    private void repopulateAuthorities(SysUser user) {
        Set<String> permissions = getUserPermissions(user.getUserSubjectId(), user.isSuperAdmin());
        user.setAuthList(permissions);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        Set<String> roleAuth = user.getRoleList() == null ? Set.of() : user.getRoleList();
        roleAuth.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        user.setAuthorities(authorities);
    }

    private void refreshRoleAuths(SysUser user) {
        if (user.getUserSubjectId() == null) {
            return;
        }
        Set<String> roleAuth = getRoleAuthList(user.getUserSubjectId());
        user.setRoleList(roleAuth);
        List<Long> roleIds = securityDao.getRoleIdList(user.getUserSubjectId());
        user.setRoleIdList(roleIds);
        if ((user.getRoleId() == null) && roleIds != null && !roleIds.isEmpty()) {
            user.setRoleId(roleIds.getFirst());
        }
        if (roleAuth != null && !roleAuth.isEmpty()) {
            user.setRoleAuth(roleAuth.stream()
                    .filter(this::isSuperAdminAuth)
                    .findFirst()
                    .orElse(roleAuth.iterator().next()));
        }
    }

    private boolean isSuperAdminAuth(String auth) {
        return Constant.ROLE_AUTH_SADMIN.equalsIgnoreCase(auth)
                || "SUPER_ADMIN".equalsIgnoreCase(auth);
    }
}

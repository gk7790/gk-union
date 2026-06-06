package com.gk.auth.service;

import cn.hutool.core.util.StrUtil;
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

        Set<String> roleAuth = getRoleAuthList(user.getId());
        user.setRoleList(roleAuth);

        // TODO 后期数据多了单独缓存
        Set<String> permissions = getUserPermissions(user.getId(), user.isSuperAdmin());
        user.setAuthList(permissions);

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
     * @param userId 用户ID
     * @return 返回部门ID列表
     */
    public Set<String> getRoleAuthList(Long userId) {
        return securityDao.getRoleAuthList(userId);
    }

    public Set<String> getUserPermissions(Long userId, boolean isAdmin) {
        //系统管理员，拥有最高权限
        List<String> permissionsList;
        if (isAdmin) {

            permissionsList = securityDao.getPermissionsList(MenuTypeEnum.auth());
        } else {
            permissionsList = securityDao.getUserPermissionsList(userId,  MenuTypeEnum.auth());
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
        return securityDao.getSubDeptIdList(deptId);
    }
}

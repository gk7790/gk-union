package com.gk.iam.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.enums.AuthTypeEnum;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.PageData;
import com.gk.common.password.PasswordUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.iam.dao.SysUserDao;
import com.gk.iam.dto.SysUserDTO;
import com.gk.iam.entity.SysUserEntity;
import com.gk.iam.entity.SysUserSubjectEntity;
import com.gk.iam.service.SysRoleService;
import com.gk.iam.service.SysRoleUserService;
import com.gk.iam.service.SysUserService;
import com.gk.iam.service.SysUserSubjectService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 系统用户。
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends BaseServiceImpl<SysUserDao, SysUserEntity> implements SysUserService {
    private final SysUserSubjectService sysUserSubjectService;
    private final SysRoleService sysRoleService;
    private final SysRoleUserService sysRoleUserService;

    @Override
    public PageData<SysUserDTO> page(Map<String, Object> params) {
        paramsToLike(params, "username");
        applySubjectQueryScope(params);

        IPage<SysUserEntity> page = getPage(params, "t1.created_at", false);
        if (!ReqContextHolder.isSuperAdmin()) {
            params.put("deptIdList", ReqContextHolder.getSubDeptIdsWithSelf());
            params.put("selfId", ReqContextHolder.getUserId());
        }

        List<SysUserEntity> list = baseDao.getList(params);
        PageData<SysUserDTO> pageData = new PageData<>(toDtoList(list), page.getTotal());
        fillAuthenticatorBound(pageData.getItems());
        return pageData;
    }

    @Override
    public List<SysUserDTO> list(Map<String, Object> params) {
        applySubjectQueryScope(params);
        if (!ReqContextHolder.isSuperAdmin()) {
            params.put("deptIdList", ReqContextHolder.getSubDeptIds());
            params.put("selfId", ReqContextHolder.getUserId());
        }

        List<SysUserEntity> entityList = baseDao.getList(params);
        List<SysUserDTO> result = toDtoList(entityList);
        fillAuthenticatorBound(result);
        return result;
    }

    @Override
    public SysUserDTO getById(Long id) {
        SysUserEntity entity = baseDao.selectById(id);
        SysUserDTO dto = toDto(entity);
        fillAuthenticatorBound(dto);
        return dto;
    }

    @Override
    public SysUserDTO getByUsername(String username) {
        SysUserEntity entity = baseDao.getByUsername(username);
        SysUserDTO dto = toDto(entity);
        fillAuthenticatorBound(dto);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SysUserDTO dto) {
        SysUserEntity entity = ConvertUtils.sourceToTarget(dto, SysUserEntity.class);
        entity.setPassword(PasswordUtils.encode(entity.getPassword()));

        insert(entity);
        dto.setId(entity.getId());

        saveSubjectAndRoles(entity.getId(), dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysUserDTO dto) {
        SysUserEntity entity = ConvertUtils.sourceToTarget(dto, SysUserEntity.class);
        if (StringUtils.isBlank(dto.getPassword())) {
            entity.setPassword(null);
        } else {
            entity.setPassword(PasswordUtils.encode(entity.getPassword()));
        }

        updateById(entity);
        saveSubjectAndRoles(entity.getId(), dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(SysUserDTO dto) {
        AssertUtils.isNull(dto.getId(), "userId");
        SysUserEntity entity = selectById(dto.getId());
        if (entity == null) {
            throw new GkException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        if (dto.getAvatar() != null) {
            entity.setAvatar(dto.getAvatar());
        }
        if (dto.getNickname() != null) {
            entity.setNickname(dto.getNickname());
        }
        if (dto.getRealName() != null) {
            entity.setRealName(dto.getRealName());
        }
        if (dto.getGender() != null) {
            entity.setGender(dto.getGender());
        }
        if (dto.getMobile() != null) {
            entity.setMobile(dto.getMobile());
        }
        if (dto.getEmail() != null) {
            entity.setEmail(dto.getEmail());
        }

        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long[] ids) {
        baseDao.deleteByIds(Arrays.asList(ids));
        sysUserSubjectService.deleteByUserIds(ids);
        sysRoleUserService.deleteByUserIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long id, String newPassword) {
        baseDao.updatePassword(id, PasswordUtils.encode(newPassword));
    }

    @Override
    public void updateAuthenticator(Long id, Integer authType, String authSecret) {
        baseDao.updateAuthenticator(id, authType, authSecret);
    }

    @Override
    public int getCountByDeptId(Long deptId) {
        return baseDao.getCountByDeptId(deptId);
    }

    @Override
    public List<Long> getUserIdListByDeptId(List<Long> deptIdList) {
        return baseDao.getUserIdListByDeptId(deptIdList);
    }

    private List<SysUserDTO> toDtoList(List<SysUserEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toDto).toList();
    }

    private SysUserDTO toDto(SysUserEntity entity) {
        if (entity == null) {
            return null;
        }
        SysUserDTO dto = ConvertUtils.sourceToTarget(entity, SysUserDTO.class);
        dto.setUserSubjectId(entity.getUserSubjectId());
        dto.setSubjectId(entity.getSubjectId());
        return dto;
    }

    private void saveSubjectAndRoles(Long userId, SysUserDTO dto) {
        SysUserSubjectEntity subject = buildSubject(dto);
        List<Long> roleIds = resolveAssignableRoleIds(dto, subject);
        subject = sysUserSubjectService.saveOrUpdate(userId, subject);
        dto.setUserSubjectId(subject.getId());
        dto.setRoleId(roleIds.getFirst());
        sysRoleUserService.saveOrUpdate(subject.getId(), userId, roleIds);
    }

    private SysUserSubjectEntity buildSubject(SysUserDTO dto) {
        if (dto == null) {
            throw new GkException(ErrorCode.NOT_NULL, "user");
        }
        String subjectType = Optional.of(dto).map(SysUserDTO::getSubjectType).orElse(SubjectTypeEnum.TENANT.code());
        Integer status = Optional.of(dto).map(SysUserDTO::getStatus).orElse(StatusEnum.NORMAL.code());
        SysUserSubjectEntity subject = new SysUserSubjectEntity();
        subject.setSubjectType(subjectType);
        subject.setTenantId(dto.getTenantId());
        subject.setSubjectId(dto.getSubjectId());
        subject.setDeptId(dto.getDeptId());
        subject.setStatus(status);
        return subject;
    }

    private List<Long> resolveAssignableRoleIds(SysUserDTO dto, SysUserSubjectEntity subject) {
        List<Long> roleIds = dto.getRoleIdList();
        if ((roleIds == null || roleIds.isEmpty()) && dto.getRoleId() != null) {
            roleIds = List.of(dto.getRoleId());
        }
        if (roleIds == null || roleIds.isEmpty()) {
            throw new GkException(ErrorCode.NOT_NULL, "roleId");
        }
        for (Long roleId : roleIds) {
            sysRoleService.assertRoleAssignable(
                    roleId,
                    subject.getSubjectType(),
                    subject.getTenantId(),
                    subject.getSubjectId()
            );
        }
        return roleIds;
    }

    private void applySubjectQueryScope(Map<String, Object> params) {
        if (ReqContextHolder.isPlatform()) {
            return;
        }
        if (SubjectTypeEnum.MERCHANT.matches(ReqContextHolder.getSubjectType())) {
            Long tenantId = ReqContextHolder.getTenantId();
            Long merchantId = ReqContextHolder.getMerchantId();
            AssertUtils.isNull(tenantId, "tenantId");
            AssertUtils.isNull(merchantId, "merchantId");
            params.put("tenantId", tenantId);
            params.put("merchantId", merchantId);
            return;
        }
        Long tenantId = ReqContextHolder.getTenantId();
        AssertUtils.isNull(tenantId, "tenantId");
        params.put("tenantId", tenantId);
    }

    private void fillAuthenticatorBound(List<SysUserDTO> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        users.stream().filter(Objects::nonNull).forEach(this::fillAuthenticatorBound);
    }

    private void fillAuthenticatorBound(SysUserDTO user) {
        if (user == null) {
            return;
        }
        user.setAuthenticatorBound(AuthTypeEnum.requiresMfa(user.getAuthType())
                && StringUtils.isNotBlank(user.getAuthSecret()));
    }
}

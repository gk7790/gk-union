package com.gk.platform.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.model.PageData;
import com.gk.common.password.PasswordUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.platform.dao.SysUserDao;
import com.gk.platform.dto.SysUserDTO;
import com.gk.platform.entity.SysUserEntity;
import com.gk.platform.entity.SysUserSubjectEntity;
import com.gk.platform.service.SysRoleService;
import com.gk.platform.service.SysRoleUserService;
import com.gk.platform.service.SysUserService;
import com.gk.platform.service.SysUserSubjectService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        return getPageData(list, page.getTotal(), SysUserDTO.class);
    }

    @Override
    public List<SysUserDTO> list(Map<String, Object> params) {
        applySubjectQueryScope(params);
        if (!ReqContextHolder.isSuperAdmin()) {
            params.put("deptIdList", ReqContextHolder.getSubDeptIds());
            params.put("selfId", ReqContextHolder.getUserId());
        }

        List<SysUserEntity> entityList = baseDao.getList(params);
        return ConvertUtils.sourceToTarget(entityList, SysUserDTO.class);
    }

    @Override
    public SysUserDTO getById(Long id) {
        SysUserEntity entity = baseDao.selectById(id);
        return ConvertUtils.sourceToTarget(entity, SysUserDTO.class);
    }

    @Override
    public SysUserDTO getByUsername(String username) {
        SysUserEntity entity = baseDao.getByUsername(username);
        return ConvertUtils.sourceToTarget(entity, SysUserDTO.class);
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
        SysUserEntity entity = selectById(dto.getId());
        entity.setAvatar(dto.getAvatar());
        entity.setRealName(dto.getRealName());
        entity.setGender(dto.getGender());
        entity.setMobile(dto.getMobile());
        entity.setEmail(dto.getEmail());

        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long[] ids) {
        baseDao.deleteBatchIds(Arrays.asList(ids));
        sysUserSubjectService.deleteByUserIds(ids);
        sysRoleUserService.deleteByUserIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long id, String newPassword) {
        baseDao.updatePassword(id, PasswordUtils.encode(newPassword));
    }

    @Override
    public int getCountByDeptId(Long deptId) {
        return baseDao.getCountByDeptId(deptId);
    }

    @Override
    public List<Long> getUserIdListByDeptId(List<Long> deptIdList) {
        return baseDao.getUserIdListByDeptId(deptIdList);
    }

    private void saveSubjectAndRoles(Long userId, SysUserDTO dto) {
        SysUserSubjectEntity subject = buildSubject(dto);
        List<Long> roleIds = resolveAssignableRoleIds(dto, subject);
        subject = sysUserSubjectService.saveOrUpdate(userId, subject);
        dto.setSubjectId(subject.getId());
        dto.setRoleId(roleIds.get(0));
        sysRoleUserService.saveOrUpdate(subject.getId(), userId, roleIds);
    }

    private SysUserSubjectEntity buildSubject(SysUserDTO dto) {
        String subjectType = Optional.ofNullable(dto).map(SysUserDTO::getSubjectType).orElse(SubjectTypeEnum.TENANT.code());
        Integer status = Optional.ofNullable(dto).map(SysUserDTO::getStatus).orElse(StatusEnum.NORMAL.code());
        SysUserSubjectEntity subject = new SysUserSubjectEntity();
        subject.setSubjectType(subjectType);
        subject.setTenantId(dto.getTenantId());
        subject.setMerchantId(dto.getMerchantId());
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
            AssertUtils.isNull(null, "roleId");
        }
        for (Long roleId : roleIds) {
            sysRoleService.assertRoleAssignable(
                    roleId,
                    subject.getSubjectType(),
                    subject.getTenantId(),
                    subject.getMerchantId()
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
}

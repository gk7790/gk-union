package com.gk.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.alibaba.fastjson2.JSON;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.redis.PaymentRedisKeys;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.ledger.service.LedgerAccountService;
import com.gk.iam.dto.SysDeptDTO;
import com.gk.iam.dto.SysRoleDTO;
import com.gk.iam.dto.SysUserDTO;
import com.gk.iam.entity.SysRoleEntity;
import com.gk.iam.service.SysDeptService;
import com.gk.iam.service.SysRoleMenuService;
import com.gk.iam.service.SysRoleService;
import com.gk.iam.service.SysUserService;
import com.gk.tenant.dao.TenantDao;
import com.gk.tenant.dao.TenantCurrencyDao;
import com.gk.tenant.dto.TenantDTO;
import com.gk.tenant.dto.TenantOnboardRequest;
import com.gk.tenant.dto.TenantOnboardResult;
import com.gk.tenant.entity.TenantCurrencyEntity;
import com.gk.tenant.entity.TenantEntity;
import com.gk.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceImpl extends CrudServiceImpl<TenantDao, TenantEntity, TenantDTO> implements TenantService {
    private static final long TENANT_DICT_CACHE_SECONDS = 60 * 60L;

    private final SysDeptService sysDeptService;
    private final SysRoleService sysRoleService;
    private final SysRoleMenuService sysRoleMenuService;
    private final SysUserService sysUserService;
    private final LedgerAccountService ledgerAccountService;
    private final RedisUtils redisUtils;
    private final TenantCurrencyDao tenantCurrencyDao;

    @Override
    public QueryWrapper<TenantEntity> getWrapper(DynMap params) {
        QueryWrapper<TenantEntity> wrapper = new QueryWrapper<>();
        List<Integer> statusList = StatusEnum.normalizeQueryStatus(params.getList("status", Integer.class, StatusEnum.defaultStatus()));
        wrapper.in("status", statusList);
        if (!ReqContextHolder.isSuperAdmin()) {
            wrapper.ge("id", Constant.MIN_SYS_ID);
        }
        return wrapper;
    }

    @Override
    public PageData<TenantDTO> page(DynMap params) {
        PageData<TenantDTO> page = super.page(params);
        fillCurrencies(page.getItems());
        return page;
    }

    @Override
    public List<LabelDTO> getDict(DynMap params) {
        List<Integer> list = params.getList("status", Integer.class, StatusEnum.defaultStatus());
        boolean includeSystemTenant = ReqContextHolder.isSuperAdmin();
        String cacheKey = PaymentRedisKeys.getTenantDictKey(statusCacheKey(list), includeSystemTenant);
        List<LabelDTO> cached = getCachedDict(cacheKey);
        if (cached != null) {
            return cached;
        }

        QueryWrapper<TenantEntity> wrapper = new QueryWrapper<>();
        wrapper.select("id", "name");
        if (!includeSystemTenant) {
            wrapper.ge("id", Constant.MIN_SYS_ID);
        }
        wrapper.in("status", list);
        List<TenantEntity> result = baseDao.selectList(wrapper);

        List<LabelDTO> dict = result.stream()
                .map(item -> new LabelDTO(item.getId(), item.getName()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        cacheDict(cacheKey, dict);
        return dict;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(TenantDTO dto) {
        applyDefaultCurrency(dto, dto.getCurrencyList());
        super.save(dto);
        syncTenantCurrencies(dto.getId(), dto.getCurrencyList());
        evictTenantDictCache();
        if (CollectionUtils.isEmpty(dto.getCurrencyList())) {
            provisionLedgerAccounts(dto.getId(), dto.getCurrency());
        } else {
            provisionLedgerAccounts(dto.getId(), dto.getCurrencyList());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TenantDTO dto) {
        applyDefaultCurrency(dto, dto.getCurrencyList());
        super.update(dto);
        syncTenantCurrencies(dto.getId(), dto.getCurrencyList());
        evictTenantDictCache();
        TenantEntity tenant = baseDao.selectById(dto.getId());
        if (tenant != null) {
            if (CollectionUtils.isEmpty(dto.getCurrencyList())) {
                provisionLedgerAccounts(tenant.getId(), tenant.getCurrency());
            } else {
                provisionLedgerAccounts(tenant.getId(), dto.getCurrencyList());
            }
        }
    }

    @Override
    public void delete(Long[] ids) {
        baseDao.update(null, new UpdateWrapper<TenantEntity>()
                .set("status", StatusEnum.STOP.code())
                .in("id", Arrays.asList(ids)));
        evictTenantDictCache();
    }

    @Override
    public void delete(Long id) {
        baseDao.update(null, new UpdateWrapper<TenantEntity>()
                .set("status", StatusEnum.STOP.code())
                .eq("id", id));
        evictTenantDictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantOnboardResult onboard(TenantOnboardRequest request) {
        requirePlatformSubject();
        validateRequest(request);

        TenantDTO tenant = request.getTenant();
        if (tenant.getStatus() == null) {
            tenant.setStatus(StatusEnum.NORMAL.code());
        }
        save(tenant);

        SysDeptDTO dept = createDefaultDept(tenant.getId(), request.getDeptName());
        SysRoleDTO role = copyTenantRoleTemplate(request.getRoleTemplateId(), tenant.getId());
        SysUserDTO user = createTenantAdminUser(request.getAdminUser(), tenant.getId(), dept.getId(), role.getId());

        return new TenantOnboardResult(tenant.getId(), dept.getId(), role.getId(), user.getId());
    }

    private void requirePlatformSubject() {
        if (!SubjectTypeEnum.PLATFORM.matches(ReqContextHolder.getSubjectType())) {
            throw new GkException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateRequest(TenantOnboardRequest request) {
        if (request == null || request.getTenant() == null || request.getAdminUser() == null || request.getRoleTemplateId() == null) {
            throw new GkException(ErrorCode.NOT_NULL);
        }
        if (StringUtils.isBlank(request.getTenant().getName())) {
            throw new GkException(ErrorCode.NOT_NULL, "tenant.name");
        }
        if (StringUtils.isBlank(request.getTenant().getCode())) {
            throw new GkException(ErrorCode.NOT_NULL, "tenant.code");
        }
        if (StringUtils.isBlank(request.getAdminUser().getUsername())) {
            throw new GkException(ErrorCode.NOT_NULL, "adminUser.username");
        }
        if (StringUtils.isBlank(request.getAdminUser().getPassword())) {
            throw new GkException(ErrorCode.NOT_NULL, "adminUser.password");
        }
    }

    private SysDeptDTO createDefaultDept(Long tenantId, String deptName) {
        SysDeptDTO dept = new SysDeptDTO();
        dept.setTenantId(tenantId);
        dept.setPid(Constant.DEPT_ROOT);
        dept.setName(StringUtils.defaultIfBlank(deptName, "默认部门"));
        dept.setSort(0);
        dept.setStatus(StatusEnum.NORMAL.code());
        sysDeptService.save(dept);
        return dept;
    }

    private SysRoleDTO copyTenantRoleTemplate(Long roleTemplateId, Long tenantId) {
        SysRoleEntity template = sysRoleService.selectById(roleTemplateId);
        if (template == null
                || !SubjectTypeEnum.TENANT.matches(template.getRoleScope())
                || !isRoleTemplate(template)
                || !StatusEnum.NORMAL.code().equals(template.getStatus())) {
            throw new GkException(ErrorCode.FORBIDDEN);
        }

        SysRoleDTO role = new SysRoleDTO();
        role.setTenantId(tenantId);
        role.setAuth(template.getAuth());
        role.setName(template.getName());
        role.setRemark(template.getRemark());
        role.setRoleScope(SubjectTypeEnum.TENANT.code());
        role.setDataScope(template.getDataScope());
        role.setStatus(template.getStatus() == null ? StatusEnum.NORMAL.code() : template.getStatus());
        role.setMenuIdList(sysRoleMenuService.getMenuIdList(roleTemplateId));
        role.setDeptIdList(Collections.emptyList());
        sysRoleService.save(role);
        return role;
    }

    private SysUserDTO createTenantAdminUser(SysUserDTO adminUser, Long tenantId, Long deptId, Long roleId) {
        adminUser.setId(null);
        adminUser.setTenantId(tenantId);
        adminUser.setMerchantId(null);
        adminUser.setDeptId(deptId);
        adminUser.setSubjectType(SubjectTypeEnum.TENANT.code());
        adminUser.setRoleId(roleId);
        adminUser.setRoleIdList(List.of(roleId));
        if (adminUser.getStatus() == null) {
            adminUser.setStatus(StatusEnum.NORMAL.code());
        }
        sysUserService.save(adminUser);
        return adminUser;
    }

    private boolean isRoleTemplate(SysRoleEntity role) {
        Long tenantId = role.getTenantId();
        return tenantId == null || Constant.DEFAULT_TENANT_ID.equals(tenantId);
    }

    private void provisionLedgerAccounts(Long tenantId, String currency) {
        if (tenantId == null || StringUtils.isBlank(currency)) {
            return;
        }
        ledgerAccountService.provisionTenantAccounts(tenantId, currency);
    }

    private void provisionLedgerAccounts(Long tenantId, List<String> currencies) {
        if (tenantId == null || currencies == null || currencies.isEmpty()) {
            return;
        }
        currencies.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .forEach(currency -> provisionLedgerAccounts(tenantId, currency));
    }

    private void applyDefaultCurrency(TenantDTO dto, List<String> currencies) {
        if (dto == null || currencies == null || currencies.isEmpty()) {
            return;
        }
        dto.setCurrency(currencies.getFirst());
    }

    private void syncTenantCurrencies(Long tenantId, List<String> currencies) {
        if (tenantId == null || currencies == null || currencies.isEmpty()) {
            return;
        }

        QueryWrapper<TenantCurrencyEntity> wrapper = new QueryWrapper<>();
        wrapper.select("id", "tenant_id", "currency", "status", "sort");
        wrapper.eq("tenant_id", tenantId);
        List<TenantCurrencyEntity> existingList = tenantCurrencyDao.selectList(wrapper);
        Map<String, TenantCurrencyEntity> existingMap = existingList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getCurrency()))
                .collect(Collectors.toMap(TenantCurrencyEntity::getCurrency, item -> item, (left, right) -> left));

        for (int i = 0; i < currencies.size(); i++) {
            String currency = currencies.get(i);
            TenantCurrencyEntity existing = existingMap.get(currency);
            int sort = (i + 1) * 10;
            if (existing == null) {
                TenantCurrencyEntity entity = new TenantCurrencyEntity();
                entity.setTenantId(tenantId);
                entity.setCurrency(currency);
                entity.setStatus(StatusEnum.NORMAL.code());
                entity.setSort(sort);
                tenantCurrencyDao.insert(entity);
            } else {
                tenantCurrencyDao.update(null, new UpdateWrapper<TenantCurrencyEntity>()
                        .set("status", StatusEnum.NORMAL.code())
                        .set("sort", sort)
                        .eq("id", existing.getId()));
            }
        }

        List<String> removedCurrencies = existingMap.keySet().stream()
                .filter(currency -> !currencies.contains(currency))
                .toList();
        if (!removedCurrencies.isEmpty()) {
            tenantCurrencyDao.update(null, new UpdateWrapper<TenantCurrencyEntity>()
                    .set("status", StatusEnum.STOP.code())
                    .eq("tenant_id", tenantId)
                    .in("currency", removedCurrencies));
        }
    }

    private void fillCurrencies(List<TenantDTO> tenants) {
        if (tenants == null || tenants.isEmpty()) {
            return;
        }
        List<Long> tenantIds = tenants.stream()
                .map(TenantDTO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (tenantIds.isEmpty()) {
            return;
        }

        QueryWrapper<TenantCurrencyEntity> tenantCurrencyWrapper = new QueryWrapper<>();
        tenantCurrencyWrapper.select("tenant_id", "currency", "status", "sort");
        tenantCurrencyWrapper.in("tenant_id", tenantIds);
        tenantCurrencyWrapper.in("status", StatusEnum.defaultStatus());
        tenantCurrencyWrapper.orderByAsc("tenant_id", "sort", "currency");
        List<TenantCurrencyEntity> tenantCurrencies = tenantCurrencyDao.selectList(tenantCurrencyWrapper);
        if (tenantCurrencies == null || tenantCurrencies.isEmpty()) {
            tenants.forEach(item -> item.setCurrencyList(List.of()));
            return;
        }

        Map<Long, List<String>> tenantCurrencyMap = tenantCurrencies.stream()
                .collect(Collectors.groupingBy(
                        TenantCurrencyEntity::getTenantId,
                        Collectors.mapping(
                                TenantCurrencyEntity::getCurrency,
                                Collectors.filtering(
                                        StringUtils::isNotBlank,
                                        Collectors.toList()
                                )
                        )
                ));
        for (TenantDTO tenant : tenants) {
            tenant.setCurrencyList(tenantCurrencyMap.getOrDefault(tenant.getId(), List.of(tenant.getCurrency())));
        }
    }

    private List<LabelDTO> getCachedDict(String cacheKey) {
        try {
            Object cached = redisUtils.get(cacheKey);
            if (cached == null) {
                return null;
            }
            switch (cached) {
                case String text -> {
                    return JSON.parseArray(text, LabelDTO.class);
                }
                case List<?> list -> {
                    List<LabelDTO> result = new ArrayList<>(list.size());
                    for (Object item : list) {
                        LabelDTO dto = ConvertUtils.sourceToTarget(item, LabelDTO.class);
                        if (dto != null) {
                            result.add(dto);
                        }
                    }
                    return result;
                }
                default -> {
                    return null;
                }
            }
        } catch (Exception e) {
            log.warn("Get tenant dict cache failed: {}", e.getMessage());
        }
        return null;
    }

    private void cacheDict(String cacheKey, List<LabelDTO> dict) {
        try {
            redisUtils.set(cacheKey, dict, TENANT_DICT_CACHE_SECONDS);
        } catch (Exception e) {
            log.warn("Set tenant dict cache failed: {}", e.getMessage());
        }
    }

    private void evictTenantDictCache() {
        try {
            Set<String> keys = redisUtils.keys(PaymentRedisKeys.getTenantDictPattern());
            if (keys != null && !keys.isEmpty()) {
                redisUtils.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Evict tenant dict cache failed: {}", e.getMessage());
        }
    }

    private String statusCacheKey(List<Integer> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return "none";
        }
        return statuses.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("none");
    }
}

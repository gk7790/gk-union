package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.meta.dao.SysBankDao;
import com.gk.meta.entity.SysBankEntity;
import com.gk.psp.dao.PspBankMappingDao;
import com.gk.psp.dto.PspBankMappingDTO;
import com.gk.psp.entity.PspBankMappingEntity;
import com.gk.psp.service.PspBankMappingService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PspBankMappingServiceImpl extends CrudServiceImpl<PspBankMappingDao, PspBankMappingEntity, PspBankMappingDTO>
        implements PspBankMappingService {

    private final SysBankDao sysBankDao;

    @Override
    public QueryWrapper<PspBankMappingEntity> getWrapper(DynMap params) {
        QueryWrapper<PspBankMappingEntity> wrapper = new QueryWrapper<>();
        Long pspId = params.getLong("pspId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String bankCode = params.getStr("bankCode");
        String pspBankCode = params.getStr("pspBankCode");

        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(bankCode), "bank_code", bankCode);
        wrapper.eq(StrUtil.isNotBlank(pspBankCode), "psp_bank_code", pspBankCode);
        wrapper.orderByAsc("sort").orderByAsc("bank_code");
        return wrapper;
    }

    @Override
    public PageData<PspBankMappingDTO> page(DynMap params) {
        IPage<PspBankMappingEntity> page = baseDao.selectPage(
                getPage(params, "id", false),
                getWrapper(params)
        );
        List<PspBankMappingDTO> list = page.getRecords().stream().map(this::toDto).toList();
        return getPageData(list, page.getTotal(), PspBankMappingDTO.class);
    }

    @Override
    public PspBankMappingDTO get(Long id) {
        return toDto(baseDao.selectById(id));
    }

    @Override
    public List<PspBankMappingDTO> getMatrix(Long pspId, String countryCode, String currency) {
        MatrixScope scope = resolveScope(pspId, countryCode, currency);

        List<SysBankEntity> banks = listEnabledBanks(scope.countryCode(), scope.currency());
        Map<String, PspBankMappingEntity> mappingMap = listMappings(scope.pspId(), scope.countryCode(), scope.currency())
                .stream()
                .collect(Collectors.toMap(PspBankMappingEntity::getBankCode, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        List<PspBankMappingDTO> items = new ArrayList<>(banks.size());
        for (SysBankEntity bank : banks) {
            PspBankMappingDTO item = new PspBankMappingDTO();
            item.setBankId(bank.getId());
            item.setPspId(scope.pspId());
            item.setCountryCode(scope.countryCode());
            item.setCurrency(scope.currency());
            item.setBankCode(bank.getBankCode());
            item.setBankName(bank.getBankName());
            item.setBankShortName(bank.getBankShortName());

            PspBankMappingEntity mapping = mappingMap.get(bank.getBankCode());
            if (mapping != null) {
                item.setMappingId(mapping.getId());
                item.setPspBankCode(mapping.getPspBankCode());
                item.setStatus(mapping.getStatus());
                item.setRemark(mapping.getRemark());
            }
            items.add(item);
        }
        return items;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMatrix(Long pspId, String countryCode, String currency, List<PspBankMappingDTO> items) {
        MatrixScope scope = resolveScope(pspId, countryCode, currency);
        if (CollectionUtils.isEmpty(items)) {
            return;
        }

        Map<String, SysBankEntity> bankMap = listEnabledBanks(scope.countryCode(), scope.currency()).stream()
                .collect(Collectors.toMap(SysBankEntity::getBankCode, Function.identity(), (a, b) -> a));
        Map<String, PspBankMappingEntity> mappingMap = listMappings(scope.pspId(), scope.countryCode(), scope.currency())
                .stream()
                .collect(Collectors.toMap(PspBankMappingEntity::getBankCode, Function.identity(), (a, b) -> a));

        for (PspBankMappingDTO item : items) {
            String bankCode = StringUtils.trimToNull(item.getBankCode());
            if (bankCode == null) {
                continue;
            }
            SysBankEntity bank = bankMap.get(bankCode);
            if (bank == null) {
                throw new GkException(ErrorCode.NOT_NULL, "bank not found or disabled: " + bankCode);
            }

            String pspBankCode = StringUtils.trimToNull(item.getPspBankCode());
            PspBankMappingEntity existing = mappingMap.get(bankCode);
            if (pspBankCode == null) {
                if (existing != null) {
                    delete(existing.getId());
                    mappingMap.remove(bankCode);
                }
                continue;
            }

            if (existing == null) {
                PspBankMappingEntity entity = new PspBankMappingEntity();
                entity.setPspId(scope.pspId());
                entity.setCountryCode(scope.countryCode());
                entity.setCurrency(scope.currency());
                entity.setBankCode(bankCode);
                entity.setPspBankCode(pspBankCode);
                entity.setStatus(item.getStatus() == null ? StatusEnum.NORMAL.code() : item.getStatus());
                entity.setSort(bank.getSort() == null ? 100 : bank.getSort());
                entity.setRemark(StringUtils.trimToNull(item.getRemark()));
                insert(entity);
                mappingMap.put(bankCode, entity);
                continue;
            }

            existing.setPspBankCode(pspBankCode);
            if (item.getStatus() != null) {
                existing.setStatus(item.getStatus());
            }
            existing.setRemark(StringUtils.trimToNull(item.getRemark()));
            updateById(existing);
        }
    }

    @Override
    public void save(PspBankMappingDTO dto) {
        normalize(dto);
        validate(dto);
        PspBankMappingEntity entity = toEntity(dto);
        if (entity.getStatus() == null) {
            entity.setStatus(StatusEnum.NORMAL.code());
        }
        if (entity.getSort() == null) {
            entity.setSort(100);
        }
        insert(entity);
        dto.setMappingId(entity.getId());
    }

    @Override
    public void update(PspBankMappingDTO dto) {
        normalize(dto);
        validate(dto);
        AssertUtils.isNull(dto.getMappingId(), "mappingId");
        updateById(toEntity(dto));
    }

    private PspBankMappingDTO toDto(PspBankMappingEntity entity) {
        if (entity == null) {
            return null;
        }
        PspBankMappingDTO dto = new PspBankMappingDTO();
        dto.setMappingId(entity.getId());
        dto.setPspId(entity.getPspId());
        dto.setCountryCode(entity.getCountryCode());
        dto.setCurrency(entity.getCurrency());
        dto.setBankCode(entity.getBankCode());
        dto.setPspBankCode(entity.getPspBankCode());
        dto.setStatus(entity.getStatus());
        dto.setRemark(entity.getRemark());
        return dto;
    }

    private PspBankMappingEntity toEntity(PspBankMappingDTO dto) {
        PspBankMappingEntity entity = new PspBankMappingEntity();
        entity.setId(dto.getMappingId());
        entity.setPspId(dto.getPspId());
        entity.setCountryCode(dto.getCountryCode());
        entity.setCurrency(dto.getCurrency());
        entity.setBankCode(dto.getBankCode());
        entity.setPspBankCode(dto.getPspBankCode());
        entity.setStatus(dto.getStatus());
        entity.setRemark(dto.getRemark());
        return entity;
    }

    private List<SysBankEntity> listEnabledBanks(String countryCode, String currency) {
        QueryWrapper<SysBankEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("country_code", countryCode);
        wrapper.eq(StringUtils.isNotBlank(currency),"currency", currency);
        wrapper.in("status", StatusEnum.defaultStatus());
        wrapper.orderByAsc("sort").orderByAsc("bank_code");
        return sysBankDao.selectList(wrapper);
    }

    private List<PspBankMappingEntity> listMappings(Long pspId, String countryCode, String currency) {
        QueryWrapper<PspBankMappingEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("psp_id", pspId);
        wrapper.eq("country_code", countryCode);
        wrapper.eq(StringUtils.isNotBlank(currency), "currency", currency);
        wrapper.orderByAsc("sort").orderByAsc("bank_code");
        return baseDao.selectList(wrapper);
    }

    private MatrixScope resolveScope(Long pspId, String countryCode, String currency) {
        AssertUtils.isNull(pspId, "pspId");
        countryCode = StringUtils.trimToNull(countryCode);
        currency = StringUtils.trimToNull(currency);
        AssertUtils.isBlank(countryCode, "countryCode");
        return new MatrixScope(pspId, countryCode, currency);
    }

    private void normalize(PspBankMappingDTO dto) {
        dto.setCountryCode(StringUtils.trimToNull(dto.getCountryCode()));
        dto.setCurrency(StringUtils.trimToNull(dto.getCurrency()));
        dto.setBankCode(StringUtils.trimToNull(dto.getBankCode()));
        dto.setPspBankCode(StringUtils.trimToNull(dto.getPspBankCode()));
        dto.setRemark(StringUtils.trimToNull(dto.getRemark()));
    }

    private void validate(PspBankMappingDTO dto) {
        AssertUtils.isNull(dto.getPspId(), "pspId");
        AssertUtils.isBlank(dto.getCountryCode(), "countryCode");
        AssertUtils.isBlank(dto.getBankCode(), "bankCode");
        AssertUtils.isBlank(dto.getPspBankCode(), "pspBankCode");

        QueryWrapper<SysBankEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("country_code", dto.getCountryCode());
        wrapper.eq("bank_code", dto.getBankCode());
        wrapper.in("status", StatusEnum.defaultStatus());
        if (sysBankDao.selectCount(wrapper) <= 0) {
            throw new GkException(ErrorCode.NOT_NULL, "bank not found or disabled");
        }
    }

    private record MatrixScope(Long pspId, String countryCode, String currency) {
    }
}

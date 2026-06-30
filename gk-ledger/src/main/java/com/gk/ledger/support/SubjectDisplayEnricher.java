package com.gk.ledger.support;

import com.gk.common.enums.SubjectTypeEnum;
import com.gk.ledger.dao.LedgerAccountDao;
import com.gk.ledger.dto.LedgerAccountDTO;
import com.gk.ledger.dto.LedgerBalanceDTO;
import com.gk.ledger.dto.LedgerEntryDTO;
import com.gk.ledger.dto.LedgerHoldDTO;
import com.gk.ledger.dto.LedgerJournalDTO;
import com.gk.ledger.dto.MerchantWalletStatementDTO;
import com.gk.ledger.entity.LedgerAccountEntity;
import com.gk.subject.model.SubjectDisplay;
import com.gk.subject.model.SubjectRef;
import com.gk.subject.service.SubjectDisplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SubjectDisplayEnricher {
    private final SubjectDisplayService subjectDisplayService;
    private final LedgerAccountDao ledgerAccountDao;

    public void enrichAccounts(Collection<LedgerAccountDTO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        Map<SubjectRef, SubjectDisplay> displays = subjectDisplayService.batchGet(items.stream()
                .flatMap(item -> Stream.of(
                        ref(item.getTenantId(), item.getOwnerType(), item.getOwnerId()),
                        ref(item.getTenantId(), SubjectTypeEnum.TENANT.code(), item.getTenantId())
                ))
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        items.forEach(item -> {
            applyOwner(item, displays.get(ref(item.getTenantId(), item.getOwnerType(), item.getOwnerId())));
            applyTenant(item, displays.get(ref(item.getTenantId(), SubjectTypeEnum.TENANT.code(), item.getTenantId())));
        });
    }

    public void enrichEntries(Collection<LedgerEntryDTO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        Map<SubjectRef, SubjectDisplay> displays = subjectDisplayService.batchGet(items.stream()
                .flatMap(item -> Stream.of(
                        ref(item.getTenantId(), item.getOwnerType(), item.getOwnerId()),
                        ref(item.getTenantId(), SubjectTypeEnum.TENANT.code(), item.getTenantId())
                )).filter(Objects::nonNull)
                .distinct()
                .toList());
        items.forEach(item -> {
            applyOwner(item, displays.get(ref(item.getTenantId(), item.getOwnerType(), item.getOwnerId())));
            applyTenant(item, displays.get(ref(item.getTenantId(), SubjectTypeEnum.TENANT.code(), item.getTenantId())));
        });
    }

    public void enrichHolds(Collection<LedgerHoldDTO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        Map<SubjectRef, SubjectDisplay> displays = subjectDisplayService.batchGet(items.stream()
                .flatMap(item -> Stream.of(
                        ref(item.getTenantId(), item.getOwnerType(), item.getOwnerId()),
                        ref(item.getTenantId(), SubjectTypeEnum.TENANT.code(), item.getTenantId())
                ))
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        items.forEach(item -> {
            applyOwner(item, displays.get(ref(item.getTenantId(), item.getOwnerType(), item.getOwnerId())));
            applyTenant(item, displays.get(ref(item.getTenantId(), SubjectTypeEnum.TENANT.code(), item.getTenantId())));
        });
    }

    public void enrichBalances(Collection<LedgerBalanceDTO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        Set<Long> accountIds = items.stream()
                .map(LedgerBalanceDTO::getAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (accountIds.isEmpty()) {
            return;
        }
        Map<Long, LedgerAccountEntity> accounts = ledgerAccountDao.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(LedgerAccountEntity::getId, Function.identity(), (left, right) -> left));
        List<SubjectRef> refs = items.stream()
                .map(item -> {
                    LedgerAccountEntity account = accounts.get(item.getAccountId());
                    if (account == null) {
                        return null;
                    }
                    item.setOwnerType(account.getOwnerType());
                    item.setOwnerId(account.getOwnerId());
                    return ref(account.getTenantId(), account.getOwnerType(), account.getOwnerId());
                })
                .filter(Objects::nonNull)
                .toList();
        Map<SubjectRef, SubjectDisplay> displays = subjectDisplayService.batchGet(refs);
        items.forEach(item -> {
            LedgerAccountEntity account = accounts.get(item.getAccountId());
            if (account != null) {
                applyOwner(item, displays.get(ref(account.getTenantId(), account.getOwnerType(), account.getOwnerId())));
            }
        });
    }

    public void enrichJournals(Collection<LedgerJournalDTO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        Map<SubjectRef, SubjectDisplay> displays = subjectDisplayService.batchGet(items.stream()
                .map(item -> ref(item.getTenantId(), SubjectTypeEnum.TENANT.code(), item.getTenantId()))
                .filter(Objects::nonNull)
                .toList());
        items.forEach(item -> {
            SubjectDisplay display = displays.get(ref(item.getTenantId(), SubjectTypeEnum.TENANT.code(), item.getTenantId()));
            if (display != null) {
                applyTenant(item, display);
            }
        });
    }

    public void enrichMerchantWalletStatements(Collection<MerchantWalletStatementDTO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        Map<SubjectRef, SubjectDisplay> displays = subjectDisplayService.batchGet(items.stream()
                .flatMap(item -> Stream.of(
                        ref(item.getTenantId(), SubjectTypeEnum.MERCHANT.code(), item.getMerchantId()),
                        ref(item.getTenantId(), SubjectTypeEnum.TENANT.code(), item.getTenantId())
                ))
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        items.forEach(item -> {
            applyMerchant(item, displays.get(ref(item.getTenantId(), SubjectTypeEnum.MERCHANT.code(), item.getMerchantId())));
            applyTenant(item, displays.get(ref(item.getTenantId(), SubjectTypeEnum.TENANT.code(), item.getTenantId())));
        });
    }

    private SubjectRef ref(Long tenantId, String subjectType, Long subjectId) {
        if (tenantId == null || subjectType == null || subjectId == null) {
            return null;
        }
        return new SubjectRef(tenantId, subjectType, subjectId);
    }

    private void applyOwner(LedgerAccountDTO item, SubjectDisplay display) {
        if (display == null) {
            return;
        }
        item.setOwnerName(ownerName(display));
    }

    private void applyTenant(LedgerAccountDTO item, SubjectDisplay display) {
        if (display == null) {
            return;
        }
        item.setTenantName(tenantName(display));
    }

    private void applyOwner(LedgerEntryDTO item, SubjectDisplay display) {
        if (display == null) {
            return;
        }
        item.setOwnerName(ownerName(display));
    }

    private void applyTenant(LedgerEntryDTO item, SubjectDisplay display) {
        if (display == null) {
            return;
        }
        item.setTenantName(tenantName(display));
    }

    private void applyTenant(LedgerJournalDTO item, SubjectDisplay display) {
        if (display == null) {
            return;
        }
        item.setTenantName(tenantName(display));
    }

    private void applyTenant(LedgerHoldDTO item, SubjectDisplay display) {
        if (display == null) {
            return;
        }
        item.setTenantName(tenantName(display));
    }

    private void applyOwner(LedgerHoldDTO item, SubjectDisplay display) {
        if (display == null) {
            return;
        }
        item.setOwnerName(ownerName(display));
    }

    private void applyOwner(LedgerBalanceDTO item, SubjectDisplay display) {
        if (display == null) {
            return;
        }
        item.setOwnerName(ownerName(display));
        item.setOwnerNo(null);
        item.setOwnerShortName(null);
        item.setOwnerDisplayName(null);
    }

    private void applyTenant(MerchantWalletStatementDTO item, SubjectDisplay display) {
        if (display == null) {
            return;
        }
        item.setTenantName(tenantName(display));
    }

    private void applyMerchant(MerchantWalletStatementDTO item, SubjectDisplay display) {
        if (display == null) {
            return;
        }
        item.setMerchantName(ownerName(display));
    }

    private String ownerName(SubjectDisplay display) {
        return display.getSubjectShortName();
    }

    private String tenantName(SubjectDisplay display) {
        if (display.getSubjectName() != null) {
            return display.getSubjectName();
        }
        return display.getDisplayName();
    }
}

package com.gk.telegram.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.telegram.support.NotifyKeyUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.iam.entity.SysUserSubjectEntity;
import com.gk.telegram.alert.TgNotifyEventCodes;
import com.gk.telegram.alert.TgMessageTaskExecutor;
import com.gk.telegram.dao.TgBotDao;
import com.gk.telegram.dao.TgChatDao;
import com.gk.telegram.dao.TgMessageTaskDao;
import com.gk.telegram.dto.TgChatDTO;
import com.gk.telegram.entity.TgBotEntity;
import com.gk.telegram.entity.TgChatEntity;
import com.gk.telegram.entity.TgMessageTaskEntity;
import com.gk.telegram.service.TgChatService;
import com.gk.telegram.support.TgConstants;
import com.gk.telegram.support.TgHtml;
import com.gk.telegram.spi.NotificationSubjectNameResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Telegram 群/会话绑定服务实现。
 * <p>
 * 封装 tg_chat 的后台查询、群绑定新增/恢复、默认订阅分配和解绑状态变更。
 */
@Service
public class TgChatServiceImpl extends CrudServiceImpl<TgChatDao, TgChatEntity, TgChatDTO> implements TgChatService {
    private final TgBotDao tgBotDao;
    private final TgMessageTaskDao tgMessageTaskDao;
    private final TgMessageTaskExecutor tgMessageTaskExecutor;
    private final ObjectProvider<NotificationSubjectNameResolver> subjectNameResolverProvider;

    public TgChatServiceImpl(TgBotDao tgBotDao, TgMessageTaskDao tgMessageTaskDao,
                             TgMessageTaskExecutor tgMessageTaskExecutor,
                             ObjectProvider<NotificationSubjectNameResolver> subjectNameResolverProvider) {
        this.tgBotDao = tgBotDao;
        this.tgMessageTaskDao = tgMessageTaskDao;
        this.tgMessageTaskExecutor = tgMessageTaskExecutor;
        this.subjectNameResolverProvider = subjectNameResolverProvider;
    }

    @Override
    public PageData<TgChatDTO> page(DynMap params) {
        PageData<TgChatDTO> page = super.page(params);
        fillBotInfo(page.getItems());
        fillSubjectInfo(page.getItems());
        return page;
    }

    /**
     * 构造后台 Telegram 会话/群绑定列表查询条件。
     */
    @Override
    public QueryWrapper<TgChatEntity> getWrapper(DynMap params) {
        QueryWrapper<TgChatEntity> wrapper = new QueryWrapper<>();
        Long tenantId = queryTenantId(params);
        Long merchantId = params.getLong("merchantId", null);
        Long botId = params.getLong("botId", null);
        Long chatId = params.getLong("chatId", null);
        String chatType = params.getStr("chatType");
        String purpose = params.getStr("purpose");
        Integer status = params.containsKey("status") ? params.getInt("status") : null;

        wrapper.gt("id", Constant.MAX_RESERVED_ID);
        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(botId != null, "bot_id", botId);
        wrapper.eq(chatId != null, "chat_id", chatId);
        wrapper.eq(StrUtil.isNotBlank(chatType), "chat_type", chatType);
        wrapper.eq(StrUtil.isNotBlank(purpose), "purpose", purpose);
        wrapper.eq(status != null, "status", status);
        return wrapper;
    }

    private Long queryTenantId(DynMap params) {
        if (!ReqContextHolder.isPlatform()) {
            Long tenantId = ReqContextHolder.getTenantId();
            return tenantId == null ? -1L : tenantId;
        }
        return params.getLong("tenantId", null);
    }

    /**
     * 查询指定 bot 下某个 Telegram chat 的有效绑定记录。
     */
    @Override
    public TgChatEntity getActiveChat(Long botId, Long chatId) {
        if (botId == null || chatId == null) {
            return null;
        }
        return baseDao.selectOne(new QueryWrapper<TgChatEntity>()
                .eq("bot_id", botId)
                .eq("chat_id", chatId)
                .eq("status", 1)
                .last("limit 1"));
    }

    /**
     * 批量填充机器人展示信息，避免分页列表逐行查询机器人表。
     */
    private void fillBotInfo(List<TgChatDTO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> botIds = items.stream()
                .map(TgChatDTO::getBotId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (botIds.isEmpty()) {
            return;
        }
        Map<Long, TgBotEntity> botMap = tgBotDao.selectByIds(botIds).stream()
                .collect(Collectors.toMap(TgBotEntity::getId, Function.identity(), (left, right) -> left));
        for (TgChatDTO item : items) {
            TgBotEntity bot = botMap.get(item.getBotId());
            if (bot == null) {
                continue;
            }
            item.setBotName(StringUtils.defaultIfBlank(bot.getName(),
                    StringUtils.defaultIfBlank(bot.getUsername(), bot.getBotNo())));
        }
    }

    private void fillSubjectInfo(List<TgChatDTO> items) {
        NotificationSubjectNameResolver resolver = subjectNameResolverProvider.getIfAvailable();
        if (resolver == null || items == null) return;
        for (TgChatDTO item : items) {
            item.setTenantName(resolver.resolve(item.getTenantId(), SubjectTypeEnum.TENANT.code(), item.getTenantId()));
            item.setMerchantName(resolver.resolve(item.getTenantId(), SubjectTypeEnum.MERCHANT.code(), item.getMerchantId()));
        }
    }

    /**
     * 新增或恢复 Telegram 群绑定。
     * <p>
     * 表上存在 bot_id + chat_id 唯一键，所以这里按唯一键做 upsert 语义。
     * 首次绑定会根据主体类型写入默认订阅事件；重新绑定时如果已有自定义 event_types，则不覆盖。
     */
    @Override
    public void bindSubjectChat(Long botId, Long chatId, String chatType, String title, String languageCode,
                                SysUserSubjectEntity subject) {
        if (botId == null || chatId == null || subject == null || subject.getId() == null) {
            throw new IllegalArgumentException("botId, chatId and subject are required");
        }
        Instant now = Instant.now();
        String normalizedType = StringUtils.defaultIfBlank(chatType, "GROUP").toUpperCase(Locale.ROOT);
        String lang = StringUtils.defaultIfBlank(languageCode, "en-US");
        TgChatEntity existed = baseDao.selectOne(new QueryWrapper<TgChatEntity>()
                .eq("bot_id", botId)
                .eq("chat_id", chatId)
                .last("limit 1"));

        if (existed == null) {
            TgChatEntity entity = new TgChatEntity();
            entity.setTenantId(defaultId(subject.getTenantId()));
            entity.setMerchantId(defaultId(subject.getSubjectId()));
            entity.setBotId(botId);
            entity.setChatId(chatId);
            entity.setChatType(normalizedType);
            entity.setTitle(title);
            entity.setPurpose(defaultPurpose(subject));
            entity.setEventTypes(defaultEventTypes(subject));
            entity.setLang(lang);
            entity.setStatus(1);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            baseDao.insert(entity);
            return;
        }

        TgChatEntity update = new TgChatEntity();
        update.setId(existed.getId());
        update.setTenantId(defaultId(subject.getTenantId()));
        update.setMerchantId(defaultId(subject.getSubjectId()));
        update.setChatType(normalizedType);
        update.setTitle(title);
        update.setLang(lang);
        if (StringUtils.isBlank(existed.getPurpose())) {
            update.setPurpose(defaultPurpose(subject));
        }
        if (StringUtils.isBlank(existed.getEventTypes())) {
            update.setEventTypes(defaultEventTypes(subject));
        }
        update.setStatus(1);
        update.setUpdatedAt(now);
        baseDao.updateById(update);

        existed.setTenantId(defaultId(subject.getTenantId()));
        existed.setMerchantId(defaultId(subject.getSubjectId()));
        existed.setChatType(normalizedType);
        existed.setTitle(title);
        existed.setLang(lang);
        if (StringUtils.isBlank(existed.getPurpose())) {
            existed.setPurpose(defaultPurpose(subject));
        }
        if (StringUtils.isBlank(existed.getEventTypes())) {
            existed.setEventTypes(defaultEventTypes(subject));
        }
        existed.setStatus(1);
        existed.setUpdatedAt(now);
    }

    @Override
    public List<TgChatDTO> listActiveMerchantChats(List<Long> merchantIds) {
        List<Long> ids = normalizeMerchantIds(merchantIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        QueryWrapper<TgChatEntity> wrapper = activeMerchantChatWrapper()
                .in("merchant_id", ids)
                .orderByDesc("updated_at")
                .orderByDesc("id");
        List<TgChatDTO> items = ConvertUtils.sourceToTarget(baseDao.selectList(wrapper), TgChatDTO.class);
        fillBotInfo(items);
        return items;
    }

    @Override
    public void unbindMerchantChat(Long merchantId) {
        Long normalizedMerchantId = normalizeMerchantId(merchantId);
        if (normalizedMerchantId == null) {
            return;
        }
        List<TgChatEntity> targets = baseDao.selectList(activeMerchantChatWrapper()
                .eq("merchant_id", normalizedMerchantId)
                .orderByDesc("updated_at")
                .orderByDesc("id"));
        Instant now = Instant.now();
        String unbindContent = TgHtml.code("商户 Telegram 群已解绑") + "\n"
                + "──────────────\n"
                + TgHtml.escape("当前群已从商户管理后台解绑，后续将不再接收该商户的机器人通知。")
                + "\n"
                + TgHtml.escape("发送时间：" + now);
        for (TgChatEntity target : targets) {
            tgMessageTaskDao.insert(buildMerchantChatTask(
                    target,
                    unbindContent,
                    null,
                    "MERCHANT_TG_UNBIND",
                    "merchant-tg-unbind",
                    now
            ));
        }
        baseDao.update(null, activeMerchantChatUpdateWrapper(normalizedMerchantId)
                .set("status", 3)
                .set("updated_at", now));
    }

    @Override
    public void sendMerchantMessage(Long merchantId, String content, String payloadJson) {
        Long normalizedMerchantId = normalizeMerchantId(merchantId);
        if (normalizedMerchantId == null) {
            throw new IllegalArgumentException("merchantId is required");
        }
        if (StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("message content is required");
        }
        TgHtml.assertTelegramHtml(content);
        String normalizedPayloadJson = normalizeMessagePayloadJson(payloadJson);
        List<TgChatEntity> targets = baseDao.selectList(activeMerchantChatWrapper()
                .eq("merchant_id", normalizedMerchantId)
                .orderByDesc("updated_at")
                .orderByDesc("id"));
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("merchant telegram chat is not bound");
        }
        Instant now = Instant.now();
        int successCount = 0;
        for (TgChatEntity target : targets) {
            TgMessageTaskEntity task = buildMerchantChatTask(
                    target,
                    content.trim(),
                    normalizedPayloadJson,
                    "MERCHANT_TG_NOTICE",
                    "merchant-tg-notice",
                    now
            );
            tgMessageTaskDao.insert(task);
            if (tgMessageTaskExecutor.sendNow(task.getId())) {
                successCount++;
            }
        }
        if (successCount == 0) {
            throw new GkException("Telegram 通知已创建，但即时发送失败，将由后台任务重试");
        }
    }

    private QueryWrapper<TgChatEntity> activeMerchantChatWrapper() {
        QueryWrapper<TgChatEntity> wrapper = new QueryWrapper<>();
        Long tenantId = queryTenantId(new DynMap());
        wrapper.gt("id", Constant.MAX_RESERVED_ID)
                .eq("status", 1)
                .gt("merchant_id", 0);
        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        return wrapper;
    }

    private UpdateWrapper<TgChatEntity> activeMerchantChatUpdateWrapper(Long merchantId) {
        UpdateWrapper<TgChatEntity> wrapper = new UpdateWrapper<>();
        Long tenantId = queryTenantId(new DynMap());
        wrapper.gt("id", Constant.MAX_RESERVED_ID)
                .eq("status", 1)
                .eq("merchant_id", merchantId);
        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        return wrapper;
    }

    private List<Long> normalizeMerchantIds(List<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return List.of();
        }
        return merchantIds.stream()
                .map(this::normalizeMerchantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Long normalizeMerchantId(Long merchantId) {
        return merchantId == null || merchantId <= Constant.MAX_RESERVED_ID ? null : merchantId;
    }

    private TgMessageTaskEntity buildMerchantChatTask(TgChatEntity target, String text, String payloadJson,
                                                      String bizNo, String tracePrefix, Instant now) {
        TgMessageTaskEntity task = new TgMessageTaskEntity();
        task.setTenantId(target.getTenantId());
        task.setMerchantId(target.getMerchantId());
        task.setBotId(target.getBotId());
        task.setChatId(target.getChatId());
        task.setTaskNo(NotifyKeyUtils.messageTaskNo());
        task.setBizType(TgConstants.MessageBizType.BUSINESS_NOTIFY);
        task.setBizNo(bizNo);
        task.setEventType(TgNotifyEventCodes.MERCHANT_NOTICE);
        task.setSourceEventId(tracePrefix + "-" + target.getId() + "-" + now.toEpochMilli());
        task.setParseMode(TgConstants.ParseMode.HTML);
        task.setContent(text);
        task.setPayloadJson(payloadJson);
        task.setStatus("INIT");
        task.setRetryCount(0);
        task.setMaxRetryCount(8);
        task.setNextRetryAt(now);
        task.setTraceId(task.getSourceEventId());
        return task;
    }

    private String normalizeMessagePayloadJson(String payloadJson) {
        if (StringUtils.isBlank(payloadJson)) {
            return null;
        }
        JSONObject payload;
        try {
            payload = JSON.parseObject(payloadJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("message payloadJson is invalid");
        }
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        for (String key : payload.keySet()) {
            if (!"reply_markup".equals(key)) {
                throw new IllegalArgumentException("Only reply_markup is supported in payloadJson");
            }
        }
        return payload.toJSONString();
    }

    /**
     * 根据绑定主体给群设置默认用途。
     * <p>
     * purpose 只用于后台展示分类，不参与通知投递过滤。
     */
    private String defaultPurpose(SysUserSubjectEntity subject) {
        if (subject == null) {
            return TgConstants.ChatPurpose.NOTIFY;
        }
        if (SubjectTypeEnum.PLATFORM.matches(subject.getSubjectType())
                || SubjectTypeEnum.TENANT.matches(subject.getSubjectType())) {
            return TgConstants.ChatPurpose.OPS;
        }
        return TgConstants.ChatPurpose.NOTIFY;
    }

    /**
     * 根据绑定主体分配默认订阅事件。
     * <p>
     * 商户群通常只有一个，所以默认接收商户相关的业务、风控、渠道和订单通知；
     * PSP_NOTICE 属于内部四方异常，默认只给平台/租户侧后续手动配置，不主动分配给商户群。
     * 平台/租户群默认接收系统和风控类通知。
     */
    private String defaultEventTypes(SysUserSubjectEntity subject) {
        if (subject != null && SubjectTypeEnum.MERCHANT.matches(subject.getSubjectType())) {
            return TgNotifyEventCodes.merchantDefaults();
        }
        return TgNotifyEventCodes.opsDefaults();
    }

    private Long defaultId(Long id) {
        return id == null ? 0L : id;
    }

    /**
     * 解绑群会话，保留记录用于审计和后续恢复。
     */
    @Override
    public void unbind(Long id) {
        baseDao.update(null, new UpdateWrapper<TgChatEntity>()
                .eq("id", id)
                .set("status", 3)
                .set("updated_at", Instant.now()));
    }
}

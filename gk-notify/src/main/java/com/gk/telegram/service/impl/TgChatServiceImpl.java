package com.gk.telegram.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.telegram.dao.TgChatDao;
import com.gk.telegram.dto.TgChatDTO;
import com.gk.telegram.entity.TgChatEntity;
import com.gk.telegram.service.TgChatService;
import org.springframework.stereotype.Service;

@Service
public class TgChatServiceImpl extends CrudServiceImpl<TgChatDao, TgChatEntity, TgChatDTO> implements TgChatService {

    @Override
    public QueryWrapper<TgChatEntity> getWrapper(DynMap params) {
        QueryWrapper<TgChatEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long botId = params.getLong("botId", null);
        Long chatId = params.getLong("chatId", null);
        String chatType = params.getStr("chatType");
        String purpose = params.getStr("purpose");
        Integer status = params.containsKey("status") ? params.getInt("status") : null;

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(botId != null, "bot_id", botId);
        wrapper.eq(chatId != null, "chat_id", chatId);
        wrapper.eq(StrUtil.isNotBlank(chatType), "chat_type", chatType);
        wrapper.eq(StrUtil.isNotBlank(purpose), "purpose", purpose);
        wrapper.eq(status != null, "status", status);
        return wrapper;
    }
}

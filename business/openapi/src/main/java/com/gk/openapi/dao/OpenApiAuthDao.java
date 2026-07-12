package com.gk.openapi.dao;

import com.gk.openapi.security.OpenApiAuthSnapshotRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OpenApiAuthDao {

    OpenApiAuthSnapshotRow selectAuthSnapshotByAppId(@Param("appId") String appId);

    List<String> selectIpWhitelistPatterns(@Param("tenantId") Long tenantId,
                                           @Param("merchantId") Long merchantId);

    List<String> selectAppIdsByMerchant(@Param("tenantId") Long tenantId,
                                        @Param("merchantId") Long merchantId);
}

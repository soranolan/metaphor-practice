package com.practice.metaphor.mapper;

import com.practice.metaphor.model.entity.Balance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 持倉數據訪問層 (MyBatis)
 */
@Mapper
public interface BalanceMapper {
    /**
     * 查詢特定交易員的特定資產餘額
     */
    Optional<Balance> findByTraderIdAndAssetId(@Param("traderId") Long traderId, 
                                              @Param("assetId") Long assetId);

    /**
     * 加鎖查詢餘額 (SELECT FOR UPDATE)
     * 並行下單時，這能在交易結束前鎖定該列，防止超賣情況
     *
     * @param traderId 交易員 ID
     * @param assetId  資產 ID
     * @return 餘額物件
     */
    Balance findByTraderIdAndAssetIdForUpdate(@Param("traderId") Long traderId, @Param("assetId") Long assetId);

    /**
     * 更新餘額 (可用金額與凍結金額)
     *
     * @param traderId        交易員 ID
     * @param assetId         資產 ID
     * @param availableAmount 新的可用金額
     * @param frozenAmount    新的凍結金額
     * @return 影響行數
     */
    int updateBalance(@Param("traderId") Long traderId, 
                      @Param("assetId") Long assetId, 
                      @Param("availableAmount") java.math.BigDecimal availableAmount, 
                      @Param("frozenAmount") java.math.BigDecimal frozenAmount);
}

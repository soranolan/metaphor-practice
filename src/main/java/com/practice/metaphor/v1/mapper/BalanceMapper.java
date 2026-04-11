package com.practice.metaphor.v1.mapper;

import com.practice.metaphor.v1.model.entity.Balance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;



/**
 * 持倉數據訪問層 (MyBatis)
 */
@Mapper
public interface BalanceMapper {
    /**
     * 查詢特定交易員的所有資產餘額列表
     */
    java.util.List<Balance> findByTraderId(@Param("traderId") Long traderId);

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

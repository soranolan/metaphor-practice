package com.practice.metaphor.v2.disruptor.event;

import com.lmax.disruptor.EventFactory;

/**
 * Disruptor 預分配 TradeResultEventV2 的工廠。
 */
public class TradeResultEventFactoryV2 implements EventFactory<TradeResultEventV2> {

    @Override
    public TradeResultEventV2 newInstance() {
        return new TradeResultEventV2();
    }
}

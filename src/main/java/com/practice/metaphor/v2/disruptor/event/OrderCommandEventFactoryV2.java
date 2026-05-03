package com.practice.metaphor.v2.disruptor.event;

import com.lmax.disruptor.EventFactory;

/**
 * Disruptor 預分配 OrderCommandEventV2 的工廠。
 */
public class OrderCommandEventFactoryV2 implements EventFactory<OrderCommandEventV2> {

    @Override
    public OrderCommandEventV2 newInstance() {
        return new OrderCommandEventV2();
    }
}

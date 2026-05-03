package com.practice.metaphor.v2.util;

import org.springframework.stereotype.Component;

/**
 * Snowflake ID 產生器 (V2)
 *
 * <p>結構：
 * [1 bit unused] [41 bits timestamp] [10 bits workerId] [12 bits sequence]
 */
@Component
public class SnowflakeIdGeneratorV2 {

    private final long twepoch = 1714131600000L; // 2024-04-26 11:40:00 (自定義起始時間)
    private final long workerIdBits = 10L;

    private final long sequenceBits = 12L;

    private final long workerIdShift = sequenceBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits;
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long workerId = 1L; // 預設 workerId 為 1
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }
}

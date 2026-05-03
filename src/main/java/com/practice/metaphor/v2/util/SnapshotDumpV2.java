package com.practice.metaphor.v2.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 快照內容傾印工具
 */
public class SnapshotDumpV2 {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public static void main(String[] args) throws IOException {
        String path = args.length > 0 ? args[0] : "snapshots";
        File target = new File(path);

        if (!target.exists()) {
            System.err.println("❌ 找不到路徑: " + path);
            return;
        }

        File latest = target.isDirectory() ? findLatest(target) : target;
        if (latest == null) {
            System.err.println("❌ 目錄內沒有快照檔案 (.bin)");
            return;
        }

        dump(latest);
    }

    private static File findLatest(File dir) {
        File[] files = dir.listFiles(f -> f.getName().endsWith(".bin"));
        if (files == null || files.length == 0) return null;
        File latest = files[0];
        for (File f : files) {
            if (f.getName().compareTo(latest.getName()) > 0) latest = f;
        }
        return latest;
    }

    private static void dump(File file) throws IOException {
        System.out.println("==========================================================================");
        System.out.println("🔍 讀取快照檔案: " + file.getAbsolutePath());
        System.out.println("==========================================================================");

        byte[] data = Files.readAllBytes(file.toPath());
        Bytes<ByteBuffer> bytes = Bytes.wrapForRead(ByteBuffer.wrap(data));
        try {
            Wire wire = WireType.BINARY_LIGHT.apply(bytes);

            long chronicleIndex = wire.read("chronicleIndex").int64();
            long createdAt = wire.read("createdAt").int64();

            System.out.println("基本資訊:");
            System.out.println("  - Chronicle Index: " + Long.toHexString(chronicleIndex));
            System.out.println("  - 建立時間: " + FMT.format(Instant.ofEpochMilli(createdAt)));
            System.out.println();

            System.out.println("帳本狀態 (可用餘額):");
            int availableSize = wire.read("availableSize").int32();
            for (int i = 0; i < availableSize; i++) {
                System.out.println("  - " + wire.read("k").text() + ": " + wire.read("v").text());
            }
            if (availableSize == 0) System.out.println("  (空)");
            System.out.println();

            System.out.println("帳本狀態 (凍結餘額):");
            int frozenSize = wire.read("frozenSize").int32();
            for (int i = 0; i < frozenSize; i++) {
                System.out.println("  - " + wire.read("k").text() + ": " + wire.read("v").text());
            }
            if (frozenSize == 0) System.out.println("  (空)");
            System.out.println();

            System.out.println("訂單簿狀態 (掛單中):");
            int ordersSize = wire.read("ordersSize").int32();
            for (int i = 0; i < ordersSize; i++) {
                long orderId = wire.read("orderId").int64();
                long traderId = wire.read("traderId").int64();
                int side = wire.read("side").int32();
                String price = wire.read("price").text();
                String totalQty = wire.read("totalQty").text();
                String filledQty = wire.read("filledQty").text();

                System.out.printf("  - [%s] OrderID: %d | Trader: %d | Price: %s | Qty: %s/%s\n",
                        (side == 0 ? "BUY" : "SELL"), orderId, traderId, price, filledQty, totalQty);
            }
            if (ordersSize == 0) System.out.println("  (空)");

        } finally {
            bytes.releaseLast();
        }
        System.out.println("==========================================================================");
    }
}

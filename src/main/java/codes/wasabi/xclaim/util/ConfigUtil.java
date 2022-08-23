package codes.wasabi.xclaim.util;

import codes.wasabi.xclaim.XClaim;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class ConfigUtil {

    public static boolean worldIsAllowed(FileConfiguration cfg, World world) {
        String worldName = world.getName();
        boolean cs = cfg.getBoolean("worlds.case-sensitive", true);
        boolean black = cfg.getBoolean("worlds.use-blacklist", false);
        if (black) {
            for (String name : cfg.getStringList("worlds.blacklist")) {
                if (cs ? name.equals(worldName) : name.equalsIgnoreCase(worldName)) return false;
            }
        }
        boolean white = cfg.getBoolean("worlds.use-whitelist", false);
        if (white) {
            for (String name : cfg.getStringList("worlds.whitelist")) {
                if (cs ? name.equals(worldName) : name.equalsIgnoreCase(worldName)) return true;
            }
            return false;
        }
        return true;
    }

    public static ChunkAllowedResponse chunkIsAllowed(FileConfiguration cfg, Collection<Chunk> others, Chunk chunk) {
        boolean diagonal = true;
        int ox = chunk.getX();
        int oz = chunk.getZ();
        if (cfg.contains("chunks.claim-rule", true)) {
            int claimRule = cfg.getInt("chunks.claim-rule", 2);
            if (claimRule == 0) return ChunkAllowedResponse.YES;
            if (claimRule == 1) diagonal = false;
            if (claimRule == 3) {
                int chunkMaxInner = cfg.getInt("chunks.max-inner-distance", 4);
                boolean considerMaxInner = chunkMaxInner > 0;
                double maxInnerSqr = Math.pow(chunkMaxInner, 2d);
                int chunkMaxOuter = cfg.getInt("chunks.max-outer-distance", 36);
                boolean considerMaxOuter = chunkMaxOuter > 0;
                double maxOuterSqr = Math.pow(chunkMaxOuter, 2d);
                if ((!considerMaxInner) && (!considerMaxOuter)) return ChunkAllowedResponse.YES;
                //
                boolean anyPass = false;
                for (Chunk c : others) {
                    double distSqr = Math.pow(c.getX() - ox, 2) + Math.pow(c.getZ() - oz, 2);
                    if (considerMaxOuter && (distSqr > maxOuterSqr)) {
                        return new ChunkAllowedResponse(
                                false,
                                XClaim.lang.getComponent(
                                        "chunk-editor-max-outer" + (chunkMaxOuter > 1 ? "-plural" : ""),
                                        chunkMaxOuter
                                )
                        );
                    }
                    if (considerMaxInner && (distSqr <= maxInnerSqr)) {
                        anyPass = true;
                        if (!considerMaxOuter) break;
                    }
                }
                if (considerMaxInner && (!anyPass)) {
                    return new ChunkAllowedResponse(
                            false,
                            XClaim.lang.getComponent(
                                    "chunk-editor-max-inner" + (chunkMaxInner > 1 ? "-plural" : ""),
                                    chunkMaxInner
                            )
                    );
                }
                return ChunkAllowedResponse.YES;
            }
        } else {
            // legacy config support (<= 1.9.3)
            if (!cfg.getBoolean("enforce-adjacent-claim-chunks", true)) return ChunkAllowedResponse.YES;
            diagonal = cfg.getBoolean("allow-diagonal-claim-chunks", true);
        }
        Set<Long> valid = new HashSet<>();
        for (int x=-1; x < 2; x++) {
            for (int z=-1; z < 2; z++) {
                if (!diagonal) {
                    if (!((x == 0) || (z == 0))) continue;
                }
                valid.add(IntLongConverter.intToLong(ox + x, oz + z));
            }
        }
        for (Chunk c : others) {
            long key = IntLongConverter.intToLong(c.getX(), c.getZ());
            if (valid.contains(key)) return ChunkAllowedResponse.YES;
        }
        return new ChunkAllowedResponse(false, XClaim.lang.getComponent("chunk-editor-adjacent"));
    }

    public static class ChunkAllowedResponse {

        static final ChunkAllowedResponse YES = new ChunkAllowedResponse(true, Component.empty());

        private final boolean allowed;
        private final Component message;
        ChunkAllowedResponse(boolean allowed, Component message) {
            this.allowed = allowed;
            this.message = message;
        }

        public final boolean isAllowed() {
            return allowed;
        }

        public final Component getMessage() {
            return message;
        }

        @Override
        public int hashCode() {
            return Objects.hash(allowed, message);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ChunkAllowedResponse) {
                ChunkAllowedResponse other = (ChunkAllowedResponse) obj;
                if (allowed == other.allowed) {
                    if (Objects.equals(message, other.message)) return true;
                }
            }
            return super.equals(obj);
        }

        @Override
        public String toString() {
            return "ChunkAllowedResponse[allowed=" + allowed + ",message=" + message + "]";
        }

    }

}

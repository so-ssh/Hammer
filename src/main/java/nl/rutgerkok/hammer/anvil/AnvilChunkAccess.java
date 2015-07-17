package nl.rutgerkok.hammer.anvil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.rutgerkok.hammer.ChunkAccess;
import nl.rutgerkok.hammer.GameFactory;
import nl.rutgerkok.hammer.anvil.RegionFileCache.Claim;
import nl.rutgerkok.hammer.anvil.tag.AnvilFormat.ChunkRootTag;
import nl.rutgerkok.hammer.anvil.tag.AnvilNbtReader;
import nl.rutgerkok.hammer.anvil.tag.AnvilNbtWriter;
import nl.rutgerkok.hammer.tag.CompoundTag;

/**
 * Provides non-sequential access to the chunks in a world.
 *
 */
final class AnvilChunkAccess implements ChunkAccess<AnvilChunk> {

    private final GameFactory gameFactory;
    private final RegionFileCache cache;
    private final Claim claim;

    public AnvilChunkAccess(GameFactory gameFactory, RegionFileCache cache) {
        this.gameFactory = gameFactory;
        this.cache = cache;

        this.claim = cache.claim();
    }

    @Override
    public void close() {
        claim.close();
    }

    private InputStream getChunkInputStream(int chunkX, int chunkZ) throws IOException {
        RegionFile regionFile = cache.getRegionFile(chunkX, chunkZ);
        return regionFile.getChunkInputStream(chunkX & 31, chunkZ & 31);
    }

    private OutputStream getChunkOutputStream(int chunkX, int chunkZ) throws IOException {
        RegionFile regionFile = cache.getRegionFile(chunkX, chunkZ);
        return regionFile.getChunkOutputStream(chunkX & 31, chunkZ & 31);
    }

    @Override
    public AnvilChunk getChunk(int chunkX, int chunkZ) throws IOException {
        try (InputStream stream = getChunkInputStream(chunkX, chunkZ)) {
            if (stream == null) {
                // Chunk doesn't exist yet
                return new AnvilChunk(gameFactory, new CompoundTag());
            }

            // Read the chunk
            CompoundTag chunkTag = AnvilNbtReader.readFromUncompressedStream(stream).getCompound(ChunkRootTag.MINECRAFT);
            return new AnvilChunk(gameFactory, chunkTag);
        }
    }

    @Override
    public void saveChunk(AnvilChunk chunk) throws IOException {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();

        try (OutputStream outputStream = getChunkOutputStream(chunkX, chunkZ)) {
            CompoundTag root = new CompoundTag();
            root.setCompound(ChunkRootTag.MINECRAFT, chunk.getTag());
            AnvilNbtWriter.writeUncompressedToStream(outputStream, root);
        }
    }
}
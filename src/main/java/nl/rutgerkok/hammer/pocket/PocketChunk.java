package nl.rutgerkok.hammer.pocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nl.rutgerkok.hammer.Chunk;
import nl.rutgerkok.hammer.GameFactory;
import nl.rutgerkok.hammer.material.MaterialData;
import nl.rutgerkok.hammer.tag.CompoundTag;
import nl.rutgerkok.hammer.util.MaterialNotFoundException;
import nl.rutgerkok.hammer.util.NibbleArray;

public final class PocketChunk implements Chunk {

    private static final int CHUNK_X_BITS = 4;
    private static final int CHUNK_X_SIZE = 1 << CHUNK_X_BITS;

    private static final int CHUNK_Y_BITS = 7;
    private static final int CHUNK_Y_SIZE = 1 << CHUNK_Y_BITS;

    private static final int CHUNK_Z_BITS = 4;
    private static final int CHUNK_Z_SIZE = 1 << CHUNK_Z_BITS;

    private static final int OFFSET_BLOCK_DATA = 32_768;
    private static final int OFFSET_BLOCK_IDS = 0;
    /**
     * Unused currently, but can be used in the future when light values can be
     * inspected/modified. Until then, this constant simply serves as a form of
     * documentation. Same story for the other unused constants in this class.
     */
    @SuppressWarnings("unused")
    private static final int OFFSET_BLOCKLIGHT_DATA = 32_768 + 16_384 + 16_384;
    @SuppressWarnings("unused")
    private static final int OFFSET_COLOR_DATA = 32_768 + 16_384 + 16_384 + 16_384 + 256;
    @SuppressWarnings("unused")
    private static final int OFFSET_MARKER_DATA = 32_768 + 16_384 + 16_384 + 16_384;
    @SuppressWarnings("unused")
    private static final int OFFSET_SKYLIGHT_DATA = 32_768 + 16_384;
    private static final int TOTAL_BYTE_LENGTH = 32_768 + 16_384 + 16_384 + 16_384 + 256 + 1024;

    /**
     * Creates a new, empty chunk.
     *
     * @param gameFactory
     *            Game factory belonging to Pocket Edition.
     * @param chunkX
     *            Chunk x coordinate.
     * @param chunkZ
     *            Chunk z coordinate.
     * @return The chunk.
     */
    static PocketChunk newEmptyChunk(PocketGameFactory gameFactory, int chunkX, int chunkZ) {
        return new PocketChunk(gameFactory, chunkX, chunkZ, new byte[TOTAL_BYTE_LENGTH], new ArrayList<CompoundTag>(),
                new ArrayList<CompoundTag>());
    }

    private final byte[] bytes;
    private final int chunkX;
    private final int chunkZ;
    private final List<CompoundTag> entities;
    private final PocketGameFactory gameFactory;

    private final List<CompoundTag> tileEntities;

    PocketChunk(PocketGameFactory gameFactory, int chunkX, int chunkZ, byte[] bytes, List<CompoundTag> entities,
            List<CompoundTag> tileEntities) {
        this.gameFactory = Objects.requireNonNull(gameFactory, "gameFactory");
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.bytes = Objects.requireNonNull(bytes, "bytes");
        this.entities = Objects.requireNonNull(entities, "entities");
        this.tileEntities = Objects.requireNonNull(tileEntities, "tileEntities");
    }

    /**
     * Gets the raw bytes for block and light storage.
     * 
     * @return The raw bytes.
     */
    byte[] accessBytes() {
        return bytes;
    }

    private void checkOutOfBounds(int x, int y, int z) {
        if (isOutOfBounds(x, y, z)) {
            throw new IndexOutOfBoundsException(
                    "(" + x + "," + y + "," + z + ") is outside the chunk, which ranges from (0,0,0) to ("
                            + CHUNK_X_SIZE + "," + CHUNK_Y_SIZE + "," + CHUNK_Z_SIZE + ")");
        }
    }

    private int getArrayPos(int x, int y, int z) {
        return (y | (z << CHUNK_Y_BITS) | (x << (CHUNK_Y_BITS + CHUNK_Z_BITS)));
    }

    @Override
    public int getChunkX() {
        return chunkX;
    }

    @Override
    public int getChunkZ() {
        return chunkZ;
    }

    @Override
    public List<CompoundTag> getEntities() {
        return entities;
    }

    @Override
    public GameFactory getGameFactory() {
        return gameFactory;
    }

    @Override
    public MaterialData getMaterial(int x, int y, int z) throws MaterialNotFoundException {
        checkOutOfBounds(x, y, z);

        int arrayPos = getArrayPos(x, y, z);
        short blockId = (short) (bytes[OFFSET_BLOCK_IDS + arrayPos] & 0xff);
        // The * 2 comes from that the nibble array has two position per byte
        byte blockData = NibbleArray.getInArray(bytes, arrayPos + OFFSET_BLOCK_DATA * 2);

        return this.gameFactory.getMaterialMap().getMaterialData(blockId, blockData);
    }

    @Override
    public int getSizeX() {
        return CHUNK_X_SIZE;
    }

    @Override
    public int getSizeY() {
        return CHUNK_Y_SIZE;
    }

    @Override
    public int getSizeZ() {
        return CHUNK_Z_SIZE;
    }

    @Override
    public CompoundTag getTag() {
        // This doesn't exist in the Pocket Edition level format
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CompoundTag> getTileEntities() {
        return tileEntities;
    }

    @Override
    public boolean isOutOfBounds(int x, int y, int z) {
        return x < 0 || x >= CHUNK_X_SIZE || y < 0 || y >= CHUNK_Y_SIZE || z < 0 || z >= CHUNK_Z_SIZE;
    }

    @Override
    public void setMaterial(int x, int y, int z, MaterialData materialData) throws MaterialNotFoundException {
        checkOutOfBounds(x, y, z);

        char minecraftId = this.gameFactory.getMaterialMap().getMinecraftId(materialData);
        int blockId = minecraftId >> 4;
        int blockData = minecraftId & 0xf;

        int arrayPos = getArrayPos(x, y, z);
        bytes[OFFSET_BLOCK_IDS + arrayPos] = (byte) blockId;
        // The * 2 comes from that the nibble array has two position per byte
        NibbleArray.setInArray(bytes, arrayPos + OFFSET_BLOCK_DATA * 2, (byte) blockData);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + this.chunkX + "," + chunkZ + ")";
    }

}

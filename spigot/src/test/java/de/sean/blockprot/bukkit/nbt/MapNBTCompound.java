package de.sean.blockprot.bukkit.nbt;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MapNBTCompound extends NBTCompound {
    private final String name;
    private final Map<String, Object> data = new LinkedHashMap<>();

    public MapNBTCompound() {
        super(null, (String) null);
        this.name = null;
    }

    public MapNBTCompound(@Nullable String name) {
        super(null, (String) null);
        this.name = name;
    }

    private MapNBTCompound(MapNBTCompound parent, String name) {
        super(parent, name);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<String> getKeys() {
        return data.keySet();
    }

    @Override
    public boolean hasTag(String key) {
        return data.containsKey(key);
    }

    @Override
    public void removeKey(String key) {
        data.remove(key);
    }

    @Override
    public void clearNBT() {
        data.clear();
    }

    @Override
    public NBTCompound addCompound(String key) {
        MapNBTCompound child = new MapNBTCompound(this, key);
        data.put(key, child);
        return child;
    }

    @Override
    public NBTCompound getCompound(String key) {
        Object val = data.get(key);
        if (val instanceof MapNBTCompound) {
            return (MapNBTCompound) val;
        }
        return null;
    }

    @Override
    public NBTCompound getOrCreateCompound(String key) {
        Object val = data.get(key);
        if (val instanceof MapNBTCompound) {
            return (MapNBTCompound) val;
        }
        MapNBTCompound child = new MapNBTCompound(this, key);
        data.put(key, child);
        return child;
    }

    @Override
    public void setString(String key, String value) {
        data.put(key, value);
    }

    @Override
    public String getString(String key) {
        Object val = data.get(key);
        return val instanceof String ? (String) val : "";
    }

    @Override
    public void setInteger(String key, Integer value) {
        data.put(key, value);
    }

    @Override
    public Integer getInteger(String key) {
        Object val = data.get(key);
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }

    @Override
    public void setDouble(String key, Double value) {
        data.put(key, value);
    }

    @Override
    public Double getDouble(String key) {
        Object val = data.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
    }

    @Override
    public void setByte(String key, Byte value) {
        data.put(key, value);
    }

    @Override
    public Byte getByte(String key) {
        Object val = data.get(key);
        return val instanceof Number ? ((Number) val).byteValue() : 0;
    }

    @Override
    public void setShort(String key, Short value) {
        data.put(key, value);
    }

    @Override
    public Short getShort(String key) {
        Object val = data.get(key);
        return val instanceof Number ? ((Number) val).shortValue() : 0;
    }

    @Override
    public void setLong(String key, Long value) {
        data.put(key, value);
    }

    @Override
    public Long getLong(String key) {
        Object val = data.get(key);
        return val instanceof Number ? ((Number) val).longValue() : 0L;
    }

    @Override
    public void setFloat(String key, Float value) {
        data.put(key, value);
    }

    @Override
    public Float getFloat(String key) {
        Object val = data.get(key);
        return val instanceof Number ? ((Number) val).floatValue() : 0.0f;
    }

    @Override
    public void setBoolean(String key, Boolean value) {
        data.put(key, value);
    }

    @Override
    public Boolean getBoolean(String key) {
        Object val = data.get(key);
        return val instanceof Boolean ? (Boolean) val : false;
    }

    @Override
    public NBTType getType(String key) {
        Object val = data.get(key);
        if (val == null) return NBTType.NBTTagEnd;
        if (val instanceof MapNBTCompound) return NBTType.NBTTagCompound;
        if (val instanceof String) return NBTType.NBTTagString;
        if (val instanceof Integer || val instanceof Byte || val instanceof Short) return NBTType.NBTTagInt;
        if (val instanceof Long) return NBTType.NBTTagLong;
        if (val instanceof Float) return NBTType.NBTTagFloat;
        if (val instanceof Double) return NBTType.NBTTagDouble;
        if (val instanceof byte[]) return NBTType.NBTTagByteArray;
        if (val instanceof int[]) return NBTType.NBTTagIntArray;
        return NBTType.NBTTagEnd;
    }

    @Override
    public void mergeCompound(ReadableNBT comp) {
        if (comp instanceof MapNBTCompound other) {
            data.putAll(other.data);
        } else if (comp instanceof NBTCompound other) {
            for (String key : other.getKeys()) {
                NBTCompound sub = other.getCompound(key);
                if (sub != null) {
                    MapNBTCompound child = new MapNBTCompound(this, key);
                    child.mergeCompound(sub);
                    data.put(key, child);
                } else {
                    data.put(key, other.getString(key));
                }
            }
        }
    }

    @Override
    public String toString() {
        return "MapNBTCompound{" + name + ": " + data + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapNBTCompound that)) return false;
        return Objects.equals(name, that.name) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, data);
    }
}

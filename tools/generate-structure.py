#!/usr/bin/env python3
"""Generate the original Probability Pattern 1.12 demonstration structure."""
import gzip, io, struct
from pathlib import Path
class Byte(int):
    pass

class Short(int):
    pass

def kind(value):
    if isinstance(value, Byte): return 1
    if isinstance(value, Short): return 2
    if isinstance(value, int): return 3
    if isinstance(value, str): return 8
    if isinstance(value, list): return 9
    if isinstance(value, dict): return 10
    raise TypeError(type(value))

def utf8(value):
    raw = value.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw

def payload(value):
    tag = kind(value)
    if tag in (1, 2, 3): return struct.pack({1: ">b", 2: ">h", 3: ">i"}[tag], value)
    if tag == 8: return utf8(value)
    if tag == 9:
        element = kind(value[0]) if value else 10
        assert all(kind(v) == element for v in value), "heterogeneous NBT list"
        return bytes([element]) + struct.pack(">i", len(value)) + b"".join(payload(v) for v in value)
    if tag == 10:
        return b"".join(bytes([kind(v)]) + utf8(k) + payload(v) for k, v in value.items()) + b"\0"
    raise AssertionError(tag)

def item(name, count=1, slot=0):
    return {"Slot": Byte(slot), "id": name, "Count": Byte(count), "Damage": Short(0)}

class Structure:
    def __init__(self, size=7):
        self.size = size
        self.palette = []
        self.blocks = {}
        for x in range(size):
            for z in range(size):
                self.block(x, 0, z, "minecraft:concrete",
                           {"color": "gray" if x in (0, size - 1) or z in (0, size - 1) else "silver"})

    def block(self, x, y, z, name, properties=None, nbt=None):
        state = {"Name": name}
        if properties: state["Properties"] = properties
        if state not in self.palette: self.palette.append(state)
        entry = {"pos": [x, y, z], "state": self.palette.index(state)}
        if nbt: entry["nbt"] = nbt
        self.blocks[x, y, z] = entry
        return self

    def machine(self, x, y, z, name, **nbt):
        tile = name if name != "dense_energy_cell" else "energy_cell"
        if name.endswith("_crafting_storage") or name == "crafting_accelerator": tile = "crafting_unit"
        return self.block(x, y, z, "ae2:" + name, nbt={
            "id": "ae2:" + tile, "x": x, "y": y, "z": z, "forward": "SOUTH", "up": "UP", **nbt})

    def chest(self, x, y, z, contents=None):
        return self.block(x, y, z, "minecraft:chest", {"facing": "north"},
                          {"id": "minecraft:chest", "x": x, "y": y, "z": z, "Items": contents or []})

    def cable(self, x, y, z, cable="fluix_glass_cable", connections=(), channels=0, **parts):
        nbt = {"id": "ae2:cable_bus", "x": x, "y": y, "z": z}
        if cable:
            nbt["cable"] = {"id": "ae2:" + cable, "visual": {
                "powered": Byte(1), "missingChannel": Byte(0), "channels": channels,
                "connections": list(connections)}}
        for side, part in parts.items():
            nbt[side] = {"id": "ae2:" + part, "visual": {"powered": Byte(1), "missingChannel": Byte(0)}}
        return self.block(x, y, z, "ae2:cable_bus", nbt=nbt)

    def data(self):
        height = max(p[1] for p in self.blocks) + 1
        root = {"DataVersion": 1343, "author": "yudream / Probability Pattern Legacy", "size": [self.size, height, self.size],
                "palette": self.palette, "blocks": list(self.blocks.values()), "entities": []}
        output = io.BytesIO()
        with gzip.GzipFile(fileobj=output, mode="wb", filename="", mtime=0) as compressed:
            compressed.write(b"\x0a\0\0" + payload(root))
        return output.getvalue()

s = Structure()
s.machine(1,1,2,'vibration_chamber').machine(1,1,3,'energy_cell').machine(2,1,3,'drive', forward='NORTH')
s.cable(3,1,3,connections=('west','east','south','north'),south='pattern_encoding_terminal')
s.blocks[(3,1,3)]['nbt']['south']['id']='statpatterns:probability_pattern_terminal'
s.machine(4,1,3,'pattern_provider').chest(5,1,3)
s.machine(3,1,2,'64k_crafting_storage')
p=Path(__file__).resolve().parents[1]/'src/main/resources/assets/statpatterns/ponder/probability_processing.nbt'
p.parent.mkdir(parents=True,exist_ok=True)
p.write_bytes(s.data())

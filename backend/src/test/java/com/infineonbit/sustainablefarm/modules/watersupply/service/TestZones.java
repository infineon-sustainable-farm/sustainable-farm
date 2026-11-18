package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;

/** Builder of {@code farm.api} zone views for the Water Supply tests. */
public final class TestZones {
    private Long id;
    private Long blockId;
    private Long farmId;
    private String blockCode;
    private String name;
    private Double areaHectares;
    private String irrigationMethod;
    private Double cropCoefficient;
    private Integer emitterCount;
    private Double emitterNominalFlowLh;

    private TestZones() {
    }

    public static TestZones zone() {
        return new TestZones();
    }

    public TestZones id(Long value) { this.id = value; return this; }
    public TestZones blockId(Long value) { this.blockId = value; return this; }
    public TestZones farmId(Long value) { this.farmId = value; return this; }
    public TestZones blockCode(String value) { this.blockCode = value; return this; }
    public TestZones name(String value) { this.name = value; return this; }
    public TestZones areaHectares(Double value) { this.areaHectares = value; return this; }
    public TestZones irrigationMethod(String value) { this.irrigationMethod = value; return this; }
    public TestZones cropCoefficient(Double value) { this.cropCoefficient = value; return this; }
    public TestZones emitterCount(Integer value) { this.emitterCount = value; return this; }
    public TestZones emitterNominalFlowLh(Double value) { this.emitterNominalFlowLh = value; return this; }

    public ZoneDto build() {
        return new ZoneDto(id, blockId, farmId, blockCode, name, areaHectares, irrigationMethod, cropCoefficient,
                emitterCount, emitterNominalFlowLh, null);
    }
}

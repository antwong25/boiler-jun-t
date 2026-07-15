package org.example.boilerpojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BoilerDetailVO {
    private String boilerId;
    private String model;
    private String brand;
    private String boilerType;
    private BigDecimal tonnage;
    private String fuelType;
    private BigDecimal workingPressure;
    private BigDecimal noxEmissions;
    private BigDecimal footprintArea;
    private LocalDate manufactureDate;
    private BigDecimal evaporationCapacity;
    private BigDecimal ratedThermalPower;
    private BigDecimal thermalEfficiency;
    private String equipmentCondition;
    private BigDecimal usageHours;
    private String testReport;
    private BigDecimal ratedOutletWaterTemperature;
    private String applicationScenario;
}

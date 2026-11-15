package com.infineonbit.sustainablefarm.modules.salesmarketing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Request record fields form the writable-property allowlist for each entity.
public final class SalesMarketingRequests {

    private SalesMarketingRequests() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CampaignRequest(
            @NotBlank(message = "Campaign name is required") String name,
            String status,
            @PositiveOrZero(message = "budgetEur cannot be negative") BigDecimal budgetEur,
            BigDecimal budgetSpentEur,
            Integer estimatedReach,
            String currency,
            LocalDate startDate,
            LocalDate endDate) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CampaignMetricRequest(
            @NotNull(message = "campaignId is required") Integer campaignId,
            LocalDate metricDate,
            @PositiveOrZero Integer clicks,
            @PositiveOrZero Integer signups,
            @PositiveOrZero Integer reach) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CertificationRequest(
            @NotBlank(message = "Certification name is required") String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomerContactRequest(
            Integer customerId,
            @NotBlank(message = "Full name is required") String fullName,
            @Email(message = "Must be a valid email address") String email,
            String phone,
            String role,
            Boolean isPrimary) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DemandForecastRequest(
            Integer productId,
            @NotNull(message = "forecastMonth is required") LocalDate forecastMonth,
            @NotBlank(message = "scenario is required") String scenario,
            @NotNull(message = "forecastedVolumeT is required")
            @PositiveOrZero BigDecimal forecastedVolumeT,
            @PositiveOrZero BigDecimal lowerBoundT,
            @PositiveOrZero BigDecimal upperBoundT,
            String capacityRisk,
            Boolean isActual,
            String modelVersion) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DemandReportRequest(
            String reportType,
            @NotNull(message = "periodStart is required") LocalDate periodStart,
            @NotNull(message = "periodEnd is required") LocalDate periodEnd,
            BigDecimal totalForecastedVolumeT,
            BigDecimal totalActualVolumeT,
            BigDecimal variancePct,
            String status,
            Boolean isScheduled,
            LocalDateTime nextRunAt,
            String scheduleFrequency,
            String summary,
            String generatedBy) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ForecastModelRunRequest(
            String cropVariety,
            String region,
            BigDecimal confidenceIntervalPct,
            Boolean useWeatherData,
            Boolean useCommodityPrices,
            Boolean useGeopoliticalIndex,
            String modelStatus) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderRequest(
            @NotNull(message = "customerId is required") Integer customerId,
            Integer salesChannelId,
            @NotNull(message = "orderDate is required") LocalDate orderDate,
            @PositiveOrZero(message = "totalVolumeKg cannot be negative") BigDecimal totalVolumeKg,
            @PositiveOrZero(message = "totalValueEur cannot be negative") BigDecimal totalValueEur,
            String status) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderItemRequest(
            @NotNull(message = "orderId is required") Integer orderId,
            @NotNull(message = "productId is required") Integer productId,
            @NotNull(message = "quantityKg is required")
            @Positive(message = "quantityKg must be greater than zero") BigDecimal quantityKg,
            @NotNull(message = "unitPriceEur is required")
            @PositiveOrZero BigDecimal unitPriceEur) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductRequest(
            @NotBlank(message = "Product name is required") String name,
            String variety,
            String unit) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductBatchRequest(
            @NotBlank(message = "batchCode is required") String batchCode,
            Integer productId,
            @PositiveOrZero(message = "stockT cannot be negative") BigDecimal stockT,
            String status) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReportFileRequest(
            @NotNull(message = "reportId is required") Integer reportId,
            @NotBlank(message = "format is required") String format,
            String fileUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SalesChannelRequest(
            @NotBlank(message = "Channel name is required") String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SalesChannelTargetRequest(
            @NotNull(message = "channelId is required") Integer channelId,
            @NotNull(message = "periodStart is required") LocalDate periodStart,
            @NotNull(message = "periodEnd is required") LocalDate periodEnd,
            @NotNull(message = "revenueTargetEur is required") BigDecimal revenueTargetEur) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ShipmentRequest(
            String shipmentCode,
            Integer orderId,
            @NotBlank(message = "origin is required") String origin,
            @NotBlank(message = "destination is required") String destination,
            String status,
            LocalDate eta,
            String vesselName,
            String voyageNumber,
            String delayReason,
            BigDecimal currentLat,
            BigDecimal currentLon,
            BigDecimal speedKnots,
            String currentLocationLabel,
            BigDecimal distanceRemainingNm,
            String commodityDescription,
            Integer containerCount,
            String containerType,
            BigDecimal cargoWeightKg,
            BigDecimal currentTempC,
            String tempStatus,
            String manifestUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ShipmentEventRequest(
            @NotNull(message = "shipmentId is required") Integer shipmentId,
            @NotBlank(message = "step is required") String step,
            LocalDateTime eventTime,
            String note) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompetitorPriceRequest(
            Integer productId,
            @NotBlank(message = "competitorName is required") String competitorName,
            @NotNull(message = "priceEur is required") @Positive BigDecimal priceEur,
            @NotNull(message = "priceDate is required") LocalDate priceDate,
            Boolean isTarget) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricingHistoryRequest(
            @NotNull(message = "productId is required") Integer productId,
            @NotNull(message = "priceDate is required") LocalDate priceDate,
            @NotNull(message = "priceEurPerKg is required")
            @Positive(message = "priceEurPerKg must be greater than zero") BigDecimal priceEurPerKg,
            BigDecimal harvestSeasonFactor,
            String seasonLabel,
            BigDecimal stockLevelTons,
            BigDecimal stockTargetTons,
            BigDecimal demandFactor,
            String demandIndexLabel,
            Boolean isApplied) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricingRecommendationRequest(
            Integer productId,
            @NotBlank(message = "message is required") String message,
            BigDecimal suggestedPriceEur) {}
}

package com.infineonbit.sustainablefarm.modules.salesmarketing.dto;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.core.money.Money;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Campaign;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CampaignMetric;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CampaignStatus;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CapacityRisk;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Certification;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CompetitorPrice;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Customer;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerCertification;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerContact;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerStatus;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerType;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.DemandForecast;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.DemandReport;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.DemandReportStatus;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ForecastModelRun;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ForecastScenario;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Order;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.OrderItem;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.PricingHistory;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.PricingRecommendation;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Product;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ProductBatch;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ReportFile;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.SalesChannel;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.SalesChannelTarget;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Shipment;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ShipmentEvent;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ShipmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * The response bodies of Sales and Marketing, with the JSON the entities used to give: the same
 * field names ({@code customerId}, {@code budgetEur}), the euro amounts as plain numbers and the
 * creation times as local date-times.
 */
public final class SalesMarketingResponses {

    private SalesMarketingResponses() {}

    public record CampaignResponse(
            Long campaignId, String name, CampaignStatus status, BigDecimal budgetEur, BigDecimal budgetSpentEur,
            Integer estimatedReach, String currency, LocalDate startDate, LocalDate endDate, LocalDateTime createdAt) {

        public static CampaignResponse of(Campaign c) {
            return new CampaignResponse(c.getId(), c.getName(), c.getStatus(), amount(c.getBudget()),
                    amount(c.getBudgetSpent()), c.getEstimatedReach(), c.getCurrency(), c.getStartDate(),
                    c.getEndDate(), local(c.getCreatedAt()));
        }
    }

    public record CampaignMetricResponse(
            Long metricId, Long campaignId, LocalDate metricDate, Integer clicks, Integer signups, Integer reach) {

        public static CampaignMetricResponse of(CampaignMetric m) {
            return new CampaignMetricResponse(m.getId(), id(m.getCampaign()), m.getMetricDate(), m.getClicks(),
                    m.getSignups(), m.getReach());
        }
    }

    public record CertificationResponse(Long certificationId, String name) {

        public static CertificationResponse of(Certification c) {
            return new CertificationResponse(c.getId(), c.getName());
        }
    }

    public record CustomerCertificationResponse(Long customerId, Long certificationId) {

        public static CustomerCertificationResponse of(CustomerCertification link) {
            return new CustomerCertificationResponse(id(link.getCustomer()), id(link.getCertification()));
        }
    }

    public record CustomerContactResponse(
            Long contactId, Long customerId, String fullName, String email, String phone, String role,
            Boolean isPrimary, LocalDateTime createdAt) {

        public static CustomerContactResponse of(CustomerContact c) {
            return new CustomerContactResponse(c.getId(), id(c.getCustomer()), c.getFullName(), c.getEmail(),
                    c.getPhone(), c.getRole(), c.getIsPrimary(), local(c.getCreatedAt()));
        }
    }

    public record CustomerResponse(
            Long customerId, String customerCode, String companyName, CustomerType customerType, String country,
            String region, String city, CustomerStatus status, String paymentTerms, LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static CustomerResponse of(Customer c) {
            return new CustomerResponse(c.getId(), c.getCustomerCode(), c.getCompanyName(), c.getCustomerType(),
                    c.getCountry(), c.getRegion(), c.getCity(), c.getStatus(), c.getPaymentTerms(),
                    local(c.getCreatedAt()), local(c.getUpdatedAt()));
        }
    }

    public record DemandForecastResponse(
            Long forecastId, Long productId, LocalDate forecastMonth, ForecastScenario scenario,
            BigDecimal forecastedVolumeT, BigDecimal lowerBoundT, BigDecimal upperBoundT, CapacityRisk capacityRisk,
            Boolean isActual, String modelVersion, LocalDateTime createdAt) {

        public static DemandForecastResponse of(DemandForecast f) {
            return new DemandForecastResponse(f.getId(), id(f.getProduct()), f.getForecastMonth(), f.getScenario(),
                    f.getForecastedVolumeT(), f.getLowerBoundT(), f.getUpperBoundT(), f.getCapacityRisk(),
                    f.getIsActual(), f.getModelVersion(), local(f.getCreatedAt()));
        }
    }

    public record DemandReportResponse(
            Long reportId, String reportType, LocalDate periodStart, LocalDate periodEnd,
            BigDecimal totalForecastedVolumeT, BigDecimal totalActualVolumeT, BigDecimal variancePct,
            DemandReportStatus status, Boolean isScheduled, LocalDateTime nextRunAt, String scheduleFrequency,
            String summary, String generatedBy, Long generatedByPersonId, LocalDateTime generatedAt) {

        /** @param generatedBy full name of the person referenced by {@code generatedByPersonId}, or null */
        public static DemandReportResponse of(DemandReport r, String generatedBy) {
            return new DemandReportResponse(r.getId(), r.getReportType(), r.getPeriodStart(), r.getPeriodEnd(),
                    r.getTotalForecastedVolumeT(), r.getTotalActualVolumeT(), r.getVariancePct(), r.getStatus(),
                    r.getIsScheduled(), r.getNextRunAt(), r.getScheduleFrequency(), r.getSummary(),
                    generatedBy, r.getGeneratedByPersonId(), r.getGeneratedAt());
        }
    }

    public record ForecastModelRunResponse(
            Long runId, String cropVariety, String region, BigDecimal confidenceIntervalPct, Boolean useWeatherData,
            Boolean useCommodityPrices, Boolean useGeopoliticalIndex, String modelStatus, LocalDateTime createdAt) {

        public static ForecastModelRunResponse of(ForecastModelRun r) {
            return new ForecastModelRunResponse(r.getId(), r.getCropVariety(), r.getRegion(),
                    r.getConfidenceIntervalPct(), r.getUseWeatherData(), r.getUseCommodityPrices(),
                    r.getUseGeopoliticalIndex(), r.getModelStatus(), local(r.getCreatedAt()));
        }
    }

    public record OrderResponse(
            Long orderId, Long customerId, Long salesChannelId, LocalDate orderDate, BigDecimal totalVolumeKg,
            BigDecimal totalValueEur, String status) {

        public static OrderResponse of(Order o) {
            return new OrderResponse(o.getId(), id(o.getCustomer()), id(o.getSalesChannel()), o.getOrderDate(),
                    o.getTotalVolumeKg(), amount(o.getTotalValue()), o.getStatus());
        }
    }

    public record OrderItemResponse(
            Long orderItemId, Long orderId, Long productId, BigDecimal quantityKg, BigDecimal unitPriceEur) {

        public static OrderItemResponse of(OrderItem i) {
            return new OrderItemResponse(i.getId(), id(i.getOrder()), id(i.getProduct()), i.getQuantityKg(),
                    amount(i.getUnitPrice()));
        }
    }

    public record ProductResponse(Long productId, String name, String variety, String unit, LocalDateTime createdAt) {

        /** A product with the catalogue name of its variety, {@code null} for none. */
        public static ProductResponse of(Product p, String varietyName) {
            return new ProductResponse(p.getId(), p.getName(), varietyName, p.getUnit(), local(p.getCreatedAt()));
        }
    }

    public record ProductBatchResponse(
            Long batchId, String batchCode, Long productId, BigDecimal stockT, String status, LocalDateTime syncedAt) {

        public static ProductBatchResponse of(ProductBatch b) {
            return new ProductBatchResponse(b.getId(), b.getBatchCode(), id(b.getProduct()), b.getStockT(),
                    b.getStatus(), b.getSyncedAt());
        }
    }

    public record ReportFileResponse(Long fileId, Long reportId, String format, String fileUrl) {

        public static ReportFileResponse of(ReportFile f) {
            return new ReportFileResponse(f.getId(), id(f.getReport()), f.getFormat(), f.getFileUrl());
        }
    }

    public record SalesChannelResponse(Long channelId, String name) {

        public static SalesChannelResponse of(SalesChannel c) {
            return new SalesChannelResponse(c.getId(), c.getName());
        }
    }

    public record SalesChannelTargetResponse(
            Long targetId, Long channelId, LocalDate periodStart, LocalDate periodEnd, BigDecimal revenueTargetEur) {

        public static SalesChannelTargetResponse of(SalesChannelTarget t) {
            return new SalesChannelTargetResponse(t.getId(), id(t.getChannel()), t.getPeriodStart(),
                    t.getPeriodEnd(), amount(t.getRevenueTarget()));
        }
    }

    public record ShipmentResponse(
            Long shipmentId, String shipmentCode, Long orderId, String origin, String destination,
            ShipmentStatus status, LocalDate eta, String vesselName, String voyageNumber, String delayReason,
            BigDecimal currentLat, BigDecimal currentLon, BigDecimal speedKnots, String currentLocationLabel,
            BigDecimal distanceRemainingNm, String commodityDescription, Integer containerCount, String containerType,
            BigDecimal cargoWeightKg, BigDecimal currentTempC, String tempStatus, String manifestUrl,
            LocalDateTime createdAt) {

        public static ShipmentResponse of(Shipment s) {
            return new ShipmentResponse(s.getId(), s.getShipmentCode(), id(s.getOrder()), s.getOrigin(),
                    s.getDestination(), s.getStatus(), s.getEta(), s.getVesselName(), s.getVoyageNumber(),
                    s.getDelayReason(), s.getCurrentLat(), s.getCurrentLon(), s.getSpeedKnots(),
                    s.getCurrentLocationLabel(), s.getDistanceRemainingNm(), s.getCommodityDescription(),
                    s.getContainerCount(), s.getContainerType(), s.getCargoWeightKg(), s.getCurrentTempC(),
                    s.getTempStatus(), s.getManifestUrl(), local(s.getCreatedAt()));
        }
    }

    public record ShipmentEventResponse(
            Long eventId, Long shipmentId, String step, LocalDateTime eventTime, String note) {

        public static ShipmentEventResponse of(ShipmentEvent e) {
            return new ShipmentEventResponse(e.getId(), id(e.getShipment()), e.getStep(), e.getEventTime(),
                    e.getNote());
        }
    }

    public record CompetitorPriceResponse(
            Long competitorPriceId, Long productId, String competitorName, BigDecimal priceEur, LocalDate priceDate,
            Boolean isTarget, LocalDateTime createdAt) {

        public static CompetitorPriceResponse of(CompetitorPrice p) {
            return new CompetitorPriceResponse(p.getId(), id(p.getProduct()), p.getCompetitorName(),
                    amount(p.getPrice()), p.getPriceDate(), p.getIsTarget(), local(p.getCreatedAt()));
        }
    }

    public record PricingHistoryResponse(
            Long pricingId, Long productId, LocalDate priceDate, BigDecimal priceEurPerKg,
            BigDecimal harvestSeasonFactor, String seasonLabel, BigDecimal stockLevelTons, BigDecimal stockTargetTons,
            BigDecimal demandFactor, String demandIndexLabel, Boolean isApplied, LocalDateTime createdAt) {

        public static PricingHistoryResponse of(PricingHistory h) {
            return new PricingHistoryResponse(h.getId(), id(h.getProduct()), h.getPriceDate(),
                    amount(h.getPricePerKg()), h.getHarvestSeasonFactor(), h.getSeasonLabel(), h.getStockLevelTons(),
                    h.getStockTargetTons(), h.getDemandFactor(), h.getDemandIndexLabel(), h.getIsApplied(),
                    local(h.getCreatedAt()));
        }
    }

    public record PricingRecommendationResponse(
            Long recommendationId, Long productId, String message, BigDecimal suggestedPriceEur,
            LocalDateTime createdAt) {

        public static PricingRecommendationResponse of(PricingRecommendation r) {
            return new PricingRecommendationResponse(r.getId(), id(r.getProduct()), r.getMessage(),
                    amount(r.getSuggestedPrice()), local(r.getCreatedAt()));
        }
    }

    /** The id of a referenced row, read from a lazy reference without loading it. */
    private static Long id(BaseEntity entity) {
        return entity == null ? null : entity.getId();
    }

    private static BigDecimal amount(Money money) {
        return money == null ? null : money.amount();
    }

    /** An audit time as the local date-time the API has always sent. */
    private static LocalDateTime local(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}

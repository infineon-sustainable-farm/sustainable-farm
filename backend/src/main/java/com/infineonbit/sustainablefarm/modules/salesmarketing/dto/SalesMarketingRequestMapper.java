package com.infineonbit.sustainablefarm.modules.salesmarketing.dto;

import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.core.money.Money;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CampaignMetricRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CampaignRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CertificationRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CompetitorPriceRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CustomerContactRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.DemandForecastRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.DemandReportRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ForecastModelRunRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.OrderItemRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.OrderRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.PricingHistoryRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.PricingRecommendationRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ProductRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ProductBatchRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ReportFileRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.SalesChannelRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.SalesChannelTargetRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ShipmentEventRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ShipmentRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Campaign;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CampaignMetric;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CampaignStatus;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CapacityRisk;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Certification;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CompetitorPrice;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Customer;
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
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.StoredValue;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CampaignRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CustomerRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.DemandReportRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.OrderRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ProductRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.SalesChannelRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ShipmentRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Writes the fields of a request onto an entity, a new one or the one being updated.
 *
 * <p>Only the fields of the request are written: the id and the audit times stay those of the
 * entity. Every field is replaced, a missing one by {@code null}, except where the column has
 * a default: a missing status, flag or unit takes the default of its column. A status outside
 * its check constraint is refused with 409, as the database refused it. A referenced row
 * ({@code customerId}, {@code productId}...) is set by reference; an unknown one is refused by
 * its foreign key, with 409.
 */
@Component
public class SalesMarketingRequestMapper {

    private final CampaignRepository campaigns;
    private final CustomerRepository customers;
    private final DemandReportRepository reports;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final SalesChannelRepository channels;
    private final ShipmentRepository shipments;

    public SalesMarketingRequestMapper(CampaignRepository campaigns, CustomerRepository customers,
                                       DemandReportRepository reports, OrderRepository orders,
                                       ProductRepository products, SalesChannelRepository channels,
                                       ShipmentRepository shipments) {
        this.campaigns = campaigns;
        this.customers = customers;
        this.reports = reports;
        this.orders = orders;
        this.products = products;
        this.channels = channels;
        this.shipments = shipments;
    }

    public Campaign apply(CampaignRequest r, Campaign c) {
        c.setName(r.name());
        c.setStatus(orDefault(StoredValue.parse(CampaignStatus.class, r.status()), CampaignStatus.SCHEDULED));
        c.setBudget(euros(r.budgetEur()));
        c.setBudgetSpent(euros(r.budgetSpentEur()));
        c.setEstimatedReach(r.estimatedReach());
        c.setCurrency(r.currency());
        c.setStartDate(r.startDate());
        c.setEndDate(r.endDate());
        return c;
    }

    public CampaignMetric apply(CampaignMetricRequest r, CampaignMetric m) {
        m.setCampaign(reference(campaigns, r.campaignId()));
        m.setMetricDate(r.metricDate());
        m.setClicks(r.clicks());
        m.setSignups(r.signups());
        m.setReach(r.reach());
        return m;
    }

    public Certification apply(CertificationRequest r, Certification c) {
        c.setName(r.name());
        return c;
    }

    public Customer apply(CustomerRequest r, Customer c) {
        c.setCompanyName(r.companyName());
        c.setCustomerCode(r.customerCode());
        c.setCustomerType(StoredValue.parse(CustomerType.class, r.customerType()));
        c.setCountry(r.country());
        c.setRegion(r.region());
        c.setCity(r.city());
        c.setStatus(StoredValue.parse(CustomerStatus.class, r.status()));
        c.setPaymentTerms(r.paymentTerms());
        return c;
    }

    public CustomerContact apply(CustomerContactRequest r, CustomerContact c) {
        c.setCustomer(reference(customers, r.customerId()));
        c.setFullName(r.fullName());
        c.setEmail(r.email());
        c.setPhone(r.phone());
        c.setRole(r.role());
        c.setIsPrimary(orDefault(r.isPrimary(), Boolean.FALSE));
        return c;
    }

    public DemandForecast apply(DemandForecastRequest r, DemandForecast f) {
        f.setProduct(reference(products, r.productId()));
        f.setForecastMonth(r.forecastMonth());
        f.setScenario(StoredValue.parse(ForecastScenario.class, r.scenario()));
        f.setForecastedVolumeT(r.forecastedVolumeT());
        f.setLowerBoundT(r.lowerBoundT());
        f.setUpperBoundT(r.upperBoundT());
        f.setCapacityRisk(StoredValue.parse(CapacityRisk.class, r.capacityRisk()));
        f.setIsActual(orDefault(r.isActual(), Boolean.FALSE));
        f.setModelVersion(r.modelVersion());
        return f;
    }

    public DemandReport apply(DemandReportRequest r, DemandReport d) {
        d.setReportType(orDefault(r.reportType(), "weekly"));
        d.setPeriodStart(r.periodStart());
        d.setPeriodEnd(r.periodEnd());
        d.setTotalForecastedVolumeT(r.totalForecastedVolumeT());
        d.setTotalActualVolumeT(r.totalActualVolumeT());
        d.setVariancePct(r.variancePct());
        d.setStatus(StoredValue.parse(DemandReportStatus.class, r.status()));
        d.setIsScheduled(r.isScheduled());
        d.setNextRunAt(r.nextRunAt());
        d.setScheduleFrequency(r.scheduleFrequency());
        d.setSummary(r.summary());
        d.setGeneratedBy(r.generatedBy());
        return d;
    }

    public ForecastModelRun apply(ForecastModelRunRequest r, ForecastModelRun m) {
        m.setCropVariety(r.cropVariety());
        m.setRegion(r.region());
        m.setConfidenceIntervalPct(r.confidenceIntervalPct());
        m.setUseWeatherData(r.useWeatherData());
        m.setUseCommodityPrices(r.useCommodityPrices());
        m.setUseGeopoliticalIndex(r.useGeopoliticalIndex());
        m.setModelStatus(r.modelStatus());
        return m;
    }

    public Order apply(OrderRequest r, Order o) {
        o.setCustomer(reference(customers, r.customerId()));
        o.setSalesChannel(reference(channels, r.salesChannelId()));
        o.setOrderDate(r.orderDate());
        o.setTotalVolumeKg(r.totalVolumeKg());
        o.setTotalValue(euros(r.totalValueEur()));
        o.setStatus(r.status());
        return o;
    }

    public OrderItem apply(OrderItemRequest r, OrderItem i) {
        i.setOrder(reference(orders, r.orderId()));
        i.setProduct(reference(products, r.productId()));
        i.setQuantityKg(r.quantityKg());
        i.setUnitPrice(euros(r.unitPriceEur()));
        return i;
    }

    /** The name and the unit; the variety, a catalogue name, is resolved by the product service. */
    public Product apply(ProductRequest r, Product p) {
        p.setName(r.name());
        p.setUnit(orDefault(r.unit(), "kg"));
        return p;
    }

    /** The batch, written now: its sync time is the time of the write. */
    public ProductBatch apply(ProductBatchRequest r, ProductBatch b) {
        b.setBatchCode(r.batchCode());
        b.setProduct(reference(products, r.productId()));
        b.setStockT(r.stockT());
        b.setStatus(r.status());
        b.setSyncedAt(LocalDateTime.now());
        return b;
    }

    public ReportFile apply(ReportFileRequest r, ReportFile f) {
        f.setReport(reference(reports, r.reportId()));
        f.setFormat(r.format());
        f.setFileUrl(r.fileUrl());
        return f;
    }

    public SalesChannel apply(SalesChannelRequest r, SalesChannel c) {
        c.setName(r.name());
        return c;
    }

    public SalesChannelTarget apply(SalesChannelTargetRequest r, SalesChannelTarget t) {
        t.setChannel(reference(channels, r.channelId()));
        t.setPeriodStart(r.periodStart());
        t.setPeriodEnd(r.periodEnd());
        t.setRevenueTarget(euros(r.revenueTargetEur()));
        return t;
    }

    public Shipment apply(ShipmentRequest r, Shipment s) {
        s.setShipmentCode(r.shipmentCode());
        s.setOrder(reference(orders, r.orderId()));
        s.setOrigin(r.origin());
        s.setDestination(r.destination());
        s.setStatus(orDefault(StoredValue.parse(ShipmentStatus.class, r.status()), ShipmentStatus.PACKED));
        s.setEta(r.eta());
        s.setVesselName(r.vesselName());
        s.setVoyageNumber(r.voyageNumber());
        s.setDelayReason(r.delayReason());
        s.setCurrentLat(r.currentLat());
        s.setCurrentLon(r.currentLon());
        s.setSpeedKnots(r.speedKnots());
        s.setCurrentLocationLabel(r.currentLocationLabel());
        s.setDistanceRemainingNm(r.distanceRemainingNm());
        s.setCommodityDescription(r.commodityDescription());
        s.setContainerCount(r.containerCount());
        s.setContainerType(r.containerType());
        s.setCargoWeightKg(r.cargoWeightKg());
        s.setCurrentTempC(r.currentTempC());
        s.setTempStatus(r.tempStatus());
        s.setManifestUrl(r.manifestUrl());
        return s;
    }

    /** The event; at the time of the write when no time is sent. */
    public ShipmentEvent apply(ShipmentEventRequest r, ShipmentEvent e) {
        e.setShipment(reference(shipments, r.shipmentId()));
        e.setStep(r.step());
        e.setEventTime(orDefault(r.eventTime(), LocalDateTime.now()));
        e.setNote(r.note());
        return e;
    }

    public CompetitorPrice apply(CompetitorPriceRequest r, CompetitorPrice p) {
        p.setProduct(reference(products, r.productId()));
        p.setCompetitorName(r.competitorName());
        p.setPrice(euros(r.priceEur()));
        p.setPriceDate(r.priceDate());
        p.setIsTarget(orDefault(r.isTarget(), Boolean.FALSE));
        return p;
    }

    public PricingHistory apply(PricingHistoryRequest r, PricingHistory h) {
        h.setProduct(reference(products, r.productId()));
        h.setPriceDate(r.priceDate());
        h.setPricePerKg(euros(r.priceEurPerKg()));
        h.setHarvestSeasonFactor(r.harvestSeasonFactor());
        h.setSeasonLabel(r.seasonLabel());
        h.setStockLevelTons(r.stockLevelTons());
        h.setStockTargetTons(r.stockTargetTons());
        h.setDemandFactor(r.demandFactor());
        h.setDemandIndexLabel(r.demandIndexLabel());
        h.setIsApplied(r.isApplied());
        return h;
    }

    public PricingRecommendation apply(PricingRecommendationRequest r, PricingRecommendation p) {
        p.setProduct(reference(products, r.productId()));
        p.setMessage(r.message());
        p.setSuggestedPrice(euros(r.suggestedPriceEur()));
        return p;
    }

    /** A reference to a row by its id, {@code null} for none; an unknown id fails on its foreign key. */
    private static <T> T reference(JpaRepository<T, Long> repository, Long id) {
        return id == null ? null : repository.getReferenceById(id);
    }

    private static Money euros(BigDecimal amount) {
        return amount == null ? null : Money.of(amount, CurrencyCode.EUR);
    }

    private static <T> T orDefault(T value, T fallback) {
        return value == null ? fallback : value;
    }
}

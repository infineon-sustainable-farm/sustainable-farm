--
-- PostgreSQL database dump
--


-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6




--
-- Name: alerts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.alerts (
    alert_id integer NOT NULL,
    alert_type character varying(50) NOT NULL,
    severity character varying(20) DEFAULT 'info'::character varying NOT NULL,
    message character varying(255) NOT NULL,
    related_table character varying(50),
    related_id integer,
    status character varying(20) DEFAULT 'open'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    resolved_at timestamp without time zone,
    alert_code character varying(20),
    region character varying(80),
    origin character varying(80),
    CONSTRAINT alerts_severity_check CHECK (((severity)::text = ANY ((ARRAY['critical'::character varying, 'warning'::character varying, 'info'::character varying])::text[]))),
    CONSTRAINT alerts_status_check CHECK (((status)::text = ANY ((ARRAY['open'::character varying, 'acknowledged'::character varying, 'resolved'::character varying, 'dismissed'::character varying])::text[])))
);


--
-- Name: alerts_alert_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.alerts_alert_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: alerts_alert_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.alerts_alert_id_seq OWNED BY public.alerts.alert_id;


--
-- Name: campaign_metrics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.campaign_metrics (
    metric_id integer NOT NULL,
    campaign_id integer NOT NULL,
    metric_date date NOT NULL,
    clicks integer DEFAULT 0,
    signups integer DEFAULT 0,
    reach integer DEFAULT 0
);


--
-- Name: campaign_metrics_metric_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.campaign_metrics_metric_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: campaign_metrics_metric_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.campaign_metrics_metric_id_seq OWNED BY public.campaign_metrics.metric_id;


--
-- Name: campaigns; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.campaigns (
    campaign_id integer NOT NULL,
    name character varying(120) NOT NULL,
    status character varying(20) DEFAULT 'scheduled'::character varying NOT NULL,
    budget_eur numeric(10,2),
    start_date date,
    end_date date,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    budget_spent_eur numeric(10,2),
    estimated_reach integer,
    currency character varying(3) DEFAULT 'EUR'::character varying,
    CONSTRAINT campaigns_status_check CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'scheduled'::character varying, 'ended'::character varying, 'draft'::character varying])::text[])))
);


--
-- Name: campaigns_campaign_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.campaigns_campaign_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: campaigns_campaign_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.campaigns_campaign_id_seq OWNED BY public.campaigns.campaign_id;


--
-- Name: certifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.certifications (
    certification_id integer NOT NULL,
    name character varying(100) NOT NULL
);


--
-- Name: certifications_certification_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.certifications_certification_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: certifications_certification_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.certifications_certification_id_seq OWNED BY public.certifications.certification_id;


--
-- Name: competitor_prices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.competitor_prices (
    competitor_price_id integer NOT NULL,
    product_id integer,
    competitor_name character varying(50) NOT NULL,
    price_eur numeric(8,2) NOT NULL,
    price_date date NOT NULL,
    is_target boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: competitor_prices_competitor_price_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.competitor_prices_competitor_price_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: competitor_prices_competitor_price_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.competitor_prices_competitor_price_id_seq OWNED BY public.competitor_prices.competitor_price_id;


--
-- Name: customer_certifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.customer_certifications (
    customer_id integer NOT NULL,
    certification_id integer NOT NULL
);


--
-- Name: customer_contacts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.customer_contacts (
    contact_id integer NOT NULL,
    customer_id integer NOT NULL,
    full_name character varying(120) NOT NULL,
    email character varying(150),
    phone character varying(40),
    role character varying(80),
    is_primary boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: customer_contacts_contact_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.customer_contacts_contact_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customer_contacts_contact_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customer_contacts_contact_id_seq OWNED BY public.customer_contacts.contact_id;


--
-- Name: customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.customers (
    customer_id integer NOT NULL,
    company_name character varying(150) NOT NULL,
    customer_type character varying(30) NOT NULL,
    country character varying(80),
    city character varying(80),
    status character varying(20) DEFAULT 'prospect'::character varying NOT NULL,
    payment_terms character varying(50),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    customer_code character varying(20),
    region character varying(80),
    CONSTRAINT customers_customer_type_check CHECK (((customer_type)::text = ANY ((ARRAY['distributor'::character varying, 'retailer'::character varying, 'direct_export'::character varying, 'other'::character varying])::text[]))),
    CONSTRAINT customers_status_check CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'inactive'::character varying, 'prospect'::character varying])::text[])))
);


--
-- Name: customers_customer_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.customers_customer_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customers_customer_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customers_customer_id_seq OWNED BY public.customers.customer_id;


--
-- Name: demand_forecasts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.demand_forecasts (
    forecast_id integer NOT NULL,
    product_id integer,
    forecast_month date NOT NULL,
    scenario character varying(20) DEFAULT 'base'::character varying NOT NULL,
    forecasted_volume_t numeric(10,2) NOT NULL,
    is_actual boolean DEFAULT false NOT NULL,
    model_version character varying(30),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    lower_bound_t numeric(10,2),
    upper_bound_t numeric(10,2),
    capacity_risk character varying(10),
    CONSTRAINT demand_forecasts_capacity_risk_check CHECK (((capacity_risk)::text = ANY ((ARRAY['low'::character varying, 'medium'::character varying, 'high'::character varying])::text[]))),
    CONSTRAINT demand_forecasts_scenario_check CHECK (((scenario)::text = ANY ((ARRAY['pessimistic'::character varying, 'base'::character varying, 'optimistic'::character varying])::text[])))
);


--
-- Name: demand_forecasts_forecast_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.demand_forecasts_forecast_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: demand_forecasts_forecast_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.demand_forecasts_forecast_id_seq OWNED BY public.demand_forecasts.forecast_id;


--
-- Name: demand_reports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.demand_reports (
    report_id integer NOT NULL,
    report_type character varying(40) DEFAULT 'weekly'::character varying NOT NULL,
    period_start date NOT NULL,
    period_end date NOT NULL,
    total_forecasted_volume_t numeric(10,2),
    total_actual_volume_t numeric(10,2),
    variance_pct numeric(5,2),
    summary text,
    generated_by character varying(100),
    generated_at timestamp without time zone DEFAULT now() NOT NULL,
    status character varying(20) DEFAULT 'ready'::character varying,
    is_scheduled boolean DEFAULT false,
    next_run_at timestamp without time zone,
    schedule_frequency character varying(20),
    CONSTRAINT demand_reports_status_check CHECK (((status)::text = ANY ((ARRAY['ready'::character varying, 'processing'::character varying, 'scheduled'::character varying, 'failed'::character varying])::text[])))
);


--
-- Name: demand_reports_report_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.demand_reports_report_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: demand_reports_report_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.demand_reports_report_id_seq OWNED BY public.demand_reports.report_id;


--
-- Name: forecast_model_runs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.forecast_model_runs (
    run_id integer NOT NULL,
    crop_variety character varying(60),
    region character varying(100),
    confidence_interval_pct numeric(5,2),
    use_weather_data boolean DEFAULT false,
    use_commodity_prices boolean DEFAULT false,
    use_geopolitical_index boolean DEFAULT false,
    model_status character varying(30) DEFAULT 'cold_start'::character varying,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: forecast_model_runs_run_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.forecast_model_runs_run_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: forecast_model_runs_run_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.forecast_model_runs_run_id_seq OWNED BY public.forecast_model_runs.run_id;


--
-- Name: order_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.order_items (
    order_item_id integer NOT NULL,
    order_id integer NOT NULL,
    product_id integer NOT NULL,
    quantity_kg numeric(12,2) NOT NULL,
    unit_price_eur numeric(10,2) NOT NULL
);


--
-- Name: order_items_order_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.order_items_order_item_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: order_items_order_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.order_items_order_item_id_seq OWNED BY public.order_items.order_item_id;


--
-- Name: orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.orders (
    order_id integer NOT NULL,
    customer_id integer NOT NULL,
    order_date date NOT NULL,
    total_volume_kg numeric(12,2),
    total_value_eur numeric(12,2),
    status character varying(30) DEFAULT 'pending'::character varying,
    sales_channel_id integer
);


--
-- Name: orders_order_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.orders_order_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: orders_order_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.orders_order_id_seq OWNED BY public.orders.order_id;


--
-- Name: pricing_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.pricing_history (
    pricing_id integer NOT NULL,
    product_id integer NOT NULL,
    price_date date NOT NULL,
    price_eur_per_kg numeric(8,2) NOT NULL,
    harvest_season_factor numeric(5,2),
    stock_level_factor numeric(5,2),
    demand_factor numeric(5,2),
    competitor_price_eur numeric(8,2),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    season_label character varying(30),
    stock_level_tons numeric(10,2),
    stock_target_tons numeric(10,2),
    demand_index_label character varying(20),
    is_applied boolean DEFAULT false
);


--
-- Name: pricing_history_pricing_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.pricing_history_pricing_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pricing_history_pricing_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pricing_history_pricing_id_seq OWNED BY public.pricing_history.pricing_id;


--
-- Name: pricing_recommendations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.pricing_recommendations (
    recommendation_id integer NOT NULL,
    product_id integer,
    message text NOT NULL,
    suggested_price_eur numeric(8,2),
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: pricing_recommendations_recommendation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.pricing_recommendations_recommendation_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pricing_recommendations_recommendation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pricing_recommendations_recommendation_id_seq OWNED BY public.pricing_recommendations.recommendation_id;


--
-- Name: product_batches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.product_batches (
    batch_id integer NOT NULL,
    batch_code character varying(20) NOT NULL,
    product_id integer,
    stock_t numeric(10,2),
    status character varying(20),
    synced_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: product_batches_batch_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.product_batches_batch_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_batches_batch_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_batches_batch_id_seq OWNED BY public.product_batches.batch_id;


--
-- Name: products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.products (
    product_id integer NOT NULL,
    name character varying(100) NOT NULL,
    variety character varying(80),
    unit character varying(10) DEFAULT 'kg'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: products_product_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.products_product_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: products_product_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.products_product_id_seq OWNED BY public.products.product_id;


--
-- Name: report_files; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.report_files (
    file_id integer NOT NULL,
    report_id integer NOT NULL,
    format character varying(10) NOT NULL,
    file_url character varying(255)
);


--
-- Name: report_files_file_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.report_files_file_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: report_files_file_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.report_files_file_id_seq OWNED BY public.report_files.file_id;


--
-- Name: sales_channel_targets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.sales_channel_targets (
    target_id integer NOT NULL,
    channel_id integer NOT NULL,
    period_start date NOT NULL,
    period_end date NOT NULL,
    revenue_target_eur numeric(12,2) NOT NULL
);


--
-- Name: sales_channel_targets_target_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.sales_channel_targets_target_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sales_channel_targets_target_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sales_channel_targets_target_id_seq OWNED BY public.sales_channel_targets.target_id;


--
-- Name: sales_channels; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.sales_channels (
    channel_id integer NOT NULL,
    name character varying(60) NOT NULL
);


--
-- Name: sales_channels_channel_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.sales_channels_channel_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sales_channels_channel_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sales_channels_channel_id_seq OWNED BY public.sales_channels.channel_id;


--
-- Name: shipment_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.shipment_events (
    event_id integer NOT NULL,
    shipment_id integer NOT NULL,
    step character varying(30) NOT NULL,
    event_time timestamp without time zone DEFAULT now() NOT NULL,
    note character varying(200)
);


--
-- Name: shipment_events_event_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.shipment_events_event_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: shipment_events_event_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.shipment_events_event_id_seq OWNED BY public.shipment_events.event_id;


--
-- Name: shipments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.shipments (
    shipment_id integer NOT NULL,
    order_id integer,
    origin character varying(100),
    destination character varying(100),
    status character varying(30) DEFAULT 'packed'::character varying NOT NULL,
    eta date,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    shipment_code character varying(20),
    vessel_name character varying(100),
    voyage_number character varying(30),
    delay_reason character varying(200),
    current_lat numeric(9,6),
    current_lon numeric(9,6),
    speed_knots numeric(5,1),
    current_location_label character varying(150),
    distance_remaining_nm numeric(8,1),
    commodity_description character varying(150),
    container_count integer,
    container_type character varying(20),
    cargo_weight_kg numeric(10,1),
    current_temp_c numeric(4,1),
    temp_status character varying(20),
    manifest_url character varying(255),
    CONSTRAINT shipments_status_check CHECK (((status)::text = ANY ((ARRAY['packed'::character varying, 'shipped'::character varying, 'port'::character varying, 'in_transit'::character varying, 'delivered'::character varying, 'delayed'::character varying])::text[])))
);


--
-- Name: shipments_shipment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.shipments_shipment_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: shipments_shipment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.shipments_shipment_id_seq OWNED BY public.shipments.shipment_id;


--
-- Name: alerts alert_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alerts ALTER COLUMN alert_id SET DEFAULT nextval('public.alerts_alert_id_seq'::regclass);


--
-- Name: campaign_metrics metric_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_metrics ALTER COLUMN metric_id SET DEFAULT nextval('public.campaign_metrics_metric_id_seq'::regclass);


--
-- Name: campaigns campaign_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaigns ALTER COLUMN campaign_id SET DEFAULT nextval('public.campaigns_campaign_id_seq'::regclass);


--
-- Name: certifications certification_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certifications ALTER COLUMN certification_id SET DEFAULT nextval('public.certifications_certification_id_seq'::regclass);


--
-- Name: competitor_prices competitor_price_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.competitor_prices ALTER COLUMN competitor_price_id SET DEFAULT nextval('public.competitor_prices_competitor_price_id_seq'::regclass);


--
-- Name: customer_contacts contact_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_contacts ALTER COLUMN contact_id SET DEFAULT nextval('public.customer_contacts_contact_id_seq'::regclass);


--
-- Name: customers customer_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers ALTER COLUMN customer_id SET DEFAULT nextval('public.customers_customer_id_seq'::regclass);


--
-- Name: demand_forecasts forecast_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.demand_forecasts ALTER COLUMN forecast_id SET DEFAULT nextval('public.demand_forecasts_forecast_id_seq'::regclass);


--
-- Name: demand_reports report_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.demand_reports ALTER COLUMN report_id SET DEFAULT nextval('public.demand_reports_report_id_seq'::regclass);


--
-- Name: forecast_model_runs run_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.forecast_model_runs ALTER COLUMN run_id SET DEFAULT nextval('public.forecast_model_runs_run_id_seq'::regclass);


--
-- Name: order_items order_item_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items ALTER COLUMN order_item_id SET DEFAULT nextval('public.order_items_order_item_id_seq'::regclass);


--
-- Name: orders order_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders ALTER COLUMN order_id SET DEFAULT nextval('public.orders_order_id_seq'::regclass);


--
-- Name: pricing_history pricing_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pricing_history ALTER COLUMN pricing_id SET DEFAULT nextval('public.pricing_history_pricing_id_seq'::regclass);


--
-- Name: pricing_recommendations recommendation_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pricing_recommendations ALTER COLUMN recommendation_id SET DEFAULT nextval('public.pricing_recommendations_recommendation_id_seq'::regclass);


--
-- Name: product_batches batch_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_batches ALTER COLUMN batch_id SET DEFAULT nextval('public.product_batches_batch_id_seq'::regclass);


--
-- Name: products product_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products ALTER COLUMN product_id SET DEFAULT nextval('public.products_product_id_seq'::regclass);


--
-- Name: report_files file_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.report_files ALTER COLUMN file_id SET DEFAULT nextval('public.report_files_file_id_seq'::regclass);


--
-- Name: sales_channel_targets target_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales_channel_targets ALTER COLUMN target_id SET DEFAULT nextval('public.sales_channel_targets_target_id_seq'::regclass);


--
-- Name: sales_channels channel_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales_channels ALTER COLUMN channel_id SET DEFAULT nextval('public.sales_channels_channel_id_seq'::regclass);


--
-- Name: shipment_events event_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipment_events ALTER COLUMN event_id SET DEFAULT nextval('public.shipment_events_event_id_seq'::regclass);


--
-- Name: shipments shipment_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipments ALTER COLUMN shipment_id SET DEFAULT nextval('public.shipments_shipment_id_seq'::regclass);


--
-- Name: alerts alerts_alert_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.alerts
        ADD CONSTRAINT alerts_alert_code_key UNIQUE (alert_code);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: alerts alerts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.alerts
        ADD CONSTRAINT alerts_pkey PRIMARY KEY (alert_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: campaign_metrics campaign_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.campaign_metrics
        ADD CONSTRAINT campaign_metrics_pkey PRIMARY KEY (metric_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: campaigns campaigns_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.campaigns
        ADD CONSTRAINT campaigns_pkey PRIMARY KEY (campaign_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: certifications certifications_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.certifications
        ADD CONSTRAINT certifications_name_key UNIQUE (name);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: certifications certifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.certifications
        ADD CONSTRAINT certifications_pkey PRIMARY KEY (certification_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: competitor_prices competitor_prices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.competitor_prices
        ADD CONSTRAINT competitor_prices_pkey PRIMARY KEY (competitor_price_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: customer_certifications customer_certifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.customer_certifications
        ADD CONSTRAINT customer_certifications_pkey PRIMARY KEY (customer_id, certification_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: customer_contacts customer_contacts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.customer_contacts
        ADD CONSTRAINT customer_contacts_pkey PRIMARY KEY (contact_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: customers customers_customer_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.customers
        ADD CONSTRAINT customers_customer_code_key UNIQUE (customer_code);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.customers
        ADD CONSTRAINT customers_pkey PRIMARY KEY (customer_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: demand_forecasts demand_forecasts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.demand_forecasts
        ADD CONSTRAINT demand_forecasts_pkey PRIMARY KEY (forecast_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: demand_reports demand_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.demand_reports
        ADD CONSTRAINT demand_reports_pkey PRIMARY KEY (report_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: forecast_model_runs forecast_model_runs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.forecast_model_runs
        ADD CONSTRAINT forecast_model_runs_pkey PRIMARY KEY (run_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.order_items
        ADD CONSTRAINT order_items_pkey PRIMARY KEY (order_item_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.orders
        ADD CONSTRAINT orders_pkey PRIMARY KEY (order_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: pricing_history pricing_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.pricing_history
        ADD CONSTRAINT pricing_history_pkey PRIMARY KEY (pricing_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: pricing_recommendations pricing_recommendations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.pricing_recommendations
        ADD CONSTRAINT pricing_recommendations_pkey PRIMARY KEY (recommendation_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: product_batches product_batches_batch_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.product_batches
        ADD CONSTRAINT product_batches_batch_code_key UNIQUE (batch_code);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: product_batches product_batches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.product_batches
        ADD CONSTRAINT product_batches_pkey PRIMARY KEY (batch_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.products
        ADD CONSTRAINT products_pkey PRIMARY KEY (product_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: report_files report_files_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.report_files
        ADD CONSTRAINT report_files_pkey PRIMARY KEY (file_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: sales_channel_targets sales_channel_targets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.sales_channel_targets
        ADD CONSTRAINT sales_channel_targets_pkey PRIMARY KEY (target_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: sales_channels sales_channels_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.sales_channels
        ADD CONSTRAINT sales_channels_name_key UNIQUE (name);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: sales_channels sales_channels_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.sales_channels
        ADD CONSTRAINT sales_channels_pkey PRIMARY KEY (channel_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: shipment_events shipment_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.shipment_events
        ADD CONSTRAINT shipment_events_pkey PRIMARY KEY (event_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: shipments shipments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.shipments
        ADD CONSTRAINT shipments_pkey PRIMARY KEY (shipment_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: shipments shipments_shipment_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.shipments
        ADD CONSTRAINT shipments_shipment_code_key UNIQUE (shipment_code);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: idx_alerts_severity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_alerts_severity ON public.alerts USING btree (severity);


--
-- Name: idx_alerts_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_alerts_status ON public.alerts USING btree (status);


--
-- Name: idx_campaign_metrics_camp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_campaign_metrics_camp ON public.campaign_metrics USING btree (campaign_id);


--
-- Name: idx_channel_targets_channel; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_channel_targets_channel ON public.sales_channel_targets USING btree (channel_id, period_start);


--
-- Name: idx_competitor_prices_product; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_competitor_prices_product ON public.competitor_prices USING btree (product_id, price_date);


--
-- Name: idx_customer_contacts_cust; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_customer_contacts_cust ON public.customer_contacts USING btree (customer_id);


--
-- Name: idx_customers_country; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_customers_country ON public.customers USING btree (country);


--
-- Name: idx_customers_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_customers_status ON public.customers USING btree (status);


--
-- Name: idx_forecasts_month; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_forecasts_month ON public.demand_forecasts USING btree (forecast_month, scenario);


--
-- Name: idx_order_items_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_order_items_order ON public.order_items USING btree (order_id);


--
-- Name: idx_order_items_product; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_order_items_product ON public.order_items USING btree (product_id);


--
-- Name: idx_orders_channel; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_orders_channel ON public.orders USING btree (sales_channel_id);


--
-- Name: idx_orders_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_orders_customer ON public.orders USING btree (customer_id);


--
-- Name: idx_pricing_product_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_pricing_product_date ON public.pricing_history USING btree (product_id, price_date);


--
-- Name: idx_product_batches_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_product_batches_status ON public.product_batches USING btree (status);


--
-- Name: idx_report_files_report; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_report_files_report ON public.report_files USING btree (report_id);


--
-- Name: idx_reports_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_reports_period ON public.demand_reports USING btree (period_start, period_end);


--
-- Name: idx_shipment_events_ship; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_shipment_events_ship ON public.shipment_events USING btree (shipment_id);


--
-- Name: idx_shipments_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_shipments_order ON public.shipments USING btree (order_id);


--
-- Name: campaign_metrics campaign_metrics_campaign_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.campaign_metrics
        ADD CONSTRAINT campaign_metrics_campaign_id_fkey FOREIGN KEY (campaign_id) REFERENCES public.campaigns(campaign_id) ON DELETE CASCADE;
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: competitor_prices competitor_prices_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.competitor_prices
        ADD CONSTRAINT competitor_prices_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(product_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: customer_certifications customer_certifications_certification_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.customer_certifications
        ADD CONSTRAINT customer_certifications_certification_id_fkey FOREIGN KEY (certification_id) REFERENCES public.certifications(certification_id) ON DELETE CASCADE;
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: customer_certifications customer_certifications_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.customer_certifications
        ADD CONSTRAINT customer_certifications_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(customer_id) ON DELETE CASCADE;
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: customer_contacts customer_contacts_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.customer_contacts
        ADD CONSTRAINT customer_contacts_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(customer_id) ON DELETE CASCADE;
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: demand_forecasts demand_forecasts_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.demand_forecasts
        ADD CONSTRAINT demand_forecasts_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(product_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: order_items order_items_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.order_items
        ADD CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(order_id) ON DELETE CASCADE;
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: order_items order_items_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.order_items
        ADD CONSTRAINT order_items_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(product_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: orders orders_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.orders
        ADD CONSTRAINT orders_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(customer_id) ON DELETE RESTRICT;
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: orders orders_sales_channel_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.orders
        ADD CONSTRAINT orders_sales_channel_id_fkey FOREIGN KEY (sales_channel_id) REFERENCES public.sales_channels(channel_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: pricing_history pricing_history_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.pricing_history
        ADD CONSTRAINT pricing_history_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(product_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: pricing_recommendations pricing_recommendations_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.pricing_recommendations
        ADD CONSTRAINT pricing_recommendations_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(product_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: product_batches product_batches_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.product_batches
        ADD CONSTRAINT product_batches_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(product_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: report_files report_files_report_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.report_files
        ADD CONSTRAINT report_files_report_id_fkey FOREIGN KEY (report_id) REFERENCES public.demand_reports(report_id) ON DELETE CASCADE;
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: sales_channel_targets sales_channel_targets_channel_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.sales_channel_targets
        ADD CONSTRAINT sales_channel_targets_channel_id_fkey FOREIGN KEY (channel_id) REFERENCES public.sales_channels(channel_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: shipment_events shipment_events_shipment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.shipment_events
        ADD CONSTRAINT shipment_events_shipment_id_fkey FOREIGN KEY (shipment_id) REFERENCES public.shipments(shipment_id) ON DELETE CASCADE;
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- Name: shipments shipments_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
    ALTER TABLE ONLY public.shipments
        ADD CONSTRAINT shipments_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(order_id);
EXCEPTION
    WHEN duplicate_object OR duplicate_table OR invalid_table_definition THEN NULL;
END $$;


--
-- PostgreSQL database dump complete
--

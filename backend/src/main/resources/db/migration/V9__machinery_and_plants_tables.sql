-- Tables of the machinery and plants modules (brought in by the develop merge).
-- Until now these tables were created by Hibernate ddl-auto=update; since the schema is
-- Flyway-managed (ddl-auto=validate), they must exist here or the application fails to start.

CREATE TABLE IF NOT EXISTS equipment (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(30) NOT NULL UNIQUE,
    category    VARCHAR(40) NOT NULL,
    stage       VARCHAR(40) NOT NULL,
    status      VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS varietes (
    id                        BIGSERIAL PRIMARY KEY,
    id_ferme                  INTEGER,
    nom                       VARCHAR(255) NOT NULL,
    nombre_arbres             INTEGER,
    espacement_inter_rang_m   DOUBLE PRECISION,
    espacement_intra_rang_m   DOUBLE PRECISION,
    densite_arbres_ha         DOUBLE PRECISION,
    rendement_attendu_kg      DOUBLE PRECISION,
    rendement_reel_kg         DOUBLE PRECISION,
    vigueur                   VARCHAR(255),
    bloc_parcelle             VARCHAR(255),
    origine_plant             VARCHAR(255),
    source                    VARCHAR(255),
    date_maj                  TIMESTAMPTZ
);

DROP TABLE IF EXISTS users, categories, locations, events,
    participation_requests, compilations, compilation_events CASCADE;

/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
/*DROP TYPE IF EXISTS mood CASCADE;

CREATE TYPE IF NOT EXISTS mood AS ENUM ('sad', 'ok', 'happy');*/


CREATE TABLE IF NOT EXISTS users
(
    id    BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name  VARCHAR(250)                            NOT NULL,
    email VARCHAR(254) UNIQUE                     NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_user UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS categories
(
    id   BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name VARCHAR(50)                             NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS locations
(
    id  BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    lat REAL                                    NOT NULL,
    lon REAL                                    NOT NULL,
    CONSTRAINT pk_locations PRIMARY KEY (id),
    CONSTRAINT uq_locations UNIQUE (lat, lon)
);

CREATE TABLE IF NOT EXISTS events
(
    id                 BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    annotation         VARCHAR(2000)                           NOT NULL,
    category_id        BIGINT                                  NOT NULL,
    description        VARCHAR(7000)                           NOT NULL,
    event_date         TIMESTAMP(0)                            NOT NULL,
    location_id        BIGINT                                  NOT NULL,
    paid               BOOLEAN DEFAULT FALSE,
    participant_limit  INTEGER DEFAULT 0,
    request_moderation BOOLEAN DEFAULT TRUE,
    title              VARCHAR(120)                            NOT NULL,


    /*пока под вопросом !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
    confirmed_Requests BIGINT,
    create_date        TIMESTAMP(0),
    initiator_id       BIGINT                                  NOT NULL,
    published_date     TIMESTAMP(0),
    status             VARCHAR(200),
    /*пока под вопросом !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/


    CONSTRAINT pk_events PRIMARY KEY (id),
    CONSTRAINT fk_events_users FOREIGN KEY (initiator_id) REFERENCES users (id),
    CONSTRAINT fk_events_categories FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_events_location FOREIGN KEY (location_id) REFERENCES locations (id)
);

CREATE TABLE IF NOT EXISTS participation_requests
(
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created      TIMESTAMP(3),
    event_id     BIGINT                                  NOT NULL,
    requester_id BIGINT                                  NOT NULL,
    status       VARCHAR(20),-- использовать ENUM ENUM ENUM ENUM ENUM ENUM ENUM ENUM ENUM ???????????????????????????????????
    CONSTRAINT pk_participation_requests PRIMARY KEY (id),
    CONSTRAINT fk_participation_requests_events FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_sparticipation_requests_users FOREIGN KEY (requester_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS compilations
(
    id     BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    pinned BOOLEAN DEFAULT FALSE                   NOT NULL,
    title  VARCHAR(50)                             NOT NULL,
    CONSTRAINT pk_compilations PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS compilation_events
(
    compilation_id BIGINT NOT NULL,
    event_id       BIGINT NOT NULL,
    FOREIGN KEY (compilation_id) REFERENCES compilations (id),
    FOREIGN KEY (event_id) REFERENCES events (id)
);
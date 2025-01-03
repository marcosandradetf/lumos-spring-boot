--
-- PostgreSQL database dump
--

-- Dumped from database version 17.2 (Debian 17.2-1.pgdg120+1)
-- Dumped by pg_dump version 17.2 (Debian 17.2-1.pgdg120+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: unaccent; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;


--
-- Name: EXTENSION unaccent; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION unaccent IS 'text search dictionary that removes accents';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: contrato_aditivo_quantitativo; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.contrato_aditivo_quantitativo (
    valor_aditivo_quantitativo numeric(38,2),
    contract_id_contract bigint,
    id_contrato_aditivo_quantitativo bigint NOT NULL
);


ALTER TABLE public.contrato_aditivo_quantitativo OWNER TO postgres;

--
-- Name: contrato_aditivo_quantitativo_id_contrato_aditivo_quantitat_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.contrato_aditivo_quantitativo ALTER COLUMN id_contrato_aditivo_quantitativo ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.contrato_aditivo_quantitativo_id_contrato_aditivo_quantitat_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: contrato_aditivo_valor; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.contrato_aditivo_valor (
    valor_aditivo numeric(38,2),
    contract_id_contract bigint,
    id_contrato_aditivo_valor bigint NOT NULL
);


ALTER TABLE public.contrato_aditivo_valor OWNER TO postgres;

--
-- Name: contrato_aditivo_valor_id_contrato_aditivo_valor_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.contrato_aditivo_valor ALTER COLUMN id_contrato_aditivo_valor ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.contrato_aditivo_valor_id_contrato_aditivo_valor_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: contrato_equipe; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.contrato_equipe (
    id_equipe integer NOT NULL
);


ALTER TABLE public.contrato_equipe OWNER TO postgres;

--
-- Name: contrato_equipe_id_equipe_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.contrato_equipe ALTER COLUMN id_equipe ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.contrato_equipe_id_equipe_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: contrato_tarefa; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.contrato_tarefa (
    equipe_id_equipe integer,
    quantidade_executada integer NOT NULL,
    quantidade_recebida integer NOT NULL,
    contract_id_contract bigint,
    id_tarefa bigint NOT NULL
);


ALTER TABLE public.contrato_tarefa OWNER TO postgres;

--
-- Name: contrato_tarefa_id_tarefa_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.contrato_tarefa ALTER COLUMN id_tarefa ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.contrato_tarefa_id_tarefa_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_companies; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_companies (
    id_company bigint NOT NULL,
    company_name text NOT NULL
);


ALTER TABLE public.tb_companies OWNER TO postgres;

--
-- Name: tb_companies_id_company_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_companies ALTER COLUMN id_company ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_companies_id_company_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_contracts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_contracts (
    contract_value numeric(38,2),
    creation_date timestamp(6) with time zone,
    id_contract bigint NOT NULL,
    city character varying(255),
    contract_doc character varying(255),
    contract_number character varying(255),
    uf character varying(255)
);


ALTER TABLE public.tb_contracts OWNER TO postgres;

--
-- Name: tb_contracts_id_contract_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_contracts ALTER COLUMN id_contract ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_contracts_id_contract_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_deposits; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_deposits (
    company_id bigint NOT NULL,
    id_deposit bigint NOT NULL,
    deposit_name text NOT NULL
);


ALTER TABLE public.tb_deposits OWNER TO postgres;

--
-- Name: tb_deposits_id_deposit_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_deposits ALTER COLUMN id_deposit ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_deposits_id_deposit_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_groups; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_groups (
    id_group bigint NOT NULL,
    group_name text NOT NULL
);


ALTER TABLE public.tb_groups OWNER TO postgres;

--
-- Name: tb_groups_id_group_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_groups ALTER COLUMN id_group ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_groups_id_group_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_items (
    id_item integer NOT NULL,
    item_quantity integer NOT NULL,
    item_total_value numeric(38,2),
    item_value numeric(38,2),
    id_contract bigint,
    id_material bigint
);


ALTER TABLE public.tb_items OWNER TO postgres;

--
-- Name: tb_items_id_item_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_items ALTER COLUMN id_item ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_items_id_item_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_logs (
    creation_timestamp timestamp(6) with time zone,
    id_log bigint NOT NULL,
    id_user uuid,
    category character varying(255),
    message character varying(255),
    type character varying(255)
);


ALTER TABLE public.tb_logs OWNER TO postgres;

--
-- Name: tb_logs_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.tb_logs_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tb_logs_seq OWNER TO postgres;

--
-- Name: tb_material_reservation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_material_reservation (
    quantity_completed integer DEFAULT 0 NOT NULL,
    reserved_quantity integer NOT NULL,
    id_material_reservation bigint NOT NULL,
    material_id_material bigint,
    pre_measurement_id_measurement bigint,
    status character varying(255),
    stock_reservation_name text
);


ALTER TABLE public.tb_material_reservation OWNER TO postgres;

--
-- Name: tb_material_reservation_id_material_reservation_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_material_reservation ALTER COLUMN id_material_reservation ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_material_reservation_id_material_reservation_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_materials; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_materials (
    cost_price numeric(38,2),
    inactive boolean DEFAULT false NOT NULL,
    stock_available integer DEFAULT 0 NOT NULL,
    stock_quantity integer DEFAULT 0 NOT NULL,
    id_company bigint NOT NULL,
    id_deposit bigint NOT NULL,
    id_material bigint NOT NULL,
    id_material_type bigint NOT NULL,
    buy_unit text NOT NULL,
    material_amps character varying(255),
    material_brand text,
    material_length character varying(255),
    material_name text NOT NULL,
    material_power character varying(255),
    request_unit text NOT NULL
);


ALTER TABLE public.tb_materials OWNER TO postgres;

--
-- Name: tb_materials_id_material_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_materials ALTER COLUMN id_material ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_materials_id_material_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_pre_measurement; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_pre_measurement (
    id_measurement bigint NOT NULL,
    city character varying(255)
);


ALTER TABLE public.tb_pre_measurement OWNER TO postgres;

--
-- Name: tb_pre_measurement_id_measurement_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_pre_measurement ALTER COLUMN id_measurement ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_pre_measurement_id_measurement_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_refresh_token; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_refresh_token (
    revoked boolean NOT NULL,
    expiry_date timestamp(6) with time zone NOT NULL,
    id_token bigint NOT NULL,
    id_user uuid NOT NULL,
    token text NOT NULL
);


ALTER TABLE public.tb_refresh_token OWNER TO postgres;

--
-- Name: tb_refresh_token_id_token_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_refresh_token ALTER COLUMN id_token ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_refresh_token_id_token_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_roles (
    id_role bigint NOT NULL,
    nome_role character varying(255)
);


ALTER TABLE public.tb_roles OWNER TO postgres;

--
-- Name: tb_roles_id_role_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_roles ALTER COLUMN id_role ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_roles_id_role_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_stock_movement; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_stock_movement (
    input_quantity integer DEFAULT 0 NOT NULL,
    price_per_item numeric(38,2) NOT NULL,
    quantity_package integer DEFAULT 1 NOT NULL,
    material_id bigint NOT NULL,
    stock_movement_id bigint NOT NULL,
    stock_movement_refresh timestamp(6) with time zone NOT NULL,
    supplier_id bigint,
    user_created_id_user uuid,
    user_finished_id_user uuid,
    buy_unit text NOT NULL,
    status character varying(255),
    stock_movement_description text,
    CONSTRAINT tb_stock_movement_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


ALTER TABLE public.tb_stock_movement OWNER TO postgres;

--
-- Name: tb_stock_movement_stock_movement_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_stock_movement ALTER COLUMN stock_movement_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_stock_movement_stock_movement_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_street; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_street (
    id_street bigint NOT NULL,
    name text
);


ALTER TABLE public.tb_street OWNER TO postgres;

--
-- Name: tb_street_id_street_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_street ALTER COLUMN id_street ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_street_id_street_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_supplier; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_supplier (
    supplier_id bigint NOT NULL,
    supplier_address text,
    supplier_cnpj text,
    supplier_contact text,
    supplier_email text,
    supplier_name text,
    supplier_phone text
);


ALTER TABLE public.tb_supplier OWNER TO postgres;

--
-- Name: tb_supplier_supplier_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_supplier ALTER COLUMN supplier_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_supplier_supplier_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_team; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_team (
    id_team bigint NOT NULL,
    region text,
    team_name text
);


ALTER TABLE public.tb_team OWNER TO postgres;

--
-- Name: tb_team_id_team_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_team ALTER COLUMN id_team ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_team_id_team_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_team_measurement; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_team_measurement (
    id_measurement bigint NOT NULL,
    id_team bigint NOT NULL
);


ALTER TABLE public.tb_team_measurement OWNER TO postgres;

--
-- Name: tb_types; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_types (
    id_group bigint NOT NULL,
    id_type bigint NOT NULL,
    type_name text NOT NULL
);


ALTER TABLE public.tb_types OWNER TO postgres;

--
-- Name: tb_types_id_type_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tb_types ALTER COLUMN id_type ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tb_types_id_type_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tb_users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_users (
    date_of_birth timestamp(6) without time zone,
    id_user uuid NOT NULL,
    email character varying(255),
    last_name character varying(255),
    name character varying(255),
    password character varying(255),
    username character varying(255)
);


ALTER TABLE public.tb_users OWNER TO postgres;

--
-- Name: tb_users_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tb_users_roles (
    id_role bigint NOT NULL,
    id_user uuid NOT NULL
);


ALTER TABLE public.tb_users_roles OWNER TO postgres;

--
-- Name: contrato_aditivo_quantitativo contrato_aditivo_quantitativo_contract_id_contract_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contrato_aditivo_quantitativo
    ADD CONSTRAINT contrato_aditivo_quantitativo_contract_id_contract_key UNIQUE (contract_id_contract);


--
-- Name: contrato_aditivo_quantitativo contrato_aditivo_quantitativo_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contrato_aditivo_quantitativo
    ADD CONSTRAINT contrato_aditivo_quantitativo_pkey PRIMARY KEY (id_contrato_aditivo_quantitativo);


--
-- Name: contrato_aditivo_valor contrato_aditivo_valor_contract_id_contract_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contrato_aditivo_valor
    ADD CONSTRAINT contrato_aditivo_valor_contract_id_contract_key UNIQUE (contract_id_contract);


--
-- Name: contrato_aditivo_valor contrato_aditivo_valor_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contrato_aditivo_valor
    ADD CONSTRAINT contrato_aditivo_valor_pkey PRIMARY KEY (id_contrato_aditivo_valor);


--
-- Name: contrato_equipe contrato_equipe_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contrato_equipe
    ADD CONSTRAINT contrato_equipe_pkey PRIMARY KEY (id_equipe);


--
-- Name: contrato_tarefa contrato_tarefa_equipe_id_equipe_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contrato_tarefa
    ADD CONSTRAINT contrato_tarefa_equipe_id_equipe_key UNIQUE (equipe_id_equipe);


--
-- Name: contrato_tarefa contrato_tarefa_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contrato_tarefa
    ADD CONSTRAINT contrato_tarefa_pkey PRIMARY KEY (id_tarefa);


--
-- Name: tb_companies tb_companies_company_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_companies
    ADD CONSTRAINT tb_companies_company_name_key UNIQUE (company_name);


--
-- Name: tb_companies tb_companies_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_companies
    ADD CONSTRAINT tb_companies_pkey PRIMARY KEY (id_company);


--
-- Name: tb_contracts tb_contracts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_contracts
    ADD CONSTRAINT tb_contracts_pkey PRIMARY KEY (id_contract);


--
-- Name: tb_deposits tb_deposits_deposit_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_deposits
    ADD CONSTRAINT tb_deposits_deposit_name_key UNIQUE (deposit_name);


--
-- Name: tb_deposits tb_deposits_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_deposits
    ADD CONSTRAINT tb_deposits_pkey PRIMARY KEY (id_deposit);


--
-- Name: tb_groups tb_groups_group_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_groups
    ADD CONSTRAINT tb_groups_group_name_key UNIQUE (group_name);


--
-- Name: tb_groups tb_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_groups
    ADD CONSTRAINT tb_groups_pkey PRIMARY KEY (id_group);


--
-- Name: tb_items tb_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_items
    ADD CONSTRAINT tb_items_pkey PRIMARY KEY (id_item);


--
-- Name: tb_logs tb_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_logs
    ADD CONSTRAINT tb_logs_pkey PRIMARY KEY (id_log);


--
-- Name: tb_material_reservation tb_material_reservation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_material_reservation
    ADD CONSTRAINT tb_material_reservation_pkey PRIMARY KEY (id_material_reservation);


--
-- Name: tb_materials tb_materials_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_materials
    ADD CONSTRAINT tb_materials_pkey PRIMARY KEY (id_material);


--
-- Name: tb_pre_measurement tb_pre_measurement_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_pre_measurement
    ADD CONSTRAINT tb_pre_measurement_pkey PRIMARY KEY (id_measurement);


--
-- Name: tb_refresh_token tb_refresh_token_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_refresh_token
    ADD CONSTRAINT tb_refresh_token_pkey PRIMARY KEY (id_token);


--
-- Name: tb_refresh_token tb_refresh_token_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_refresh_token
    ADD CONSTRAINT tb_refresh_token_token_key UNIQUE (token);


--
-- Name: tb_roles tb_roles_nome_role_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_roles
    ADD CONSTRAINT tb_roles_nome_role_key UNIQUE (nome_role);


--
-- Name: tb_roles tb_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_roles
    ADD CONSTRAINT tb_roles_pkey PRIMARY KEY (id_role);


--
-- Name: tb_stock_movement tb_stock_movement_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_stock_movement
    ADD CONSTRAINT tb_stock_movement_pkey PRIMARY KEY (stock_movement_id);


--
-- Name: tb_street tb_street_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_street
    ADD CONSTRAINT tb_street_pkey PRIMARY KEY (id_street);


--
-- Name: tb_supplier tb_supplier_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_supplier
    ADD CONSTRAINT tb_supplier_pkey PRIMARY KEY (supplier_id);


--
-- Name: tb_team tb_team_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_team
    ADD CONSTRAINT tb_team_pkey PRIMARY KEY (id_team);


--
-- Name: tb_types tb_types_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_types
    ADD CONSTRAINT tb_types_pkey PRIMARY KEY (id_type);


--
-- Name: tb_users tb_users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_users
    ADD CONSTRAINT tb_users_email_key UNIQUE (email);


--
-- Name: tb_users tb_users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_users
    ADD CONSTRAINT tb_users_pkey PRIMARY KEY (id_user);


--
-- Name: tb_users_roles tb_users_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_users_roles
    ADD CONSTRAINT tb_users_roles_pkey PRIMARY KEY (id_role, id_user);


--
-- Name: tb_users tb_users_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_users
    ADD CONSTRAINT tb_users_username_key UNIQUE (username);


--
-- Name: contrato_tarefa fk1o87egfs3x6r7h4ofw13yvba6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contrato_tarefa
    ADD CONSTRAINT fk1o87egfs3x6r7h4ofw13yvba6 FOREIGN KEY (equipe_id_equipe) REFERENCES public.contrato_equipe(id_equipe);


--
-- Name: tb_refresh_token fk22l7qcsuk3g734h9whcajqqfp; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_refresh_token
    ADD CONSTRAINT fk22l7qcsuk3g734h9whcajqqfp FOREIGN KEY (id_user) REFERENCES public.tb_users(id_user);


--
-- Name: tb_team_measurement fk2ufgkom7hl40gu7g9eq4lij6x; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_team_measurement
    ADD CONSTRAINT fk2ufgkom7hl40gu7g9eq4lij6x FOREIGN KEY (id_measurement) REFERENCES public.tb_pre_measurement(id_measurement);


--
-- Name: tb_deposits fk30bl52pqdqauqiwn1c4ugsrac; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_deposits
    ADD CONSTRAINT fk30bl52pqdqauqiwn1c4ugsrac FOREIGN KEY (company_id) REFERENCES public.tb_companies(id_company);


--
-- Name: tb_stock_movement fk3acrsye3c555jcp0dq3y6l5e4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_stock_movement
    ADD CONSTRAINT fk3acrsye3c555jcp0dq3y6l5e4 FOREIGN KEY (user_created_id_user) REFERENCES public.tb_users(id_user);


--
-- Name: tb_items fk5fu8qyuwhghch6b1s33py6yod; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_items
    ADD CONSTRAINT fk5fu8qyuwhghch6b1s33py6yod FOREIGN KEY (id_material) REFERENCES public.tb_materials(id_material);


--
-- Name: tb_items fk6awtxqmkw1x530ya0uypjb9l9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_items
    ADD CONSTRAINT fk6awtxqmkw1x530ya0uypjb9l9 FOREIGN KEY (id_contract) REFERENCES public.tb_contracts(id_contract);


--
-- Name: tb_materials fk7aaiq92t195insfq5sgv6tpj1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_materials
    ADD CONSTRAINT fk7aaiq92t195insfq5sgv6tpj1 FOREIGN KEY (id_company) REFERENCES public.tb_companies(id_company);


--
-- Name: tb_materials fk7ydh03ya0poxlswbtevi3efnm; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_materials
    ADD CONSTRAINT fk7ydh03ya0poxlswbtevi3efnm FOREIGN KEY (id_material_type) REFERENCES public.tb_types(id_type);


--
-- Name: tb_types fk8r67ymnr5b9wmc3rpwkkd7i2l; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_types
    ADD CONSTRAINT fk8r67ymnr5b9wmc3rpwkkd7i2l FOREIGN KEY (id_group) REFERENCES public.tb_groups(id_group);


--
-- Name: tb_stock_movement fk8ss20h1kkxgj3xovkq0soyw64; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_stock_movement
    ADD CONSTRAINT fk8ss20h1kkxgj3xovkq0soyw64 FOREIGN KEY (supplier_id) REFERENCES public.tb_supplier(supplier_id);


--
-- Name: tb_users_roles fkavuue8j4nwuigf7hml9jcqt3k; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_users_roles
    ADD CONSTRAINT fkavuue8j4nwuigf7hml9jcqt3k FOREIGN KEY (id_role) REFERENCES public.tb_roles(id_role);


--
-- Name: contrato_tarefa fkb0tkjmpnjuph6ig7mcie105ga; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contrato_tarefa
    ADD CONSTRAINT fkb0tkjmpnjuph6ig7mcie105ga FOREIGN KEY (contract_id_contract) REFERENCES public.tb_contracts(id_contract);


--
-- Name: tb_stock_movement fkb232gy3422m7cvbuwa37ghf3y; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_stock_movement
    ADD CONSTRAINT fkb232gy3422m7cvbuwa37ghf3y FOREIGN KEY (user_finished_id_user) REFERENCES public.tb_users(id_user);


--
-- Name: tb_material_reservation fkb5v9wl5vw1j615672tc9mjipa; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_material_reservation
    ADD CONSTRAINT fkb5v9wl5vw1j615672tc9mjipa FOREIGN KEY (pre_measurement_id_measurement) REFERENCES public.tb_pre_measurement(id_measurement);


--
-- Name: contrato_aditivo_valor fkh0edr1yshvmh3kr3hab8vcyrn; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contrato_aditivo_valor
    ADD CONSTRAINT fkh0edr1yshvmh3kr3hab8vcyrn FOREIGN KEY (contract_id_contract) REFERENCES public.tb_contracts(id_contract);


--
-- Name: contrato_aditivo_quantitativo fkh9hv418lffu2l1kvo6wiixkhb; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contrato_aditivo_quantitativo
    ADD CONSTRAINT fkh9hv418lffu2l1kvo6wiixkhb FOREIGN KEY (contract_id_contract) REFERENCES public.tb_contracts(id_contract);


--
-- Name: tb_team_measurement fkjj4myrdt7bm0kn3p42qxif3np; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_team_measurement
    ADD CONSTRAINT fkjj4myrdt7bm0kn3p42qxif3np FOREIGN KEY (id_team) REFERENCES public.tb_team(id_team);


--
-- Name: tb_materials fkjv99evrhfus9i6kewpwe1xi0n; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_materials
    ADD CONSTRAINT fkjv99evrhfus9i6kewpwe1xi0n FOREIGN KEY (id_deposit) REFERENCES public.tb_deposits(id_deposit);


--
-- Name: tb_contracts fklk5wm9l152fqda3xmrcmscoj0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_contracts
    ADD CONSTRAINT fklk5wm9l152fqda3xmrcmscoj0 FOREIGN KEY (id_contract) REFERENCES public.tb_materials(id_material);


--
-- Name: tb_items fkopt7gsn54tlg40kf1ssq7yt28; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_items
    ADD CONSTRAINT fkopt7gsn54tlg40kf1ssq7yt28 FOREIGN KEY (id_item) REFERENCES public.tb_street(id_street);


--
-- Name: tb_users_roles fkpxqifg4h2h48v9fit0pe3hvu1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_users_roles
    ADD CONSTRAINT fkpxqifg4h2h48v9fit0pe3hvu1 FOREIGN KEY (id_user) REFERENCES public.tb_users(id_user);


--
-- Name: tb_logs fkr9hxwmji48pndyjjsqda4yevs; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_logs
    ADD CONSTRAINT fkr9hxwmji48pndyjjsqda4yevs FOREIGN KEY (id_user) REFERENCES public.tb_users(id_user);


--
-- Name: tb_material_reservation fkrafp78ymra1rercv9joiogc76; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_material_reservation
    ADD CONSTRAINT fkrafp78ymra1rercv9joiogc76 FOREIGN KEY (material_id_material) REFERENCES public.tb_materials(id_material);


--
-- Name: tb_stock_movement fktrcbxir2202ljnir1knxmnaxg; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tb_stock_movement
    ADD CONSTRAINT fktrcbxir2202ljnir1knxmnaxg FOREIGN KEY (material_id) REFERENCES public.tb_materials(id_material);


--
-- PostgreSQL database dump complete
--


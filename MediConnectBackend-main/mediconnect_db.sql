-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : mar. 07 avr. 2026 à 12:33
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `mediconnect_db`
--

-- --------------------------------------------------------

--
-- Structure de la table `appointment`
--

CREATE TABLE `appointment` (
  `id_appointment` bigint(20) NOT NULL,
  `date` date DEFAULT NULL,
  `google_cal_event_id` varchar(255) DEFAULT NULL,
  `heure` time DEFAULT NULL,
  `laboratoire` varchar(255) DEFAULT NULL,
  `medecin` varchar(255) DEFAULT NULL,
  `motif` varchar(255) DEFAULT NULL,
  `specialite` varchar(255) DEFAULT NULL,
  `status` enum('CANCELLED','COMPLETED','CONFIRMED','DONE','NO_SHOW','PENDING') DEFAULT NULL,
  `type_rdv` enum('DOCTOR','LAB') DEFAULT NULL,
  `consultation_id_consultation` bigint(20) DEFAULT NULL,
  `laboratory_id_lab` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `consultation`
--

CREATE TABLE `consultation` (
  `id_consultation` bigint(20) NOT NULL,
  `clinical_notes` varchar(255) DEFAULT NULL,
  `date_cons` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `follow_up_date` datetime(6) DEFAULT NULL,
  `medical_record_id_medical_record` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `consultation_prerequisite`
--

CREATE TABLE `consultation_prerequisite` (
  `id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `follow_up`
--

CREATE TABLE `follow_up` (
  `id_follow_up` bigint(20) NOT NULL,
  `is_read` bit(1) DEFAULT NULL,
  `message` varchar(255) DEFAULT NULL,
  `sender_type` tinyint(4) DEFAULT NULL,
  `sent_at` datetime(6) DEFAULT NULL,
  `consultation_id_consultation` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `laboratory`
--

CREATE TABLE `laboratory` (
  `id_lab` bigint(20) NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `lab_name` varchar(255) DEFAULT NULL,
  `phone` varbinary(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `mc_audit_log`
--

CREATE TABLE `mc_audit_log` (
  `id` bigint(20) NOT NULL,
  `action` varchar(60) NOT NULL,
  `details` varchar(500) DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `success` bit(1) NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `user_agent` text DEFAULT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `mc_audit_log`
--

INSERT INTO `mc_audit_log` (`id`, `action`, `details`, `ip_address`, `success`, `timestamp`, `user_agent`, `user_id`) VALUES
(1, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-02 00:23:27.000000', NULL, 1),
(2, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-03 04:26:21.000000', NULL, 2),
(3, 'ACCOUNT_CREATED', 'RPPS pending admin verification', NULL, b'1', '2026-03-03 05:29:12.000000', NULL, 3),
(4, 'ACCOUNT_CREATED', 'RPPS pending admin verification', NULL, b'1', '2026-03-28 12:57:44.000000', NULL, 5),
(5, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-28 14:03:51.000000', NULL, 7),
(6, 'PROFILE_UPDATE', NULL, NULL, b'1', '2026-03-28 14:52:52.000000', NULL, 7),
(7, 'PROFILE_UPDATE', NULL, NULL, b'1', '2026-03-28 14:52:59.000000', NULL, 7),
(8, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-30 08:44:19.000000', NULL, 8),
(9, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-30 08:57:00.000000', NULL, 9),
(10, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-30 08:59:36.000000', NULL, 10),
(11, 'ACCOUNT_DEACTIVATED', NULL, NULL, b'1', '2026-03-30 10:02:27.000000', NULL, 3),
(12, 'ACCOUNT_ACTIVATED', NULL, NULL, b'1', '2026-03-30 10:19:31.000000', NULL, 3),
(13, 'ACCOUNT_ACTIVATED', NULL, NULL, b'1', '2026-03-30 10:19:33.000000', NULL, 5),
(14, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-30 16:03:50.000000', NULL, 12),
(15, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-30 16:04:54.000000', NULL, 13),
(16, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-30 16:31:19.000000', NULL, 14),
(17, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-30 16:43:40.000000', NULL, 15),
(18, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-30 16:55:36.000000', NULL, 16),
(19, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-30 16:57:41.000000', NULL, 17),
(20, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-30 17:00:46.000000', NULL, 18),
(21, 'PROFILE_UPDATE', NULL, NULL, b'1', '2026-03-30 17:03:56.000000', NULL, 17),
(22, 'PROFILE_UPDATE', NULL, NULL, b'1', '2026-03-30 17:04:46.000000', NULL, 17),
(23, 'DOCTOR_VERIFICATION_APPROVED', NULL, NULL, b'1', '2026-03-30 17:06:03.000000', NULL, 3),
(24, 'ACCOUNT_DEACTIVATED', NULL, NULL, b'1', '2026-03-30 17:06:44.000000', NULL, 1),
(25, 'ACCOUNT_ACTIVATED', NULL, NULL, b'1', '2026-03-30 17:06:52.000000', NULL, 1),
(26, 'ACCOUNT_DEACTIVATED', NULL, NULL, b'1', '2026-03-30 17:06:59.000000', NULL, 2),
(27, 'ACCOUNT_ACTIVATED', NULL, NULL, b'1', '2026-03-30 17:07:14.000000', NULL, 2),
(28, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-30 19:06:01.000000', NULL, 19),
(29, 'ACCOUNT_CREATED', 'RPPS pending admin verification', NULL, b'1', '2026-03-30 20:16:09.000000', NULL, 20),
(30, 'PROFILE_UPDATE', NULL, NULL, b'1', '2026-03-30 20:17:24.000000', NULL, 20),
(31, 'DOCTOR_VERIFICATION_APPROVED', NULL, NULL, b'1', '2026-03-30 20:18:28.000000', NULL, 20),
(32, 'DOCTOR_VERIFICATION_SUSPENDED', 'motif 5:  missconduct', NULL, b'1', '2026-03-30 20:18:49.000000', NULL, 20),
(33, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-31 08:39:48.000000', NULL, 21),
(34, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-31 08:42:26.000000', NULL, 22),
(35, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-31 09:02:57.000000', NULL, 23),
(36, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-31 09:08:02.000000', NULL, 24),
(37, 'PROFILE_UPDATE', NULL, NULL, b'1', '2026-03-31 09:10:49.000000', NULL, 24),
(38, 'PROFILE_UPDATE', NULL, NULL, b'1', '2026-03-31 09:12:10.000000', NULL, 24),
(39, 'ACCOUNT_DEACTIVATED', NULL, NULL, b'1', '2026-03-31 10:20:54.000000', NULL, 1),
(40, 'ACCOUNT_ACTIVATED', NULL, NULL, b'1', '2026-03-31 10:21:06.000000', NULL, 17),
(41, 'ACCOUNT_DEACTIVATED', NULL, NULL, b'1', '2026-03-31 10:21:30.000000', NULL, 17),
(42, 'ACCOUNT_ACTIVATED', NULL, NULL, b'1', '2026-03-31 10:21:42.000000', NULL, 17),
(43, 'ACCOUNT_CREATED', NULL, NULL, b'1', '2026-03-31 10:29:09.000000', NULL, 25),
(44, 'ACCOUNT_ACTIVATED', NULL, NULL, b'1', '2026-03-31 10:34:19.000000', NULL, 1),
(45, 'DOCTOR_VERIFICATION_SUSPENDED', 'ezgezyfgezhefz', NULL, b'1', '2026-03-31 10:35:00.000000', NULL, 3),
(46, 'PROFILE_UPDATE', NULL, NULL, b'1', '2026-04-07 08:20:56.000000', NULL, 25);

-- --------------------------------------------------------

--
-- Structure de la table `mc_biometric_data`
--

CREATE TABLE `mc_biometric_data` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `embedding_vector` tinytext NOT NULL,
  `is_active` bit(1) NOT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `mc_oauth_tokens`
--

CREATE TABLE `mc_oauth_tokens` (
  `id` bigint(20) NOT NULL,
  `access_token` text NOT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `provider` varchar(20) NOT NULL,
  `refresh_token` text DEFAULT NULL,
  `scopes` varchar(500) DEFAULT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `medical_record`
--

CREATE TABLE `medical_record` (
  `id_medical_record` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `reminder`
--

CREATE TABLE `reminder` (
  `id_reminder` bigint(20) NOT NULL,
  `channel` enum('Email','SMS') DEFAULT NULL,
  `scheduled_at` datetime(6) DEFAULT NULL,
  `status` enum('Failed','Sent') DEFAULT NULL,
  `appointment_id_appointment` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `users`
--

CREATE TABLE `users` (
  `user_type` varchar(30) NOT NULL,
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `first_name` varchar(100) NOT NULL,
  `is_active` bit(1) NOT NULL,
  `keycloak_id` varchar(70) NOT NULL,
  `last_name` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `profile_picture` varchar(500) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `admin_level` varchar(20) NOT NULL DEFAULT 'ADMIN',
  `biometric_required` tinyint(1) NOT NULL DEFAULT 1,
  `department` varchar(100) DEFAULT NULL,
  `last_login_ip` varchar(45) DEFAULT NULL,
  `two_factor_enforced` tinyint(1) NOT NULL DEFAULT 1,
  `clinic_id` bigint(20) DEFAULT NULL,
  `consultation_duration` int(11) DEFAULT NULL,
  `consultation_fee` decimal(10,3) DEFAULT NULL,
  `doctor_plan_id` varchar(36) DEFAULT NULL,
  `google_calendar_linked` tinyint(1) NOT NULL DEFAULT 0,
  `is_verified` tinyint(1) DEFAULT 0,
  `license_number` varchar(50) DEFAULT NULL,
  `office_address` varchar(500) DEFAULT NULL,
  `rating` decimal(3,2) DEFAULT NULL,
  `rpps_number` varchar(11) DEFAULT NULL,
  `specialization` enum('CARDIOLOGY','DERMATOLOGY','GENERAL_PRACTICE','NEUROLOGY','ORTHOPEDICS','OTHER','PEDIATRICS','PSYCHIATRY','RADIOLOGY') DEFAULT NULL,
  `verification_status` enum('APPROVED','PENDING','REJECTED','SUSPENDED') DEFAULT NULL,
  `address` varchar(500) DEFAULT NULL,
  `allergies` text DEFAULT NULL,
  `biometric_enrolled` tinyint(1) NOT NULL DEFAULT 0,
  `blood_type` varchar(5) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `emergency_contact` varchar(255) DEFAULT NULL,
  `gender` enum('FEMALE','MALE','OTHER','PREFER_NOT_TO_SAY') DEFAULT NULL,
  `no_show_score` double DEFAULT NULL,
  `patient_plan_id` varchar(36) DEFAULT NULL,
  `social_security_num` varchar(400) DEFAULT NULL
) ;

--
-- Déchargement des données de la table `users`
--

INSERT INTO `users` (`user_type`, `id`, `created_at`, `email`, `first_name`, `is_active`, `keycloak_id`, `last_name`, `phone`, `profile_picture`, `updated_at`, `admin_level`, `biometric_required`, `department`, `last_login_ip`, `two_factor_enforced`, `clinic_id`, `consultation_duration`, `consultation_fee`, `doctor_plan_id`, `google_calendar_linked`, `is_verified`, `license_number`, `office_address`, `rating`, `rpps_number`, `specialization`, `verification_status`, `address`, `allergies`, `biometric_enrolled`, `blood_type`, `date_of_birth`, `emergency_contact`, `gender`, `no_show_score`, `patient_plan_id`, `social_security_num`) VALUES
('PATIENT', 1, '2026-03-02 00:23:27.000000', 'john.doe@email.com', 'John', b'1', '550e8400-e29b-41d4-a716-446655440000', 'Doe', '+21612345678', NULL, '2026-03-31 10:34:19.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, '12 Rue de la République, Tunis', NULL, 0, 'A+', '1990-05-15', 'Jane Doe +21698765432', 'MALE', NULL, NULL, 'Jqwuruk2TsaH39jRBdTH3MvM0dDyb9tInIP6SzOg0K0HBBczJG+FPbojoQ=='),
('PATIENT', 2, '2026-03-03 04:26:21.000000', 'AMINE@gmail.com', 'amine', b'1', 'TEMP-dcf4fa91-217c-4d86-8b6e-c8f9ddcd448e', 'mEKKI', '+21625789634', NULL, '2026-03-30 17:07:14.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2001-02-25', NULL, 'MALE', NULL, NULL, NULL),
('DOCTOR', 3, '2026-03-03 05:29:12.000000', 'ahmed@gmail.com', 'ahmed ', b'1', 'TEMP-736ff677-7533-4c5b-b002-1efed4335aa7', 'kozdoghli', '+21629365487', NULL, '2026-03-31 10:35:00.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, 30, NULL, NULL, 0, 0, NULL, '', NULL, '21547896311', 'DERMATOLOGY', 'SUSPENDED', NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
('DOCTOR', 5, '2026-03-28 12:57:44.000000', 'amineee@gmail.com', 'amine', b'1', 'TEMP-6b9dc163-85d1-40ad-ab16-6b92c3384695', 'mekki', '+21625487963', NULL, '2026-03-28 12:57:44.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, 30, 25.000, NULL, 0, 0, '15423', '12 ouarddia 1', NULL, '21345698715', 'DERMATOLOGY', 'PENDING', NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
('PATIENT', 7, '2026-03-28 14:03:51.000000', 'ahmeeed@gmail.com', 'ahmed ', b'1', '42f3c624-162d-44db-bcb1-8b99e4fe9570', 'ahmed', '+216 52147896', NULL, '2026-03-28 14:03:51.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, 'tunis,ouardia', NULL, 0, 'B+', '2001-08-25', 'mama+995478633', 'PREFER_NOT_TO_SAY', NULL, NULL, 'FEneMnPcODOjEQ4kpVg4MLHaMFMHobIrxWRbPyEJ37IX2KpIsiPxuHcREA=='),
('PATIENT', 8, '2026-03-30 08:44:19.000000', 'patient.null.doctor.fields+1774860259074@example.com', 'Test', b'1', 'TEST-1774860259076', 'Patient', '+21600000000', NULL, '2026-03-30 08:44:19.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '1999-01-01', NULL, 'MALE', NULL, NULL, NULL),
('PATIENT', 9, '2026-03-30 08:57:00.000000', 'patient.null.doctor.fields+1774861020700@example.com', 'Test', b'1', 'TEST-1774861020701', 'Patient', '+21600000000', NULL, '2026-03-30 08:57:00.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '1999-01-01', NULL, 'MALE', NULL, NULL, NULL),
('PATIENT', 10, '2026-03-30 08:59:36.000000', 'patient.null.doctor.fields+1774861176543@example.com', 'Test', b'1', 'TEST-1774861176545', 'Patient', '+21600000000', NULL, '2026-03-30 08:59:36.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '1999-01-01', NULL, 'MALE', NULL, NULL, NULL),
('ADMINISTRATOR', 11, '2026-03-30 10:59:41.000000', 'admin@mediconnect.tn', 'Super', b'1', '26a4a0cf-90eb-4d0c-881f-83e73f291de1', 'Admin', NULL, NULL, '2026-03-30 10:59:41.000000', 'SUPER_ADMIN', 1, 'Platform', NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
('PATIENT', 12, '2026-03-30 16:03:50.000000', 'patient.null.doctor.fields+1774886630509@example.com', 'Test', b'1', 'TEST-1774886630509', 'Patient', '+21600000000', NULL, '2026-03-30 16:03:50.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '1999-01-01', NULL, 'MALE', NULL, NULL, NULL),
('PATIENT', 13, '2026-03-30 16:04:54.000000', 'patient.null.doctor.fields+1774886694274@example.com', 'Test', b'1', 'TEST-1774886694275', 'Patient', '+21600000000', NULL, '2026-03-30 16:04:54.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '1999-01-01', NULL, 'MALE', NULL, NULL, NULL),
('PATIENT', 14, '2026-03-30 16:31:19.000000', 'patient.null.doctor.fields+1774888279041@example.com', 'Test', b'1', 'TEST-1774888279042', 'Patient', '+21600000000', NULL, '2026-03-30 16:31:19.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '1999-01-01', NULL, 'MALE', NULL, NULL, NULL),
('PATIENT', 15, '2026-03-30 16:43:40.000000', 'patient.null.doctor.fields+1774889020165@example.com', 'Test', b'1', 'TEST-1774889020165', 'Patient', '+21600000000', NULL, '2026-03-30 16:43:40.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '1999-01-01', NULL, 'MALE', NULL, NULL, NULL),
('PATIENT', 16, '2026-03-30 16:55:36.000000', 'patient.null.doctor.fields+1774889736603@example.com', 'Test', b'1', 'TEST-1774889736603', 'Patient', '+21600000000', NULL, '2026-03-30 16:55:36.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '1999-01-01', NULL, 'MALE', NULL, NULL, NULL),
('PATIENT', 17, '2026-03-30 16:57:41.000000', 'mekkiamine34@gmail.com', 'amine', b'1', '2a1d092e-6491-415c-a0e4-4b734afabd68', 'mekki', '+216 95874632', 'http://localhost:8080/mediconnect/api/files/images/anon_1774889860862_abfb4fad-8750-4bd9-a7a5-e3edec119442_chapitre1-solution.png', '2026-03-31 10:21:42.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 1, NULL, NULL, NULL, NULL, NULL, NULL, 'ouardia,tunis', NULL, 0, 'B-', '2002-08-25', 'mama + 997521587', 'FEMALE', NULL, NULL, 'R0XwLkFq4UfRwXZR8aV3/51GAZhAklR/Q9ogDKhP9QwNVG3xedVQNpviAA=='),
('PATIENT', 18, '2026-03-30 17:00:46.000000', 'patient.null.doctor.fields+1774890046451@example.com', 'Test', b'1', 'TEST-1774890046452', 'Patient', '+21600000000', NULL, '2026-03-30 17:00:46.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '1999-01-01', NULL, 'MALE', NULL, NULL, NULL),
('PATIENT', 19, '2026-03-30 19:06:01.000000', 'patient.null.doctor.fields+1774897561458@example.com', 'Test', b'1', 'TEST-1774897561458', 'Patient', '+21600000000', NULL, '2026-03-30 19:06:01.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '1999-01-01', NULL, 'MALE', NULL, NULL, NULL),
('DOCTOR', 20, '2026-03-30 20:16:08.000000', 'amineemakki147@gmail.com', 'amine', b'1', '65f76e35-7be5-44c8-9192-0653a302efbb', 'mekki', '+21625789632', 'http://localhost:8080/mediconnect/api/files/images/anon_1774901768367_c4577d1d-a7c5-4529-8878-18280bbab795_chapitre1-solution.png', '2026-03-30 20:18:49.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, 30, 80.000, NULL, 0, 1, '25478', 'ouardia tunis', NULL, '10012345678', 'CARDIOLOGY', 'SUSPENDED', 'ouardia,tunis', NULL, 0, NULL, '2002-08-25', NULL, 'MALE', NULL, NULL, NULL),
('PATIENT', 21, '2026-03-31 08:39:48.000000', 'nibrassounissi@gmail.com', 'nibras', b'1', '6dd9a787-0350-4ef6-9828-8f60241945a5', 'ounissi', '+21645789632', 'http://localhost:8080/mediconnect/api/files/images/anon_1774946387951_06b1835c-308a-488d-96dc-ac33b9a2a43d_doctor.png', '2026-03-31 08:39:48.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, 'tunis,ouardia', 'x', 0, 'B+', '2001-08-25', 'mama|+21695647812', 'FEMALE', NULL, NULL, NULL),
('PATIENT', 22, '2026-03-31 08:42:26.000000', 'nebrasseounissi@gmail.com', 'nibras', b'1', 'c137f2ee-008b-40db-abd5-8186f61d8d3b', 'ounissi', '+21645789632', 'http://localhost:8080/mediconnect/api/files/images/anon_1774946546711_36e6a9ce-c5cd-4ce6-9e4c-a8fcb1a1f112_doctor.png', '2026-03-31 08:43:28.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 1, NULL, NULL, NULL, NULL, NULL, NULL, 'tunis,ouardia', 'x', 0, 'B+', '2001-08-25', 'mama|+21695647812', 'FEMALE', NULL, NULL, NULL),
('PATIENT', 23, '2026-03-31 09:02:57.000000', 'amenimensi91@gmail.com', 'ameni', b'1', '2ff83a60-d37f-4634-85a9-445d4de591d9', 'mensi', '+21625789632', 'http://localhost:8080/mediconnect/api/files/images/anon_1774947777370_9c30c6ed-f910-43c9-a8ab-13882b060de1_examination.png', '2026-03-31 09:03:28.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 1, NULL, NULL, NULL, NULL, NULL, NULL, 'Ouardia,tunis', 'Peniciline', 0, 'B+', '2001-08-25', 'mama|+21699057842', 'FEMALE', NULL, NULL, NULL),
('PATIENT', 24, '2026-03-31 09:08:02.000000', 'ameni.mensi@esprit.tn', 'Mensi', b'1', '0bcaa43b-2ffe-4a19-86fe-57654f749726', 'Ameni', '+21654691559', 'http://localhost:8080/mediconnect/api/files/images/anon_1774948317353_e26a6034-c9cd-41ff-bec9-eb07e027fb24_hospital.png', '2026-03-31 09:12:10.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 1, NULL, NULL, NULL, NULL, NULL, NULL, 'tunis1', NULL, 0, NULL, '2002-07-04', NULL, 'FEMALE', NULL, NULL, NULL),
('PATIENT', 25, '2026-03-31 10:29:09.000000', 'mohamedamine.mekki@esprit.tn', 'amine', b'1', '9bd485e9-fbc7-4d53-be4c-b05920240bff', 'mekki', '+21629787544', 'http://localhost:8080/mediconnect/api/files/images/anon_1775550056491_6af519af-ab0e-4ed2-bd4d-d2a4e8000796_doctor.png', '2026-04-07 08:20:56.000000', 'ADMIN', 1, NULL, NULL, 1, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 'A+', '2002-02-25', 'pere|+21698547963', 'MALE', NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `verification_codes`
--

CREATE TABLE `verification_codes` (
  `id` bigint(20) NOT NULL,
  `code` varchar(6) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `used` bit(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `verification_codes`
--

INSERT INTO `verification_codes` (`id`, `code`, `created_at`, `email`, `expires_at`, `used`) VALUES
(1, '172446', '2026-03-30 16:57:41.000000', 'mekkiamine34@gmail.com', '2026-03-30 17:07:41.000000', b'1'),
(2, '867240', '2026-03-30 17:01:47.000000', 'mekkiamine34@gmail.com', '2026-03-30 17:11:47.000000', b'1'),
(3, '428384', '2026-03-30 17:02:48.000000', 'mekkiamine34@gmail.com', '2026-03-30 17:12:48.000000', b'1'),
(4, '875819', '2026-03-30 20:16:09.000000', 'amineemakki147@gmail.com', '2026-03-30 20:26:09.000000', b'1'),
(5, '637914', '2026-03-31 08:39:48.000000', 'nibrassounissi@gmail.com', '2026-03-31 08:49:48.000000', b'1'),
(6, '166257', '2026-03-31 08:41:03.000000', 'nibrassounissi@gmail.com', '2026-03-31 08:51:03.000000', b'0'),
(7, '040244', '2026-03-31 08:42:26.000000', 'nebrasseounissi@gmail.com', '2026-03-31 08:52:26.000000', b'1'),
(8, '381251', '2026-03-31 09:02:57.000000', 'amenimensi91@gmail.com', '2026-03-31 09:12:57.000000', b'1'),
(9, '862068', '2026-03-31 09:08:02.000000', 'ameni.mensi@esprit.tn', '2026-03-31 09:18:02.000000', b'1'),
(10, '316560', '2026-03-31 10:29:09.000000', 'mohamedamine.mekki@esprit.tn', '2026-03-31 10:39:09.000000', b'1'),
(11, '360191', '2026-03-31 10:30:05.000000', 'mohamedamine.mekki@esprit.tn', '2026-03-31 10:40:05.000000', b'0');

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `appointment`
--
ALTER TABLE `appointment`
  ADD PRIMARY KEY (`id_appointment`),
  ADD UNIQUE KEY `UKknw1lvapm0ay33i66s2diwq28` (`consultation_id_consultation`),
  ADD KEY `FKf6kv6agh3j5cnrfxhs9312o9r` (`laboratory_id_lab`);

--
-- Index pour la table `consultation`
--
ALTER TABLE `consultation`
  ADD PRIMARY KEY (`id_consultation`),
  ADD KEY `FK28k84tlgl12jic95dkebdehrp` (`medical_record_id_medical_record`);

--
-- Index pour la table `consultation_prerequisite`
--
ALTER TABLE `consultation_prerequisite`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `follow_up`
--
ALTER TABLE `follow_up`
  ADD PRIMARY KEY (`id_follow_up`),
  ADD KEY `FKw3r5bk5q1h377pl9tomr8d9u` (`consultation_id_consultation`);

--
-- Index pour la table `laboratory`
--
ALTER TABLE `laboratory`
  ADD PRIMARY KEY (`id_lab`);

--
-- Index pour la table `mc_audit_log`
--
ALTER TABLE `mc_audit_log`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_audit_user_id` (`user_id`),
  ADD KEY `idx_audit_timestamp` (`timestamp`),
  ADD KEY `idx_audit_action` (`action`);

--
-- Index pour la table `mc_biometric_data`
--
ALTER TABLE `mc_biometric_data`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK74pl5do4hu3lfo3hq9kse5e6s` (`user_id`);

--
-- Index pour la table `mc_oauth_tokens`
--
ALTER TABLE `mc_oauth_tokens`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKc6jfpagaaf3oxy283joabrc9b` (`user_id`);

--
-- Index pour la table `medical_record`
--
ALTER TABLE `medical_record`
  ADD PRIMARY KEY (`id_medical_record`);

--
-- Index pour la table `reminder`
--
ALTER TABLE `reminder`
  ADD PRIMARY KEY (`id_reminder`),
  ADD KEY `FK5ohh7ueakw003glgc2by3fef1` (`appointment_id_appointment`);

--
-- Index pour la table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_users_keycloak_id` (`keycloak_id`),
  ADD UNIQUE KEY `idx_users_email` (`email`),
  ADD UNIQUE KEY `UKq9vwlr3y0rupptwg77l3rchl7` (`rpps_number`),
  ADD UNIQUE KEY `UK3et8k9j319e5yod1p92yggav7` (`social_security_num`),
  ADD KEY `idx_users_user_type` (`user_type`);

--
-- Index pour la table `verification_codes`
--
ALTER TABLE `verification_codes`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `appointment`
--
ALTER TABLE `appointment`
  MODIFY `id_appointment` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `consultation`
--
ALTER TABLE `consultation`
  MODIFY `id_consultation` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `consultation_prerequisite`
--
ALTER TABLE `consultation_prerequisite`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `follow_up`
--
ALTER TABLE `follow_up`
  MODIFY `id_follow_up` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `laboratory`
--
ALTER TABLE `laboratory`
  MODIFY `id_lab` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `mc_audit_log`
--
ALTER TABLE `mc_audit_log`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=47;

--
-- AUTO_INCREMENT pour la table `mc_biometric_data`
--
ALTER TABLE `mc_biometric_data`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `mc_oauth_tokens`
--
ALTER TABLE `mc_oauth_tokens`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `medical_record`
--
ALTER TABLE `medical_record`
  MODIFY `id_medical_record` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `reminder`
--
ALTER TABLE `reminder`
  MODIFY `id_reminder` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `verification_codes`
--
ALTER TABLE `verification_codes`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `appointment`
--
ALTER TABLE `appointment`
  ADD CONSTRAINT `FKf6kv6agh3j5cnrfxhs9312o9r` FOREIGN KEY (`laboratory_id_lab`) REFERENCES `laboratory` (`id_lab`),
  ADD CONSTRAINT `FKr0k8xivurnb3wiagp46vpiq4x` FOREIGN KEY (`consultation_id_consultation`) REFERENCES `consultation` (`id_consultation`);

--
-- Contraintes pour la table `consultation`
--
ALTER TABLE `consultation`
  ADD CONSTRAINT `FK28k84tlgl12jic95dkebdehrp` FOREIGN KEY (`medical_record_id_medical_record`) REFERENCES `medical_record` (`id_medical_record`);

--
-- Contraintes pour la table `follow_up`
--
ALTER TABLE `follow_up`
  ADD CONSTRAINT `FKw3r5bk5q1h377pl9tomr8d9u` FOREIGN KEY (`consultation_id_consultation`) REFERENCES `consultation` (`id_consultation`);

--
-- Contraintes pour la table `mc_audit_log`
--
ALTER TABLE `mc_audit_log`
  ADD CONSTRAINT `FK9dyraoq5bpx2v2pjl6797umgg` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Contraintes pour la table `mc_biometric_data`
--
ALTER TABLE `mc_biometric_data`
  ADD CONSTRAINT `FKq7d81lilmdrfgxi5ls497proc` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Contraintes pour la table `mc_oauth_tokens`
--
ALTER TABLE `mc_oauth_tokens`
  ADD CONSTRAINT `FKni5sl9k8of7qsfjwvxt9a7tl7` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Contraintes pour la table `reminder`
--
ALTER TABLE `reminder`
  ADD CONSTRAINT `FK5ohh7ueakw003glgc2by3fef1` FOREIGN KEY (`appointment_id_appointment`) REFERENCES `appointment` (`id_appointment`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
